<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'logout':
      userStore.logoutUser()
      break
  }
}
</script>

<template>
  <header class="header">
    <div class="header-left">
      <h1 class="page-title">
        <slot name="title">元启 AI</slot>
      </h1>
    </div>
    
    <div class="header-right">
      <el-dropdown @command="handleCommand">
        <div class="user-info">
          <el-avatar
            :size="36"
            :src="userStore.user?.avatar"
            class="user-avatar"
          >
            {{ userStore.user?.username?.charAt(0) }}
          </el-avatar>
          <span class="username">{{ userStore.user?.username }}</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              个人中心
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped>
.header {
  height: var(--header-height);
  background: var(--color-bg-secondary);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--color-glass-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--spacing-lg);
  box-shadow: 0 1px 10px rgba(0, 0, 0, 0.02);
  position: sticky;
  top: 0;
  z-index: 50;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: var(--font-size-xl);
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--color-text);
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  cursor: pointer;
  padding: 6px 10px;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  background: transparent;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.6);
  box-shadow: 0 2px 5px rgba(0,0,0,0.02);
}

.user-avatar {
  background: linear-gradient(135deg, var(--color-primary-light) 0%, var(--color-primary) 100%);
  color: #fff;
  box-shadow: var(--shadow-sm);
  border: 1px solid rgba(255,255,255,0.4);
}

.username {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-text);
}
</style>
