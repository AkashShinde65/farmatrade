// Copy-paste template only — not imported anywhere. The live version is components/navbar.js.
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/auth-hook';

const NAV_LINKS_BY_ROLE = {
  FARMER: [
    { to: '/farmer/dashboard', label: 'Dashboard' },
    { to: '/farmer/create-lot', label: 'Create Lot' },
    { to: '/farmer/sales-history', label: 'Sales History' },
  ],
  BUYER: [
    { to: '/buyer/browse-lots', label: 'Browse Lots' },
    { to: '/buyer/purchase-history', label: 'Purchase History' },
    { to: '/buyer/invoices', label: 'Invoices' },
  ],
  ADMIN: [
    { to: '/admin/oversight', label: 'Oversight' },
    { to: '/admin/users', label: 'Users' },
    { to: '/admin/audit-log', label: 'Audit Log' },
    { to: '/admin/create-admin', label: 'Create Admin' },
  ],
};

export function Navbar() {
  const { isAuthenticated, role, user, logout } = useAuth();
  const navigate = useNavigate();
  const links = role ? NAV_LINKS_BY_ROLE[role] || [] : [];

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M7 20h10" />
            <path d="M10 20c5.5-2.5.8-6.4 3-10" />
            <path d="M9.5 9.4c1.1.8 1.8 2.2 2.3 3.7-2 .4-3.5.4-4.8-.2-1.2-.6-2.3-1.9-3-4.2 2.8-.5 4.4 0 5.5.7z" />
            <path d="M14.1 6c-.7 1.5-1.4 2.4-2.3 3.2C10 7.5 9.3 6 9 4.2c1.8-.4 3.6.2 5.1 1.8z" />
          </svg>
          FarmaTrade
        </Link>

        {isAuthenticated ? (
          <nav className="navbar-links">
            {links.map((link) => (
              <Link key={link.to} to={link.to}>
                {link.label}
              </Link>
            ))}
          </nav>
        ) : null}

        <div className="navbar-right">
          {isAuthenticated ? (
            <>
              <span className="navbar-user">
                {user?.fullName} · {role}
              </span>
              <button type="button" className="btn btn-secondary" onClick={handleLogout}>
                Logout
              </button>
            </>
          ) : (
            <div className="navbar-actions">
              <Link to="/login">Login</Link>
              <Link to="/register" className="btn btn-primary">
                Get Started
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
