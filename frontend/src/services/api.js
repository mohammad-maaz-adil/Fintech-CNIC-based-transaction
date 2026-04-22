import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api'
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  } else if (config.headers?.Authorization) {
    delete config.headers.Authorization
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    const url = error?.config?.url || ''
    const isAuthRoute = url.includes('/auth/login') || url.includes('/auth/signup')

    if (status === 401 && !isAuthRoute) {
      localStorage.removeItem('jwt_token')
      if (window.location.pathname !== '/login') {
        window.location.replace('/login')
      }
    }
    return Promise.reject(error)
  }
)

export default api