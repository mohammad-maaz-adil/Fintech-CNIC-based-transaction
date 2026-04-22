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
    isValidCNIC: ({ context }) => /^\d{5}-\d{7}-\d$/.test(context.cnic),

    /**
     * Guard: validates password minimum length
     */
    isValidPassword: ({ context }) =>
      typeof context.password === 'string' && context.password.length >= 6
  },
  actions: {
    setCredentials: assign(({ context, event }) => ({
      // Preserve existing values if field not provided in event
      cnic: event.cnic ?? context.cnic ?? '',
      password: event.password ?? context.password ?? '',
      fullName: event.fullName ?? context.fullName ?? '',
      isSignup: event.isSignup ?? context.isSignup ?? false,
      error: null
    })),

    setError: assign(({ event }) => ({
      error:
        event.error?.response?.data?.message ||
        event.error?.data?.message ||
        event.error?.message ||
        event.data?.message ||
        'An error occurred'
    })),

    storeToken: assign(({ event }) => {
      const token = event.output?.token
      if (token) localStorage.setItem('jwt_token', token)
      return { token }
    }),

    clearAuth: assign(() => {
      localStorage.removeItem('jwt_token')
      return { token: null, cnic: '', password: '', fullName: '', error: null }
    })
  },
  actors: {
    loginService: fromPromise(async ({ input }) => {
      const res = await api.post('/auth/login', {
        cnic: input.cnic?.trim(),
        password: input.password
      })
      return res.data
    }),

    signupService: fromPromise(async ({ input }) => {
      const res = await api.post('/auth/signup', {
        cnic: input.cnic?.trim(),
        password: input.password,
        fullName: input.fullName?.trim()
      })
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
            guard: ({ context }) =>
              /^\d{5}-\d{7}-\d$/.test(context.cnic) &&
              !context.isSignup &&
              typeof context.password === 'string' &&
              context.password.length >= 6,
            target: 'validatingLogin'
          },
          {
            guard: ({ context }) =>
              /^\d{5}-\d{7}-\d$/.test(context.cnic) &&
              context.isSignup &&
              typeof context.password === 'string' &&
              context.password.length >= 6 &&
              typeof context.fullName === 'string' &&
              context.fullName.trim().length > 0,
            target: 'validatingSignup'
          },
          {
            actions: assign(({ context }) => ({
              error: !/^\d{5}-\d{7}-\d$/.test(context.cnic)
                ? 'Invalid CNIC format. Expected: XXXXX-XXXXXXX-X'
                : context.password.length < 6
                ? 'Password must be at least 6 characters'
                : context.isSignup && !context.fullName.trim()
                ? 'Full name is required'
                : 'Please check your input'
            }))
          }
        ],

        BACK: 'idle'
      }
    },

    validatingLogin: {
      invoke: {
        id: 'loginRequest',
        src: 'loginService',
        input: ({ context }) => ({
          cnic: context.cnic,
          password: context.password
        }),
        onDone: {
          target: 'success',
          actions: 'storeToken'
        },
        onError: {
          target: 'error',
          actions: assign(({ event }) => ({
            error:
              event.error?.response?.data?.message ||
              event.error?.data?.message ||
              event.error?.message ||
              'Authentication failed'
          }))
        }
      }
    },

    validatingSignup: {
      invoke: {
        id: 'signupRequest',
        src: 'signupService',
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
            error:
              event.error?.response?.data?.message ||
              event.error?.data?.message ||
              event.error?.message ||
              'Signup failed'
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

export default authMachine