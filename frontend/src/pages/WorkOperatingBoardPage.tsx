import React, { useState, useEffect, useMemo, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  LayoutGrid,
  List,
  Search,
  Filter,
  RefreshCw,
  ExternalLink,
  AlertTriangle,
  Clock,
  CheckCircle2,
  AlertCircle,
  Building,
  User,
  Calendar,
  Layers,
  ArrowUpDown,
  ShieldAlert,
  ChevronRight,
  Briefcase,
  FileText,
  CheckSquare,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { StatusBadge } from '../components/common/StatusBadge';
import { Badge } from '../components/common/Badge';
import { workInventoryApi, clientApi, employeeApi } from '../api/endpoints';
import {
  WorkInventoryItemDto,
  WorkInventoryType,
  WorkInventoryStatus,
  WorkInventoryFilterParams,
  Client,
  Employee,
} from '../types';
import {
  KANBAN_COLUMNS,
  isWorkItemOverdue,
  getAuthoritativeNavigationUrl,
} from '../utils/workOperatingBoardUtils';
import clsx from 'clsx';

export const WorkOperatingBoardPage: React.FC = () => {
  const navigate = useNavigate();

  // View mode
  const [viewMode, setViewMode] = useState<'kanban' | 'list'>('kanban');

  // Filter state
  const [search, setSearch] = useState('');
  const [selectedWorkType, setSelectedWorkType] = useState<string>('');
  const [selectedStatus, setSelectedStatus] = useState<string>('');
  const [selectedPriority, setSelectedPriority] = useState<string>('');
  const [selectedClientId, setSelectedClientId] = useState<string>('');
  const [selectedAssigneeId, setSelectedAssigneeId] = useState<string>('');
  const [blockedOnly, setBlockedOnly] = useState(false);
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  // Pagination & Sorting state
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(50);
  const [sortBy, setSortBy] = useState('dueDate');
  const [sortDirection, setSortDirection] = useState<'ASC' | 'DESC'>('ASC');

  // Data state
  const [items, setItems] = useState<WorkInventoryItemDto[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Aux state for dropdowns
  const [clients, setClients] = useState<Client[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);

  // Fetch Aux Data
  useEffect(() => {
    let mounted = true;
    Promise.all([
      clientApi.getAll({ page: 0, size: 200 }).catch(() => ({ content: [] })),
      employeeApi.getAll({ page: 0, size: 200 }).catch(() => ({ content: [] })),
    ]).then(([clientsRes, employeesRes]) => {
      if (mounted) {
        setClients(clientsRes?.content || []);
        const eList = Array.isArray(employeesRes) ? employeesRes : (employeesRes?.content || []);
        setEmployees(eList);
      }
    });
    return () => {
      mounted = false;
    };
  }, []);

  // Request sequence tracker to avoid out-of-order stale responses
  const activeRequestIdRef = useRef(0);

  // Fetch Inventory items
  const fetchInventory = useCallback(async () => {
    const currentRequestId = ++activeRequestIdRef.current;
    setIsLoading(true);
    setError(null);
    try {
      const params: WorkInventoryFilterParams = {
        page,
        size: pageSize,
        sortBy,
        sortDirection,
      };

      if (search.trim()) params.search = search.trim();
      if (selectedWorkType) params.workType = selectedWorkType as WorkInventoryType;
      if (selectedStatus) params.status = selectedStatus as WorkInventoryStatus;
      if (selectedPriority) params.priority = selectedPriority;
      if (selectedClientId) params.clientId = selectedClientId;
      if (selectedAssigneeId) params.assignedUserId = selectedAssigneeId;
      if (blockedOnly) params.blockedOnly = true;
      if (fromDate) params.fromDate = fromDate;
      if (toDate) params.toDate = toDate;

      const paged = await workInventoryApi.getInventory(params);
      if (currentRequestId === activeRequestIdRef.current) {
        setItems(paged.content || []);
        setTotalElements(paged.totalElements || 0);
        setTotalPages(paged.totalPages || 0);
      }
    } catch (err: any) {
      if (currentRequestId === activeRequestIdRef.current) {
        console.error('Failed to load work inventory', err);
        setError(err?.response?.data?.message || err?.message || 'Failed to load work inventory');
      }
    } finally {
      if (currentRequestId === activeRequestIdRef.current) {
        setIsLoading(false);
      }
    }
  }, [
    page,
    pageSize,
    sortBy,
    sortDirection,
    search,
    selectedWorkType,
    selectedStatus,
    selectedPriority,
    selectedClientId,
    selectedAssigneeId,
    blockedOnly,
    fromDate,
    toDate,
  ]);

  useEffect(() => {
    fetchInventory();
  }, [fetchInventory]);

  // Derived metrics from current loaded items
  const overdueCount = useMemo(() => {
    return items.filter(isWorkItemOverdue).length;
  }, [items]);

  const blockedCount = useMemo(() => {
    return items.filter((item) => item.blocked).length;
  }, [items]);

  const highPriorityCount = useMemo(() => {
    return items.filter(
      (item) => item.priority === 'HIGH' || item.priority === 'CRITICAL' || item.priority === 'URGENT'
    ).length;
  }, [items]);

  // Group items by Kanban column
  const kanbanBuckets = useMemo(() => {
    const buckets: Record<string, WorkInventoryItemDto[]> = {};
    KANBAN_COLUMNS.forEach((col) => {
      buckets[col.id] = [];
    });

    items.forEach((item) => {
      // Find matching column
      const matchedCol = KANBAN_COLUMNS.find((col) => col.statuses.includes(item.normalizedStatus));
      if (matchedCol) {
        buckets[matchedCol.id].push(item);
      } else {
        // Fallback to in-progress if unmapped
        buckets['in_progress'].push(item);
      }
    });

    return buckets;
  }, [items]);

  const clearAllFilters = () => {
    setSearch('');
    setSelectedWorkType('');
    setSelectedStatus('');
    setSelectedPriority('');
    setSelectedClientId('');
    setSelectedAssigneeId('');
    setBlockedOnly(false);
    setFromDate('');
    setToDate('');
    setPage(0);
  };

  const hasActiveFilters =
    Boolean(search) ||
    Boolean(selectedWorkType) ||
    Boolean(selectedStatus) ||
    Boolean(selectedPriority) ||
    Boolean(selectedClientId) ||
    Boolean(selectedAssigneeId) ||
    blockedOnly ||
    Boolean(fromDate) ||
    Boolean(toDate);

  const getWorkTypeBadge = (type: WorkInventoryType) => {
    switch (type) {
      case 'WORK_INSTANCE':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-blue-900/60 text-blue-300 border border-blue-700/50">
            <Layers className="w-3 h-3 text-blue-400" />
            Service Work
          </span>
        );
      case 'COMPLIANCE_OBLIGATION':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-purple-900/60 text-purple-300 border border-purple-700/50">
            <Briefcase className="w-3 h-3 text-purple-400" />
            Compliance
          </span>
        );
      case 'TASK':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-slate-800 text-slate-300 border border-slate-700">
            <CheckSquare className="w-3 h-3 text-slate-400" />
            Task
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-slate-800 text-slate-300">
            {type}
          </span>
        );
    }
  };

  const getPriorityBadge = (priority: string) => {
    const p = (priority || '').toUpperCase();
    if (p === 'CRITICAL' || p === 'URGENT') {
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-rose-950/80 text-rose-300 border border-rose-700/60">
          <ShieldAlert className="w-3 h-3 text-rose-400" />
          {p}
        </span>
      );
    }
    if (p === 'HIGH') {
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-amber-950/80 text-amber-300 border border-amber-700/60">
          HIGH
        </span>
      );
    }
    if (p === 'LOW') {
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-slate-800 text-slate-400 border border-slate-700">
          LOW
        </span>
      );
    }
    return (
      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-slate-800/60 text-slate-300 border border-slate-700/50">
        MED
      </span>
    );
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 sm:p-6 lg:p-8 space-y-6">
      {/* Header & Primary Stats */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-teal-500/10 border border-teal-500/30 text-teal-400">
              <LayoutGrid className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
                Work Operating Board
                <span className="text-xs px-2.5 py-0.5 rounded-full font-semibold bg-teal-950/80 text-teal-300 border border-teal-700/60">
                  Authoritative
                </span>
              </h1>
              <p className="text-sm text-slate-400 mt-0.5">
                Unified live inventory of service work instances, statutory compliance obligations, and operational tasks.
              </p>
            </div>
          </div>
        </div>

        {/* View Switcher & Action buttons */}
        <div className="flex items-center gap-2 self-start md:self-auto">
          {/* Quick Metrics */}
          <div className="hidden lg:flex items-center gap-2 mr-2">
            <div className="px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-800 flex items-center gap-2 text-xs">
              <span className="text-slate-400">Total Work:</span>
              <span className="font-semibold text-white">{totalElements}</span>
            </div>
            {overdueCount > 0 && (
              <div className="px-3 py-1.5 rounded-lg bg-rose-950/60 border border-rose-800/60 flex items-center gap-1.5 text-xs text-rose-300">
                <AlertTriangle className="w-3.5 h-3.5 text-rose-400" />
                <span>Overdue:</span>
                <span className="font-bold">{overdueCount}</span>
              </div>
            )}
            {blockedCount > 0 && (
              <div className="px-3 py-1.5 rounded-lg bg-amber-950/60 border border-amber-800/60 flex items-center gap-1.5 text-xs text-amber-300">
                <AlertCircle className="w-3.5 h-3.5 text-amber-400" />
                <span>Blocked:</span>
                <span className="font-bold">{blockedCount}</span>
              </div>
            )}
          </div>

          {/* Kanban / List Toggle */}
          <div className="inline-flex p-1 rounded-xl bg-slate-900 border border-slate-800">
            <button
              onClick={() => setViewMode('kanban')}
              className={clsx(
                'inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all',
                viewMode === 'kanban'
                  ? 'bg-teal-500/20 text-teal-300 border border-teal-500/40 shadow-sm'
                  : 'text-slate-400 hover:text-white'
              )}
            >
              <LayoutGrid className="w-3.5 h-3.5" />
              Kanban
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={clsx(
                'inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all',
                viewMode === 'list'
                  ? 'bg-teal-500/20 text-teal-300 border border-teal-500/40 shadow-sm'
                  : 'text-slate-400 hover:text-white'
              )}
            >
              <List className="w-3.5 h-3.5" />
              List
            </button>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={fetchInventory}
            disabled={isLoading}
            className="flex items-center gap-1.5 border-slate-800 bg-slate-900 text-slate-300 hover:text-white"
          >
            <RefreshCw className={clsx('w-3.5 h-3.5', isLoading && 'animate-spin')} />
            Refresh
          </Button>
        </div>
      </div>

      {/* Filter and Control Toolbar */}
      <div className="p-4 rounded-2xl bg-slate-900/70 border border-slate-800/80 backdrop-blur-sm space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
          {/* Search Box */}
          <div className="relative lg:col-span-2">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
            <input
              type="text"
              placeholder="Search title, client, PAN, rule..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="w-full pl-9 pr-3 py-2 text-xs rounded-xl bg-slate-950 border border-slate-800 text-white placeholder-slate-500 focus:outline-none focus:border-teal-500 transition-colors"
            />
          </div>

          {/* Work Type Filter */}
          <div>
            <select
              value={selectedWorkType}
              onChange={(e) => {
                setSelectedWorkType(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-xs rounded-xl bg-slate-950 border border-slate-800 text-slate-300 focus:outline-none focus:border-teal-500"
            >
              <option value="">All Work Types</option>
              <option value="WORK_INSTANCE">Service Work Instances</option>
              <option value="COMPLIANCE_OBLIGATION">Compliance Obligations</option>
              <option value="TASK">Standalone Tasks</option>
            </select>
          </div>

          {/* Status Filter */}
          <div>
            <select
              value={selectedStatus}
              onChange={(e) => {
                setSelectedStatus(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-xs rounded-xl bg-slate-950 border border-slate-800 text-slate-300 focus:outline-none focus:border-teal-500"
            >
              <option value="">All Statuses</option>
              <option value="NOT_STARTED">To Do / Not Started</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="BLOCKED">Blocked</option>
              <option value="UNDER_REVIEW">Under Review</option>
              <option value="READY_FOR_FILING">Ready for Filing</option>
              <option value="FILED">Filed</option>
              <option value="COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>

          {/* Priority Filter */}
          <div>
            <select
              value={selectedPriority}
              onChange={(e) => {
                setSelectedPriority(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-xs rounded-xl bg-slate-950 border border-slate-800 text-slate-300 focus:outline-none focus:border-teal-500"
            >
              <option value="">All Priorities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>

          {/* Client Filter */}
          <div>
            <select
              value={selectedClientId}
              onChange={(e) => {
                setSelectedClientId(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-xs rounded-xl bg-slate-950 border border-slate-800 text-slate-300 focus:outline-none focus:border-teal-500"
            >
              <option value="">All Clients</option>
              {clients.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.displayName || c.legalName || c.pan}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Secondary Filter Line */}
        <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-800/60 text-xs">
          <div className="flex flex-wrap items-center gap-3">
            {/* Assignee Filter */}
            <div className="flex items-center gap-1.5">
              <span className="text-slate-400">Assignee:</span>
              <select
                value={selectedAssigneeId}
                onChange={(e) => {
                  setSelectedAssigneeId(e.target.value);
                  setPage(0);
                }}
                className="px-2.5 py-1 text-xs rounded-lg bg-slate-950 border border-slate-800 text-slate-300 focus:outline-none focus:border-teal-500"
              >
                <option value="">Anyone</option>
                {employees.map((emp) => (
                  <option key={emp.id} value={emp.userId || emp.id}>
                    {emp.fullName || `${emp.firstName || ''} ${emp.lastName || ''}`.trim() || emp.designation || emp.employeeCode || emp.id}
                  </option>
                ))}
              </select>
            </div>

            {/* Blocked Only Toggle */}
            <label className="flex items-center gap-2 cursor-pointer select-none text-slate-300">
              <input
                type="checkbox"
                checked={blockedOnly}
                onChange={(e) => {
                  setBlockedOnly(e.target.checked);
                  setPage(0);
                }}
                className="w-4 h-4 rounded border-slate-700 bg-slate-950 text-rose-500 focus:ring-rose-500 focus:ring-offset-slate-900"
              />
              <span className={clsx('font-medium', blockedOnly && 'text-rose-400 font-semibold')}>
                Blocked Only
              </span>
            </label>

            {/* Date Range */}
            <div className="flex items-center gap-1.5 text-slate-400">
              <span>Due:</span>
              <input
                type="date"
                value={fromDate}
                onChange={(e) => {
                  setFromDate(e.target.value);
                  setPage(0);
                }}
                className="px-2 py-0.5 text-xs rounded-lg bg-slate-950 border border-slate-800 text-slate-300"
              />
              <span>to</span>
              <input
                type="date"
                value={toDate}
                onChange={(e) => {
                  setToDate(e.target.value);
                  setPage(0);
                }}
                className="px-2 py-0.5 text-xs rounded-lg bg-slate-950 border border-slate-800 text-slate-300"
              />
            </div>
          </div>

          {/* Reset Filters & Page Size */}
          <div className="flex items-center gap-2">
            {hasActiveFilters && (
              <button
                onClick={clearAllFilters}
                className="text-xs text-teal-400 hover:text-teal-300 font-medium underline"
              >
                Clear all filters
              </button>
            )}

            <div className="flex items-center gap-1 text-slate-400">
              <span>Show:</span>
              <select
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value));
                  setPage(0);
                }}
                className="px-2 py-0.5 text-xs rounded-lg bg-slate-950 border border-slate-800 text-slate-300"
              >
                <option value={20}>20</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="p-4 rounded-xl bg-rose-950/50 border border-rose-800/80 text-rose-200 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-5 h-5 text-rose-400" />
            <span>{error}</span>
          </div>
          <Button variant="outline" size="sm" onClick={fetchInventory}>
            Try again
          </Button>
        </div>
      )}

      {/* Loading Skeleton */}
      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-4">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div
              key={i}
              className="h-96 rounded-2xl bg-slate-900/50 border border-slate-800/60 p-4 space-y-4 animate-pulse"
            >
              <div className="h-6 w-3/4 bg-slate-800 rounded" />
              <div className="h-28 bg-slate-800/60 rounded-xl" />
              <div className="h-28 bg-slate-800/60 rounded-xl" />
            </div>
          ))}
        </div>
      )}

      {/* Content Rendering: Kanban or List */}
      {!isLoading && items.length === 0 && (
        <div className="p-12 text-center rounded-2xl bg-slate-900/40 border border-slate-800/60">
          <Layers className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-lg font-semibold text-white">No work items found</h3>
          <p className="text-sm text-slate-400 mt-1 max-w-md mx-auto">
            {hasActiveFilters
              ? 'No work items match the selected filter criteria. Try clearing or relaxing some filters.'
              : 'There are currently no active work instances, obligations, or tasks in your practice.'}
          </p>
          {hasActiveFilters && (
            <Button variant="outline" size="sm" onClick={clearAllFilters} className="mt-4">
              Clear filters
            </Button>
          )}
        </div>
      )}

      {/* View: KANBAN */}
      {!isLoading && items.length > 0 && viewMode === 'kanban' && (
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4 items-start">
            {KANBAN_COLUMNS.map((column) => {
              const colItems = kanbanBuckets[column.id] || [];

              return (
                <div
                  key={column.id}
                  className="flex flex-col rounded-2xl bg-slate-900/40 border border-slate-800/80 overflow-hidden min-h-[450px]"
                >
                  {/* Column Header */}
                  <div
                    className={clsx(
                      'p-3 border-b flex items-center justify-between sticky top-0 backdrop-blur-md z-10',
                      column.headerBg
                    )}
                  >
                    <div className="flex items-center gap-2">
                      <span className={clsx('w-2 h-2 rounded-full', column.dotColor)} />
                      <h3 className={clsx('text-xs font-bold tracking-wide uppercase', column.textColor)}>
                        {column.title}
                      </h3>
                    </div>
                    <span
                      className={clsx(
                        'text-xs font-bold px-2 py-0.5 rounded-full',
                        column.badgeBg
                      )}
                    >
                      {colItems.length}
                    </span>
                  </div>

                  {/* Column Cards Container */}
                  <div className="p-2 space-y-2.5 flex-1 overflow-y-auto max-h-[calc(100vh-280px)]">
                    {colItems.length === 0 ? (
                      <div className="h-32 flex items-center justify-center border border-dashed border-slate-800/80 rounded-xl text-xs text-slate-600">
                        Empty
                      </div>
                    ) : (
                      colItems.map((item) => {
                        const isOverdue = isWorkItemOverdue(item);
                        const detailUrl = getAuthoritativeNavigationUrl(item);

                        const isCancelled = item.normalizedStatus === 'CANCELLED';

                        return (
                          <div
                            key={item.id}
                            className={clsx(
                              'group relative p-3 rounded-xl bg-slate-900 border text-left transition-all hover:shadow-lg',
                              isCancelled
                                ? 'border-slate-800/60 bg-slate-950/40 opacity-75'
                                : isOverdue
                                ? 'border-rose-800/60 bg-rose-950/20 hover:border-slate-600'
                                : item.blocked
                                ? 'border-amber-800/60 bg-amber-950/10 hover:border-slate-600'
                                : 'border-slate-800/90 hover:border-slate-600'
                            )}
                          >
                            {/* Card Top: Work Type & Priority */}
                            <div className="flex items-center justify-between gap-2 mb-2">
                              {getWorkTypeBadge(item.workType)}
                              {isCancelled ? (
                                <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-slate-800 text-slate-400 border border-slate-700">
                                  CANCELLED
                                </span>
                              ) : (
                                getPriorityBadge(item.priority)
                              )}
                            </div>

                            {/* Title */}
                            <h4
                              className={clsx(
                                'text-xs font-semibold leading-snug line-clamp-2 transition-colors',
                                isCancelled
                                  ? 'text-slate-400 line-through group-hover:text-slate-300'
                                  : 'text-white group-hover:text-teal-300'
                              )}
                            >
                              {item.title}
                            </h4>

                            {/* Client & PAN */}
                            {item.clientName && (
                              <div className="mt-2 flex items-center gap-1.5 text-xs text-slate-300">
                                <Building className="w-3 h-3 text-slate-500 shrink-0" />
                                <span className="truncate">{item.clientName}</span>
                                {item.clientPan && (
                                  <span className="text-[10px] text-slate-500 font-mono">
                                    ({item.clientPan})
                                  </span>
                                )}
                              </div>
                            )}

                            {/* Engagement Context */}
                            {item.engagementName && (
                              <div className="mt-1 flex items-center gap-1 text-[11px] text-slate-400">
                                <Layers className="w-3 h-3 text-slate-500 shrink-0" />
                                <span className="truncate">{item.engagementName}</span>
                              </div>
                            )}

                            {/* Blocked Reason Callout */}
                            {item.blocked && (
                              <div className="mt-2 p-1.5 rounded-lg bg-rose-950/70 border border-rose-800/80 text-[11px] text-rose-300 flex items-start gap-1">
                                <AlertCircle className="w-3.5 h-3.5 text-rose-400 shrink-0 mt-0.5" />
                                <span className="line-clamp-2">
                                  {item.blockedReason || 'Blocked — Action required'}
                                </span>
                              </div>
                            )}

                            {/* Next Actionable Step */}
                            {item.nextActionableStep && !item.blocked && (
                              <div className="mt-2 text-[11px] text-slate-400 flex items-center gap-1">
                                <ChevronRight className="w-3 h-3 text-teal-400 shrink-0" />
                                <span className="truncate italic">{item.nextActionableStep}</span>
                              </div>
                            )}

                            {/* Task Progress (if available) */}
                            {item.totalTasks > 0 && (
                              <div className="mt-2 flex items-center justify-between text-[11px] text-slate-400 border-t border-slate-800/60 pt-1.5">
                                <span>Tasks</span>
                                <span className="font-medium text-slate-300">
                                  {item.completedTasks} / {item.totalTasks}
                                </span>
                              </div>
                            )}

                            {/* Footer: Due Date & Assignee */}
                            <div className="mt-2.5 pt-2 border-t border-slate-800 flex items-center justify-between gap-2 text-xs">
                              {/* Due Date */}
                              <div
                                className={clsx(
                                  'flex items-center gap-1 text-[11px] font-medium',
                                  isOverdue ? 'text-rose-400 font-bold' : 'text-slate-400'
                                )}
                              >
                                <Calendar className="w-3 h-3" />
                                <span>{item.dueDate ? item.dueDate : 'No Due Date'}</span>
                                {isOverdue && (
                                  <span className="text-[10px] px-1 py-0.2 rounded bg-rose-900/80 text-rose-200">
                                    OVERDUE
                                  </span>
                                )}
                              </div>

                              {/* Assignee */}
                              <div className="flex items-center gap-1 text-[11px] text-slate-300 truncate max-w-[100px]">
                                <User className="w-3 h-3 text-slate-500 shrink-0" />
                                <span className="truncate">{item.assignedUserName || 'Unassigned'}</span>
                              </div>
                            </div>

                            {/* Authoritative Detail Link */}
                            <div className="mt-2 pt-2 border-t border-slate-800/80 flex justify-end">
                              <button
                                onClick={() => navigate(detailUrl)}
                                className="inline-flex items-center gap-1 text-xs text-teal-400 hover:text-teal-300 font-medium"
                              >
                                View Details
                                <ExternalLink className="w-3 h-3" />
                              </button>
                            </div>
                          </div>
                        );
                      })
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Kanban Pagination / Element Count Info */}
          <div className="p-3 rounded-xl bg-slate-900/50 border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-400">
            <div>
              Showing page <span className="font-semibold text-white">{page + 1}</span> of{' '}
              <span className="font-semibold text-white">{Math.max(1, totalPages)}</span> ({totalElements} total work items)
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* View: LIST */}
      {!isLoading && items.length > 0 && viewMode === 'list' && (
        <div className="space-y-4">
          <div className="overflow-x-auto rounded-2xl border border-slate-800 bg-slate-900/60">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950/80 text-slate-400 font-semibold uppercase tracking-wider border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3">Work Item</th>
                  <th className="px-3 py-3">Type</th>
                  <th className="px-4 py-3">Client</th>
                  <th className="px-3 py-3">Status</th>
                  <th className="px-3 py-3">Priority</th>
                  <th className="px-3 py-3">Assignee</th>
                  <th className="px-3 py-3">Due Date</th>
                  <th className="px-3 py-3">Next Step</th>
                  <th className="px-4 py-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {items.map((item) => {
                  const isOverdue = isWorkItemOverdue(item);
                  const isCancelled = item.normalizedStatus === 'CANCELLED';
                  const detailUrl = getAuthoritativeNavigationUrl(item);

                  return (
                    <tr
                      key={item.id}
                      className={clsx(
                        'hover:bg-slate-800/40 transition-colors',
                        isCancelled
                          ? 'opacity-70 bg-slate-950/30'
                          : isOverdue
                          ? 'bg-rose-950/10'
                          : item.blocked
                          ? 'bg-amber-950/10'
                          : ''
                      )}
                    >
                      {/* Title & Engagement */}
                      <td className="px-4 py-3">
                        <div
                          className={clsx(
                            'font-semibold line-clamp-1',
                            isCancelled ? 'text-slate-400 line-through' : 'text-white'
                          )}
                        >
                          {item.title}
                        </div>
                        {item.engagementName && (
                          <div className="text-[11px] text-slate-500 flex items-center gap-1 mt-0.5">
                            <Layers className="w-3 h-3" />
                            {item.engagementName}
                          </div>
                        )}
                        {item.blocked && (
                          <div className="text-[11px] text-rose-400 flex items-center gap-1 mt-0.5">
                            <AlertCircle className="w-3 h-3" />
                            {item.blockedReason || 'Blocked'}
                          </div>
                        )}
                      </td>

                      {/* Type */}
                      <td className="px-3 py-3">{getWorkTypeBadge(item.workType)}</td>

                      {/* Client */}
                      <td className="px-4 py-3">
                        <div className="text-white font-medium">{item.clientName || '—'}</div>
                        {item.clientPan && (
                          <div className="text-[10px] text-slate-500 font-mono">{item.clientPan}</div>
                        )}
                      </td>

                      {/* Status */}
                      <td className="px-3 py-3">
                        <StatusBadge status={item.normalizedStatus} size="sm" />
                      </td>

                      {/* Priority */}
                      <td className="px-3 py-3">{getPriorityBadge(item.priority)}</td>

                      {/* Assignee */}
                      <td className="px-3 py-3">
                        <span className="text-slate-200">{item.assignedUserName || 'Unassigned'}</span>
                      </td>

                      {/* Due Date */}
                      <td className="px-3 py-3">
                        <div
                          className={clsx(
                            'font-medium flex items-center gap-1',
                            isOverdue ? 'text-rose-400 font-bold' : 'text-slate-300'
                          )}
                        >
                          <Calendar className="w-3 h-3" />
                          <span>{item.dueDate || '—'}</span>
                          {isOverdue && (
                            <span className="text-[9px] px-1 py-0.2 rounded bg-rose-900/80 text-rose-200">
                              OVERDUE
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Next Step */}
                      <td className="px-3 py-3 max-w-xs truncate text-slate-400 italic">
                        {item.nextActionableStep || '—'}
                      </td>

                      {/* Action */}
                      <td className="px-4 py-3 text-right">
                        <button
                          onClick={() => navigate(detailUrl)}
                          className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-800 text-teal-300 hover:bg-slate-700 transition-colors"
                        >
                          Details
                          <ExternalLink className="w-3 h-3" />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* List Pagination Controls */}
          <div className="p-3 rounded-xl bg-slate-900/50 border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-400">
            <div>
              Showing <span className="font-semibold text-white">{items.length}</span> of{' '}
              <span className="font-semibold text-white">{totalElements}</span> work items (Page {page + 1} of{' '}
              {Math.max(1, totalPages)})
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
