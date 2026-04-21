# Finite State Machine Diagrams

This document describes the XState machines used in the frontend.

---

## 1. authMachine

Manages the login/signup flow.

```
idle
  ├─[START_LOGIN]──→ enteringCredentials
  └─[START_SIGNUP]─→ enteringCredentials

enteringCredentials
  ├─[UPDATE_CREDENTIALS]─→ (self, updates context)
  ├─[SUBMIT / guard:isValidCNIC]─→ validating
  ├─[SUBMIT / !isValidCNIC]──→ (self, sets error)
  └─[BACK]─→ idle

validating (invokes: loginService | signupService)
  ├─[onDone]─→ success  (stores JWT)
  └─[onError]─→ error   (sets error message)

success
  └─[LOGOUT]─→ idle (clears JWT)

error
  ├─[RETRY]─→ enteringCredentials
  └─[RESET]─→ idle
```

**Guards:**
- `isValidCNIC`: Regex `^\d{5}-\d{7}-\d$`
- `isValidPassword`: Length ≥ 6

---

## 2. transactionMachine

FSM-driven send-money flow with full OTP handling.

```
idle
  └─[START]─→ enteringRecipientCNIC

enteringRecipientCNIC
  ├─[UPDATE_CNIC]─→ (self)
  ├─[SUBMIT_CNIC / guard:isValidCNIC]─→ validatingRecipient
  ├─[SUBMIT_CNIC / !isValidCNIC]──→ (self, error: invalid format)
  └─[CANCEL]─→ idle

validatingRecipient (invokes: validateRecipientService)
  ├─[onDone / VALIDATE_SUCCESS]─→ enteringAmount (sets recipient info)
  └─[onError / VALIDATE_FAIL]──→ enteringRecipientCNIC (sets error)

enteringAmount
  ├─[UPDATE_AMOUNT]─→ (self)
  ├─[SUBMIT_AMOUNT / guard:hasSufficientBalance]─→ confirmingTransaction
  ├─[SUBMIT_AMOUNT / !hasSufficientBalance]──→ (self, error)
  ├─[BACK]─→ enteringRecipientCNIC
  └─[CANCEL]─→ idle

confirmingTransaction
  ├─[CONFIRM]─→ OTPVerification
  ├─[BACK]─→ enteringAmount
  └─[CANCEL]─→ idle

OTPVerification (60s timeout)
  ├─[UPDATE_OTP]─→ (self)
  ├─[OTP_SUCCESS]─→ processing
  ├─[OTP_FAIL / guard:canRetryOTP]─→ (self, increment retries)
  ├─[OTP_FAIL / !canRetryOTP]──→ failure (max retries exceeded)
  ├─[after 60000ms]─→ failure (timeout)
  └─[CANCEL]─→ idle

processing (invokes: processTransactionService)
  ├─[onDone]─→ success
  └─[onError]─→ failure

success
  ├─[NEW_TRANSACTION]─→ idle (reset context)
  └─[DONE]─→ idle

failure
  ├─[RETRY]─→ enteringRecipientCNIC (reset context)
  └─[CANCEL]─→ idle
```

**Guards:**
- `isValidCNIC`: Regex `^\d{5}-\d{7}-\d$`
- `hasSufficientBalance`: `amount > 0 && amount <= availableBalance`
- `canRetryOTP`: `otpRetries < maxOtpRetries` (default max: 3)

**Services:**
- `validateRecipientService`: `POST /api/transactions/validate-recipient`
- `processTransactionService`: `POST /api/transactions/send`

---

## 3. dashboardMachine

```
loading (invokes: fetchDashboardService)
  ├─[onDone]─→ loaded
  └─[onError]─→ error

loaded
  └─[REFRESH]─→ refreshing

error
  └─[RETRY]─→ loading

refreshing (invokes: fetchDashboardService)
  ├─[onDone]─→ loaded
  └─[onError]─→ error
```

---

## 4. historyMachine (Optional)

```
fetching (invokes: fetchHistoryService)
  ├─[onDone / items > 0]─→ success
  ├─[onDone / items = 0]─→ empty
  └─[onError]─→ error

success
  ├─[REFRESH]─→ fetching
  └─[LOAD_PAGE]─→ fetching (with new page number)

empty
  └─[REFRESH]─→ fetching

error
  └─[RETRY]─→ fetching
```
