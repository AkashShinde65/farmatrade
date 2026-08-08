import { useEffect, useState } from 'react';
import * as lotService from '../services/lot-service';
import { LotCard } from '../components/lot-card';

export function LotBrowsePage() {
  const [lots, setLots] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    lotService
      .getAllLots()
      .then((data) => {
        if (!cancelled) setLots(data.filter((lot) => lot.status === 'LISTED' || lot.status === 'ACTIVE'));
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Could not load lots.');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Browse lots</h1>
          <p className="page-subtitle">Open produce available to bid on or buy now.</p>
        </div>
      </div>

      {error ? <p className="form-error">{error}</p> : null}
      {lots === null && !error ? (
        <p className="loading-text">Loading…</p>
      ) : lots.length === 0 ? (
        <p className="empty-text">No open lots right now.</p>
      ) : (
        <div className="lot-card-grid">
          {lots.map((lot) => (
            <LotCard key={lot.id} lot={lot} />
          ))}
        </div>
      )}
    </div>
  );
}
