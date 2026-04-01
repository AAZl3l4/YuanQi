import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getSessionList, createSession, deleteSession, updateSessionTitle } from '@/api/session'

export const useSessionStore = defineStore('session', () => {
  const sessions = ref([])
  const currentSession = ref(null)
  const loading = ref(false)
  const total = ref(0)
  const page = ref(1)
  const size = ref(20)

  const hasMore = computed(() => sessions.value.length < total.value)

  async function fetchSessions(reset = false) {
    if (reset) {
      page.value = 1
      sessions.value = []
    }
    
    loading.value = true
    try {
      const res = await getSessionList({ page: page.value, size: size.value })
      if (res.code === 200) {
        if (reset) {
          sessions.value = res.data.records || []
        } else {
          sessions.value.push(...(res.data.records || []))
        }
        total.value = res.data.total
      }
    } catch (error) {
      console.error('Fetch sessions error:', error)
    } finally {
      loading.value = false
    }
  }

  async function createNewSession(agentId = null) {
    try {
      const res = await createSession({ agentId })
      if (res.code === 200) {
        const newSession = res.data
        sessions.value.unshift(newSession)
        currentSession.value = newSession
        return newSession
      }
      return null
    } catch (error) {
      console.error('Create session error:', error)
      return null
    }
  }

  async function removeSession(sessionId) {
    try {
      const res = await deleteSession(sessionId)
      if (res.code === 200) {
        sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
        if (currentSession.value?.sessionId === sessionId) {
          currentSession.value = sessions.value[0] || null
        }
        return true
      }
      return false
    } catch (error) {
      console.error('Delete session error:', error)
      return false
    }
  }

  async function renameSession(sessionId, title) {
    try {
      const res = await updateSessionTitle(sessionId, { sessionId, title })
      if (res.code === 200) {
        const session = sessions.value.find(s => s.sessionId === sessionId)
        if (session) {
          session.title = title
        }
        return true
      }
      return false
    } catch (error) {
      console.error('Rename session error:', error)
      return false
    }
  }

  function setCurrentSession(session) {
    currentSession.value = session
  }

  function loadMore() {
    if (hasMore.value && !loading.value) {
      page.value++
      fetchSessions()
    }
  }

  return {
    sessions,
    currentSession,
    loading,
    total,
    hasMore,
    fetchSessions,
    createNewSession,
    removeSession,
    renameSession,
    setCurrentSession,
    loadMore
  }
})
