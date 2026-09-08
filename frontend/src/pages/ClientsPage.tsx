import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Plus,
  Eye,
  Edit2,
  Trash2,
  Building2,
  User,
  FileSpreadsheet,
  CheckCircle2,
  PauseCircle,
  Ban,
  Archive,
  Power,
  ShieldAlert,
  AlertTriangle,
  RotateCcw,
  Clock,
  Mail,
  Send,
} from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { Drawer } from '../components/common/Drawer';
import { clientApi } from '../api/endpoints';
import { useAuth } from '../context/AuthContext';
import { Client } from '../types';
import { ClientDocumentRequestsTab } from '../components/docrequest/ClientDocumentRequestsTab';
import clsx from 'clsx';

export const ClientsPage: React.FC = () => {
  const [clients, setClients] = useState<Client[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedClient, setSelectedClient] = useState<Client | null>(null);
  const [drawerTab, setDrawerTab] = useState<'overview' | 'doc_requests'>('overview');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [statusUpdatingId, setStatusUpdatingId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'ARCHIVED'>('ALL');
  const [portalStatusFilter, setPortalStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INVITED' | 'SUSPENDED' | 'INACTIVE' | 'NOT_ENABLED'>('ALL');

  // Confirmation Modal State
  const [portalModalAction, setPortalModalAction] = useState<{
    type: 'SUSPEND' | 'RESTORE' | 'DEACTIVATE' | 'RESEND';
    client: Client;
  } | null>(null);

  // Form State
  const [formData, setFormData] = useState({
    displayName: '',
    legalName: '',
    pan: '',
    gstin: '',
    clientType: 'PRIVATE_LIMITED',
    email: '',
    phone: '',
    status: 'ACTIVE',
  });

  useEffect(() => {
    loadClients();
  }, [page, pageSize, statusFilter, portalStatusFilter]);

  const loadClients = async () => {
    try {
      setIsLoading(true);
      const params: any = { page, size: pageSize };
      if (statusFilter !== 'ALL') {
        params.status = statusFilter;
      }
      if (portalStatusFilter !== 'ALL') {
        params.portalStatus = portalStatusFilter;
      }
      const res = await clientApi.getAll(params);
      setClients(res.content);
      setTotalElements(res.totalElements);
    } catch (err) {
      console.error('Failed to load clients', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpdateStatus = async (
    clientId: string,
    newStatus: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'ARCHIVED'
  ) => {
    try {
      setStatusUpdatingId(clientId);
      await clientApi.updateStatus(clientId, newStatus);
      setClients((prev) =>
        prev.map((c) => (c.id === clientId ? { ...c, status: newStatus, ...(newStatus === 'ARCHIVED' ? { portalStatus: 'INACTIVE' } : {}) } : c))
      );
      if (selectedClient && selectedClient.id === clientId) {
        setSelectedClient({ ...selectedClient, status: newStatus, ...(newStatus === 'ARCHIVED' ? { portalStatus: 'INACTIVE' } : {}) });
      }
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update client status');
    } finally {
      setStatusUpdatingId(null);
    }
  };

  const [resendingId, setResendingId] = useState<string | null>(null);

  const handleUpdatePortalStatus = async (clientId: string, newPortalStatus: 'ACTIVE' | 'SUSPENDED' | 'INACTIVE') => {
    try {
      setStatusUpdatingId(clientId);
      const updated = await clientApi.updatePortalStatus(clientId, newPortalStatus);
      setClients((prev) =>
        prev.map((c) => (c.id === clientId ? { ...c, portalStatus: updated.portalStatus } : c))
      );
      if (selectedClient && selectedClient.id === clientId) {
        setSelectedClient({ ...selectedClient, portalStatus: updated.portalStatus });
      }
      setPortalModalAction(null);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update portal access status');
    } finally {
      setStatusUpdatingId(null);
    }
  };

  const handleResendPortalInvite = async (clientId: string) => {
    try {
      setResendingId(clientId);
      await clientApi.resendPortalInvitation(clientId);
      alert('Client portal activation invitation email has been resent successfully!');
      setPortalModalAction(null);
      loadClients();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to resend portal invitation');
    } finally {
      setResendingId(null);
    }
  };

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState('');

  const handleCreateClient = async (e: React.FormEvent) => {
    e.preventDefault();
    setFieldErrors({});
    setGeneralError('');

    try {
      setIsSubmitting(true);
      const payload: any = {
        displayName: formData.displayName.trim(),
        pan: formData.pan.trim().toUpperCase(),
        clientType: formData.clientType,
        status: formData.status,
      };

      if (formData.legalName.trim()) payload.legalName = formData.legalName.trim();
      if (formData.gstin.trim()) payload.gstin = formData.gstin.trim().toUpperCase();
      if (formData.email.trim()) payload.email = formData.email.trim();
      if (formData.phone.trim()) payload.phone = formData.phone.trim();

      await clientApi.create(payload);
      setIsModalOpen(false);
      setFormData({
        displayName: '',
        legalName: '',
        pan: '',
        gstin: '',
        clientType: 'PRIVATE_LIMITED',
        email: '',
        phone: '',
        status: 'ACTIVE',
      });
      loadClients();
    } catch (err: any) {
      const data = err.response?.data;
      if (data?.validationErrors && Array.isArray(data.validationErrors)) {
        const errorsMap: Record<string, string> = {};
        data.validationErrors.forEach((vErr: { field: string; message: string }) => {
          errorsMap[vErr.field] = vErr.message;
        });
        setFieldErrors(errorsMap);
      } else {
        setGeneralError(data?.message || 'Failed to create client. Please check your inputs.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const clientColumns: Column<Client>[] = [
    {
      header: 'Client / Business Name',
      accessor: (row) => (
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-brand-50 border border-brand-200 text-brand-600 flex items-center justify-center font-bold text-xs shrink-0">
            {row.clientType === 'INDIVIDUAL' ? <User className="w-4 h-4" /> : <Building2 className="w-4 h-4" />}
          </div>
          <div>
            <span className="font-bold text-slate-900 block">{row.displayName}</span>
            <span className="text-[10px] text-slate-400 font-medium">
              {row.clientType.replace('_', ' ')} {row.legalName ? `• ${row.legalName}` : ''}
            </span>
          </div>
        </div>
      ),
    },
    {
      header: 'PAN Card',
      accessor: (row) => (
        <span className="font-mono text-xs font-semibold bg-slate-100 px-2 py-0.5 rounded border border-slate-200 text-slate-800">
          {row.pan}
        </span>
      ),
    },
    {
      header: 'GSTIN',
      accessor: (row) => row.gstin ? (
        <span className="font-mono text-xs text-slate-700">{row.gstin}</span>
      ) : (
        <span className="text-slate-400 italic">Not Registered</span>
      ),
    },
    {
      header: 'Contact Email',
      accessor: (row) => row.email || <span className="text-slate-400">—</span>,
    },
    {
      header: 'Client Portal',
      accessor: (row) => {
        const status = (row as any).portalStatus || (row.email ? 'INVITED' : 'NO_EMAIL');
        if (status === 'ACTIVE') {
          return (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
              <CheckCircle2 className="w-3 h-3 text-emerald-600" />
              Active
            </span>
          );
        }
        if (status === 'INVITED') {
          return (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200">
              <Clock className="w-3 h-3 text-amber-600" />
              Invite Sent
            </span>
          );
        }
        if (status === 'SUSPENDED') {
          return (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-50 text-rose-700 border border-rose-200">
              <Ban className="w-3 h-3 text-rose-600" />
              Suspended
            </span>
          );
        }
        if (status === 'INACTIVE') {
          return (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700 border border-slate-200">
              <PauseCircle className="w-3 h-3 text-slate-500" />
              Inactive
            </span>
          );
        }
        return (
          <span className="text-[10px] text-slate-400 font-medium italic">
            {row.email ? 'Not Set Up' : 'No Email'}
          </span>
        );
      },
      align: 'center',
    },
    {
      header: 'Client Status',
      accessor: (row) => <StatusBadge status={row.status} size="sm" />,
      align: 'center',
    },
    {
      header: 'Quick Actions',
      align: 'right',
      cell: (row) => {
        const isUpdating = statusUpdatingId === row.id;
        const isResending = resendingId === row.id;
        const portalStatus = (row as any).portalStatus;

        return (
          <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
            {/* Resend Portal Invitation button if invited or no password */}
            {row.email && portalStatus === 'INVITED' && (
              <button
                disabled={isResending || isUpdating}
                onClick={() => setPortalModalAction({ type: 'RESEND', client: row })}
                className="px-2 py-1 bg-sky-50 hover:bg-sky-100 text-sky-700 border border-sky-200 rounded text-[11px] font-semibold inline-flex items-center gap-1 transition-colors disabled:opacity-50"
                title="Resend portal invitation"
              >
                <Send className="w-3 h-3 text-sky-600" />
                <span>Invite</span>
              </button>
            )}

            {/* Portal Lifecycle Action Triggers */}
            {portalStatus === 'ACTIVE' && (
              <button
                disabled={isUpdating}
                onClick={() => setPortalModalAction({ type: 'SUSPEND', client: row })}
                className="px-2 py-1 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded text-[11px] font-semibold inline-flex items-center gap-1 transition-colors disabled:opacity-50"
                title="Suspend portal access"
              >
                <Ban className="w-3 h-3 text-rose-600" />
                <span>Suspend Portal</span>
              </button>
            )}

            {portalStatus === 'SUSPENDED' && (
              <button
                disabled={isUpdating}
                onClick={() => setPortalModalAction({ type: 'RESTORE', client: row })}
                className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded text-[11px] font-semibold inline-flex items-center gap-1 transition-colors disabled:opacity-50"
                title="Restore portal access"
              >
                <RotateCcw className="w-3 h-3 text-emerald-600" />
                <span>Restore Portal</span>
              </button>
            )}

            {/* 360 View Button */}
            <button
              onClick={() => setSelectedClient(row)}
              className="p-1.5 text-slate-500 hover:text-brand-600 hover:bg-slate-100 rounded-md transition-colors"
              title="View 360° Profile"
            >
              <Eye className="w-4 h-4" />
            </button>
          </div>
        );
      },
    },
  ];

  const { user } = useAuth();
  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isFirmAdmin = userRoleCodes.some((r: string) => ['ORG_ADMIN', 'SUPER_ADMIN', 'PARTNER'].includes(r));
  const isStaff = userRoleCodes.some((r: string) => ['ARTICLE_ASSISTANT', 'STAFF', 'TRAINEE'].includes(r)) && !isFirmAdmin;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">
            {isStaff ? 'My Assigned Client Portfolio' : 'Clients Directory'}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            {isStaff
              ? 'Showing clients assigned to your department and active workflow deliverables.'
              : 'Centralized repository for corporate and individual clients, lifecycle management, and portal controls.'}
          </p>
        </div>
        {!isStaff && (
          <div className="flex flex-wrap items-center gap-2.5 w-full sm:w-auto">
            <Link to="/clients/migration" className="w-full sm:w-auto">
              <Button variant="outline" leftIcon={<FileSpreadsheet className="w-4 h-4 text-emerald-600" />} className="w-full sm:w-auto justify-center">
                Migrate / Bulk Import
              </Button>
            </Link>
            <Button onClick={() => setIsModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />} className="w-full sm:w-auto justify-center">
              Add New Client
            </Button>
          </div>
        )}
      </div>

      {isStaff && (
        <div className="bg-amber-50/80 border border-amber-200/80 rounded-xl p-3.5 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <span className="text-base">🔒</span>
            <div>
              <p className="text-xs font-bold text-amber-900">
                Departmental Data Boundary Active ({user?.firstName} {user?.lastName || ''})
              </p>
              <p className="text-[11px] text-amber-700">
                Under firm security policy, you have restricted visibility to your assigned client accounts and direct deliverables.
              </p>
            </div>
          </div>
          <span className="text-[11px] font-bold bg-amber-200/70 text-amber-800 px-2.5 py-1 rounded-md">
            {totalElements} Assigned Accounts
          </span>
        </div>
      )}

      {/* Lifecycle Status Filter Bars */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        {/* Client Business Lifecycle Tabs */}
        <div className="flex items-center gap-1 p-1 bg-slate-100 border border-slate-200 rounded-xl text-xs font-semibold overflow-x-auto no-scrollbar">
          <button
            onClick={() => setStatusFilter('ALL')}
            className={clsx(
              'px-3 py-1.5 rounded-lg transition-all shrink-0',
              statusFilter === 'ALL' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
            )}
          >
            All Clients
          </button>
          <button
            onClick={() => setStatusFilter('ACTIVE')}
            className={clsx(
              'px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5 shrink-0',
              statusFilter === 'ACTIVE' ? 'bg-white text-emerald-700 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
            )}
          >
            <span className="w-2 h-2 rounded-full bg-emerald-500" />
            <span>Active</span>
          </button>
          <button
            onClick={() => setStatusFilter('INACTIVE')}
            className={clsx(
              'px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5 shrink-0',
              statusFilter === 'INACTIVE' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
            )}
          >
            <span className="w-2 h-2 rounded-full bg-slate-400" />
            <span>Deactivated</span>
          </button>
          <button
            onClick={() => setStatusFilter('SUSPENDED')}
            className={clsx(
              'px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5 shrink-0',
              statusFilter === 'SUSPENDED' ? 'bg-white text-rose-700 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
            )}
          >
            <span className="w-2 h-2 rounded-full bg-rose-500" />
            <span>Suspended</span>
          </button>
          <button
            onClick={() => setStatusFilter('ARCHIVED')}
            className={clsx(
              'px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5 shrink-0',
              statusFilter === 'ARCHIVED' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
            )}
          >
            <Archive className="w-3.5 h-3.5 text-slate-400" />
            <span>Archived</span>
          </button>
        </div>

        {/* Portal Access Filter Selector */}
        <div className="flex items-center gap-2 text-xs">
          <span className="text-slate-500 font-semibold shrink-0">Portal Status:</span>
          <select
            value={portalStatusFilter}
            onChange={(e) => setPortalStatusFilter(e.target.value as any)}
            className="bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs font-semibold text-slate-800 shadow-2xs focus:ring-1 focus:ring-brand-500 outline-none"
          >
            <option value="ALL">All Portal States</option>
            <option value="ACTIVE">Active Portal</option>
            <option value="INVITED">Invite Pending</option>
            <option value="SUSPENDED">Portal Suspended</option>
            <option value="INACTIVE">Portal Inactive</option>
            <option value="NOT_ENABLED">Not Enabled</option>
          </select>
        </div>
      </div>

      {/* Data Table */}
      <DataTable
        columns={clientColumns}
        data={clients}
        isLoading={isLoading}
        searchPlaceholder="Search clients by name, PAN, or GSTIN..."
      />

      {/* Add Client Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Add New Client Account"
        subtitle="Onboard a new client into your practice"
      >
        <form onSubmit={handleCreateClient} className="space-y-4">
          {generalError && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-rose-700 text-xs font-semibold">
              {generalError}
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Client / Business Name <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              placeholder="e.g. Acme Tech Solutions Pvt Ltd"
              value={formData.displayName}
              onChange={(e) => {
                setFormData({ ...formData, displayName: e.target.value });
                if (fieldErrors.displayName) setFieldErrors({ ...fieldErrors, displayName: '' });
              }}
              className={`w-full text-xs px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 ${
                fieldErrors.displayName ? 'border-rose-400 bg-rose-50/20 focus:ring-rose-500/20' : 'border-slate-200 focus:ring-brand-500'
              }`}
            />
            {fieldErrors.displayName && (
              <p className="text-rose-600 text-[11px] font-medium mt-1">{fieldErrors.displayName}</p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Legal Registered Name <span className="text-slate-400">(Optional)</span>
            </label>
            <input
              type="text"
              placeholder="e.g. Acme Technology Solutions Private Limited"
              value={formData.legalName}
              onChange={(e) => setFormData({ ...formData, legalName: e.target.value })}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Permanent Account Number (PAN) <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                maxLength={10}
                placeholder="e.g. ABCDE1234F"
                value={formData.pan}
                onChange={(e) => {
                  setFormData({ ...formData, pan: e.target.value.toUpperCase() });
                  if (fieldErrors.pan) setFieldErrors({ ...fieldErrors, pan: '' });
                }}
                className={`w-full text-xs font-mono px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 uppercase ${
                  fieldErrors.pan ? 'border-rose-400 bg-rose-50/20 focus:ring-rose-500/20' : 'border-slate-200 focus:ring-brand-500'
                }`}
              />
              {fieldErrors.pan && (
                <p className="text-rose-600 text-[11px] font-medium mt-1">{fieldErrors.pan}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                GSTIN <span className="text-slate-400">(Optional)</span>
              </label>
              <input
                type="text"
                maxLength={15}
                placeholder="e.g. 27ABCDE1234F1Z5"
                value={formData.gstin}
                onChange={(e) => {
                  setFormData({ ...formData, gstin: e.target.value.toUpperCase() });
                  if (fieldErrors.gstin) setFieldErrors({ ...fieldErrors, gstin: '' });
                }}
                className={`w-full text-xs font-mono px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 uppercase ${
                  fieldErrors.gstin ? 'border-rose-400 bg-rose-50/20 focus:ring-rose-500/20' : 'border-slate-200 focus:ring-brand-500'
                }`}
              />
              {fieldErrors.gstin && (
                <p className="text-rose-600 text-[11px] font-medium mt-1">{fieldErrors.gstin}</p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Constitution Type <span className="text-rose-500">*</span>
              </label>
              <select
                value={formData.clientType}
                onChange={(e) => setFormData({ ...formData, clientType: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
              >
                <option value="INDIVIDUAL">Individual / Salaried</option>
                <option value="PROPRIETORSHIP">Proprietorship</option>
                <option value="PARTNERSHIP">Partnership Firm</option>
                <option value="LLP">Limited Liability Partnership (LLP)</option>
                <option value="PRIVATE_LIMITED">Private Limited Company</option>
                <option value="PUBLIC_LIMITED">Public Limited Company</option>
                <option value="TRUST">Trust / NGO</option>
                <option value="HUF">Hindu Undivided Family (HUF)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Initial Status
              </label>
              <select
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
              >
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive / Deactivated</option>
                <option value="PROSPECT">Prospect</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Phone Number <span className="text-slate-400">(Optional)</span>
              </label>
              <input
                type="tel"
                placeholder="e.g. 9811122233"
                value={formData.phone}
                onChange={(e) => {
                  setFormData({ ...formData, phone: e.target.value });
                  if (fieldErrors.phone) setFieldErrors({ ...fieldErrors, phone: '' });
                }}
                className={`w-full text-xs px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 ${
                  fieldErrors.phone ? 'border-rose-400 bg-rose-50/20 focus:ring-rose-500/20' : 'border-slate-200 focus:ring-brand-500'
                }`}
              />
              {fieldErrors.phone && (
                <p className="text-rose-600 text-[11px] font-medium mt-1">{fieldErrors.phone}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Email Address <span className="text-slate-400">(Optional)</span>
              </label>
              <input
                type="email"
                placeholder="e.g. finance@acmetech.com"
                value={formData.email}
                onChange={(e) => {
                  setFormData({ ...formData, email: e.target.value });
                  if (fieldErrors.email) setFieldErrors({ ...fieldErrors, email: '' });
                }}
                className={`w-full text-xs px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 ${
                  fieldErrors.email ? 'border-rose-400 bg-rose-50/20 focus:ring-rose-500/20' : 'border-slate-200 focus:ring-brand-500'
                }`}
              />
              {fieldErrors.email && (
                <p className="text-rose-600 text-[11px] font-medium mt-1">{fieldErrors.email}</p>
              )}
            </div>
          </div>

          <div className="p-3 bg-sky-50 border border-sky-200 rounded-lg text-xs text-sky-800 flex items-start gap-2">
            <Mail className="w-4 h-4 text-sky-600 shrink-0 mt-0.5" />
            <p>
              <strong>Automated Client Portal Onboarding:</strong> Providing an email address will automatically send a secure portal invitation email with a link for the client to set up their password for the first time.
            </p>
          </div>

          <div className="pt-4 flex items-center justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Save Client
            </Button>
          </div>
        </form>
      </Modal>

      {/* Client 360° Drawer */}
      <Drawer
        isOpen={!!selectedClient}
        onClose={() => {
          setSelectedClient(null);
          setDrawerTab('overview');
        }}
        title={selectedClient?.displayName}
        subtitle={`Client PAN: ${selectedClient?.pan} • ${selectedClient?.clientType}`}
      >
        {selectedClient && (
          <div className="space-y-6">
            {/* Drawer Tab Switcher */}
            <div className="flex border-b border-slate-200">
              <button
                onClick={() => setDrawerTab('overview')}
                className={clsx(
                  'px-4 py-2 text-xs font-bold border-b-2 transition-all',
                  drawerTab === 'overview'
                    ? 'border-emerald-600 text-emerald-800 bg-emerald-50/50'
                    : 'border-transparent text-slate-500 hover:text-slate-700'
                )}
              >
                Overview & Status
              </button>
              <button
                onClick={() => setDrawerTab('doc_requests')}
                className={clsx(
                  'px-4 py-2 text-xs font-bold border-b-2 transition-all flex items-center gap-1.5',
                  drawerTab === 'doc_requests'
                    ? 'border-emerald-600 text-emerald-800 bg-emerald-50/50'
                    : 'border-transparent text-slate-500 hover:text-slate-700'
                )}
              >
                <span>Document Requests</span>
                <span className="bg-emerald-100 text-emerald-800 text-[10px] font-extrabold px-1.5 py-0.2 rounded-full">
                  V1
                </span>
              </button>
            </div>

            {drawerTab === 'doc_requests' ? (
              <ClientDocumentRequestsTab
                clientId={selectedClient.id}
                clientName={selectedClient.displayName}
              />
            ) : (
              <div className="space-y-6">
                {/* Lifecycle Status Management Card */}
                <div className="p-4 rounded-xl border border-slate-200 bg-white shadow-xs space-y-3">
                  <div className="flex items-center justify-between">
                    <div>
                      <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Account Lifecycle Status</h4>
                      <p className="text-[11px] text-slate-500 mt-0.5">Control practice filing access & account state</p>
                    </div>
                    <StatusBadge status={selectedClient.status} size="md" />
                  </div>

              {/* Status Actions Bar */}
              <div className="pt-3 border-t border-slate-100 flex flex-wrap items-center gap-2">
                {selectedClient.status !== 'ACTIVE' && (
                  <Button
                    size="sm"
                    variant="primary"
                    leftIcon={<CheckCircle2 className="w-3.5 h-3.5" />}
                    isLoading={statusUpdatingId === selectedClient.id}
                    onClick={() => handleUpdateStatus(selectedClient.id, 'ACTIVE')}
                  >
                    Set as Active
                  </Button>
                )}

                {selectedClient.status !== 'INACTIVE' && (
                  <Button
                    size="sm"
                    variant="outline"
                    leftIcon={<PauseCircle className="w-3.5 h-3.5 text-slate-500" />}
                    isLoading={statusUpdatingId === selectedClient.id}
                    onClick={() => handleUpdateStatus(selectedClient.id, 'INACTIVE')}
                  >
                    Deactivate Client
                  </Button>
                )}

                {selectedClient.status !== 'SUSPENDED' && (
                  <button
                    disabled={statusUpdatingId === selectedClient.id}
                    onClick={() => handleUpdateStatus(selectedClient.id, 'SUSPENDED')}
                    className="px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-lg text-xs font-bold inline-flex items-center gap-1.5 transition-colors disabled:opacity-50"
                  >
                    <Ban className="w-3.5 h-3.5 text-rose-600" />
                    <span>Suspend Account</span>
                  </button>
                )}

                {selectedClient.status !== 'ARCHIVED' && (
                  <Button
                    size="sm"
                    variant="outline"
                    leftIcon={<Archive className="w-3.5 h-3.5 text-slate-400" />}
                    isLoading={statusUpdatingId === selectedClient.id}
                    onClick={() => handleUpdateStatus(selectedClient.id, 'ARCHIVED')}
                  >
                    Archive Record
                  </Button>
                )}
              </div>
            </div>

            {/* Profile Overview Card */}
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 space-y-2.5 text-xs">
              <div className="flex justify-between">
                <span className="text-slate-500">PAN Card:</span>
                <span className="font-mono font-bold text-slate-800">{selectedClient.pan}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">GSTIN:</span>
                <span className="font-mono font-bold text-slate-800">{selectedClient.gstin || 'None'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Email:</span>
                <span className="font-medium text-slate-800">{selectedClient.email || '—'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Phone:</span>
                <span className="font-medium text-slate-800">{selectedClient.phone || '—'}</span>
              </div>
            </div>

            {/* Client Portal Access Card */}
            <div className="p-4 rounded-xl border border-sky-200 bg-sky-50/50 space-y-3 text-xs">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Mail className="w-4 h-4 text-sky-600" />
                  <span className="font-bold text-slate-900">Client Portal Access</span>
                </div>
                {((selectedClient as any).portalStatus === 'ACTIVE') ? (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800">
                    <CheckCircle2 className="w-3 h-3" /> Active
                  </span>
                ) : ((selectedClient as any).portalStatus === 'INVITED') ? (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800">
                    <Clock className="w-3 h-3" /> Pending Activation
                  </span>
                ) : ((selectedClient as any).portalStatus === 'SUSPENDED') ? (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800">
                    <Ban className="w-3 h-3" /> Suspended
                  </span>
                ) : ((selectedClient as any).portalStatus === 'INACTIVE') ? (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700">
                    <PauseCircle className="w-3 h-3" /> Inactive
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700">
                    Not Set Up
                  </span>
                )}
              </div>
              <p className="text-[11px] text-slate-600">
                {selectedClient.email
                  ? `Primary contact email (${selectedClient.email}) is linked to portal access.`
                  : 'Add an email address to this client to enable self-service portal access.'}
              </p>
              {selectedClient.email && (
                <div className="flex flex-wrap items-center gap-2 pt-1">
                  {(selectedClient as any).portalStatus === 'ACTIVE' && (
                    <>
                      <Button
                        size="sm"
                        variant="outline"
                        leftIcon={<Ban className="w-3.5 h-3.5 text-rose-600" />}
                        onClick={() => setPortalModalAction({ type: 'SUSPEND', client: selectedClient })}
                        className="bg-white border-rose-200 text-rose-700 hover:bg-rose-50"
                      >
                        Suspend Access
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        leftIcon={<PauseCircle className="w-3.5 h-3.5 text-slate-500" />}
                        onClick={() => setPortalModalAction({ type: 'DEACTIVATE', client: selectedClient })}
                        className="bg-white border-slate-200 text-slate-700 hover:bg-slate-50"
                      >
                        Disable Access
                      </Button>
                    </>
                  )}

                  {(selectedClient as any).portalStatus === 'SUSPENDED' && (
                    <>
                      <Button
                        size="sm"
                        variant="primary"
                        leftIcon={<RotateCcw className="w-3.5 h-3.5" />}
                        onClick={() => setPortalModalAction({ type: 'RESTORE', client: selectedClient })}
                      >
                        Restore Access
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        leftIcon={<PauseCircle className="w-3.5 h-3.5 text-slate-500" />}
                        onClick={() => setPortalModalAction({ type: 'DEACTIVATE', client: selectedClient })}
                        className="bg-white border-slate-200 text-slate-700 hover:bg-slate-50"
                      >
                        Disable Access
                      </Button>
                    </>
                  )}

                  {(selectedClient as any).portalStatus === 'INACTIVE' && (
                    <Button
                      size="sm"
                      variant="primary"
                      leftIcon={<RotateCcw className="w-3.5 h-3.5" />}
                      onClick={() => setPortalModalAction({ type: 'RESTORE', client: selectedClient })}
                    >
                      Restore Access
                    </Button>
                  )}

                  {(selectedClient as any).portalStatus === 'INVITED' && (
                    <>
                      <Button
                        size="sm"
                        variant="outline"
                        leftIcon={<Send className="w-3.5 h-3.5 text-sky-600" />}
                        isLoading={resendingId === selectedClient.id}
                        onClick={() => handleResendPortalInvite(selectedClient.id)}
                        className="bg-white border-sky-300 text-sky-700 hover:bg-sky-50"
                      >
                        Resend Activation Invite
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        leftIcon={<PauseCircle className="w-3.5 h-3.5 text-slate-500" />}
                        onClick={() => setPortalModalAction({ type: 'DEACTIVATE', client: selectedClient })}
                        className="bg-white border-slate-200 text-slate-700 hover:bg-slate-50"
                      >
                        Disable Access
                      </Button>
                    </>
                  )}
                </div>
              )}
            </div>

            {/* Practice Modules Grid */}
            <div className="border-t border-slate-200 pt-4">
              <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-3">Client Practice Modules</h4>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                <div className="p-3 border border-slate-200 rounded-lg hover:border-brand-500 cursor-pointer bg-white">
                  <p className="font-bold text-slate-800">GST Profiles</p>
                  <p className="text-[10px] text-slate-400 mt-0.5">Manage GSTIN state filings</p>
                </div>
                <div className="p-3 border border-slate-200 rounded-lg hover:border-brand-500 cursor-pointer bg-white">
                  <p className="font-bold text-slate-800">ITR Returns</p>
                  <p className="text-[10px] text-slate-400 mt-0.5">AY historical returns</p>
                </div>
                <div className="p-3 border border-slate-200 rounded-lg hover:border-brand-500 cursor-pointer bg-white">
                  <p className="font-bold text-slate-800">Document Vault</p>
                  <p className="text-[10px] text-slate-400 mt-0.5">Financials & 26AS</p>
                </div>
                <div className="p-3 border border-slate-200 rounded-lg hover:border-brand-500 cursor-pointer bg-white">
                  <p className="font-bold text-slate-800">Invoices & Fees</p>
                  <p className="text-[10px] text-slate-400 mt-0.5">Ledgers & outstanding</p>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    )}
  </Drawer>

  {/* Portal Action Confirmation Modal */}
  <Modal
    isOpen={!!portalModalAction}
    onClose={() => setPortalModalAction(null)}
    title={
      portalModalAction?.type === 'SUSPEND'
        ? 'Suspend Client Portal Access'
        : portalModalAction?.type === 'RESTORE'
        ? 'Restore Client Portal Access'
        : portalModalAction?.type === 'DEACTIVATE'
        ? 'Disable Client Portal Access'
        : 'Resend Portal Activation Invite'
    }
  >
    <div className="space-y-4 text-xs text-slate-600">
      <div className="flex items-start gap-3 p-3 rounded-lg bg-slate-50 border border-slate-200">
        {portalModalAction?.type === 'SUSPEND' && (
          <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
        )}
        {portalModalAction?.type === 'RESTORE' && (
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
        )}
        {portalModalAction?.type === 'DEACTIVATE' && (
          <ShieldAlert className="w-5 h-5 text-slate-600 shrink-0 mt-0.5" />
        )}
        {portalModalAction?.type === 'RESEND' && (
          <Mail className="w-5 h-5 text-sky-600 shrink-0 mt-0.5" />
        )}
        <div>
          <p className="font-bold text-slate-800 text-sm mb-1">
            {portalModalAction?.client.displayName}
          </p>
          <p className="text-slate-600">
            {portalModalAction?.type === 'SUSPEND' &&
              "Are you sure you want to suspend this client's portal access? The client will no longer be able to sign in."}
            {portalModalAction?.type === 'RESTORE' &&
              "Are you sure you want to restore this client's portal access? The client will be able to sign in using their existing password."}
            {portalModalAction?.type === 'DEACTIVATE' &&
              "Are you sure you want to disable this client's portal access? The client data will be retained."}
            {portalModalAction?.type === 'RESEND' &&
              'Resend portal activation invitation to this client?'}
          </p>
        </div>
      </div>

      <div className="pt-2 flex items-center justify-end gap-2">
        <Button
          type="button"
          variant="outline"
          onClick={() => setPortalModalAction(null)}
          disabled={!!statusUpdatingId || !!resendingId}
        >
          Cancel
        </Button>
        {portalModalAction?.type === 'SUSPEND' && (
          <Button
            type="button"
            isLoading={statusUpdatingId === portalModalAction.client.id}
            onClick={() => handleUpdatePortalStatus(portalModalAction.client.id, 'SUSPENDED')}
            className="bg-rose-600 hover:bg-rose-700 text-white"
          >
            Suspend Access
          </Button>
        )}
        {portalModalAction?.type === 'RESTORE' && (
          <Button
            type="button"
            isLoading={statusUpdatingId === portalModalAction.client.id}
            onClick={() => handleUpdatePortalStatus(portalModalAction.client.id, 'ACTIVE')}
            className="bg-emerald-600 hover:bg-emerald-700 text-white"
          >
            Restore Access
          </Button>
        )}
        {portalModalAction?.type === 'DEACTIVATE' && (
          <Button
            type="button"
            isLoading={statusUpdatingId === portalModalAction.client.id}
            onClick={() => handleUpdatePortalStatus(portalModalAction.client.id, 'INACTIVE')}
            className="bg-slate-700 hover:bg-slate-800 text-white"
          >
            Disable Access
          </Button>
        )}
        {portalModalAction?.type === 'RESEND' && (
          <Button
            type="button"
            isLoading={resendingId === portalModalAction.client.id}
            onClick={() => handleResendPortalInvite(portalModalAction.client.id)}
            className="bg-sky-600 hover:bg-sky-700 text-white"
          >
            Resend Invitation
          </Button>
        )}
      </div>
    </div>
  </Modal>
</div>
);
};
