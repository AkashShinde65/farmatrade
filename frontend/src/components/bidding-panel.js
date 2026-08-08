import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/auth-hook';
import * as biddingService from '../services/bidding-service';
import { useAuctionSocket } from '../hooks/bidding-socket-hook';
import { formatCurrency } from '../utils/format-currency';

function formatCountdown(msRemaining) {
  if (msRemaining <= 0) {
    return 'Ending…';
  }
  const totalSeconds = Math.floor(msRemaining / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const pad = (n) => String(n).padStart(2, '0');
  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(seconds)}` : `${pad(minutes)}:${pad(seconds)}`;
}

export function BiddingPanel({ lot, auctionResult }) {
  const { token, role, user } = useAuth();
  const navigate = useNavigate();
  const liveEvent = useAuctionSocket(lot.id);
  const [bidAmount, setBidAmount] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [now, setNow] = useState(Date.now());

  const endTime = liveEvent?.auctionEndTime ?? auctionResult?.endTime;

  // Ticks once a second purely to re-render the countdown -- the actual remaining time is always
  // computed fresh from endTime, this just triggers the recalculation on a clock.
  useEffect(() => {
    if (!endTime) return undefined;
    const interval = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(interval);
  }, [endTime]);

  // If this buyer's own bid wins, take them straight into the same logistics/payment flow
  // Buy Now uses -- they don't have to go hunting through Purchase History for it.
  useEffect(() => {
    if (liveEvent?.eventType === 'WINNER_ANNOUNCED' && String(liveEvent.winnerId) === String(user?.id)) {
      navigate(`/buyer/lots/${lot.id}/purchase-complete`);
    }
  }, [liveEvent, user?.id, lot.id, navigate]);

  if (role !== 'BUYER') {
    return null;
  }

  const currentHighest = liveEvent?.highestBid ?? auctionResult?.winningAmount ?? lot.currentHighestBid ?? lot.basePrice;
  const isClosed =
    liveEvent?.eventType === 'AUCTION_CLOSED' ||
    liveEvent?.eventType === 'WINNER_ANNOUNCED' ||
    auctionResult?.status === 'CLOSED' ||
    auctionResult?.status === 'SOLD';
  const msRemaining = endTime ? new Date(endTime).getTime() - now : null;

  async function handleBid(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await biddingService.placeBid(lot.id, Number(bidAmount), token);
      setBidAmount('');
    } catch (err) {
      setError(err.message || 'Could not place bid.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleBuyNow() {
    setError('');
    setSubmitting(true);
    try {
      await biddingService.buyNow(lot.id, crypto.randomUUID(), token);
      // The logistics request for this lot already exists by the time this call returns --
      // bidding-service notifies logistics-service synchronously as part of closing the sale --
      // so it's safe to go straight to the accept/decline + payment flow.
      navigate(`/buyer/lots/${lot.id}/purchase-complete`);
    } catch (err) {
      setError(err.message || 'Could not complete the purchase.');
      setSubmitting(false);
    }
  }

  if (lot.saleType === 'FIXED_PRICE') {
    return (
      <div className="card">
        <div className="card-body">
          <div className="stat-tile-label">Price</div>
          <div className="stat-tile-value">{formatCurrency(lot.fixedPrice)}</div>
          {error ? <p className="form-error">{error}</p> : null}
          <button type="button" className="btn btn-primary" style={{ marginTop: 12 }} onClick={handleBuyNow} disabled={submitting || isClosed}>
            {isClosed ? 'Sold' : submitting ? 'Processing…' : 'Buy now'}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="card-body">
        <div className="stat-tile-label">Current highest bid</div>
        <div className="stat-tile-value">{formatCurrency(currentHighest)}</div>
        {!isClosed && msRemaining !== null ? (
          <p className="field-hint">Time remaining: {formatCountdown(msRemaining)}</p>
        ) : null}
        {isClosed ? (
          <p className="field-hint">This auction has closed.</p>
        ) : (
          <form className="form" style={{ maxWidth: 'none', marginTop: 12 }} onSubmit={handleBid}>
            <div className="field">
              <label className="field-label" htmlFor="bidAmount">
                Your bid (₹)
              </label>
              <input
                id="bidAmount"
                className="input"
                type="number"
                min={Number(currentHighest) + 0.01}
                step="0.01"
                value={bidAmount}
                onChange={(e) => setBidAmount(e.target.value)}
                required
              />
            </div>
            {error ? <p className="form-error">{error}</p> : null}
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Placing…' : 'Place bid'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
