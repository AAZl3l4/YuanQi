import request from '@/utils/request'

/**
 * 调用中转接口
 */
export function callRelay(apiKey, data) {
  return request.post('/api/relay/call', data, {
    headers: { 'X-API-Key': apiKey }
  })
}

/**
 * 获取我的中转配置列表
 */
export function getRelayConfigList(params) {
  return request.get('/api/api-relay/config/list', { params })
}

/**
 * 获取中转配置详情
 */
export function getRelayConfig(id) {
  return request.get(`/api/api-relay/config/${id}`)
}

/**
 * 创建中转配置
 */
export function createRelayConfig(data) {
  return request.post('/api/api-relay/config/create', data)
}

/**
 * 更新中转配置
 */
export function updateRelayConfig(data) {
  return request.put('/api/api-relay/config/update', data)
}

/**
 * 删除中转配置
 */
export function deleteRelayConfig(id) {
  return request.delete(`/api/api-relay/config/my/${id}`)
}

/**
 * 管理员获取中转配置列表
 */
export function getAdminRelayConfigList(params) {
  return request.get('/api/api-relay/config/admin/list', { params })
}

/**
 * 管理员删除中转配置
 */
export function deleteRelayConfigAdmin(id) {
  return request.delete(`/api/api-relay/config/admin/${id}`)
}

/**
 * 获取我的调用记录
 */
export function getMyRelayLogs(params) {
  return request.get('/api/relay/my/logs', { params })
}

/**
 * 管理员获取调用记录
 */
export function getAdminRelayLogs(params) {
  return request.get('/api/relay/admin/logs', { params })
}
