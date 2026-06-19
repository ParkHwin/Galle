import { Link } from 'react-router-dom'
import './ComparePage.css'

export default function ComparePage() {
  return (
    <div className="compare-page container">
      <div className="compare-page__inner">
        <span className="compare-page__icon" aria-hidden="true">🔧</span>
        <h1 className="compare-page__title">비교 기능 준비 중</h1>
        <p className="compare-page__desc">
          교통수단 상세 비교 기능을 준비하고 있습니다.<br />
          지금은 검색 결과 페이지에서 비교해 보세요.
        </p>
        <Link to="/" className="btn-primary compare-page__cta">
          검색 결과 보러 가기
        </Link>
      </div>
    </div>
  )
}
