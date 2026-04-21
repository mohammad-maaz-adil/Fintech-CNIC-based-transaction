<template>
  <div class="spinner-wrapper" :class="{ overlay: overlay }">
    <div class="spinner"></div>
    <p v-if="message" class="spinner-msg">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  message: { type: String, default: '' },
  overlay: { type: Boolean, default: false }
})
</script>

<style scoped>
.spinner-wrapper { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 2rem; }
.spinner-wrapper.overlay { position: fixed; inset: 0; background: rgba(255,255,255,0.8); z-index: 999; }
.spinner { width: 48px; height: 48px; border: 4px solid #e3e3e3; border-top-color: #1a237e; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.spinner-msg { margin-top: 1rem; color: #555; font-size: 0.95rem; }
</style>
