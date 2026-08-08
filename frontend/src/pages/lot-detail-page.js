import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../hooks/auth-hook';
import * as lotService from '../services/lot-service';
import * as biddingService from '../services/bidding-service';
import { WeatherWidget } from '../components/weather-widget';
import { BiddingPanel } from '../components/bidding-panel';
import { formatCurrency } from '../utils/format-currency';

export function LotDetailPage() {
  const { id } = useParams();
  const { token } = useAuth();
  const [lot, setLot] = useState(null);
  const [auctionResult, setAuctionResult] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    lotService.getLotById(id).then(setLot).catch((err) => setError(err.message || 'Could not load this lot.'));
    biddingService
      .getAuctionResult(id, token)
      .then(setAuctionResult)
      .catch(() => {
        // Auction row may not exist yet, or the call may fail independently of the lot itself —
        // the page still works with just lot-service's own currentHighestBid field.
      });
  }, [id, token]);

  useEffect(() => {
    load();
  }, [load]);

  if (error) {
    return (
      <div className="page">
        <p className="form-error">{error}</p>
      </div>
    );
  }

  if (!lot) {
    return (
      <div className="page">
        <p className="loading-text">Loading…</p>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">
            {lot.cropName} — Grade {lot.grade}
          </h1>
          <p className="page-subtitle">
            {lot.locationName} · {lot.quantity} {lot.unit} · by Farmer #{lot.farmerId}
          </p>
        </div>
      </div>

      <div className="lot-card-grid" style={{ gridTemplateColumns: '2fr 1fr' }}>
        <div className="card">
          <div className="card-body">
            {lot.imageUrl ? (
              <img
                src={lot.imageUrl}
                alt={lot.cropName}
                style={{ width: '100%', borderRadius: 'var(--radius)', marginBottom: 12 }}
              />
            ) : null}
            <p>{lot.saleType === 'AUCTION' ? 'Live auction' : 'Fixed-price sale'}</p>
            {lot.mandiReferencePrice ? (
              <p className="field-hint">Mandi reference price: {formatCurrency(lot.mandiReferencePrice)}</p>
            ) : null}
            <WeatherWidget weather={lot.weather} />
          </div>
        </div>

        <BiddingPanel lot={lot} auctionResult={auctionResult} />
      </div>
    </div>
  );
}
