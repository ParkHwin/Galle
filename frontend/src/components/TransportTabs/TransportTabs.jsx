import './TransportTabs.css'

const TABS = [
  { id: 'all', label: '전체' },
  { id: 'KTX', label: 'KTX' },
  { id: 'SRT', label: 'SRT' },
  { id: 'ITX-마음', label: 'ITX-마음' },
  { id: 'ITX-새마을', label: 'ITX-새마을' },
  { id: 'ITX-청춘', label: 'ITX-청춘' },
  { id: '무궁화호', label: '무궁화호' },
  { id: '고속버스', label: '고속버스' },
  { id: '자가용', label: '자가용' },
]

export default function TransportTabs({ activeTab, onTabChange }) {
  return (
    <div className="transport-tabs" role="tablist" aria-label="교통수단 필터">
      <div className="transport-tabs__scroll">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            role="tab"
            aria-selected={activeTab === tab.id}
            className={
              activeTab === tab.id
                ? 'transport-tabs__tab transport-tabs__tab--active'
                : 'transport-tabs__tab'
            }
            onClick={() => onTabChange(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>
    </div>
  )
}
