import { useState } from 'react';
import { useAuth } from '../hooks/auth-hook';
import * as authService from '../services/auth-service';

export function AuthAdminCreatePage() {
  const { token } = useAuth();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [mobile, setMobile] = useState('');
  const [password, setPassword] = useState('');
  const [aadhaar, setAadhaar] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSuccess('');
    setSubmitting(true);
    try {
      await authService.register('ADMIN', { fullName, email, mobile, password, aadhaar }, token);
      setSuccess(`${fullName} was registered as an admin.`);
      setFullName('');
      setEmail('');
      setMobile('');
      setPassword('');
      setAadhaar('');
    } catch (err) {
      setError(err.message || 'Could not create admin.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Create admin</h1>
          <p className="page-subtitle">Grant another user admin access.</p>
        </div>
      </div>

      <div className="card" style={{ maxWidth: 480 }}>
        <div className="card-body">
          <form className="form" onSubmit={handleSubmit}>
            <div className="field">
              <label className="field-label" htmlFor="fullName">
                Full name
              </label>
              <input id="fullName" className="input" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
            </div>
            <div className="field">
              <label className="field-label" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                className="input"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label className="field-label" htmlFor="password">
                Password
              </label>
              <input
                id="password"
                className="input"
                type="password"
                minLength={10}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label className="field-label" htmlFor="mobile">
                Phone
              </label>
              <input id="mobile" className="input" value={mobile} onChange={(e) => setMobile(e.target.value)} required />
            </div>
            <div className="field">
              <label className="field-label" htmlFor="aadhaar">
                Aadhaar number
              </label>
              <input
                id="aadhaar"
                className="input"
                value={aadhaar}
                onChange={(e) => setAadhaar(e.target.value)}
                maxLength={12}
                required
              />
              <span className="field-hint">12 digits.</span>
            </div>
            {error ? <p className="form-error">{error}</p> : null}
            {success ? <p className="field-hint">{success}</p> : null}
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Creating…' : 'Create admin'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
