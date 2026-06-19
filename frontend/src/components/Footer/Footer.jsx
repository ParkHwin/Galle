import './Footer.css'

export default function Footer() {
  return (
    <footer className="footer">
      <div className="footer__inner container">
        <div className="footer__brand">
          <span className="footer__logo">🚆 갈래</span>
          <p className="footer__tagline">도시 간 교통 비교 플랫폼</p>
        </div>
        <p className="footer__disclaimer">
          표시된 가격은 참고용이며 실제 가격은 각 예매 사이트에서 확인하세요.
          실시간 요금·시간표는 변동될 수 있습니다.
        </p>
        <p className="footer__copy">© 2026 갈래(Gallae). All rights reserved.</p>
      </div>
    </footer>
  )
}
