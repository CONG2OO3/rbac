import request from './request'

export function listLogs(type, params) {
  return request.get(`/system/log/${type}/list`, { params })
}

export function deleteLogs(type, ids) {
  return request.delete(`/system/log/${type}/${ids.join(',')}`)
}

export function clearLogs(type) {
  return request.delete(`/system/log/${type}/clear`)
}
