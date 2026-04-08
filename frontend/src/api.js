const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export const api = {
  // Events
  async getEvents({ partnerId, status, eventType, page = 0, size = 20 } = {}) {
    const params = new URLSearchParams();
    if (partnerId) params.set('partnerId', partnerId);
    if (status) params.set('status', status);
    if (eventType) params.set('eventType', eventType);
    params.set('page', page.toString());
    params.set('size', size.toString());

    const res = await fetch(`${API_BASE}/api/events?${params}`);
    if (!res.ok) throw new Error(`Failed to fetch events: ${res.status}`);
    return res.json();
  },

  async getEventDetail(id) {
    const res = await fetch(`${API_BASE}/api/events/${id}`);
    if (!res.ok) throw new Error(`Failed to fetch event: ${res.status}`);
    return res.json();
  },

  // Partners
  async getPartners() {
    const res = await fetch(`${API_BASE}/api/partners`);
    if (!res.ok) throw new Error(`Failed to fetch partners: ${res.status}`);
    return res.json();
  },

  async registerPartner(data) {
    const res = await fetch(`${API_BASE}/api/partners`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error(`Failed to register partner: ${res.status}`);
    return res.json();
  },

  // Ingest event (for testing from dashboard)
  async ingestEvent(data) {
    const res = await fetch(`${API_BASE}/api/events`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error(`Failed to ingest event: ${res.status}`);
    return res.json();
  },
};
