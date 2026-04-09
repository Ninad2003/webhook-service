export default function DeliveryAttempts({ attempts }) {
  if (!attempts || attempts.length === 0) {
    return <p>No attempts yet.</p>;
  }

  return (
    <div>
      {attempts.map(attempt => (
        <div key={attempt.id} className="attempt-item">
          <div className="attempt-header">
            <span>Attempt #{attempt.attemptNumber}</span>
            <span>{new Date(attempt.createdAt).toLocaleString()}</span>
          </div>
          <p>Status Code: {attempt.statusCode || 'Connection Failed'}</p>
          {attempt.responseTimeMs && <p>Response Time: {attempt.responseTimeMs}ms</p>}
          {attempt.error && <div className="attempt-error">{attempt.error}</div>}
          {attempt.responseBody && (
            <pre className="payload-block" style={{marginTop: '10px'}}>
              {attempt.responseBody}
            </pre>
          )}
        </div>
      ))}
    </div>
  );
}
