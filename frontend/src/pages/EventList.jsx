import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';

const EVENT_TYPES = ['', 'KYC_REGISTERED', 'TXN_SCREENED', 'TXN_BLOCKED', 'TXN_RELEASED', 'INVALID_TXN'];
const STATUSES = ['', 'PENDING', 'PROCESSING', 'DELIVERED', 'FAILED'];

import StatsBar from '../components/StatsBar';
import EventFilters from '../components/EventFilters';
import EventTable from '../components/EventTable';
import Pagination from '../components/Pagination';

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
      
      <StatsBar stats={stats} />

      <EventFilters 
        partners={partners}
        partnerId={partnerId} setPartnerId={setPartnerId}
        status={status} setStatus={setStatus}
        eventType={eventType} setEventType={setEventType}
        setPage={setPage} resetFilters={resetFilters} 
      />

      <div className="table-container">
        <EventTable 
          loading={loading} 
          events={events} 
          onRowClick={(id) => navigate(`/events/${id}`)} 
        />
        
        <Pagination 
          eventsLength={events.length}
          totalElements={totalElements}
          page={page} setPage={setPage}
          totalPages={totalPages}
        />
      </div>
    </div>
  );
}
