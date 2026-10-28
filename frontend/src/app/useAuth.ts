import { useContext } from 'react';
import { AuthContext, type AuthValue } from './authState';

export function useAuth(): AuthValue {
  const value = useContext(AuthContext);
  if (!value) throw new Error('AuthProvider is missing');
  return value;
}
