const EVENT_TYPES = ['', 'KYC_REGISTERED', 'TXN_SCREENED', 'TXN_BLOCKED', 'TXN_RELEASED', 'INVALID_TXN'];
const STATUSES = ['', 'PENDING', 'PROCESSING', 'DELIVERED', 'FAILED'];

export default function EventFilters({ 
  partners, 
  partnerId, setPartnerId, 
  status, setStatus, 
  eventType, setEventType,
  setPage, resetFilters 
}) {
  return (
    <div className="filters-bar">
      <div className="filter-group">
        <label>Partner</label>
        <select className="filter-select" value={partnerId} onChange={(e) => { setPartnerId(e.target.value); setPage(0); }}>
          <option value="">All</option>
          {partners.map(p => <option key={p.partnerId}>{p.partnerId}</option>)}
        </select>
      </div>
      <div className="filter-group">
        <label>Status</label>
        <select className="filter-select" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
          {STATUSES.map(s => <option key={s} value={s}>{s || 'All'}</option>)}
        </select>
      </div>
      <div className="filter-group">
        <label>Event Type</label>
        <select className="filter-select" value={eventType} onChange={(e) => { setEventType(e.target.value); setPage(0); }}>
          {EVENT_TYPES.map(t => <option key={t} value={t}>{t || 'All'}</option>)}
        </select>
      </div>
      <button className="btn-reset" onClick={resetFilters}>Reset</button>
    </div>
  );
}
