import { setup, assign, fromPromise } from 'xstate'
import api from '@/services/api'

/**
 * Auth Machine
 * States: idle → enteringCredentials → validating → success | error
 * Handles login, signup, JWT storage, and error recovery.
 */
export const authMachine = setup({
  types: {},
  guards: {
    /**
     * Guard: validates CNIC format (XXXXX-XXXXXXX-X)
     */
    isValidCNIC: ({ context }) =>
      /^\d{5}-\d{7}-\d$/.test(context.cnic),
    /**
     * Guard: validates password minimum length
     */
    isValidPassword: ({ context }) =>
      typeof context.password === 'string' && context.password.length >= 6
  },
  actions: {
    setCredentials: assign(({ event }) => ({
      cnic: event.cnic ?? '',
      password: event.password ?? '',
      fullName: event.fullName ?? '',
      isSignup: event.isSignup ?? false,
      error: null
    })),
    setError: assign(({ event }) => ({
      error: event.error ?? event.data?.message ?? 'An error occurred'
    })),
    storeToken: assign(({ event }) => {
      const token = event.output?.token
      if (token) localStorage.setItem('jwt_token', token)
      return { token }
    }),
    clearAuth: assign(() => {
      localStorage.removeItem('jwt_token')
      return { token: null, cnic: '', password: '' }
    })
  },
  actors: {
    loginService: fromPromise(async ({ input }) => {
      const res = await api.post('/auth/login', { cnic: input.cnic, password: input.password })
      return res.data
    }),
    signupService: fromPromise(async ({ input }) => {
      const res = await api.post('/auth/signup', { cnic: input.cnic, password: input.password, fullName: input.fullName })
      return res.data
    })
  }
}).createMachine({
  id: 'auth',
  initial: 'idle',
  context: {
    cnic: '',
    password: '',
    fullName: '',
    isSignup: false,
    token: null,
    error: null
  },
  states: {
    idle: {
      on: {
        START_LOGIN: {
          target: 'enteringCredentials',
          actions: assign({ isSignup: false, error: null })
        },
        START_SIGNUP: {
          target: 'enteringCredentials',
          actions: assign({ isSignup: true, error: null })
        }
      }
    },
    enteringCredentials: {
      on: {
        UPDATE_CREDENTIALS: {
          actions: 'setCredentials'
        },
        SUBMIT: [
          {
            guard: 'isValidCNIC',
            target: 'validating'
          },
          {
            actions: assign({ error: 'Invalid CNIC format. Expected: XXXXX-XXXXXXX-X' })
          }
        ],
        BACK: 'idle'
      }
    },
    validating: {
      invoke: {
        id: 'authService',
        src: ({ context }) => context.isSignup ? 'signupService' : 'loginService',
        input: ({ context }) => ({
          cnic: context.cnic,
          password: context.password,
          fullName: context.fullName
        }),
        onDone: {
          target: 'success',
          actions: 'storeToken'
        },
        onError: {
          target: 'error',
          actions: assign(({ event }) => ({
            error: event.error?.message ?? 'Authentication failed'
          }))
        }
      }
    },
    success: {
      on: {
        LOGOUT: {
          target: 'idle',
          actions: 'clearAuth'
        }
      }
    },
    error: {
      on: {
        RETRY: 'enteringCredentials',
        RESET: 'idle'
      }
    }
  }
})
