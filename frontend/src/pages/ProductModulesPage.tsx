import React, { useState, useEffect } from 'react';
import {
  Boxes,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  FileText,
  CheckSquare,
  FolderOpen,
  Send,
  Globe,
  Bell,
  ShieldCheck,
  Receipt,
  FileSpreadsheet,
  Building,
  FileCheck,
  TrendingUp,
  Store,
  Mail,
  UserCheck,
  Lock,
  Zap,
  Info,
  Layers,
  Database,
  Users,
  MapPin,
  KeyRound,
  SlidersHorizontal,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { useAuth } from '../context/AuthContext';
import { useModuleEntitlement } from '../context/ModuleEntitlementContext';
import { moduleConfigApi } from '../api/endpoints';
import {
  OrganizationModule,
  ProductModuleCategory,
  ProductModuleCode,
} from '../types';
import { hasPermission } from '../utils/permissionUtils';
import clsx from 'clsx';

export const CATEGORY_LABELS: Record<ProductModuleCategory, string> = {
  CORE: 'Core Platform Capabilities',
  FOUNDATION: 'Practice Operations Foundation',
  BUSINESS: 'Specialized Tax & Compliance',
  OPTIONAL: 'Extensions & Integrations',
};

export const CATEGORY_DESCRIPTIONS: Record<ProductModuleCategory, string> = {
  CORE: 'Fundamental platform infrastructure including security, user management, and system auditing. Always active.',
  FOUNDATION: 'Essential operating modules for practice workflow, clients, documents, and billing. Resource limits governed by subscription.',
  BUSINESS: 'Statutory tax return engines and assessment tracker. Configurable per organization and entitled by plan.',
  OPTIONAL: 'Modular taxpayer portal, marketplace presence, and third-party integrations (Gmail OAuth).',
};

const MODULE_ICONS: Record<string, React.ReactNode> = {
  ORGANIZATION: <Building className="w-5 h-5 text-indigo-400" />,
  LOCATIONS: <MapPin className="w-5 h-5 text-sky-400" />,
  USERS: <Users className="w-5 h-5 text-blue-400" />,
  ROLES: <KeyRound className="w-5 h-5 text-purple-400" />,
  SECURITY: <ShieldCheck className="w-5 h-5 text-emerald-400" />,
  SUBSCRIPTION: <Zap className="w-5 h-5 text-amber-400" />,
  MODULE_CONFIG: <SlidersHorizontal className="w-5 h-5 text-rose-400" />,
  CLIENTS: <Building className="w-5 h-5 text-indigo-400" />,
  TASKS: <CheckSquare className="w-5 h-5 text-emerald-400" />,
  DOCUMENTS: <FolderOpen className="w-5 h-5 text-amber-400" />,
  DOCUMENT_REQUESTS: <Send className="w-5 h-5 text-cyan-400" />,
  CLIENT_PORTAL: <Globe className="w-5 h-5 text-blue-400" />,
  NOTIFICATIONS: <Bell className="w-5 h-5 text-purple-400" />,
  AUDIT: <ShieldCheck className="w-5 h-5 text-rose-400" />,
  GST: <Receipt className="w-5 h-5 text-teal-400" />,
  GST_COMPLIANCE: <Receipt className="w-5 h-5 text-teal-400" />,
  ITR: <FileText className="w-5 h-5 text-sky-400" />,
  ITR_COMPLIANCE: <FileText className="w-5 h-5 text-sky-400" />,
  TDS: <FileCheck className="w-5 h-5 text-orange-400" />,
  TDS_COMPLIANCE: <FileCheck className="w-5 h-5 text-orange-400" />,
  TAX_NOTICES: <AlertCircle className="w-5 h-5 text-red-400" />,
  TAX_NOTICE_MANAGEMENT: <AlertCircle className="w-5 h-5 text-red-400" />,
  BILLING: <FileSpreadsheet className="w-5 h-5 text-emerald-400" />,
  BILLING_PRACTICE_OPERATIONS: <FileSpreadsheet className="w-5 h-5 text-emerald-400" />,
  REPORTS: <TrendingUp className="w-5 h-5 text-indigo-400" />,
  DASHBOARD: <Layers className="w-5 h-5 text-blue-400" />,
  PRACTICE_DASHBOARD: <Layers className="w-5 h-5 text-blue-400" />,
  MARKETPLACE: <Store className="w-5 h-5 text-pink-400" />,
  GMAIL: <Mail className="w-5 h-5 text-red-400" />,
  SELF_ITR: <UserCheck className="w-5 h-5 text-teal-400" />,
};

export const ProductModulesPage: React.FC = () => {
  const { user } = useAuth();
  const { refreshEntitlements } = useModuleEntitlement();
  const [modules, setModules] = useState<OrganizationModule[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [togglingCode, setTogglingCode] = useState<ProductModuleCode | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const canEdit = hasPermission(
    user,
    ['ORGANIZATION_UPDATE', 'ORG_WRITE'],
    ['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER']
  );

  const fetchModules = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await moduleConfigApi.getOrganizationModules();
      setModules(data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load product module configurations.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchModules();
  }, []);

  const handleToggle = async (moduleCode: ProductModuleCode, currentEnabled: boolean) => {
    if (!canEdit || togglingCode) return;

    try {
      setTogglingCode(moduleCode);
      setError(null);
      setSuccessMessage(null);

      const targetEnabled = !currentEnabled;
      const updated = await moduleConfigApi.updateModuleStatus(moduleCode, targetEnabled);

      setModules((prev) =>
        prev.map((m) => (m.moduleCode === moduleCode ? updated : m))
      );

      // Refresh global entitlement context so sidebar, action menus, and route guards reflect immediately
      refreshEntitlements().catch((e) => console.warn('Failed to refresh global entitlements:', e));

      setSuccessMessage(
        `${updated.moduleName} is now ${targetEnabled ? 'ENABLED' : 'DISABLED'}.`
      );

      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err: any) {
      setError(err?.response?.data?.message || `Failed to update ${moduleCode} status.`);
    } finally {
      setTogglingCode(null);
    }
  };

  const categories: ProductModuleCategory[] = ['CORE', 'FOUNDATION', 'BUSINESS', 'OPTIONAL'];

  return (
    <div className="space-y-6 max-w-6xl mx-auto pb-12">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-700/60 pb-5">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-brand-500/10 rounded-lg text-brand-400 border border-brand-500/20">
              <Boxes className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-white tracking-tight">Product Modules & Classification</h1>
              <p className="text-xs text-slate-400 mt-0.5">
                Practice architecture governance: Core, Foundation, Business Compliance, and Optional Integrations.
              </p>
            </div>
          </div>
        </div>
        <button
          onClick={fetchModules}
          disabled={loading}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-slate-300 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-md transition-colors disabled:opacity-50"
        >
          <RefreshCw className={clsx('w-3.5 h-3.5', loading && 'animate-spin')} />
          Refresh
        </button>
      </div>

      {/* Notifications */}
      {successMessage && (
        <div className="flex items-center gap-2 p-3 bg-emerald-500/10 border border-emerald-500/30 rounded-lg text-xs text-emerald-300 animate-fadeIn">
          <CheckCircle2 className="w-4 h-4 flex-shrink-0 text-emerald-400" />
          <span>{successMessage}</span>
        </div>
      )}

      {error && (
        <div className="flex items-center gap-2 p-3 bg-red-500/10 border border-red-500/30 rounded-lg text-xs text-red-300">
          <AlertCircle className="w-4 h-4 flex-shrink-0 text-red-400" />
          <span>{error}</span>
        </div>
      )}

      {!canEdit && (
        <div className="flex items-center gap-2 p-3 bg-amber-500/10 border border-amber-500/30 rounded-lg text-xs text-amber-300">
          <Info className="w-4 h-4 flex-shrink-0 text-amber-400" />
          <span>You have read-only access to organization module settings. Contact an Organization Administrator to change configuration.</span>
        </div>
      )}

      {/* Loading Skeleton */}
      {loading && modules.length === 0 && (
        <div className="space-y-6">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="bg-slate-800/40 border border-slate-700/50 rounded-xl p-5 animate-pulse space-y-4">
              <div className="h-4 bg-slate-700 rounded w-1/4" />
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {[1, 2, 3, 4].map((j) => (
                  <div key={j} className="h-20 bg-slate-800/60 rounded-lg" />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Grouped Module Sections */}
      {!loading && modules.length > 0 && (
        <div className="space-y-8">
          {categories.map((category) => {
            const categoryModules = modules.filter((m) => m.category === category);
            if (categoryModules.length === 0) return null;

            const isLockedCategory = category === 'CORE' || category === 'FOUNDATION';

            return (
              <div key={category} className="space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="flex items-center gap-2">
                      <h2 className="text-sm font-semibold text-slate-200 tracking-wide">
                        {CATEGORY_LABELS[category]}
                      </h2>
                      <span className={clsx(
                        'text-[10px] font-semibold px-2 py-0.5 rounded-full uppercase tracking-wider',
                        category === 'CORE' && 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/30',
                        category === 'FOUNDATION' && 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/30',
                        category === 'BUSINESS' && 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30',
                        category === 'OPTIONAL' && 'bg-purple-500/10 text-purple-400 border border-purple-500/30',
                      )}>
                        {category}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 mt-0.5">
                      {CATEGORY_DESCRIPTIONS[category]}
                    </p>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                  {categoryModules.map((mod) => {
                    const isToggling = togglingCode === mod.moduleCode;
                    const isMandatory = mod.mandatory || isLockedCategory;

                    return (
                      <Card
                        key={mod.moduleCode}
                        className={clsx(
                          'p-4 transition-all duration-200 border',
                          mod.enabled
                            ? 'bg-slate-800/60 border-slate-700/80 hover:border-slate-600'
                            : 'bg-slate-900/40 border-slate-800/60 opacity-80'
                        )}
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div className="flex items-start gap-3 min-w-0">
                            <div className="p-2 rounded-lg bg-slate-800 border border-slate-700/60 flex-shrink-0 mt-0.5">
                              {MODULE_ICONS[mod.moduleCode] || <Boxes className="w-5 h-5 text-slate-400" />}
                            </div>
                            <div className="min-w-0">
                              <div className="flex items-center gap-2 flex-wrap">
                                <h3 className="text-sm font-semibold text-white truncate">
                                  {mod.moduleName}
                                </h3>
                                <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700/50">
                                  {mod.moduleCode}
                                </span>
                                {mod.usageControlled && (
                                  <span className="inline-flex items-center gap-1 text-[10px] font-medium px-1.5 py-0.5 rounded bg-amber-500/10 text-amber-300 border border-amber-500/20">
                                    <Database className="w-2.5 h-2.5" /> Quota Monitored
                                  </span>
                                )}
                              </div>
                              <p className="text-xs text-slate-400 mt-1 line-clamp-2">
                                {mod.moduleDescription || 'Standard functional module.'}
                              </p>
                            </div>
                          </div>

                          {/* Controls: Locked Badge vs Interactive Switch */}
                          <div className="flex flex-col items-end gap-1 flex-shrink-0">
                            {isMandatory ? (
                              <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-semibold">
                                <Lock className="w-3 h-3 text-emerald-400" />
                                <span>{category === 'CORE' ? 'ALWAYS ACTIVE' : 'MANDATORY'}</span>
                              </div>
                            ) : (
                              <>
                                <button
                                  type="button"
                                  role="switch"
                                  aria-checked={mod.enabled}
                                  aria-label={`Toggle ${mod.moduleName}`}
                                  disabled={!canEdit || isToggling}
                                  onClick={() => handleToggle(mod.moduleCode, mod.enabled)}
                                  className={clsx(
                                    'relative inline-flex h-5 w-9 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2 focus:ring-offset-slate-900',
                                    mod.enabled ? 'bg-[#00D1A3]' : 'bg-slate-700',
                                    (!canEdit || isToggling) && 'cursor-not-allowed opacity-50'
                                  )}
                                >
                                  <span
                                    aria-hidden="true"
                                    className={clsx(
                                      'pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow-lg ring-0 transition duration-200 ease-in-out',
                                      mod.enabled ? 'translate-x-4' : 'translate-x-0'
                                    )}
                                  />
                                </button>
                                <span className="text-[10px] font-medium text-slate-400">
                                  {isToggling ? (
                                    <span className="text-brand-400 flex items-center gap-1">
                                      <RefreshCw className="w-2.5 h-2.5 animate-spin" /> Updating
                                    </span>
                                  ) : mod.enabled ? (
                                    <span className="text-emerald-400 font-semibold">ON</span>
                                  ) : (
                                    <span className="text-slate-500">OFF</span>
                                  )}
                                </span>
                              </>
                            )}
                          </div>
                        </div>

                        {/* Configuration Metadata Badge */}
                        <div className="mt-3 pt-2.5 border-t border-slate-700/40 flex items-center justify-between text-[11px] text-slate-500">
                          <span>
                            {isMandatory ? (
                              <span className="text-emerald-400/90 font-medium">Non-Configurable Core Platform</span>
                            ) : mod.explicitlyConfigured ? (
                              <span className="text-slate-400">Custom Organization Setting</span>
                            ) : (
                              <span className="text-slate-500 italic">Default Catalog Setting</span>
                            )}
                          </span>
                          {mod.updatedAt && (
                            <span>Updated {new Date(mod.updatedAt).toLocaleDateString()}</span>
                          )}
                        </div>
                      </Card>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
