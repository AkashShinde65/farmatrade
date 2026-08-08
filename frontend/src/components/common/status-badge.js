// Copy-paste template only — not imported anywhere. The live version is components/status-badge.js.
const TONE_BY_STATUS = {
  LISTED: 'neutral',
  ACTIVE: 'neutral',
  SOLD: 'success',
  CANCELLED: 'danger',
  EXPIRED: 'danger',
  PENDING: 'warning',
  PENDING_CHOICE: 'warning',
  REQUESTED: 'success',
  DECLINED: 'danger',
  PAID: 'success',
  FAILED: 'danger',
  SETTLED: 'success',
  ACCEPTED: 'success',
  BOOKED: 'success',
  PICKED_UP: 'success',
  DELIVERED: 'success',
  ENABLED: 'success',
  DISABLED: 'danger',
};

export function StatusBadge({ status, tone }) {
  const resolvedTone = tone || TONE_BY_STATUS[status] || 'neutral';
  const className = resolvedTone === 'neutral' ? 'status-badge' : `status-badge status-badge--${resolvedTone}`;
  return <span className={className}>{status}</span>;
}
