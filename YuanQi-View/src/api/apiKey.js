import request from '@/utils/request'

export function createApiKey(data) {
  return request.post('/api/api-key/create', data)
}

export function getMyApiKeys(params) {
  return request.get('/api/api-key/my', { params })
}

export function deleteMyApiKey(id) {
  return request.delete(`/api/api-key/my/${id}`)
}

export function getAdminApiKeyList(params) {
  return request.get('/api/api-key/admin/list', { params })
}

export function deleteApiKeyAdmin(id) {
  return request.delete(`/api/api-key/admin/${id}`)
}
