<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <span class="auth-logo">💳</span>
        <h1>FinTech CNIC System</h1>
        <p>Secure CNIC-based Financial Transactions</p>
      </div>

      <!-- Toggle Login / Signup -->
      <div class="auth-tabs">
        <button :class="{ active: !isSignup }" @click="setMode(false)">Login</button>
        <button :class="{ active: isSignup }" @click="setMode(true)">Sign Up</button>
      </div>

      <AlertMessage v-if="showError" :message="errorMessage" type="error" @dismiss="errorMessage = ''" />

      <form @submit.prevent="handleSubmit" class="auth-form">
        <div v-if="isSignup" class="form-group">
          <label>Full Name</label>
          <input v-model="form.fullName" type="text" placeholder="Muhammad Ahmed" required />
        </div>

        <div class="form-group">
          <label>CNIC</label>
          <input
            v-model="form.cnic"
            type="text"
            placeholder="XXXXX-XXXXXXX-X"
            maxlength="15"
            @input="formatCNIC"
            required
          />
          <small class="hint">Format: 12345-1234567-1</small>
        </div>

        <div class="form-group">
          <label>Password</label>
          <input v-model="form.password" type="password" placeholder="••••••••" required minlength="6" />
        </div>

        <!-- Loading state from machine -->
        <button type="submit" class="btn-primary" :disabled="isLoading">
          <span v-if="isLoading" class="btn-spinner"></span>
          {{ isLoading ? 'Please wait...' : (isSignup ? 'Create Account' : 'Login') }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useMachine } from '@xstate/vue'
import { useRouter } from 'vue-router'
import authMachine from '@/machines/authMachine'
import AlertMessage from '@/components/AlertMessage.vue'

const router = useRouter()
const { snapshot, send } = useMachine(authMachine)
const showError = computed(() => snapshot.value.matches('error') && !!errorMessage.value)
const isSignup = ref(false)
const errorMessage = ref('')
const form = ref({ cnic: '', password: '', fullName: '' })

const isLoading = computed(() =>
  snapshot.value.matches('validatingLogin') ||
  snapshot.value.matches('validatingSignup')
)

function setMode(signup) {
  isSignup.value = signup
  errorMessage.value = ''
  form.value = { cnic: '', password: '', fullName: '' }
  send({ type: signup ? 'START_SIGNUP' : 'START_LOGIN' })
}

function formatCNIC(e) {
  let v = e.target.value.replace(/\D/g, '')
  if (v.length > 5) v = v.slice(0, 5) + '-' + v.slice(5)
  if (v.length > 13) v = v.slice(0, 13) + '-' + v.slice(13)
  form.value.cnic = v.slice(0, 15)
}

function handleSubmit() {
  errorMessage.value = ''

  send({
    type: 'UPDATE_CREDENTIALS',
    cnic: form.value.cnic,
    password: form.value.password,
    fullName: form.value.fullName,
    isSignup: isSignup.value
  })

  send({ type: 'SUBMIT' })

  // if machine stayed in enteringCredentials, show local validation error
  queueMicrotask(() => {
    if (snapshot.value.matches('enteringCredentials') && snapshot.value.context.error) {
      errorMessage.value = snapshot.value.context.error
    }
  })
}

// Watch machine state changes
watch(
  () => snapshot.value,
  (snap) => {
    if (snap.matches('success')) {
      errorMessage.value = ''
      router.push('/dashboard')
      return
    }

    if (snap.matches('error')) {
      errorMessage.value = snap.context.error ?? 'Authentication failed'
      return
    }

    // clear only when user is editing or fresh state
    if (snap.matches('idle') || snap.matches('enteringCredentials')) {
      errorMessage.value = ''
    }
  },
  { immediate: true }
)

// Immediately send START_LOGIN
send({ type: 'START_LOGIN' })
</script>

<style scoped>
.auth-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #1a237e 0%, #283593 50%, #1565c0 100%); padding: 1rem; }
.auth-card { background: white; border-radius: 16px; padding: 2.5rem; width: 100%; max-width: 420px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }
.auth-header { text-align: center; margin-bottom: 1.5rem; }
.auth-logo { font-size: 3rem; display: block; margin-bottom: 0.5rem; }
.auth-header h1 { font-size: 1.5rem; color: #1a237e; margin-bottom: 0.25rem; }
.auth-header p { color: #666; font-size: 0.9rem; }
.auth-tabs { display: flex; border-radius: 8px; overflow: hidden; border: 1px solid #e0e0e0; margin-bottom: 1.5rem; }
.auth-tabs button { flex: 1; padding: 0.6rem; border: none; background: #f5f5f5; cursor: pointer; font-size: 0.95rem; transition: all 0.2s; }
.auth-tabs button.active { background: #1a237e; color: white; }
.auth-form { display: flex; flex-direction: column; gap: 1rem; }
.form-group { display: flex; flex-direction: column; gap: 0.4rem; }
.form-group label { font-size: 0.9rem; color: #444; font-weight: 500; }
.form-group input { padding: 0.75rem; border: 1.5px solid #ddd; border-radius: 8px; font-size: 1rem; transition: border-color 0.2s; }
.form-group input:focus { outline: none; border-color: #1a237e; }
.hint { color: #888; font-size: 0.8rem; }
.btn-primary { background: #1a237e; color: white; border: none; padding: 0.85rem; border-radius: 8px; font-size: 1rem; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 0.5rem; transition: background 0.2s; margin-top: 0.5rem; }
.btn-primary:hover:not(:disabled) { background: #283593; }
.btn-primary:disabled { opacity: 0.7; cursor: not-allowed; }
.btn-spinner { width: 18px; height: 18px; border: 2px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
