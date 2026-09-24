import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import { useAuth } from './AuthContext';
import { moduleConfigApi } from '../api/endpoints';
import { isPlatformUser } from '../utils/permissionUtils';
import type {
  ProductModuleCode,
  OrganizationModule,
  ModuleAccessStatus,
} from '../types/index';

interface ModuleEntitlementContextType {
  modules: Record<ProductModuleCode, OrganizationModule>;
  modulesList: OrganizationModule[];
  isLoading: boolean;
  error: string | null;
  isModuleAvailable: (code?: ProductModuleCode) => boolean;
  getModuleAccessStatus: (code?: ProductModuleCode) => ModuleAccessStatus;
  getModuleReason: (code?: ProductModuleCode) => string | undefined;
  getModule: (code?: ProductModuleCode) => OrganizationModule | undefined;
  refreshEntitlements: () => Promise<void>;
}

const ModuleEntitlementContext = createContext<ModuleEntitlementContextType | undefined>(undefined);

export const ModuleEntitlementProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, isAuthenticated } = useAuth();
  const [modules, setModules] = useState<Record<ProductModuleCode, OrganizationModule>>({} as Record<ProductModuleCode, OrganizationModule>);
  const [modulesList, setModulesList] = useState<OrganizationModule[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const isSuperOrPlatform = useMemo(() => isPlatformUser(user), [user]);

  const fetchEntitlements = useCallback(async () => {
    if (!isAuthenticated || !user?.organizationId) {
      setModules({} as Record<ProductModuleCode, OrganizationModule>);
      setModulesList([]);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const data = await moduleConfigApi.getOrganizationModules();
      setModulesList(data || []);

      const record = (data || []).reduce<Record<ProductModuleCode, OrganizationModule>>((acc, mod) => {
        acc[mod.moduleCode] = mod;
        return acc;
      }, {} as Record<ProductModuleCode, OrganizationModule>);

      setModules(record);
    } catch (err: any) {
      // Non-fatal: log warning and keep existing state
      console.warn('Failed to load organization module entitlements:', err?.message || err);
      setError(err?.response?.data?.message || 'Failed to load module entitlements');
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated, user?.organizationId]);

  useEffect(() => {
    fetchEntitlements();
  }, [fetchEntitlements]);

  const isModuleAvailable = useCallback(
    (code?: ProductModuleCode): boolean => {
      if (!code) return true;
      if (isSuperOrPlatform) return true;

      const mod = modules[code];
      if (!mod) {
        // If not loaded yet, assume available if loading, or check default
        return true;
      }

      // Check effective access if present, otherwise check enabled
      if (typeof mod.effectiveAccess === 'boolean') {
        return mod.effectiveAccess;
      }

      return mod.enabled !== false;
    },
    [modules, isSuperOrPlatform]
  );

  const getModuleAccessStatus = useCallback(
    (code?: ProductModuleCode): ModuleAccessStatus => {
      if (!code) return 'AVAILABLE';
      if (isSuperOrPlatform) return 'AVAILABLE';

      if (isLoading && Object.keys(modules).length === 0) {
        return 'LOADING';
      }

      const mod = modules[code];
      if (!mod) {
        return 'AVAILABLE';
      }

      if (mod.accessStatus) {
        return mod.accessStatus;
      }

      if (mod.enabled === false) {
        return 'MODULE_DISABLED';
      }

      return 'AVAILABLE';
    },
    [modules, isLoading, isSuperOrPlatform]
  );

  const getModuleReason = useCallback(
    (code?: ProductModuleCode): string | undefined => {
      if (!code) return undefined;
      const mod = modules[code];
      return mod?.reason;
    },
    [modules]
  );

  const getModule = useCallback(
    (code?: ProductModuleCode): OrganizationModule | undefined => {
      if (!code) return undefined;
      return modules[code];
    },
    [modules]
  );

  const value = useMemo(
    () => ({
      modules,
      modulesList,
      isLoading,
      error,
      isModuleAvailable,
      getModuleAccessStatus,
      getModuleReason,
      getModule,
      refreshEntitlements: fetchEntitlements,
    }),
    [
      modules,
      modulesList,
      isLoading,
      error,
      isModuleAvailable,
      getModuleAccessStatus,
      getModuleReason,
      getModule,
      fetchEntitlements,
    ]
  );

  return (
    <ModuleEntitlementContext.Provider value={value}>
      {children}
    </ModuleEntitlementContext.Provider>
  );
};

export const useModuleEntitlement = (): ModuleEntitlementContextType => {
  const context = useContext(ModuleEntitlementContext);
  if (!context) {
    throw new Error('useModuleEntitlement must be used within a ModuleEntitlementProvider');
  }
  return context;
};
