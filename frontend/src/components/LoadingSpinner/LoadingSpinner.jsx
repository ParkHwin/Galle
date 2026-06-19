import './LoadingSpinner.css'

export default function LoadingSpinner({ message = '교통수단 비교 중...' }) {
  return (
    <div className="loading-spinner" role="status" aria-live="polite">
      <div className="loading-spinner__ring" aria-hidden="true">
        <div></div>
        <div></div>
        <div></div>
        <div></div>
      </div>
      <p className="loading-spinner__text">{message}</p>
    </div>
  )
}
