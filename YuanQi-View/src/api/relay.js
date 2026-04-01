import request from '@/utils/request'

export function createRelayConfig(data) {
  return request.post('/api/api-relay/config/create', data)
}

export function updateRelayConfig(data) {
  return request.put('/api/api-relay/config/update', data)
}

export function deleteRelayConfig(id) {
  return request.delete(`/api/api-relay/config/my/${id}`)
}

export function getRelayConfig(id) {
  return request.get(`/api/api-relay/config/${id}`)
}

export function getRelayConfigList(params) {
  return request.get('/api/api-relay/config/list', { params })
}

export function getAdminRelayConfigList(params) {
  return request.get('/api/api-relay/config/admin/list', { params })
}

export function deleteRelayConfigAdmin(id) {
  return request.delete(`/api/api-relay/config/admin/${id}`)
}

export function relayCall(data) {
  return request.post('/api/relay/call', data)
}

export function getMyRelayLogs(params) {
  return request.get('/api/relay/my/logs', { params })
}

export function getAdminRelayLogs(params) {
  return request.get('/api/relay/admin/logs', { params })
}
