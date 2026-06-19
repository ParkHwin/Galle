import { useState } from 'react'
import './SearchForm.css'

const CITIES = ['서울', '부산', '대전', '대구', '광주', '제주']

function getTodayDate() {
  return new Date().toISOString().slice(0, 10)
}

function getCurrentTime() {
  const now = new Date()
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

export default function SearchForm({ onSearch, initialValues = {} }) {
  const [from, setFrom] = useState(initialValues.from || '서울')
  const [to, setTo] = useState(initialValues.to || '부산')
  const [date, setDate] = useState(initialValues.date || getTodayDate())
  const [time, setTime] = useState(initialValues.time || getCurrentTime())
  const [error, setError] = useState('')

  function handleSwap() {
    setFrom(to)
    setTo(from)
  }

  function handleSubmit(e) {
    e.preventDefault()
    if (from === to) {
      setError('출발지와 도착지가 같습니다.')
      return
    }
    setError('')
    onSearch({ from, to, date, time })
  }

  return (
    <form className="search-form" onSubmit={handleSubmit} aria-label="교통수단 검색">
      <div className="search-form__row">
        <div className="search-form__field">
          <label className="search-form__label" htmlFor="sf-from">출발지</label>
          <select
            id="sf-from"
            className="search-form__select"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          >
            {CITIES.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>

        <button
          type="button"
          className="search-form__swap"
          onClick={handleSwap}
          aria-label="출발지 도착지 바꾸기"
        >
          ⇄
        </button>

        <div className="search-form__field">
          <label className="search-form__label" htmlFor="sf-to">도착지</label>
          <select
            id="sf-to"
            className="search-form__select"
            value={to}
            onChange={(e) => setTo(e.target.value)}
          >
            {CITIES.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>

        <div className="search-form__field">
          <label className="search-form__label" htmlFor="sf-date">날짜</label>
          <input
            id="sf-date"
            className="search-form__input"
            type="date"
            value={date}
            min={getTodayDate()}
            onChange={(e) => setDate(e.target.value)}
          />
        </div>

        <div className="search-form__field">
          <label className="search-form__label" htmlFor="sf-time">시간</label>
          <input
            id="sf-time"
            className="search-form__input"
            type="time"
            value={time}
            onChange={(e) => setTime(e.target.value)}
          />
        </div>

        <button type="submit" className="search-form__submit btn-primary">
          비교하기
        </button>
      </div>

      {error && <p className="search-form__error" role="alert">{error}</p>}
    </form>
  )
}
