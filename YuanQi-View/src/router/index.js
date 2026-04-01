import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/RegisterView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/chat'
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/chat/ChatView.vue')
      },
      {
        path: 'chat/:sessionId',
        name: 'ChatSession',
        component: () => import('@/views/chat/ChatView.vue')
      },
      {
        path: 'history',
        name: 'History',
        component: () => import('@/views/history/HistoryView.vue')
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/KnowledgeView.vue')
      },
      {
        path: 'agent',
        name: 'AgentSquare',
        component: () => import('@/views/agent/AgentSquareView.vue')
      },
      {
        path: 'agent/:id',
        name: 'AgentChat',
        component: () => import('@/views/agent/AgentChatView.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/ProfileView.vue')
      },
      {
        path: 'content',
        name: 'Content',
        component: () => import('@/views/content/ContentView.vue')
      },
      {
        path: 'usage',
        name: 'Usage',
        component: () => import('@/views/usage/UsageView.vue')
      },
      {
        path: 'api-key',
        name: 'ApiKey',
        component: () => import('@/views/api-key/ApiKeyView.vue')
      },
      {
        path: 'relay-config',
        name: 'RelayConfig',
        component: () => import('@/views/relay-config/RelayConfigView.vue')
      },
      {
        path: 'admin/user',
        name: 'AdminUser',
        component: () => import('@/views/admin/UserManageView.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/knowledge',
        name: 'AdminKnowledge',
        component: () => import('@/views/admin/KnowledgeManageView.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/agent',
        name: 'AdminAgent',
        component: () => import('@/views/admin/AgentManageView.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/mcp',
        name: 'AdminMcp',
        component: () => import('@/views/admin/McpManageView.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/content',
        name: 'AdminContent',
        component: () => import('@/views/admin/ContentManageView.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/api-key',
        name: 'AdminApiKey',
        component: () => import('@/views/admin/ApiKeyManageView.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/relay-logs',
        name: 'AdminRelayLogs',
        component: () => import('@/views/admin/RelayLogView.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/relay-config',
        name: 'AdminRelayConfig',
        component: () => import('@/views/admin/RelayConfigManageView.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/usage',
        name: 'AdminUsage',
        component: () => import('@/views/admin/UsageManageView.vue'),
        meta: { requiresAdmin: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth === false) {
    return
  }
  
  if (!userStore.isLoggedIn) {
    try {
      await userStore.fetchUserInfo()
    } catch (error) {
      return '/login'
    }
  }
  
  if (to.meta.requiresAdmin && userStore.user?.role !== 'admin') {
    return '/chat'
  }
})

export default router
