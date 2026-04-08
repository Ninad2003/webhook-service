import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import EventList from './pages/EventList';
import EventDetail from './pages/EventDetail';
import './index.css';

function App() {
  return (
    <BrowserRouter>
      <div className="app-container">
        <header className="header">
          <Link to="/" className="logo">Webhook Delivery Service</Link>
          <nav>
            <Link to="/" className="nav-link">Events</Link>
          </nav>
        </header>
        
        <main className="main-content">
          <Routes>
            <Route path="/" element={<EventList />} />
            <Route path="/events/:id" element={<EventDetail />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;
