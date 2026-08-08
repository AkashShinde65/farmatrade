import { useEffect, useState } from 'react';
import * as lotService from '../services/lot-service';
import { StatTile, StatTileRow } from '../components/stat-tile';
import { DataTable } from '../components/data-table';
import { StatusBadge } from '../components/status-badge';
import { formatCurrency } from '../utils/format-currency';

// Cross-service aggregation is limited by what's actually exposed: lot-service's public
// GET /api/lots already returns every lot across every farmer, so it's the real source here
// rather than a synthetic "admin overview" endpoint that doesn't exist.
export function AdminOversightPage() {
  const [lots, setLots] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    lotService
      .getAllLots()
      .then(setLots)
      .catch((err) => setError(err.message || 'Could not load lots.'));
  }, []);

  const sold = lots?.filter((lot) => lot.status === 'SOLD') ?? [];
  const revenue = sold.reduce((sum, lot) => sum + Number(lot.currentHighestBid ?? lot.fixedPrice ?? 0), 0);

  const columns = [
    { key: 'cropName', label: 'Crop' },
    { key: 'farmerId', label: 'Farmer', render: (row) => `#${row.farmerId}` },
    { key: 'quantity', label: 'Qty' },
    { key: 'saleType', label: 'Mode', render: (row) => (row.saleType === 'AUCTION' ? 'Auction' : 'Fixed price') },
    {
      key: 'price',
      label: 'Final',
      render: (row) => formatCurrency(row.saleType === 'AUCTION' ? row.currentHighestBid ?? row.basePrice : row.fixedPrice),
    },
    { key: 'winningBuyerId', label: 'Buyer', render: (row) => (row.winningBuyerId ? `#${row.winningBuyerId}` : '—') },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
  ];

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Oversight dashboard</h1>
          <p className="page-subtitle">Every lot, across all farmers and buyers.</p>
        </div>
      </div>

      <StatTileRow>
        <StatTile label="Total lots" value={lots?.length ?? '—'} />
        <StatTile label="Sold" value={sold.length} />
        <StatTile label="Revenue" value={formatCurrency(revenue)} />
      </StatTileRow>

      {error ? <p className="form-error">{error}</p> : null}
      {lots === null && !error ? <p className="loading-text">Loading…</p> : <DataTable columns={columns} rows={lots} />}
    </div>
  );
}
