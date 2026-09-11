import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { UserCheck, Shield, Plus, Mail, Phone, Sparkles, Camera, KeyRound, UserX, RefreshCw, Send, CheckCircle2, AlertTriangle, AlertCircle, X } from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { teamApi } from '../api/endpoints';
import { Employee, Role } from '../types';
import { useBranding } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import { AddTeamMemberModal } from '../components/team/AddTeamMemberModal';
import { ChangeRoleModal } from '../components/team/ChangeRoleModal';

type StatusFilterType = 'ALL' | 'ACTIVE' | 'INVITED' | 'SUSPENDED' | 'INACTIVE';

interface StatusModalState {
  isOpen: boolean;
  employee: Employee | null;
  targetStatus: 'ACTIVE' | 'SUSPENDED' | 'INACTIVE' | null;
}

export const TeamManagementPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [activeTab, setActiveTab] = useState<'employees' | 'roles'>('employees');
  const [statusFilter, setStatusFilter] = useState<StatusFilterType>('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [isAddModalOpen, setIsAddModalOpen] = useState(
    () => searchParams.get('action') === 'add' || searchParams.get('action') === 'new' || searchParams.get('create') === 'true'
  );
  const [isChangeRoleModalOpen, setIsChangeRoleModalOpen] = useState(false);
  const [selectedEmployeeForRole, setSelectedEmployeeForRole] = useState<Employee | null>(null);

  const [statusModal, setStatusModal] = useState<StatusModalState>({
    isOpen: false,
    employee: null,
    targetStatus: null,
  });
  const [isStatusSubmitting, setIsStatusSubmitting] = useState(false);
  const [resendingEmployeeId, setResendingEmployeeId] = useState<string | null>(null);
  const [feedbackBanner, setFeedbackBanner] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const { user } = useAuth();
  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isFirmAdmin = userRoleCodes.some((r: string) => ['ORG_ADMIN', 'SUPER_ADMIN', 'PARTNER', 'PRACTICE_OWNER', 'PRACTICE_ADMIN'].includes(r));
  const isStaff = userRoleCodes.some((r: string) => ['ARTICLE_ASSISTANT', 'STAFF', 'TRAINEE'].includes(r)) && !isFirmAdmin;

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    if (searchParams.get('action') === 'add' || searchParams.get('action') === 'new' || searchParams.get('create') === 'true') {
      setIsAddModalOpen(true);
    }
  }, [searchParams]);

  const loadData = async () => {
    try {
      setIsLoading(true);
      const [empRes, roleRes] = await Promise.all([
        teamApi.getEmployees(),
        teamApi.getRoles(),
      ]);
      setEmployees(empRes.content || []);
      setRoles(roleRes || []);
    } catch (err) {
      console.error('Failed to load team data', err);
    } finally {
      setIsLoading(false);
    }
  };

  const { getEmployeeAvatar, setEmployeeAvatar, currentTheme } = useBranding();
  const fileInputRef = React.useRef<HTMLInputElement>(null);
  const [targetEmployeeEmail, setTargetEmployeeEmail] = useState<string | null>(null);

  const handleAvatarFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file && targetEmployeeEmail) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setEmployeeAvatar(targetEmployeeEmail, reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleChangeRoleClick = (employee: Employee) => {
    setSelectedEmployeeForRole(employee);
    setIsChangeRoleModalOpen(true);
  };

  const handleOpenStatusModal = (employee: Employee, targetStatus: 'ACTIVE' | 'SUSPENDED' | 'INACTIVE') => {
    setFeedbackBanner(null);
    setStatusModal({
      isOpen: true,
      employee,
      targetStatus,
    });
  };

  const handleCloseStatusModal = () => {
    if (!isStatusSubmitting) {
      setStatusModal({
        isOpen: false,
        employee: null,
        targetStatus: null,
      });
    }
  };

  const handleConfirmStatusChange = async () => {
    if (!statusModal.employee || !statusModal.targetStatus) return;

    setIsStatusSubmitting(true);
    try {
      await teamApi.updateEmployeeStatus(statusModal.employee.id, statusModal.targetStatus);
      const actionLabel =
        statusModal.targetStatus === 'ACTIVE'
          ? 'reactivated'
          : statusModal.targetStatus === 'SUSPENDED'
          ? 'suspended'
          : 'deactivated';

      setFeedbackBanner({
        type: 'success',
        message: `Successfully ${actionLabel} ${statusModal.employee.firstName} ${statusModal.employee.lastName || ''} (${statusModal.employee.employeeNumber || statusModal.employee.employeeCode}).`,
      });
      handleCloseStatusModal();
      await loadData();
    } catch (err: any) {
      console.error('Failed to update employee status', err);
      const apiMsg = err.response?.data?.message || err.message || 'Failed to update employee status.';
      setFeedbackBanner({
        type: 'error',
        message: apiMsg,
      });
      handleCloseStatusModal();
    } finally {
      setIsStatusSubmitting(false);
    }
  };

  const handleResendInvitation = async (employee: Employee) => {
    setResendingEmployeeId(employee.id);
    setFeedbackBanner(null);
    try {
      await teamApi.resendInvitation(employee.id);
      setFeedbackBanner({
        type: 'success',
        message: `Fresh invitation and activation email successfully dispatched to ${employee.email}.`,
      });
    } catch (err: any) {
      console.error('Failed to resend invitation', err);
      const apiMsg = err.response?.data?.message || err.message || 'Failed to resend invitation email.';
      setFeedbackBanner({
        type: 'error',
        message: apiMsg,
      });
    } finally {
      setResendingEmployeeId(null);
    }
  };

  // Filter employees according to status filter
  const filteredEmployees = employees.filter((emp) => {
    if (statusFilter === 'ALL') return true;
    const s = emp.status ? emp.status.toUpperCase() : 'ACTIVE';
    if (statusFilter === 'INVITED') {
      return s === 'INVITED' || s === 'PENDING';
    }
    return s === statusFilter;
  });

  const countActive = employees.filter((e) => (e.status || 'ACTIVE').toUpperCase() === 'ACTIVE').length;
  const countInvited = employees.filter((e) => ['INVITED', 'PENDING'].includes((e.status || '').toUpperCase())).length;
  const countSuspended = employees.filter((e) => (e.status || '').toUpperCase() === 'SUSPENDED').length;
  const countInactive = employees.filter((e) => ['INACTIVE', 'TERMINATED', 'RESIGNED'].includes((e.status || '').toUpperCase())).length;

  const employeeColumns: Column<Employee>[] = [
    {
      header: 'Staff Member',
      accessor: (row) => {
        const avatar = getEmployeeAvatar(row.email || row.id);
        return (
          <div className="flex items-center gap-3">
            <div className="relative group shrink-0">
              {avatar ? (
                <img
                  src={avatar}
                  alt={row.firstName}
                  className="w-9 h-9 rounded-full object-cover border border-slate-200 shadow-2xs"
                />
              ) : (
                <div
                  className="w-9 h-9 rounded-full text-white font-bold text-xs flex items-center justify-center shadow-2xs"
                  style={{ backgroundColor: currentTheme.primaryColor }}
                >
                  {row.firstName ? row.firstName.charAt(0).toUpperCase() : 'E'}
                </div>
              )}
            </div>
            <div>
              <span className="font-bold text-slate-900 block">{row.firstName} {row.lastName || ''}</span>
              <span className="font-mono text-[10px] text-slate-500 font-semibold block">{row.employeeNumber || row.employeeCode}</span>
            </div>
          </div>
        );
      },
    },
    {
      header: 'Practice Role (RBAC)',
      accessor: (row) => {
        const roleDisplay = row.roleName || (
          roles.find((r) => r.code === row.roleCode)?.name || row.roleCode || 'Practitioner'
        );
        const isAdminRole = ['ORG_ADMIN', 'PRACTICE_ADMIN', 'PRACTICE_OWNER', 'PARTNER'].includes(row.roleCode || '');

        return (
          <div className="flex items-center gap-1.5">
            <span
              className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                isAdminRole
                  ? 'bg-amber-50 text-amber-800 border border-amber-200/80'
                  : 'bg-slate-100 text-slate-700 border border-slate-200'
              }`}
            >
              <Shield className="w-3 h-3 text-brand-600 shrink-0" />
              {roleDisplay}
            </span>
          </div>
        );
      },
    },
    {
      header: 'Designation',
      accessor: (row) => (
        <span className="text-xs font-medium text-slate-800">{row.designation || 'Staff Associate'}</span>
      ),
    },
    {
      header: 'Department',
      accessor: (row) => <span className="text-xs font-medium text-slate-700">{row.department || 'General Tax'}</span>,
    },
    {
      header: 'Email Address',
      accessor: (row) => <span className="text-xs text-slate-700">{row.email}</span>,
    },
    {
      header: 'Status',
      accessor: (row) => <StatusBadge status={row.status} size="sm" />,
      align: 'center',
    },
    {
      header: 'Actions',
      align: 'right',
      cell: (row) => {
        const normalizedStatus = (row.status || 'ACTIVE').toUpperCase();
        const isCurrentUser = Boolean(
          user && (
            (row.userId && row.userId === user.id) ||
            (user.email && row.email && row.email.toLowerCase() === user.email.toLowerCase())
          )
        );

        return (
          <div className="flex items-center justify-end gap-1.5 flex-wrap">
            {/* Active Actions */}
            {normalizedStatus === 'ACTIVE' && isFirmAdmin && (
              <>
                <button
                  onClick={() => handleChangeRoleClick(row)}
                  className="px-2.5 py-1 bg-brand-50 hover:bg-brand-100 text-brand-700 border border-brand-200/80 rounded-lg text-xs font-semibold inline-flex items-center gap-1 transition-colors shadow-2xs"
                  title="Change practice role and access permissions"
                >
                  <KeyRound className="w-3.5 h-3.5 text-brand-600" />
                  Role
                </button>
                <button
                  disabled={isCurrentUser}
                  onClick={() => handleOpenStatusModal(row, 'SUSPENDED')}
                  className={`px-2 py-1 rounded-lg text-xs font-semibold inline-flex items-center gap-1 transition-colors ${
                    isCurrentUser
                      ? 'bg-slate-100 text-slate-400 cursor-not-allowed opacity-60'
                      : 'bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-200'
                  }`}
                  title={isCurrentUser ? 'You cannot suspend your own account' : 'Temporarily suspend staff access'}
                >
                  <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
                  Suspend
                </button>
                <button
                  disabled={isCurrentUser}
                  onClick={() => handleOpenStatusModal(row, 'INACTIVE')}
                  className={`px-2 py-1 rounded-lg text-xs font-semibold inline-flex items-center gap-1 transition-colors ${
                    isCurrentUser
                      ? 'bg-slate-100 text-slate-400 cursor-not-allowed opacity-60'
                      : 'bg-slate-100 hover:bg-rose-50 text-slate-700 hover:text-rose-700 border border-slate-200 hover:border-rose-200'
                  }`}
                  title={isCurrentUser ? 'You cannot deactivate your own account' : 'Deactivate employee'}
                >
                  <UserX className="w-3.5 h-3.5" />
                  Deactivate
                </button>
              </>
            )}

            {/* Suspended Actions */}
            {normalizedStatus === 'SUSPENDED' && isFirmAdmin && (
              <>
                <button
                  onClick={() => handleOpenStatusModal(row, 'ACTIVE')}
                  className="px-2.5 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-200 rounded-lg text-xs font-semibold inline-flex items-center gap-1 transition-colors shadow-2xs"
                  title="Reactivate employee access"
                >
                  <RefreshCw className="w-3.5 h-3.5 text-emerald-600" />
                  Reactivate
                </button>
                <button
                  disabled={isCurrentUser}
                  onClick={() => handleOpenStatusModal(row, 'INACTIVE')}
                  className="px-2 py-1 bg-slate-100 hover:bg-rose-50 text-slate-700 hover:text-rose-700 border border-slate-200 rounded-lg text-xs font-semibold inline-flex items-center gap-1 transition-colors"
                  title="Deactivate employee"
                >
                  <UserX className="w-3.5 h-3.5" />
                  Deactivate
                </button>
              </>
            )}

            {/* Inactive Actions */}
            {(normalizedStatus === 'INACTIVE' || normalizedStatus === 'TERMINATED') && isFirmAdmin && (
              <button
                onClick={() => handleOpenStatusModal(row, 'ACTIVE')}
                className="px-2.5 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-200 rounded-lg text-xs font-semibold inline-flex items-center gap-1 transition-colors shadow-2xs"
                title="Restore and reactivate employee"
              >
                <RefreshCw className="w-3.5 h-3.5 text-emerald-600" />
                Reactivate
              </button>
            )}

            {/* Invited Actions */}
            {(normalizedStatus === 'INVITED' || normalizedStatus === 'PENDING') && isFirmAdmin && (
              <>
                <button
                  disabled={resendingEmployeeId === row.id}
                  onClick={() => handleResendInvitation(row)}
                  className="px-2.5 py-1 bg-blue-50 hover:bg-blue-100 text-blue-800 border border-blue-200 rounded-lg text-xs font-semibold inline-flex items-center gap-1 transition-colors shadow-2xs"
                  title="Resend activation invitation email"
                >
                  <Send className={`w-3.5 h-3.5 text-blue-600 ${resendingEmployeeId === row.id ? 'animate-pulse' : ''}`} />
                  {resendingEmployeeId === row.id ? 'Sending...' : 'Resend Invite'}
                </button>
                <button
                  disabled={isCurrentUser}
                  onClick={() => handleOpenStatusModal(row, 'INACTIVE')}
                  className="px-2 py-1 bg-slate-100 hover:bg-rose-50 text-slate-700 hover:text-rose-700 border border-slate-200 rounded-lg text-xs font-semibold inline-flex items-center gap-1 transition-colors"
                  title="Cancel invitation & deactivate"
                >
                  <UserX className="w-3.5 h-3.5" />
                  Cancel
                </button>
              </>
            )}

            {/* Photo Action */}
            <button
              onClick={() => {
                setTargetEmployeeEmail(row.email || row.id);
                fileInputRef.current?.click();
              }}
              className="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-lg text-xs font-semibold inline-flex items-center gap-1 transition-colors"
              title="Upload staff profile picture"
            >
              <Camera className="w-3.5 h-3.5 text-slate-500" />
              Photo
            </button>
          </div>
        );
      },
    },
  ];

  return (
    <div className="space-y-6">
      {/* Feedback Banner */}
      {feedbackBanner && (
        <div
          className={`p-3.5 rounded-xl border flex items-center justify-between text-xs transition-all ${
            feedbackBanner.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900 font-semibold'
              : 'bg-rose-50 border-rose-200 text-rose-900'
          }`}
        >
          <div className="flex items-center gap-2">
            {feedbackBanner.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            ) : (
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            )}
            <span>{feedbackBanner.message}</span>
          </div>
          <button
            onClick={() => setFeedbackBanner(null)}
            className="text-slate-400 hover:text-slate-600 p-1 rounded-md"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">
            {isStaff ? 'Department Team Directory' : 'Team Directory & RBAC'}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            {isStaff
              ? 'Directory of colleagues in your practice department and reporting leads.'
              : 'Manage practice partners, staff status lifecycle, access privileges, and RBAC roles.'}
          </p>
        </div>
        {!isStaff && (
          <div className="flex items-center gap-2">
            <Link to="/team/bulk">
              <Button variant="outline" leftIcon={<Sparkles className="w-4 h-4 text-brand-600" />}>
                ⚡ Bulk Onboard Team
              </Button>
            </Link>
            <Button leftIcon={<Plus className="w-4 h-4" />} onClick={() => setIsAddModalOpen(true)}>
              Add Team Member
            </Button>
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="border-b border-slate-200 flex items-center justify-between gap-2 flex-wrap">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setActiveTab('employees')}
            className={`px-4 py-2.5 text-xs font-bold border-b-2 transition-all ${
              activeTab === 'employees'
                ? 'border-brand-600 text-brand-600 bg-brand-50/50 rounded-t-lg'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            {isStaff ? 'Department Members' : 'Employee Directory'} ({employees.length})
          </button>
          {!isStaff && (
            <button
              onClick={() => setActiveTab('roles')}
              className={`px-4 py-2.5 text-xs font-bold border-b-2 transition-all ${
                activeTab === 'roles'
                  ? 'border-brand-600 text-brand-600 bg-brand-50/50 rounded-t-lg'
                  : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}
            >
              Practice Roles & Permissions Matrix ({roles.length})
            </button>
          )}
        </div>

        {/* Status Lifecycle Filter Chips (Only shown in employees tab) */}
        {activeTab === 'employees' && (
          <div className="flex items-center gap-1 pb-1">
            <span className="text-[11px] font-semibold text-slate-400 mr-1 hidden sm:inline">Status:</span>
            <button
              onClick={() => setStatusFilter('ALL')}
              className={`px-2.5 py-1 rounded-full text-xs font-bold transition-colors ${
                statusFilter === 'ALL'
                  ? 'bg-slate-900 text-white shadow-2xs'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              All ({employees.length})
            </button>
            <button
              onClick={() => setStatusFilter('ACTIVE')}
              className={`px-2.5 py-1 rounded-full text-xs font-bold transition-colors ${
                statusFilter === 'ACTIVE'
                  ? 'bg-emerald-600 text-white shadow-2xs'
                  : 'bg-emerald-50 text-emerald-800 hover:bg-emerald-100'
              }`}
            >
              Active ({countActive})
            </button>
            <button
              onClick={() => setStatusFilter('INVITED')}
              className={`px-2.5 py-1 rounded-full text-xs font-bold transition-colors ${
                statusFilter === 'INVITED'
                  ? 'bg-blue-600 text-white shadow-2xs'
                  : 'bg-blue-50 text-blue-800 hover:bg-blue-100'
              }`}
            >
              Invited ({countInvited})
            </button>
            <button
              onClick={() => setStatusFilter('SUSPENDED')}
              className={`px-2.5 py-1 rounded-full text-xs font-bold transition-colors ${
                statusFilter === 'SUSPENDED'
                  ? 'bg-rose-600 text-white shadow-2xs'
                  : 'bg-rose-50 text-rose-800 hover:bg-rose-100'
              }`}
            >
              Suspended ({countSuspended})
            </button>
            <button
              onClick={() => setStatusFilter('INACTIVE')}
              className={`px-2.5 py-1 rounded-full text-xs font-bold transition-colors ${
                statusFilter === 'INACTIVE'
                  ? 'bg-slate-600 text-white shadow-2xs'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              Inactive ({countInactive})
            </button>
          </div>
        )}
      </div>

      {/* Content */}
      {activeTab === 'employees' ? (
        <DataTable
          columns={employeeColumns}
          data={filteredEmployees}
          isLoading={isLoading}
          searchPlaceholder="Search employees by name, email, role, or code..."
        />
      ) : (
        <div className="space-y-4">
          <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl flex items-center justify-between text-xs text-slate-600">
            <div className="flex items-center gap-2">
              <Shield className="w-4 h-4 text-brand-600" />
              <span>
                These roles and permission boundaries apply strictly within your practice organization. Platform-internal administrator roles are segregated and not visible here.
              </span>
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {roles.map((role) => (
              <div key={role.id} className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card space-y-3">
                <div className="flex items-center justify-between">
                  <span className="font-mono text-xs font-bold bg-slate-100 text-slate-800 px-2 py-0.5 rounded border border-slate-200">
                    {role.code}
                  </span>
                  {role.isSystemRole && (
                    <span className="text-[10px] text-brand-600 font-semibold bg-brand-50 border border-brand-200 px-2 py-0.5 rounded-full">
                      Practice Standard Role
                    </span>
                  )}
                </div>
                <div>
                  <h4 className="text-sm font-bold text-slate-900">{role.name}</h4>
                  <p className="text-xs text-slate-500 mt-0.5">{role.description || 'Practice role and capability definition'}</p>
                </div>
                <div className="pt-3 border-t border-slate-100 text-[10px] text-slate-500">
                  <span className="font-semibold text-slate-700">{role.permissions?.length || 0}</span> Permissions Configured
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Add Team Member Modal */}
      <AddTeamMemberModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onSuccess={loadData}
      />

      {/* Change Role Modal */}
      <ChangeRoleModal
        isOpen={isChangeRoleModalOpen}
        employee={selectedEmployeeForRole}
        availableRoles={roles}
        onClose={() => {
          setIsChangeRoleModalOpen(false);
          setSelectedEmployeeForRole(null);
        }}
        onSuccess={loadData}
      />

      {/* Status Lifecycle Confirmation Modal */}
      {statusModal.isOpen && statusModal.employee && statusModal.targetStatus && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="relative bg-white rounded-2xl max-w-md w-full shadow-2xl border border-slate-200 overflow-hidden transform transition-all">
            <div className="p-6 space-y-4">
              <div className="flex items-center gap-3">
                <div
                  className={`w-11 h-11 rounded-xl flex items-center justify-center border shrink-0 ${
                    statusModal.targetStatus === 'SUSPENDED'
                      ? 'bg-amber-50 text-amber-600 border-amber-200'
                      : statusModal.targetStatus === 'INACTIVE'
                      ? 'bg-rose-50 text-rose-600 border-rose-200'
                      : 'bg-emerald-50 text-emerald-600 border-emerald-200'
                  }`}
                >
                  {statusModal.targetStatus === 'SUSPENDED' && <AlertTriangle className="w-6 h-6" />}
                  {statusModal.targetStatus === 'INACTIVE' && <UserX className="w-6 h-6" />}
                  {statusModal.targetStatus === 'ACTIVE' && <RefreshCw className="w-6 h-6" />}
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    {statusModal.targetStatus === 'SUSPENDED' && 'Suspend Employee Access?'}
                    {statusModal.targetStatus === 'INACTIVE' && 'Deactivate Employee?'}
                    {statusModal.targetStatus === 'ACTIVE' && 'Reactivate Employee Access?'}
                  </h3>
                  <p className="text-xs text-slate-500 font-medium">
                    {statusModal.employee.firstName} {statusModal.employee.lastName || ''} ({statusModal.employee.employeeNumber || statusModal.employee.employeeCode})
                  </p>
                </div>
              </div>

              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 text-xs text-slate-600 space-y-1.5">
                {statusModal.targetStatus === 'SUSPENDED' && (
                  <>
                    <p className="font-semibold text-amber-900">Immediate access suspension consequences:</p>
                    <ul className="list-disc list-inside space-y-1 text-slate-600">
                      <li>Active sessions and refresh tokens will be immediately terminated.</li>
                      <li>Employee cannot log in until reactivated by an administrator.</li>
                      <li>Assigned tasks, client records, and audit history are safely preserved.</li>
                    </ul>
                  </>
                )}
                {statusModal.targetStatus === 'INACTIVE' && (
                  <>
                    <p className="font-semibold text-rose-900">Deactivation details:</p>
                    <ul className="list-disc list-inside space-y-1 text-slate-600">
                      <li>Staff login will be disabled and sessions revoked.</li>
                      <li>Historical filings, invoices, and work logs remain fully intact.</li>
                    </ul>
                  </>
                )}
                {statusModal.targetStatus === 'ACTIVE' && (
                  <>
                    <p className="font-semibold text-emerald-900">Reactivation details:</p>
                    <ul className="list-disc list-inside space-y-1 text-slate-600">
                      <li>Restores login access and enables practice portal capabilities.</li>
                      <li>Employee role and department assignments remain active.</li>
                    </ul>
                  </>
                )}
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleCloseStatusModal}
                  disabled={isStatusSubmitting}
                >
                  Cancel
                </Button>
                <Button
                  type="button"
                  isLoading={isStatusSubmitting}
                  onClick={handleConfirmStatusChange}
                  className={
                    statusModal.targetStatus === 'SUSPENDED'
                      ? 'bg-amber-600 hover:bg-amber-700 text-white'
                      : statusModal.targetStatus === 'INACTIVE'
                      ? 'bg-rose-600 hover:bg-rose-700 text-white'
                      : 'bg-emerald-600 hover:bg-emerald-700 text-white'
                  }
                >
                  {statusModal.targetStatus === 'SUSPENDED' && 'Confirm Suspension'}
                  {statusModal.targetStatus === 'INACTIVE' && 'Confirm Deactivation'}
                  {statusModal.targetStatus === 'ACTIVE' && 'Confirm Reactivation'}
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Hidden File Input for Avatar Upload */}
      <input
        type="file"
        ref={fileInputRef}
        accept="image/png, image/jpeg, image/webp"
        onChange={handleAvatarFileChange}
        className="hidden"
      />
    </div>
  );
};
