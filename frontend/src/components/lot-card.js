import { Link } from 'react-router-dom';
import { WeatherWidget } from './weather-widget';
import { formatCurrency } from '../utils/format-currency';

export function LotCard({ lot }) {
  return (
    <Link to={`/buyer/lots/${lot.id}`} className="lot-card">
      <h3>
        {lot.cropName} — Grade {lot.grade}
      </h3>
      <div className="lot-card-meta">
        {lot.locationName} · {lot.quantity} {lot.unit}
      </div>
      <div className="lot-card-meta">{lot.saleType === 'AUCTION' ? 'Auction' : 'Fixed price'}</div>
      <div className="stat-tile-value">
        {formatCurrency(lot.saleType === 'AUCTION' ? lot.currentHighestBid ?? lot.basePrice : lot.fixedPrice)}
      </div>
      {lot.mandiReferencePrice ? (
        <div className="field-hint">Mandi reference: {formatCurrency(lot.mandiReferencePrice)}</div>
      ) : null}
      <WeatherWidget weather={lot.weather} />
    </Link>
  );
}
