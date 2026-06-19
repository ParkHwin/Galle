import { useState, useEffect } from 'react'
import './SettingsPage.css'

const CAR_TYPES = [
  { id: 'small', label: '경차', example: '모닝, 스파크', mpg: 16 },
  { id: 'mid', label: '중형차', example: '아반떼, K5', mpg: 12 },
  { id: 'large', label: '대형차', example: 'K8, 그랜저', mpg: 9 },
  { id: 'suv', label: 'SUV', example: '투싼, 스포티지', mpg: 10 },
]

const DEFAULTS = {
  carType: 'mid',
  fuelEfficiency: 12,
  fuelPrice: 1700,
}

export default function SettingsPage() {
  const [settings, setSettings] = useState(DEFAULTS)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    try {
      const stored = JSON.parse(localStorage.getItem('gallae_settings') || 'null')
      if (stored) setSettings({ ...DEFAULTS, ...stored })
    } catch {
      // ignore
    }
  }, [])

  function handleCarTypeChange(carType) {
    const found = CAR_TYPES.find((c) => c.id === carType)
    setSettings((prev) => ({
      ...prev,
      carType,
      fuelEfficiency: found ? found.mpg : prev.fuelEfficiency,
    }))
    setSaved(false)
  }

  function handleSave() {
    localStorage.setItem('gallae_settings', JSON.stringify(settings))
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  function handleReset() {
    setSettings(DEFAULTS)
    localStorage.removeItem('gallae_settings')
    setSaved(false)
  }

  return (
    <div className="settings-page container">
      <div className="settings-page__header">
        <h1 className="settings-page__title">설정</h1>
        <p className="settings-page__desc">자가용 비용 계산에 사용할 차량 정보를 설정하세요.</p>
      </div>

      <div className="settings-page__section card">
        <h2 className="settings-section__title">차종 선택</h2>
        <div className="settings-car-grid">
          {CAR_TYPES.map((car) => (
            <button
              key={car.id}
              className={
                settings.carType === car.id
                  ? 'car-type-btn car-type-btn--active'
                  : 'car-type-btn'
              }
              onClick={() => handleCarTypeChange(car.id)}
              aria-pressed={settings.carType === car.id}
            >
              <span className="car-type-btn__label">{car.label}</span>
              <span className="car-type-btn__example">{car.example}</span>
              <span className="car-type-btn__mpg">기본 연비 {car.mpg}km/L</span>
            </button>
          ))}
        </div>
      </div>

      <div className="settings-page__section card">
        <h2 className="settings-section__title">연비 및 유류비 설정</h2>
        <div className="settings-form">
          <div className="settings-field">
            <label className="settings-field__label" htmlFor="s-eff">
              연비 (km/L)
            </label>
            <input
              id="s-eff"
              className="settings-field__input"
              type="number"
              min={1}
              max={30}
              step={0.5}
              value={settings.fuelEfficiency}
              onChange={(e) =>
                setSettings((prev) => ({
                  ...prev,
                  fuelEfficiency: parseFloat(e.target.value) || 0,
                }))
              }
            />
            <span className="settings-field__unit">km/L</span>
          </div>

          <div className="settings-field">
            <label className="settings-field__label" htmlFor="s-price">
              연료 단가
            </label>
            <input
              id="s-price"
              className="settings-field__input"
              type="number"
              min={500}
              max={5000}
              step={10}
              value={settings.fuelPrice}
              onChange={(e) =>
                setSettings((prev) => ({
                  ...prev,
                  fuelPrice: parseInt(e.target.value, 10) || 0,
                }))
              }
            />
            <span className="settings-field__unit">원/L</span>
          </div>
        </div>
      </div>

      <div className="settings-page__actions">
        <button className="btn-primary settings-save-btn" onClick={handleSave}>
          {saved ? '저장됨 ✓' : '저장하기'}
        </button>
        <button className="settings-reset-btn" onClick={handleReset}>
          초기화
        </button>
      </div>
    </div>
  )
}
