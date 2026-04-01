<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSessionStore } from '@/stores/session'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const sessionStore = useSessionStore()

const sidebarCollapsed = ref(false)

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

onMounted(() => {
  sessionStore.fetchSessions(true)
})
</script>

<template>
  <div class="app-layout" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <Sidebar :collapsed="sidebarCollapsed" @toggle="toggleSidebar" />
    <div class="main-container">
      <Header />
      <main class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  width: 100%;
  height: 100vh;
  background: transparent;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: var(--sidebar-width);
  transition: margin-left var(--transition-normal) cubic-bezier(0.16, 1, 0.3, 1);
  min-width: 0;
}

.sidebar-collapsed .main-container {
  margin-left: 64px;
}

.content-area {
  flex: 1;
  overflow: auto;
  padding: var(--spacing-lg);
  scroll-behavior: smooth;
}
</style>
