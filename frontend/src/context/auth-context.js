import { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import * as authService from '../services/auth-service';
import { isTokenExpired } from '../utils/jwt';

const STORAGE_KEY = 'farmatrade.auth';

export const AuthContext = createContext(null);

function readStoredAuth() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (!parsed?.token || isTokenExpired(parsed.token)) return null;
    return parsed;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => readStoredAuth());

  useEffect(() => {
    if (auth) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  }, [auth]);

  const login = useCallback(async (role, credentials) => {
    const response = await authService.login(role, credentials);
    const nextAuth = { token: response.accessToken, role, user: response.user };
    setAuth(nextAuth);
    return nextAuth;
  }, []);

  // Registration doesn't return a token, so we log in right after with the same
  // credentials rather than making the user submit a second form.
  const registerAndLogin = useCallback(
    async (role, details) => {
      await authService.register(role, details);
      return login(role, { identifier: details.email, password: details.password });
    },
    [login]
  );

  const logout = useCallback(() => {
    setAuth(null);
  }, []);

  const value = useMemo(
    () => ({
      token: auth?.token ?? null,
      role: auth?.role ?? null,
      user: auth?.user ?? null,
      isAuthenticated: Boolean(auth?.token),
      login,
      registerAndLogin,
      logout,
    }),
    [auth, login, registerAndLogin, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
