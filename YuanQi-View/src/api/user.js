import request from '@/utils/request'

export function sendCode(email) {
  return request.post('/api/user/send-code', null, {
    params: { email }
  })
}

export function register(data) {
  return request.post('/api/user/register', data)
}

export function login(data) {
  return request.post('/api/user/login', data)
}

export function getUserInfo() {
  return request.get('/api/user/info')
}

export function updateUserInfo(data) {
  return request.put('/api/user/updata', data)
}

export function logout() {
  return request.post('/api/user/logout')
}

export function getUserList(params) {
  return request.get('/api/user/list', { params })
}

export function updateUser(userId, data) {
  return request.put(`/api/user/${userId}`, data)
}

export function deleteUser(userId) {
  return request.delete(`/api/user/${userId}`)
}

export function getOnlineUsers() {
  return request.get('/api/user/online')
}

export function kickoutUser(userId) {
  return request.post(`/api/user/kickout/${userId}`)
}

export function sendEmailToUser(userId, subject, content) {
  return request.post(`/api/user/send-email/${userId}`, null, {
    params: { subject, content }
  })
}
