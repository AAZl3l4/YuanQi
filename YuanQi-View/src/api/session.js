import request from '@/utils/request'

export function createSession(data) {
  const params = {}
  if (data && data.agentId) {
    params.agentId = data.agentId
  }
  return request.post('/api/session', null, { params })
}

export function getSessionList(params) {
  return request.get('/api/session/list', { params })
}

export function updateSessionTitle(sessionId, data) {
  return request.put(`/api/session/${sessionId}/title`, data)
}

export function deleteSession(sessionId) {
  return request.delete(`/api/session/${sessionId}`)
}
