import request from './request'

export function getServerSnapshot() {
  return request.get('/system/monitor/server')
}

export function listOnlineUsers(params) {
  return request.get('/system/monitor/online', { params })
}

export function kickoutOnlineUser(sessionId) {
  return request.delete(`/system/monitor/online/${sessionId}`)
}
