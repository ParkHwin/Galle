export default function FareBadge({ fare }) {
  if (fare == null) return null
  const formatted = fare.toLocaleString('ko-KR')
  return (
    <span className="fare-badge">
      {formatted}원
    </span>
  )
}
