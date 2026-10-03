import React, { useEffect, useState, useMemo } from 'react';
import {
  ShieldCheck,
  Plus,
  Search,
  RefreshCw,
  AlertTriangle,
  Clock,
  CheckCircle,
  XCircle,
  Edit2,
  Trash2,
  Ban,
  Bell,
  Building2,
  User,
  Filter,
  Calendar,
  Lock,
  Layers,
  Info,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { dscApi, clientApi } from '../api/endpoints';
import {
  DscDto,
  DscStatusType,
  DscCertificateType,
  DscSummaryDto,
  CreateDscRequest,
  UpdateDscRequest,
  Client,
} from '../types';
import { useAuth } from '../context/AuthContext';

const CERTIFICATE_TYPES: { value: DscCertificateType; label: string }[] = [
  { value: 'CLASS_3', label: 'Class 3 (Standard for GST / MCA / ITR)' },
  { value: 'CLASS_2', label: 'Class 2 (Legacy / Compatibility)' },
  { value: 'DGFT', label: 'DGFT (Import / Export Code)' },
  { value: 'OTHER', label: 'Other Custom Certificate' },
];

const COMMON_ISSUERS = ['eMudhra', 'VSign', 'Capricorn', 'Pantasign', 'NSDL', 'Sify', 'Other'];

const COMMON_SERVICES = [
  { id: 'GST', label: 'GST Compliance (GSTR-1, GSTR-3B)' },
  { id: 'ITR', label: 'Income Tax (ITR, Audit 3CD)' },
  { id: 'TDS', label: 'TDS (Form 24Q, 26Q, 27Q)' },
  { id: 'MCA', label: 'MCA / ROC (Company Filings)' },
  { id: 'PF_ESI', label: 'EPFO & ESIC Returns' },
  { id: 'CUSTOMS', label: 'Customs & ICEGATE' },
];

export const DscRegisterPage: React.FC = () => {
  const { user } = useAuth();
  const [dscList, setDscList] = useState<DscDto[]>([]);
  const [summary, setSummary] = useState<DscSummaryDto | null>(null);
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Filters
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [clientFilter, setClientFilter] = useState<string>('ALL');
  const [certTypeFilter, setCertTypeFilter] = useState<string>('ALL');
  const [serviceFilter, setServiceFilter] = useState<string>('ALL');

  // Add / Edit Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingDsc, setEditingDsc] = useState<DscDto | null>(null);
  const [holderName, setHolderName] = useState('');
  const [selectedClientId, setSelectedClientId] = useState<string>('');
  const [certificateIdentifier, setCertificateIdentifier] = useState('');
  const [certificateType, setCertificateType] = useState<DscCertificateType>('CLASS_3');
  const [issuer, setIssuer] = useState('eMudhra');
  const [customIssuer, setCustomIssuer] = useState('');
  const [issuedDate, setIssuedDate] = useState('');
  const [expiryDate, setExpiryDate] = useState('');
  const [selectedServices, setSelectedServices] = useState<string[]>(['GST', 'ITR', 'TDS']);
  const [notes, setNotes] = useState('');
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  // Revoke Modal
  const [isRevokeModalOpen, setIsRevokeModalOpen] = useState(false);
  const [revokingDsc, setRevokingDsc] = useState<DscDto | null>(null);
  const [revocationReason, setRevocationReason] = useState('');
  const [processingRevoke, setProcessingRevoke] = useState(false);

  // Delete Modal
  const [dscToDelete, setDscToDelete] = useState<DscDto | null>(null);
  const [deleting, setDeleting] = useState(false);

  // Expiry check
  const [triggeringAlerts, setTriggeringAlerts] = useState(false);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [dscPage, summaryData] = await Promise.all([
        dscApi.getDscList({ size: 100 }),
        dscApi.getDscSummary(),
      ]);
      setDscList(dscPage.content || []);
      setSummary(summaryData);
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to load DSC register');
    } finally {
      setLoading(false);
    }
  };

  const loadClients = async () => {
    try {
      const res = await clientApi.getAll({ size: 200 });
      setClients(res.content || []);
    } catch {
      // client load failure is non-blocking
    }
  };

  useEffect(() => {
    loadData();
    loadClients();
  }, []);

  const openCreateModal = () => {
    setEditingDsc(null);
    setHolderName('');
    setSelectedClientId('');
    setCertificateIdentifier('');
    setCertificateType('CLASS_3');
    setIssuer('eMudhra');
    setCustomIssuer('');
    const todayStr = new Date().toISOString().split('T')[0];
    const twoYearsLater = new Date();
    twoYearsLater.setFullYear(twoYearsLater.getFullYear() + 2);
    setIssuedDate(todayStr);
    setExpiryDate(twoYearsLater.toISOString().split('T')[0]);
    setSelectedServices(['GST', 'ITR', 'TDS', 'MCA']);
    setNotes('');
    setFormError('');
    setIsModalOpen(true);
  };

  const openEditModal = (dsc: DscDto) => {
    setEditingDsc(dsc);
    setHolderName(dsc.holderName);
    setSelectedClientId(dsc.clientId || '');
    setCertificateIdentifier(dsc.certificateIdentifier || '');
    setCertificateType(dsc.certificateType || 'CLASS_3');
    if (COMMON_ISSUERS.includes(dsc.issuer || '')) {
      setIssuer(dsc.issuer || 'eMudhra');
      setCustomIssuer('');
    } else {
      setIssuer('Other');
      setCustomIssuer(dsc.issuer || '');
    }
    setIssuedDate(dsc.issuedDate);
    setExpiryDate(dsc.expiryDate);
    setSelectedServices(dsc.applicableServices ? dsc.applicableServices.split(',').map((s) => s.trim()) : []);
    setNotes(dsc.notes || '');
    setFormError('');
    setIsModalOpen(true);
  };

  const handleSaveDsc = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError('');

    if (!holderName.trim()) {
      setFormError('Holder Name is required.');
      return;
    }
    if (!issuedDate || !expiryDate) {
      setFormError('Both Issue Date and Expiry Date are required.');
      return;
    }
    if (new Date(issuedDate) > new Date(expiryDate)) {
      setFormError('Issue Date cannot be later than Expiry Date.');
      return;
    }

    setSaving(true);
    const resolvedIssuer = issuer === 'Other' ? customIssuer.trim() : issuer;
    const servicesStr = selectedServices.join(', ');

    try {
      if (editingDsc) {
        const payload: UpdateDscRequest = {
          clientId: selectedClientId ? selectedClientId : null,
          holderName: holderName.trim(),
          certificateIdentifier: certificateIdentifier.trim() || null,
          certificateType,
          issuer: resolvedIssuer || null,
          issuedDate,
          expiryDate,
          applicableServices: servicesStr || null,
          notes: notes.trim() || null,
        };
        await dscApi.updateDsc(editingDsc.id, payload);
        setSuccessMessage(`Updated DSC for ${holderName.trim()} successfully.`);
      } else {
        const payload: CreateDscRequest = {
          clientId: selectedClientId ? selectedClientId : null,
          holderName: holderName.trim(),
          certificateIdentifier: certificateIdentifier.trim() || null,
          certificateType,
          issuer: resolvedIssuer || null,
          issuedDate,
          expiryDate,
          applicableServices: servicesStr || null,
          notes: notes.trim() || null,
        };
        await dscApi.createDsc(payload);
        setSuccessMessage(`Registered DSC for ${holderName.trim()} successfully.`);
      }
      setIsModalOpen(false);
      await loadData();
    } catch (err: any) {
      setFormError(err?.response?.data?.message || err?.message || 'Failed to save DSC.');
    } finally {
      setSaving(false);
    }
  };

  const handleToggleActive = async (dsc: DscDto) => {
    try {
      if (dsc.status === 'INACTIVE') {
        await dscApi.activateDsc(dsc.id);
        setSuccessMessage(`Activated DSC for ${dsc.holderName}.`);
      } else {
        await dscApi.deactivateDsc(dsc.id);
        setSuccessMessage(`Deactivated DSC for ${dsc.holderName}.`);
      }
      await loadData();
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to update DSC status.');
    }
  };

  const openRevokeModal = (dsc: DscDto) => {
    setRevokingDsc(dsc);
    setRevocationReason('');
    setIsRevokeModalOpen(true);
  };

  const handleRevokeDsc = async () => {
    if (!revokingDsc) return;
    setProcessingRevoke(true);
    try {
      await dscApi.revokeDsc(revokingDsc.id, revocationReason.trim() || undefined);
      setSuccessMessage(`Revoked DSC for ${revokingDsc.holderName}.`);
      setIsRevokeModalOpen(false);
      setRevokingDsc(null);
      await loadData();
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to revoke DSC.');
    } finally {
      setProcessingRevoke(false);
    }
  };

  const handleDeleteDsc = async () => {
    if (!dscToDelete) return;
    setDeleting(true);
    try {
      await dscApi.deleteDsc(dscToDelete.id);
      setSuccessMessage(`Deleted DSC record for ${dscToDelete.holderName}.`);
      setDscToDelete(null);
      await loadData();
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to delete DSC.');
    } finally {
      setDeleting(false);
    }
  };

  const handleTriggerAlerts = async () => {
    setTriggeringAlerts(true);
    try {
      const dispatched = await dscApi.triggerExpiryReminders();
      setSuccessMessage(`Dispatched ${dispatched} DSC expiry notification alert(s) to team.`);
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to trigger expiry reminders.');
    } finally {
      setTriggeringAlerts(false);
    }
  };

  const toggleServiceSelection = (serviceId: string) => {
    setSelectedServices((prev) =>
      prev.includes(serviceId) ? prev.filter((s) => s !== serviceId) : [...prev, serviceId]
    );
  };

  // Filtered DSC list
  const filteredList = useMemo(() => {
    return dscList.filter((dsc) => {
      // Search
      if (search.trim()) {
        const term = search.toLowerCase();
        const matchesHolder = dsc.holderName.toLowerCase().includes(term);
        const matchesCertId = dsc.certificateIdentifier?.toLowerCase().includes(term);
        const matchesIssuer = dsc.issuer?.toLowerCase().includes(term);
        const matchesClient = dsc.clientName?.toLowerCase().includes(term);
        if (!matchesHolder && !matchesCertId && !matchesIssuer && !matchesClient) {
          return false;
        }
      }

      // Status
      if (statusFilter !== 'ALL' && dsc.status !== statusFilter) {
        return false;
      }

      // Client
      if (clientFilter !== 'ALL') {
        if (clientFilter === 'PRACTICE_ONLY' && dsc.clientId) return false;
        if (clientFilter !== 'PRACTICE_ONLY' && dsc.clientId !== clientFilter) return false;
      }

      // Certificate Type
      if (certTypeFilter !== 'ALL' && dsc.certificateType !== certTypeFilter) {
        return false;
      }

      // Service
      if (serviceFilter !== 'ALL') {
        if (!dsc.applicableServices?.toUpperCase().includes(serviceFilter.toUpperCase())) {
          return false;
        }
      }

      return true;
    });
  }, [dscList, search, statusFilter, clientFilter, certTypeFilter, serviceFilter]);

  const getStatusBadge = (status: DscStatusType, daysLeft?: number | null) => {
    switch (status) {
      case 'ACTIVE':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <CheckCircle className="w-3.5 h-3.5" />
            Active
          </span>
        );
      case 'EXPIRING':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-300">
            <Clock className="w-3.5 h-3.5 text-amber-600 animate-pulse" />
            Expiring ({daysLeft != null ? `${daysLeft}d left` : 'Soon'})
          </span>
        );
      case 'EXPIRED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200">
            <AlertTriangle className="w-3.5 h-3.5" />
            Expired
          </span>
        );
      case 'REVOKED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-purple-50 text-purple-700 border border-purple-200">
            <Ban className="w-3.5 h-3.5" />
            Revoked
          </span>
        );
      case 'INACTIVE':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-600 border border-slate-200">
            <XCircle className="w-3.5 h-3.5" />
            Inactive
          </span>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200 shadow-xs">
        <div>
          <div className="flex items-center gap-2">
            <div className="w-10 h-10 rounded-xl bg-teal-50 border border-teal-200 flex items-center justify-center text-teal-600">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-slate-900">DSC Register & Lifecycle</h1>
              <p className="text-xs text-slate-500">
                Track, manage, and monitor practice and client Digital Signature Certificates with automated expiry alerts.
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            onClick={handleTriggerAlerts}
            disabled={triggeringAlerts}
            className="flex items-center gap-2 text-xs font-semibold text-slate-700"
          >
            <Bell className={`w-4 h-4 ${triggeringAlerts ? 'animate-spin' : 'text-amber-500'}`} />
            {triggeringAlerts ? 'Checking Alerts...' : 'Check Expiry Alerts'}
          </Button>

          <Button
            onClick={openCreateModal}
            className="flex items-center gap-2 text-xs font-semibold bg-teal-600 hover:bg-teal-700 text-white shadow-xs"
          >
            <Plus className="w-4 h-4" />
            Register DSC
          </Button>
        </div>
      </div>

      {/* Messages */}
      {successMessage && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs text-emerald-800">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-4 h-4 text-emerald-600" />
            <span>{successMessage}</span>
          </div>
          <button onClick={() => setSuccessMessage('')} className="text-emerald-600 hover:text-emerald-900 font-bold">
            ×
          </button>
        </div>
      )}

      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 flex items-center justify-between text-xs text-rose-800">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-rose-600" />
            <span>{error}</span>
          </div>
          <button onClick={() => setError('')} className="text-rose-600 hover:text-rose-900 font-bold">
            ×
          </button>
        </div>
      )}

      {/* Metrics Summary Cards */}
      {summary && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
            <div className="text-xs font-medium text-slate-500">Total Registered</div>
            <div className="text-2xl font-bold text-slate-900 mt-1">{summary.total}</div>
            <div className="text-[11px] text-slate-400 mt-0.5">Certificates on file</div>
          </div>

          <div className="bg-white p-4 rounded-xl border border-emerald-200 bg-emerald-50/20 shadow-xs">
            <div className="text-xs font-medium text-emerald-700">Active & Ready</div>
            <div className="text-2xl font-bold text-emerald-700 mt-1">{summary.active}</div>
            <div className="text-[11px] text-emerald-600/80 mt-0.5">Valid &gt; 30 days</div>
          </div>

          <div className="bg-white p-4 rounded-xl border border-amber-300 bg-amber-50/30 shadow-xs">
            <div className="text-xs font-medium text-amber-700 flex items-center gap-1">
              <Clock className="w-3.5 h-3.5 text-amber-600" />
              Expiring Soon
            </div>
            <div className="text-2xl font-bold text-amber-800 mt-1">{summary.expiringSoon}</div>
            <div className="text-[11px] text-amber-700/80 mt-0.5">Within 30 days</div>
          </div>

          <div className="bg-white p-4 rounded-xl border border-rose-200 bg-rose-50/20 shadow-xs">
            <div className="text-xs font-medium text-rose-700">Expired</div>
            <div className="text-2xl font-bold text-rose-700 mt-1">{summary.expired}</div>
            <div className="text-[11px] text-rose-600/80 mt-0.5">Needs renewal</div>
          </div>

          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
            <div className="text-xs font-medium text-slate-500">Revoked / Inactive</div>
            <div className="text-2xl font-bold text-slate-700 mt-1">{summary.revoked + summary.inactive}</div>
            <div className="text-[11px] text-slate-400 mt-0.5">
              {summary.revoked} revoked · {summary.inactive} inactive
            </div>
          </div>
        </div>
      )}

      {/* Filter and Control Bar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-3">
        <div className="flex flex-wrap items-center gap-3">
          {/* Search Box */}
          <div className="relative flex-1 min-w-[240px]">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by holder name, serial identifier, or issuer..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-800 placeholder-slate-400 focus:outline-hidden focus:ring-2 focus:ring-teal-500 focus:bg-white"
            />
          </div>

          {/* Status Filter */}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs font-medium text-slate-700 focus:outline-hidden focus:ring-2 focus:ring-teal-500"
          >
            <option value="ALL">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="EXPIRING">Expiring Soon (≤30d)</option>
            <option value="EXPIRED">Expired</option>
            <option value="REVOKED">Revoked</option>
            <option value="INACTIVE">Inactive</option>
          </select>

          {/* Client Filter */}
          <select
            value={clientFilter}
            onChange={(e) => setClientFilter(e.target.value)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs font-medium text-slate-700 focus:outline-hidden focus:ring-2 focus:ring-teal-500 max-w-[200px]"
          >
            <option value="ALL">All Associated Entities</option>
            <option value="PRACTICE_ONLY">Practice / Firm Only</option>
            {clients.map((c) => (
              <option key={c.id} value={c.id}>
                {c.displayName}
              </option>
            ))}
          </select>

          {/* Certificate Type */}
          <select
            value={certTypeFilter}
            onChange={(e) => setCertTypeFilter(e.target.value)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs font-medium text-slate-700 focus:outline-hidden focus:ring-2 focus:ring-teal-500"
          >
            <option value="ALL">All Types</option>
            <option value="CLASS_3">Class 3</option>
            <option value="CLASS_2">Class 2</option>
            <option value="DGFT">DGFT</option>
            <option value="OTHER">Other</option>
          </select>

          {/* Refresh Button */}
          <Button
            variant="ghost"
            onClick={loadData}
            disabled={loading}
            className="p-2 text-slate-500 hover:text-slate-800"
            title="Refresh register"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </Button>
        </div>
      </div>

      {/* DSC Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50/75 text-[11px] font-semibold text-slate-600 uppercase tracking-wider">
                <th className="py-3 px-4">DSC Holder & Serial</th>
                <th className="py-3 px-4">Entity / Client</th>
                <th className="py-3 px-4">Type & Issuer</th>
                <th className="py-3 px-4">Validity Range</th>
                <th className="py-3 px-4">Status & Expiry</th>
                <th className="py-3 px-4">Applicable Services</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs text-slate-700">
              {loading ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-slate-400">
                    <div className="inline-flex items-center gap-2">
                      <RefreshCw className="w-4 h-4 animate-spin text-teal-600" />
                      <span>Loading DSC register...</span>
                    </div>
                  </td>
                </tr>
              ) : filteredList.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-slate-400">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <ShieldCheck className="w-8 h-8 text-slate-300" />
                      <p className="text-sm font-medium text-slate-600">No Digital Signature Certificates found</p>
                      <p className="text-xs text-slate-400 max-w-sm">
                        {search || statusFilter !== 'ALL' || clientFilter !== 'ALL'
                          ? 'No certificates match the selected filter criteria.'
                          : 'Register DSC metadata for your practice partners and authorized client signatories to track validity.'}
                      </p>
                      <Button onClick={openCreateModal} variant="outline" className="mt-2 text-xs">
                        <Plus className="w-3.5 h-3.5 mr-1" />
                        Register First DSC
                      </Button>
                    </div>
                  </td>
                </tr>
              ) : (
                filteredList.map((dsc) => {
                  return (
                    <tr key={dsc.id} className="hover:bg-slate-50/75 transition-colors">
                      {/* Holder Name & Identifier */}
                      <td className="py-3 px-4">
                        <div className="font-semibold text-slate-900">{dsc.holderName}</div>
                        {dsc.certificateIdentifier ? (
                          <div className="font-mono text-[11px] text-slate-500 mt-0.5 flex items-center gap-1">
                            <Lock className="w-3 h-3 text-slate-400" />
                            {dsc.certificateIdentifier}
                          </div>
                        ) : (
                          <span className="text-[11px] text-slate-400 italic">No serial ID recorded</span>
                        )}
                      </td>

                      {/* Associated Entity */}
                      <td className="py-3 px-4">
                        {dsc.clientId ? (
                          <div>
                            <div className="font-medium text-slate-900 flex items-center gap-1">
                              <Building2 className="w-3.5 h-3.5 text-teal-600" />
                              {dsc.clientName || 'Client'}
                            </div>
                            {dsc.clientPan && (
                              <div className="text-[11px] text-slate-400 font-mono">PAN: {dsc.clientPan}</div>
                            )}
                          </div>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-medium bg-slate-100 text-slate-700 border border-slate-200">
                            <User className="w-3 h-3 text-slate-500" />
                            Practice Firm DSC
                          </span>
                        )}
                      </td>

                      {/* Type & Issuer */}
                      <td className="py-3 px-4">
                        <span className="font-medium text-slate-800">{dsc.certificateType.replace('_', ' ')}</span>
                        <div className="text-[11px] text-slate-500">{dsc.issuer || 'Standard CA'}</div>
                      </td>

                      {/* Validity Range */}
                      <td className="py-3 px-4">
                        <div className="text-slate-800 font-medium">{dsc.expiryDate}</div>
                        <div className="text-[11px] text-slate-400">Issued: {dsc.issuedDate}</div>
                      </td>

                      {/* Status */}
                      <td className="py-3 px-4">{getStatusBadge(dsc.status, dsc.daysUntilExpiry)}</td>

                      {/* Applicable Services */}
                      <td className="py-3 px-4">
                        <div className="flex flex-wrap gap-1 max-w-[200px]">
                          {dsc.applicableServices ? (
                            dsc.applicableServices.split(',').map((s, idx) => (
                              <span
                                key={idx}
                                className="px-1.5 py-0.5 rounded-sm text-[10px] font-semibold bg-slate-100 text-slate-600 border border-slate-200"
                              >
                                {s.trim()}
                              </span>
                            ))
                          ) : (
                            <span className="text-[11px] text-slate-400">All Services</span>
                          )}
                        </div>
                      </td>

                      {/* Actions */}
                      <td className="py-3 px-4 text-right">
                        <div className="inline-flex items-center gap-1">
                          <button
                            onClick={() => openEditModal(dsc)}
                            className="p-1.5 rounded-lg text-slate-500 hover:text-teal-600 hover:bg-teal-50 transition-colors"
                            title="Edit metadata"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>

                          {dsc.status !== 'REVOKED' && (
                            <button
                              onClick={() => handleToggleActive(dsc)}
                              className={`p-1.5 rounded-lg transition-colors ${
                                dsc.status === 'INACTIVE'
                                  ? 'text-emerald-600 hover:bg-emerald-50'
                                  : 'text-slate-500 hover:text-amber-600 hover:bg-amber-50'
                              }`}
                              title={dsc.status === 'INACTIVE' ? 'Activate DSC' : 'Deactivate DSC'}
                            >
                              {dsc.status === 'INACTIVE' ? (
                                <CheckCircle className="w-4 h-4" />
                              ) : (
                                <XCircle className="w-4 h-4" />
                              )}
                            </button>
                          )}

                          {dsc.status !== 'REVOKED' && (
                            <button
                              onClick={() => openRevokeModal(dsc)}
                              className="p-1.5 rounded-lg text-slate-500 hover:text-purple-600 hover:bg-purple-50 transition-colors"
                              title="Revoke certificate"
                            >
                              <Ban className="w-4 h-4" />
                            </button>
                          )}

                          <button
                            onClick={() => setDscToDelete(dsc)}
                            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
                            title="Delete entry"
                          >
                            <Trash2 className="w-4 h-4" />
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
      </div>

      {/* Security Assurance Notice */}
      <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 flex items-start gap-3">
        <Lock className="w-5 h-5 text-slate-400 shrink-0 mt-0.5" />
        <div className="text-xs text-slate-600 space-y-1">
          <p className="font-semibold text-slate-800">Security & Compliance Safeguard</p>
          <p>
            Taxoryn never requests, accepts, or stores private keys, USB token PINs, or cryptographic secrets.
            This register maintains only certificate metadata and lifecycle validity for readiness verification and expiry notifications.
          </p>
        </div>
      </div>

      {/* Add / Edit DSC Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xl max-w-lg w-full overflow-hidden flex flex-col max-h-[90vh]">
            <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-teal-600" />
                <h2 className="text-sm font-bold text-slate-900">
                  {editingDsc ? 'Edit DSC Metadata' : 'Register Digital Signature Certificate'}
                </h2>
              </div>
              <button
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 text-lg font-bold"
              >
                ×
              </button>
            </div>

            <form onSubmit={handleSaveDsc} className="p-6 overflow-y-auto space-y-4">
              {formError && (
                <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-700 flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 shrink-0" />
                  <span>{formError}</span>
                </div>
              )}

              {/* Holder Name */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  DSC Holder / Signatory Name <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Ramesh Chandra Sharma"
                  value={holderName}
                  onChange={(e) => setHolderName(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-teal-500 focus:bg-white"
                />
              </div>

              {/* Client Association */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Associated Entity / Client <span className="text-slate-400 font-normal">(Optional)</span>
                </label>
                <select
                  value={selectedClientId}
                  onChange={(e) => setSelectedClientId(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-teal-500"
                >
                  <option value="">None (Practice / Firm Partner DSC)</option>
                  {clients.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.displayName} {c.pan ? `(${c.pan})` : ''}
                    </option>
                  ))}
                </select>
              </div>

              {/* Certificate Identifier */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Certificate Serial Number / Token ID
                </label>
                <input
                  type="text"
                  placeholder="e.g. 6F 4A 9B 12 00 3C"
                  value={certificateIdentifier}
                  onChange={(e) => setCertificateIdentifier(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs font-mono text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-teal-500 focus:bg-white"
                />
              </div>

              {/* Certificate Type & Issuer */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Certificate Type</label>
                  <select
                    value={certificateType}
                    onChange={(e) => setCertificateType(e.target.value as DscCertificateType)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-teal-500"
                  >
                    {CERTIFICATE_TYPES.map((t) => (
                      <option key={t.value} value={t.value}>
                        {t.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Issuing Authority</label>
                  <select
                    value={issuer}
                    onChange={(e) => setIssuer(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-teal-500"
                  >
                    {COMMON_ISSUERS.map((iss) => (
                      <option key={iss} value={iss}>
                        {iss}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {issuer === 'Other' && (
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Custom Issuer Name</label>
                  <input
                    type="text"
                    placeholder="Enter certifying authority name"
                    value={customIssuer}
                    onChange={(e) => setCustomIssuer(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-teal-500 focus:bg-white"
                  />
                </div>
              )}

              {/* Dates */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Issue Date <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="date"
                    required
                    value={issuedDate}
                    onChange={(e) => setIssuedDate(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-teal-500 focus:bg-white"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Expiry Date <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="date"
                    required
                    value={expiryDate}
                    onChange={(e) => setExpiryDate(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-teal-500 focus:bg-white"
                  />
                </div>
              </div>

              {/* Applicable Services Checkboxes */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-2">Applicable Compliance Filings</label>
                <div className="grid grid-cols-2 gap-2 bg-slate-50 p-3 rounded-lg border border-slate-200">
                  {COMMON_SERVICES.map((s) => (
                    <label key={s.id} className="flex items-center gap-2 text-xs text-slate-700 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={selectedServices.includes(s.id)}
                        onChange={() => toggleServiceSelection(s.id)}
                        className="rounded border-slate-300 text-teal-600 focus:ring-teal-500"
                      />
                      <span>{s.id}</span>
                    </label>
                  ))}
                </div>
              </div>

              {/* Notes */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Internal Notes & Remarks</label>
                <textarea
                  rows={2}
                  placeholder="e.g. USB token held in Office Locker #3. Assigned to Partner Aarav."
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-teal-500 focus:bg-white"
                />
              </div>

              {/* Modal Footer */}
              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
                <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)} disabled={saving} className="text-xs">
                  Cancel
                </Button>
                <Button type="submit" disabled={saving} className="text-xs font-semibold bg-teal-600 hover:bg-teal-700 text-white">
                  {saving ? 'Saving...' : editingDsc ? 'Update DSC' : 'Register DSC'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Revoke DSC Modal */}
      {isRevokeModalOpen && revokingDsc && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xl max-w-md w-full p-6 space-y-4">
            <div className="flex items-center gap-3 text-purple-600">
              <div className="w-10 h-10 rounded-full bg-purple-50 flex items-center justify-center">
                <Ban className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-900">Revoke Certificate</h3>
                <p className="text-xs text-slate-500">{revokingDsc.holderName}</p>
              </div>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              Marking this DSC as revoked indicates it is no longer valid for signing tax returns or MCA forms. Historical audit records will be preserved.
            </p>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Revocation Reason (Optional)</label>
              <textarea
                rows={2}
                placeholder="e.g. Token lost / compromised / replaced by new certificate."
                value={revocationReason}
                onChange={(e) => setRevocationReason(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-purple-500"
              />
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <Button
                variant="outline"
                onClick={() => setIsRevokeModalOpen(false)}
                disabled={processingRevoke}
                className="text-xs"
              >
                Cancel
              </Button>
              <Button
                onClick={handleRevokeDsc}
                disabled={processingRevoke}
                className="text-xs font-semibold bg-purple-600 hover:bg-purple-700 text-white"
              >
                {processingRevoke ? 'Revoking...' : 'Confirm Revoke'}
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {dscToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xl max-w-md w-full p-6 space-y-4">
            <div className="flex items-center gap-3 text-rose-600">
              <div className="w-10 h-10 rounded-full bg-rose-50 flex items-center justify-center">
                <Trash2 className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-900">Delete DSC Record</h3>
                <p className="text-xs text-slate-500">{dscToDelete.holderName}</p>
              </div>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              Are you sure you want to delete this DSC entry? This action cannot be undone.
            </p>

            <div className="flex items-center justify-end gap-3 pt-2">
              <Button variant="outline" onClick={() => setDscToDelete(null)} disabled={deleting} className="text-xs">
                Cancel
              </Button>
              <Button
                onClick={handleDeleteDsc}
                disabled={deleting}
                className="text-xs font-semibold bg-rose-600 hover:bg-rose-700 text-white"
              >
                {deleting ? 'Deleting...' : 'Delete Record'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
