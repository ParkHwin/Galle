import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
  timeout: 30000,
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('[API Error]', error?.response?.data || error.message)
    return Promise.reject(error)
  }
)

export const searchRoutes = (params) =>
  api.get('/api/search/routes', { params })

export const fetchBusCities = () => api.get('/api/bus/cities')
export const fetchBusTerminals = (params) => api.get('/api/bus/terminals', { params })
export const fetchCarDirections = (params) => api.get('/api/car/directions', { params })
export const fetchHealth = () => api.get('/api/health')

export default api
