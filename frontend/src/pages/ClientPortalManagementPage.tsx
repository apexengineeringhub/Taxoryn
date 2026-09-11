import React, { useState, useEffect, useMemo, useRef } from 'react';
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
  Percent,
  MessageSquare,
  Send,
  MessageCircle,
  RefreshCw,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { DataTable } from '../components/common/DataTable';
import { useAuth } from '../context/AuthContext';
import { portalApi, clientApi, documentApi, tdsApi, documentRequestApi } from '../api/endpoints';
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
  TdsReturn,
  DocumentRequest,
} from '../types';
import { PortalDocumentRequestsView } from '../components/docrequest/PortalDocumentRequestsView';
import { ClientContextBar } from '../components/common/ClientContextBar';
import clsx from 'clsx';

export const ClientPortalManagementPage: React.FC = () => {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // Active Tab
  const activeTab = searchParams.get('tab') || 'overview';
  const setActiveTab = (tab: string) => {
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set('tab', tab);
    if (isPracticeUser && selectedClientId) {
      nextParams.set('clientId', selectedClientId);
    }
    setSearchParams(nextParams);
  };

  // User Role Detection
  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isClientUser = userRoleCodes.some((r: string) => ['CLIENT_USER', 'CLIENT_ADMIN'].includes(r));
  const isPracticeUser = !isClientUser;

  // Practice state (Single Source of Truth: selectedClientId)
  const [clients, setClients] = useState<Client[]>([]);
  const [selectedClientId, setSelectedClientId] = useState<string>(() => searchParams.get('clientId') || '');
  const [clientUsers, setClientUsers] = useState<ClientPortalUser[]>([]);

  // Portal Data State
  const [dashboard, setDashboard] = useState<ClientPortalDashboard | null>(null);
  const [gstFilings, setGstFilings] = useState<ClientGstStatus[]>([]);
  const [itrReturns, setItrReturns] = useState<ClientItrStatus[]>([]);
  const [tdsReturns, setTdsReturns] = useState<TdsReturn[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [pendingDocRequests, setPendingDocRequests] = useState<ClientDocumentRequest[]>([]);
  const [activeDocRequests, setActiveDocRequests] = useState<DocumentRequest[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Active Request Counter to prevent race conditions on rapid switching
  const activeRequestIdRef = useRef<number>(0);

  // Derived active requests and unified count semantics
  const activeRequestsList = useMemo(() => {
    if (activeDocRequests.length > 0) {
      return activeDocRequests.filter((r) => r.status !== 'CANCELLED');
    }
    if (dashboard?.activeMultiItemRequests && dashboard.activeMultiItemRequests.length > 0) {
      return dashboard.activeMultiItemRequests.filter((r) => r.status !== 'CANCELLED');
    }
    return [];
  }, [activeDocRequests, dashboard?.activeMultiItemRequests]);

  // Request Count = Number of active document requests assigned to the client
  const activeDocRequestsCount = useMemo(() => {
    return activeRequestsList.length + (activeRequestsList.length === 0 ? pendingDocRequests.length : 0);
  }, [activeRequestsList, pendingDocRequests]);

  // Pending Document Count = Number of required document items not yet uploaded/completed
  const totalPendingDocItems = useMemo(() => {
    if (activeRequestsList.length > 0) {
      return (
        activeRequestsList.reduce(
          (acc, req) => acc + (req.pendingItems || 0) + (req.rejectedItems || 0),
          0
        ) + pendingDocRequests.length
      );
    }
    return dashboard?.pendingDocumentsCount ?? pendingDocRequests.length;
  }, [activeRequestsList, dashboard?.pendingDocumentsCount, pendingDocRequests]);

  // Derived selected client
  const selectedClient = useMemo(() => {
    if (!selectedClientId || clients.length === 0) return null;
    return clients.find((c) => c.id === selectedClientId) || null;
  }, [clients, selectedClientId]);

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
    password: '',
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
    category: 'ITR_ACKNOWLEDGEMENT',
    description: '',
  });

  const [requestDocForm, setRequestDocForm] = useState({
    clientId: '',
    title: '',
    description: '',
    documentType: 'BANK_STATEMENT',
    dueDate: '',
  });

  // Messages / Direct Consultation State
  const [messagesList, setMessagesList] = useState<Array<{
    id: string;
    sender: 'CLIENT' | 'CONSULTANT';
    senderName: string;
    text: string;
    timestamp: string;
  }>>([
    {
      id: 'm-1',
      sender: 'CONSULTANT',
      senderName: 'Tax Practitioner Team',
      text: 'Hello! Welcome to your Taxoryn Client Portal. All your GST filings, ITR acknowledgements, and TDS statements are synchronized here. Feel free to reach out if you have any questions.',
      timestamp: '2 hours ago',
    },
    {
      id: 'm-2',
      sender: 'CLIENT',
      senderName: 'You',
      text: 'Thank you! I have uploaded the requested documents under Document Vault.',
      timestamp: '1 hour ago',
    },
    {
      id: 'm-3',
      sender: 'CONSULTANT',
      senderName: 'Tax Practitioner Team',
      text: 'Received, thank you. We are reviewing the files and will update your return filing status shortly.',
      timestamp: 'Just now',
    },
  ]);
  const [newMessageText, setNewMessageText] = useState('');

  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMessageText.trim()) return;
    const msg = {
      id: `m-${Date.now()}`,
      sender: 'CLIENT' as const,
      senderName: user?.firstName ? `${user.firstName} ${user.lastName || ''}`.trim() : 'You',
      text: newMessageText.trim(),
      timestamp: 'Just now',
    };
    setMessagesList((prev) => [...prev, msg]);
    setNewMessageText('');
  };

  // Load clients if practice user
  useEffect(() => {
    if (isPracticeUser) {
      clientApi.getAll({ size: 100 }).then((res) => {
        const clientList = res.content || [];
        setClients(clientList);
        const paramClientId = searchParams.get('clientId');
        if (paramClientId && clientList.some((c) => c.id === paramClientId)) {
          setSelectedClientId(paramClientId);
        } else if (clientList.length > 0) {
          const currentValid = selectedClientId && clientList.some((c) => c.id === selectedClientId);
          const initialId = currentValid ? selectedClientId : clientList[0].id;
          setSelectedClientId(initialId);
          const nextParams = new URLSearchParams(searchParams);
          nextParams.set('tab', activeTab);
          nextParams.set('clientId', initialId);
          setSearchParams(nextParams, { replace: true });
        }
      }).catch((err) => {
        console.error('Failed to load clients list', err);
      });
    }
  }, [isPracticeUser]);

  useEffect(() => {
    const paramClientId = searchParams.get('clientId');
    if (paramClientId && paramClientId !== selectedClientId && clients.some((c) => c.id === paramClientId)) {
      setSelectedClientId(paramClientId);
    }
  }, [searchParams, clients, selectedClientId]);

  // Synchronize dropdown change
  const handleClientChange = (newClientId: string) => {
    if (!newClientId || newClientId === selectedClientId) return;
    setSelectedClientId(newClientId);
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set('tab', activeTab);
    nextParams.set('clientId', newClientId);
    setSearchParams(nextParams);

    // Immediately clear all previous client data to prevent stale data display
    setDashboard(null);
    setGstFilings([]);
    setItrReturns([]);
    setTdsReturns([]);
    setInvoices([]);
    setDocuments([]);
    setPendingDocRequests([]);
    setActiveDocRequests([]);
    setClientUsers([]);
    setLoadError(null);
    setIsLoading(true);
  };

  // Load Portal Data
  const loadPortalData = async (targetClientId?: string) => {
    const clientIdToLoad = isPracticeUser ? (targetClientId || selectedClientId) : undefined;
    if (isPracticeUser && !clientIdToLoad) {
      setIsLoading(false);
      return;
    }

    const requestId = ++activeRequestIdRef.current;
    setIsLoading(true);
    setLoadError(null);

    // Clear previous data buffers before in-flight requests resolve
    setDashboard(null);
    setGstFilings([]);
    setItrReturns([]);
    setTdsReturns([]);
    setInvoices([]);
    setDocuments([]);
    setPendingDocRequests([]);
    setActiveDocRequests([]);
    setClientUsers([]);

    try {
      if (isClientUser) {
        // Logged-in Customer View
        const [dash, gst, itr, invs, docs, pending, docRequests] = await Promise.allSettled([
          portalApi.getDashboard(),
          portalApi.getGstStatus(),
          portalApi.getItrStatus(),
          portalApi.getClientInvoices(),
          portalApi.getClientDocuments(),
          portalApi.getPendingDocuments(),
          documentRequestApi.getPortalRequests(),
        ]);

        if (activeRequestIdRef.current !== requestId) return;

        if (dash.status === 'fulfilled' && dash.value) setDashboard(dash.value);
        if (gst.status === 'fulfilled' && gst.value) setGstFilings(gst.value);
        if (itr.status === 'fulfilled' && itr.value) setItrReturns(itr.value);
        if (invs.status === 'fulfilled' && invs.value) setInvoices(invs.value);
        if (docs.status === 'fulfilled' && docs.value) setDocuments(docs.value);
        if (pending.status === 'fulfilled' && pending.value) setPendingDocRequests(pending.value);
        if (docRequests.status === 'fulfilled' && docRequests.value) {
          const reqs = Array.isArray(docRequests.value)
            ? docRequests.value
            : (docRequests.value as any)?.content || [];
          setActiveDocRequests(reqs);
        }
      } else if (clientIdToLoad) {
        // Practice Preview View
        const [dash, usersRes, docsRes, tdsRes, docRequests] = await Promise.allSettled([
          portalApi.getDashboardPreview(clientIdToLoad),
          portalApi.getClientPortalUsers(clientIdToLoad),
          documentApi.getByClientId ? documentApi.getByClientId(clientIdToLoad) : documentApi.getAll({ clientId: clientIdToLoad }),
          tdsApi.getClientReturnHistory(clientIdToLoad),
          documentRequestApi.getByClient(clientIdToLoad),
        ]);

        if (activeRequestIdRef.current !== requestId) return;

        if (dash.status === 'fulfilled' && dash.value) {
          setDashboard(dash.value);
          setGstFilings(dash.value.latestGstFilings || []);
          setItrReturns(dash.value.latestItrReturns || []);
          setInvoices(dash.value.latestInvoices || []);
          setPendingDocRequests(dash.value.pendingDocumentRequests || []);
        } else if (dash.status === 'rejected') {
          console.error('Failed to load dashboard preview', dash.reason);
          setLoadError('Unable to load client portal dashboard for the selected client.');
        }

        if (usersRes.status === 'fulfilled' && usersRes.value) {
          setClientUsers(usersRes.value);
        }

        if (docsRes.status === 'fulfilled' && docsRes.value) {
          const docList = Array.isArray(docsRes.value)
            ? docsRes.value
            : (docsRes.value as any)?.content || [];
          setDocuments(docList);
        }

        if (tdsRes.status === 'fulfilled' && tdsRes.value) {
          setTdsReturns(tdsRes.value || []);
        }

        if (docRequests.status === 'fulfilled' && docRequests.value) {
          const reqs = Array.isArray(docRequests.value)
            ? docRequests.value
            : (docRequests.value as any)?.content || [];
          setActiveDocRequests(reqs);
        }
      }
    } catch (err) {
      if (activeRequestIdRef.current === requestId) {
        console.error('Failed to load portal data', err);
        setLoadError('Failed to load client details. Please try again.');
      }
    } finally {
      if (activeRequestIdRef.current === requestId) {
        setIsLoading(false);
      }
    }
  };

  useEffect(() => {
    if (isClientUser || selectedClientId) {
      loadPortalData(selectedClientId);
    }
  }, [isClientUser, selectedClientId]);

  // Handle Quick Setup & Resend Portal Invitation
  const [isInviting, setIsInviting] = useState(false);
  const handleQuickSetupInvite = async (clientId: string) => {
    if (!clientId) return;
    setIsInviting(true);
    try {
      await clientApi.resendPortalInvitation(clientId);
      alert('Client portal invitation sent successfully! The client has received a secure activation link.');
      const usersRes = await portalApi.getClientPortalUsers(clientId);
      setClientUsers(usersRes);
    } catch (err: any) {
      alert(`Failed to send invitation: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsInviting(false);
    }
  };

  // Handle Provision User
  const handleProvisionUser = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await portalApi.registerUser(provisionForm);
      alert(`Client portal invitation dispatched successfully for ${provisionForm.email}`);
      setIsProvisionModalOpen(false);
      const targetId = provisionForm.clientId || selectedClientId;
      if (targetId) {
        const usersRes = await portalApi.getClientPortalUsers(targetId);
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
    const targetClientId = isPracticeUser ? selectedClientId : dashboard?.clientId;
    try {
      await portalApi.uploadDocument(
        uploadForm.file,
        {
          title: uploadForm.title || uploadForm.file.name,
          category: uploadForm.category,
          documentType: uploadForm.category,
          description: uploadForm.description,
          clientId: targetClientId,
        },
        selectedDocRequest?.id
      );
      alert('Document uploaded successfully to vault!');
      setIsUploadModalOpen(false);
      setSelectedDocRequest(null);
      setUploadForm({ file: null, title: '', category: 'ITR_ACKNOWLEDGEMENT', description: '' });
      loadPortalData(targetClientId);
    } catch (err: any) {
      alert(`Upload failed: ${err.response?.data?.message || err.message}`);
    }
  };

  // Handle Request Document
  const handleRequestDocument = async (e: React.FormEvent) => {
    e.preventDefault();
    const targetClientId = requestDocForm.clientId || selectedClientId;
    try {
      await portalApi.requestDocument({
        ...requestDocForm,
        clientId: targetClientId,
      });
      alert('Document request sent to client successfully!');
      setIsRequestDocModalOpen(false);
      loadPortalData(targetClientId);
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
    if (['UNDER_REVIEW', 'PARTIALLY_PAID', 'SUBMITTED', 'IN_PROGRESS', 'READY_TO_FILE', 'CHALLANS_ATTACHED'].includes(s)) {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
          <Clock className="w-3 h-3" />
          {s.replace(/_/g, ' ')}
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

  // Active client identity
  const activeClientName = dashboard?.displayName || selectedClient?.displayName || (isPracticeUser ? 'Select a Client' : 'My Client Account');
  const activeClientLegalName = dashboard?.legalName || selectedClient?.legalName;
  const activeClientType = dashboard?.clientType || selectedClient?.clientType || 'BUSINESS CLIENT';
  const activeClientPan = dashboard?.pan || selectedClient?.pan || 'N/A';
  const activeClientGstin = dashboard?.gstin || selectedClient?.gstin || 'Unregistered';
  const activeClientTan = dashboard?.tan || selectedClient?.tan || 'N/A';

  return (
    <div className="space-y-4 sm:space-y-5">
      {/* Error Banner with Retry */}
      {loadError && (
        <div className="bg-rose-50 border border-rose-200 rounded-xl p-3 sm:p-4 flex items-center justify-between text-xs text-rose-800 animate-in fade-in">
          <div className="flex items-center gap-2.5">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <div>
              <p className="font-bold">Unable to load client portal data</p>
              <p className="text-[11px] text-rose-600 mt-0.5">{loadError}</p>
            </div>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => loadPortalData(selectedClientId)}
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
          >
            Retry
          </Button>
        </div>
      )}

      {/* Compact Reusable Client Context Bar */}
      {(dashboard || (isPracticeUser && selectedClient) || clients.length > 0) && (
        <ClientContextBar
          clientName={activeClientName}
          legalName={activeClientLegalName}
          clientType={activeClientType}
          pan={activeClientPan}
          gstin={activeClientGstin}
          tan={activeClientTan}
          isVerified={true}
          assignedPractitionerName={dashboard?.assignedPractitionerName}
          assignedPractitionerPhone={dashboard?.assignedPractitionerPhone}
          isPracticeUser={isPracticeUser}
          clients={clients}
          selectedClientId={selectedClientId}
          onClientChange={handleClientChange}
          onProvisionLogin={() => {
            const target = selectedClient || clients.find((c) => c.id === selectedClientId);
            setProvisionForm({
              clientId: selectedClientId,
              email: target?.email || '',
              password: '',
              firstName: target?.displayName?.split(' ')[0] || 'Client',
              lastName: target?.displayName?.split(' ').slice(1).join(' ') || 'User',
              phone: target?.phone || '',
              role: 'CLIENT_USER',
            });
            setIsProvisionModalOpen(true);
          }}
          onRequestDocument={() => {
            setRequestDocForm({
              clientId: selectedClientId,
              title: '',
              description: '',
              documentType: 'BANK_STATEMENT',
              dueDate: '',
            });
            setIsRequestDocModalOpen(true);
          }}
          isLoading={isLoading}
        />
      )}

      {/* Compact Secondary Navigation Tabs */}
      <div className="flex items-center gap-1.5 sm:gap-2 border-b border-slate-200/90 pb-2 overflow-x-auto scrollbar-none select-none">
        {[
          { id: 'overview', label: 'Overview', icon: Globe },
          { id: 'gst', label: `GST (${gstFilings.length})`, icon: Building2 },
          { id: 'itr', label: `ITR (${itrReturns.length})`, icon: FileSpreadsheet },
          { id: 'tds', label: `TDS (${tdsReturns.length})`, icon: Percent },
          { id: 'invoices', label: `Bills (${invoices.length})`, icon: Receipt },
          { id: 'documents', label: `Documents & Requests (${activeDocRequestsCount})`, icon: FolderLock },
          { id: 'messages', label: 'Messages', icon: MessageSquare },
          ...(isPracticeUser ? [{ id: 'users', label: `Logins (${clientUsers.length})`, icon: KeyRound }] : []),
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={clsx(
                'inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold rounded-xl transition-all shrink-0 cursor-pointer',
                isActive
                  ? 'bg-brand-600 text-white shadow-xs'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
              )}
            >
              <Icon className="w-3.5 h-3.5" />
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
              <p className="text-2xl font-black text-amber-600 mt-1">{totalPendingDocItems}</p>
              <span className="text-[10px] text-slate-400">Action items for client</span>
            </div>
            <div
              onClick={() => setActiveTab('invoices')}
              className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs hover:border-brand-300 transition-colors cursor-pointer group"
            >
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-slate-500 uppercase">Unpaid Invoices</span>
                <span className="text-[10px] font-bold text-brand-600 group-hover:underline">View Bills →</span>
              </div>
              <p className="text-2xl font-black text-rose-600 mt-1">{unpaidCount}</p>
              <span className="text-[10px] text-slate-500 font-semibold">{formatCurrency(outstandingBalance)} balance due</span>
            </div>
          </div>

          {/* Action Required: Multi-Item Document Requests & Legacy Requests */}
          {(activeRequestsList.length > 0 || pendingDocRequests.length > 0) && (
            <Card
              title="🚨 Action Required: Pending Document Uploads"
              subtitle="Please upload required tax and compliance documents to avoid delays"
              className="border-amber-200 bg-amber-50/20 shadow-2xs"
            >
              <div className="space-y-3">
                {activeRequestsList.map((req) => (
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
                        {((req.pendingItems || 0) + (req.rejectedItems || 0)) > 0
                          ? `${(req.pendingItems || 0) + (req.rejectedItems || 0)} items remaining to upload`
                          : req.items
                          ? `${req.items.filter((i) => i.status === 'PENDING' || i.status === 'REJECTED').length} items remaining to upload`
                          : 'Pending documents required'}
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

      {/* TAB: TDS Statements & Tax Credits */}
      {activeTab === 'tds' && (
        <div className="space-y-6">
          {/* TDS Profile & 26AS/AIS Summary */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-2xs">
              <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider block">Tax Deduction Account (TAN)</span>
              <span className="text-xl font-black text-slate-900 font-mono mt-1 block">
                {activeClientTan !== 'N/A' ? activeClientTan : (dashboard?.tan || 'Registered / On File')}
              </span>
              <span className="text-xs text-slate-400 mt-1 block">Deductor & Collection Account</span>
            </div>
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-2xs">
              <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider block">Form 26AS & AIS Reconciliation</span>
              <span className="text-xl font-black text-emerald-600 mt-1 flex items-center gap-1.5">
                <CheckCircle2 className="w-5 h-5 text-emerald-500" />
                Synchronized
              </span>
              <span className="text-xs text-slate-400 mt-1 block">Prepaid tax credits matched with TRACES</span>
            </div>
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-2xs">
              <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider block">Form 16 / 16A Certificates</span>
              <span className="text-xl font-black text-brand-600 mt-1 block">Available in Vault</span>
              <span className="text-xs text-slate-400 mt-1 block">Download digitally signed TDS certificates</span>
            </div>
          </div>

          {/* TDS Compliance Overview Table */}
          <Card
            title="TDS Statements & Quarterly Filing Status"
            subtitle="Quarterly TDS returns (Form 24Q - Salary, Form 26Q - Non-Salary, Form 27Q - NRI)"
          >
            {tdsReturns.length === 0 ? (
              <div className="py-8 text-center text-xs text-slate-400">
                No quarterly TDS returns filed or recorded for this client.
              </div>
            ) : (
              <div className="space-y-3">
                {tdsReturns.map((tdsItem) => (
                  <div
                    key={tdsItem.id}
                    className="p-4 bg-slate-50 rounded-xl border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-slate-900">
                          {tdsItem.quarter} (FY {tdsItem.financialYear || '2025-26'})
                        </span>
                        <span className="px-2 py-0.5 rounded bg-brand-50 text-brand-700 font-mono font-bold text-[11px]">
                          {(tdsItem.formType || 'FORM_26Q').replace('_', ' ')}
                        </span>
                      </div>
                      <span className="text-slate-500 block">
                        Tax Deducted: {formatCurrency(tdsItem.totalTaxDeducted)} • Tax Deposited: {formatCurrency(tdsItem.totalTaxDeposited)}
                      </span>
                      {(tdsItem.tokenNumber || tdsItem.receiptNumber) && (
                        <span className="text-[11px] text-emerald-700 font-mono block">
                          TRACES Token / Ack: {tdsItem.tokenNumber || tdsItem.receiptNumber}
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-3">
                      <div className="text-right">
                        {tdsItem.dueDate && (
                          <span className="text-[10px] text-slate-400 block">Due Date: {tdsItem.dueDate}</span>
                        )}
                        {renderStatusBadge(tdsItem.filingStatus)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
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

      {/* TAB: Messages / Consultation Chat */}
      {activeTab === 'messages' && (
        <div className="space-y-6">
          <Card
            title="Direct Consultation & Messages"
            subtitle={`Direct secure communication channel with ${dashboard?.assignedPractitionerName || 'your assigned Tax Consultant'}`}
          >
            <div className="space-y-4">
              {/* Consultant Header */}
              <div className="flex items-center justify-between p-4 bg-slate-50 rounded-xl border border-slate-200">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-brand-600 text-white flex items-center justify-center font-bold text-sm">
                    {(dashboard?.assignedPractitionerName || 'CA').charAt(0)}
                  </div>
                  <div>
                    <span className="font-bold text-slate-900 block text-sm">
                      {dashboard?.assignedPractitionerName || 'Assigned Tax Consultant'}
                    </span>
                    <span className="text-xs text-emerald-600 flex items-center gap-1 font-medium">
                      <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                      Direct Practice Channel Active
                    </span>
                  </div>
                </div>
                {dashboard?.assignedPractitionerPhone && (
                  <a
                    href={`tel:${dashboard.assignedPractitionerPhone}`}
                    className="inline-flex items-center gap-1.5 text-xs font-bold text-brand-600 bg-brand-50 hover:bg-brand-100 border border-brand-200 px-3 py-1.5 rounded-lg transition-colors"
                  >
                    <Phone className="w-3.5 h-3.5" />
                    Call Consultant
                  </a>
                )}
              </div>

              {/* Chat Thread */}
              <div className="border border-slate-200 rounded-xl p-4 min-h-[280px] max-h-[420px] overflow-y-auto space-y-3 bg-slate-50/50">
                {messagesList.map((msg) => {
                  const isMe = msg.sender === 'CLIENT';
                  return (
                    <div
                      key={msg.id}
                      className={clsx('flex flex-col', isMe ? 'items-end' : 'items-start')}
                    >
                      <span className="text-[10px] text-slate-400 mb-1 px-1">
                        {msg.senderName} • {msg.timestamp}
                      </span>
                      <div
                        className={clsx(
                          'max-w-md p-3.5 rounded-2xl text-xs shadow-2xs leading-relaxed',
                          isMe
                            ? 'bg-brand-600 text-white rounded-br-none'
                            : 'bg-white text-slate-800 border border-slate-200 rounded-bl-none'
                        )}
                      >
                        {msg.text}
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Message Composer */}
              <form onSubmit={handleSendMessage} className="flex gap-2">
                <input
                  type="text"
                  value={newMessageText}
                  onChange={(e) => setNewMessageText(e.target.value)}
                  placeholder={`Type your query to ${dashboard?.assignedPractitionerName || 'your tax consultant'}...`}
                  className="flex-1 px-4 py-2.5 text-xs bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
                />
                <Button
                  type="submit"
                  variant="primary"
                  size="sm"
                  leftIcon={<Send className="w-4 h-4" />}
                >
                  Send
                </Button>
              </form>
            </div>
          </Card>
        </div>
      )}

      {/* TAB 6: Portal Users & Logins (For Practice Admins) */}
      {activeTab === 'users' && isPracticeUser && (
        <Card
          title="Client Portal Logins & Credentials"
          subtitle="Customer users who can log into this client portal"
          action={
            <div className="flex items-center gap-2">
              <Button
                variant="primary"
                size="sm"
                isLoading={isInviting}
                onClick={() => handleQuickSetupInvite(selectedClientId)}
                leftIcon={<Send className="w-4 h-4" />}
              >
                Set Up Portal & Send Invite
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  const target = clients.find((c) => c.id === selectedClientId);
                  setProvisionForm({
                    clientId: selectedClientId,
                    email: target?.email || '',
                    password: '',
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
            </div>
          }
        >
          {clientUsers.length === 0 ? (
            <div className="py-12 text-center space-y-4">
              <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center text-slate-400 mx-auto">
                <KeyRound className="w-6 h-6" />
              </div>
              <div className="space-y-1">
                <p className="text-sm font-bold text-slate-800">
                  No portal login credentials provisioned yet
                </p>
                <p className="text-xs text-slate-500 max-w-sm mx-auto">
                  Send a secure activation invite to allow the client to set their password, or provision a user manually.
                </p>
              </div>
              <div className="flex items-center justify-center gap-2 pt-2">
                <Button
                  variant="primary"
                  size="sm"
                  isLoading={isInviting}
                  onClick={() => handleQuickSetupInvite(selectedClientId)}
                  leftIcon={<Send className="w-4 h-4" />}
                >
                  Set Up Portal & Send Invite
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    const target = clients.find((c) => c.id === selectedClientId);
                    setProvisionForm({
                      clientId: selectedClientId,
                      email: target?.email || '',
                      password: '',
                      firstName: target?.displayName?.split(' ')[0] || 'Client',
                      lastName: target?.displayName?.split(' ').slice(1).join(' ') || 'User',
                      phone: target?.phone || '',
                      role: 'CLIENT_USER',
                    });
                    setIsProvisionModalOpen(true);
                  }}
                  leftIcon={<Plus className="w-4 h-4" />}
                >
                  Manual Provisioning
                </Button>
              </div>
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
                <label className="block font-bold text-slate-700 mb-1">Initial Password (Optional)</label>
                <input
                  type="text"
                  value={provisionForm.password}
                  onChange={(e) => setProvisionForm({ ...provisionForm, password: e.target.value })}
                  placeholder="Leave blank to send 24h activation email"
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg font-mono"
                />
                <span className="text-[10px] text-slate-400 mt-1 block">
                  If left blank, a secure activation link will be emailed to the client to set their password.
                </span>
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
                  Send Invite / Create User
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
                  <option value="ITR_ACKNOWLEDGEMENT">Signed Tax Returns (ITR)</option>
                  <option value="FORM_16">Form 16 / TDS Certificate</option>
                  <option value="FORM_26AS">Form 26AS / AIS / TIS</option>
                  <option value="BANK_STATEMENT">Bank Statement</option>
                  <option value="GST_INVOICE_SALE">Sales Invoices (GST)</option>
                  <option value="GST_INVOICE_PURCHASE">Purchase Invoices / Bills</option>
                  <option value="PAN_CARD">PAN Card</option>
                  <option value="AADHAAR_CARD">Aadhaar Card</option>
                  <option value="FINANCIAL_STATEMENTS">Financial Statements / P&L</option>
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
