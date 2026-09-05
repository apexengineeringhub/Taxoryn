import React, { useEffect, useState, useCallback } from 'react';
import {
  Users,
  Search,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  RefreshCw,
  ShieldCheck,
  ShieldAlert,
  Mail,
  Phone,
  Calendar,
  UserCheck,
  UserPlus,
  Edit2,
  X,
  Lock,
} from 'lucide-react';
import { adminUserApi } from '../api/endpoints';
import { User } from '../types';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { useAuth } from '../context/AuthContext';
import clsx from 'clsx';

export const PlatformUsersPage: React.FC = () => {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Status Modal State
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
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

  const loadUsers = useCallback(async () => {
    try {
      setIsLoading(true);
      setErrorMessage(null);
      const res = await adminUserApi.getUsers({
        role: roleFilter !== 'ALL' ? roleFilter : undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        search: searchTerm.trim() || undefined,
        size: 100,
      });
      setUsers(res?.content || []);
    } catch (err: any) {
      console.error('Failed to load users', err);
      setErrorMessage(err?.response?.data?.message || 'Failed to load platform users.');
    } finally {
      setIsLoading(false);
    }
  }, [roleFilter, statusFilter, searchTerm]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

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
        temporaryPassword: 'Password123!',
      });
      await loadUsers();
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
      setSuccessMessage(`User status updated to ${targetStatus}`);
      setIsStatusModalOpen(false);
      setSelectedUser(null);
      await loadUsers();
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
      setSuccessMessage(`User role updated to ${formatRoleDisplayName(targetRoleCode)}`);
      setIsRoleModalOpen(false);
      setSelectedUser(null);
      await loadUsers();
    } catch (err: any) {
      console.error('Failed to update user role', err);
      setErrorMessage(err?.response?.data?.message || 'Failed to update user role. Privilege escalation denied.');
    } finally {
      setIsUpdatingRole(false);
    }
  };

  const formatRoleDisplayName = (code?: string) => {
    if (!code) return 'Standard User';
    return switchRoleName(code);
  };

  const switchRoleName = (code: string) => {
    switch (code) {
      case 'TAXORYN_SUPERADMIN':
      case 'SUPER_ADMIN':
        return 'Taxoryn SuperAdmin';
      case 'TAXORYN_OPERATIONS_ADMIN':
        return 'Taxoryn Operations Admin';
      case 'TAXORYN_SUPPORT_ADMIN':
        return 'Taxoryn Support Admin';
      case 'TAXORYN_MARKETPLACE_ADMIN':
        return 'Taxoryn Marketplace Admin';
      case 'TAXORYN_FINANCE_ADMIN':
        return 'Taxoryn Finance Admin';
      case 'TAXORYN_CONTENT_ADMIN':
        return 'Taxoryn Content Admin';
      case 'TAXORYN_SECURITY_ADMIN':
        return 'Taxoryn Security Admin';
      case 'TAXORYN_ENGINEERING_ADMIN':
        return 'Taxoryn Engineering Admin';
      case 'ORG_ADMIN':
      case 'PRACTICE_ADMIN':
        return 'Practice Admin';
      case 'PRACTICE_OWNER':
        return 'Practice Owner';
      case 'PRACTITIONER':
        return 'Tax Practitioner';
      case 'STAFF':
      case 'PRACTICE_EMPLOYEE':
        return 'Practice Employee';
      case 'ARTICLE_ASSISTANT':
        return 'Article Assistant';
      case 'CLIENT_USER':
      case 'PRACTICE_CLIENT':
        return 'Practice Client';
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
            Taxoryn Platform Users
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Provision, govern, and audit internal Taxoryn platform administrators and enterprise users with backend least-privilege RBAC.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant="secondary"
            onClick={loadUsers}
            disabled={isLoading}
            className="text-xs gap-1.5 shadow-2xs font-bold"
          >
            <RefreshCw className={clsx('w-3.5 h-3.5', isLoading && 'animate-spin')} /> Refresh
          </Button>
          <Button
            variant="primary"
            onClick={() => setIsCreateModalOpen(true)}
            className="text-xs gap-1.5 bg-purple-600 hover:bg-purple-700 text-white shadow-xs font-bold"
          >
            <UserPlus className="w-4 h-4" /> Create Taxoryn User
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
      {/* 2. Filters & Role Tabs                                                    */}
      {/* ========================================================================= */}
      <div className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card space-y-3">
        <div className="flex flex-col md:flex-row items-center justify-between gap-3">
          <div className="relative w-full md:w-96">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by name, email, phone..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 font-medium"
            />
          </div>

          <div className="flex items-center gap-2 w-full md:w-auto">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
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
              onClick={() => setRoleFilter(tab.value)}
              className={clsx(
                'px-3 py-1.5 rounded-lg text-xs font-bold whitespace-nowrap transition-all shrink-0',
                roleFilter === tab.value
                  ? 'bg-purple-600 text-white shadow-xs'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 3. Users Table & Mobile Cards                                             */}
      {/* ========================================================================= */}
      <div className="bg-white border border-slate-200/90 rounded-2xl shadow-card overflow-hidden">
        {/* Desktop Table View (hidden md:block) */}
        <div className="hidden md:block overflow-x-auto">
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
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-slate-400">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <RefreshCw className="w-5 h-5 animate-spin text-purple-600" />
                      <span className="font-bold text-slate-600">Loading platform users...</span>
                    </div>
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-slate-400">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <Users className="w-6 h-6 text-slate-300" />
                      <span className="font-bold text-slate-700">No users found matching filter criteria</span>
                    </div>
                  </td>
                </tr>
              ) : (
                users.map((u) => {
                  const roleObj = u.roles && u.roles.length > 0 ? u.roles[0] : null;
                  const roleCode = typeof roleObj === 'string' ? roleObj : roleObj?.code || 'USER';

                  return (
                    <tr key={u.id} className="hover:bg-slate-50/70 transition-colors">
                      {/* Name */}
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-2.5">
                          <div className="w-7 h-7 rounded-full bg-purple-100 text-purple-800 font-black text-xs flex items-center justify-center shrink-0 border border-purple-200">
                            {(u.firstName || 'U').charAt(0).toUpperCase()}
                          </div>
                          <div>
                            <p className="font-bold text-slate-900 text-sm">
                              {u.firstName} {u.lastName}
                            </p>
                            <span className="text-[10px] text-slate-400 font-mono">
                              ID: {u.id.substring(0, 8)}...
                            </span>
                          </div>
                        </div>
                      </td>

                      {/* Contact */}
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

                      {/* Role */}
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

                      {/* Created Date */}
                      <td className="px-4 py-3.5 text-slate-500 font-medium whitespace-nowrap">
                        {u.createdAt ? new Date(u.createdAt).toLocaleDateString('en-IN', { dateStyle: 'medium' }) : 'N/A'}
                      </td>

                      {/* Status */}
                      <td className="px-4 py-3.5 whitespace-nowrap">
                        <span className={clsx(
                          'px-2.5 py-0.5 rounded-full text-[10px] font-bold border inline-flex items-center gap-1',
                          u.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
                          u.status === 'SUSPENDED' ? 'bg-rose-50 text-rose-700 border-rose-200' :
                          'bg-slate-100 text-slate-600 border-slate-200'
                        )}>
                          <span className={clsx(
                            'w-1.5 h-1.5 rounded-full',
                            u.status === 'ACTIVE' ? 'bg-emerald-500' :
                            u.status === 'SUSPENDED' ? 'bg-rose-500' : 'bg-slate-400'
                          )} />
                          {u.status || 'ACTIVE'}
                        </span>
                      </td>

                      {/* Actions */}
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

        {/* Mobile Cards View (md:hidden) */}
        <div className="md:hidden divide-y divide-slate-100">
          {isLoading ? (
            <div className="text-center py-12 text-slate-400 text-xs font-bold">
              Loading platform users...
            </div>
          ) : users.length === 0 ? (
            <div className="text-center py-12 text-slate-400 text-xs font-bold">
              No users found matching filter criteria
            </div>
          ) : (
            users.map((u) => {
              const roleObj = u.roles && u.roles.length > 0 ? u.roles[0] : null;
              const roleCode = typeof roleObj === 'string' ? roleObj : roleObj?.code || 'USER';

              return (
                <div key={u.id} className="p-4 space-y-2.5">
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2.5 min-w-0">
                      <div className="w-8 h-8 rounded-full bg-purple-100 text-purple-800 font-black text-xs flex items-center justify-center shrink-0 border border-purple-200">
                        {(u.firstName || 'U').charAt(0).toUpperCase()}
                      </div>
                      <div className="min-w-0">
                        <p className="font-bold text-slate-900 text-sm truncate">
                          {u.firstName} {u.lastName}
                        </p>
                        <span className="text-[10px] text-slate-400 font-mono">
                          ID: {u.id.substring(0, 8)}...
                        </span>
                      </div>
                    </div>
                    <span className={clsx(
                      'px-2 py-0.5 rounded-full text-[10px] font-bold border inline-flex items-center gap-1 shrink-0',
                      u.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
                      u.status === 'SUSPENDED' ? 'bg-rose-50 text-rose-700 border-rose-200' :
                      'bg-slate-100 text-slate-600 border-slate-200'
                    )}>
                      {u.status || 'ACTIVE'}
                    </span>
                  </div>

                  <div className="text-xs space-y-1 pt-1 border-t border-slate-50">
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400">Email:</span>
                      <span className="text-slate-700 font-medium truncate max-w-[180px]">{u.email}</span>
                    </div>
                    {u.phone && (
                      <div className="flex items-center justify-between">
                        <span className="text-slate-400">Phone:</span>
                        <span className="text-slate-700 font-medium">{u.phone}</span>
                      </div>
                    )}
                    <div className="flex items-center justify-between gap-2 pt-0.5">
                      <span className="text-slate-400 shrink-0">Role:</span>
                      <div className="flex flex-wrap gap-1 justify-end">
                        {u.roles?.map((r, idx) => {
                          const code = typeof r === 'string' ? r : r.code;
                          return (
                            <span
                              key={code || idx}
                              className={clsx(
                                'px-2 py-0.5 rounded-full text-[10px] font-bold border',
                                getRoleBadgeStyle(code)
                              )}
                            >
                              {formatRoleDisplayName(code)}
                            </span>
                          );
                        }) || <span className="text-slate-400">Standard User</span>}
                      </div>
                    </div>
                  </div>

                  <div className="pt-2 flex items-center justify-end gap-2 border-t border-slate-50">
                    <button
                      onClick={() => {
                        setSelectedUser(u);
                        setTargetRoleCode(roleCode);
                        setIsRoleModalOpen(true);
                      }}
                      className="flex-1 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-200 rounded-lg text-xs font-bold inline-flex items-center justify-center gap-1 transition-colors"
                    >
                      <Edit2 className="w-3 h-3" /> Role
                    </button>
                    <button
                      onClick={() => {
                        setSelectedUser(u);
                        setTargetStatus(u.status || 'ACTIVE');
                        setIsStatusModalOpen(true);
                      }}
                      className="flex-1 py-1.5 bg-slate-100 hover:bg-purple-100 text-slate-700 hover:text-purple-900 border border-slate-200 rounded-lg text-xs font-bold inline-flex items-center justify-center gap-1 transition-colors"
                    >
                      Status
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 4. Create Platform User Modal                                            */}
      {/* ========================================================================= */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Provision Taxoryn Platform User"
        subtitle="Create an internal platform administrator with controlled least-privilege role assignment."
        maxWidth="lg"
      >
        <form onSubmit={handleCreateUser} className="space-y-4 text-xs">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block font-bold text-slate-700 mb-1">First Name *</label>
              <input
                type="text"
                required
                value={newUserData.firstName}
                onChange={(e) => setNewUserData({ ...newUserData, firstName: e.target.value })}
                placeholder="e.g. Anjani"
                className="w-full p-2 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 font-medium"
              />
            </div>
            <div>
              <label className="block font-bold text-slate-700 mb-1">Last Name *</label>
              <input
                type="text"
                required
                value={newUserData.lastName}
                onChange={(e) => setNewUserData({ ...newUserData, lastName: e.target.value })}
                placeholder="e.g. Pathak"
                className="w-full p-2 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 font-medium"
              />
            </div>
          </div>

          <div>
            <label className="block font-bold text-slate-700 mb-1">Corporate Email Address *</label>
            <input
              type="email"
              required
              value={newUserData.email}
              onChange={(e) => setNewUserData({ ...newUserData, email: e.target.value })}
              placeholder="e.g. anjani.pathak@taxoryn.com"
              className="w-full p-2 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 font-medium"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block font-bold text-slate-700 mb-1">Phone Number</label>
              <input
                type="text"
                value={newUserData.phone}
                onChange={(e) => setNewUserData({ ...newUserData, phone: e.target.value })}
                placeholder="+91 98765 43210"
                className="w-full p-2 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500 font-medium"
              />
            </div>
            <div>
              <label className="block font-bold text-slate-700 mb-1">Account Status</label>
              <select
                value={newUserData.status}
                onChange={(e) => setNewUserData({ ...newUserData, status: e.target.value })}
                className="w-full p-2 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:bg-white font-bold text-slate-700"
              >
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block font-bold text-slate-700 mb-1">Assigned Platform Role *</label>
            <select
              value={newUserData.roleCode}
              onChange={(e) => setNewUserData({ ...newUserData, roleCode: e.target.value })}
              className="w-full p-2.5 text-xs rounded-xl bg-purple-50/60 border border-purple-200 font-bold text-purple-900 focus:outline-none focus:ring-2 focus:ring-purple-500"
            >
              <option value="TAXORYN_OPERATIONS_ADMIN">Taxoryn Operations Admin (Day-to-day platform & onboarding)</option>
              <option value="TAXORYN_SUPPORT_ADMIN">Taxoryn Support Admin (Support & feedback triage)</option>
              <option value="TAXORYN_MARKETPLACE_ADMIN">Taxoryn Marketplace Admin (Lead matching & disputes)</option>
              <option value="TAXORYN_FINANCE_ADMIN">Taxoryn Finance Admin (SaaS subscriptions, MRR & commercial revenue)</option>
              <option value="TAXORYN_CONTENT_ADMIN">Taxoryn Content Admin (Knowledge base & articles)</option>
              {isSuperAdmin && (
                <>
                  <option value="TAXORYN_SECURITY_ADMIN">Taxoryn Security Admin (Audit inspection & security alerts)</option>
                  <option value="TAXORYN_ENGINEERING_ADMIN">Taxoryn Engineering Admin (Platform health & technical incidents)</option>
                  <option value="TAXORYN_SUPERADMIN">Taxoryn SuperAdmin (Full governance authority)</option>
                </>
              )}
            </select>
            <p className="text-[10px] text-slate-400 mt-1">
              Roles are constrained to system-defined scopes with strict backend privilege validation.
            </p>
          </div>

          <div>
            <label className="block font-bold text-slate-700 mb-1">Temporary Password</label>
            <input
              type="text"
              value={newUserData.temporaryPassword}
              onChange={(e) => setNewUserData({ ...newUserData, temporaryPassword: e.target.value })}
              className="w-full p-2 text-xs rounded-xl bg-slate-50 border border-slate-200 font-mono"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-4 border-t border-slate-100">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => setIsCreateModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              disabled={isCreating}
              className="bg-purple-600 hover:bg-purple-700 text-white font-bold"
            >
              {isCreating ? 'Provisioning...' : 'Provision User'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* ========================================================================= */}
      {/* 5. Role Assignment Modal                                                 */}
      {/* ========================================================================= */}
      <Modal
        isOpen={isRoleModalOpen && !!selectedUser}
        onClose={() => {
          setIsRoleModalOpen(false);
          setSelectedUser(null);
        }}
        title="Reassign Platform Role"
        subtitle={`User: ${selectedUser?.firstName} ${selectedUser?.lastName} (${selectedUser?.email})`}
        maxWidth="md"
      >
        <div className="space-y-4 text-xs">
          <div>
            <label className="block font-bold text-slate-700 mb-1">Select New Role</label>
            <select
              value={targetRoleCode}
              onChange={(e) => setTargetRoleCode(e.target.value)}
              className="w-full p-2.5 text-xs rounded-xl bg-purple-50/60 border border-purple-200 font-bold text-purple-900 focus:outline-none focus:ring-2 focus:ring-purple-500"
            >
              <option value="TAXORYN_OPERATIONS_ADMIN">Taxoryn Operations Admin</option>
              <option value="TAXORYN_SUPPORT_ADMIN">Taxoryn Support Admin</option>
              <option value="TAXORYN_MARKETPLACE_ADMIN">Taxoryn Marketplace Admin</option>
              <option value="TAXORYN_FINANCE_ADMIN">Taxoryn Finance Admin</option>
              <option value="TAXORYN_CONTENT_ADMIN">Taxoryn Content Admin</option>
              {isSuperAdmin && (
                <>
                  <option value="TAXORYN_SECURITY_ADMIN">Taxoryn Security Admin</option>
                  <option value="TAXORYN_ENGINEERING_ADMIN">Taxoryn Engineering Admin</option>
                  <option value="TAXORYN_SUPERADMIN">Taxoryn SuperAdmin</option>
                </>
              )}
            </select>
          </div>

          <div className="flex items-center justify-end gap-2 pt-4 border-t border-slate-100">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => {
                setIsRoleModalOpen(false);
                setSelectedUser(null);
              }}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
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
      {/* 6. Status Update Modal                                                   */}
      {/* ========================================================================= */}
      <Modal
        isOpen={isStatusModalOpen && !!selectedUser}
        onClose={() => {
          setIsStatusModalOpen(false);
          setSelectedUser(null);
        }}
        title="Update User Account Status"
        subtitle={`User: ${selectedUser?.firstName} ${selectedUser?.lastName} (${selectedUser?.email})`}
        maxWidth="md"
      >
        <div className="space-y-4 text-xs">
          <div>
            <label className="block font-bold text-slate-700 mb-1">Account Status</label>
            <select
              value={targetStatus}
              onChange={(e) => setTargetStatus(e.target.value)}
              className="w-full p-2.5 text-xs rounded-xl bg-slate-50 border border-slate-200 font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-purple-500"
            >
              <option value="ACTIVE">ACTIVE (Full access)</option>
              <option value="SUSPENDED">SUSPENDED (Access blocked)</option>
              <option value="INACTIVE">INACTIVE (Deactivated)</option>
            </select>
          </div>

          <div className="flex items-center justify-end gap-2 pt-4 border-t border-slate-100">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => {
                setIsStatusModalOpen(false);
                setSelectedUser(null);
              }}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleUpdateStatus}
              disabled={isUpdatingStatus}
              className="bg-purple-600 hover:bg-purple-700 text-white font-bold"
            >
              {isUpdatingStatus ? 'Updating...' : 'Confirm Status'}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
