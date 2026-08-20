import React, { useState, useEffect } from 'react';
import { Plus, Eye, Edit2, Trash2, Building2, User } from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { Drawer } from '../components/common/Drawer';
import { clientApi } from '../api/endpoints';
import { Client } from '../types';

export const ClientsPage: React.FC = () => {
  const [clients, setClients] = useState<Client[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedClient, setSelectedClient] = useState<Client | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

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
  }, [page, pageSize]);

  const loadClients = async () => {
    try {
      setIsLoading(true);
      const res = await clientApi.getAll({ page, size: pageSize });
      setClients(res.content);
      setTotalElements(res.totalElements);
    } catch (err) {
      console.error('Failed to load clients', err);
    } finally {
      setIsLoading(false);
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
      if (formData.legalName?.trim()) payload.legalName = formData.legalName.trim();
      if (formData.gstin?.trim()) payload.gstin = formData.gstin.trim().toUpperCase();
      if (formData.email?.trim()) payload.email = formData.email.trim();
      if (formData.phone?.trim()) payload.phone = formData.phone.trim();

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
      const resp = err.response?.data;
      if (resp?.validationErrors && Array.isArray(resp.validationErrors)) {
        const backendFieldErrors: Record<string, string> = {};
        resp.validationErrors.forEach((vErr: { field: string; message: string }) => {
          backendFieldErrors[vErr.field] = vErr.message;
        });
        setFieldErrors(backendFieldErrors);
        setGeneralError('Please resolve the highlighted field errors below.');
      } else {
        setGeneralError(resp?.message || 'Failed to create client.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const columns: Column<Client>[] = [
    {
      header: 'Client / Business Name',
      accessor: (row) => (
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-700 font-bold text-xs shrink-0">
            {row.clientType === 'INDIVIDUAL' ? <User className="w-4 h-4 text-blue-600" /> : <Building2 className="w-4 h-4 text-slate-700" />}
          </div>
          <div>
            <span className="font-bold text-slate-900 block hover:text-brand-600 transition-colors">
              {row.displayName}
            </span>
            <span className="text-[10px] text-slate-400 block uppercase tracking-wider">
              {row.clientType.replace('_', ' ')}
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
      header: 'Compliance Status',
      accessor: (row) => <StatusBadge status={row.status} size="sm" />,
      align: 'center',
    },
    {
      header: 'Actions',
      align: 'right',
      cell: (row) => (
        <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
          <button
            onClick={() => setSelectedClient(row)}
            className="p-1.5 text-slate-500 hover:text-brand-600 hover:bg-slate-100 rounded-md transition-colors"
            title="View 360° Profile"
          >
            <Eye className="w-4 h-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">Clients Directory</h1>
          <p className="text-xs text-slate-500 mt-1">
            Centralized repository for corporate and individual clients, GSTINs, PANs, and filing histories.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
          Add New Client
        </Button>
      </div>

      {/* Data Table */}
      <DataTable
        columns={columns}
        data={clients}
        totalElements={totalElements}
        pageSize={pageSize}
        pageNumber={page}
        onPageChange={setPage}
        onPageSizeChange={setPageSize}
        isLoading={isLoading}
        searchPlaceholder="Search clients by name, PAN, or GSTIN..."
        onRowClick={(row) => setSelectedClient(row)}
      />

      {/* Add Client Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Add New Client"
        subtitle="Create an individual or corporate practice client"
      >
        <form onSubmit={handleCreateClient} className="space-y-4">
          {generalError && (
            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs font-semibold text-rose-700">
              {generalError}
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Display Name *</label>
            <input
              type="text"
              required
              placeholder="e.g. Apex Global Solutions Pvt Ltd"
              value={formData.displayName}
              onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
              className={`w-full text-xs px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 ${
                fieldErrors.displayName ? 'border-rose-400 bg-rose-50/20 focus:ring-rose-500/20' : 'border-slate-200 focus:ring-brand-500'
              }`}
            />
            {fieldErrors.displayName && (
              <p className="text-rose-600 text-[11px] font-medium mt-1">{fieldErrors.displayName}</p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">PAN Number *</label>
              <input
                type="text"
                required
                maxLength={10}
                placeholder="ABCDE1234F"
                value={formData.pan}
                onChange={(e) => setFormData({ ...formData, pan: e.target.value.toUpperCase() })}
                className={`w-full font-mono text-xs px-3 py-2 border rounded-lg uppercase focus:outline-none focus:ring-2 ${
                  fieldErrors.pan ? 'border-rose-400 bg-rose-50/20 focus:ring-rose-500/20' : 'border-slate-200 focus:ring-brand-500'
                }`}
              />
              {fieldErrors.pan && (
                <p className="text-rose-600 text-[11px] font-medium mt-1">{fieldErrors.pan}</p>
              )}
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Client Type *</label>
              <select
                value={formData.clientType}
                onChange={(e) => setFormData({ ...formData, clientType: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
              >
                <option value="PRIVATE_LIMITED">Private Limited</option>
                <option value="PUBLIC_LIMITED">Public Limited</option>
                <option value="LLP">LLP</option>
                <option value="PROPRIETORSHIP">Proprietorship</option>
                <option value="INDIVIDUAL">Individual</option>
                <option value="HUF">HUF</option>
                <option value="TRUST">Trust</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Primary GSTIN (Optional)</label>
              <input
                type="text"
                maxLength={15}
                placeholder="27ABCDE1234F1Z5"
                value={formData.gstin}
                onChange={(e) => setFormData({ ...formData, gstin: e.target.value.toUpperCase() })}
                className={`w-full font-mono text-xs px-3 py-2 border rounded-lg uppercase focus:outline-none focus:ring-2 ${
                  fieldErrors.gstin ? 'border-rose-400 bg-rose-50/20 focus:ring-rose-500/20' : 'border-slate-200 focus:ring-brand-500'
                }`}
              />
              {fieldErrors.gstin && (
                <p className="text-rose-600 text-[11px] font-medium mt-1">{fieldErrors.gstin}</p>
              )}
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Contact Email</label>
              <input
                type="email"
                placeholder="accounts@client.com"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
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
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 space-y-2 text-xs">
              <div className="flex justify-between">
                <span className="text-slate-500">Compliance Status:</span>
                <StatusBadge status={selectedClient.status} size="sm" />
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">PAN:</span>
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
            </div>

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
