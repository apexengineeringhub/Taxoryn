import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, Organization } from '../types';
import { authApi } from '../api/endpoints';
import { setAccessToken } from '../api/client';

interface AuthContextType {
  user: User | null;
  organization: Organization | null;
  practiceName: string;
  practiceInitials: string;
  subscriptionPlan: string;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<User>;
  logout: () => Promise<void> | void;
  setOrganization: (org: Organization | null) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const getInitials = (name?: string): string => {
  if (!name) return 'TX';
  const clean = name.replace(/[^a-zA-Z0-9\s]/g, '').trim();
  const words = clean.split(/\s+/).filter(Boolean);
  if (words.length === 0) return 'TX';
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return (words[0][0] + words[1][0]).toUpperCase();
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [organization, setOrganization] = useState<Organization | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Background session restoration via HttpOnly refresh cookie on application load
  useEffect(() => {
    let isMounted = true;

    const restoreSession = async () => {
      try {
        const data = await authApi.refreshToken();
        if (isMounted && data?.accessToken) {
          setAccessToken(data.accessToken);
          setUser(data.user);
          if (data.organization) {
            setOrganization(data.organization);
          } else if (data.user?.organizationName) {
            setOrganization({
              id: data.user.organizationId,
              name: data.user.organizationName,
            });
          }
        }
      } catch {
        if (isMounted) {
          setAccessToken(null);
          setUser(null);
          setOrganization(null);
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    restoreSession();

    return () => {
      isMounted = false;
    };
  }, []);

  const practiceName = organization?.name || user?.organizationName || 'Tax Practice Hub';
  const practiceInitials = getInitials(practiceName);
  const subscriptionPlan = organization?.subscriptionPlan || 'PROFESSIONAL';

  // Dynamic Browser Tab Title
  useEffect(() => {
    if (practiceName) {
      document.title = `${practiceName} | Tax Practice Management`;
    }
  }, [practiceName]);

  const login = async (email: string, password: string): Promise<User> => {
    setIsLoading(true);
    try {
      const data = await authApi.login({ email, password });
      // In-Memory Access Token ONLY (Never persisted to localStorage/sessionStorage)
      setAccessToken(data.accessToken);

      if (data.organization) {
        setOrganization(data.organization);
      } else if (data.user?.organizationName) {
        const dummyOrg: Organization = {
          id: data.user.organizationId,
          name: data.user.organizationName,
        };
        setOrganization(dummyOrg);
      }

      setUser(data.user);
      return data.user;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch (err) {
      console.warn('Backend logout completed with notice', err);
    } finally {
      // Clear in-memory token & state
      setAccessToken(null);
      localStorage.removeItem('taxoryn_user');
      localStorage.removeItem('taxoryn_org');
      setUser(null);
      setOrganization(null);
      window.location.href = '/login';
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        organization,
        practiceName,
        practiceInitials,
        subscriptionPlan,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
        setOrganization,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
