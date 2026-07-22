<script setup lang="ts">
</script>

<template>
  <header class="header">
    <div class="header-inner container">
      <div class="header-brand">
        <div class="header-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"/>
            <line x1="8" y1="7" x2="16" y2="7"/>
            <line x1="8" y1="11" x2="14" y2="11"/>
          </svg>
        </div>
        <div class="header-text">
          <h1 class="header-title">MedSearch</h1>
          <p class="header-subtitle">PubMed医学文献AI查询系统</p>
        </div>
      </div>
      <div class="header-badge">
        <span class="badge-dot"></span>
        <span class="badge-text">AI Powered</span>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  background: var(--color-white);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(12px);
  background: rgba(255, 255, 255, 0.92);
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #0052FF, #4D7CFF);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.header-text {
  display: flex;
  flex-direction: column;
}

.header-title {
  font-family: 'Plus Jakarta Sans', 'PingFang SC', 'Microsoft YaHei UI', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-secondary);
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.header-subtitle {
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.4;
  margin-top: 1px;
}

.header-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 20px;
  background: #F0F4FF;
  border: 1px solid #DBEAFE;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #0052FF;
  animation: pulse-dot 2s ease-in-out infinite;
}

.badge-text {
  font-size: 12px;
  font-weight: 600;
  color: #0052FF;
  letter-spacing: 0.02em;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.3); }
}

@media (max-width: 768px) {
  .header-inner {
    height: 56px;
  }
  .header-title {
    font-size: 17px;
  }
  .header-badge {
    display: none;
  }
}
</style>
