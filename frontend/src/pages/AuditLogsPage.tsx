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
} from 'lucide-react';
import { Modal } from '../components/common/Modal';
import { auditApi } from '../api/endpoints';
import { AuditLog } from '../types';
import clsx from 'clsx';

export const AuditLogsPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Filters & Search
  const [searchTerm, setSearchTerm] = useState('');
  const [entityTypeFilter, setEntityTypeFilter] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);

  const loadLogs = useCallback(async () => {
    try {
      setIsLoading(true);
      setErrorMessage(null);
      const res = await auditApi.getLogs({
        page,
        size: pageSize,
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
        setErrorMessage('You do not have permission to view platform audit logs.');
      } else {
        setErrorMessage('Unable to load audit activity. Please try again.');
      }
      setLogs([]);
      setTotalElements(0);
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, searchTerm, entityTypeFilter, actionFilter, statusFilter]);

  useEffect(() => {
    loadLogs();
  }, [loadLogs]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    loadLogs();
  };

  const handleClearFilters = () => {
    setSearchTerm('');
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
      {/* 1. Header                                                                 */}
      {/* ========================================================================= */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white border border-slate-200/90 p-6 rounded-2xl shadow-card">
        <div>
          <div className="flex items-center gap-2 mb-1.5">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-purple-100 text-purple-800 border border-purple-200">
              Taxoryn Platform
            </span>
            <span className="text-slate-300 text-xs">•</span>
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              AUTHORITATIVE AUDIT TRAIL
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-slate-900">
            Security & Audit Trail
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Immutable compliance record of platform events, practice registrations, role modifications, and administrative actions.
          </p>
        </div>

        <div className="flex items-center gap-3 self-start sm:self-auto">
          <button
            onClick={loadLogs}
            disabled={isLoading}
            className="px-3.5 py-2 bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-all flex items-center gap-2 shadow-2xs"
            title="Refresh Audit Records"
          >
            <RefreshCw className={clsx('w-4 h-4', isLoading && 'animate-spin')} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 2. Filters & Search Toolbar                                              */}
      {/* ========================================================================= */}
      <div className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card">
        <form onSubmit={handleSearchSubmit} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {/* Search Input */}
          <div className="relative lg:col-span-2">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search by action, practice name, actor email..."
              className="w-full pl-9 pr-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 transition-all placeholder:text-slate-400 font-medium"
            />
          </div>

          {/* Entity Type Filter */}
          <div>
            <select
              value={entityTypeFilter}
              onChange={(e) => {
                setEntityTypeFilter(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 transition-all font-medium text-slate-700"
            >
              <option value="">All Entities</option>
              <option value="ORGANIZATION">Practice Tenant</option>
              <option value="USER">User Account</option>
              <option value="APPLICATION_FEEDBACK">Feedback Ops</option>
              <option value="MARKETPLACE_LEAD">Marketplace</option>
              <option value="SUBSCRIPTION">SaaS Subscription</option>
              <option value="ROLE">Role & Permissions</option>
              <option value="CLIENT">Practice Client</option>
              <option value="GST_PROFILE">GST Compliance</option>
              <option value="ITR_PROFILE">ITR Computation</option>
              <option value="INVOICE">Invoice</option>
            </select>
          </div>

          {/* Action Filter */}
          <div>
            <select
              value={actionFilter}
              onChange={(e) => {
                setActionFilter(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 transition-all font-medium text-slate-700"
            >
              <option value="">All Actions</option>
              <option value="PRACTICE_VERIFIED">Practice Verified</option>
              <option value="PRACTICE_CREATED">Practice Created</option>
              <option value="PRACTICE_SUSPENDED">Practice Suspended</option>
              <option value="APPLICATION_FEEDBACK_CREATED">Feedback Created</option>
              <option value="FEEDBACK_STATUS_UPDATED">Feedback Resolved</option>
              <option value="FEEDBACK_ESCALATED">Feedback Escalated</option>
              <option value="CUSTOMER_PROFILE_CREATED">Customer Registered</option>
              <option value="USER_ROLES_ASSIGNED">Roles Assigned</option>
              <option value="SUBSCRIPTION_UPDATED">Subscription Updated</option>
              <option value="SECURITY_EVENT">Security Event</option>
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
            {(searchTerm || entityTypeFilter || actionFilter || statusFilter) && (
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
      {/* 3. Audit Records Table & Mobile Cards                                     */}
      {/* ========================================================================= */}
      <div className="bg-white border border-slate-200/90 rounded-2xl shadow-card overflow-hidden">
        {/* Desktop / Tablet Table */}
        <div className="hidden md:block overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/80 font-bold text-slate-500 uppercase tracking-wider">
                <th className="px-5 py-3.5">Timestamp</th>
                <th className="px-5 py-3.5">Activity</th>
                <th className="px-5 py-3.5">Actor</th>
                <th className="px-5 py-3.5">Target / Practice</th>
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
                      <span className="font-semibold text-slate-500">Loading audit records...</span>
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
                      <span className="font-bold text-slate-800">No audit activity yet</span>
                      <p className="text-slate-400 text-xs max-w-sm">
                        Platform activity will appear here when administrative or security actions occur.
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

                    {/* Activity Title */}
                    <td className="px-5 py-3.5">
                      <div>
                        <p className="font-bold text-slate-900">
                          {logItem.displayAction || formatDisplayAction(logItem)}
                        </p>
                        <span className="text-[10px] font-mono text-slate-400 bg-slate-100 px-1.5 py-0.2 rounded border border-slate-200 inline-block mt-0.5">
                          {logItem.action}
                        </span>
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

                    {/* Target / Practice */}
                    <td className="px-5 py-3.5">
                      <div>
                        <span className="font-bold text-slate-800 block truncate max-w-[180px]">
                          {logItem.practiceName || logItem.organizationName || logItem.targetDisplayName || 'Platform Global'}
                        </span>
                        <span className="text-[10px] text-purple-700 font-semibold bg-purple-50 px-1.5 py-0.2 rounded-full border border-purple-200 inline-block mt-0.5">
                          {logItem.displayEntityType || logItem.entityType}
                        </span>
                      </div>
                    </td>

                    {/* Status */}
                    <td className="px-5 py-3.5 whitespace-nowrap">
                      <span className={clsx(
                        'px-2.5 py-0.5 rounded-full text-[10px] font-bold border inline-flex items-center gap-1',
                        logItem.status === 'ALERT' || logItem.severity === 'WARNING' || logItem.severity === 'CRITICAL'
                          ? 'bg-amber-50 text-amber-800 border-amber-200'
                          : 'bg-emerald-50 text-emerald-800 border-emerald-200'
                      )}>
                        <span className={clsx(
                          'w-1.5 h-1.5 rounded-full',
                          logItem.status === 'ALERT' || logItem.severity === 'WARNING' || logItem.severity === 'CRITICAL'
                            ? 'bg-amber-500'
                            : 'bg-emerald-500'
                        )}></span>
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
              <span className="font-semibold text-xs text-slate-500">Loading audit records...</span>
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
              <span className="font-bold text-xs text-slate-800">No audit activity yet</span>
              <p className="text-slate-400 text-[11px] max-w-xs mx-auto mt-1">
                Platform activity will appear here when administrative or security actions occur.
              </p>
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
                  <span className={clsx(
                    'shrink-0 px-2 py-0.5 rounded-full text-[10px] font-bold border inline-flex items-center gap-1',
                    logItem.status === 'ALERT' || logItem.severity === 'WARNING' || logItem.severity === 'CRITICAL'
                      ? 'bg-amber-50 text-amber-800 border-amber-200'
                      : 'bg-emerald-50 text-emerald-800 border-emerald-200'
                  )}>
                    <span className={clsx(
                      'w-1.5 h-1.5 rounded-full',
                      logItem.status === 'ALERT' || logItem.severity === 'WARNING' || logItem.severity === 'CRITICAL'
                        ? 'bg-amber-500'
                        : 'bg-emerald-500'
                    )}></span>
                    {logItem.status || 'SUCCESS'}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-100 text-[11px]">
                  <div>
                    <span className="text-slate-400 block font-medium uppercase text-[10px]">Actor</span>
                    <span className="font-bold text-slate-800 truncate block">
                      {logItem.actorName || logItem.actor || 'System'}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-400 block font-medium uppercase text-[10px]">Target / Practice</span>
                    <span className="font-bold text-slate-800 truncate block">
                      {logItem.practiceName || logItem.organizationName || logItem.targetDisplayName || 'Platform Global'}
                    </span>
                  </div>
                </div>

                <div className="pt-2 flex items-center justify-between gap-2 border-t border-slate-100">
                  <span className="text-[10px] font-mono text-slate-400 bg-slate-100 px-1.5 py-0.5 rounded border border-slate-200 truncate">
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
        title="Audit Event Details"
        subtitle={selectedLog ? `${selectedLog.displayAction || formatDisplayAction(selectedLog)} • ${selectedLog.action}` : ''}
        maxWidth="2xl"
      >
        {selectedLog && (
          <div className="space-y-4 text-xs">
            {/* Meta Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3 p-4 bg-slate-50 border border-slate-200/80 rounded-xl">
              <div>
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Target / Practice</span>
                <span className="font-bold text-slate-900 mt-0.5 block truncate">
                  {selectedLog.practiceName || selectedLog.organizationName || selectedLog.targetDisplayName || 'Platform Global'}
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
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Timestamp</span>
                <span className="font-bold text-slate-800 mt-0.5 block">
                  {formatDateTime(selectedLog.timestamp || selectedLog.createdAt)}
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
                      ? (selectedLog.oldValue.startsWith('{') || selectedLog.oldValue.startsWith('[')
                          ? JSON.stringify(JSON.parse(selectedLog.oldValue), null, 2)
                          : selectedLog.oldValue)
                      : 'null (Initial Creation)'}
                  </pre>
                </div>

                <div>
                  <span className="font-bold text-slate-700 block mb-1.5 flex items-center gap-1">
                    <span>New State</span>
                  </span>
                  <pre className="p-3 bg-slate-900 text-emerald-400 font-mono text-[11px] rounded-xl overflow-x-auto max-h-52 border border-slate-800">
                    {selectedLog.newValue
                      ? (selectedLog.newValue.startsWith('{') || selectedLog.newValue.startsWith('[')
                          ? JSON.stringify(JSON.parse(selectedLog.newValue), null, 2)
                          : selectedLog.newValue)
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
