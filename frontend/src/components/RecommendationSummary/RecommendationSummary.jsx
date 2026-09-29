import './RecommendationSummary.css'

export default function RecommendationSummary({ summary }) {
  if (!summary) return null

  const items = [
    {
      key: 'cheapest',
      emoji: '💰',
      label: '최저가',
      value: summary.cheapest,
      colorClass: 'summary-card--green',
    },
    {
      key: 'fastest',
      emoji: '⚡',
      label: '최단시간',
      value: summary.fastest,
      colorClass: 'summary-card--blue',
    },
    {
      key: 'recommended',
      emoji: '⭐',
      label: '종합추천',
      value: summary.recommended,
      colorClass: 'summary-card--orange',
    },
  ]

  const visibleItems = items.filter((item) => item.value != null)
  if (visibleItems.length === 0) return null

  return (
    <div className="recommendation-summary" aria-label="검색 결과 요약">
      {visibleItems.map((item) => (
        <div key={item.key} className={`summary-card ${item.colorClass}`}>
          <span className="summary-card__emoji" aria-hidden="true">{item.emoji}</span>
          <div className="summary-card__body">
            <span className="summary-card__label">{item.label}</span>
            <span className="summary-card__value">{item.value}</span>
          </div>
        </div>
      ))}
    </div>
  )
}
