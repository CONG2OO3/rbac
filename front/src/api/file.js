import request from './request'

export function listFiles(params) {
  return request.get('/system/file/list', { params })
}

export function uploadFile(fileType, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/system/file/upload', formData, {
    params: { fileType },
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: fileType === 'video' ? 600000 : 120000,
  })
}

export function deleteFile(id) {
  return request.delete(`/system/file/${id}`)
}

export function fetchFileContent(id, download = false) {
  return request.get(`/system/file/${id}/content`, {
    params: { download },
    responseType: 'blob',
  })
}
