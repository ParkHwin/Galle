import axios from 'axios'

const TOKEN_KEY = 'gallae_token'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
  timeout: 30000,
})

// 요청 인터셉터 — localStorage에서 토큰 읽어 Authorization 헤더 주입
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

// 응답 인터셉터 — 401이면 토큰 삭제 (만료 처리)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      delete api.defaults.headers.common['Authorization']
    }
    return Promise.reject(error)
  }
)

export const searchRoutes = (params) =>
  api.get('/api/search/routes', { params })

export const fetchBusCities = () => api.get('/api/bus/cities')
export const fetchBusTerminals = (params) => api.get('/api/bus/terminals', { params })
export const fetchCarDirections = (params) => api.get('/api/car/directions', { params })
export const fetchHealth = () => api.get('/api/health')
export const fetchMe = () => api.get('/api/auth/me')

// 즐겨찾기 — 로그인 사용자 전용 (백엔드 저장). 비로그인 사용자는 AuthContext에서 localStorage로 처리한다.
export const fetchFavorites = () => api.get('/api/favorites')
export const addFavorite = ({ from, to, label }) => api.post('/api/favorites', { from, to, label })
export const removeFavorite = ({ from, to }) => api.delete('/api/favorites', { params: { from, to } })

export default api
