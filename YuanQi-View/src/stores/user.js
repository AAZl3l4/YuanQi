import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserInfo, logout } from '@/api/user'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  async function fetchUserInfo() {
    loading.value = true
    try {
      const res = await getUserInfo()
      if (res.code === 200) {
        user.value = res.data
      } else {
        throw new Error(res.message)
      }
    } catch (error) {
      user.value = null
      throw error
    } finally {
      loading.value = false
    }
  }

  async function logoutUser() {
    try {
      await logout()
    } catch (error) {
      console.error('Logout error:', error)
    } finally {
      user.value = null
      router.push('/login')
    }
  }

  function updateUser(userData) {
    user.value = { ...user.value, ...userData }
  }

  return {
    user,
    loading,
    isLoggedIn,
    isAdmin,
    fetchUserInfo,
    logoutUser,
    updateUser
  }
})
