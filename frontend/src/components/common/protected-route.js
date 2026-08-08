// Copy-paste template only — not imported anywhere. The live version is components/protected-route.js.
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../hooks/auth-hook';

export function ProtectedRoute() {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return <Outlet />;
}
