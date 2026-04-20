<template>
  <div class="send-money-page">
    <NavBar @logout="handleLogout" />

    <div class="send-content">
      <div class="send-card">
        <h1>Send Money</h1>

        <!-- Progress Steps derived from machine state -->
        <div class="steps">
          <div v-for="(step, i) in steps" :key="i" class="step" :class="getStepClass(step.state)">
            <div class="step-num">{{ i + 1 }}</div>
            <div class="step-label">{{ step.label }}</div>
          </div>
        </div>

        <!-- Alert -->
        <AlertMessage
          v-if="currentError"
          :message="currentError"
          type="error"
          @dismiss="currentError = ''"
        />
        <AlertMessage
          v-if="successMsg"
          :message="successMsg"
          type="success"
          :dismissible="false"
        />

        <!-- STATE: idle -->
        <div v-if="snapshot.matches('idle')" class="state-panel">
          <p class="state-desc">Transfer money securely using recipient's CNIC.</p>
          <button class="btn-primary" @click="send({ type: 'START' })">Start Transfer</button>
        </div>

        <!-- STATE: enteringRecipientCNIC -->
        <div v-else-if="snapshot.matches('enteringRecipientCNIC')" class="state-panel">
          <div class="form-group">
            <label>Recipient CNIC</label>
            <input
              v-model="recipientCNIC"
              type="text"
              placeholder="XXXXX-XXXXXXX-X"
              maxlength="15"
              @input="formatCNIC"
              @keyup.enter="submitCNIC"
            />
            <small class="hint">Format: 12345-1234567-1</small>
          </div>
          <div class="btn-row">
            <button class="btn-secondary" @click="send({ type: 'CANCEL' })">Cancel</button>
            <button class="btn-primary" @click="submitCNIC" :disabled="!recipientCNIC">Next</button>
          </div>
        </div>

        <!-- STATE: validatingRecipient -->
        <div v-else-if="snapshot.matches('validatingRecipient')" class="state-panel">
          <LoadingSpinner message="Validating recipient..." />
        </div>

        <!-- STATE: enteringAmount -->
        <div v-else-if="snapshot.matches('enteringAmount')" class="state-panel">
          <div class="recipient-info">
            <span class="ri-label">Sending to:</span>
            <span class="ri-name">{{ snapshot.context.recipientName }}</span>
            <span class="ri-cnic">{{ snapshot.context.recipientCNIC }}</span>
          </div>
          <div class="form-group">
            <label>Amount (PKR)</label>
            <input
              v-model.number="amount"
              type="number"
              placeholder="0.00"
              min="1"
              step="0.01"
              @keyup.enter="submitAmount"
            />
            <small class="hint">Available: PKR {{ formatAmount(snapshot.context.availableBalance) }}</small>
          </div>
          <div class="btn-row">
            <button class="btn-secondary" @click="send({ type: 'BACK' })">Back</button>
            <button class="btn-primary" @click="submitAmount" :disabled="!amount || amount <= 0">Next</button>
          </div>
        </div>

        <!-- STATE: confirmingTransaction -->
        <div v-else-if="snapshot.matches('confirmingTransaction')" class="state-panel">
          <div class="confirm-box">
            <h3>Confirm Transfer</h3>
            <div class="confirm-row"><span>To:</span><strong>{{ snapshot.context.recipientName }}</strong></div>
            <div class="confirm-row"><span>CNIC:</span><strong>{{ snapshot.context.recipientCNIC }}</strong></div>
            <div class="confirm-row amount-row"><span>Amount:</span><strong>PKR {{ formatAmount(snapshot.context.amount) }}</strong></div>
          </div>
          <div class="btn-row">
            <button class="btn-secondary" @click="send({ type: 'BACK' })">Back</button>
            <button class="btn-primary" @click="send({ type: 'CONFIRM' })">Confirm &amp; Get OTP</button>
          </div>
        </div>

        <!-- STATE: OTPVerification -->
        <div v-else-if="snapshot.matches('OTPVerification')" class="state-panel">
          <div class="otp-info">
            <p>OTP sent to your registered number. Enter it below.</p>
            <p class="otp-timer">Time remaining: <strong>{{ otpTimer }}s</strong></p>
            <p v-if="snapshot.context.otpRetries > 0" class="otp-retries">
              Attempts: {{ snapshot.context.otpRetries }} / {{ snapshot.context.maxOtpRetries }}
            </p>
          </div>
          <div class="form-group">
            <label>OTP Code</label>
            <input
              v-model="otpCode"
              type="text"
              placeholder="6-digit OTP"
              maxlength="6"
              class="otp-input"
              @keyup.enter="submitOTP"
            />
          </div>
          <div class="btn-row">
            <button class="btn-secondary" @click="send({ type: 'CANCEL' })">Cancel</button>
            <button class="btn-primary" @click="submitOTP" :disabled="!otpCode || otpCode.length < 4">Verify OTP</button>
          </div>
        </div>

        <!-- STATE: processing -->
        <div v-else-if="snapshot.matches('processing')" class="state-panel">
          <LoadingSpinner message="Processing your transfer..." />
        </div>

        <!-- STATE: success -->
        <div v-else-if="snapshot.matches('success')" class="state-panel success-state">
          <div class="success-icon">✅</div>
          <h2>Transfer Successful!</h2>
          <p>PKR {{ formatAmount(snapshot.context.amount) }} sent to {{ snapshot.context.recipientName }}</p>
          <p v-if="snapshot.context.transactionRef" class="ref-no">Ref: {{ snapshot.context.transactionRef }}</p>
          <div class="btn-row">
            <RouterLink to="/dashboard" class="btn-secondary-link">Dashboard</RouterLink>
            <button class="btn-primary" @click="send({ type: 'NEW_TRANSACTION' })">New Transfer</button>
          </div>
        </div>

        <!-- STATE: failure -->
        <div v-else-if="snapshot.matches('failure')" class="state-panel failure-state">
          <div class="failure-icon">❌</div>
          <h2>Transfer Failed</h2>
          <p class="failure-msg">{{ snapshot.context.error }}</p>
          <div class="btn-row">
            <button class="btn-secondary" @click="send({ type: 'CANCEL' })">Cancel</button>
            <button class="btn-primary" @click="send({ type: 'RETRY' })">Try Again</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { useMachine } from '@xstate/vue'
