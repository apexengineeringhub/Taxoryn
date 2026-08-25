import React, { useEffect, useState } from 'react';
import {
  Users,
  Search,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  RefreshCw,
  ShieldCheck,
  Mail,
  Phone,
  Calendar,
  UserCheck,
  ShieldAlert,
} from 'lucide-react';
import { adminUserApi } from '../api/endpoints';
import { User } from '../types';
import { Button } from '../components/common/Button';
import clsx from 'clsx';

export const PlatformUsersPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [targetStatus, setTargetStatus] = useState<string>('ACTIVE');
  const [isUpdating, setIsUpdating] = useState(false);

  useEffect(() => {
    loadUsers();
  }, [roleFilter, statusFilter]);

  const loadUsers = async () => {
    try {
      setIsLoading(true);
      const res = await adminUserApi.getUsers({
        role: roleFilter !== 'ALL' ? roleFilter : undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        search: searchTerm || undefined,
        size: 100,
      });
      setUsers(res?.content || []);
    } catch (err) {
      console.error('Failed to load users', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpdateStatus = async () => {
    if (!selectedUser) return;
    try {
      setIsUpdating(true);
      await adminUserApi.updateStatus(selectedUser.id, targetStatus);
      setIsStatusModalOpen(false);
      setSelectedUser(null);
      await loadUsers();
    } catch (err) {
      console.error('Failed to update user status', err);
    } finally {
      setIsUpdating(false);
    }
  };

  const filteredUsers = users.filter((u) => {
    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      const matchName = `${u.firstName || ''} ${u.lastName || ''}`.toLowerCase().includes(q);
      const matchEmail = u.email?.toLowerCase().includes(q);
      const matchPhone = u.phone?.includes(q);
      if (!matchName && !matchEmail && !matchPhone) return false;
    }
    return true;
  });

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/90 shadow-sm">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-widest bg-blue-100 text-blue-800 border border-blue-200">
              Identity & Access
            </span>
            <span className="text-xs text-slate-500">• Platform SuperAdmin</span>
          </div>
          <h1 className="text-2xl font-black text-slate-900 flex items-center gap-2.5">
            <Users className="w-7 h-7 text-blue-600" />
            Platform User Governance
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            Monitor and govern user accounts across SuperAdmins, Practitioners, Staff, and Client Taxpayers.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="secondary" onClick={loadUsers} disabled={isLoading} className="text-xs gap-1.5">
            <RefreshCw className={clsx('w-3.5 h-3.5', isLoading && 'animate-spin')} /> Refresh
          </Button>
        </div>
      </div>

      {/* Filters & Search */}
      <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search by name, email, phone..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        {/* Role Tabs */}
        <div className="flex items-center gap-2 w-full md:w-auto overflow-x-auto">
          {[
            { label: 'All Roles', value: 'ALL' },
            { label: 'SuperAdmin', value: 'SUPER_ADMIN' },
            { label: 'Practitioners / CAs', value: 'ORG_ADMIN' },
            { label: 'Staff / Trainees', value: 'STAFF' },
            { label: 'Customers', value: 'CLIENT_USER' },
          ].map((tab) => (
            <button
              key={tab.value}
              onClick={() => setRoleFilter(tab.value)}
              className={clsx(
                'px-3 py-1.5 rounded-lg text-xs font-bold whitespace-nowrap transition-colors',
                roleFilter === tab.value
                  ? 'bg-blue-600 text-white'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Users Table */}
      <div className="bg-white border border-slate-200 rounded-xl shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 font-semibold text-slate-500 uppercase tracking-wider">
                <th className="px-5 py-3">User Name</th>
                <th className="px-4 py-3">Email & Contact</th>
                <th className="px-4 py-3">Assigned Roles</th>
                <th className="px-4 py-3">Created</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-5 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="text-center py-10 text-slate-400">Loading platform users...</td>
                </tr>
              ) : filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-10 text-slate-400">No users found matching criteria</td>
                </tr>
              ) : (
                filteredUsers.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="px-5 py-3.5">
                      <div className="font-bold text-slate-900 text-sm">
                        {u.firstName} {u.lastName}
                      </div>
                      <div className="text-[11px] text-slate-400 font-mono mt-0.5">ID: {u.id.substring(0, 8)}...</div>
                    </td>
                    <td className="px-4 py-3.5 text-slate-600">
                      <div className="flex items-center gap-1.5 font-medium text-slate-800">
                        <Mail className="w-3.5 h-3.5 text-slate-400" />
                        <span>{u.email}</span>
                      </div>
                      {u.phone && (
                        <div className="flex items-center gap-1.5 text-slate-400 mt-0.5">
                          <Phone className="w-3 h-3" />
                          <span>{u.phone}</span>
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3.5">
                        {u.roles?.map((r, idx) => {
                          const code = typeof r === 'string' ? r : r.code;
                          const name = typeof r === 'string' ? r : r.name || r.code;
                          return (
                            <span
                              key={code || idx}
                              className={clsx(
                                'px-2 py-0.5 rounded-full text-[10px] font-bold border',
                                code === 'SUPER_ADMIN' ? 'bg-purple-50 text-purple-700 border-purple-200' :
                                code === 'ORG_ADMIN' ? 'bg-blue-50 text-blue-700 border-blue-200' :
                                code === 'STAFF' ? 'bg-amber-50 text-amber-700 border-amber-200' :
                                'bg-slate-100 text-slate-700 border-slate-200'
                              )}
                            >
                              {name}
                            </span>
                          );
                        }) || <span className="text-slate-400">Standard User</span>}
                    </td>
                    <td className="px-4 py-3.5 text-slate-500">
                      {u.createdAt ? new Date(u.createdAt).toLocaleDateString('en-IN', { dateStyle: 'medium' }) : 'N/A'}
                    </td>
                    <td className="px-4 py-3.5">
                      <span className={clsx(
                        'px-2.5 py-0.5 rounded-full text-[10px] font-bold border',
                        u.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
                        u.status === 'SUSPENDED' ? 'bg-rose-50 text-rose-700 border-rose-200' :
                        'bg-slate-100 text-slate-600 border-slate-200'
                      )}>
                        {u.status || 'ACTIVE'}
                      </span>
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => {
                          setSelectedUser(u);
                          setTargetStatus(u.status || 'ACTIVE');
                          setIsStatusModalOpen(true);
                        }}
                        className="text-xs font-semibold"
                      >
                        Status
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Status Update Modal */}
      {isStatusModalOpen && selectedUser && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200 animate-scale-up">
            <h3 className="text-lg font-bold text-slate-900 mb-1">
              Update User Account Status
            </h3>
            <p className="text-xs text-slate-500 mb-4">
              User: <strong className="text-slate-800">{selectedUser.firstName} {selectedUser.lastName} ({selectedUser.email})</strong>
            </p>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Target Account Status</label>
                <select
                  value={targetStatus}
                  onChange={(e) => setTargetStatus(e.target.value)}
                  className="w-full p-2 text-xs rounded-lg border border-slate-300 focus:ring-2 focus:ring-blue-500"
                >
                  <option value="ACTIVE">ACTIVE (Normal Access)</option>
                  <option value="SUSPENDED">SUSPENDED (Access Blocked)</option>
                  <option value="INACTIVE">INACTIVE (Deactivated)</option>
                </select>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 mt-6 pt-4 border-t border-slate-100">
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
                disabled={isUpdating}
              >
                {isUpdating ? 'Updating...' : 'Confirm Status'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
