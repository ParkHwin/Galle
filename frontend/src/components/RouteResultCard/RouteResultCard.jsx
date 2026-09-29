import { useState, useCallback } from 'react'
import FareBadge from '../FareBadge/FareBadge'
import DurationBadge from '../DurationBadge/DurationBadge'
import Icon from '../Icon/Icon'
import { useAuth } from '../../context/AuthContext'
import './RouteResultCard.css'

const TRANSPORT_ICON = {
  KTX: 'train',
  SRT: 'train',
  'ITX-마음': 'train',
  'ITX-새마을': 'train',
  'ITX-청춘': 'train',
  '무궁화호': 'train',
  '누리로': 'train',
  '고속버스': 'bus',
  '자가용': 'car',
}

const TAG_CLASS = {
  '최저가': 'tag--green',
  '최단시간': 'tag--blue',
  '종합추천': 'tag--orange',
}

export default function RouteResultCard({ result }) {
  const {
    type,
    name,
    departureName,
    arrivalName,
    departureTime,
    arrivalTime,
    durationMinutes,
    fare,
    recommendTags = [],
    bookingUrl,
    detail,
  } = result

  const { user, login, isFavoritedRoute, toggleFavoriteRoute } = useAuth()
  const isCar = type === '자가용'
  // 자가용은 로그인 없이 항상 가능, 나머지는 로그인 필요
  const canBook = isCar || !!user

  const from = departureName?.replace(/(역|터미널)$/, '') ?? ''
  const to   = arrivalName?.replace(/(역|터미널)$/, '') ?? ''

  // 즐겨찾기 여부는 AuthContext가 들고 있는 favorites에서 파생한다.
  // 로그인 사용자는 서버(favorites 테이블), 비로그인 사용자는 localStorage — 컴포넌트는 신경 쓰지 않는다.
  const faved = isFavoritedRoute(from, to)
  const [loginHint, setLoginHint] = useState(false)

  const handleFav = useCallback(() => {
    toggleFavoriteRoute(from, to)
  }, [from, to, toggleFavoriteRoute])

  // 비로그인 상태에서 예매 버튼 클릭 시
  const handleBookingClick = useCallback(() => {
    setLoginHint(true)
    setTimeout(() => setLoginHint(false), 3000)
  }, [])

  const iconName = TRANSPORT_ICON[type] || 'train'

  return (
    <article className="route-card card">
      <div className="route-card__header">
        <div className="route-card__type-row">
          <span className="route-card__emoji">
            <Icon name={iconName} size={22} />
          </span>
          <span className="route-card__type">{type}</span>
          <span className="route-card__name">{name}</span>
        </div>
        <div className="route-card__header-right">
          {recommendTags.length > 0 && (
            <div className="route-card__tags" aria-label="추천 태그">
              {recommendTags.map((tag) => (
                <span key={tag} className={`route-card__tag ${TAG_CLASS[tag] || ''}`}>
                  {tag}
                </span>
              ))}
            </div>
          )}
          <button
            className={`route-card__fav-btn ${faved ? 'route-card__fav-btn--active' : ''}`}
            onClick={handleFav}
            aria-label={faved ? '즐겨찾기 해제' : '즐겨찾기 추가'}
            title={faved ? '즐겨찾기 해제' : '즐겨찾기 추가'}
          >
            {faved ? '★' : '☆'}
            <span className="route-card__fav-label">{faved ? '저장됨' : '저장'}</span>
          </button>
        </div>
      </div>

      <div className="route-card__route">
        <div className="route-card__station">
          <span className="route-card__time">{departureTime ?? '—'}</span>
          <span className="route-card__station-name">{departureName}</span>
        </div>
        <div className="route-card__arrow">
          <DurationBadge minutes={durationMinutes} />
          <span className="route-card__arrow-line" aria-hidden="true">→</span>
        </div>
        <div className="route-card__station route-card__station--right">
          <span className="route-card__time">{arrivalTime ?? '—'}</span>
          <span className="route-card__station-name">{arrivalName}</span>
        </div>
      </div>

      {detail && (
        <div className="route-card__detail">
          <span>거리 {detail.distanceKm}km</span>
          <span>통행료 {detail.toll?.toLocaleString('ko-KR')}원</span>
          <span>유류비 약 {detail.fuelCost?.toLocaleString('ko-KR')}원</span>
          {detail.fuelStandard && (
            <span className="route-card__detail-note">({detail.fuelStandard})</span>
          )}
        </div>
      )}

      <div className="route-card__footer">
        <div className="route-card__fare">
          <FareBadge fare={fare} />
        </div>

        {!bookingUrl ? (
          <span className="route-card__no-booking">예매 불필요</span>
        ) : canBook ? (
          /* 로그인 상태 or 자가용 — 정상 버튼 */
          <a
            href={bookingUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="route-card__book btn-primary"
          >
            {isCar ? '카카오맵 길찾기' : '예매하기'}
          </a>
        ) : (
          /* 비로그인 + 대중교통 — 잠금 버튼 */
          <div className="route-card__login-gate">
            <button
              className="route-card__book route-card__book--locked btn-primary"
              onClick={handleBookingClick}
              aria-label="로그인 후 예매 가능"
            >
              🔒 예매하기
            </button>
            {loginHint && (
              <div className="route-card__login-hint">
                <span>예매는 로그인 후 이용 가능합니다.</span>
                <button
                  className="route-card__login-hint-btn"
                  onClick={() => login('kakao')}
                >
                  로그인하기
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </article>
  )
}
