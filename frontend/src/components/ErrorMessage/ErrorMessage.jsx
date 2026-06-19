export default function ErrorMessage({
  message = '검색 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
}) {
  return (
    <div
      className="error-message"
      role="alert"
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 'var(--space-sm)',
        background: 'rgba(207,32,47,0.06)',
        border: '1px solid rgba(207,32,47,0.2)',
        borderRadius: 'var(--radius-lg)',
        padding: 'var(--space-base) var(--space-lg)',
        color: 'var(--color-semantic-down)',
        fontSize: '14px',
        fontWeight: '500',
      }}
    >
      <span aria-hidden="true" style={{ fontSize: '18px' }}>⚠️</span>
      <span>{message}</span>
    </div>
  )
}
