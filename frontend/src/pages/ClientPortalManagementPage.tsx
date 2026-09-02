import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Globe,
  ShieldCheck,
  Building2,
  FileSpreadsheet,
  Receipt,
  FolderLock,
  UploadCloud,
  CheckCircle2,
  AlertCircle,
  Clock,
  KeyRound,
  FileText,
  CreditCard,
  Phone,
  ArrowUpRight,
  Eye,
  Plus,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { DataTable } from '../components/common/DataTable';
import { useAuth } from '../context/AuthContext';
import { portalApi, clientApi } from '../api/endpoints';
import {
  Client,
  ClientPortalDashboard,
  ClientGstStatus,
  ClientItrStatus,
  ClientDocumentRequest,
  ClientPortalUser,
  Invoice,
  DocumentItem,
  RegisterClientPortalUserRequest,
} from '../types';
import { PortalDocumentRequestsView } from '../components/docrequest/PortalDocumentRequestsView';
import clsx from 'clsx';

export const ClientPortalManagementPage: React.FC = () => {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // Active Tab
  const activeTab = searchParams.get('tab') || 'overview';
  const setActiveTab = (tab: string) => {
    setSearchParams({ tab });
  };

  // User Role Detection
  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isClientUser = userRoleCodes.some((r: string) => ['CLIENT_USER', 'CLIENT_ADMIN'].includes(r));
  const isPracticeUser = !isClientUser;

  // Practice state
  const [clients, setClients] = useState<Client[]>([]);
  const [selectedClientId, setSelectedClientId] = useState<string>('');
  const [clientUsers, setClientUsers] = useState<ClientPortalUser[]>([]);

  // Portal Data State
  const [dashboard, setDashboard] = useState<ClientPortalDashboard | null>(null);
  const [gstFilings, setGstFilings] = useState<ClientGstStatus[]>([]);
  const [itrReturns, setItrReturns] = useState<ClientItrStatus[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [pendingDocRequests, setPendingDocRequests] = useState<ClientDocumentRequest[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Modals
  const [isProvisionModalOpen, setIsProvisionModalOpen] = useState(false);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [selectedDocRequest, setSelectedDocRequest] = useState<ClientDocumentRequest | null>(null);
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);
  const [isRequestDocModalOpen, setIsRequestDocModalOpen] = useState(false);

  // Form States
  const [provisionForm, setProvisionForm] = useState<RegisterClientPortalUserRequest>({
    clientId: '',
    email: '',
    password: 'Password123!',
    firstName: '',
    lastName: '',
    phone: '',
    role: 'CLIENT_USER',
  });

  const [uploadForm, setUploadForm] = useState<{
    file: File | null;
    title: string;
    category: string;
    description: string;
  }>({
    file: null,
    title: '',
    category: 'TAX_RETURNS',
    description: '',
  });

  const [requestDocForm, setRequestDocForm] = useState({
    clientId: '',
    title: '',
    description: '',
    documentType: 'BANK_STATEMENT',
    dueDate: '',
  });

  // Load clients if practice user
  useEffect(() => {
    if (isPracticeUser) {
      clientApi.getAll({ size: 100 }).then((res) => {
        const clientList = res.content || [];
        setClients(clientList);
        if (clientList.length > 0 && !selectedClientId) {
          setSelectedClientId(clientList[0].id);
        }
      }).catch(console.error);
    }
  }, [isPracticeUser]);

  // Load Portal Data
  const loadPortalData = async () => {
    setIsLoading(true);
    try {
      if (isClientUser) {
        // Logged-in Customer View
        const [dash, gst, itr, invs, docs, pending] = await Promise.allSettled([
          portalApi.getDashboard(),
          portalApi.getGstStatus(),
          portalApi.getItrStatus(),
          portalApi.getClientInvoices(),
          portalApi.getClientDocuments(),
          portalApi.getPendingDocuments(),
        ]);

        if (dash.status === 'fulfilled') setDashboard(dash.value);
        if (gst.status === 'fulfilled') setGstFilings(gst.value);
        if (itr.status === 'fulfilled') setItrReturns(itr.value);
        if (invs.status === 'fulfilled') setInvoices(invs.value);
        if (docs.status === 'fulfilled') setDocuments(docs.value);
        if (pending.status === 'fulfilled') setPendingDocRequests(pending.value);
      } else if (selectedClientId) {
        // Practice Preview View
        const [dash, usersRes] = await Promise.allSettled([
          portalApi.getDashboardPreview(selectedClientId),
          portalApi.getClientPortalUsers(selectedClientId),
        ]);

        if (dash.status === 'fulfilled') {
          setDashboard(dash.value);
          setGstFilings(dash.value.latestGstFilings || []);
          setItrReturns(dash.value.latestItrReturns || []);
          setInvoices(dash.value.latestInvoices || []);
          setPendingDocRequests(dash.value.pendingDocumentRequests || []);
        }
        if (usersRes.status === 'fulfilled') {
          setClientUsers(usersRes.value);
        }
      }
    } catch (err) {
      console.error('Failed to load portal data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadPortalData();
  }, [isClientUser, selectedClientId]);

  // Handle Provision User
  const handleProvisionUser = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await portalApi.registerUser(provisionForm);
      alert(`Client portal user provisioned successfully for ${provisionForm.email}`);
      setIsProvisionModalOpen(false);
      if (selectedClientId) {
        const usersRes = await portalApi.getClientPortalUsers(selectedClientId);
        setClientUsers(usersRes);
      }
    } catch (err: any) {
      alert(`Failed to provision user: ${err.response?.data?.message || err.message}`);
    }
  };

  // Handle Upload Document
  const handleUploadDocument = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadForm.file) {
      alert('Please select a file to upload');
      return;
    }
    try {
      await portalApi.uploadDocument(
        uploadForm.file,
        {
          title: uploadForm.title || uploadForm.file.name,
          category: uploadForm.category,
          description: uploadForm.description,
          clientId: dashboard?.clientId,
        },
        selectedDocRequest?.id
      );
      alert('Document uploaded successfully to vault!');
      setIsUploadModalOpen(false);
      setSelectedDocRequest(null);
      setUploadForm({ file: null, title: '', category: 'TAX_RETURNS', description: '' });
      loadPortalData();
    } catch (err: any) {
      alert(`Upload failed: ${err.response?.data?.message || err.message}`);
    }
  };

  // Handle Request Document
  const handleRequestDocument = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await portalApi.requestDocument({
        ...requestDocForm,
        clientId: requestDocForm.clientId || selectedClientId,
      });
      alert('Document request sent to client successfully!');
      setIsRequestDocModalOpen(false);
      loadPortalData();
    } catch (err: any) {
      alert(`Failed to create document request: ${err.response?.data?.message || err.message}`);
    }
  };

  // Status Badge Helper
  const renderStatusBadge = (status: string) => {
    const s = (status || '').toUpperCase();
    if (['FILED', 'PAID', 'VERIFIED', 'COMPLETED', 'ACKNOWLEDGED'].includes(s)) {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
          <CheckCircle2 className="w-3 h-3" />
          {s}
        </span>
      );
    }
    if (['UNDER_REVIEW', 'PARTIALLY_PAID', 'SUBMITTED', 'IN_PROGRESS'].includes(s)) {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
          <Clock className="w-3 h-3" />
          {s.replace('_', ' ')}
        </span>
      );
    }
    if (['DRAFT', 'PENDING', 'UNPAID', 'SENT'].includes(s)) {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-amber-50 text-amber-700 border border-amber-200">
          <AlertCircle className="w-3 h-3" />
          {s}
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-rose-50 text-rose-700 border border-rose-200">
        <AlertCircle className="w-3 h-3" />
        {s}
      </span>
    );
  };

  // Currency formatter
  const formatCurrency = (val?: number) => {
    if (val === undefined || val === null) return '₹0.00';
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(val);
  };

  // Outstanding balance & stats
  const outstandingBalance = dashboard?.outstandingBalance || 0;
  const unpaidCount = dashboard?.unpaidInvoicesCount || 0;

  return (
    <div className="space-y-6">
      {/* Top Header & Context Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-2xs">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-brand-50 border border-brand-100 flex items-center justify-center text-brand-600 shadow-xs">
              <Globe className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-xl font-black tracking-tight text-slate-900 flex items-center gap-2">
                {isClientUser ? 'Client Self-Service Portal' : 'Client Portal & Customer Hub'}
                <span className="text-[11px] font-bold uppercase tracking-wider px-2 py-0.5 bg-emerald-100 text-emerald-800 rounded-full">
                  {isClientUser ? 'Customer Access' : 'Practice Admin Mode'}
                </span>
              </h1>
              <p className="text-xs text-slate-500 mt-0.5">
                {isClientUser
                  ? 'Real-time GST & ITR filing status, fee invoices, outstanding bills, and tax compliance vault.'
                  : 'Manage client portal credentials, preview customer dashboards, and track client document requests.'}
              </p>
            </div>
          </div>
        </div>

        {/* Practice Admin Actions & Client Selector */}
        {isPracticeUser && (
          <div className="flex flex-wrap items-center gap-2.5">
            <div className="flex items-center gap-1.5 bg-slate-50 px-3 py-1.5 rounded-xl border border-slate-200">
              <span className="text-xs font-semibold text-slate-600">Client Preview:</span>
              <select
                value={selectedClientId}
                onChange={(e) => setSelectedClientId(e.target.value)}
                className="text-xs font-bold text-slate-900 bg-transparent border-0 focus:ring-0 cursor-pointer pr-6"
              >
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.displayName} ({c.pan})
                  </option>
                ))}
              </select>
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                const target = clients.find((c) => c.id === selectedClientId);
                setProvisionForm({
                  clientId: selectedClientId,
                  email: target?.email || '',
                  password: 'Password123!',
                  firstName: target?.displayName?.split(' ')[0] || 'Client',
                  lastName: target?.displayName?.split(' ').slice(1).join(' ') || 'User',
                  phone: target?.phone || '',
                  role: 'CLIENT_USER',
                });
                setIsProvisionModalOpen(true);
              }}
              leftIcon={<KeyRound className="w-4 h-4" />}
            >
              Provision Login
            </Button>

            <Button
              variant="primary"
              size="sm"
              onClick={() => {
                setRequestDocForm({
                  clientId: selectedClientId,
                  title: '',
                  description: '',
                  documentType: 'BANK_STATEMENT',
                  dueDate: '',
                });
                setIsRequestDocModalOpen(true);
              }}
              leftIcon={<Plus className="w-4 h-4" />}
            >
              Request Document
            </Button>
          </div>
        )}
      </div>

      {/* Hero Card: Client Profile & Outstanding Due Price Alert */}
      {dashboard && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
          {/* Client Details */}
          <div className="lg:col-span-2 bg-gradient-to-br from-slate-900 to-slate-800 text-white rounded-2xl p-6 shadow-md relative overflow-hidden flex flex-col justify-between">
            <div className="space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <span className="text-[11px] font-bold uppercase tracking-widest text-brand-300 font-mono">
                    {dashboard.clientType || 'BUSINESS CLIENT'}
                  </span>
                  <h2 className="text-2xl font-black tracking-tight text-white mt-0.5">
                    {dashboard.displayName}
                  </h2>
                  {dashboard.legalName && dashboard.legalName !== dashboard.displayName && (
                    <p className="text-xs text-slate-400">{dashboard.legalName}</p>
                  )}
                </div>

                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                  <ShieldCheck className="w-3.5 h-3.5" />
                  Verified Tax Account
                </span>
              </div>

              {/* Tax Identifiers Grid */}
              <div className="grid grid-cols-3 gap-3 pt-3 border-t border-slate-700/60 font-mono text-xs">
                <div className="bg-slate-800/80 p-2.5 rounded-lg border border-slate-700">
                  <span className="text-[10px] text-slate-400 block font-sans uppercase font-bold">PAN</span>
                  <span className="font-bold text-amber-300">{dashboard.pan || 'N/A'}</span>
                </div>
                <div className="bg-slate-800/80 p-2.5 rounded-lg border border-slate-700">
                  <span className="text-[10px] text-slate-400 block font-sans uppercase font-bold">GSTIN</span>
                  <span className="font-bold text-sky-300 truncate block">{dashboard.gstin || 'Unregistered'}</span>
                </div>
                <div className="bg-slate-800/80 p-2.5 rounded-lg border border-slate-700">
                  <span className="text-[10px] text-slate-400 block font-sans uppercase font-bold">TAN</span>
                  <span className="font-bold text-purple-300">{dashboard.tan || 'N/A'}</span>
                </div>
              </div>
            </div>

            {/* Assigned CA Practitioner Footer */}
            {dashboard.assignedPractitionerName && (
              <div className="mt-5 pt-3 border-t border-slate-700/60 flex items-center justify-between text-xs">
                <div className="flex items-center gap-2">
                  <div className="w-7 h-7 rounded-full bg-brand-500/30 border border-brand-400/40 flex items-center justify-center font-bold text-brand-200 text-xs">
                    {dashboard.assignedPractitionerName.charAt(0)}
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-400 block">Assigned CA Consultant</span>
                    <span className="font-bold text-slate-200">{dashboard.assignedPractitionerName}</span>
                  </div>
                </div>
                {dashboard.assignedPractitionerPhone && (
                  <a
                    href={`tel:${dashboard.assignedPractitionerPhone}`}
                    className="inline-flex items-center gap-1 text-[11px] text-brand-300 hover:text-brand-200 font-semibold bg-white/5 px-2.5 py-1 rounded-md"
                  >
                    <Phone className="w-3 h-3" />
                    {dashboard.assignedPractitionerPhone}
                  </a>
                )}
              </div>
            )}
          </div>

          {/* Outstanding Due Price & Invoices Box */}
          <div className="bg-gradient-to-br from-white to-amber-50/50 rounded-2xl p-6 border border-amber-200/80 shadow-2xs flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-amber-800">
                  Fee Billing & Invoices
                </span>
                <span className="w-8 h-8 rounded-full bg-amber-100 flex items-center justify-center text-amber-700">
                  <Receipt className="w-4 h-4" />
                </span>
              </div>

              <div className="mt-4">
                <span className="text-xs text-slate-500 block font-medium">Total Outstanding Balance Due</span>
                <div className="text-3xl font-black tracking-tight text-slate-900 mt-1">
                  {formatCurrency(outstandingBalance)}
                </div>
                <div className="flex items-center gap-2 mt-2">
                  <span
                    className={clsx(
                      'text-xs font-bold px-2 py-0.5 rounded-full',
                      outstandingBalance > 0
                        ? 'bg-rose-100 text-rose-800 border border-rose-200'
                        : 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                    )}
                  >
                    {unpaidCount} Unpaid {unpaidCount === 1 ? 'Invoice' : 'Invoices'}
                  </span>
                  {outstandingBalance === 0 && (
                    <span className="text-xs text-emerald-700 font-bold">✨ All dues cleared</span>
                  )}
                </div>
              </div>
            </div>

            <div className="pt-5 border-t border-amber-200/60 mt-4 flex items-center gap-2">
              <Button
                variant="primary"
                size="sm"
                className="w-full justify-center shadow-xs"
                onClick={() => setActiveTab('invoices')}
                leftIcon={<CreditCard className="w-4 h-4" />}
              >
                View & Pay Invoices
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-200 pb-2 overflow-x-auto select-none">
        {[
          { id: 'overview', label: 'Overview & Summary', icon: Globe },
          { id: 'gst', label: `GST Filings (${gstFilings.length})`, icon: Building2 },
          { id: 'itr', label: `ITR Returns (${itrReturns.length})`, icon: FileSpreadsheet },
          { id: 'invoices', label: `Invoices & Bills (${invoices.length})`, icon: Receipt },
          { id: 'documents', label: `Documents & Requests (${pendingDocRequests.length + documents.length})`, icon: FolderLock },
          ...(isPracticeUser ? [{ id: 'users', label: `Portal Logins (${clientUsers.length})`, icon: KeyRound }] : []),
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={clsx(
                'inline-flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-xl transition-all shrink-0',
                isActive
                  ? 'bg-brand-600 text-white shadow-xs'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
              )}
            >
              <Icon className="w-4 h-4" />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* TAB 1: Overview & Summary */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          {/* Quick Metrics */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
              <span className="text-[11px] font-bold text-slate-500 uppercase">GST Returns</span>
              <p className="text-2xl font-black text-slate-900 mt-1">{gstFilings.length}</p>
              <span className="text-[10px] text-slate-400">Total tracked periods</span>
            </div>
            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
              <span className="text-[11px] font-bold text-slate-500 uppercase">ITR Returns</span>
              <p className="text-2xl font-black text-slate-900 mt-1">{itrReturns.length}</p>
              <span className="text-[10px] text-slate-400">Assessment years filed</span>
            </div>
            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
              <span className="text-[11px] font-bold text-slate-500 uppercase">Pending Docs</span>
              <p className="text-2xl font-black text-amber-600 mt-1">{pendingDocRequests.length}</p>
              <span className="text-[10px] text-slate-400">Action items for client</span>
            </div>
            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
              <span className="text-[11px] font-bold text-slate-500 uppercase">Unpaid Invoices</span>
              <p className="text-2xl font-black text-rose-600 mt-1">{unpaidCount}</p>
              <span className="text-[10px] text-slate-400">{formatCurrency(outstandingBalance)} due</span>
            </div>
          </div>

          {/* Action Required: Multi-Item Document Requests & Legacy Requests */}
          {((dashboard?.activeMultiItemRequests && dashboard.activeMultiItemRequests.length > 0) || pendingDocRequests.length > 0) && (
            <Card
              title="🚨 Action Required: Pending Document Uploads"
              subtitle="Please upload required tax and compliance documents to avoid delays"
              className="border-amber-200 bg-amber-50/20 shadow-2xs"
            >
              <div className="space-y-3">
                {dashboard?.activeMultiItemRequests?.map((req) => (
                  <div
                    key={req.id}
                    className="bg-white p-4 rounded-xl border border-amber-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-2xs"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-900">{req.purpose}</span>
                        <span className="text-[10px] font-mono font-bold bg-amber-100 text-amber-800 px-2 py-0.5 rounded">
                          {req.requestNumber}
                        </span>
                        {req.isOverdue && (
                          <span className="text-[10px] font-bold bg-rose-100 text-rose-800 px-2 py-0.5 rounded">
                            OVERDUE
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-slate-500">
                        {req.items?.filter((i) => i.status === 'PENDING' || i.status === 'REJECTED').length || 0} items remaining to upload
                      </p>
                      {req.dueDate && (
                        <span className="text-[11px] text-amber-700 font-semibold block">
                          Due Date: {req.dueDate}
                        </span>
                      )}
                    </div>

                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => setActiveTab('documents')}
                      leftIcon={<UploadCloud className="w-4 h-4" />}
                    >
                      Open Checklist & Upload
                    </Button>
                  </div>
                ))}

                {pendingDocRequests.map((req) => (
                  <div
                    key={req.id}
                    className="bg-white p-4 rounded-xl border border-amber-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-2xs"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-900">{req.title}</span>
                        <span className="text-[10px] font-mono font-bold bg-amber-100 text-amber-800 px-2 py-0.5 rounded">
                          {req.documentType}
                        </span>
                      </div>
                      {req.description && (
                        <p className="text-xs text-slate-500">{req.description}</p>
                      )}
                      {req.dueDate && (
                        <span className="text-[11px] text-rose-600 font-semibold block">
                          Due Date: {req.dueDate}
                        </span>
                      )}
                    </div>

                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => {
                        setSelectedDocRequest(req);
                        setUploadForm({
                          file: null,
                          title: req.title,
                          category: 'CLIENT_UPLOAD',
                          description: `Fulfilling request: ${req.title}`,
                        });
                        setIsUploadModalOpen(true);
                      }}
                      leftIcon={<UploadCloud className="w-4 h-4" />}
                    >
                      Upload Now
                    </Button>
                  </div>
                ))}
              </div>
            </Card>
          )}

          {/* Dual Grid: Recent GST & ITR */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* GST Summary */}
            <Card
              title="Recent GST Filing Status"
              subtitle="Monthly & quarterly return status"
              action={
                <Button variant="ghost" size="sm" onClick={() => setActiveTab('gst')} leftIcon={<ArrowUpRight className="w-4 h-4" />}>
                  View All
                </Button>
              }
            >
              {gstFilings.length === 0 ? (
                <div className="py-8 text-center text-xs text-slate-400">No GST filings recorded.</div>
              ) : (
                <div className="space-y-3">
                  {gstFilings.slice(0, 4).map((f) => (
                    <div
                      key={f.id}
                      className="p-3 bg-slate-50 rounded-xl border border-slate-200 flex items-center justify-between gap-3 text-xs"
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-slate-900">{f.returnType}</span>
                          <span className="text-slate-500 font-mono">({f.returnPeriod})</span>
                        </div>
                        {f.arn && (
                          <span className="text-[11px] text-emerald-700 font-mono block">ARN: {f.arn}</span>
                        )}
                        <span className="text-[10px] text-slate-400">Due: {f.dueDate}</span>
                      </div>
                      <div className="text-right">
                        {renderStatusBadge(f.status)}
                        {f.totalTaxPayable !== undefined && f.totalTaxPayable > 0 && (
                          <span className="text-[11px] font-bold text-slate-700 block mt-1">
                            {formatCurrency(f.totalTaxPayable)}
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </Card>

            {/* ITR Summary */}
            <Card
              title="Recent Income Tax Returns"
              subtitle="Annual return filing history"
              action={
                <Button variant="ghost" size="sm" onClick={() => setActiveTab('itr')} leftIcon={<ArrowUpRight className="w-4 h-4" />}>
                  View All
                </Button>
              }
            >
              {itrReturns.length === 0 ? (
                <div className="py-8 text-center text-xs text-slate-400">No ITR returns recorded.</div>
              ) : (
                <div className="space-y-3">
                  {itrReturns.slice(0, 4).map((itr) => (
                    <div
                      key={itr.id}
                      className="p-3 bg-slate-50 rounded-xl border border-slate-200 flex items-center justify-between gap-3 text-xs"
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-slate-900">AY {itr.assessmentYear}</span>
                          <span className="text-slate-500 font-mono">({itr.itrType})</span>
                        </div>
                        {itr.acknowledgementNumber && (
                          <span className="text-[11px] text-emerald-700 font-mono block">
                            Ack: {itr.acknowledgementNumber}
                          </span>
                        )}
                        <span className="text-[10px] text-slate-400">Due: {itr.dueDate}</span>
                      </div>
                      <div className="text-right">{renderStatusBadge(itr.status)}</div>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>
        </div>
      )}

      {/* TAB 2: GST Filings Detail */}
      {activeTab === 'gst' && (
        <Card title="GST Return Filing Status" subtitle="Detailed breakdown of all client GST returns & acknowledgements">
          <DataTable<ClientGstStatus>
            columns={[
              {
                header: 'Return Type',
                accessor: 'returnType',
                cell: (row) => (
                  <div>
                    <span className="font-bold text-slate-900">{row.returnType}</span>
                    <span className="text-[11px] text-slate-500 block">FY {row.financialYear || '2026-27'}</span>
                  </div>
                ),
              },
              {
                header: 'Period',
                accessor: 'returnPeriod',
                cell: (row) => <span className="font-mono font-semibold text-slate-700">{row.returnPeriod}</span>,
              },
              {
                header: 'Due Date',
                accessor: 'dueDate',
                cell: (row) => <span className="font-mono text-slate-600">{row.dueDate}</span>,
              },
              {
                header: 'ARN / Ack Number',
                accessor: 'arn',
                cell: (row) =>
                  row.arn ? (
                    <span className="font-mono text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                      {row.arn}
                    </span>
                  ) : (
                    <span className="text-slate-400 italic">Not Generated</span>
                  ),
              },
              {
                header: 'Tax Payable',
                cell: (row) => (
                  <span className="font-semibold text-slate-900">
                    {row.totalTaxPayable ? formatCurrency(row.totalTaxPayable) : '₹0.00'}
                  </span>
                ),
              },
              {
                header: 'Filing Status',
                accessor: 'status',
                cell: (row) => renderStatusBadge(row.status),
              },
            ]}
            data={gstFilings}
            isLoading={isLoading}
            searchPlaceholder="Search GST returns by period, ARN, or type..."
          />
        </Card>
      )}

      {/* TAB 3: ITR Returns Detail */}
      {activeTab === 'itr' && (
        <Card title="Income Tax Returns (ITR)" subtitle="All assessment years, ITR forms, and filing acknowledgement records">
          <DataTable<ClientItrStatus>
            columns={[
              {
                header: 'Assessment Year',
                accessor: 'assessmentYear',
                cell: (row) => <span className="font-bold text-slate-900 font-mono">AY {row.assessmentYear}</span>,
              },
              {
                header: 'ITR Form',
                accessor: 'itrType',
                cell: (row) => (
                  <span className="font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-700 text-[11px] font-mono">
                    {row.itrType}
                  </span>
                ),
              },
              {
                header: 'Due Date',
                accessor: 'dueDate',
                cell: (row) => <span className="font-mono text-slate-600">{row.dueDate}</span>,
              },
              {
                header: 'Filing Date',
                accessor: 'filingDate',
                cell: (row) =>
                  row.filingDate ? (
                    <span className="font-mono text-slate-700">{row.filingDate}</span>
                  ) : (
                    <span className="text-slate-400 italic">Pending</span>
                  ),
              },
              {
                header: 'Acknowledgement Number',
                accessor: 'acknowledgementNumber',
                cell: (row) =>
                  row.acknowledgementNumber ? (
                    <span className="font-mono text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                      {row.acknowledgementNumber}
                    </span>
                  ) : (
                    <span className="text-slate-400 italic">Pending Filing</span>
                  ),
              },
              {
                header: 'Status',
                accessor: 'status',
                cell: (row) => renderStatusBadge(row.status),
              },
            ]}
            data={itrReturns}
            isLoading={isLoading}
            searchPlaceholder="Search ITR returns by AY or Ack number..."
          />
        </Card>
      )}

      {/* TAB 4: Invoices & Due Bills */}
      {activeTab === 'invoices' && (
        <div className="space-y-6">
          {/* Outstanding Balance Banner */}
          <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-2xs flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-2xl bg-amber-100 border border-amber-200 flex items-center justify-center text-amber-700">
                <Receipt className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs text-slate-500 font-semibold uppercase tracking-wider block">
                  Current Due Balance
                </span>
                <span className="text-2xl font-black text-slate-900">{formatCurrency(outstandingBalance)}</span>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <div className="bg-slate-50 px-4 py-2 rounded-xl border border-slate-200 text-xs">
                <span className="text-slate-500 block">Unpaid Invoices</span>
                <span className="font-bold text-rose-600 text-sm">{unpaidCount}</span>
              </div>
              <div className="bg-slate-50 px-4 py-2 rounded-xl border border-slate-200 text-xs">
                <span className="text-slate-500 block">Total Invoices</span>
                <span className="font-bold text-slate-900 text-sm">{invoices.length}</span>
              </div>
            </div>
          </div>

          {/* Invoices DataTable */}
          <Card title="Fee Invoices & Payment Receipts" subtitle="Professional consulting fee bills issued to this client">
            <DataTable<Invoice>
              columns={[
                {
                  header: 'Invoice Number',
                  accessor: 'invoiceNumber',
                  cell: (row) => (
                    <div>
                      <span className="font-mono font-bold text-brand-600 block">{row.invoiceNumber}</span>
                      <span className="text-[10px] text-slate-400">Date: {row.invoiceDate}</span>
                    </div>
                  ),
                },
                {
                  header: 'Due Date',
                  accessor: 'dueDate',
                  cell: (row) => <span className="font-mono text-slate-600">{row.dueDate}</span>,
                },
                {
                  header: 'Total Price',
                  accessor: 'total',
                  cell: (row) => (
                    <span className="font-bold text-slate-900">{formatCurrency(row.totalAmount || row.total)}</span>
                  ),
                },
                {
                  header: 'Paid Amount',
                  accessor: 'paidAmount',
                  cell: (row) => (
                    <span className="font-semibold text-emerald-700">{formatCurrency(row.paidAmount)}</span>
                  ),
                },
                {
                  header: 'Balance Due',
                  accessor: 'balanceDue',
                  cell: (row) => (
                    <span
                      className={clsx(
                        'font-bold',
                        (row.balanceDue || 0) > 0 ? 'text-rose-600' : 'text-slate-500'
                      )}
                    >
                      {formatCurrency(row.balanceDue)}
                    </span>
                  ),
                },
                {
                  header: 'Status',
                  accessor: 'status',
                  cell: (row) => renderStatusBadge(row.status),
                },
                {
                  header: 'Actions',
                  cell: (row) => (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setSelectedInvoice(row)}
                      leftIcon={<Eye className="w-4 h-4" />}
                    >
                      Details
                    </Button>
                  ),
                },
              ]}
              data={invoices}
              isLoading={isLoading}
              searchPlaceholder="Search invoices by number or date..."
            />
          </Card>
        </div>
      )}

      {/* TAB 5: Document Requests & Vault */}
      {activeTab === 'documents' && (
        <div className="space-y-6">
          {/* Multi-Item Document Requests V1 */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <FileText className="w-4 h-4 text-emerald-600" />
                Requested Documents from Tax Consultant
              </h2>
              <span className="text-xs font-semibold text-slate-500">Document Request Checklist V1</span>
            </div>
            <PortalDocumentRequestsView
              isPracticeUser={isPracticeUser}
              clientId={isPracticeUser ? selectedClientId : undefined}
            />
          </div>

          <div className="flex items-center justify-between bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
            <div>
              <h2 className="text-sm font-bold text-slate-900">Secure Document Repository</h2>
              <p className="text-xs text-slate-500">Directly upload bank statements, TDS certificates, and tax computation files.</p>
            </div>
            <Button
              variant="primary"
              size="sm"
              onClick={() => {
                setSelectedDocRequest(null);
                setUploadForm({ file: null, title: '', category: 'TAX_RETURNS', description: '' });
                setIsUploadModalOpen(true);
              }}
              leftIcon={<UploadCloud className="w-4 h-4" />}
            >
              Upload Document
            </Button>
          </div>

          <Card title="Uploaded Client Documents" subtitle="Verified files in your client vault">
            <DataTable<DocumentItem>
              columns={[
                {
                  header: 'Document Name',
                  accessor: 'filename',
                  cell: (row) => (
                    <div className="flex items-center gap-2">
                      <FileText className="w-4 h-4 text-brand-600 shrink-0" />
                      <div>
                        <span className="font-bold text-slate-900 block">{row.filename || row.title}</span>
                        <span className="text-[10px] text-slate-400">{row.contentType || 'PDF'}</span>
                      </div>
                    </div>
                  ),
                },
                {
                  header: 'Category',
                  accessor: 'category',
                  cell: (row) => (
                    <span className="text-[11px] font-semibold bg-slate-100 text-slate-700 px-2 py-0.5 rounded">
                      {row.category}
                    </span>
                  ),
                },
                {
                  header: 'Uploaded Date',
                  accessor: 'createdAt',
                  cell: (row) => (
                    <span className="font-mono text-slate-600">
                      {row.createdAt ? new Date(row.createdAt).toLocaleDateString() : 'Recent'}
                    </span>
                  ),
                },
              ]}
              data={documents}
              isLoading={isLoading}
              searchPlaceholder="Search client documents..."
            />
          </Card>
        </div>
      )}

      {/* TAB 6: Portal Users & Logins (For Practice Admins) */}
      {activeTab === 'users' && isPracticeUser && (
        <Card
          title="Client Portal Logins & Credentials"
          subtitle="Customer users who can log into this client portal"
          action={
            <Button
              variant="primary"
              size="sm"
              onClick={() => {
                const target = clients.find((c) => c.id === selectedClientId);
                setProvisionForm({
                  clientId: selectedClientId,
                  email: target?.email || '',
                  password: 'Password123!',
                  firstName: target?.displayName?.split(' ')[0] || 'Client',
                  lastName: target?.displayName?.split(' ').slice(1).join(' ') || 'User',
                  phone: target?.phone || '',
                  role: 'CLIENT_USER',
                });
                setIsProvisionModalOpen(true);
              }}
              leftIcon={<Plus className="w-4 h-4" />}
            >
              Provision New User
            </Button>
          }
        >
          {clientUsers.length === 0 ? (
            <div className="py-12 text-center space-y-3">
              <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center text-slate-400 mx-auto">
                <KeyRound className="w-6 h-6" />
              </div>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                No portal login credentials have been provisioned for this client yet. Click <strong>Provision New User</strong> to invite them.
              </p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100 text-xs">
              {clientUsers.map((u) => (
                <div key={u.userId} className="py-3 flex items-center justify-between gap-4">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-brand-100 text-brand-700 flex items-center justify-center font-bold">
                      {u.firstName?.charAt(0) || 'U'}
                    </div>
                    <div>
                      <span className="font-bold text-slate-900 block">{u.fullName || u.email}</span>
                      <span className="text-slate-500 font-mono">{u.email}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-0.5 rounded-full font-bold text-[11px] bg-emerald-50 text-emerald-700 border border-emerald-200">
                      {u.roles?.join(', ') || 'CLIENT_USER'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      )}

      {/* MODAL 1: Provision Client Login */}
      {isProvisionModalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-xs flex items-end sm:items-center justify-center p-0 sm:p-4 z-50 animate-in fade-in">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-5 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-base font-black text-slate-900 flex items-center gap-2">
                <KeyRound className="w-5 h-5 text-brand-600" />
                Provision Client Portal User
              </h3>
              <button
                onClick={() => setIsProvisionModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 text-sm font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleProvisionUser} className="space-y-4 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Target Client Account</label>
                <select
                  value={provisionForm.clientId}
                  onChange={(e) => setProvisionForm({ ...provisionForm, clientId: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg font-bold"
                  required
                >
                  <option value="">Select a Client</option>
                  {clients.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.displayName} ({c.pan})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-700 mb-1">First Name</label>
                  <input
                    type="text"
                    value={provisionForm.firstName}
                    onChange={(e) => setProvisionForm({ ...provisionForm, firstName: e.target.value })}
                    className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg"
                    required
                  />
                </div>
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Last Name</label>
                  <input
                    type="text"
                    value={provisionForm.lastName || ''}
                    onChange={(e) => setProvisionForm({ ...provisionForm, lastName: e.target.value })}
                    className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg"
                  />
                </div>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Login Email</label>
                <input
                  type="email"
                  value={provisionForm.email}
                  onChange={(e) => setProvisionForm({ ...provisionForm, email: e.target.value })}
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg font-mono"
                  required
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Initial Password</label>
                <input
                  type="text"
                  value={provisionForm.password}
                  onChange={(e) => setProvisionForm({ ...provisionForm, password: e.target.value })}
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg font-mono"
                  required
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Portal Role</label>
                <select
                  value={provisionForm.role}
                  onChange={(e) => setProvisionForm({ ...provisionForm, role: e.target.value as any })}
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg font-semibold"
                >
                  <option value="CLIENT_USER">CLIENT_USER (Standard Access)</option>
                  <option value="CLIENT_ADMIN">CLIENT_ADMIN (Full Client Portal Admin)</option>
                </select>
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                <Button variant="outline" size="sm" type="button" onClick={() => setIsProvisionModalOpen(false)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" type="submit">
                  Create User Login
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL 2: Upload Document */}
      {isUploadModalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-xs flex items-end sm:items-center justify-center p-0 sm:p-4 z-50 animate-in fade-in">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-5 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-base font-black text-slate-900 flex items-center gap-2">
                <UploadCloud className="w-5 h-5 text-brand-600" />
                Upload Compliance Document
              </h3>
              <button
                onClick={() => setIsUploadModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 text-sm font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleUploadDocument} className="space-y-4 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Document Title</label>
                <input
                  type="text"
                  value={uploadForm.title}
                  onChange={(e) => setUploadForm({ ...uploadForm, title: e.target.value })}
                  placeholder="e.g. Bank Statement April-June 2026"
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg"
                  required
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Category</label>
                <select
                  value={uploadForm.category}
                  onChange={(e) => setUploadForm({ ...uploadForm, category: e.target.value })}
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg"
                >
                  <option value="BANK_STATEMENT">Bank Statement</option>
                  <option value="FORM_16">Form 16 / TDS Certificate</option>
                  <option value="GST_INVOICES">Sales / Purchase Invoices</option>
                  <option value="TAX_RETURNS">Signed Tax Returns</option>
                  <option value="OTHER">Other Supporting Document</option>
                </select>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Select File (PDF, XLSX, ZIP, JPG)</label>
                <input
                  type="file"
                  onChange={(e) => {
                    const f = e.target.files?.[0] || null;
                    setUploadForm({ ...uploadForm, file: f, title: uploadForm.title || f?.name || '' });
                  }}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs"
                  required
                />
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                <Button variant="outline" size="sm" type="button" onClick={() => setIsUploadModalOpen(false)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" type="submit">
                  Upload to Vault
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL 3: Invoice Details Breakdown */}
      {selectedInvoice && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-xs flex items-end sm:items-center justify-center p-0 sm:p-4 z-50 animate-in fade-in">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-5 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div>
                <span className="text-[10px] uppercase font-bold text-slate-400">Invoice Details</span>
                <h3 className="text-base font-black text-slate-900 font-mono">
                  {selectedInvoice.invoiceNumber}
                </h3>
              </div>
              <button
                onClick={() => setSelectedInvoice(null)}
                className="text-slate-400 hover:text-slate-600 text-sm font-bold"
              >
                ✕
              </button>
            </div>

            <div className="space-y-4 text-xs">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 bg-slate-50 p-3.5 rounded-xl border border-slate-200">
                <div>
                  <span className="text-slate-500 block">Invoice Date:</span>
                  <span className="font-bold text-slate-800">{selectedInvoice.invoiceDate}</span>
                </div>
                <div>
                  <span className="text-slate-500 block">Due Date:</span>
                  <span className="font-bold text-slate-800">{selectedInvoice.dueDate}</span>
                </div>
                <div>
                  <span className="text-slate-500 block">Status:</span>
                  <div className="mt-0.5">{renderStatusBadge(selectedInvoice.status)}</div>
                </div>
                <div>
                  <span className="text-slate-500 block">Balance Due:</span>
                  <span className="font-bold text-rose-600 text-sm">
                    {formatCurrency(selectedInvoice.balanceDue)}
                  </span>
                </div>
              </div>

              {/* Line Items */}
              {selectedInvoice.items && selectedInvoice.items.length > 0 && (
                <div className="space-y-2">
                  <h4 className="font-bold text-slate-800 uppercase tracking-wider text-[11px]">Billed Services</h4>
                  <div className="border border-slate-200 rounded-xl overflow-hidden divide-y divide-slate-100">
                    {selectedInvoice.items.map((item, idx) => (
                      <div key={idx} className="p-3 flex items-center justify-between gap-3 bg-white">
                        <div>
                          <span className="font-bold text-slate-900 block">{item.description}</span>
                          <span className="text-[10px] text-slate-400">
                            Qty: {item.quantity} × {formatCurrency(item.unitPrice)}
                          </span>
                        </div>
                        <span className="font-bold text-slate-900">{formatCurrency(item.amount)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Payment Receipts */}
              {selectedInvoice.payments && selectedInvoice.payments.length > 0 && (
                <div className="space-y-2">
                  <h4 className="font-bold text-emerald-800 uppercase tracking-wider text-[11px]">Payment Receipts</h4>
                  <div className="border border-emerald-200 bg-emerald-50/30 rounded-xl overflow-hidden divide-y divide-emerald-100">
                    {selectedInvoice.payments.map((p, idx) => (
                      <div key={idx} className="p-3 flex items-center justify-between text-emerald-900">
                        <div>
                          <span className="font-bold block">
                            {formatCurrency(p.amount)} via {p.paymentMode}
                          </span>
                          <span className="text-[10px] text-emerald-700">Ref: {p.referenceNumber || 'N/A'}</span>
                        </div>
                        <span className="text-[11px] font-mono">{p.paymentDate}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="pt-3 border-t border-slate-100 flex items-center justify-end">
              <Button variant="outline" size="sm" onClick={() => setSelectedInvoice(null)}>
                Close
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 4: Request Document from Client */}
      {isRequestDocModalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-xs flex items-end sm:items-center justify-center p-0 sm:p-4 z-50 animate-in fade-in">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-5 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-base font-black text-slate-900 flex items-center gap-2">
                <Plus className="w-5 h-5 text-brand-600" />
                Request Document from Client
              </h3>
              <button
                onClick={() => setIsRequestDocModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 text-sm font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleRequestDocument} className="space-y-4 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Client</label>
                <select
                  value={requestDocForm.clientId}
                  onChange={(e) => setRequestDocForm({ ...requestDocForm, clientId: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg font-bold"
                  required
                >
                  {clients.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.displayName}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Document Request Title</label>
                <input
                  type="text"
                  value={requestDocForm.title}
                  onChange={(e) => setRequestDocForm({ ...requestDocForm, title: e.target.value })}
                  placeholder="e.g. FY 2025-26 Bank Statement (All Accounts)"
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg"
                  required
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Document Type</label>
                <select
                  value={requestDocForm.documentType}
                  onChange={(e) => setRequestDocForm({ ...requestDocForm, documentType: e.target.value })}
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg"
                >
                  <option value="BANK_STATEMENT">Bank Statement</option>
                  <option value="FORM_16">Form 16 / TDS Certificate</option>
                  <option value="GST_INVOICES">Sales / Purchase Register</option>
                  <option value="COMPUTATION">Signed Tax Computation</option>
                  <option value="OTHER">Other Required Document</option>
                </select>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Instructions / Description</label>
                <textarea
                  value={requestDocForm.description}
                  onChange={(e) => setRequestDocForm({ ...requestDocForm, description: e.target.value })}
                  placeholder="Please provide PDF bank statement with passbook front page..."
                  rows={2}
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Due Date</label>
                <input
                  type="date"
                  value={requestDocForm.dueDate}
                  onChange={(e) => setRequestDocForm({ ...requestDocForm, dueDate: e.target.value })}
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg font-mono"
                />
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                <Button variant="outline" size="sm" type="button" onClick={() => setIsRequestDocModalOpen(false)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" type="submit">
                  Send Request to Client
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
