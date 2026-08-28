import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ShieldAlert, ArrowLeft } from 'lucide-react';
import { Button } from './Button';

interface RoleRouteGuardProps {
  children: React.ReactNode;
  allowedRoles?: string[];
  requiredPermissions?: string[];
}

export const RoleRouteGuard: React.FC<RoleRouteGuardProps> = ({
  children,
  allowedRoles,
  requiredPermissions,
}) => {
  const { user } = useAuth();
  const location = useLocation();

  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const userPermissions = user?.permissions || [];

  const isSuperAdmin = userRoleCodes.includes('TAXORYN_SUPERADMIN') || userRoleCodes.includes('SUPER_ADMIN');

  if (isSuperAdmin) {
    return <>{children}</>;
  }

  // Check roles if specified
  const roleAllowed = !allowedRoles || allowedRoles.some((r) => userRoleCodes.includes(r));

  // Check permissions if specified
  const permissionAllowed = !requiredPermissions || requiredPermissions.some((p) => userPermissions.includes(p));

  if (!roleAllowed && !permissionAllowed) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center p-6 text-center animate-fade-in">
        <div className="w-16 h-16 rounded-2xl bg-rose-100 text-rose-600 flex items-center justify-center mb-4 shadow-sm border border-rose-200">
          <ShieldAlert className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-black text-slate-900 tracking-tight">Access Denied (403 Forbidden)</h2>
        <p className="text-xs text-slate-500 max-w-md mt-1 mb-6 leading-relaxed">
          Your assigned role does not have authorization to access the requested platform module (
          <code className="text-purple-600 font-mono bg-purple-50 px-1.5 py-0.5 rounded">{location.pathname}</code>
          ). Access is restricted under least-privilege RBAC.
        </p>
        <div className="flex items-center gap-3">
          <Button
            variant="secondary"
            onClick={() => window.history.back()}
            className="text-xs gap-1.5 font-bold"
          >
            <ArrowLeft className="w-4 h-4" /> Go Back
          </Button>
          <Button
            variant="primary"
            onClick={() => window.location.href = '/dashboard'}
            className="text-xs bg-purple-600 hover:bg-purple-700 text-white font-bold"
          >
            Return to Workspace
          </Button>
        </div>
      </div>
    );
  }

  return <>{children}</>;
};
