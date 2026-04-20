<template>
  <div class="profile-page">
    <NavBar @logout="handleLogout" />

    <div class="profile-content">
      <div class="profile-card">
        <div class="profile-avatar">{{ initials }}</div>
        <h2>{{ profile.fullName }}</h2>
        <p class="profile-cnic">{{ profile.cnic }}</p>
        <div class="profile-status" :class="profile.status?.toLowerCase()">{{ profile.status }}</div>
      </div>

      <div class="info-card">
        <h3>Account Details</h3>
        <div class="info-row"><span>Account Number</span><strong>{{ profile.accountNumber }}</strong></div>
        <div class="info-row"><span>Balance</span><strong>PKR {{ formatAmount(profile.balance) }}</strong></div>
        <div class="info-row"><span>Currency</span><strong>{{ profile.currency ?? 'PKR' }}</strong></div>
        <div class="info-row"><span>Member Since</span><strong>{{ formatDate(profile.createdAt) }}</strong></div>
      </div>

      <div v-if="loading" class="loading-state">
        <LoadingSpinner message="Loading profile..." />
      </div>
      <AlertMessage v-if="error" :message="error" type="error" @dismiss="error = ''" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import NavBar from '@/components/NavBar.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import AlertMessage from '@/components/AlertMessage.vue'

const router = useRouter()
const loading = ref(true)
const error = ref('')
const profile = ref({
  fullName: '',
  cnic: '',
  status: '',
  accountNumber: '',
  balance: 0,
  currency: 'PKR',
  createdAt: null
})

const initials = computed(() =>
  profile.value.fullName?.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) || '?'
)

function formatAmount(n) {
  return Number(n ?? 0).toLocaleString('en-PK', { minimumFractionDigits: 2 })
}

function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('en-PK', { day: 'numeric', month: 'long', year: 'numeric' }) : ''
}

function handleLogout() {
  localStorage.removeItem('jwt_token')
  router.push('/login')
}

onMounted(async () => {
  try {
    const res = await api.get('/profile')
    profile.value = { ...profile.value, ...res.data }
  } catch (err) {
    error.value = err.response?.data?.message ?? 'Failed to load profile'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.profile-page { min-height: 100vh; background: #f0f2f5; }
.profile-content { max-width: 600px; margin: 0 auto; padding: 2rem 1rem; display: flex; flex-direction: column; gap: 1.5rem; }
.profile-card { background: linear-gradient(135deg, #1a237e, #1565c0); color: white; border-radius: 16px; padding: 2.5rem 2rem; text-align: center; box-shadow: 0 8px 24px rgba(26,35,126,0.3); }
.profile-avatar { width: 80px; height: 80px; border-radius: 50%; background: rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center; font-size: 1.75rem; font-weight: 700; margin: 0 auto 1rem; }
.profile-card h2 { font-size: 1.4rem; margin-bottom: 0.25rem; }
.profile-cnic { opacity: 0.8; font-size: 0.9rem; letter-spacing: 0.05rem; }
.profile-status { display: inline-block; margin-top: 0.75rem; padding: 0.25rem 1rem; border-radius: 20px; font-size: 0.85rem; font-weight: 600; background: rgba(255,255,255,0.2); }
.profile-status.active { background: rgba(76, 175, 80, 0.3); }
.profile-status.inactive { background: rgba(244, 67, 54, 0.3); }
.info-card { background: white; border-radius: 12px; padding: 1.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.info-card h3 { color: #1a237e; margin-bottom: 1rem; font-size: 1rem; }
.info-row { display: flex; justify-content: space-between; align-items: center; padding: 0.75rem 0; border-bottom: 1px solid #f0f0f0; font-size: 0.9rem; }
.info-row:last-child { border-bottom: none; }
.info-row span { color: #666; }
.info-row strong { color: #333; }
.loading-state { text-align: center; }
</style>
