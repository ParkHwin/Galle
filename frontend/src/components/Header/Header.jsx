import { useState, useRef, useEffect } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import './Header.css'

const NAV_LINKS = [
  { to: '/', label: '홈', end: true },
  { to: '/favorites', label: '즐겨찾기' },
  { to: '/settings', label: '설정' },
]

export default function Header() {
  const { user, login, logout } = useAuth()
  const [loginMenuOpen, setLoginMenuOpen] = useState(false)
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  const loginRef = useRef(null)
  const userRef = useRef(null)

  // 바깥 클릭 시 드롭다운 닫기
  useEffect(() => {
    function handleClick(e) {
      if (loginRef.current && !loginRef.current.contains(e.target)) setLoginMenuOpen(false)
      if (userRef.current && !userRef.current.contains(e.target)) setUserMenuOpen(false)
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  // 모바일 nav 열릴 때 스크롤 잠금
  useEffect(() => {
    document.body.style.overflow = mobileNavOpen ? 'hidden' : ''
    return () => { document.body.style.overflow = '' }
  }, [mobileNavOpen])

  return (
    <header className="header">
      <div className="header__inner container">
        <Link to="/" className="header__logo">
          <img
            src="/GALLE_logo.png"
            alt="갈래 – 모든 이동, 한 번에 비교"
            className="header__logo-img"
          />
        </Link>

        {/* 데스크톱 nav */}
        <nav className="header__nav" aria-label="주요 메뉴">
          {NAV_LINKS.map(({ to, label, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                isActive ? 'header__nav-link header__nav-link--active' : 'header__nav-link'
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="header__right">
          <div className="header__auth">
            {user ? (
              /* 로그인 상태 — 아바타 + 드롭다운 */
              <div className="header__user" ref={userRef}>
                <button
                  className="header__avatar-btn"
                  onClick={() => setUserMenuOpen((v) => !v)}
                  aria-label="사용자 메뉴"
                  aria-expanded={userMenuOpen}
                >
                  {user.profileImageUrl ? (
                    <img className="header__avatar-img" src={user.profileImageUrl} alt={user.nickname} />
                  ) : (
                    <span className="header__avatar-initial">
                      {(user.nickname ?? '?')[0].toUpperCase()}
                    </span>
                  )}
                </button>
                {userMenuOpen && (
                  <div className="header__dropdown" role="menu">
                    <div className="header__dropdown-name">{user.nickname}</div>
                    <div className="header__dropdown-email">{user.email}</div>
                    <hr className="header__dropdown-divider" />
                    <button
                      className="header__dropdown-item header__dropdown-item--danger"
                      onClick={() => { logout(); setUserMenuOpen(false) }}
                    >
                      로그아웃
                    </button>
                  </div>
                )}
              </div>
            ) : (
              /* 비로그인 상태 — 로그인 버튼 + 소셜 드롭다운 */
              <div className="header__login-wrap" ref={loginRef}>
                <button
                  className="btn-outline header__login-btn"
                  onClick={() => setLoginMenuOpen((v) => !v)}
                  aria-expanded={loginMenuOpen}
                >
                  로그인
                </button>
                {loginMenuOpen && (
                  <div className="header__dropdown" role="menu">
                    <p className="header__dropdown-hint">소셜 계정으로 로그인</p>
                    <button
                      className="header__social-btn header__social-btn--kakao"
                      onClick={() => { login('kakao'); setLoginMenuOpen(false) }}
                    >
                      <span className="header__social-icon">💬</span> 카카오로 계속하기
                    </button>
                    <button
                      className="header__social-btn header__social-btn--naver"
                      onClick={() => { login('naver'); setLoginMenuOpen(false) }}
                    >
                      <span className="header__social-icon">🇳</span> 네이버로 계속하기
                    </button>
                    <button
                      className="header__social-btn header__social-btn--google"
                      onClick={() => { login('google'); setLoginMenuOpen(false) }}
                    >
                      <span className="header__social-icon">🔵</span> Google로 계속하기
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* 햄버거 버튼 (모바일 전용) */}
          <button
            className="header__hamburger"
            onClick={() => setMobileNavOpen((v) => !v)}
            aria-label={mobileNavOpen ? '메뉴 닫기' : '메뉴 열기'}
            aria-expanded={mobileNavOpen}
          >
            <span className={`header__hamburger-bar ${mobileNavOpen ? 'header__hamburger-bar--open' : ''}`} />
          </button>
        </div>
      </div>

      {/* 모바일 nav 드로어 */}
      {mobileNavOpen && (
        <>
          <div
            className="header__mobile-overlay"
            onClick={() => setMobileNavOpen(false)}
            aria-hidden="true"
          />
          <nav className="header__mobile-nav" aria-label="모바일 메뉴">
            {NAV_LINKS.map(({ to, label, end }) => (
              <NavLink
                key={to}
                to={to}
                end={end}
                className={({ isActive }) =>
                  isActive ? 'header__mobile-link header__mobile-link--active' : 'header__mobile-link'
                }
                onClick={() => setMobileNavOpen(false)}
              >
                {label}
              </NavLink>
            ))}
          </nav>
        </>
      )}
    </header>
  )
}
