import { useNavigate } from 'react-router-dom'
import SearchForm from '../components/SearchForm/SearchForm'
import Icon from '../components/Icon/Icon'
import './HomePage.css'

export default function HomePage() {
  const navigate = useNavigate()

  function handleSearch({ from, to, date, time }) {
    const params = new URLSearchParams({ from, to, date, time })
    navigate(`/search?${params.toString()}`)
  }

  return (
    <div className="home-page">
      {/* Hero */}
      <section className="home-hero">
        <div className="home-hero__inner container">
          <div className="home-hero__text">
            <p className="home-hero__eyebrow">교통수단 통합 비교</p>
            <h1 className="home-hero__title">갈래</h1>
            <p className="home-hero__subtitle">
              서울에서 부산, 어떻게 갈래?<br />
              KTX · SRT · 고속버스 · 자가용을<br />
              한 번에 비교하세요.
            </p>
          </div>
          <div className="home-hero__form-wrap">
            <SearchForm onSearch={handleSearch} />
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="home-features">
        <div className="container">
          <h2 className="home-features__heading">왜 갈래인가요?</h2>
          <div className="home-features__grid">
            {FEATURES.map((f) => (
              <div key={f.title} className="feature-card">
                <span className="feature-card__icon">
                  <Icon name={f.icon} size={26} />
                </span>
                <h3 className="feature-card__title">{f.title}</h3>
                <p className="feature-card__desc">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Popular routes */}
      <section className="home-popular">
        <div className="container">
          <h2 className="home-popular__heading">인기 노선</h2>
          <div className="home-popular__grid">
            {POPULAR_ROUTES.map((r) => (
              <button
                key={`${r.from}-${r.to}`}
                className="popular-route-btn"
                onClick={() => handleSearch({
                  from: r.from,
                  to: r.to,
                  date: new Date().toISOString().slice(0, 10),
                  time: '09:00',
                })}
              >
                <span className="popular-route-btn__from">{r.from}</span>
                <span className="popular-route-btn__arrow" aria-hidden="true">→</span>
                <span className="popular-route-btn__to">{r.to}</span>
              </button>
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}

const FEATURES = [
  {
    icon: 'bolt',
    title: '10초 비교',
    desc: 'KTX, SRT, 고속버스, 자가용 비용을 한 번에 조회합니다.',
  },
  {
    icon: 'wallet',
    title: '최저가 탐색',
    desc: '모든 교통수단 중 가장 저렴한 옵션을 즉시 확인합니다.',
  },
  {
    icon: 'map',
    title: '자가용 경비 계산',
    desc: '고속도로 통행료와 유류비를 포함한 실제 비용을 계산합니다.',
  },
  {
    icon: 'bookmark',
    title: '즐겨찾기',
    desc: '자주 이용하는 노선을 저장해 빠르게 검색하세요.',
  },
]

const POPULAR_ROUTES = [
  { from: '서울', to: '부산' },
  { from: '서울', to: '대전' },
  { from: '서울', to: '대구' },
  { from: '서울', to: '광주' },
  { from: '부산', to: '서울' },
  { from: '대전', to: '부산' },
]
