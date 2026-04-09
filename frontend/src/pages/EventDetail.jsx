import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../api';

import StatusBadge from '../components/StatusBadge';
import DetailCard from '../components/DetailCard';
import DeliveryAttempts from '../components/DeliveryAttempts';

export default function EventDetail() {
  const { id } = useParams();
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchEvent = async () => {
      setLoading(true);
      try {
        const data = await api.getEventDetail(id);
        setEvent(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchEvent();

    const interval = setInterval(fetchEvent, 3000);
    return () => clearInterval(interval);
  }, [id]);

  if (loading && !event) return <p>Loading event details...</p>;
  if (error) return <div><p>Error: {error}</p><Link to="/">Back</Link></div>;
  if (!event) return null;

  return (
    <div>
      <Link to="/" className="back-link">← Back to Events</Link>
      
      <h2>Event Detail - <StatusBadge status={event.status} /></h2>

      <div className="detail-grid">
        <DetailCard title="Information">
          <div className="detail-row"><span className="detail-label">Event ID</span><span>{event.eventId}</span></div>
          <div className="detail-row"><span className="detail-label">Transaction ID</span><span>{event.transactionId}</span></div>
          <div className="detail-row"><span className="detail-label">Partner ID</span><span>{event.partnerId}</span></div>
          <div className="detail-row"><span className="detail-label">Event Type</span><span>{event.eventType}</span></div>
        </DetailCard>

        <DetailCard title="Delivery Status">
          <div className="detail-row"><span className="detail-label">Status</span><span>{event.status}</span></div>
          <div className="detail-row"><span className="detail-label">Sequence #</span><span>{event.sequenceNumber}</span></div>
          <div className="detail-row"><span className="detail-label">Attempts</span><span>{event.attemptCount} / {event.maxAttempts}</span></div>
          <div className="detail-row"><span className="detail-label">Next Retry</span><span>{event.nextRetryAt ? new Date(event.nextRetryAt).toLocaleString() : 'N/A'}</span></div>
          <div className="detail-row"><span className="detail-label">Created At</span><span>{new Date(event.createdAt).toLocaleString()}</span></div>
        </DetailCard>

        <DetailCard title="Payload">
          <pre className="payload-block">{event.payload}</pre>
        </DetailCard>

        <DetailCard title={`Attempts (${event.deliveryAttempts?.length || 0})`}>
          <DeliveryAttempts attempts={event.deliveryAttempts} />
        </DetailCard>
      </div>
    </div>
  );
}
