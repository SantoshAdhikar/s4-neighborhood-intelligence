import { createContext } from 'react';
import type { Account } from '../services/authApi';

export interface AuthValue {
  account: Account | null;
  loading: boolean;
  signIn(email: string, password: string): Promise<void>;
  signOut(): Promise<void>;
}

export const AuthContext = createContext<AuthValue | null>(null);
