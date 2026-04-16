import request from '@/utils/request'

// 获取图片验证码
export function getCaptcha() {
  return request.get('/api/captcha/get')
}

// 发送邮箱验证码（需要图片验证码）
export function sendCode(data) {
  return request.post('/api/user/send-code', data)
}

// 用户注册
export function register(data) {
  return request.post('/api/user/register', data)
}

// 用户登录
export function login(data) {
  return request.post('/api/user/login', data)
}

// 修改密码
export function changePassword(data) {
  return request.post('/api/user/change-password', data)
}

// 获取当前用户信息
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
