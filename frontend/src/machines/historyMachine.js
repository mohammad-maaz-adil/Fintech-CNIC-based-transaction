import { setup, assign, fromPromise } from 'xstate'
import api from '@/services/api'

/**
 * Transaction History Machine (Optional FSM)
 * States: fetching → success | empty | error
 */
export const historyMachine = setup({
  types: {},
  actors: {
    fetchHistoryService: fromPromise(async ({ input }) => {
      const params = input?.page ? `?page=${input.page}&size=20` : '?page=0&size=20'
      const res = await api.get(`/transactions/history${params}`)
      return res.data
    })
  },
  actions: {
    setTransactions: assign(({ event }) => ({
      transactions: event.output?.transactions ?? [],
      totalPages: event.output?.totalPages ?? 0,
      error: null
    })),
    setError: assign(({ event }) => ({
      error: event.error?.message ?? 'Failed to load transaction history'
    }))
  }
}).createMachine({
  id: 'history',
  initial: 'fetching',
  context: {
    transactions: [],
    totalPages: 0,
    currentPage: 0,
    error: null
  },
  states: {
    fetching: {
      invoke: {
        id: 'fetchHistory',
        src: 'fetchHistoryService',
        input: ({ context }) => ({ page: context.currentPage }),
        onDone: [
          {
            guard: ({ event }) => (event.output?.transactions?.length ?? 0) > 0,
            target: 'success',
            actions: 'setTransactions'
          },
          {
            target: 'empty',
            actions: 'setTransactions'
          }
        ],
        onError: {
          target: 'error',
          actions: 'setError'
        }
      }
    },
    success: {
      on: {
        REFRESH: 'fetching',
        LOAD_PAGE: {
          target: 'fetching',
          actions: assign(({ event }) => ({ currentPage: event.page }))
        }
      }
    },
    empty: {
      on: {
        REFRESH: 'fetching'
      }
    },
    error: {
      on: {
        RETRY: 'fetching'
      }
    }
  }
})
