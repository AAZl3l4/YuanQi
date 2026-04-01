import request from '@/utils/request'

export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request.post('/api/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getFileUrl(filename) {
  return `/api/file/${filename}`
}

export function getDownloadUrl(filename) {
  return `/api/file/download/${filename}`
}
