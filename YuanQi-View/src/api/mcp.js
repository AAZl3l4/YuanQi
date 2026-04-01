import request from '@/utils/request'

export function getEnabledTools() {
  return request.get('/api/mcp/tools')
}

export function getToolList() {
  return request.get('/api/mcp/list')
}

export function createTool(data) {
  return request.post('/api/mcp', data)
}

export function updateToolStatus(id, enabled) {
  return request.put(`/api/mcp/${id}`, null, { params: { enabled } })
}
