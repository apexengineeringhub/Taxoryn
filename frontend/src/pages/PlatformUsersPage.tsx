import React, { useEffect, useState, useCallback } from 'react';
import {
  Users,
  Search,
  CheckCircle2,
  AlertTriangle,
  RefreshCw,
  Mail,
  Phone,
  UserPlus,
  Edit2,
  X,
  ChevronDown,
  ChevronRight,
  Building2,
  ShieldCheck,
  List,
  GitFork,
  MapPin,
  ExternalLink,
  Lock,
} from 'lucide-react';
import { adminUserApi, adminPracticeUserApi } from '../api/endpoints';
import { User, PracticeUserSummary, AdminUserSummary } from '../types';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { useAuth } from '../context/AuthContext';
import clsx from 'clsx';

export const PlatformUsersPage: React.FC = () => {
  const { user: currentUser } = useAuth();

  // View Mode: 'tree' (Practice -> Admin -> Users) or 'list' (Flat Users)
  const [viewMode, setViewMode] = useState<'tree' | 'list'>('tree');

  // --- Tree View State (Practice Hierarchy) ---
  const [practices, setPractices] = useState<PracticeUserSummary[]>([]);
  const [isPracticesLoading, setIsPracticesLoading] = useState(true);
  const [practiceSearchTerm, setPracticeSearchTerm] = useState('');
  const [practiceStatusFilter, setPracticeStatusFilter] = useState('ALL');
  const [practicePage, setPracticePage] = useState(0);
  const [practiceTotalPages, setPracticeTotalPages] = useState(1);
  const [practiceTotalElements, setPracticeTotalElements] = useState(0);

  // Expanded practice IDs
  const [expandedPracticeIds, setExpandedPracticeIds] = useState<Set<string>>(new Set());

  // Lazy loaded practice users cache: { [orgId]: { users: User[], isLoading: boolean, page: number, totalPages: number, totalElements: number, search: string, role: string, status: string } }
  interface PracticeUsersCache {
    users: User[];
    isLoading: boolean;
    page: number;
    totalPages: number;
    totalElements: number;
    search: string;
    role: string;
    status: string;
  }
  const [practiceUsersMap, setPracticeUsersMap] = useState<Record<string, PracticeUsersCache>>({});

  // --- List View State (Flat Users) ---
  const [flatUsers, setFlatUsers] = useState<User[]>([]);
  const [isFlatUsersLoading, setIsFlatUsersLoading] = useState(false);
  const [flatSearchTerm, setFlatSearchTerm] = useState('');
  const [flatRoleFilter, setFlatRoleFilter] = useState('ALL');
  const [flatStatusFilter, setFlatStatusFilter] = useState('ALL');
  const [flatPage, setFlatPage] = useState(0);
  const [flatTotalPages, setFlatTotalPages] = useState(1);
  const [flatTotalElements, setFlatTotalElements] = useState(0);

  // Global Notifications
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Status Modal State
  const [selectedUser, setSelectedUser] = useState<User | AdminUserSummary | null>(null);
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [targetStatus, setTargetStatus] = useState<string>('ACTIVE');
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);

  // Role Modal State
  const [isRoleModalOpen, setIsRoleModalOpen] = useState(false);
  const [targetRoleCode, setTargetRoleCode] = useState<string>('TAXORYN_OPERATIONS_ADMIN');
  const [isUpdatingRole, setIsUpdatingRole] = useState(false);

  // Create User Modal State
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [newUserData, setNewUserData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    roleCode: 'TAXORYN_OPERATIONS_ADMIN',
    status: 'ACTIVE',
    temporaryPassword: '',
  });

  const currentUserRoleCodes = (currentUser?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isSuperAdmin = currentUserRoleCodes.includes('TAXORYN_SUPERADMIN') || currentUserRoleCodes.includes('SUPER_ADMIN');

  // ===========================================================================
  // 1. Load Practices (Tree View)
  // ===========================================================================
  const loadPractices = useCallback(async (page: number = 0) => {
    try {
      setIsPracticesLoading(true);
      setErrorMessage(null);
      const res = await adminPracticeUserApi.getPracticeSummaries({
        search: practiceSearchTerm.trim() || undefined,
        status: practiceStatusFilter !== 'ALL' ? practiceStatusFilter : undefined,
        page,
        size: 20,
      });
      setPractices(res?.content || []);
      setPracticePage(res?.pageNumber || 0);
      setPracticeTotalPages(res?.totalPages || 1);
      setPracticeTotalElements(res?.totalElements || 0);
    } catch (err: any) {
      console.error('Failed to load practice summaries', err);
      setErrorMessage(err?.response?.data?.message || 'Failed to load practice summaries.');
    } finally {
      setIsPracticesLoading(false);
    }
  }, [practiceSearchTerm, practiceStatusFilter]);

  useEffect(() => {
    if (viewMode === 'tree') {
      loadPractices(0);
    }
  }, [loadPractices, viewMode]);

  // ===========================================================================
  // 2. Load Practice Users on Expand (Lazy Loading)
  // ===========================================================================
  const loadPracticeUsers = async (
    orgId: string,
    page: number = 0,
    search?: string,
    role?: string,
    status?: string
  ) => {
    setPracticeUsersMap((prev) => ({
      ...prev,
      [orgId]: {
        users: prev[orgId]?.users || [],
        isLoading: true,
        page,
        totalPages: prev[orgId]?.totalPages || 1,
        totalElements: prev[orgId]?.totalElements || 0,
        search: search !== undefined ? search : prev[orgId]?.search || '',
        role: role !== undefined ? role : prev[orgId]?.role || 'ALL',
        status: status !== undefined ? status : prev[orgId]?.status || 'ALL',
      },
    }));

    try {
      const activeSearch = search !== undefined ? search : practiceUsersMap[orgId]?.search;
      const activeRole = role !== undefined ? role : practiceUsersMap[orgId]?.role;
      const activeStatus = status !== undefined ? status : practiceUsersMap[orgId]?.status;

      const res = await adminPracticeUserApi.getPracticeUsers(orgId, {
        search: activeSearch?.trim() || undefined,
        role: activeRole !== 'ALL' ? activeRole : undefined,
        status: activeStatus !== 'ALL' ? activeStatus : undefined,
        page,
        size: 15,
      });

      setPracticeUsersMap((prev) => ({
        ...prev,
        [orgId]: {
          users: res?.content || [],
          isLoading: false,
          page: res?.pageNumber || 0,
          totalPages: res?.totalPages || 1,
          totalElements: res?.totalElements || 0,
          search: activeSearch || '',
          role: activeRole || 'ALL',
          status: activeStatus || 'ALL',
        },
      }));
    } catch (err: any) {
      console.error(`Failed to load users for practice ${orgId}`, err);
      setPracticeUsersMap((prev) => ({
        ...prev,
        [orgId]: {
          ...(prev[orgId] || {
            users: [],
            page: 0,
            totalPages: 1,
            totalElements: 0,
            search: '',
            role: 'ALL',
            status: 'ALL',
          }),
          isLoading: false,
        },
      }));
      setErrorMessage(`Failed to load users for practice.`);
    }
  };

  const togglePracticeExpand = (orgId: string) => {
    setExpandedPracticeIds((prev) => {
      const next = new Set(prev);
      if (next.has(orgId)) {
        next.delete(orgId);
      } else {
        next.add(orgId);
        // If not cached, load immediately
        if (!practiceUsersMap[orgId]) {
          loadPracticeUsers(orgId, 0);
        }
      }
      return next;
    });
  };

  // ===========================================================================
  // 3. Load Flat Users (List View)
  // ===========================================================================
  const loadFlatUsers = useCallback(async (page: number = 0) => {
    try {
      setIsFlatUsersLoading(true);
      setErrorMessage(null);
      const res = await adminUserApi.getUsers({
        role: flatRoleFilter !== 'ALL' ? flatRoleFilter : undefined,
        status: flatStatusFilter !== 'ALL' ? flatStatusFilter : undefined,
        search: flatSearchTerm.trim() || undefined,
        page,
        size: 25,
      });
      setFlatUsers(res?.content || []);
      setFlatPage(res?.pageNumber || 0);
      setFlatTotalPages(res?.totalPages || 1);
      setFlatTotalElements(res?.totalElements || 0);
    } catch (err: any) {
      console.error('Failed to load flat users', err);
      setErrorMessage(err?.response?.data?.message || 'Failed to load platform users.');
    } finally {
      setIsFlatUsersLoading(false);
    }
  }, [flatRoleFilter, flatStatusFilter, flatSearchTerm]);

  useEffect(() => {
    if (viewMode === 'list') {
      loadFlatUsers(0);
    }
  }, [loadFlatUsers, viewMode]);

  // ===========================================================================
  // 4. User Mutations (Role, Status, Create)
  // ===========================================================================
  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setIsCreating(true);
      setErrorMessage(null);
      setSuccessMessage(null);
      await adminUserApi.createUser({
        firstName: newUserData.firstName.trim(),
        lastName: newUserData.lastName.trim(),
        email: newUserData.email.trim(),
        phone: newUserData.phone.trim() || undefined,
        roleCode: newUserData.roleCode,
        status: newUserData.status,
        temporaryPassword: newUserData.temporaryPassword,
      });
      setSuccessMessage(`Platform user ${newUserData.email} created successfully with role ${formatRoleDisplayName(newUserData.roleCode)}`);
      setIsCreateModalOpen(false);
      setNewUserData({
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        roleCode: 'TAXORYN_OPERATIONS_ADMIN',
        status: 'ACTIVE',
        temporaryPassword: '',
      });
      if (viewMode === 'tree') {
        loadPractices(practicePage);
      } else {
        loadFlatUsers(flatPage);
      }
    } catch (err: any) {
      console.error('Failed to create platform user', err);
      setErrorMessage(err?.response?.data?.message || 'Failed to create platform user. Privilege escalation check failed.');
    } finally {
      setIsCreating(false);
    }
  };

  const handleUpdateStatus = async () => {
    if (!selectedUser) return;
    try {
      setIsUpdatingStatus(true);
      setErrorMessage(null);
      setSuccessMessage(null);
      await adminUserApi.updateStatus(selectedUser.id, targetStatus);
      setSuccessMessage(`User ${selectedUser.email} status updated to ${targetStatus}`);
      setIsStatusModalOpen(false);
      setSelectedUser(null);
      if (viewMode === 'tree') {
        loadPractices(practicePage);
        // Refresh open practice user caches
        expandedPracticeIds.forEach((orgId) => loadPracticeUsers(orgId, practiceUsersMap[orgId]?.page || 0));
      } else {
        loadFlatUsers(flatPage);
      }
    } catch (err: any) {
      console.error('Failed to update user status', err);
      setErrorMessage(err?.response?.data?.message || 'Failed to update user status.');
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const handleUpdateRole = async () => {
    if (!selectedUser) return;
    try {
      setIsUpdatingRole(true);
      setErrorMessage(null);
      setSuccessMessage(null);
      await adminUserApi.updateRole(selectedUser.id, targetRoleCode);
      setSuccessMessage(`User ${selectedUser.email} role updated to ${formatRoleDisplayName(targetRoleCode)}`);
      setIsRoleModalOpen(false);
      setSelectedUser(null);
      if (viewMode === 'tree') {
        loadPractices(practicePage);
        expandedPracticeIds.forEach((orgId) => loadPracticeUsers(orgId, practiceUsersMap[orgId]?.page || 0));
      } else {
        loadFlatUsers(flatPage);
      }
    } catch (err: any) {
      console.error('Failed to update user role', err);
      setErrorMessage(err?.response?.data?.message || 'Failed to update user role. Privilege escalation denied.');
    } finally {
      setIsUpdatingRole(false);
    }
  };

  // ===========================================================================
  // Helpers & Formatters
  // ===========================================================================
  const formatRoleDisplayName = (code?: string) => {
    if (!code) return 'Standard User';
    switch (code) {
      case 'TAXORYN_SUPERADMIN':
      case 'SUPER_ADMIN':
        return 'Taxoryn SuperAdmin';
      case 'TAXORYN_OPERATIONS_ADMIN':
        return 'Operations Admin';
      case 'TAXORYN_SUPPORT_ADMIN':
        return 'Support Admin';
      case 'TAXORYN_MARKETPLACE_ADMIN':
        return 'Marketplace Admin';
      case 'TAXORYN_FINANCE_ADMIN':
        return 'Finance Admin';
      case 'TAXORYN_CONTENT_ADMIN':
        return 'Content Admin';
      case 'TAXORYN_SECURITY_ADMIN':
        return 'Security Admin';
      case 'TAXORYN_ENGINEERING_ADMIN':
        return 'Engineering Admin';
      case 'ORG_ADMIN':
      case 'PRACTICE_ADMIN':
        return 'Practice Admin';
      case 'PRACTICE_OWNER':
        return 'Practice Owner';
      case 'PRACTITIONER':
        return 'Tax Practitioner';
      case 'STAFF':
      case 'PRACTICE_EMPLOYEE':
        return 'Practice Staff';
      case 'ARTICLE_ASSISTANT':
        return 'Article Assistant';
      case 'CLIENT_USER':
      case 'PRACTICE_CLIENT':
        return 'Client User';
      case 'CLIENT_ADMIN':
        return 'Client Admin';
      case 'MARKETPLACE_CUSTOMER':
        return 'Marketplace Customer';
      default:
        return code.replace(/_/g, ' ');
    }
  };

  const getRoleBadgeStyle = (code: string) => {
    if (code.startsWith('TAXORYN_') || code === 'SUPER_ADMIN') {
      if (code.includes('SUPERADMIN') || code === 'SUPER_ADMIN') {
        return 'bg-purple-100 text-purple-800 border-purple-200';
      }
      if (code.includes('SECURITY') || code.includes('ENGINEERING')) {
        return 'bg-rose-100 text-rose-800 border-rose-200';
      }
      if (code.includes('FINANCE')) {
        return 'bg-emerald-100 text-emerald-800 border-emerald-200';
      }
      if (code.includes('MARKETPLACE')) {
        return 'bg-amber-100 text-amber-800 border-amber-200';
      }
      return 'bg-indigo-100 text-indigo-800 border-indigo-200';
    }
    if (code.startsWith('PRACTICE_') || code === 'ORG_ADMIN' || code === 'PRACTITIONER') {
      return 'bg-blue-100 text-blue-800 border-blue-200';
    }
    if (code === 'STAFF' || code === 'ARTICLE_ASSISTANT') {
      return 'bg-sky-100 text-sky-800 border-sky-200';
    }
    return 'bg-slate-100 text-slate-700 border-slate-200';
  };

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      {/* ========================================================================= */}
      {/* 1. Header                                                                 */}
      {/* ========================================================================= */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/90 shadow-card">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-purple-100 text-purple-800 border border-purple-200">
              Taxoryn Identity & RBAC
            </span>
            <span className="text-xs text-slate-400">•</span>
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              PLATFORM USER GOVERNANCE
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 flex items-center gap-2.5">
            <Users className="w-8 h-8 text-purple-600" />
            Practice Users
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Govern practices, practice administrators, and tenant users with zero N+1 aggregation and least-privilege RBAC.
          </p>
        </div>
        <div className="flex items-center gap-3">
          {/* View Mode Toggle */}
          <div className="flex items-center bg-slate-100 p-1 rounded-xl border border-slate-200/80 shadow-2xs">
            <button
              onClick={() => setViewMode('tree')}
              className={clsx(
                'flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all',
                viewMode === 'tree'
                  ? 'bg-white text-purple-900 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              )}
            >
              <GitFork className="w-3.5 h-3.5 text-purple-600 rotate-90" /> Tree View
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={clsx(
                'flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all',
                viewMode === 'list'
                  ? 'bg-white text-purple-900 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              )}
            >
              <List className="w-3.5 h-3.5 text-purple-600" /> List View
            </button>
          </div>

          <Button
            variant="secondary"
            onClick={() => (viewMode === 'tree' ? loadPractices(practicePage) : loadFlatUsers(flatPage))}
            disabled={isPracticesLoading || isFlatUsersLoading}
            className="text-xs gap-1.5 shadow-2xs font-bold"
          >
            <RefreshCw className={clsx('w-3.5 h-3.5', (isPracticesLoading || isFlatUsersLoading) && 'animate-spin')} /> Refresh
          </Button>
          <Button
            variant="primary"
            onClick={() => setIsCreateModalOpen(true)}
            className="text-xs gap-1.5 bg-purple-600 hover:bg-purple-700 text-white shadow-xs font-bold"
          >
            <UserPlus className="w-4 h-4" /> Create User
          </Button>
        </div>
      </div>

      {/* Notifications */}
      {successMessage && (
        <div className="p-3.5 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs font-semibold flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{successMessage}</span>
          </div>
          <button onClick={() => setSuccessMessage(null)} className="text-emerald-500 hover:text-emerald-800">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {errorMessage && (
        <div className="p-3.5 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-xs font-semibold flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button onClick={() => setErrorMessage(null)} className="text-rose-500 hover:text-rose-800">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 2. TREE VIEW (Practice -> Admin(s) -> Users)                              */}
      {/* ========================================================================= */}
      {viewMode === 'tree' && (
        <div className="space-y-4">
          {/* Practice Filter Bar */}
          <div className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card flex flex-col md:flex-row items-center justify-between gap-3">
            <div className="relative w-full md:w-96">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search practice, admin, user or email..."
                value={practiceSearchTerm}
                onChange={(e) => setPracticeSearchTerm(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 font-medium"
              />
            </div>

            <div className="flex items-center gap-3 w-full md:w-auto">
              <select
                value={practiceStatusFilter}
                onChange={(e) => setPracticeStatusFilter(e.target.value)}
                className="px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-700 focus:outline-none focus:ring-1 focus:ring-purple-500"
              >
                <option value="ALL">All Practice Statuses</option>
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
                <option value="SUSPENDED">SUSPENDED</option>
                <option value="DEACTIVATED">DEACTIVATED</option>
              </select>
              <div className="text-xs font-bold text-slate-500 whitespace-nowrap">
                Total: <span className="text-slate-900">{practiceTotalElements} Practices</span>
              </div>
            </div>
          </div>

          {/* Practice Hierarchy Cards */}
          {isPracticesLoading ? (
            <div className="bg-white border border-slate-200/90 rounded-2xl p-12 text-center text-slate-400">
              <div className="flex flex-col items-center justify-center gap-2">
                <RefreshCw className="w-6 h-6 animate-spin text-purple-600" />
                <span className="font-bold text-slate-700">Loading practices & hierarchies...</span>
              </div>
            </div>
          ) : practices.length === 0 ? (
            <div className="bg-white border border-slate-200/90 rounded-2xl p-12 text-center text-slate-400">
              <Building2 className="w-8 h-8 text-slate-300 mx-auto mb-2" />
              <p className="font-bold text-slate-700 text-sm">No practices found matching criteria</p>
              <p className="text-xs text-slate-400 mt-1">Try adjusting your search terms or status filter</p>
            </div>
          ) : (
            <div className="space-y-3">
              {practices.map((practice) => {
                const isExpanded = expandedPracticeIds.has(practice.organizationId);
                const userCache = practiceUsersMap[practice.organizationId];

                return (
                  <div
                    key={practice.organizationId}
                    className={clsx(
                      'bg-white border rounded-2xl transition-all shadow-card overflow-hidden',
                      isExpanded ? 'border-purple-300 ring-1 ring-purple-100' : 'border-slate-200/90 hover:border-slate-300'
                    )}
                  >
                    {/* Practice Header Card */}
                    <div
                      onClick={() => togglePracticeExpand(practice.organizationId)}
                      className="p-4 sm:p-5 flex flex-col md:flex-row md:items-center justify-between gap-3 cursor-pointer hover:bg-slate-50/60 transition-colors"
                    >
                      <div className="flex items-start gap-3 min-w-0">
                        <button
                          type="button"
                          className="mt-0.5 p-1 rounded-lg hover:bg-slate-200/70 text-slate-500 transition-transform"
                        >
                          {isExpanded ? (
                            <ChevronDown className="w-5 h-5 text-purple-600" />
                          ) : (
                            <ChevronRight className="w-5 h-5 text-slate-400" />
                          )}
                        </button>
                        <div className="min-w-0">
                          <div className="flex items-center gap-2 flex-wrap">
                            <h2 className="text-sm sm:text-base font-black text-slate-900 truncate">
                              {practice.organizationName}
                            </h2>
                            <span
                              className={clsx(
                                'px-2 py-0.5 rounded-full text-[10px] font-bold border',
                                practice.status === 'ACTIVE'
                                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                                  : practice.status === 'SUSPENDED'
                                  ? 'bg-rose-50 text-rose-700 border-rose-200'
                                  : 'bg-slate-100 text-slate-600 border-slate-200'
                              )}
                            >
                              {practice.status}
                            </span>
                            {practice.subscriptionPlan && (
                              <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-purple-50 text-purple-700 border border-purple-200">
                                {practice.subscriptionPlan}
                              </span>
                            )}
                          </div>
                          <div className="flex items-center gap-3 text-xs text-slate-500 mt-1 flex-wrap">
                            <span className="flex items-center gap-1">
                              <Mail className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                              {practice.email}
                            </span>
                            {practice.city && (
                              <span className="flex items-center gap-1 text-slate-400">
                                <MapPin className="w-3.5 h-3.5 shrink-0" />
                                {practice.city}, {practice.state || practice.country}
                              </span>
                            )}
                          </div>
                        </div>
                      </div>

                      {/* Summary Badges Bar */}
                      <div className="flex items-center gap-2 shrink-0 flex-wrap pl-8 md:pl-0">
                        <span className="px-2.5 py-1 rounded-lg text-xs font-bold bg-blue-50 text-blue-800 border border-blue-200 flex items-center gap-1.5">
                          <ShieldCheck className="w-3.5 h-3.5 text-blue-600" />
                          {practice.adminCount} {practice.adminCount === 1 ? 'Admin' : 'Admins'}
                        </span>
                        <span className="px-2.5 py-1 rounded-lg text-xs font-bold bg-slate-100 text-slate-700 border border-slate-200 flex items-center gap-1.5">
                          <Users className="w-3.5 h-3.5 text-slate-500" />
                          {practice.totalUserCount} Users
                        </span>
                        <span className="px-2.5 py-1 rounded-lg text-xs font-bold bg-emerald-50 text-emerald-800 border border-emerald-200 flex items-center gap-1.5">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                          {practice.activeUserCount} Active
                        </span>
                      </div>
                    </div>

                    {/* Expanded Content (Admins & Users) */}
                    {isExpanded && (
                      <div className="border-t border-slate-100 bg-slate-50/50 p-4 sm:p-6 space-y-6">
                        {/* 1. Practice Administrators */}
                        <div>
                          <div className="flex items-center justify-between mb-3">
                            <h3 className="text-xs font-black uppercase tracking-wider text-slate-500 flex items-center gap-2">
                              <ShieldCheck className="w-4 h-4 text-blue-600" />
                              Practice Administrators ({practice.admins?.length || 0})
                            </h3>
                          </div>

                          {practice.admins && practice.admins.length > 0 ? (
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                              {practice.admins.map((admin) => (
                                <div
                                  key={admin.id}
                                  className="bg-white p-3.5 rounded-xl border border-slate-200/90 shadow-2xs flex items-start justify-between gap-3"
                                >
                                  <div className="flex items-start gap-2.5 min-w-0">
                                    <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-800 font-black text-xs flex items-center justify-center shrink-0 border border-blue-200">
                                      {(admin.firstName || 'A').charAt(0).toUpperCase()}
                                    </div>
                                    <div className="min-w-0">
                                      <p className="font-bold text-slate-900 text-xs truncate">
                                        {admin.fullName || `${admin.firstName} ${admin.lastName || ''}`}
                                      </p>
                                      <p className="text-[11px] text-slate-500 truncate">{admin.email}</p>
                                      <div className="flex items-center gap-1.5 mt-1.5">
                                        <span className={clsx('px-2 py-0.5 rounded-full text-[9px] font-bold border', getRoleBadgeStyle(admin.roleCode))}>
                                          {admin.roleDisplayName || formatRoleDisplayName(admin.roleCode)}
                                        </span>
                                        <span className={clsx(
                                          'px-1.5 py-0.5 rounded-full text-[9px] font-bold border',
                                          admin.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-600 border-slate-200'
                                        )}>
                                          {admin.status}
                                        </span>
                                      </div>
                                    </div>
                                  </div>

                                  <div className="flex flex-col gap-1 shrink-0">
                                    <button
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        setSelectedUser(admin);
                                        setTargetRoleCode(admin.roleCode);
                                        setIsRoleModalOpen(true);
                                      }}
                                      className="p-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-[10px] font-bold inline-flex items-center justify-center transition-colors"
                                      title="Edit Role"
                                    >
                                      <Edit2 className="w-3 h-3" />
                                    </button>
                                    <button
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        setSelectedUser(admin);
                                        setTargetStatus(admin.status || 'ACTIVE');
                                        setIsStatusModalOpen(true);
                                      }}
                                      className="px-1.5 py-1 bg-slate-100 hover:bg-purple-100 text-slate-700 hover:text-purple-900 rounded-lg text-[10px] font-bold inline-flex items-center justify-center transition-colors"
                                      title="Change Status"
                                    >
                                      Status
                                    </button>
                                  </div>
                                </div>
                              ))}
                            </div>
                          ) : (
                            <div className="p-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-xl text-xs font-semibold flex items-center gap-2">
                              <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
                              <span>No practice administrator assigned to this organization.</span>
                            </div>
                          )}
                        </div>

                        {/* 2. Practice Users (Lazy Loaded) */}
                        <div className="space-y-3 pt-3 border-t border-slate-200/80">
                          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                            <h3 className="text-xs font-black uppercase tracking-wider text-slate-500 flex items-center gap-2">
                              <Users className="w-4 h-4 text-purple-600" />
                              Practice Users ({userCache ? userCache.totalElements : practice.totalUserCount})
                            </h3>

                            {/* In-practice quick search / role filter */}
                            <div className="flex items-center gap-2">
                              <div className="relative w-48 sm:w-60">
                                <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
                                <input
                                  type="text"
                                  placeholder="Search users..."
                                  value={userCache?.search || ''}
                                  onChange={(e) =>
                                    loadPracticeUsers(
                                      practice.organizationId,
                                      0,
                                      e.target.value,
                                      userCache?.role,
                                      userCache?.status
                                    )
                                  }
                                  className="w-full pl-8 pr-2.5 py-1 text-xs rounded-lg bg-white border border-slate-200 focus:outline-none focus:ring-1 focus:ring-purple-500"
                                />
                              </div>
                              <select
                                value={userCache?.status || 'ALL'}
                                onChange={(e) =>
                                  loadPracticeUsers(
                                    practice.organizationId,
                                    0,
                                    userCache?.search,
                                    userCache?.role,
                                    e.target.value
                                  )
                                }
                                className="px-2 py-1 text-xs bg-white border border-slate-200 rounded-lg font-bold text-slate-700 focus:outline-none focus:ring-1 focus:ring-purple-500"
                              >
                                <option value="ALL">All Statuses</option>
                                <option value="ACTIVE">ACTIVE</option>
                                <option value="INACTIVE">INACTIVE</option>
                                <option value="SUSPENDED">SUSPENDED</option>
                              </select>
                            </div>
                          </div>

                          {/* User List Table */}
                          <div className="bg-white border border-slate-200/90 rounded-xl overflow-hidden shadow-2xs">
                            {userCache?.isLoading ? (
                              <div className="text-center py-8 text-slate-400 text-xs font-bold flex items-center justify-center gap-2">
                                <RefreshCw className="w-4 h-4 animate-spin text-purple-600" />
                                Loading users for {practice.organizationName}...
                              </div>
                            ) : !userCache || userCache.users.length === 0 ? (
                              <div className="text-center py-8 text-slate-400 text-xs font-bold">
                                No users found for this practice matching search/filter.
                              </div>
                            ) : (
                              <div className="overflow-x-auto">
                                <table className="w-full text-left text-xs border-collapse">
                                  <thead>
                                    <tr className="bg-slate-50 border-b border-slate-200 font-bold text-slate-500 uppercase tracking-wider text-[10px]">
                                      <th className="px-4 py-2.5">User</th>
                                      <th className="px-4 py-2.5">Contact</th>
                                      <th className="px-4 py-2.5">Role</th>
                                      <th className="px-4 py-2.5">Status</th>
                                      <th className="px-4 py-2.5 text-right">Actions</th>
                                    </tr>
                                  </thead>
                                  <tbody className="divide-y divide-slate-100">
                                    {userCache.users.map((u) => {
                                      const roleObj = u.roles && u.roles.length > 0 ? u.roles[0] : null;
                                      const roleCode = typeof roleObj === 'string' ? roleObj : roleObj?.code || 'USER';

                                      return (
                                        <tr key={u.id} className="hover:bg-slate-50/70 transition-colors">
                                          <td className="px-4 py-2.5">
                                            <div className="flex items-center gap-2">
                                              <div className="w-6 h-6 rounded-full bg-purple-100 text-purple-800 font-black text-[10px] flex items-center justify-center shrink-0">
                                                {(u.firstName || 'U').charAt(0).toUpperCase()}
                                              </div>
                                              <div>
                                                <p className="font-bold text-slate-900">
                                                  {u.firstName} {u.lastName}
                                                </p>
                                              </div>
                                            </div>
                                          </td>

                                          <td className="px-4 py-2.5 text-slate-600 font-medium">
                                            <p>{u.email}</p>
                                            {u.phone && <p className="text-[10px] text-slate-400">{u.phone}</p>}
                                          </td>

                                          <td className="px-4 py-2.5">
                                            <div className="flex flex-wrap gap-1">
                                              {u.roles?.map((r, idx) => {
                                                const code = typeof r === 'string' ? r : r.code;
                                                return (
                                                  <span
                                                    key={code || idx}
                                                    className={clsx(
                                                      'px-2 py-0.5 rounded-full text-[9px] font-bold border inline-flex items-center',
                                                      getRoleBadgeStyle(code)
                                                    )}
                                                  >
                                                    {formatRoleDisplayName(code)}
                                                  </span>
                                                );
                                              }) || <span className="text-slate-400 text-[10px]">Standard User</span>}
                                            </div>
                                          </td>

                                          <td className="px-4 py-2.5 whitespace-nowrap">
                                            <span
                                              className={clsx(
                                                'px-2 py-0.5 rounded-full text-[9px] font-bold border inline-flex items-center gap-1',
                                                u.status === 'ACTIVE'
                                                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                                                  : u.status === 'SUSPENDED'
                                                  ? 'bg-rose-50 text-rose-700 border-rose-200'
                                                  : 'bg-slate-100 text-slate-600 border-slate-200'
                                              )}
                                            >
                                              {u.status || 'ACTIVE'}
                                            </span>
                                          </td>

                                          <td className="px-4 py-2.5 text-right whitespace-nowrap">
                                            <div className="flex items-center justify-end gap-1.5">
                                              <button
                                                onClick={() => {
                                                  setSelectedUser(u);
                                                  setTargetRoleCode(roleCode);
                                                  setIsRoleModalOpen(true);
                                                }}
                                                className="px-2 py-0.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded text-[10px] font-bold transition-colors"
                                              >
                                                Role
                                              </button>
                                              <button
                                                onClick={() => {
                                                  setSelectedUser(u);
                                                  setTargetStatus(u.status || 'ACTIVE');
                                                  setIsStatusModalOpen(true);
                                                }}
                                                className="px-2 py-0.5 bg-slate-100 hover:bg-purple-100 text-slate-700 hover:text-purple-900 rounded text-[10px] font-bold transition-colors"
                                              >
                                                Status
                                              </button>
                                            </div>
                                          </td>
                                        </tr>
                                      );
                                    })}
                                  </tbody>
                                </table>
                              </div>
                            )}

                            {/* Practice-scoped Users Pagination */}
                            {userCache && userCache.totalPages > 1 && (
                              <div className="px-4 py-2 bg-slate-50 border-t border-slate-100 flex items-center justify-between text-xs">
                                <span className="text-slate-500 font-medium">
                                  Page {userCache.page + 1} of {userCache.totalPages} ({userCache.totalElements} users)
                                </span>
                                <div className="flex items-center gap-1">
                                  <button
                                    onClick={() =>
                                      loadPracticeUsers(
                                        practice.organizationId,
                                        userCache.page - 1,
                                        userCache.search,
                                        userCache.role,
                                        userCache.status
                                      )
                                    }
                                    disabled={userCache.page === 0}
                                    className="px-2.5 py-1 rounded bg-white border border-slate-200 font-bold text-slate-700 disabled:opacity-40"
                                  >
                                    Prev
                                  </button>
                                  <button
                                    onClick={() =>
                                      loadPracticeUsers(
                                        practice.organizationId,
                                        userCache.page + 1,
                                        userCache.search,
                                        userCache.role,
                                        userCache.status
                                      )
                                    }
                                    disabled={userCache.page >= userCache.totalPages - 1}
                                    className="px-2.5 py-1 rounded bg-white border border-slate-200 font-bold text-slate-700 disabled:opacity-40"
                                  >
                                    Next
                                  </button>
                                </div>
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}

          {/* Practice Pagination Bar */}
          {practiceTotalPages > 1 && (
            <div className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card flex items-center justify-between text-xs">
              <span className="font-bold text-slate-600">
                Showing page {practicePage + 1} of {practiceTotalPages} ({practiceTotalElements} total practices)
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="secondary"
                  disabled={practicePage === 0 || isPracticesLoading}
                  onClick={() => loadPractices(practicePage - 1)}
                  className="text-xs font-bold"
                >
                  Previous
                </Button>
                <Button
                  variant="secondary"
                  disabled={practicePage >= practiceTotalPages - 1 || isPracticesLoading}
                  onClick={() => loadPractices(practicePage + 1)}
                  className="text-xs font-bold"
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* 3. LIST VIEW (Flat Paginated Users)                                       */}
      {/* ========================================================================= */}
      {viewMode === 'list' && (
        <div className="space-y-4">
          {/* Filters & Role Tabs */}
          <div className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card space-y-3">
            <div className="flex flex-col md:flex-row items-center justify-between gap-3">
              <div className="relative w-full md:w-96">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Search by name, email, phone..."
                  value={flatSearchTerm}
                  onChange={(e) => setFlatSearchTerm(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 font-medium"
                />
              </div>

              <div className="flex items-center gap-2 w-full md:w-auto">
                <select
                  value={flatStatusFilter}
                  onChange={(e) => setFlatStatusFilter(e.target.value)}
                  className="px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-700 focus:outline-none focus:ring-1 focus:ring-purple-500"
                >
                  <option value="ALL">All Statuses</option>
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="SUSPENDED">SUSPENDED</option>
                  <option value="INACTIVE">INACTIVE</option>
                </select>
              </div>
            </div>

            {/* Role Tabs */}
            <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-1 border-t border-slate-100 pt-3">
              {[
                { label: 'All Users', value: 'ALL' },
                { label: 'SuperAdmin', value: 'SUPERADMIN' },
                { label: 'Operations', value: 'OPERATIONS' },
                { label: 'Support', value: 'SUPPORT' },
                { label: 'Marketplace', value: 'MARKETPLACE' },
                { label: 'Finance', value: 'FINANCE' },
                { label: 'Content', value: 'CONTENT' },
                { label: 'Security', value: 'SECURITY' },
                { label: 'Engineering', value: 'ENGINEERING' },
                { label: 'Practitioners', value: 'PRACTITIONERS' },
                { label: 'Practice Staff', value: 'STAFF' },
                { label: 'Customers', value: 'CUSTOMERS' },
              ].map((tab) => (
                <button
                  key={tab.value}
                  onClick={() => setFlatRoleFilter(tab.value)}
                  className={clsx(
                    'px-3 py-1.5 rounded-lg text-xs font-bold whitespace-nowrap transition-all shrink-0',
                    flatRoleFilter === tab.value
                      ? 'bg-purple-600 text-white shadow-xs'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  )}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>

          {/* Flat Table */}
          <div className="bg-white border border-slate-200/90 rounded-2xl shadow-card overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-slate-50/80 border-b border-slate-200 font-bold text-slate-500 uppercase tracking-wider">
                    <th className="px-5 py-3.5">User Identity</th>
                    <th className="px-4 py-3.5">Email & Contact</th>
                    <th className="px-4 py-3.5">Assigned Platform Role</th>
                    <th className="px-4 py-3.5">Created</th>
                    <th className="px-4 py-3.5">Status</th>
                    <th className="px-5 py-3.5 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {isFlatUsersLoading ? (
                    <tr>
                      <td colSpan={6} className="text-center py-12 text-slate-400">
                        <div className="flex flex-col items-center justify-center gap-2">
                          <RefreshCw className="w-5 h-5 animate-spin text-purple-600" />
                          <span className="font-bold text-slate-600">Loading platform users...</span>
                        </div>
                      </td>
                    </tr>
                  ) : flatUsers.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="text-center py-12 text-slate-400">
                        <div className="flex flex-col items-center justify-center gap-2">
                          <Users className="w-6 h-6 text-slate-300" />
                          <span className="font-bold text-slate-700">No users found matching filter criteria</span>
                        </div>
                      </td>
                    </tr>
                  ) : (
                    flatUsers.map((u) => {
                      const roleObj = u.roles && u.roles.length > 0 ? u.roles[0] : null;
                      const roleCode = typeof roleObj === 'string' ? roleObj : roleObj?.code || 'USER';

                      return (
                        <tr key={u.id} className="hover:bg-slate-50/70 transition-colors">
                          <td className="px-5 py-3.5">
                            <div className="flex items-center gap-2.5">
                              <div className="w-7 h-7 rounded-full bg-purple-100 text-purple-800 font-black text-xs flex items-center justify-center shrink-0 border border-purple-200">
                                {(u.firstName || 'U').charAt(0).toUpperCase()}
                              </div>
                              <div>
                                <p className="font-bold text-slate-900 text-sm">
                                  {u.firstName} {u.lastName}
                                </p>
                                <span className="text-[10px] text-slate-400 font-mono">ID: {u.id.substring(0, 8)}...</span>
                              </div>
                            </div>
                          </td>

                          <td className="px-4 py-3.5 text-slate-600">
                            <div className="flex items-center gap-1.5 font-semibold text-slate-800">
                              <Mail className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                              <span>{u.email}</span>
                            </div>
                            {u.phone && (
                              <div className="flex items-center gap-1.5 text-slate-400 mt-0.5 text-[11px]">
                                <Phone className="w-3 h-3 text-slate-400 shrink-0" />
                                <span>{u.phone}</span>
                              </div>
                            )}
                          </td>

                          <td className="px-4 py-3.5">
                            <div className="flex flex-wrap gap-1">
                              {u.roles?.map((r, idx) => {
                                const code = typeof r === 'string' ? r : r.code;
                                return (
                                  <span
                                    key={code || idx}
                                    className={clsx(
                                      'px-2.5 py-0.5 rounded-full text-[10px] font-bold border inline-flex items-center gap-1',
                                      getRoleBadgeStyle(code)
                                    )}
                                  >
                                    {formatRoleDisplayName(code)}
                                  </span>
                                );
                              }) || <span className="text-slate-400">Standard User</span>}
                            </div>
                          </td>

                          <td className="px-4 py-3.5 text-slate-500 font-medium whitespace-nowrap">
                            {u.createdAt ? new Date(u.createdAt).toLocaleDateString('en-IN', { dateStyle: 'medium' }) : 'N/A'}
                          </td>

                          <td className="px-4 py-3.5 whitespace-nowrap">
                            <span
                              className={clsx(
                                'px-2.5 py-0.5 rounded-full text-[10px] font-bold border inline-flex items-center gap-1',
                                u.status === 'ACTIVE'
                                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                                  : u.status === 'SUSPENDED'
                                  ? 'bg-rose-50 text-rose-700 border-rose-200'
                                  : 'bg-slate-100 text-slate-600 border-slate-200'
                              )}
                            >
                              <span
                                className={clsx(
                                  'w-1.5 h-1.5 rounded-full',
                                  u.status === 'ACTIVE' ? 'bg-emerald-500' : u.status === 'SUSPENDED' ? 'bg-rose-500' : 'bg-slate-400'
                                )}
                              />
                              {u.status || 'ACTIVE'}
                            </span>
                          </td>

                          <td className="px-5 py-3.5 text-right whitespace-nowrap">
                            <div className="flex items-center justify-end gap-1.5">
                              <button
                                onClick={() => {
                                  setSelectedUser(u);
                                  setTargetRoleCode(roleCode);
                                  setIsRoleModalOpen(true);
                                }}
                                className="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-200 rounded-lg text-xs font-bold inline-flex items-center gap-1 transition-colors shadow-2xs"
                              >
                                <Edit2 className="w-3 h-3" /> Role
                              </button>
                              <button
                                onClick={() => {
                                  setSelectedUser(u);
                                  setTargetStatus(u.status || 'ACTIVE');
                                  setIsStatusModalOpen(true);
                                }}
                                className="px-2.5 py-1 bg-slate-100 hover:bg-purple-100 text-slate-700 hover:text-purple-900 border border-slate-200 rounded-lg text-xs font-bold inline-flex items-center gap-1 transition-colors shadow-2xs"
                              >
                                Status
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>

            {/* Flat Pagination Bar */}
            {flatTotalPages > 1 && (
              <div className="px-5 py-3 bg-slate-50 border-t border-slate-100 flex items-center justify-between text-xs">
                <span className="font-bold text-slate-600">
                  Page {flatPage + 1} of {flatTotalPages} ({flatTotalElements} users)
                </span>
                <div className="flex items-center gap-2">
                  <Button
                    variant="secondary"
                    disabled={flatPage === 0 || isFlatUsersLoading}
                    onClick={() => loadFlatUsers(flatPage - 1)}
                    className="text-xs font-bold"
                  >
                    Previous
                  </Button>
                  <Button
                    variant="secondary"
                    disabled={flatPage >= flatTotalPages - 1 || isFlatUsersLoading}
                    onClick={() => loadFlatUsers(flatPage + 1)}
                    className="text-xs font-bold"
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 4. Role Update Modal                                                      */}
      {/* ========================================================================= */}
      <Modal
        isOpen={isRoleModalOpen}
        onClose={() => setIsRoleModalOpen(false)}
        title="Reassign Platform Role"
        maxWidth="md"
      >
        <div className="space-y-4">
          <div className="bg-purple-50/70 border border-purple-200/80 rounded-xl p-3.5 text-xs text-purple-900 space-y-1">
            <p className="font-bold flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-purple-700" />
              Role Mutation Governance
            </p>
            <p className="text-purple-800">
              Updating role for <span className="font-bold">{selectedUser?.email}</span>. Only roles authorized for your privilege tier can be assigned.
            </p>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">Select System / Platform Role</label>
            <select
              value={targetRoleCode}
              onChange={(e) => setTargetRoleCode(e.target.value)}
              className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
            >
              {isSuperAdmin && <option value="TAXORYN_SUPERADMIN">Taxoryn SuperAdmin (Full Authority)</option>}
              <option value="TAXORYN_OPERATIONS_ADMIN">Taxoryn Operations Admin</option>
              <option value="TAXORYN_SUPPORT_ADMIN">Taxoryn Support Admin</option>
              <option value="TAXORYN_MARKETPLACE_ADMIN">Taxoryn Marketplace Admin</option>
              <option value="TAXORYN_FINANCE_ADMIN">Taxoryn Finance Admin</option>
              <option value="TAXORYN_CONTENT_ADMIN">Taxoryn Content Admin</option>
              <option value="TAXORYN_SECURITY_ADMIN">Taxoryn Security Admin</option>
              <option value="TAXORYN_ENGINEERING_ADMIN">Taxoryn Engineering Admin</option>
            </select>
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
            <Button variant="secondary" onClick={() => setIsRoleModalOpen(false)} disabled={isUpdatingRole}>
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleUpdateRole}
              disabled={isUpdatingRole}
              className="bg-purple-600 hover:bg-purple-700 text-white font-bold"
            >
              {isUpdatingRole ? 'Updating...' : 'Confirm Role'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* ========================================================================= */}
      {/* 5. Status Update Modal                                                    */}
      {/* ========================================================================= */}
      <Modal
        isOpen={isStatusModalOpen}
        onClose={() => setIsStatusModalOpen(false)}
        title="Update User Operational Status"
        maxWidth="md"
      >
        <div className="space-y-4">
          <p className="text-xs text-slate-600">
            Select the new lifecycle status for user <span className="font-bold text-slate-900">{selectedUser?.email}</span>:
          </p>

          <div className="grid grid-cols-3 gap-2">
            {['ACTIVE', 'SUSPENDED', 'INACTIVE'].map((st) => (
              <button
                key={st}
                type="button"
                onClick={() => setTargetStatus(st)}
                className={clsx(
                  'py-2 rounded-xl text-xs font-bold border transition-all text-center',
                  targetStatus === st
                    ? st === 'ACTIVE'
                      ? 'bg-emerald-600 text-white border-emerald-600 shadow-xs'
                      : st === 'SUSPENDED'
                      ? 'bg-rose-600 text-white border-rose-600 shadow-xs'
                      : 'bg-slate-800 text-white border-slate-800 shadow-xs'
                    : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                )}
              >
                {st}
              </button>
            ))}
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
            <Button variant="secondary" onClick={() => setIsStatusModalOpen(false)} disabled={isUpdatingStatus}>
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleUpdateStatus}
              disabled={isUpdatingStatus}
              className="bg-purple-600 hover:bg-purple-700 text-white font-bold"
            >
              {isUpdatingStatus ? 'Saving...' : 'Update Status'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* ========================================================================= */}
      {/* 6. Create User Modal                                                      */}
      {/* ========================================================================= */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Provision Platform User"
        maxWidth="lg"
      >
        <form onSubmit={handleCreateUser} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">First Name *</label>
              <input
                type="text"
                required
                value={newUserData.firstName}
                onChange={(e) => setNewUserData({ ...newUserData, firstName: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
                placeholder="e.g. Rahul"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Last Name *</label>
              <input
                type="text"
                required
                value={newUserData.lastName}
                onChange={(e) => setNewUserData({ ...newUserData, lastName: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
                placeholder="e.g. Sharma"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">Work Email *</label>
            <input
              type="email"
              required
              value={newUserData.email}
              onChange={(e) => setNewUserData({ ...newUserData, email: e.target.value })}
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
              placeholder="e.g. rahul@taxoryn.com"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">Phone Number</label>
            <input
              type="tel"
              value={newUserData.phone}
              onChange={(e) => setNewUserData({ ...newUserData, phone: e.target.value })}
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
              placeholder="e.g. +91 98765 43210"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">Initial Platform Role *</label>
            <select
              value={newUserData.roleCode}
              onChange={(e) => setNewUserData({ ...newUserData, roleCode: e.target.value })}
              className="w-full px-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
            >
              {isSuperAdmin && <option value="TAXORYN_SUPERADMIN">Taxoryn SuperAdmin (Full Authority)</option>}
              <option value="TAXORYN_OPERATIONS_ADMIN">Taxoryn Operations Admin</option>
              <option value="TAXORYN_SUPPORT_ADMIN">Taxoryn Support Admin</option>
              <option value="TAXORYN_MARKETPLACE_ADMIN">Taxoryn Marketplace Admin</option>
              <option value="TAXORYN_FINANCE_ADMIN">Taxoryn Finance Admin</option>
              <option value="TAXORYN_CONTENT_ADMIN">Taxoryn Content Admin</option>
              <option value="TAXORYN_SECURITY_ADMIN">Taxoryn Security Admin</option>
              <option value="TAXORYN_ENGINEERING_ADMIN">Taxoryn Engineering Admin</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">Temporary Password (Optional)</label>
            <input
              type="password"
              value={newUserData.temporaryPassword}
              onChange={(e) => setNewUserData({ ...newUserData, temporaryPassword: e.target.value })}
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
              placeholder="Leave empty for auto-generated secure password"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
            <Button variant="secondary" type="button" onClick={() => setIsCreateModalOpen(false)} disabled={isCreating}>
              Cancel
            </Button>
            <Button
              variant="primary"
              type="submit"
              disabled={isCreating}
              className="bg-purple-600 hover:bg-purple-700 text-white font-bold"
            >
              {isCreating ? 'Provisioning...' : 'Provision User'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
