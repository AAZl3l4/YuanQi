import request from '@/utils/request'

export function createAgent(data) {
  return request.post('/api/agent/create', data)
}

export function updateAgent(data) {
  return request.put('/api/agent/update', data)
}

export function deleteAgent(id) {
  return request.delete(`/api/agent/my/${id}`)
}

export function getAgent(id) {
  return request.get(`/api/agent/${id}`)
}

export function getAgentList(params) {
  return request.get('/api/agent/list', { params })
}

export function getAdminAgentList(params) {
  return request.get('/api/agent/admin/list', { params })
}

export function deleteAgentAdmin(id) {
  return request.delete(`/api/agent/admin/${id}`)
}
