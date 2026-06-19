import FareBadge from '../FareBadge/FareBadge'
import DurationBadge from '../DurationBadge/DurationBadge'
import './RouteResultCard.css'

const TRANSPORT_EMOJI = {
  KTX: '🚄',
  SRT: '🚄',
  'ITX-마음': '🚄',
  'ITX-새마을': '🚄',
  '새마을호': '🚂',
  '무궁화호': '🚂',
  '고속버스': '🚌',
  '자가용': '🚗',
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

  const emoji = TRANSPORT_EMOJI[type] || '🚆'

  return (
    <article className="route-card card">
      <div className="route-card__header">
        <div className="route-card__type-row">
          <span className="route-card__emoji" aria-hidden="true">{emoji}</span>
          <span className="route-card__type">{type}</span>
          <span className="route-card__name">{name}</span>
        </div>
        {recommendTags.length > 0 && (
          <div className="route-card__tags" aria-label="추천 태그">
            {recommendTags.map((tag) => (
              <span key={tag} className={`route-card__tag ${TAG_CLASS[tag] || ''}`}>
                {tag}
              </span>
            ))}
          </div>
        )}
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
        {bookingUrl ? (
          <a
            href={bookingUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="route-card__book btn-primary"
          >
            예매하기
          </a>
        ) : (
          <span className="route-card__no-booking">예매 불필요</span>
        )}
      </div>
    </article>
  )
}
