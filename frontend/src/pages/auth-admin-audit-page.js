import { useEffect, useState } from 'react';
import { useAuth } from '../hooks/auth-hook';
import * as authService from '../services/auth-service';
import { DataTable } from '../components/data-table';
import { formatDateTime } from '../utils/format-date';

export function AuthAdminAuditPage() {
  const { token } = useAuth();
  const [page, setPage] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    authService
      .listAuditEvents(token)
      .then(setPage)
      .catch((err) => setError(err.message || 'Could not load audit events.'));
  }, [token]);

  const columns = [
    { key: 'eventType', label: 'Event' },
    { key: 'actingUserId', label: 'Actor', render: (row) => (row.actingUserId ? `#${row.actingUserId}` : '—') },
    { key: 'targetUserId', label: 'Target', render: (row) => (row.targetUserId ? `#${row.targetUserId}` : '—') },
    { key: 'description', label: 'Description' },
    { key: 'createdAt', label: 'When', render: (row) => formatDateTime(row.createdAt) },
  ];

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Audit log</h1>
          <p className="page-subtitle">Security-relevant account events.</p>
        </div>
      </div>

      {error ? <p className="form-error">{error}</p> : null}
      {page === null && !error ? (
        <p className="loading-text">Loading…</p>
      ) : (
        <DataTable columns={columns} rows={page.content} emptyText="No events yet." />
      )}
    </div>
  );
}
