import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ShieldAlert,
  AlertTriangle,
  CreditCard,
  Sparkles,
  ArrowLeft,
  Home,
  Sliders,
  Layers,
  Loader2,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useModuleEntitlement } from '../../context/ModuleEntitlementContext';
import { hasPermission } from '../../utils/permissionUtils';
import { Button } from './Button';
import type { ProductModuleCode } from '../../types/index';

interface ModuleRouteGuardProps {
  children: React.ReactNode;
  moduleCode?: ProductModuleCode;
  allowedRoles?: string[];
  requiredPermissions?: string[];
  featureName?: string;
}

export const ModuleRouteGuard: React.FC<ModuleRouteGuardProps> = ({
  children,
  moduleCode,
  allowedRoles,
  requiredPermissions,
  featureName,
}) => {
  const { user } = useAuth();
  const { getModuleAccessStatus, getModuleReason, getModule } = useModuleEntitlement();
  const navigate = useNavigate();

  // 1. RBAC / Role Permission Check
  const isAuthorized = hasPermission(user, requiredPermissions, allowedRoles);
  if (!isAuthorized) {
    return (
      <div className="min-h-[65vh] flex flex-col items-center justify-center p-6 text-center animate-fade-in select-none">
        <div className="w-16 h-16 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center mb-4 shadow-xs border border-rose-200">
          <ShieldAlert className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-black text-slate-900 tracking-tight">
          You don't have access to this area
        </h2>
        <p className="text-xs text-slate-500 max-w-md mt-2 mb-6 leading-relaxed">
          Your current role doesn't have permission to access {featureName || 'this module'}. If you believe you need access, contact your practice administrator.
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
          Error code: 403 (PERMISSION_DENIED)
        </span>
      </div>
    );
  }

  // 2. Product Module & Subscription Entitlement Check
  if (moduleCode) {
    const accessStatus = getModuleAccessStatus(moduleCode);
    const backendReason = getModuleReason(moduleCode);
    const moduleInfo = getModule(moduleCode);
    const displayName = moduleInfo?.moduleName || featureName || moduleCode;

    const isAdmin = hasPermission(
      user,
      ['ORGANIZATION_UPDATE', 'ORG_WRITE'],
      ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'TAXORYN_SUPERADMIN', 'SUPER_ADMIN']
    );

    if (accessStatus === 'LOADING') {
      return (
        <div className="min-h-[50vh] flex flex-col items-center justify-center p-6 text-center select-none">
          <Loader2 className="w-8 h-8 text-brand-500 animate-spin mb-3" />
          <p className="text-xs text-slate-400 font-medium">Verifying module entitlement...</p>
        </div>
      );
    }

    if (accessStatus === 'MODULE_DISABLED') {
      return (
        <div className="min-h-[65vh] flex flex-col items-center justify-center p-6 text-center animate-fade-in select-none">
          <div className="w-16 h-16 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center mb-4 shadow-xs border border-amber-200">
            <AlertTriangle className="w-8 h-8" />
          </div>
          <h2 className="text-xl font-black text-slate-900 tracking-tight">
            {displayName} is Disabled
          </h2>
          <p className="text-xs text-slate-500 max-w-md mt-2 mb-6 leading-relaxed">
            {backendReason || `The ${displayName} module is administratively disabled for your organization. Practice administrators can enable it in organization settings.`}
          </p>
          <div className="flex flex-wrap items-center justify-center gap-3">
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
            {isAdmin ? (
              <Button
                variant="primary"
                onClick={() => navigate('/settings/modules')}
                className="text-xs gap-1.5 font-bold shadow-2xs bg-amber-600 hover:bg-amber-700 text-white"
              >
                <Sliders className="w-4 h-4" /> Manage Modules
              </Button>
            ) : (
              <Button
                variant="primary"
                onClick={() => navigate('/dashboard')}
                className="text-xs gap-1.5 font-bold shadow-2xs"
              >
                <Home className="w-4 h-4" /> Return to Dashboard
              </Button>
            )}
          </div>
          <span className="text-[11px] text-slate-400 font-mono mt-6">
            Status: MODULE_DISABLED ({moduleCode})
          </span>
        </div>
      );
    }

    if (accessStatus === 'SUBSCRIPTION_REQUIRED') {
      return (
        <div className="min-h-[65vh] flex flex-col items-center justify-center p-6 text-center animate-fade-in select-none">
          <div className="w-16 h-16 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center mb-4 shadow-xs border border-rose-200">
            <CreditCard className="w-8 h-8" />
          </div>
          <h2 className="text-xl font-black text-slate-900 tracking-tight">
            Active Subscription Required
          </h2>
          <p className="text-xs text-slate-500 max-w-md mt-2 mb-6 leading-relaxed">
            {backendReason || `Your subscription is currently inactive, expired, or past due. Please renew or update your subscription to continue using ${displayName}.`}
          </p>
          <div className="flex flex-wrap items-center justify-center gap-3">
            <Button
              variant="secondary"
              onClick={() => navigate('/dashboard')}
              className="text-xs gap-1.5 font-bold shadow-2xs"
            >
              <Home className="w-4 h-4" /> Dashboard
            </Button>
            {isAdmin && (
              <Button
                variant="primary"
                onClick={() => navigate('/settings/subscription')}
                className="text-xs gap-1.5 font-bold shadow-2xs"
              >
                <CreditCard className="w-4 h-4" /> Manage Subscription
              </Button>
            )}
          </div>
          <span className="text-[11px] text-slate-400 font-mono mt-6">
            Status: SUBSCRIPTION_REQUIRED ({moduleCode})
          </span>
        </div>
      );
    }

    if (accessStatus === 'UPGRADE_REQUIRED') {
      return (
        <div className="min-h-[65vh] flex flex-col items-center justify-center p-6 text-center animate-fade-in select-none">
          <div className="w-16 h-16 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mb-4 shadow-xs border border-indigo-200">
            <Sparkles className="w-8 h-8" />
          </div>
          <h2 className="text-xl font-black text-slate-900 tracking-tight">
            Plan Upgrade Required
          </h2>
          <p className="text-xs text-slate-500 max-w-md mt-2 mb-6 leading-relaxed">
            {backendReason || `The ${displayName} module is not included in your current subscription plan. Upgrade your plan tier to unlock this capability.`}
          </p>
          <div className="flex flex-wrap items-center justify-center gap-3">
            <Button
              variant="secondary"
              onClick={() => navigate('/dashboard')}
              className="text-xs gap-1.5 font-bold shadow-2xs"
            >
              <Home className="w-4 h-4" /> Dashboard
            </Button>
            {isAdmin && (
              <Button
                variant="primary"
                onClick={() => navigate('/settings/subscription')}
                className="text-xs gap-1.5 font-bold shadow-2xs bg-indigo-600 hover:bg-indigo-700 text-white"
              >
                <Layers className="w-4 h-4" /> Upgrade Plan
              </Button>
            )}
          </div>
          <span className="text-[11px] text-slate-400 font-mono mt-6">
            Status: UPGRADE_REQUIRED ({moduleCode})
          </span>
        </div>
      );
    }
  }

  return <>{children}</>;
};
