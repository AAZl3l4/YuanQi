import request from '@/utils/request'

// 获取赞助榜单（公开）
export function getSponsorList() {
  return request.get('/api/sponsor/list')
}

// 分页查询赞助记录（管理员）
export function getSponsorPage(params) {
  return request.get('/api/sponsor/page', { params })
}

// 新增赞助记录（管理员）
export function addSponsor(data) {
  return request.post('/api/sponsor', data)
}

// 修改赞助记录（管理员）
export function updateSponsor(id, data) {
  return request.put(`/api/sponsor/${id}`, data)
}

// 删除赞助记录（管理员）
export function deleteSponsor(id) {
  return request.delete(`/api/sponsor/${id}`)
}
