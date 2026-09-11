import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ShieldAlert, ArrowLeft, Home } from 'lucide-react';
import { Button } from './Button';
import { hasPermission } from '../../utils/permissionUtils';

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
  const navigate = useNavigate();

  const isAuthorized = hasPermission(user, requiredPermissions, allowedRoles);

  if (!isAuthorized) {
    return (
      <div className="min-h-[65vh] flex flex-col items-center justify-center p-6 text-center animate-fade-in select-none">
        <div className="w-16 h-16 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center mb-4 shadow-sm border border-rose-200">
          <ShieldAlert className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-black text-slate-900 tracking-tight">
          You don't have access to this area
        </h2>
        <p className="text-xs text-slate-500 max-w-md mt-2 mb-6 leading-relaxed">
          Your current role doesn't have permission to access this module. If you believe you need access, contact your practice administrator.
        </p>
        <div className="flex items-center gap-3">
          <Button
            variant="secondary"
            onClick={() => {
              if (window.history.length > 1) {
                navigate(-1);
              } else {
                navigate('/dashboard');
              }
            }}
            className="text-xs gap-1.5 font-bold shadow-2xs"
          >
            <ArrowLeft className="w-4 h-4" /> Go Back
          </Button>
          <Button
            variant="primary"
            onClick={() => navigate('/dashboard')}
            className="text-xs gap-1.5 font-bold shadow-2xs"
          >
            <Home className="w-4 h-4" /> Return to Workspace
          </Button>
        </div>
        <span className="text-[11px] text-slate-400 font-mono mt-6">
          Error code: 403
        </span>
      </div>
    );
  }

  return <>{children}</>;
};

