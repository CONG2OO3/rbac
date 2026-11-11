import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 12000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body?.code === 401) {
      localStorage.removeItem('access_token')
      if (window.location.pathname !== '/login') window.location.href = '/login'
      return Promise.reject(new Error(body.message || '登录状态已失效'))
    }
    if (body && body.code !== undefined && body.code !== 200) return Promise.reject(new Error(body.message || '请求失败'))
    return body && body.data !== undefined ? body.data : body
  },
  (error) => {
    if (error.response?.status === 401) localStorage.removeItem('access_token')
    return Promise.reject(error)
  },
)

export default request
