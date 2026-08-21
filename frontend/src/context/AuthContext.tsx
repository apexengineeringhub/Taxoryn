import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, Organization } from '../types';
import { authApi } from '../api/endpoints';

interface AuthContextType {
  user: User | null;
  organization: Organization | null;
  practiceName: string;
  practiceInitials: string;
  subscriptionPlan: string;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<User>;
  logout: () => void;
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

  useEffect(() => {
    const savedUser = localStorage.getItem('taxoryn_user');
    const savedOrg = localStorage.getItem('taxoryn_org');
    const token = localStorage.getItem('taxoryn_access_token');

    if (savedUser && token) {
      try {
        setUser(JSON.parse(savedUser));
      } catch (e) {
        localStorage.removeItem('taxoryn_user');
      }
    }

    if (savedOrg) {
      try {
        setOrganization(JSON.parse(savedOrg));
      } catch (e) {
        localStorage.removeItem('taxoryn_org');
      }
    }

    setIsLoading(false);
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

  const login = async (email: string, password: string) => {
    setIsLoading(true);
    try {
      const data = await authApi.login({ email, password });
      localStorage.setItem('taxoryn_access_token', data.accessToken);
      localStorage.setItem('taxoryn_refresh_token', data.refreshToken);
      localStorage.setItem('taxoryn_user', JSON.stringify(data.user));

      if (data.organization) {
        localStorage.setItem('taxoryn_org', JSON.stringify(data.organization));
        setOrganization(data.organization);
      } else if (data.user?.organizationName) {
        const dummyOrg: Organization = {
          id: data.user.organizationId,
          name: data.user.organizationName,
        };
        localStorage.setItem('taxoryn_org', JSON.stringify(dummyOrg));
        setOrganization(dummyOrg);
      }

      setUser(data.user);
      return data.user;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('taxoryn_access_token');
    localStorage.removeItem('taxoryn_refresh_token');
    localStorage.removeItem('taxoryn_user');
    localStorage.removeItem('taxoryn_org');
    setUser(null);
    setOrganization(null);
    window.location.href = '/login';
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
