import { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import './FavoritesPage.css'

export default function FavoritesPage() {
  const { favorites, removeFavoriteRoute, refreshFavorites } = useAuth()
  const navigate = useNavigate()

  // 로그인 사용자는 서버 값이 최신인지 다시 확인, 비로그인 사용자는 localStorage를 다시 읽는다.
  useEffect(() => {
    refreshFavorites()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function handleRemove(fav) {
    removeFavoriteRoute(fav.from, fav.to)
  }

  function handleSearch(fav) {
    const params = new URLSearchParams({
      from: fav.from,
      to: fav.to,
      date: new Date().toISOString().slice(0, 10),
      time: '09:00',
    })
    navigate(`/search?${params.toString()}`)
  }

  return (
    <div className="favorites-page container">
      <div className="favorites-page__header">
        <h1 className="favorites-page__title">즐겨찾기</h1>
        <p className="favorites-page__desc">자주 이용하는 노선을 저장하세요.</p>
      </div>

      {favorites.length === 0 ? (
        <div className="favorites-page__empty">
          <span aria-hidden="true">🔖</span>
          <p>즐겨찾기가 없습니다.</p>
          <p className="favorites-page__empty-sub">
            검색 결과 페이지에서 노선을 즐겨찾기에 추가할 수 있습니다.
          </p>
          <Link to="/" className="btn-primary favorites-page__cta">
            검색하러 가기
          </Link>
        </div>
      ) : (
        <ul className="favorites-page__list" aria-label="즐겨찾기 목록">
          {favorites.map((fav, i) => (
            <li key={fav.id ?? `${fav.from}-${fav.to}-${i}`} className="favorites-page__item card">
              <div className="favorites-item__route">
                <span className="favorites-item__from">{fav.from}</span>
                <span className="favorites-item__arrow" aria-hidden="true">→</span>
                <span className="favorites-item__to">{fav.to}</span>
              </div>
              {fav.label && (
                <span className="favorites-item__label">{fav.label}</span>
              )}
              <div className="favorites-item__actions">
                <button
                  className="btn-primary favorites-item__search"
                  onClick={() => handleSearch(fav)}
                >
                  검색
                </button>
                <button
                  className="favorites-item__remove"
                  onClick={() => handleRemove(fav)}
                  aria-label={`${fav.from}→${fav.to} 즐겨찾기 삭제`}
                >
                  삭제
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
