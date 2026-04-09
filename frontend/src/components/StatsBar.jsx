export default function StatsBar({ stats }) {
  return (
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
  );
}
