<script setup lang="ts">
import HeaderSection from './components/HeaderSection.vue'
import SearchSection from './components/SearchSection.vue'
import LiteratureSection from './components/LiteratureSection.vue'
import SummarySection from './components/SummarySection.vue'
</script>

<template>
  <div class="app">
    <HeaderSection />
    <main class="main-content container">
      <SearchSection />
      <LiteratureSection />
      <SummarySection />
    </main>
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  background-color: var(--color-bg);
}

.main-content {
  padding-top: 24px;
  padding-bottom: 48px;
}
</style>
