import { setup, assign, fromPromise } from 'xstate'
import api from '@/services/api'

/**
 * Transaction Machine
 * Full FSM-driven send-money flow:
 * idle → enteringRecipientCNIC → validatingRecipient → enteringAmount
 *   → confirmingTransaction → OTPVerification → processing → success | failure
 *
 * Events: SUBMIT_CNIC, VALIDATE_SUCCESS, VALIDATE_FAIL,
 *         SUBMIT_AMOUNT, CONFIRM, OTP_SUCCESS, OTP_FAIL,
 *         RETRY, CANCEL, TIMEOUT
 */
export const transactionMachine = setup({
  types: {},
  guards: {
    /**
     * Guard: CNIC must match XXXXX-XXXXXXX-X format
     */
    isValidCNIC: ({ context }) =>
      /^\d{5}-\d{7}-\d$/.test(context.recipientCNIC),
    /**
     * Guard: Amount must be positive and not exceed available balance
     */
    hasSufficientBalance: ({ context }) =>
      context.amount > 0 && context.amount <= context.availableBalance,
    /**
     * Guard: OTP retry allowed if under max retries
     */
    canRetryOTP: ({ context }) =>
      context.otpRetries < context.maxOtpRetries
  },
  actions: {
    setRecipientCNIC: assign(({ event }) => ({
      recipientCNIC: event.cnic ?? '',
      error: null
    })),
    setRecipientInfo: assign(({ event }) => ({
      recipientName: event.output?.recipientName ?? '',
      recipientAccountId: event.output?.accountId ?? null
    })),
    setAmount: assign(({ event }) => ({
      amount: event.amount ?? 0,
      error: null
    })),
    setOTP: assign(({ event }) => ({
      otp: event.otp ?? ''
    })),
    setError: assign(({ event }) => ({
      error: event.error ?? event.data?.message ?? 'An error occurred'
    })),
    incrementOtpRetries: assign(({ context }) => ({
      otpRetries: context.otpRetries + 1
    })),
    resetFlow: assign(() => ({
      recipientCNIC: '',
      recipientName: '',
      recipientAccountId: null,
      amount: 0,
      otp: '',
      otpRetries: 0,
      error: null
    })),
    setValidationError: assign(({ event }) => ({
      error: event.error ?? 'Recipient validation failed'
    }))
  },
  actors: {
    /**
     * Service: Validates recipient CNIC against backend
     */
    validateRecipientService: fromPromise(async ({ input }) => {
      const res = await api.post('/transactions/validate-recipient', { cnic: input.recipientCNIC })
      return res.data
    }),
    /**
     * Service: Sends OTP to user's registered phone
     */
    sendOTPService: fromPromise(async ({ input }) => {
      const res = await api.post('/otp/send', { transactionRef: input.transactionRef })
      return res.data
    }),
    /**
     * Service: Processes the actual money transfer
     */
    processTransactionService: fromPromise(async ({ input }) => {
      const res = await api.post('/transactions/send', {
        recipientCNIC: input.recipientCNIC,
        amount: input.amount,
        otp: input.otp
      })
      return res.data
    })
  }
}).createMachine({
  id: 'transaction',
  initial: 'idle',
  context: {
    recipientCNIC: '',
    recipientName: '',
    recipientAccountId: null,
    amount: 0,
    otp: '',
    otpRetries: 0,
    maxOtpRetries: 3,
    availableBalance: 0,
    transactionRef: null,
    error: null
  },
  states: {
    idle: {
      on: {
        START: {
          target: 'enteringRecipientCNIC',
          actions: 'resetFlow'
        }
      }
    },

    enteringRecipientCNIC: {
      on: {
        UPDATE_CNIC: {
          actions: assign(({ event }) => ({ recipientCNIC: event.cnic, error: null }))
        },
        SUBMIT_CNIC: [
          {
            // Guard: CNIC must be valid format before calling API
            guard: 'isValidCNIC',
            target: 'validatingRecipient',
            actions: 'setRecipientCNIC'
          },
          {
            // Invalid CNIC format - stay in state, show error
            actions: assign({ error: 'Invalid CNIC format. Expected: XXXXX-XXXXXXX-X' })
          }
        ],
        CANCEL: 'idle'
      }
    },

    validatingRecipient: {
      invoke: {
        id: 'validateRecipient',
        src: 'validateRecipientService',
        input: ({ context }) => ({ recipientCNIC: context.recipientCNIC }),
        onDone: {
          target: 'enteringAmount',
          actions: 'setRecipientInfo'
        },
        onError: {
          target: 'enteringRecipientCNIC',
          actions: assign(({ event }) => ({
            error: event.error?.response?.data?.message ?? event.error?.message ?? 'Recipient not found or account inactive'
          }))
        }
      },
      // Explicit VALIDATE_SUCCESS / VALIDATE_FAIL events (for manual/test use)
      on: {
        VALIDATE_SUCCESS: {
          target: 'enteringAmount',
          actions: 'setRecipientInfo'
        },
        VALIDATE_FAIL: {
          target: 'enteringRecipientCNIC',
          actions: 'setValidationError'
        }
      }
    },

    enteringAmount: {
      on: {
        UPDATE_AMOUNT: {
          actions: assign(({ event }) => ({ amount: event.amount, error: null }))
        },
        SET_BALANCE: {
          actions: assign(({ event }) => ({ availableBalance: event.balance }))
        },
        SUBMIT_AMOUNT: [
          {
            // Guard: check balance before allowing next step
            guard: 'hasSufficientBalance',
            target: 'confirmingTransaction',
            actions: 'setAmount'
          },
          {
            actions: assign(({ context }) => ({
              error: context.amount <= 0
                ? 'Amount must be greater than 0'
                : 'Insufficient balance'
            }))
          }
        ],
        BACK: 'enteringRecipientCNIC',
        CANCEL: 'idle'
      }
    },

    confirmingTransaction: {
      on: {
        CONFIRM: {
          target: 'OTPVerification'
        },
        BACK: 'enteringAmount',
        CANCEL: 'idle'
      }
    },

    OTPVerification: {
      // Timeout after 60 seconds if no OTP entered
      after: {
        60000: {
          target: 'failure',
          actions: assign({ error: 'OTP verification timed out. Please try again.' })
        }
      },
      entry: assign({ otpRetries: 0 }),
      on: {
        UPDATE_OTP: {
          actions: assign(({ event }) => ({ otp: event.otp }))
        },
        OTP_SUCCESS: {
          target: 'processing',
          actions: 'setOTP'
        },
        OTP_FAIL: [
          {
            // Guard: allow retry if under max attempts
            guard: 'canRetryOTP',
            // Stay in OTPVerification state and increment retries
            actions: ['incrementOtpRetries', 'setError']
          },
          {
            // Max retries exceeded - move to failure
            target: 'failure',
            actions: assign({ error: 'Maximum OTP attempts exceeded. Transaction cancelled.' })
          }
        ],
        CANCEL: 'idle'
      }
    },

    processing: {
      invoke: {
        id: 'processTransaction',
        src: 'processTransactionService',
        input: ({ context }) => ({
          recipientCNIC: context.recipientCNIC,
          amount: context.amount,
          otp: context.otp
        }),
        onDone: {
          target: 'success',
          actions: assign(({ event }) => ({
            transactionRef: event.output?.referenceNumber ?? null
          }))
        },
        onError: {
          target: 'failure',
          actions: assign(({ event }) => ({
            error: event.error?.response?.data?.message ?? event.error?.message ?? 'Transaction processing failed'
          }))
        }
      }
    },

    success: {
      on: {
        NEW_TRANSACTION: {
          target: 'idle',
          actions: 'resetFlow'
        },
        DONE: 'idle'
      }
    },

    failure: {
      on: {
        RETRY: {
          target: 'enteringRecipientCNIC',
          actions: 'resetFlow'
        },
        CANCEL: 'idle'
      }
    }
  }
})
