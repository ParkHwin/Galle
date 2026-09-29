import './ErrorMessage.css'

export default function ErrorMessage({
  message = '검색 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
}) {
  return (
    <div className="error-message" role="alert">
      <span className="error-message__icon" aria-hidden="true">⚠️</span>
      <span>{message}</span>
    </div>
  )
}
