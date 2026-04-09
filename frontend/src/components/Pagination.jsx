export default function Pagination({ eventsLength, totalElements, page, setPage, totalPages }) {
  return (
    <div className="pagination">
      <span>Showing {eventsLength} of {totalElements}</span>
      <div>
        <button className="pagination-btn" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Prev</button>
        <span style={{margin: '0 10px'}}>Page {page + 1} of {Math.max(1, totalPages)}</span>
        <button className="pagination-btn" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next</button>
      </div>
    </div>
  );
}
