<template>
  <div class="dashboard-page">
    <NavBar @logout="handleLogout" />

    <div class="dashboard-content">
      <!-- Loading state from dashboardMachine -->
      <LoadingSpinner v-if="isLoading" message="Loading your dashboard..." />

      <!-- Error state -->
      <div v-else-if="isError" class="error-state">
        <AlertMessage :message="snapshot.context.error" type="error" :dismissible="false" />
        <button class="btn-retry" @click="send({ type: 'RETRY' })">Try Again</button>
      </div>

      <!-- Loaded / Refreshing states -->
      <template v-else>
        <div v-if="isRefreshing" class="refresh-banner">
          <span class="refresh-spin">🔄</span> Refreshing...
        </div>

        <!-- Balance Card -->
        <div class="balance-card">
          <div class="balance-label">Available Balance</div>
          <div class="balance-amount">PKR {{ formatAmount(snapshot.context.balance) }}</div>
          <div class="account-no">Account: {{ snapshot.context.accountNumber }}</div>
          <div class="welcome-name">Welcome, {{ snapshot.context.fullName }}</div>
        </div>

        <!-- Quick Actions -->
        <div class="quick-actions">
          <RouterLink to="/send-money" class="action-card">
            <span class="action-icon">💸</span>
            <span>Send Money</span>
          </RouterLink>
          <RouterLink to="/history" class="action-card">
            <span class="action-icon">📋</span>
            <span>History</span>
          </RouterLink>
          <RouterLink to="/profile" class="action-card">
            <span class="action-icon">👤</span>
            <span>Profile</span>
          </RouterLink>
          <button class="action-card" @click="send({ type: 'REFRESH' })">
            <span class="action-icon">🔄</span>
            <span>Refresh</span>
          </button>
        </div>

        <!-- Recent Transactions -->
        <div class="section">
          <h2>Recent Transactions</h2>
          <div v-if="snapshot.context.recentTransactions.length === 0" class="empty-state">
            No recent transactions
          </div>
          <div v-else class="txn-list">
            <div
              v-for="txn in snapshot.context.recentTransactions"
              :key="txn.id"
              class="txn-item"
              :class="txn.type"
            >
              <div class="txn-icon">{{ txn.type === 'credit' ? '⬆️' : '⬇️' }}</div>
              <div class="txn-details">
                <div class="txn-desc">{{ txn.description }}</div>
                <div class="txn-date">{{ formatDate(txn.createdAt) }}</div>
              </div>
              <div class="txn-amount" :class="txn.type">
                {{ txn.type === 'credit' ? '+' : '-' }}PKR {{ formatAmount(txn.amount) }}
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useMachine } from '@xstate/vue'
import { useRouter, RouterLink } from 'vue-router'
import { dashboardMachine } from '@/machines/dashboardMachine'
import NavBar from '@/components/NavBar.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import AlertMessage from '@/components/AlertMessage.vue'

const router = useRouter()
const { snapshot, send } = useMachine(dashboardMachine)

const isLoading = computed(() => snapshot.value.matches('loading'))
const isError = computed(() => snapshot.value.matches('error'))
const isRefreshing = computed(() => snapshot.value.matches('refreshing'))

function handleLogout() {
  localStorage.removeItem('jwt_token')
  router.push('/login')
}

function formatAmount(n) {
  return Number(n ?? 0).toLocaleString('en-PK', { minimumFractionDigits: 2 })
}

function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('en-PK', { day: 'numeric', month: 'short', year: 'numeric' }) : ''
}
</script>

<style scoped>
.dashboard-page { min-height: 100vh; background: #f0f2f5; }
.dashboard-content { max-width: 800px; margin: 0 auto; padding: 2rem 1rem; }
.balance-card { background: linear-gradient(135deg, #1a237e, #1565c0); color: white; border-radius: 16px; padding: 2rem; margin-bottom: 1.5rem; box-shadow: 0 8px 24px rgba(26,35,126,0.3); }
.balance-label { font-size: 0.9rem; opacity: 0.8; margin-bottom: 0.5rem; }
.balance-amount { font-size: 2.5rem; font-weight: 700; margin-bottom: 0.5rem; }
.account-no { font-size: 0.85rem; opacity: 0.7; }
.welcome-name { font-size: 0.95rem; margin-top: 0.75rem; }
.quick-actions { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 1.5rem; }
.action-card { background: white; border-radius: 12px; padding: 1.25rem 1rem; text-align: center; text-decoration: none; color: #333; display: flex; flex-direction: column; align-items: center; gap: 0.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); cursor: pointer; border: none; font-size: 0.9rem; transition: transform 0.15s, box-shadow 0.15s; }
.action-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.12); }
.action-icon { font-size: 1.75rem; }
.section { background: white; border-radius: 12px; padding: 1.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.section h2 { font-size: 1.1rem; color: #333; margin-bottom: 1rem; }
.empty-state { color: #999; text-align: center; padding: 2rem; }
.txn-list { display: flex; flex-direction: column; gap: 0.75rem; }
.txn-item { display: flex; align-items: center; gap: 1rem; padding: 0.75rem; border-radius: 8px; background: #f9f9f9; }
.txn-icon { font-size: 1.25rem; }
.txn-details { flex: 1; }
.txn-desc { font-size: 0.9rem; font-weight: 500; color: #333; }
.txn-date { font-size: 0.8rem; color: #999; margin-top: 0.15rem; }
.txn-amount { font-weight: 600; font-size: 0.95rem; }
.txn-amount.credit { color: #2e7d32; }
.txn-amount.debit { color: #c62828; }
.error-state { text-align: center; padding: 2rem; }
.btn-retry { background: #1a237e; color: white; border: none; padding: 0.75rem 2rem; border-radius: 8px; cursor: pointer; font-size: 0.95rem; }
.refresh-banner { background: #e3f2fd; color: #1565c0; padding: 0.6rem 1rem; border-radius: 8px; margin-bottom: 1rem; display: flex; align-items: center; gap: 0.5rem; }
.refresh-spin { animation: spin 1s linear infinite; display: inline-block; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 600px) { .quick-actions { grid-template-columns: repeat(2, 1fr); } .balance-amount { font-size: 1.8rem; } }
</style>
