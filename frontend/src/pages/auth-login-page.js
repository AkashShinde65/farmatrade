import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/auth-hook';

const HOME_PATH_BY_ROLE = {
  FARMER: '/farmer/dashboard',
  BUYER: '/buyer/browse-lots',
  ADMIN: '/admin/oversight',
};

export function AuthLoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [role, setRole] = useState('FARMER');
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await login(role, { identifier, password });
      navigate(HOME_PATH_BY_ROLE[role]);
    } catch (err) {
      setError(err.message || 'Login failed.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card card">
        <div className="card-body">
          <div className="page-header">
            <h1 className="page-title">Welcome back</h1>
            <p className="page-subtitle">One login for farmers, buyers, and admins.</p>
          </div>

          <div className="role-tabs">
            {['FARMER', 'BUYER', 'ADMIN'].map((option) => (
              <button
                key={option}
                type="button"
                className={role === option ? 'active' : ''}
                onClick={() => setRole(option)}
              >
                {option.charAt(0) + option.slice(1).toLowerCase()}
              </button>
            ))}
          </div>

          <form className="form" onSubmit={handleSubmit}>
            <div className="field">
              <label className="field-label" htmlFor="identifier">Email or phone number</label>
              <input
                id="identifier"
                className="input"
                type="text"
                value={identifier}
                onChange={(event) => setIdentifier(event.target.value)}
                required
              />
            </div>
            <div className="field">
              <label className="field-label" htmlFor="password">Password</label>
              <input
                id="password"
                className="input"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
              />
            </div>
            {error ? <p className="form-error">{error}</p> : null}
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Logging in…' : 'Login'}
            </button>
          </form>

          <p>
            New to FarmaTrade? <Link to={`/register?role=${role}`}>Create an account</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
