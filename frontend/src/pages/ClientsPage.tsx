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
} from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { Drawer } from '../components/common/Drawer';
import { clientApi } from '../api/endpoints';
import { Client } from '../types';
import clsx from 'clsx';

export const ClientsPage: React.FC = () => {
  const [clients, setClients] = useState<Client[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedClient, setSelectedClient] = useState<Client | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [statusUpdatingId, setStatusUpdatingId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'ARCHIVED'>('ALL');

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
  }, [page, pageSize, statusFilter]);

  const loadClients = async () => {
    try {
      setIsLoading(true);
      const params: any = { page, size: pageSize };
      if (statusFilter !== 'ALL') {
        params.status = statusFilter;
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
        prev.map((c) => (c.id === clientId ? { ...c, status: newStatus } : c))
      );
      if (selectedClient && selectedClient.id === clientId) {
        setSelectedClient({ ...selectedClient, status: newStatus });
      }
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update client status');
    } finally {
      setStatusUpdatingId(null);
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
      header: 'Lifecycle Status',
      accessor: (row) => <StatusBadge status={row.status} size="sm" />,
      align: 'center',
    },
    {
      header: 'Quick Status Actions',
      align: 'right',
      cell: (row) => {
        const isUpdating = statusUpdatingId === row.id;

        return (
          <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
            {/* Quick Status State Buttons */}
            {row.status === 'ACTIVE' && (
              <>
                <button
                  disabled={isUpdating}
                  onClick={() => handleUpdateStatus(row.id, 'INACTIVE')}
                  className="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded text-[11px] font-semibold inline-flex items-center gap-1 transition-colors disabled:opacity-50"
                  title="Deactivate client (Mark Inactive)"
                >
                  <PauseCircle className="w-3 h-3 text-slate-500" />
                  <span>Deactivate</span>
                </button>

                <button
                  disabled={isUpdating}
                  onClick={() => handleUpdateStatus(row.id, 'SUSPENDED')}
                  className="px-2 py-1 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded text-[11px] font-semibold inline-flex items-center gap-1 transition-colors disabled:opacity-50"
                  title="Suspend client account"
                >
                  <Ban className="w-3 h-3 text-rose-600" />
                  <span>Suspend</span>
                </button>
              </>
            )}

            {row.status === 'INACTIVE' && (
              <>
                <button
                  disabled={isUpdating}
                  onClick={() => handleUpdateStatus(row.id, 'ACTIVE')}
                  className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded text-[11px] font-semibold inline-flex items-center gap-1 transition-colors disabled:opacity-50"
                  title="Re-activate client account"
                >
                  <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                  <span>Activate</span>
                </button>

                <button
                  disabled={isUpdating}
                  onClick={() => handleUpdateStatus(row.id, 'SUSPENDED')}
                  className="px-2 py-1 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded text-[11px] font-semibold inline-flex items-center gap-1 transition-colors disabled:opacity-50"
                  title="Suspend client account"
                >
                  <Ban className="w-3 h-3 text-rose-600" />
                  <span>Suspend</span>
                </button>
              </>
            )}

            {row.status === 'SUSPENDED' && (
              <button
                disabled={isUpdating}
                onClick={() => handleUpdateStatus(row.id, 'ACTIVE')}
                className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded text-[11px] font-semibold inline-flex items-center gap-1 transition-colors disabled:opacity-50"
                title="Lift suspension & Re-activate"
              >
                <RotateCcw className="w-3 h-3 text-emerald-600" />
                <span>Lift Suspension</span>
              </button>
            )}

            {row.status === 'ARCHIVED' && (
              <button
                disabled={isUpdating}
                onClick={() => handleUpdateStatus(row.id, 'ACTIVE')}
                className="px-2 py-1 bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-200 rounded text-[11px] font-semibold inline-flex items-center gap-1 transition-colors disabled:opacity-50"
                title="Restore & Activate client"
              >
                <RotateCcw className="w-3 h-3 text-blue-600" />
                <span>Restore</span>
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

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">Clients Directory</h1>
          <p className="text-xs text-slate-500 mt-1">
            Centralized repository for corporate and individual clients, lifecycle management, and status controls.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <Link to="/clients/migration">
            <Button variant="outline" leftIcon={<FileSpreadsheet className="w-4 h-4 text-emerald-600" />}>
              Migrate / Bulk Import
            </Button>
          </Link>
          <Button onClick={() => setIsModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
            Add New Client
          </Button>
        </div>
      </div>

      {/* Lifecycle Status Filter Tabs */}
      <div className="flex items-center gap-1 p-1 bg-slate-100 border border-slate-200 rounded-xl w-fit text-xs font-semibold">
        <button
          onClick={() => setStatusFilter('ALL')}
          className={clsx(
            'px-3 py-1.5 rounded-lg transition-all',
            statusFilter === 'ALL' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
          )}
        >
          All Clients
        </button>
        <button
          onClick={() => setStatusFilter('ACTIVE')}
          className={clsx(
            'px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5',
            statusFilter === 'ACTIVE' ? 'bg-white text-emerald-700 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
          )}
        >
          <span className="w-2 h-2 rounded-full bg-emerald-500" />
          <span>Active</span>
        </button>
        <button
          onClick={() => setStatusFilter('INACTIVE')}
          className={clsx(
            'px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5',
            statusFilter === 'INACTIVE' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
          )}
        >
          <span className="w-2 h-2 rounded-full bg-slate-400" />
          <span>Deactivated</span>
        </button>
        <button
          onClick={() => setStatusFilter('SUSPENDED')}
          className={clsx(
            'px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5',
            statusFilter === 'SUSPENDED' ? 'bg-white text-rose-700 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
          )}
        >
          <span className="w-2 h-2 rounded-full bg-rose-500" />
          <span>Suspended</span>
        </button>
        <button
          onClick={() => setStatusFilter('ARCHIVED')}
          className={clsx(
            'px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5',
            statusFilter === 'ARCHIVED' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
          )}
        >
          <Archive className="w-3.5 h-3.5 text-slate-400" />
          <span>Archived</span>
        </button>
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

          <div className="grid grid-cols-2 gap-3">
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

          <div className="grid grid-cols-2 gap-3">
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

          <div className="grid grid-cols-2 gap-3">
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
        onClose={() => setSelectedClient(null)}
        title={selectedClient?.displayName}
        subtitle={`Client PAN: ${selectedClient?.pan} • ${selectedClient?.clientType}`}
      >
        {selectedClient && (
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

            {/* Practice Modules Grid */}
            <div className="border-t border-slate-200 pt-4">
              <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-3">Client Practice Modules</h4>
              <div className="grid grid-cols-2 gap-2 text-xs">
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
      </Drawer>
    </div>
  );
};
