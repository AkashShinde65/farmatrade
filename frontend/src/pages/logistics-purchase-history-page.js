import { useEffect, useState } from 'react';
import { useAuth } from '../hooks/auth-hook';
import * as logisticsService from '../services/logistics-service';
import { StatusBadge } from '../components/status-badge';
import { formatCurrency } from '../utils/format-currency';

export function LogisticsPurchaseHistoryPage() {
  const { user, token } = useAuth();
  const [requests, setRequests] = useState(null);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState({});
  const [acceptResults, setAcceptResults] = useState({});
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    let cancelled = false;
    logisticsService
      .getRequestsByBuyer(user.id, token)
      .then((data) => {
        if (!cancelled) setRequests(data);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Could not load purchase history.');
      });
    return () => {
      cancelled = true;
    };
  }, [user.id, token]);

  async function handleAccept(id) {
    setBusyId(id);
    setActionError((prev) => ({ ...prev, [id]: '' }));
    try {
      const result = await logisticsService.acceptRequest(id, token);
      setAcceptResults((prev) => ({ ...prev, [id]: result }));
      setRequests((prev) => prev.map((request) => (request.id === id ? { ...request, status: result.status } : request)));
    } catch (err) {
      setActionError((prev) => ({ ...prev, [id]: err.message || 'Could not accept.' }));
    } finally {
      setBusyId(null);
    }
  }

  async function handleDecline(id) {
    setBusyId(id);
    setActionError((prev) => ({ ...prev, [id]: '' }));
    try {
      const updated = await logisticsService.declineRequest(id, token);
      setRequests((prev) => prev.map((request) => (request.id === id ? updated : request)));
    } catch (err) {
      setActionError((prev) => ({ ...prev, [id]: err.message || 'Could not decline.' }));
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Purchase history</h1>
          <p className="page-subtitle">Lots you've won, amounts paid, and logistics choices.</p>
        </div>
      </div>

      {error ? <p className="form-error">{error}</p> : null}
      {requests === null && !error ? <p className="loading-text">Loading…</p> : null}
      {requests && requests.length === 0 ? <p className="empty-text">No lots won yet.</p> : null}

      {requests?.map((request) => (
        <div key={request.id} className="card" style={{ marginBottom: 16 }}>
          <div className="card-body">
            <div className="page-header" style={{ marginBottom: 8 }}>
              <div>
                <strong>{request.cropName}</strong>
                {request.quantity ? ` · ${request.quantity}` : ''} · {formatCurrency(request.totalAmount)}
              </div>
              <StatusBadge status={request.status} />
            </div>

            {request.status === 'PENDING_CHOICE' ? (
              <div style={{ display: 'flex', gap: 8 }}>
                <button
                  type="button"
                  className="btn btn-primary"
                  disabled={busyId === request.id}
                  onClick={() => handleAccept(request.id)}
                >
                  {busyId === request.id ? 'Working…' : 'Accept logistics'}
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  disabled={busyId === request.id}
                  onClick={() => handleDecline(request.id)}
                >
                  Decline
                </button>
              </div>
            ) : null}

            {actionError[request.id] ? <p className="form-error">{actionError[request.id]}</p> : null}

            {acceptResults[request.id] ? (
              <div style={{ marginTop: 12 }}>
                <p className="field-label">Truck booked</p>
                <p className="field-hint">
                  {acceptResults[request.id].truckBooking?.registrationNumber} —{' '}
                  {acceptResults[request.id].truckBooking?.distanceKm?.toFixed?.(1)} km away
                </p>
                <p className="field-label" style={{ marginTop: 8 }}>
                  Nearby cold storage
                </p>
                {acceptResults[request.id].coldStorageMatches?.map((match) => (
                  <p key={match.facilityId} className="field-hint">
                    {match.name} — {match.distanceKm.toFixed(1)} km, {match.capacityTons}t capacity
                  </p>
                ))}
              </div>
            ) : null}
          </div>
        </div>
      ))}
    </div>
  );
}
