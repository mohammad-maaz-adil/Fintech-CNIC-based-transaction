import { setup, assign, fromPromise } from 'xstate'
import api from '@/services/api'

/**
 * Dashboard Machine
 * States: loading → loaded | error
 * Supports manual refresh: loaded → refreshing → loaded | error
 */
export const dashboardMachine = setup({
  types: {},
  actors: {
    fetchDashboardService: fromPromise(async () => {
      const res = await api.get('/dashboard/summary')
      return res.data
    })
  },
  actions: {
    setDashboardData: assign(({ event }) => ({
      balance: event.output?.balance ?? 0,
      accountNumber: event.output?.accountNumber ?? '',
      fullName: event.output?.fullName ?? '',
      recentTransactions: event.output?.recentTransactions ?? [],
      error: null
    })),
    setError: assign(({ event }) => ({
      error: event.error?.message ?? 'Failed to load dashboard'
    }))
  }
}).createMachine({
  id: 'dashboard',
  initial: 'loading',
  context: {
    balance: 0,
    accountNumber: '',
    fullName: '',
    recentTransactions: [],
    error: null
  },
  states: {
    loading: {
      invoke: {
        id: 'fetchDashboard',
        src: 'fetchDashboardService',
        onDone: {
          target: 'loaded',
          actions: 'setDashboardData'
        },
        onError: {
          target: 'error',
          actions: 'setError'
        }
      }
    },
    loaded: {
      on: {
        REFRESH: 'refreshing'
      }
    },
    error: {
      on: {
        RETRY: 'loading'
      }
    },
    refreshing: {
      invoke: {
        id: 'refreshDashboard',
        src: 'fetchDashboardService',
        onDone: {
          target: 'loaded',
          actions: 'setDashboardData'
        },
        onError: {
          target: 'error',
          actions: 'setError'
        }
      }
    }
  }
})
