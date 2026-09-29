import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import api, { fetchFavorites, addFavorite, removeFavorite } from '../api'

const AuthContext = createContext(null)

const TOKEN_KEY = 'gallae_token'
const FAV_KEY = 'gallae_favorites'

function readLocalFavorites() {
  try {
    return JSON.parse(localStorage.getItem(FAV_KEY) || '[]')
  } catch {
    return []
  }
}

function writeLocalFavorites(list) {
  localStorage.setItem(FAV_KEY, JSON.stringify(list))
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY))
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(!!localStorage.getItem(TOKEN_KEY))
  // 로그인 사용자는 서버(favorites 테이블), 비로그인 사용자는 localStorage를 사용한다.
  // 두 경우 모두 {from, to, label} 형태로 통일해서 컴포넌트가 로그인 여부를 신경 쓰지 않게 한다.
  const [favorites, setFavorites] = useState(() => readLocalFavorites())

  // 토큰 저장 & axios 헤더 동기화
  const saveToken = useCallback((t) => {
    if (t) {
      localStorage.setItem(TOKEN_KEY, t)
      api.defaults.headers.common['Authorization'] = `Bearer ${t}`
    } else {
      localStorage.removeItem(TOKEN_KEY)
      delete api.defaults.headers.common['Authorization']
    }
    setToken(t)
  }, [])

  // currentUser를 인자로 받아 로그인 상태에 따라 서버/localStorage 중 어느 쪽에서 즐겨찾기를 읽을지 결정한다.
  const refreshFavorites = useCallback(async (currentUser) => {
    if (currentUser) {
      try {
        const res = await fetchFavorites()
        setFavorites(res.data?.data ?? res.data ?? [])
      } catch {
        // 서버 조회 실패 시 빈 목록으로 — 로그인 세션 자체는 유지
        setFavorites([])
      }
    } else {
      setFavorites(readLocalFavorites())
    }
  }, [])

  // 앱 시작 시 토큰이 있으면 /api/auth/me로 유저 정보 가져오기
  useEffect(() => {
    const stored = localStorage.getItem(TOKEN_KEY)
    if (!stored) { setLoading(false); return }
    api.defaults.headers.common['Authorization'] = `Bearer ${stored}`
    api.get('/api/auth/me')
      .then((res) => {
        const me = res.data?.data ?? res.data
        setUser(me)
        return refreshFavorites(me)
      })
      .catch(() => saveToken(null))   // 토큰 만료 → 로그아웃
      .finally(() => setLoading(false))
  }, [saveToken, refreshFavorites])

  const login = useCallback((provider) => {
    // 백엔드 OAuth2 엔드포인트로 리다이렉트
    window.location.href = `${import.meta.env.VITE_API_BASE_URL || ''}/oauth2/authorization/${provider}`
  }, [])

  const loginWithToken = useCallback(async (newToken) => {
    saveToken(newToken)
    try {
      const res = await api.get('/api/auth/me')
      const me = res.data?.data ?? res.data
      setUser(me)
      await refreshFavorites(me)
    } catch {
      saveToken(null)
    }
  }, [saveToken, refreshFavorites])

  const logout = useCallback(() => {
    saveToken(null)
    setUser(null)
    refreshFavorites(null)
  }, [saveToken, refreshFavorites])

  const isFavoritedRoute = useCallback((from, to) => {
    return favorites.some((f) => f.from === from && f.to === to)
  }, [favorites])

  const addFavoriteRoute = useCallback(async (from, to, label) => {
    if (user) {
      try {
        await addFavorite({ from, to, label })
        await refreshFavorites(user)
        return true
      } catch {
        return false
      }
    }
    const list = readLocalFavorites()
    if (!list.some((f) => f.from === from && f.to === to)) {
      list.unshift({ from, to, label: label || `${from} → ${to}` })
      writeLocalFavorites(list)
      setFavorites(list)
    }
    return true
  }, [user, refreshFavorites])

  const removeFavoriteRoute = useCallback(async (from, to) => {
    if (user) {
      try {
        await removeFavorite({ from, to })
        await refreshFavorites(user)
        return true
      } catch {
        return false
      }
    }
    const list = readLocalFavorites().filter((f) => !(f.from === from && f.to === to))
    writeLocalFavorites(list)
    setFavorites(list)
    return true
  }, [user, refreshFavorites])

  const toggleFavoriteRoute = useCallback(async (from, to, label) => {
    if (isFavoritedRoute(from, to)) {
      await removeFavoriteRoute(from, to)
      return false
    }
    await addFavoriteRoute(from, to, label)
    return true
  }, [isFavoritedRoute, addFavoriteRoute, removeFavoriteRoute])

  return (
    <AuthContext.Provider value={{
      token, user, loading, login, loginWithToken, logout,
      favorites,
      refreshFavorites: () => refreshFavorites(user),
      isFavoritedRoute, addFavoriteRoute, removeFavoriteRoute, toggleFavoriteRoute,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
