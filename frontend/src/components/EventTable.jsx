import StatusBadge from './StatusBadge';

export default function EventTable({ loading, events, onRowClick }) {
  if (loading && events.length === 0) {
    return <p style={{padding: '20px'}}>Loading...</p>;
  }

  return (
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
          <tr key={event.id} onClick={() => onRowClick(event.id)}>
            <td>{event.eventId.substring(0, 10)}...</td>
            <td>{event.transactionId}</td>
            <td>{event.partnerId}</td>
            <td><span className="event-type-badge">{event.eventType}</span></td>
            <td><StatusBadge status={event.status} /></td>
            <td>{event.attemptCount}/{event.maxAttempts}</td>
            <td>{new Date(event.createdAt).toLocaleString()}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
