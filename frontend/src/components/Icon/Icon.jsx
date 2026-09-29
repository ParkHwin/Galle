// 이모지 아이콘을 대체하는 단순 라인 SVG 아이콘 세트.
// 모든 아이콘은 24x24 viewBox, stroke=currentColor 라서 부모의 color 값을 그대로 물려받는다.
const PATHS = {
  bolt: (
    <path d="M13 2 3 14h7l-1 8 10-12h-7l1-8Z" />
  ),
  wallet: (
    <>
      <path d="M3 7a2 2 0 0 1 2-2h13a1 1 0 0 1 1 1v2" />
      <path d="M3 7v10a2 2 0 0 0 2 2h14a1 1 0 0 0 1-1v-3" />
      <path d="M15 12.5h4a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1h-4a2 2 0 0 1 0-4Z" />
    </>
  ),
  map: (
    <>
      <path d="M9 4 3 6v14l6-2 6 2 6-2V4l-6 2-6-2Z" />
      <path d="M9 4v14" />
      <path d="M15 6v14" />
    </>
  ),
  bookmark: (
    <path d="M6 3h12a1 1 0 0 1 1 1v16l-7-4-7 4V4a1 1 0 0 1 1-1Z" />
  ),
  train: (
    <>
      <rect x="5" y="3" width="14" height="13" rx="4" />
      <path d="M5 12h14" />
      <circle cx="8.5" cy="15.5" r="0" />
      <path d="M8 19.5 6 22" />
      <path d="M16 19.5 18 22" />
      <path d="M8.5 12v-6" />
      <path d="M15.5 12v-6" />
    </>
  ),
  bus: (
    <>
      <rect x="3" y="4" width="18" height="12" rx="2.5" />
      <path d="M3 11h18" />
      <path d="M7 19v1.5" />
      <path d="M17 19v1.5" />
      <circle cx="7.5" cy="16.5" r="1.3" />
      <circle cx="16.5" cy="16.5" r="1.3" />
    </>
  ),
  car: (
    <>
      <path d="M4 16V11l2.2-5A2 2 0 0 1 8.1 4.7h7.8a2 2 0 0 1 1.9 1.3L20 11v5" />
      <path d="M4 16h16v2.5a1 1 0 0 1-1 1h-1.5a1 1 0 0 1-1-1V17H7.5v1.5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V16Z" />
      <path d="M4 11h16" />
      <circle cx="7.5" cy="13.5" r="0.9" fill="currentColor" stroke="none" />
      <circle cx="16.5" cy="13.5" r="0.9" fill="currentColor" stroke="none" />
    </>
  ),
}

export default function Icon({ name, size = 24, className, ...rest }) {
  const path = PATHS[name]
  if (!path) return null
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...rest}
    >
      {path}
    </svg>
  )
}
