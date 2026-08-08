import { createApiClient } from './http';

const client = createApiClient(process.env.REACT_APP_AUTH_URL || 'http://localhost:8081');

const LOGIN_PATH_BY_ROLE = {
  FARMER: '/api/auth/login/farmer',
  BUYER: '/api/auth/login/buyer',
  ADMIN: '/api/auth/login/admin',
};

const REGISTER_PATH_BY_ROLE = {
  FARMER: '/api/auth/register/farmer',
  BUYER: '/api/auth/register/buyer',
  ADMIN: '/api/auth/register/admin',
};

export function login(role, { identifier, password }) {
  return client.post(LOGIN_PATH_BY_ROLE[role], { identifier, password });
}

export function register(role, { fullName, email, mobile, password, aadhaar }, token) {
  return client.post(REGISTER_PATH_BY_ROLE[role], { fullName, email, mobile, password, aadhaar }, token);
}

export function getMe(token) {
  return client.get('/api/auth/me', token);
}

export function listUsers(token, { search, role, enabled, page = 0, size = 20 } = {}) {
  const params = new URLSearchParams({ page, size });
  if (search) params.set('search', search);
  if (role) params.set('role', role);
  if (enabled !== undefined && enabled !== '') params.set('enabled', enabled);
  return client.get(`/api/admin/users?${params.toString()}`, token);
}

export function setUserStatus(token, id, enabled) {
  return client.patch(`/api/admin/users/${id}/status`, { enabled }, token);
}

export function listAuditEvents(token, page = 0, size = 20) {
  const params = new URLSearchParams({ page, size });
  return client.get(`/api/admin/audit-events?${params.toString()}`, token);
}
