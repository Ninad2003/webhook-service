export default function StatusBadge({ status }) {
  if (!status) return null;
  return (
    <span className={`status-badge ${status.toLowerCase()}`}>
      {status}
    </span>
  );
}
