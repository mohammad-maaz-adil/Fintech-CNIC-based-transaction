<template>
  <div v-if="message" class="alert" :class="`alert-${type}`" role="alert">
    <span class="alert-icon">{{ icons[type] }}</span>
    <span>{{ message }}</span>
    <button v-if="dismissible" class="alert-close" @click="$emit('dismiss')">✕</button>
  </div>
</template>

<script setup>
defineProps({
  message: { type: String, default: '' },
  type: { type: String, default: 'error', validator: v => ['error', 'success', 'info', 'warning'].includes(v) },
  dismissible: { type: Boolean, default: true }
})
defineEmits(['dismiss'])
const icons = { error: '❌', success: '✅', info: 'ℹ️', warning: '⚠️' }
</script>

<style scoped>
.alert { display: flex; align-items: center; gap: 0.75rem; padding: 0.85rem 1.25rem; border-radius: 8px; margin-bottom: 1rem; font-size: 0.95rem; }
.alert-error { background: #ffebee; color: #c62828; border: 1px solid #ef9a9a; }
.alert-success { background: #e8f5e9; color: #2e7d32; border: 1px solid #a5d6a7; }
.alert-info { background: #e3f2fd; color: #1565c0; border: 1px solid #90caf9; }
.alert-warning { background: #fff8e1; color: #f57f17; border: 1px solid #ffe082; }
.alert-close { margin-left: auto; background: none; border: none; cursor: pointer; font-size: 1rem; opacity: 0.6; }
.alert-close:hover { opacity: 1; }
</style>
