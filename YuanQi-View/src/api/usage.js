import request from '@/utils/request'

export function getMyUsage(params) {
  return request.get('/api/usage/my', { params })
}

export function getUsageList(params) {
  return request.get('/api/usage/list', { params })
}
