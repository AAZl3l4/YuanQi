import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '',
  timeout: 60000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 401) {
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
      return Promise.reject(new Error('未登录'))
    }
    if (res.code !== 200 && res.code !== undefined) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
