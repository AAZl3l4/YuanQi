import request from '@/utils/request'

export function getMyContent(params) {
  return request.get('/api/content/my', { params })
}

export function deleteMyContent(id) {
  return request.delete(`/api/content/my/${id}`)
}

export function getContentList(params) {
  return request.get('/api/content/list', { params })
}

export function deleteContentAdmin(id) {
  return request.delete(`/api/content/${id}`)
}

export function generateImage(data) {
  return request.post('/api/message/image', data)
}

export function submitVideoTask(data) {
  return request.post('/api/message/video', data)
}

export function queryVideoTask(taskId) {
  return request.get(`/api/message/video/${taskId}`)
}
