import request from './request'

const resourcePaths = {
  users: 'user',
  roles: 'role',
  menus: 'menu',
  departments: 'dept',
  positions: 'post',
}

export function listResource(resource, params) {
  return request.get(`/system/${resourcePaths[resource]}/list`, { params })
}

export function createResource(resource, data) {
  return request.post(`/system/${resourcePaths[resource]}`, data)
}

export function updateResource(resource, data) {
  return request.put(`/system/${resourcePaths[resource]}`, data)
}

export function deleteResource(resource, id) {
  return request.delete(`/system/${resourcePaths[resource]}/${id}`)
}

export function getUserRoles(userId) {
  return request.get(`/system/user/${userId}/roles`)
}

export function setUserRoles(userId, roleIds) {
  return request.put(`/system/user/${userId}/roles`, roleIds)
}

export function getRoleMenus(roleId) {
  return request.get(`/system/role/${roleId}/menus`)
}

export function setRoleMenus(roleId, menuIds) {
  return request.put(`/system/role/${roleId}/menus`, menuIds)
}

export function exportResource(resource) {
  return request.post(`/system/excel/${resource}/export`, null, {
    responseType: 'blob',
  })
}

export function importResource(resource, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/system/excel/${resource}/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
