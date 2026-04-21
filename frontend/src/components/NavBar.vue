<template>
  <nav class="navbar">
    <div class="nav-brand">
      <span class="brand-icon">💳</span>
      <span class="brand-name">FinTech CNIC</span>
    </div>
    <div class="nav-links">
      <RouterLink to="/dashboard" class="nav-link">Dashboard</RouterLink>
      <RouterLink to="/send-money" class="nav-link">Send Money</RouterLink>
      <RouterLink to="/history" class="nav-link">History</RouterLink>
      <RouterLink to="/profile" class="nav-link">Profile</RouterLink>
      <button class="btn-logout" @click="$emit('logout')">Logout</button>
    </div>
  </nav>
</template>

<script setup>
import { RouterLink } from 'vue-router'
defineEmits(['logout'])
</script>

<style scoped>
.navbar {
  background: linear-gradient(135deg, #1a237e, #283593);
  color: white;
  padding: 0 2rem;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
  position: sticky;
  top: 0;
  z-index: 100;
}
.nav-brand { display: flex; align-items: center; gap: 0.5rem; font-size: 1.2rem; font-weight: 700; }
.brand-icon { font-size: 1.4rem; }
.nav-links { display: flex; align-items: center; gap: 1.5rem; }
.nav-link { color: rgba(255,255,255,0.85); text-decoration: none; font-size: 0.95rem; transition: color 0.2s; }
.nav-link:hover, .nav-link.router-link-active { color: #fff; border-bottom: 2px solid #42a5f5; padding-bottom: 2px; }
.btn-logout { background: rgba(255,255,255,0.15); color: white; border: 1px solid rgba(255,255,255,0.3); padding: 0.4rem 1rem; border-radius: 6px; cursor: pointer; font-size: 0.9rem; transition: background 0.2s; }
.btn-logout:hover { background: rgba(255,255,255,0.25); }
</style>
