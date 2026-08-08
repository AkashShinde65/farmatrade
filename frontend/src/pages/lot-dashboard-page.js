import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/auth-hook';
import * as lotService from '../services/lot-service';
import { StatTile, StatTileRow } from '../components/stat-tile';
import { DataTable } from '../components/data-table';
import { StatusBadge } from '../components/status-badge';
import { formatCurrency } from '../utils/format-currency';

export function LotDashboardPage() {
  const { user, token } = useAuth();
  const navigate = useNavigate();
  const [lots, setLots] = useState(null);
  const [error, setError] = useState('');
  const [deletingId, setDeletingId] = useState(null);
  // Tracks which row is showing its "are you sure?" state -- an inline confirm instead of
  // window.confirm(), since a native confirm dialog is easy to dismiss without noticing and
  // leaves no visible trace in the page if that happens.
  const [confirmingId, setConfirmingId] = useState(null);

  function load() {
    lotService
      .getLotsByFarmer(user.id, token)
      .then(setLots)
      .catch((err) => setError(err.message || 'Could not load your lots.'));
  }

  useEffect(load, [user.id, token]); // eslint-disable-line react-hooks/exhaustive-deps

  async function handleDelete(lot) {
    setConfirmingId(null);
    setDeletingId(lot.id);
    setError('');
    try {
      await lotService.cancelLot(lot.id, token);
      load();
    } catch (err) {
      setError(err.message || 'Could not delete this lot.');
    } finally {
      setDeletingId(null);
    }
  }

  const listed = lots?.filter((lot) => lot.status === 'LISTED' || lot.status === 'ACTIVE').length ?? 0;
  const sold = lots?.filter((lot) => lot.status === 'SOLD') ?? [];
  const expired = lots?.filter((lot) => lot.status === 'EXPIRED' || lot.status === 'CANCELLED').length ?? 0;
  const revenue = sold.reduce((sum, lot) => sum + Number(lot.currentHighestBid ?? lot.fixedPrice ?? 0), 0);

  const columns = [
    { key: 'cropName', label: 'Crop' },
    { key: 'grade', label: 'Grade' },
    { key: 'quantity', label: 'Qty' },
    { key: 'saleType', label: 'Mode', render: (row) => (row.saleType === 'AUCTION' ? 'Auction' : 'Fixed price') },
    {
      key: 'price',
      label: 'Price / Bid',
      render: (row) => formatCurrency(row.saleType === 'AUCTION' ? row.currentHighestBid ?? row.basePrice : row.fixedPrice),
    },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
    {
      key: 'actions',
      label: '',
      render: (row) => {
        const locked = row.status === 'SOLD' || row.status === 'CANCELLED';

        if (confirmingId === row.id) {
          return (
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <span className="field-hint">Delete this lot?</span>
              <button type="button" className="btn btn-primary" disabled={deletingId === row.id} onClick={() => handleDelete(row)}>
                {deletingId === row.id ? 'Deleting…' : 'Yes, delete'}
              </button>
              <button type="button" className="btn btn-secondary" onClick={() => setConfirmingId(null)}>
                Cancel
              </button>
            </div>
          );
        }

        return (
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              type="button"
              className="btn btn-secondary"
              disabled={locked}
              onClick={() => navigate(`/farmer/lots/${row.id}/edit`)}
            >
              Update
            </button>
            <button type="button" className="btn btn-secondary" disabled={locked} onClick={() => setConfirmingId(row.id)}>
              Delete
            </button>
          </div>
        );
      },
    },
  ];

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Your lots</h1>
          <p className="page-subtitle">Manage your produce listings.</p>
        </div>
        <Link to="/farmer/create-lot" className="btn btn-primary">
          Create lot
        </Link>
      </div>

      <StatTileRow>
        <StatTile label="Listed" value={listed} />
        <StatTile label="Sold" value={sold.length} />
        <StatTile label="Expired" value={expired} />
        <StatTile label="Revenue" value={formatCurrency(revenue)} />
      </StatTileRow>

      {error ? <p className="form-error">{error}</p> : null}
      {lots === null && !error ? (
        <p className="loading-text">Loading…</p>
      ) : (
        <DataTable columns={columns} rows={lots} emptyText="No lots yet — create your first one." />
      )}
    </div>
  );
}
