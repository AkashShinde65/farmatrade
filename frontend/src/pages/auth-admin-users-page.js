import { useEffect, useState } from 'react';
import { useAuth } from '../hooks/auth-hook';
import * as authService from '../services/auth-service';
import { DataTable } from '../components/data-table';
import { StatusBadge } from '../components/status-badge';

const ROLES = ['FARMER', 'BUYER', 'ADMIN'];

export function AuthAdminUsersPage() {
  const { token } = useAuth();
  const [role, setRole] = useState('FARMER');
  const [page, setPage] = useState(null);
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);
  const [profileUser, setProfileUser] = useState(null);

  function load() {
    authService
      .listUsers(token, { search, role })
      .then(setPage)
      .catch((err) => setError(err.message || 'Could not load users.'));
  }

  useEffect(load, [token, role]); // eslint-disable-line react-hooks/exhaustive-deps

  async function handleToggle(user) {
    setBusyId(user.id);
    try {
      await authService.setUserStatus(token, user.id, !user.enabled);
      load();
    } catch (err) {
      setError(err.message || 'Could not update status.');
    } finally {
      setBusyId(null);
    }
  }

  const columns = [
    { key: 'fullName', label: 'Name' },
    { key: 'email', label: 'Email' },
    { key: 'enabled', label: 'Status', render: (row) => <StatusBadge status={row.enabled ? 'ENABLED' : 'DISABLED'} /> },
    {
      key: 'actions',
      label: '',
      render: (row) => (
        <div style={{ display: 'flex', gap: 8 }}>
          <button type="button" className="btn btn-secondary" onClick={() => setProfileUser(row)}>
            Profile
          </button>
          <button type="button" className="btn btn-secondary" disabled={busyId === row.id} onClick={() => handleToggle(row)}>
            {row.enabled ? 'Disable' : 'Enable'}
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Users</h1>
          <p className="page-subtitle">Search and manage accounts.</p>
        </div>
      </div>

      <div className="role-tabs" style={{ marginBottom: 16 }}>
        {ROLES.map((option) => (
          <button key={option} type="button" className={role === option ? 'active' : ''} onClick={() => setRole(option)}>
            {option.charAt(0) + option.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      <div className="field" style={{ maxWidth: 320 }}>
        <input
          className="input"
          placeholder="Search by name or email"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && load()}
        />
      </div>

      {error ? <p className="form-error">{error}</p> : null}
      {page === null && !error ? (
        <p className="loading-text">Loading…</p>
      ) : (
        <DataTable columns={columns} rows={page.content} emptyText={`No ${role.toLowerCase()}s found.`} />
      )}

      {profileUser ? (
        <div
          role="dialog"
          aria-modal="true"
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0, 0, 0, 0.4)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 100,
          }}
          onClick={() => setProfileUser(null)}
        >
          <div className="card" style={{ width: 420, maxWidth: '90vw' }} onClick={(e) => e.stopPropagation()}>
            <div className="card-body">
              <div className="page-header" style={{ marginBottom: 12 }}>
                <h2 className="page-title" style={{ fontSize: '1.25rem' }}>
                  Profile
                </h2>
                <button type="button" className="btn btn-secondary" onClick={() => setProfileUser(null)}>
                  Close
                </button>
              </div>

              <ProfileField label="Full name" value={profileUser.fullName} />
              <ProfileField label="Email" value={profileUser.email} />
              <ProfileField label="Phone" value={profileUser.mobile} />
              <ProfileField label="Aadhaar number" value={profileUser.aadhaar} />
              <ProfileField label="Password" value="***" />
              <ProfileField label="Role" value={profileUser.role} />
              <ProfileField label="Status" value={profileUser.enabled ? 'Enabled' : 'Disabled'} />
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function ProfileField({ label, value }) {
  return (
    <div style={{ marginBottom: 10 }}>
      <div className="field-label">{label}</div>
      <div>{value ?? '—'}</div>
    </div>
  );
}
