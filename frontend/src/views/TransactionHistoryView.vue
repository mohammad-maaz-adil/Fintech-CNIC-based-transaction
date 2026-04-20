<template>
  <div class="history-page">
    <NavBar @logout="handleLogout" />

    <div class="history-content">
      <div class="history-header">
        <h1>Transaction History</h1>
        <button class="btn-refresh" @click="send({ type: 'REFRESH' })">🔄 Refresh</button>
      </div>

      <!-- Loading state from historyMachine -->
      <LoadingSpinner v-if="snapshot.matches('fetching')" message="Loading transactions..." />

      <!-- Error state -->
      <div v-else-if="snapshot.matches('error')" class="state-box error-box">
        <AlertMessage :message="snapshot.context.error" type="error" :dismissible="false" />
        <button class="btn-retry" @click="send({ type: 'RETRY' })">Try Again</button>
      </div>

      <!-- Empty state -->
      <div v-else-if="snapshot.matches('empty')" class="state-box empty-box">
        <span class="empty-icon">📋</span>
        <p>No transactions found</p>
      </div>

      <!-- Success state -->
      <div v-else-if="snapshot.matches('success')" class="txn-list-container">
        <div class="txn-card" v-for="txn in snapshot.context.transactions" :key="txn.id">
          <div class="txn-icon">{{ getTxnIcon(txn.type) }}</div>
          <div class="txn-info">
            <div class="txn-title">{{ txn.description }}</div>
            <div class="txn-meta">{{ formatDate(txn.createdAt) }} · {{ txn.status }}</div>
            <div class="txn-ref">Ref: {{ txn.referenceNumber }}</div>
          </div>
          <div class="txn-amount" :class="txn.type">
            {{ txn.type === 'credit' ? '+' : '-' }}PKR {{ formatAmount(txn.amount) }}
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="snapshot.context.totalPages > 1" class="pagination">
          <button
            v-for="p in snapshot.context.totalPages"
            :key="p"
            @click="send({ type: 'LOAD_PAGE', page: p - 1 })"
            :class="{ active: snapshot.context.currentPage === p - 1 }"
          >{{ p }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useMachine } from '@xstate/vue'
import { useRouter } from 'vue-router'
import { historyMachine } from '@/machines/historyMachine'
import NavBar from '@/components/NavBar.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import AlertMessage from '@/components/AlertMessage.vue'

const router = useRouter()
const { snapshot, send } = useMachine(historyMachine)

function handleLogout() {
  localStorage.removeItem('jwt_token')
  router.push('/login')
}

function getTxnIcon(type) {
  return type === 'credit' ? '⬆️' : '⬇️'
}

function formatAmount(n) {
  return Number(n ?? 0).toLocaleString('en-PK', { minimumFractionDigits: 2 })
}

function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('en-PK', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''
}
</script>

<style scoped>
.history-page { min-height: 100vh; background: #f0f2f5; }
.history-content { max-width: 760px; margin: 0 auto; padding: 2rem 1rem; }
.history-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.5rem; }
.history-header h1 { font-size: 1.5rem; color: #1a237e; }
.btn-refresh { background: white; border: 1px solid #ddd; padding: 0.5rem 1rem; border-radius: 8px; cursor: pointer; color: #1a237e; font-size: 0.9rem; }
.state-box { background: white; border-radius: 12px; padding: 3rem 2rem; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.empty-icon { font-size: 3rem; display: block; margin-bottom: 1rem; }
.btn-retry { background: #1a237e; color: white; border: none; padding: 0.75rem 2rem; border-radius: 8px; cursor: pointer; margin-top: 1rem; }
.txn-list-container { display: flex; flex-direction: column; gap: 0.75rem; }
.txn-card { background: white; border-radius: 12px; padding: 1.25rem; display: flex; align-items: center; gap: 1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.txn-icon { font-size: 1.5rem; }
.txn-info { flex: 1; }
.txn-title { font-weight: 500; color: #333; font-size: 0.95rem; }
.txn-meta { font-size: 0.8rem; color: #888; margin-top: 0.15rem; }
.txn-ref { font-size: 0.75rem; color: #aaa; font-family: monospace; }
.txn-amount { font-weight: 600; font-size: 1rem; }
.txn-amount.credit { color: #2e7d32; }
.txn-amount.debit { color: #c62828; }
.pagination { display: flex; gap: 0.5rem; justify-content: center; margin-top: 1.5rem; }
.pagination button { width: 36px; height: 36px; border-radius: 8px; border: 1px solid #ddd; background: white; cursor: pointer; font-size: 0.9rem; }
.pagination button.active { background: #1a237e; color: white; border-color: #1a237e; }
</style>
