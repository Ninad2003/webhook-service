import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';

const EVENT_TYPES = ['', 'KYC_REGISTERED', 'TXN_SCREENED', 'TXN_BLOCKED', 'TXN_RELEASED', 'INVALID_TXN'];
const STATUSES = ['', 'PENDING', 'PROCESSING', 'DELIVERED', 'FAILED'];

export default function EventList() {
  const navigate = useNavigate();
  const [events, setEvents] = useState([]);
  const [partners, setPartners] = useState([]);
  const [loading, setLoading] = useState(true);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [partnerId, setPartnerId] = useState('');
  const [status, setStatus] = useState('');
  const [eventType, setEventType] = useState('');
  const [page, setPage] = useState(0);

  const [stats, setStats] = useState({ total: 0, pending: 0, delivered: 0, failed: 0 });

  const fetchEvents = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.getEvents({ partnerId, status, eventType, page, size: 20 });
      setEvents(data.content || []);
      setTotalElements(data.totalElements || 0);
      setTotalPages(data.totalPages || 0);
    } catch (err) {
      console.error('Failed to fetch events:', err);
    } finally {
      setLoading(false);
    }
  }, [partnerId, status, eventType, page]);

  const fetchStats = useCallback(async () => {
    try {
      const [all, pending, delivered, failed] = await Promise.all([
        api.getEvents({ size: 1 }),
        api.getEvents({ status: 'PENDING', size: 1 }),
        api.getEvents({ status: 'DELIVERED', size: 1 }),
        api.getEvents({ status: 'FAILED', size: 1 }),
      ]);
      setStats({
        total: all.totalElements || 0,
        pending: pending.totalElements || 0,
        delivered: delivered.totalElements || 0,
        failed: failed.totalElements || 0,
      });
    } catch (err) {
      console.error('Failed to fetch stats:', err);
    }
  }, []);

  const fetchPartners = useCallback(async () => {
    try {
      const data = await api.getPartners();
      setPartners(data);
    } catch (err) {
      console.error('Failed to fetch partners:', err);
    }
  }, []);

  useEffect(() => {
    fetchPartners();
    fetchStats();
  }, [fetchPartners, fetchStats]);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  useEffect(() => {
    const interval = setInterval(() => {
      fetchEvents();
      fetchStats();
    }, 3000);
    return () => clearInterval(interval);
  }, [fetchEvents, fetchStats]);

  const resetFilters = () => {
    setPartnerId('');
    setStatus('');
    setEventType('');
    setPage(0);
  };

  return (
    <div>
      <h2>Webhook Events</h2>
      
      <div className="stats-bar">
        <div className="stat-card">
          <h3>{stats.total}</h3>
          <p>Total Events</p>
        </div>
        <div className="stat-card">
          <h3>{stats.pending}</h3>
          <p>Pending</p>
        </div>
        <div className="stat-card">
          <h3>{stats.delivered}</h3>
          <p>Delivered</p>
        </div>
        <div className="stat-card">
          <h3>{stats.failed}</h3>
          <p>Failed</p>
        </div>
      </div>

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

      <div className="table-container">
        {loading && events.length === 0 ? <p style={{padding: '20px'}}>Loading...</p> : 
          <table>
            <thead>
              <tr>
                <th>Event ID</th>
                <th>Transaction ID</th>
                <th>Partner</th>
                <th>Type</th>
                <th>Status</th>
                <th>Attempts</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {events.map(event => (
                <tr key={event.id} onClick={() => navigate(`/events/${event.id}`)}>
                  <td>{event.eventId.substring(0, 10)}...</td>
                  <td>{event.transactionId}</td>
                  <td>{event.partnerId}</td>
                  <td><span className="event-type-badge">{event.eventType}</span></td>
                  <td><span className={`status-badge ${event.status.toLowerCase()}`}>{event.status}</span></td>
                  <td>{event.attemptCount}/{event.maxAttempts}</td>
                  <td>{new Date(event.createdAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        }
        
        <div className="pagination">
          <span>Showing {events.length} of {totalElements}</span>
          <div>
            <button className="pagination-btn" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Prev</button>
            <span style={{margin: '0 10px'}}>Page {page + 1} of {Math.max(1, totalPages)}</span>
            <button className="pagination-btn" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next</button>
          </div>
        </div>
      </div>
    </div>
  );
}
