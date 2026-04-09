export default function DetailCard({ title, children }) {
  return (
    <div className="detail-card">
      <h3>{title}</h3>
      {children}
    </div>
  );
}
