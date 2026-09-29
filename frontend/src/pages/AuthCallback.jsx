import { useEffect, useRef } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import LoadingSpinner from '../components/LoadingSpinner/LoadingSpinner'
import './AuthCallback.css'

/**
 * OAuth2 로그인 성공 후 백엔드가
 *   http://localhost:5173/auth/callback?token=JWT_TOKEN
 * 으로 리다이렉트하면 이 페이지가 받아서 처리한다.
 */
export default function AuthCallback() {
  const [searchParams] = useSearchParams()
  const { loginWithToken } = useAuth()
  const navigate = useNavigate()
  const called = useRef(false)

  useEffect(() => {
    if (called.current) return
    called.current = true

    const token = searchParams.get('token')
    if (!token) {
      navigate('/', { replace: true })
      return
    }
    loginWithToken(token).then(() => {
      navigate('/', { replace: true })
    })
  }, [searchParams, loginWithToken, navigate])

  return (
    <div className="auth-callback">
      <LoadingSpinner />
    </div>
  )
}
