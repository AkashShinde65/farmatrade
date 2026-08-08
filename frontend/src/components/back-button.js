import { useLocation, useNavigate } from 'react-router-dom';

// Shown at the top of every page except the home page, so the user can always get back to
// wherever they came from without hunting for a nav link. Uses real browser history (back()),
// not a hardcoded "parent route", so it correctly reverses whatever path the user actually took.
export function BackButton() {
  const navigate = useNavigate();
  const location = useLocation();

  if (location.pathname === '/') {
    return null;
  }

  return (
    <div className="back-button-row">
      <button type="button" className="btn btn-secondary back-button" onClick={() => navigate(-1)}>
        ← Back
      </button>
    </div>
  );
}
