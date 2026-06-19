import { Link, NavLink } from 'react-router-dom'
import './Header.css'

export default function Header() {
  return (
    <header className="header">
      <div className="header__inner container">
        <Link to="/" className="header__logo">
          <span className="header__logo-icon">🚆</span>
          <span className="header__logo-text">갈래</span>
        </Link>
        <nav className="header__nav" aria-label="주요 메뉴">
          <NavLink
            to="/"
            end
            className={({ isActive }) =>
              isActive ? 'header__nav-link header__nav-link--active' : 'header__nav-link'
            }
          >
            홈
          </NavLink>
          <NavLink
            to="/favorites"
            className={({ isActive }) =>
              isActive ? 'header__nav-link header__nav-link--active' : 'header__nav-link'
            }
          >
            즐겨찾기
          </NavLink>
          <NavLink
            to="/settings"
            className={({ isActive }) =>
              isActive ? 'header__nav-link header__nav-link--active' : 'header__nav-link'
            }
          >
            설정
          </NavLink>
        </nav>
      </div>
    </header>
  )
}
