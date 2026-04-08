import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../api';

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
      
      <h2>Event Detail - <span className={`status-badge ${event.status.toLowerCase()}`}>{event.status}</span></h2>

      <div className="detail-grid">
        <div className="detail-card">
          <h3>Information</h3>
          <div className="detail-row"><span className="detail-label">Event ID</span><span>{event.eventId}</span></div>
          <div className="detail-row"><span className="detail-label">Transaction ID</span><span>{event.transactionId}</span></div>
          <div className="detail-row"><span className="detail-label">Partner ID</span><span>{event.partnerId}</span></div>
          <div className="detail-row"><span className="detail-label">Event Type</span><span>{event.eventType}</span></div>
        </div>

        <div className="detail-card">
          <h3>Delivery Status</h3>
          <div className="detail-row"><span className="detail-label">Status</span><span>{event.status}</span></div>
          <div className="detail-row"><span className="detail-label">Sequence #</span><span>{event.sequenceNumber}</span></div>
          <div className="detail-row"><span className="detail-label">Attempts</span><span>{event.attemptCount} / {event.maxAttempts}</span></div>
          <div className="detail-row"><span className="detail-label">Next Retry</span><span>{event.nextRetryAt ? new Date(event.nextRetryAt).toLocaleString() : 'N/A'}</span></div>
          <div className="detail-row"><span className="detail-label">Created At</span><span>{new Date(event.createdAt).toLocaleString()}</span></div>
        </div>

        <div className="detail-card">
          <h3>Payload</h3>
          <pre className="payload-block">{event.payload}</pre>
        </div>

        <div className="detail-card">
          <h3>Attempts ({event.deliveryAttempts?.length || 0})</h3>
          {(!event.deliveryAttempts || event.deliveryAttempts.length === 0) ? <p>No attempts yet.</p> : (
            <div>
              {event.deliveryAttempts.map(attempt => (
                <div key={attempt.id} className="attempt-item">
                  <div className="attempt-header">
                    <span>Attempt #{attempt.attemptNumber}</span>
                    <span>{new Date(attempt.createdAt).toLocaleString()}</span>
                  </div>
                  <p>Status Code: {attempt.statusCode || 'Connection Failed'}</p>
                  {attempt.responseTimeMs && <p>Response Time: {attempt.responseTimeMs}ms</p>}
                  {attempt.error && <div className="attempt-error">{attempt.error}</div>}
                  {attempt.responseBody && <pre className="payload-block" style={{marginTop: '10px'}}>{attempt.responseBody}</pre>}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
