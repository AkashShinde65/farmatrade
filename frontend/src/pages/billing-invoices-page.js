import { useEffect, useState } from 'react';
import { useAuth } from '../hooks/auth-hook';
import * as billingService from '../services/billing-service';
import { DataTable } from '../components/data-table';
import { StatusBadge } from '../components/status-badge';
import { formatCurrency } from '../utils/format-currency';

// Razorpay's client SDK is loaded on demand (only when a payment is actually started), via
// their standard hosted checkout.js — not bundled as an npm dependency.
function loadRazorpayScript() {
  return new Promise((resolve, reject) => {
    if (window.Razorpay) {
      resolve();
      return;
    }
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('Could not load Razorpay checkout.'));
    document.body.appendChild(script);
  });
}

export function BillingInvoicesPage() {
  const { user, token } = useAuth();
  const [invoices, setInvoices] = useState(null);
  const [error, setError] = useState('');
  const [payingId, setPayingId] = useState(null);

  function load() {
    billingService
      .getInvoicesByBuyer(user.id, token)
      .then(setInvoices)
      .catch((err) => setError(err.message || 'Could not load invoices.'));
  }

  useEffect(load, [user.id, token]);

  async function handlePay(invoice) {
    setError('');
    setPayingId(invoice.invoiceId);
    try {
      await loadRazorpayScript();
      const order = await billingService.createOrder(invoice.invoiceId, token);
      const razorpay = new window.Razorpay({
        key: order.keyId,
        amount: Math.round(order.amount * 100),
        currency: order.currency,
        order_id: order.orderId,
        name: 'FarmaTrade',
        description: invoice.invoiceNumber,
        // Confirms payment via our backend directly (client-side verification) instead of
        // waiting on Razorpay's webhook, which can't reach a local, non-public backend.
        handler: async (response) => {
          try {
            await billingService.verifyPayment(
              {
                razorpayOrderId: response.razorpay_order_id,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature,
              },
              token,
            );
          } catch (err) {
            setError(err.message || 'Payment succeeded but could not be confirmed. Please refresh.');
          } finally {
            load();
          }
        },
        modal: { ondismiss: () => setPayingId(null) },
      });
      razorpay.open();
    } catch (err) {
      setError(err.message || 'Could not start payment.');
    } finally {
      setPayingId(null);
    }
  }

  async function handleDownload(invoice) {
    try {
      const blob = await billingService.getInvoicePdf(invoice.invoiceId, token);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `invoice-${invoice.invoiceId}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.message || 'Could not download the invoice.');
    }
  }

  const columns = [
    { key: 'invoiceNumber', label: 'Invoice' },
    { key: 'cropName', label: 'Crop' },
    { key: 'totalAmount', label: 'Total', render: (row) => formatCurrency(row.totalAmount) },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
    {
      key: 'actions',
      label: '',
      render: (row) => (
        <div style={{ display: 'flex', gap: 8 }}>
          {row.status === 'PENDING' ? (
            <button type="button" className="btn btn-primary" disabled={payingId === row.invoiceId} onClick={() => handlePay(row)}>
              {payingId === row.invoiceId ? 'Opening…' : 'Pay now'}
            </button>
          ) : null}
          <button type="button" className="btn btn-secondary" onClick={() => handleDownload(row)}>
            PDF
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Invoices</h1>
          <p className="page-subtitle">Payments for lots you've won.</p>
        </div>
      </div>

      {error ? <p className="form-error">{error}</p> : null}
      {invoices === null && !error ? (
        <p className="loading-text">Loading…</p>
      ) : (
        <DataTable columns={columns} rows={invoices} emptyText="No invoices yet." />
      )}
    </div>
  );
}
