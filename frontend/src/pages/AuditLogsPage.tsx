import React, { useState, useEffect, useCallback } from 'react';
import {
  ShieldAlert,
  ShieldCheck,
  Eye,
  Search,
  Filter,
  RefreshCw,
  ArrowRight,
  Clock,
  User,
  Building2,
  AlertTriangle,
  CheckCircle2,
  AlertCircle,
  Terminal,
  Activity,
  Layers,
  Calendar,
  X,
  Lock,
  FileText,
  Users,
  CheckSquare,
  Receipt,
  HelpCircle,
} from 'lucide-react';
import { Modal } from '../components/common/Modal';
import { auditApi, clientApi } from '../api/endpoints';
import { AuditLog, Client } from '../types';
import { useAuth } from '../context/AuthContext';
import clsx from 'clsx';

export const AuditLogsPage: React.FC = () => {
  const { user } = useAuth();

  // Determine user permissions and roles
  const userRoleCodes = (user?.roles || []).map((r: any) =>
    (typeof r === 'string' ? r : r.code || '').toUpperCase().replace(/^ROLE_/, '')
  );
  const userPermissions = (user?.permissions || []).map((p: any) =>
    (typeof p === 'string' ? p : p.code || '').toUpperCase()
  );

  const isPlatformSuperAdmin =
    userRoleCodes.includes('TAXORYN_SUPERADMIN') ||
    userRoleCodes.includes('SUPER_ADMIN') ||
    userRoleCodes.includes('TAXORYN_OPERATIONS_ADMIN') ||
    userRoleCodes.includes('TAXORYN_SECURITY_ADMIN');

  const isPracticeAdminOrOwner =
    userRoleCodes.includes('PRACTICE_OWNER') ||
    userRoleCodes.includes('PRACTICE_ADMIN') ||
    userRoleCodes.includes('ORG_ADMIN') ||
    userRoleCodes.includes('PARTNER');

  const canViewSecurityAudit =
    isPlatformSuperAdmin ||
    isPracticeAdminOrOwner ||
    userPermissions.includes('SECURITY_VIEW') ||
    userPermissions.includes('AUDIT_SECURITY_VIEW');

  // Active Tab: PRACTICE_ACTIVITY (default) vs SECURITY
  const [activeTab, setActiveTab] = useState<'PRACTICE_ACTIVITY' | 'SECURITY'>('PRACTICE_ACTIVITY');

  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Clients list for client filter
  const [clients, setClients] = useState<Client[]>([]);
  const [selectedClientId, setSelectedClientId] = useState<string>('');

  // Filters & Search
  const [searchTerm, setSearchTerm] = useState('');
  const [entityTypeFilter, setEntityTypeFilter] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);

  // Load clients for filter dropdown
  useEffect(() => {
    const fetchClients = async () => {
      try {
        const res = await clientApi.getAll({ size: 100 });
        setClients(res?.content || []);
      } catch {
        // Ignore if user cannot fetch clients
      }
    };
    fetchClients();
  }, []);

  const loadLogs = useCallback(async () => {
    try {
      setIsLoading(true);
      setErrorMessage(null);

      const category = activeTab === 'SECURITY' ? 'SECURITY' : 'PRACTICE_ACTIVITY';

      const res = await auditApi.getLogs({
        page,
        size: pageSize,
        category,
        clientId: selectedClientId || undefined,
        search: searchTerm.trim() || undefined,
        entityType: entityTypeFilter || undefined,
        action: actionFilter || undefined,
        status: statusFilter || undefined,
      });

      setLogs(res?.content || []);
      setTotalElements(res?.totalElements || 0);
    } catch (err: any) {
      console.error('Failed to load audit logs', err);
      if (err?.response?.status === 403) {
        setErrorMessage(
          activeTab === 'SECURITY'
            ? 'You do not have permission to view security audit events.'
            : 'You do not have permission to view activity & audit logs.'
        );
      } else {
        setErrorMessage('Unable to load activity records. Please try again.');
      }
      setLogs([]);
      setTotalElements(0);
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, activeTab, selectedClientId, searchTerm, entityTypeFilter, actionFilter, statusFilter]);

  useEffect(() => {
    loadLogs();
  }, [loadLogs]);

  const handleTabChange = (tab: 'PRACTICE_ACTIVITY' | 'SECURITY') => {
    setActiveTab(tab);
    setPage(0);
    setActionFilter('');
    setEntityTypeFilter('');
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    loadLogs();
  };

  const handleClearFilters = () => {
    setSearchTerm('');
    setSelectedClientId('');
    setEntityTypeFilter('');
    setActionFilter('');
    setStatusFilter('');
    setPage(0);
  };

  const formatDateTime = (timestamp?: string) => {
    if (!timestamp) return 'N/A';
    const d = new Date(timestamp);
    return `${d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })} ${d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}`;
  };

  const formatDisplayAction = (log: AuditLog) => {
    if (log.displayAction) return log.displayAction;
    const act = log.action || '';
    const formatted = act.replace(/_/g, ' ').toLowerCase();
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  };

  const totalPages = Math.ceil(totalElements / pageSize) || 1;

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      {/* ========================================================================= */}
      {/* 1. Header & Tab Navigation                                               */}
      {/* ========================================================================= */}
      <div className="bg-white border border-slate-200/90 p-6 rounded-2xl shadow-card space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1.5">
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-purple-100 text-purple-800 border border-purple-200">
                Practice Governance
              </span>
              <span className="text-slate-300 text-xs">•</span>
              <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
                COMPREHENSIVE AUDIT TRAIL
              </span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-slate-900">
              Activity & Audit
            </h1>
            <p className="text-xs sm:text-sm text-slate-500 mt-1">
              Review practice operations, client records, compliance filings, and administrative actions with full timeline traceability.
            </p>
          </div>

          <div className="flex items-center gap-3 self-start sm:self-auto">
            <button
              onClick={loadLogs}
              disabled={isLoading}
              className="px-3.5 py-2 bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-all flex items-center gap-2 shadow-2xs"
              title="Refresh Activity Records"
            >
              <RefreshCw className={clsx('w-4 h-4', isLoading && 'animate-spin')} />
              <span>Refresh</span>
            </button>
          </div>
        </div>

        {/* Navigation Tabs: Practice Activity vs Security Audit */}
        <div className="flex items-center gap-2 border-t border-slate-100 pt-4">
          <button
            type="button"
            onClick={() => handleTabChange('PRACTICE_ACTIVITY')}
            className={clsx(
              'px-4 py-2 text-xs font-bold rounded-xl transition-all flex items-center gap-2 border',
              activeTab === 'PRACTICE_ACTIVITY'
                ? 'bg-purple-600 text-white border-purple-600 shadow-xs'
                : 'bg-slate-50 hover:bg-slate-100 text-slate-600 border-slate-200'
            )}
          >
            <Activity className="w-3.5 h-3.5" />
            <span>Practice Activity</span>
          </button>

          {canViewSecurityAudit && (
            <button
              type="button"
              onClick={() => handleTabChange('SECURITY')}
              className={clsx(
                'px-4 py-2 text-xs font-bold rounded-xl transition-all flex items-center gap-2 border',
                activeTab === 'SECURITY'
                  ? 'bg-slate-900 text-white border-slate-900 shadow-xs'
                  : 'bg-slate-50 hover:bg-slate-100 text-slate-600 border-slate-200'
              )}
            >
              <ShieldAlert className="w-3.5 h-3.5 text-amber-400" />
              <span>Security Audit</span>
              <span className="text-[10px] font-black uppercase px-1.5 py-0.2 bg-amber-500/20 text-amber-700 rounded border border-amber-400/30">
                Admin
              </span>
            </button>
          )}
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 2. Filters & Search Toolbar                                              */}
      {/* ========================================================================= */}
      <div className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card">
        <form onSubmit={handleSearchSubmit} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {/* Universal Search Input */}
          <div className="relative lg:col-span-2">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder={
                activeTab === 'SECURITY'
                  ? 'Search security event, IP address, user email...'
                  : 'Search by activity, client name, staff, action...'
              }
              className="w-full pl-9 pr-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 transition-all placeholder:text-slate-400 font-medium"
            />
          </div>

          {/* Client Filter (when on Practice Activity) */}
          {activeTab === 'PRACTICE_ACTIVITY' ? (
            <div>
              <select
                value={selectedClientId}
                onChange={(e) => {
                  setSelectedClientId(e.target.value);
                  setPage(0);
                }}
                className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 transition-all font-medium text-slate-700 truncate"
              >
                <option value="">All Clients</option>
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.displayName || c.legalName || 'Client'}
                  </option>
                ))}
              </select>
            </div>
          ) : (
            <div>
              <select
                value={entityTypeFilter}
                onChange={(e) => {
                  setEntityTypeFilter(e.target.value);
                  setPage(0);
                }}
                className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 transition-all font-medium text-slate-700"
              >
                <option value="">All Security Scopes</option>
                <option value="AUTH">Authentication</option>
                <option value="TOKEN">Token Lifecycle</option>
                <option value="SESSION">User Session</option>
                <option value="ROLE">Privilege & Role</option>
              </select>
            </div>
          )}

          {/* Action Filter */}
          <div>
            <select
              value={actionFilter}
              onChange={(e) => {
                setActionFilter(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 transition-all font-medium text-slate-700 truncate"
            >
              {activeTab === 'PRACTICE_ACTIVITY' ? (
                <>
                  <option value="">All Activities</option>
                  <option value="CLIENT_CREATED">Client Created</option>
                  <option value="CLIENT_UPDATED">Client Updated</option>
                  <option value="CLIENT_STATUS_UPDATED">Client Status Changed</option>
                  <option value="DOCUMENT_UPLOADED">Document Uploaded</option>
                  <option value="DOCUMENT_VERIFIED">Document Verified</option>
                  <option value="TASK_CREATED">Task Created</option>
                  <option value="TASK_STATUS_UPDATED">Task Status Updated</option>
                  <option value="GST_FILING_SUBMITTED">GST Return Submitted</option>
                  <option value="ITR_RETURN_SUBMITTED">ITR Prepared / Submitted</option>
                  <option value="INVOICE_CREATED">Invoice Created</option>
                  <option value="INVOICE_PAYMENT_RECORDED">Payment Recorded</option>
                  <option value="EMPLOYEE_CREATED">Staff Member Onboarded</option>
                  <option value="ROLE_UPDATED">Role / Permissions Changed</option>
                  <option value="CLIENT_PORTAL_USER_INVITED">Client Portal Invitation</option>
                  <option value="LOGIN_SUCCESS">User Login</option>
                  <option value="LOGOUT">User Logout</option>
                </>
              ) : (
                <>
                  <option value="">All Security Events</option>
                  <option value="REFRESH_TOKEN_ROTATED">Refresh Token Rotated</option>
                  <option value="REFRESH_TOKEN_EXPIRED">Refresh Token Expired</option>
                  <option value="REFRESH_TOKEN_REVOKED">Refresh Token Revoked</option>
                  <option value="TOKEN_REUSE_DETECTED">Token Reuse Detected</option>
                  <option value="SESSION_REVOKED">Session Revoked</option>
                  <option value="ALL_SESSIONS_REVOKED">All Sessions Revoked</option>
                  <option value="SESSION_ROTATED">Session Rotated</option>
                  <option value="JWT_VALIDATION_FAILURE">JWT Validation Failure</option>
                  <option value="PASSWORD_HASH_UPDATED">Password Hash Updated</option>
                  <option value="SECURITY_ALERT">Security Alert</option>
                </>
              )}
            </select>
          </div>

          {/* Filter Actions */}
          <div className="flex items-center gap-2">
            <button
              type="submit"
              className="flex-1 py-2 bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold rounded-xl shadow-xs transition-colors text-center"
            >
              Apply Filter
            </button>
            {(searchTerm || selectedClientId || entityTypeFilter || actionFilter || statusFilter) && (
              <button
                type="button"
                onClick={handleClearFilters}
                className="p-2 text-slate-400 hover:text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors"
                title="Clear Filters"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>
        </form>
      </div>

      {/* ========================================================================= */}
      {/* 3. Activity Table & Mobile Cards                                          */}
      {/* ========================================================================= */}
      <div className="bg-white border border-slate-200/90 rounded-2xl shadow-card overflow-hidden">
        {/* Desktop / Tablet Table */}
        <div className="hidden md:block overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/80 font-bold text-slate-500 uppercase tracking-wider">
                <th className="px-5 py-3.5">Date & Time</th>
                <th className="px-5 py-3.5">Activity</th>
                <th className="px-5 py-3.5">Client / Target</th>
                <th className="px-5 py-3.5">Performed By</th>
                <th className="px-5 py-3.5">Status</th>
                <th className="px-5 py-3.5 text-right">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-slate-400">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <RefreshCw className="w-5 h-5 animate-spin text-purple-600" />
                      <span className="font-semibold text-slate-500">Loading activity records...</span>
                    </div>
                  </td>
                </tr>
              ) : errorMessage ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-slate-500">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <AlertCircle className="w-6 h-6 text-rose-500" />
                      <span className="font-bold text-slate-800">{errorMessage}</span>
                    </div>
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-slate-500">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <div className="w-10 h-10 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center">
                        <Activity className="w-5 h-5" />
                      </div>
                      <span className="font-bold text-slate-800">
                        {activeTab === 'SECURITY' ? 'No security events recorded' : 'No practice activity recorded'}
                      </span>
                      <p className="text-slate-400 text-xs max-w-sm">
                        {activeTab === 'SECURITY'
                          ? 'Low-level security events and token operations will appear here.'
                          : 'Client records, documents, filings, tasks, and team activities will appear here.'}
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                logs.map((logItem) => (
                  <tr
                    key={logItem.id}
                    className="hover:bg-slate-50/70 transition-colors group cursor-pointer"
                    onClick={() => setSelectedLog(logItem)}
                  >
                    {/* Timestamp */}
                    <td className="px-5 py-3.5 text-slate-600 whitespace-nowrap font-medium">
                      <div className="flex items-center gap-1.5">
                        <Clock className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                        <span>{formatDateTime(logItem.timestamp || logItem.createdAt)}</span>
                      </div>
                    </td>

                    {/* Activity */}
                    <td className="px-5 py-3.5">
                      <div>
                        <p className="font-bold text-slate-900">
                          {logItem.displayAction || formatDisplayAction(logItem)}
                        </p>
                        <div className="flex items-center gap-1.5 mt-0.5">
                          <span className="text-[10px] font-mono text-slate-500 bg-slate-100 px-1.5 py-0.2 rounded border border-slate-200">
                            {logItem.action}
                          </span>
                          {logItem.category && (
                            <span
                              className={clsx(
                                'text-[9px] font-bold uppercase px-1.5 py-0.2 rounded border',
                                logItem.category === 'SECURITY'
                                  ? 'bg-amber-50 text-amber-700 border-amber-200'
                                  : logItem.category === 'ACCESS'
                                  ? 'bg-blue-50 text-blue-700 border-blue-200'
                                  : 'bg-purple-50 text-purple-700 border-purple-200'
                              )}
                            >
                              {logItem.category}
                            </span>
                          )}
                        </div>
                      </div>
                    </td>

                    {/* Client / Target */}
                    <td className="px-5 py-3.5">
                      <div>
                        {logItem.clientName ? (
                          <>
                            <span className="font-bold text-slate-900 block truncate max-w-[180px]">
                              {logItem.clientName}
                            </span>
                            <span className="text-[10px] text-emerald-700 font-semibold bg-emerald-50 px-1.5 py-0.2 rounded-full border border-emerald-200 inline-block mt-0.5">
                              Client
                            </span>
                          </>
                        ) : (
                          <>
                            <span className="font-bold text-slate-800 block truncate max-w-[180px]">
                              {logItem.targetDisplayName || logItem.practiceName || logItem.organizationName || 'Practice'}
                            </span>
                            <span className="text-[10px] text-purple-700 font-semibold bg-purple-50 px-1.5 py-0.2 rounded-full border border-purple-200 inline-block mt-0.5">
                              {logItem.displayEntityType || logItem.entityType}
                            </span>
                          </>
                        )}
                      </div>
                    </td>

                    {/* Actor */}
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full bg-purple-50 text-purple-700 font-bold text-[10px] flex items-center justify-center shrink-0 border border-purple-200">
                          {(logItem.actorName || logItem.actor || 'U').charAt(0).toUpperCase()}
                        </div>
                        <div className="truncate max-w-[160px]">
                          <p className="font-bold text-slate-800 truncate">
                            {logItem.actorName || logItem.actor || 'System'}
                          </p>
                          <p className="text-[10px] text-slate-400 truncate">
                            {logItem.actorEmail || logItem.userEmail || 'system@taxoryn.com'}
                          </p>
                        </div>
                      </div>
                    </td>

                    {/* Status */}
                    <td className="px-5 py-3.5 whitespace-nowrap">
                      <span
                        className={clsx(
                          'px-2.5 py-0.5 rounded-full text-[10px] font-bold border inline-flex items-center gap-1',
                          logItem.status === 'ALERT' || logItem.severity === 'WARNING' || logItem.severity === 'CRITICAL'
                            ? 'bg-amber-50 text-amber-800 border-amber-200'
                            : 'bg-emerald-50 text-emerald-800 border-emerald-200'
                        )}
                      >
                        <span
                          className={clsx(
                            'w-1.5 h-1.5 rounded-full',
                            logItem.status === 'ALERT' || logItem.severity === 'WARNING' || logItem.severity === 'CRITICAL'
                              ? 'bg-amber-500'
                              : 'bg-emerald-500'
                          )}
                        ></span>
                        {logItem.status || 'SUCCESS'}
                      </span>
                    </td>

                    {/* Details Button */}
                    <td className="px-5 py-3.5 text-right whitespace-nowrap">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedLog(logItem);
                        }}
                        className="px-3 py-1 bg-slate-100 hover:bg-purple-100 text-slate-700 hover:text-purple-900 border border-slate-200 hover:border-purple-300 rounded-lg text-xs font-bold inline-flex items-center gap-1 transition-all shadow-2xs"
                      >
                        <span>View</span>
                        <ArrowRight className="w-3 h-3 group-hover:translate-x-0.5 transition-transform" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Mobile Card Representation */}
        <div className="md:hidden divide-y divide-slate-100">
          {isLoading ? (
            <div className="py-12 text-center text-slate-400">
              <RefreshCw className="w-5 h-5 animate-spin text-purple-600 mx-auto mb-2" />
              <span className="font-semibold text-xs text-slate-500">Loading activity records...</span>
            </div>
          ) : errorMessage ? (
            <div className="py-12 text-center text-slate-500 px-4">
              <AlertCircle className="w-6 h-6 text-rose-500 mx-auto mb-2" />
              <span className="font-bold text-xs text-slate-800">{errorMessage}</span>
            </div>
          ) : logs.length === 0 ? (
            <div className="py-12 text-center text-slate-500 px-4">
              <div className="w-10 h-10 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mx-auto mb-2">
                <Activity className="w-5 h-5" />
              </div>
              <span className="font-bold text-xs text-slate-800">
                {activeTab === 'SECURITY' ? 'No security events recorded' : 'No practice activity recorded'}
              </span>
            </div>
          ) : (
            logs.map((logItem) => (
              <div
                key={logItem.id}
                onClick={() => setSelectedLog(logItem)}
                className="p-4 space-y-2.5 text-xs hover:bg-slate-50/70 transition-colors cursor-pointer"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className="font-bold text-slate-900 truncate">
                      {logItem.displayAction || formatDisplayAction(logItem)}
                    </p>
                    <div className="flex items-center gap-1.5 mt-0.5 text-slate-500 text-[11px]">
                      <Clock className="w-3 h-3 text-slate-400 shrink-0" />
                      <span>{formatDateTime(logItem.timestamp || logItem.createdAt)}</span>
                    </div>
                  </div>
                  <span
                    className={clsx(
                      'shrink-0 px-2 py-0.5 rounded-full text-[10px] font-bold border inline-flex items-center gap-1',
                      logItem.status === 'ALERT' || logItem.severity === 'WARNING' || logItem.severity === 'CRITICAL'
                        ? 'bg-amber-50 text-amber-800 border-amber-200'
                        : 'bg-emerald-50 text-emerald-800 border-emerald-200'
                    )}
                  >
                    <span
                      className={clsx(
                        'w-1.5 h-1.5 rounded-full',
                        logItem.status === 'ALERT' || logItem.severity === 'WARNING' || logItem.severity === 'CRITICAL'
                          ? 'bg-amber-500'
                          : 'bg-emerald-500'
                      )}
                    ></span>
                    {logItem.status || 'SUCCESS'}
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-2 border-t border-slate-100 text-[11px]">
                  <div>
                    <span className="text-slate-400 block font-medium uppercase text-[10px]">Actor</span>
                    <span className="font-bold text-slate-800 truncate block">
                      {logItem.actorName || logItem.actor || 'System'}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-400 block font-medium uppercase text-[10px]">Client / Target</span>
                    <span className="font-bold text-slate-800 truncate block">
                      {logItem.clientName || logItem.targetDisplayName || logItem.practiceName || logItem.organizationName || 'Practice'}
                    </span>
                  </div>
                </div>

                <div className="pt-2 flex items-center justify-between gap-2 border-t border-slate-100">
                  <span className="text-[10px] font-mono text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded border border-slate-200 truncate">
                    {logItem.action}
                  </span>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelectedLog(logItem);
                    }}
                    className="px-2.5 py-1 bg-purple-50 hover:bg-purple-100 text-purple-700 border border-purple-200 rounded-lg text-xs font-bold inline-flex items-center gap-1 transition-all"
                  >
                    <span>View Details</span>
                    <ArrowRight className="w-3 h-3" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        {/* ========================================================================= */}
        {/* 4. Pagination Footer                                                     */}
        {/* ========================================================================= */}
        <div className="p-4 border-t border-slate-100 bg-slate-50/60 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs text-slate-500">
          <div className="flex items-center gap-2 flex-wrap">
            <span>Rows per page:</span>
            <select
              value={pageSize}
              onChange={(e) => {
                setPageSize(Number(e.target.value));
                setPage(0);
              }}
              className="px-2 py-1 bg-white border border-slate-200 rounded-lg font-bold text-slate-700 focus:outline-none focus:ring-1 focus:ring-purple-500"
            >
              <option value={10}>10</option>
              <option value={25}>25</option>
              <option value={50}>50</option>
            </select>
            <span className="ml-2">
              Showing {totalElements > 0 ? page * pageSize + 1 : 0} to {Math.min((page + 1) * pageSize, totalElements)} of {totalElements} records
            </span>
          </div>

          <div className="flex items-center gap-2 self-center sm:self-auto">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0 || isLoading}
              className="px-3 py-1.5 bg-white hover:bg-slate-100 disabled:opacity-40 border border-slate-200 rounded-lg font-bold text-slate-700 transition-colors shadow-2xs"
            >
              Previous
            </button>
            <span className="font-bold text-slate-700 px-2">
              {page + 1} / {totalPages}
            </span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1 || isLoading}
              className="px-3 py-1.5 bg-white hover:bg-slate-100 disabled:opacity-40 border border-slate-200 rounded-lg font-bold text-slate-700 transition-colors shadow-2xs"
            >
              Next
            </button>
          </div>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 5. Enterprise Details Drawer / Modal                                     */}
      {/* ========================================================================= */}
      <Modal
        isOpen={!!selectedLog}
        onClose={() => setSelectedLog(null)}
        title="Activity & Audit Details"
        subtitle={selectedLog ? `${selectedLog.displayAction || formatDisplayAction(selectedLog)} • ${selectedLog.action}` : ''}
        maxWidth="2xl"
      >
        {selectedLog && (
          <div className="space-y-4 text-xs">
            {/* Meta Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3 p-4 bg-slate-50 border border-slate-200/80 rounded-xl">
              <div>
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Target / Client</span>
                <span className="font-bold text-slate-900 mt-0.5 block truncate">
                  {selectedLog.clientName || selectedLog.targetDisplayName || selectedLog.practiceName || selectedLog.organizationName || 'Practice Global'}
                </span>
              </div>

              <div>
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Actor Name</span>
                <span className="font-bold text-slate-900 mt-0.5 block truncate">
                  {selectedLog.actorName || selectedLog.actor || 'System'}
                </span>
              </div>

              <div>
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Actor Role</span>
                <span className="font-bold text-purple-700 mt-0.5 block">
                  {selectedLog.actorRole || 'SYSTEM'}
                </span>
              </div>

              <div>
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Category</span>
                <span className="font-bold text-slate-800 mt-0.5 block">
                  {selectedLog.category || 'BUSINESS'}
                </span>
              </div>

              <div>
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Status</span>
                <span className="font-bold text-emerald-700 mt-0.5 block">
                  {selectedLog.status || 'SUCCESS'}
                </span>
              </div>

              <div>
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">IP Address</span>
                <span className="font-mono text-slate-700 mt-0.5 block">
                  {selectedLog.ipAddress || '127.0.0.1'}
                </span>
              </div>

              <div className="sm:col-span-3">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Correlation / Request ID</span>
                <span className="font-mono text-[11px] text-slate-600 mt-0.5 block break-all">
                  {selectedLog.requestId || 'N/A'}
                </span>
              </div>
            </div>

            {/* State Diffs (Clean JSON representation) */}
            {(selectedLog.oldValue || selectedLog.newValue) && (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <span className="font-bold text-slate-700 block mb-1.5 flex items-center gap-1">
                    <span>Previous State</span>
                  </span>
                  <pre className="p-3 bg-slate-900 text-slate-300 font-mono text-[11px] rounded-xl overflow-x-auto max-h-52 border border-slate-800">
                    {selectedLog.oldValue
                      ? selectedLog.oldValue.startsWith('{') || selectedLog.oldValue.startsWith('[')
                        ? JSON.stringify(JSON.parse(selectedLog.oldValue), null, 2)
                        : selectedLog.oldValue
                      : 'null (Initial Creation)'}
                  </pre>
                </div>

                <div>
                  <span className="font-bold text-slate-700 block mb-1.5 flex items-center gap-1">
                    <span>New State</span>
                  </span>
                  <pre className="p-3 bg-slate-900 text-emerald-400 font-mono text-[11px] rounded-xl overflow-x-auto max-h-52 border border-slate-800">
                    {selectedLog.newValue
                      ? selectedLog.newValue.startsWith('{') || selectedLog.newValue.startsWith('[')
                        ? JSON.stringify(JSON.parse(selectedLog.newValue), null, 2)
                        : selectedLog.newValue
                      : 'null (Record Deleted)'}
                  </pre>
                </div>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
};
