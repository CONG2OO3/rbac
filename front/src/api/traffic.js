import request from './request'

export function recordVisit() {
  return request.post('/dashboard/visit')
}

export function getTraffic(range) {
  return request.get('/dashboard/traffic', { params: { range } })
}