import { useRouter, RouterLink } from 'vue-router'
import { transactionMachine } from '@/machines/transactionMachine'
import api from '@/services/api'
import NavBar from '@/components/NavBar.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import AlertMessage from '@/components/AlertMessage.vue'

const router = useRouter()
const { snapshot, send } = useMachine(transactionMachine)

const recipientCNIC = ref('')
const amount = ref(null)
const otpCode = ref('')
const currentError = ref('')
const successMsg = ref('')
const otpTimer = ref(60)
let otpInterval = null

const steps = [
  { label: 'Recipient', state: ['enteringRecipientCNIC', 'validatingRecipient'] },
  { label: 'Amount', state: ['enteringAmount'] },
  { label: 'Confirm', state: ['confirmingTransaction'] },
  { label: 'OTP', state: ['OTPVerification', 'processing'] },
  { label: 'Done', state: ['success'] }
]

function getStepClass(states) {
  const current = snapshot.value.value
  if (states.includes(current)) return 'active'
  const allStates = ['idle', 'enteringRecipientCNIC', 'validatingRecipient', 'enteringAmount', 'confirmingTransaction', 'OTPVerification', 'processing', 'success', 'failure']
  const stepStart = allStates.indexOf(states[0])
  const currentIdx = allStates.indexOf(current)
  return currentIdx > stepStart ? 'done' : ''
}

function formatCNIC(e) {
  let v = e.target.value.replace(/\D/g, '')
  if (v.length > 5) v = v.slice(0, 5) + '-' + v.slice(5)
  if (v.length > 13) v = v.slice(0, 13) + '-' + v.slice(13)
  recipientCNIC.value = v.slice(0, 15)
}

function submitCNIC() {
  currentError.value = ''
  send({ type: 'SUBMIT_CNIC', cnic: recipientCNIC.value })
}

function submitAmount() {
  currentError.value = ''
  send({ type: 'SUBMIT_AMOUNT', amount: amount.value })
}

async function submitOTP() {
  currentError.value = ''
  try {
    const res = await api.post('/otp/verify', { otp: otpCode.value })
    if (res.data.valid) {
      send({ type: 'OTP_SUCCESS', otp: otpCode.value })
    } else {
      send({ type: 'OTP_FAIL', error: 'Invalid OTP code' })
    }
  } catch (err) {
    send({ type: 'OTP_FAIL', error: err.response?.data?.message ?? 'OTP verification failed' })
  }
}

function formatAmount(n) {
  return Number(n ?? 0).toLocaleString('en-PK', { minimumFractionDigits: 2 })
}

function handleLogout() {
  localStorage.removeItem('jwt_token')
  router.push('/login')
}

// Watch for OTP state and start countdown timer
watch(
  () => snapshot.value.value,
  (state) => {
    if (state === 'OTPVerification') {
      otpTimer.value = 60
      otpInterval = setInterval(() => {
        otpTimer.value--
        if (otpTimer.value <= 0) {
          clearInterval(otpInterval)
        }
      }, 1000)
    } else {
      clearInterval(otpInterval)
    }

    if (state === 'enteringAmount') {
      loadBalance()
    }
  }
)

