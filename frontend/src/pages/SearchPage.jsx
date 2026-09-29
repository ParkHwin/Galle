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
  const [page, setPage] = useState(1)
  const PAGE_SIZE = 10

  // URL 파라미터 변경 시 (뒤로가기 포함) 탭·페이지 초기화
  useEffect(() => {
    setActiveTab('all')
    setPage(1)
  }, [from, to, date, time])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')

    searchRoutes({ from, to, date, time })
      .then((res) => {
        if (!cancelled) {
          setData(res.data?.data ?? res.data)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err?.response?.data?.message || '검색 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.')
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
    setPage(1)
  }

  function handleTabChange(tab) {
    setActiveTab(tab)
    setPage(1)
  }

  const results = data?.results ?? []
  const filteredResults =
    activeTab === 'all'
      ? results
      : results.filter((r) => r.type === activeTab)

  const totalPages = activeTab === 'all' ? Math.ceil(filteredResults.length / PAGE_SIZE) : 1
  const pagedResults =
    activeTab === 'all'
      ? filteredResults.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)
      : filteredResults

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
              <TransportTabs activeTab={activeTab} onTabChange={handleTabChange} />
            </div>

            {/* Results */}
            <div className="search-page__results">
              {filteredResults.length === 0 ? (
                <div className="search-page__empty">
                  <span aria-hidden="true">🔍</span>
                  <p>해당 교통수단의 결과가 없습니다.</p>
                </div>
              ) : (
                <>
                  {pagedResults.map((result, i) => (
                    <RouteResultCard key={`${result.type}-${(page - 1) * PAGE_SIZE + i}`} result={result} />
                  ))}

                  {activeTab === 'all' && totalPages > 1 && (
                    <div className="search-page__pagination">
                      <button
                        className="search-page__page-btn"
                        onClick={() => setPage(p => Math.max(1, p - 1))}
                        disabled={page === 1}
                        aria-label="이전 페이지"
                      >
                        ‹
                      </button>
                      {Array.from({ length: totalPages }, (_, i) => i + 1).map(p => (
                        <button
                          key={p}
                          className={`search-page__page-btn${p === page ? ' search-page__page-btn--active' : ''}`}
                          onClick={() => setPage(p)}
                          aria-current={p === page ? 'page' : undefined}
                        >
                          {p}
                        </button>
                      ))}
                      <button
                        className="search-page__page-btn"
                        onClick={() => setPage(p => Math.min(totalPages, p + 1))}
                        disabled={page === totalPages}
                        aria-label="다음 페이지"
                      >
                        ›
                      </button>
                    </div>
                  )}
                </>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  )
}
