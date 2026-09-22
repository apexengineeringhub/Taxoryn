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
  Info,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { useAuth } from '../context/AuthContext';
import { moduleConfigApi } from '../api/endpoints';
import {
  OrganizationModule,
  ProductModuleCategory,
  ProductModuleCode,
} from '../types';
import { hasPermission } from '../utils/permissionUtils';
import clsx from 'clsx';

export const CATEGORY_LABELS: Record<ProductModuleCategory, string> = {
  CORE: 'Core Practice Modules',
  TAX: 'Tax Compliance Modules',
  PRACTICE_OPERATIONS: 'Practice Operations & Finance',
  NETWORK_GROWTH: 'Network & Growth',
};

export const CATEGORY_DESCRIPTIONS: Record<ProductModuleCategory, string> = {
  CORE: 'Fundamental practice management capabilities including clients, tasks, documents, and portal.',
  TAX: 'Statutory tax return filing and assessment notice tracking engines for Indian compliance.',
  PRACTICE_OPERATIONS: 'Professional fee billing, invoicing workflows, and central productivity reporting.',
  NETWORK_GROWTH: 'Marketplace discovery, practice lead generation, and client acquisition channels.',
};

const MODULE_ICONS: Record<ProductModuleCode, React.ReactNode> = {
  CLIENTS: <Building className="w-5 h-5 text-indigo-400" />,
  TASKS: <CheckSquare className="w-5 h-5 text-emerald-400" />,
  DOCUMENTS: <FolderOpen className="w-5 h-5 text-amber-400" />,
  DOCUMENT_REQUESTS: <Send className="w-5 h-5 text-cyan-400" />,
  CLIENT_PORTAL: <Globe className="w-5 h-5 text-blue-400" />,
  NOTIFICATIONS: <Bell className="w-5 h-5 text-purple-400" />,
  AUDIT: <ShieldCheck className="w-5 h-5 text-rose-400" />,
  GST: <Receipt className="w-5 h-5 text-teal-400" />,
  ITR: <FileText className="w-5 h-5 text-sky-400" />,
  TDS: <FileCheck className="w-5 h-5 text-orange-400" />,
  TAX_NOTICES: <AlertCircle className="w-5 h-5 text-red-400" />,
  BILLING: <FileSpreadsheet className="w-5 h-5 text-emerald-400" />,
  REPORTS: <TrendingUp className="w-5 h-5 text-indigo-400" />,
  MARKETPLACE: <Store className="w-5 h-5 text-pink-400" />,
};

export const ProductModulesPage: React.FC = () => {
  const { user } = useAuth();
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

  const categories: ProductModuleCategory[] = ['CORE', 'TAX', 'PRACTICE_OPERATIONS', 'NETWORK_GROWTH'];

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
              <h1 className="text-xl font-bold text-white tracking-tight">Product Modules & Features</h1>
              <p className="text-xs text-slate-400 mt-0.5">
                Configure enabled functional modules and service capabilities for your practice organization.
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
          {[1, 2, 3].map((i) => (
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

            return (
              <div key={category} className="space-y-3">
                <div>
                  <h2 className="text-sm font-semibold text-slate-200 tracking-wide">
                    {CATEGORY_LABELS[category]}
                  </h2>
                  <p className="text-xs text-slate-400 mt-0.5">
                    {CATEGORY_DESCRIPTIONS[category]}
                  </p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                  {categoryModules.map((mod) => {
                    const isToggling = togglingCode === mod.moduleCode;

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
                              <div className="flex items-center gap-2">
                                <h3 className="text-sm font-semibold text-white truncate">
                                  {mod.moduleName}
                                </h3>
                                <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700/50">
                                  {mod.moduleCode}
                                </span>
                              </div>
                              <p className="text-xs text-slate-400 mt-1 line-clamp-2">
                                {mod.moduleDescription || 'Standard functional module.'}
                              </p>
                            </div>
                          </div>

                          {/* Toggle Switch */}
                          <div className="flex flex-col items-end gap-1 flex-shrink-0">
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
                          </div>
                        </div>

                        {/* Configuration Metadata Badge */}
                        <div className="mt-3 pt-2.5 border-t border-slate-700/40 flex items-center justify-between text-[11px] text-slate-500">
                          <span>
                            {mod.explicitlyConfigured ? (
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
