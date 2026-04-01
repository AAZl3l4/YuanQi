import request from '@/utils/request'

export function createKnowledge(data) {
  return request.post('/api/knowledge/create', data)
}

export function updateKnowledge(data) {
  return request.put('/api/knowledge/update', data)
}

export function deleteKnowledge(id) {
  return request.delete(`/api/knowledge/my/${id}`)
}

export function getKnowledge(id) {
  return request.get(`/api/knowledge/${id}`)
}

export function getMyKnowledgeList(params) {
  return request.get('/api/knowledge/my', { params })
}

export function getKnowledgeList(params) {
  return request.get('/api/knowledge/list', { params })
}

export function deleteKnowledgeAdmin(id) {
  return request.delete(`/api/knowledge/${id}`)
}
