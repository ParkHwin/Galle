import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import './FavoritesPage.css'

export default function FavoritesPage() {
  const [favorites, setFavorites] = useState([])
  const navigate = useNavigate()

  useEffect(() => {
    try {
      const stored = JSON.parse(localStorage.getItem('gallae_favorites') || '[]')
      setFavorites(stored)
    } catch {
      setFavorites([])
    }
  }, [])

  function handleRemove(index) {
    const updated = favorites.filter((_, i) => i !== index)
    setFavorites(updated)
    localStorage.setItem('gallae_favorites', JSON.stringify(updated))
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
            <li key={i} className="favorites-page__item card">
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
                  onClick={() => handleRemove(i)}
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
