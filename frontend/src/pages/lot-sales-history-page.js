import { useEffect, useState } from 'react';
import { useAuth } from '../hooks/auth-hook';
import * as lotService from '../services/lot-service';
import * as billingService from '../services/billing-service';
import { DataTable } from '../components/data-table';
import { StatusBadge } from '../components/status-badge';
import { formatCurrency } from '../utils/format-currency';

export function LotSalesHistoryPage() {
  const { user, token } = useAuth();
  const [lots, setLots] = useState(null);
  // Keyed by lotId -- lets each sold lot show whether the buyer has actually paid yet, not just
  // that the lot itself sold. Fetched separately since lot-service has no notion of payment
  // status; that only exists in billing-service's own Invoice records.
  const [invoicesByLotId, setInvoicesByLotId] = useState({});
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    lotService
      .getLotsByFarmer(user.id, token)
      .then((data) => {
        if (!cancelled) setLots(data.filter((lot) => lot.status === 'SOLD'));
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Could not load sales history.');
      });
    billingService
      .getInvoicesByFarmer(user.id, token)
      .then((invoices) => {
        if (cancelled) return;
        const byLotId = {};
        invoices.forEach((invoice) => {
          byLotId[invoice.lotId] = invoice;
        });
        setInvoicesByLotId(byLotId);
      })
      .catch(() => {
        // Payment status is supplementary here -- the sales list itself still works without it.
      });
    return () => {
      cancelled = true;
    };
  }, [user.id, token]);

  const columns = [
    { key: 'cropName', label: 'Crop' },
    { key: 'quantity', label: 'Qty' },
    { key: 'saleType', label: 'Mode', render: (row) => (row.saleType === 'AUCTION' ? 'Auction' : 'Fixed price') },
    {
      key: 'price',
      label: 'Final price',
      render: (row) => formatCurrency(row.saleType === 'AUCTION' ? row.currentHighestBid : row.fixedPrice),
    },
    {
      key: 'winningBuyerId',
      label: 'Buyer',
      render: (row) => (row.winningBuyerId ? `Buyer #${row.winningBuyerId}` : '—'),
    },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
    {
      key: 'payment',
      label: 'Payment',
      render: (row) => {
        const invoice = invoicesByLotId[row.id];
        if (!invoice) return '—';
        return <StatusBadge status={invoice.status} />;
      },
    },
  ];

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Sales history</h1>
          <p className="page-subtitle">Past lots and final prices.</p>
        </div>
      </div>

      {error ? <p className="form-error">{error}</p> : null}
      {lots === null && !error ? (
        <p className="loading-text">Loading…</p>
      ) : (
        <DataTable columns={columns} rows={lots} emptyText="No sales yet." />
      )}
    </div>
  );
}
