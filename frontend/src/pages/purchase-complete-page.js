import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/auth-hook';
import * as logisticsService from '../services/logistics-service';
import * as billingService from '../services/billing-service';
import { StatusBadge } from '../components/status-badge';
import { formatCurrency } from '../utils/format-currency';

// Razorpay's client SDK, loaded on demand -- same approach as billing-invoices-page.
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

// Shown right after Buy Now succeeds. The logistics request and (once a choice is made) the
// invoice already exist on the backend by the time this page loads -- both are created
// synchronously as part of the sale-closing chain -- so this page just walks the buyer through
// deciding on logistics, then paying, then getting the bill, without needing separate visits to
// the Purchase History and Invoices pages. Those pages still exist for reviewing past purchases
// (including auction wins, which resolve later and can't land here directly).
export function PurchaseCompletePage() {
  const { lotId } = useParams();
  const { user, token } = useAuth();

  const [request, setRequest] = useState(null);
  const [invoice, setInvoice] = useState(null);
  const [acceptResult, setAcceptResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const loadRequest = useCallback(() => {
    return logisticsService
      .getRequestsByBuyer(user.id, token)
      .then((all) => {
        const match = all.find((r) => String(r.lotId) === String(lotId));
        setRequest(match || null);
      })
      .catch((err) => setError(err.message || 'Could not load your purchase.'));
  }, [lotId, user.id, token]);

  const loadInvoice = useCallback(
    (saleId) => {
      billingService
        .getInvoicesByBuyer(user.id, token)
        .then((all) => setInvoice(all.find((inv) => String(inv.saleId) === String(saleId)) || null))
        .catch(() => {
          // Invoice may not exist yet the instant logistics is decided -- the buyer can still
          // retry by revisiting Purchase History later.
        });
    },
    [user.id, token],
  );

  useEffect(() => {
    loadRequest().finally(() => setLoading(false));
  }, [loadRequest]);

  useEffect(() => {
    if (request && request.status !== 'PENDING_CHOICE') {
      loadInvoice(request.saleId);
    }
  }, [request, loadInvoice]);

  async function handleAccept() {
    setBusy(true);
    setError('');
    try {
      const result = await logisticsService.acceptRequest(request.id, token);
      setAcceptResult(result);
      setRequest((prev) => ({ ...prev, status: result.status }));
      loadInvoice(request.saleId);
    } catch (err) {
      setError(err.message || 'Could not accept logistics.');
    } finally {
      setBusy(false);
    }
  }

  async function handleDecline() {
    setBusy(true);
    setError('');
    try {
      const updated = await logisticsService.declineRequest(request.id, token);
      setRequest(updated);
      loadInvoice(request.saleId);
    } catch (err) {
      setError(err.message || 'Could not decline logistics.');
    } finally {
      setBusy(false);
    }
  }

  async function handlePay() {
    setBusy(true);
    setError('');
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
        // waiting on Razorpay's webhook, which can't reach a local, non-public backend. Razorpay
        // hands these three values to the browser only once the payment genuinely succeeded.
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
            loadInvoice(request.saleId);
          }
        },
        modal: { ondismiss: () => setBusy(false) },
      });
      razorpay.open();
    } catch (err) {
      setError(err.message || 'Could not start payment.');
    } finally {
      setBusy(false);
    }
  }

  async function handleDownload() {
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

  if (loading) {
    return (
      <div className="page">
        <p className="loading-text">Loading…</p>
      </div>
    );
  }

  if (!request) {
    return (
      <div className="page">
        <p className="form-error">{error || 'No purchase found for this lot.'}</p>
        <Link to="/buyer/purchase-history" className="btn btn-secondary" style={{ marginTop: 12 }}>
          Go to purchase history
        </Link>
      </div>
    );
  }

  const logisticsDecided = request.status !== 'PENDING_CHOICE';

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Purchase complete</h1>
          <p className="page-subtitle">
            {request.cropName} · {formatCurrency(request.totalAmount)}
          </p>
        </div>
      </div>

      {error ? <p className="form-error">{error}</p> : null}

      <div className="card" style={{ marginBottom: 16 }}>
        <div className="card-body">
          <div className="page-header" style={{ marginBottom: 8 }}>
            <h3 style={{ margin: 0 }}>1. Logistics</h3>
            <StatusBadge status={request.status} />
          </div>

          {!logisticsDecided ? (
            <>
              <p className="field-hint">
                Would you like FarmaTrade to arrange cold storage and truck pickup for this lot?
              </p>
              <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
                <button type="button" className="btn btn-primary" disabled={busy} onClick={handleAccept}>
                  {busy ? 'Working…' : 'Accept logistics'}
                </button>
                <button type="button" className="btn btn-secondary" disabled={busy} onClick={handleDecline}>
                  Decline
                </button>
              </div>
            </>
          ) : (
            <p className="field-hint">
              {request.status === 'REQUESTED' ? 'Logistics accepted.' : 'Logistics declined.'}
            </p>
          )}

          {acceptResult ? (
            <div style={{ marginTop: 12 }}>
              <p className="field-label">Truck booked</p>
              <p className="field-hint">
                {acceptResult.truckBooking?.registrationNumber} —{' '}
                {acceptResult.truckBooking?.distanceKm?.toFixed?.(1)} km away
              </p>
              <p className="field-label" style={{ marginTop: 8 }}>
                Nearby cold storage
              </p>
              {acceptResult.coldStorageMatches?.map((match) => (
                <p key={match.facilityId} className="field-hint">
                  {match.name} — {match.distanceKm.toFixed(1)} km, {match.capacityTons}t capacity
                </p>
              ))}
            </div>
          ) : null}
        </div>
      </div>

      {logisticsDecided ? (
        <div className="card" style={{ marginBottom: 16 }}>
          <div className="card-body">
            <h3 style={{ marginTop: 0 }}>2. Payment</h3>
            {!invoice ? (
              <p className="loading-text">Preparing your invoice…</p>
            ) : (
              <>
                <div className="page-header" style={{ marginBottom: 8 }}>
                  <p className="field-hint" style={{ margin: 0 }}>
                    Invoice {invoice.invoiceNumber} — {formatCurrency(invoice.totalAmount)}
                  </p>
                  <StatusBadge status={invoice.status} />
                </div>
                {invoice.status === 'PENDING' ? (
                  <button type="button" className="btn btn-primary" disabled={busy} onClick={handlePay}>
                    {busy ? 'Opening…' : 'Pay now'}
                  </button>
                ) : null}
              </>
            )}
          </div>
        </div>
      ) : null}

      {invoice ? (
        <div className="card">
          <div className="card-body">
            <h3 style={{ marginTop: 0 }}>3. Your bill</h3>
            <button type="button" className="btn btn-secondary" onClick={handleDownload}>
              Download invoice PDF
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
