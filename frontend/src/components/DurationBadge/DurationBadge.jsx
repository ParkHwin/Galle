export default function DurationBadge({ minutes }) {
  if (minutes == null) return null
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  const text = h > 0 ? (m > 0 ? `${h}시간 ${m}분` : `${h}시간`) : `${m}분`
  return (
    <span className="duration-badge">
      {text}
    </span>
  )
}