// Watch for errors in context
watch(
  () => snapshot.value.context.error,
  (err) => {
    if (err) currentError.value = err
  }
)

async function loadBalance() {
  try {
    const res = await api.get('/dashboard/summary')
    snapshot.value.context.availableBalance = res.data.balance
  } catch {}
}

onUnmounted(() => clearInterval(otpInterval))

// Load initial balance
loadBalance()
</script>

<style scoped>
.send-money-page { min-height: 100vh; background: #f0f2f5; }
.send-content { max-width: 560px; margin: 0 auto; padding: 2rem 1rem; }
.send-card { background: white; border-radius: 16px; padding: 2rem; box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.send-card h1 { font-size: 1.5rem; color: #1a237e; margin-bottom: 1.5rem; }
.steps { display: flex; gap: 0; margin-bottom: 2rem; }
.step { flex: 1; text-align: center; position: relative; }
.step::after { content: ''; position: absolute; top: 16px; left: 50%; width: 100%; height: 2px; background: #e0e0e0; z-index: 0; }
.step:last-child::after { display: none; }
.step-num { width: 32px; height: 32px; border-radius: 50%; background: #e0e0e0; color: #999; display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 0.85rem; margin: 0 auto 0.3rem; position: relative; z-index: 1; transition: all 0.2s; }
.step.active .step-num { background: #1a237e; color: white; }
.step.done .step-num { background: #2e7d32; color: white; }
.step-label { font-size: 0.75rem; color: #999; }
.step.active .step-label { color: #1a237e; font-weight: 600; }
.step.done .step-label { color: #2e7d32; }
.state-panel { padding: 1rem 0; }
.state-desc { color: #666; margin-bottom: 1.5rem; }
.form-group { display: flex; flex-direction: column; gap: 0.4rem; margin-bottom: 1rem; }
.form-group label { font-size: 0.9rem; color: #444; font-weight: 500; }
.form-group input { padding: 0.75rem; border: 1.5px solid #ddd; border-radius: 8px; font-size: 1rem; transition: border-color 0.2s; }
.form-group input:focus { outline: none; border-color: #1a237e; }
.hint { color: #888; font-size: 0.8rem; }
.btn-row { display: flex; gap: 1rem; margin-top: 1rem; }
.btn-primary { flex: 1; background: #1a237e; color: white; border: none; padding: 0.85rem; border-radius: 8px; font-size: 1rem; cursor: pointer; transition: background 0.2s; }
.btn-primary:hover:not(:disabled) { background: #283593; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-secondary { flex: 1; background: #f5f5f5; color: #333; border: 1px solid #ddd; padding: 0.85rem; border-radius: 8px; font-size: 1rem; cursor: pointer; }
.btn-secondary-link { flex: 1; background: #f5f5f5; color: #333; border: 1px solid #ddd; padding: 0.85rem; border-radius: 8px; font-size: 1rem; text-align: center; text-decoration: none; }
.recipient-info { background: #e8eaf6; border-radius: 8px; padding: 1rem; margin-bottom: 1rem; }
.ri-label { font-size: 0.85rem; color: #666; display: block; }
.ri-name { font-size: 1.1rem; font-weight: 600; color: #1a237e; display: block; }
.ri-cnic { font-size: 0.85rem; color: #555; display: block; }
.confirm-box { background: #f5f7ff; border-radius: 10px; padding: 1.5rem; margin-bottom: 1rem; }
.confirm-box h3 { color: #1a237e; margin-bottom: 1rem; }
.confirm-row { display: flex; justify-content: space-between; padding: 0.5rem 0; border-bottom: 1px solid #e0e0e0; font-size: 0.95rem; }
.confirm-row:last-child { border-bottom: none; }
.amount-row strong { color: #1a237e; font-size: 1.1rem; }
.otp-info { background: #e8f5e9; border-radius: 8px; padding: 1rem; margin-bottom: 1rem; }
.otp-info p { color: #2e7d32; font-size: 0.95rem; margin-bottom: 0.25rem; }
.otp-timer { color: #333 !important; }
.otp-retries { color: #e65100 !important; }
.otp-input { letter-spacing: 0.5rem; font-size: 1.5rem; text-align: center; font-weight: 600; }
.success-state, .failure-state { text-align: center; padding: 1.5rem 0; }
.success-icon, .failure-icon { font-size: 3.5rem; margin-bottom: 1rem; }
.success-state h2 { color: #2e7d32; margin-bottom: 0.5rem; }
.success-state p { color: #555; margin-bottom: 0.25rem; }
.ref-no { font-size: 0.8rem; color: #999; font-family: monospace; }
.failure-state h2 { color: #c62828; margin-bottom: 0.5rem; }
.failure-msg { color: #c62828; margin-bottom: 1rem; }
</style>
