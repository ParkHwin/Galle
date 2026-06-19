import { useEffect, useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import SearchForm from '../components/SearchForm/SearchForm'
import TransportTabs from '../components/TransportTabs/TransportTabs'
import RecommendationSummary from '../components/RecommendationSummary/RecommendationSummary'
import RouteResultCard from '../components/RouteResultCard/RouteResultCard'
import LoadingSpinner from '../components/LoadingSpinner/LoadingSpinner'
import ErrorMessage from '../components/ErrorMessage/ErrorMessage'
import { searchRoutes } from '../api'
import './SearchPage.css'

// Mock data — used when API is unavailable
const MOCK_RESULTS = {
  from: '서울',
  to: '부산',
  date: '2026-06-19',
  time: '09:00',
  results: [
    {
      type: 'KTX',
      name: 'KTX 101',
      departureName: '서울역',
      arrivalName: '부산역',
      departureTime: '09:00',
      arrivalTime: '11:18',
      durationMinutes: 138,
      fare: 59800,
      recommendTags: ['최단시간', '종합추천'],
      bookingUrl: 'https://www.letskorail.com',
    },
    {
      type: 'SRT',
      name: 'SRT 301',
      departureName: '수서역',
      arrivalName: '부산역',
      departureTime: '09:10',
      arrivalTime: '11:25',
      durationMinutes: 135,
      fare: 52600,
      recommendTags: [],
      bookingUrl: 'https://etk.srail.kr',
    },
    {
      type: 'ITX-새마을',
      name: 'ITX-새마을 1001',
      departureName: '서울역',
      arrivalName: '부산역',
      departureTime: '08:40',
      arrivalTime: '13:22',
      durationMinutes: 282,
      fare: 42600,
      recommendTags: [],
      bookingUrl: 'https://www.letskorail.com',
    },
    {
      type: '고속버스',
      name: '서울경부 → 부산',
      departureName: '서울경부터미널',
      arrivalName: '부산종합터미널',
      departureTime: '09:20',
      arrivalTime: '13:40',
      durationMinutes: 260,
      fare: 25900,
      recommendTags: ['최저가'],
      bookingUrl: 'https://www.kobus.co.kr',
    },
    {
      type: '자가용',
      name: '자가용 (경부고속도로)',
      departureName: '서울',
      arrivalName: '부산',
      departureTime: null,
      arrivalTime: null,
      durationMinutes: 280,
      fare: 58100,
      recommendTags: [],
      bookingUrl: null,
      detail: {
        distanceKm: 325,
        toll: 28100,
        fuelCost: 30000,
        fuelStandard: '중형차 연비 12km/L 기준',
      },
    },
  ],
  summary: {
    cheapest: '고속버스',
    fastest: 'KTX',
    recommended: 'KTX',
  },
}

export default function SearchPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  const from = searchParams.get('from') || '서울'
  const to = searchParams.get('to') || '부산'
  const date = searchParams.get('date') || new Date().toISOString().slice(0, 10)
  const time = searchParams.get('time') || '09:00'

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [data, setData] = useState(null)
  const [activeTab, setActiveTab] = useState('all')
  const [usingMock, setUsingMock] = useState(false)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')
    setUsingMock(false)

    searchRoutes({ from, to, date, time })
      .then((res) => {
        if (!cancelled) {
          setData(res.data)
        }
      })
      .catch(() => {
        if (!cancelled) {
          // Fall back to mock data
          setData({
            ...MOCK_RESULTS,
            from,
            to,
            date,
            time,
          })
          setUsingMock(true)
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => { cancelled = true }
  }, [from, to, date, time])

  function handleSearch(params) {
    const qs = new URLSearchParams(params)
    navigate(`/search?${qs.toString()}`)
    setActiveTab('all')
  }

  const results = data?.results ?? []
  const filteredResults =
    activeTab === 'all'
      ? results
      : results.filter((r) => r.type === activeTab)

  return (
    <div className="search-page">
      {/* Sticky search bar */}
      <div className="search-page__form-bar">
        <div className="container">
          <SearchForm
            onSearch={handleSearch}
            initialValues={{ from, to, date, time }}
          />
        </div>
      </div>

      <div className="search-page__body container">
        {/* Route heading */}
        <div className="search-page__heading">
          <h1 className="search-page__title">
            {from} <span className="search-page__arrow">→</span> {to}
          </h1>
          <span className="search-page__meta">
            {date} · {time} 출발 기준
          </span>
          {usingMock && (
            <span className="search-page__mock-badge">
              * 샘플 데이터 (API 미연결)
            </span>
          )}
        </div>

        {loading && <LoadingSpinner />}

        {!loading && error && <ErrorMessage message={error} />}

        {!loading && data && (
          <>
            {/* Summary */}
            {data.summary && (
              <div className="search-page__section">
                <RecommendationSummary summary={data.summary} />
              </div>
            )}

            {/* Tabs */}
            <div className="search-page__section">
              <TransportTabs activeTab={activeTab} onTabChange={setActiveTab} />
            </div>

            {/* Results */}
            <div className="search-page__results">
              {filteredResults.length === 0 ? (
                <div className="search-page__empty">
                  <span aria-hidden="true">🔍</span>
                  <p>해당 교통수단의 결과가 없습니다.</p>
                </div>
              ) : (
                filteredResults.map((result, i) => (
                  <RouteResultCard key={`${result.type}-${i}`} result={result} />
                ))
              )}
            </div>
          </>
        )}
      </div>
    </div>
  )
}
