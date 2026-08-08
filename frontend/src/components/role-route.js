import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/auth-hook';

export function RoleRoute({ allow }) {
  const { role } = useAuth();
  if (!allow.includes(role)) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
