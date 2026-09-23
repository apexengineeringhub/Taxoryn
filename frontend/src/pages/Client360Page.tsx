import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Building2,
  User,
  ShieldCheck,
  CreditCard,
  FileText,
  CheckCircle2,
  Clock,
  AlertTriangle,
  FileSpreadsheet,
  Download,
  Plus,
  Send,
  Calendar,
  Layers,
  MessageSquare,
  Activity,
  History,
  Lock,
  ExternalLink,
  ChevronRight,
  Sparkles,
  DollarSign,
  AlertCircle,
  Briefcase,
  Phone,
  Mail,
  RefreshCw,
  Tag,
  Check,
  X,
  XCircle,
  PlayCircle,
  PauseCircle,
  CheckCircle,
} from 'lucide-react';
import { clientApi, clientServicesApi, complianceWorkApi, employeeApi } from '../api/endpoints';
import { useAuth } from '../context/AuthContext';
import { useModuleEntitlement } from '../context/ModuleEntitlementContext';
import {
  Client360Overview,
  ClientNote,
  ClientServiceDto,
  ServiceCatalogItem,
  ClientServiceType,
  ClientServiceStatus,
  ComplianceWorkItem,
  Employee,
} from '../types';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { ClientDocumentRequestsTab } from '../components/docrequest/ClientDocumentRequestsTab';
import clsx from 'clsx';

export const Client360Page: React.FC = () => {
  const { clientId } = useParams<{ clientId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { isModuleAvailable } = useModuleEntitlement();

  const [overview, setOverview] = useState<Client360Overview | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<
    'overview' | 'services' | 'compliance' | 'documents' | 'doc_requests' | 'tasks' | 'notices' | 'billing' | 'activity'
  >('overview');

  // Services State
  const [clientServices, setClientServices] = useState<ClientServiceDto[]>([]);
  const [catalog, setCatalog] = useState<ServiceCatalogItem[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [isLoadingServices, setIsLoadingServices] = useState(false);
  const [isServiceModalOpen, setIsServiceModalOpen] = useState(false);
  const [isSubmittingService, setIsSubmittingService] = useState(false);
  const [serviceStatusFilter, setServiceStatusFilter] = useState<'ALL' | 'ACTIVE' | 'SUSPENDED' | 'COMPLETED' | 'INACTIVE'>('ALL');

  // Compliance Work Items State
  const [complianceWorkItems, setComplianceWorkItems] = useState<ComplianceWorkItem[]>([]);
  const [isLoadingComplianceWork, setIsLoadingComplianceWork] = useState(false);

  // New Service Form State
  const [newServiceType, setNewServiceType] = useState<ClientServiceType>('GST_COMPLIANCE');
  const [newServiceName, setNewServiceName] = useState('');
  const [newAssignedEmployeeId, setNewAssignedEmployeeId] = useState('');
  const [newBillingCycle, setNewBillingCycle] = useState<'MONTHLY' | 'QUARTERLY' | 'ANNUAL' | 'ONE_TIME'>('MONTHLY');
  const [newAgreedFee, setNewAgreedFee] = useState('');
  const [newStartDate, setNewStartDate] = useState(new Date().toISOString().split('T')[0]);
  const [newEndDate, setNewEndDate] = useState('');
  const [newEngagementNotes, setNewEngagementNotes] = useState('');

  // Note Modal State
  const [isNoteModalOpen, setIsNoteModalOpen] = useState(false);
  const [noteTitle, setNoteTitle] = useState('');
  const [noteContent, setNoteContent] = useState('');
  const [noteType, setNoteType] = useState('GENERAL');
  const [isSubmittingNote, setIsSubmittingNote] = useState(false);

  useEffect(() => {
    if (clientId) {
      loadClientOverview();
      loadServices();
      loadComplianceWork();
    }
  }, [clientId]);

  const loadComplianceWork = async () => {
    if (!clientId) return;
    try {
      setIsLoadingComplianceWork(true);
      const data = await complianceWorkApi.getByClientId(clientId);
      setComplianceWorkItems(data || []);
    } catch (err: any) {
      console.warn('Failed to load compliance work items for client', err);
    } finally {
      setIsLoadingComplianceWork(false);
    }
  };

  const loadClientOverview = async () => {
    if (!clientId) return;
    try {
      setIsLoading(true);
      setError(null);
      const data = await clientApi.getClient360(clientId);
      setOverview(data);
    } catch (err: any) {
      console.error('Failed to load client 360 overview', err);
      setError(err.response?.data?.message || 'Failed to load client profile details.');
    } finally {
      setIsLoading(false);
    }
  };

  const loadServices = async () => {
    if (!clientId) return;
    try {
      setIsLoadingServices(true);
      const [servicesData, catalogData, empRes] = await Promise.all([
        clientServicesApi.getByClientId(clientId).catch(() => []),
        clientServicesApi.getCatalog().catch(() => []),
        employeeApi.getAll({ size: 100 }).catch(() => ({ content: [] })),
      ]);
      setClientServices(servicesData || []);
      setCatalog(catalogData || []);
      setEmployees(empRes?.content || []);
    } catch (err: any) {
      console.error('Failed to load client services engagement data', err);
    } finally {
      setIsLoadingServices(false);
    }
  };

  const resetServiceForm = () => {
    setNewServiceType('GST_COMPLIANCE');
    setNewServiceName('');
    setNewAssignedEmployeeId('');
    setNewBillingCycle('MONTHLY');
    setNewAgreedFee('');
    setNewStartDate(new Date().toISOString().split('T')[0]);
    setNewEndDate('');
    setNewEngagementNotes('');
  };

  const handleCreateService = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!clientId || !newServiceType) return;
    try {
      setIsSubmittingService(true);
      await clientServicesApi.create(clientId, {
        serviceType: newServiceType,
        serviceName: newServiceName.trim() || undefined,
        assignedEmployeeId: newAssignedEmployeeId || undefined,
        billingCycle: newBillingCycle,
        agreedFee: newAgreedFee ? parseFloat(newAgreedFee) : undefined,
        startDate: newStartDate || undefined,
        endDate: newEndDate || undefined,
        engagementNotes: newEngagementNotes.trim() || undefined,
      });
      setIsServiceModalOpen(false);
      resetServiceForm();
      await Promise.all([loadServices(), loadClientOverview()]);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to engage service');
    } finally {
      setIsSubmittingService(false);
    }
  };

  const handleUpdateServiceStatus = async (serviceId: string, status: string) => {
    if (!clientId) return;
    try {
      await clientServicesApi.updateStatus(clientId, serviceId, status);
      await Promise.all([loadServices(), loadClientOverview()]);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update service status');
    }
  };

  const handleDeactivateService = async (serviceId: string, serviceName: string) => {
    if (!clientId) return;
    if (!window.confirm(`Are you sure you want to deactivate '${serviceName}' for this client?`)) return;
    try {
      await clientServicesApi.deactivate(clientId, serviceId);
      await Promise.all([loadServices(), loadClientOverview()]);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to deactivate service');
    }
  };

  const handleAddNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!clientId || !noteTitle.trim() || !noteContent.trim()) return;

    try {
      setIsSubmittingNote(true);
      await clientApi.addNote(clientId, {
        title: noteTitle.trim(),
        content: noteContent.trim(),
        noteType,
      });
      setIsNoteModalOpen(false);
      setNoteTitle('');
      setNoteContent('');
      setNoteType('GENERAL');
      await loadClientOverview();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to record communication note');
    } finally {
      setIsSubmittingNote(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center space-y-4">
        <div className="w-10 h-10 border-4 border-brand-500 border-t-transparent rounded-full animate-spin" />
        <p className="text-xs font-bold text-slate-500 tracking-wide uppercase">
          Aggregating Client 360° Data...
        </p>
      </div>
    );
  }

  if (error || !overview) {
    return (
      <div className="min-h-[50vh] flex flex-col items-center justify-center p-6 text-center">
        <div className="w-12 h-12 bg-rose-50 text-rose-600 rounded-full flex items-center justify-center mb-3">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <h2 className="text-base font-bold text-slate-900 mb-1">Unable to Load Client Profile</h2>
        <p className="text-xs text-slate-500 max-w-md mb-4">{error || 'Client record not found or access denied.'}</p>
        <Link to="/clients">
          <Button variant="outline" size="sm" leftIcon={<ArrowLeft className="w-4 h-4" />}>
            Back to Clients Directory
          </Button>
        </Link>
      </div>
    );
  }

  const { client, statutory, services, taskSummary, complianceSummary, documentsSummary, docRequestsSummary, billingSummary, noticeSummary, recentNotes, activityTimeline } = overview;

  return (
    <div className="space-y-6">
      {/* Top Breadcrumb & Actions Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Link
            to="/clients"
            className="p-1.5 rounded-lg border border-slate-200 bg-white text-slate-600 hover:text-slate-900 hover:bg-slate-50 transition-colors"
            title="Back to Clients"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-slate-400">Clients</span>
              <ChevronRight className="w-3.5 h-3.5 text-slate-300" />
              <span className="text-xs font-bold text-slate-700">{client.displayName}</span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            size="sm"
            variant="outline"
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            onClick={loadClientOverview}
          >
            Refresh
          </Button>
          <Button
            size="sm"
            variant="primary"
            leftIcon={<MessageSquare className="w-3.5 h-3.5" />}
            onClick={() => setIsNoteModalOpen(true)}
          >
            Add Note
          </Button>
        </div>
      </div>

      {/* Hero Client Master Banner */}
      <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs relative overflow-hidden">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="flex items-start gap-4">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-brand-50 to-brand-100 border border-brand-200 text-brand-700 flex items-center justify-center font-black text-xl shrink-0 shadow-2xs">
              {client.clientType === 'INDIVIDUAL' ? <User className="w-7 h-7" /> : <Building2 className="w-7 h-7" />}
            </div>
            <div className="space-y-1.5">
              <div className="flex flex-wrap items-center gap-2.5">
                <h1 className="text-xl font-black text-slate-900 tracking-tight">{client.displayName}</h1>
                <StatusBadge status={client.status} size="sm" />
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700 border border-slate-200">
                  {client.clientType.replace(/_/g, ' ')}
                </span>
                {client.portalStatus === 'ACTIVE' && (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                    <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                    Portal Active
                  </span>
                )}
              </div>
              {client.legalName && (
                <p className="text-xs text-slate-500 font-medium">
                  Legal Name: <span className="font-semibold text-slate-700">{client.legalName}</span>
                </p>
              )}
              {/* Statutory Quick Chips */}
              <div className="flex flex-wrap items-center gap-2 pt-1">
                {statutory?.pan && (
                  <span className="inline-flex items-center gap-1 font-mono text-[11px] font-bold bg-slate-50 px-2 py-0.5 rounded border border-slate-200 text-slate-800">
                    <span className="text-slate-400 font-normal">PAN:</span> {statutory.pan}
                  </span>
                )}
                {statutory?.gstin && (
                  <span className="inline-flex items-center gap-1 font-mono text-[11px] font-bold bg-slate-50 px-2 py-0.5 rounded border border-slate-200 text-slate-800">
                    <span className="text-slate-400 font-normal">GSTIN:</span> {statutory.gstin}
                  </span>
                )}
                {statutory?.tan && (
                  <span className="inline-flex items-center gap-1 font-mono text-[11px] font-bold bg-slate-50 px-2 py-0.5 rounded border border-slate-200 text-slate-800">
                    <span className="text-slate-400 font-normal">TAN:</span> {statutory.tan}
                  </span>
                )}
                {statutory?.cin && (
                  <span className="inline-flex items-center gap-1 font-mono text-[11px] font-bold bg-slate-50 px-2 py-0.5 rounded border border-slate-200 text-slate-800">
                    <span className="text-slate-400 font-normal">CIN:</span> {statutory.cin}
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* Quick Context Stats */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-slate-50/80 p-3.5 rounded-xl border border-slate-200/80">
            <div className="space-y-0.5">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Open Tasks</span>
              <span className="text-base font-black text-slate-900">
                {(taskSummary.pendingTasks || 0) + (taskSummary.inProgressTasks || 0) + (taskSummary.underReviewTasks || 0)}
              </span>
            </div>
            <div className="space-y-0.5">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Doc Vault</span>
              <span className="text-base font-black text-slate-900">{documentsSummary.totalDocuments || 0}</span>
            </div>
            <div className="space-y-0.5">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Notices</span>
              <span className={clsx("text-base font-black", noticeSummary.activeNotices > 0 ? "text-amber-600" : "text-slate-900")}>
                {noticeSummary.activeNotices || 0}
              </span>
            </div>
            <div className="space-y-0.5">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Balance</span>
              <span className={clsx("text-base font-black", billingSummary && billingSummary.outstandingBalance > 0 ? "text-rose-600" : "text-slate-900")}>
                {billingSummary ? `₹${billingSummary.outstandingBalance.toLocaleString('en-IN')}` : '—'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Tab Navigation Navigation Bar */}
      <div className="border-b border-slate-200 bg-white rounded-xl shadow-2xs px-2 flex items-center gap-1 overflow-x-auto no-scrollbar">
        {[
          { id: 'overview', label: '360° Overview', icon: Layers },
          { id: 'services', label: 'Services', icon: Briefcase, count: services?.length },
          { id: 'compliance', label: 'Tax Compliance', icon: ShieldCheck },
          { id: 'documents', label: 'Document Vault', icon: FileText, count: documentsSummary?.totalDocuments },
          { id: 'doc_requests', label: 'Doc Requests', icon: Send, count: docRequestsSummary?.pendingRequests },
          { id: 'tasks', label: 'Workflow Tasks', icon: CheckCircle2, count: taskSummary?.totalTasks },
          { id: 'notices', label: 'Tax Notices', icon: AlertCircle, count: noticeSummary?.activeNotices },
          { id: 'billing', label: 'Billing & Invoices', icon: CreditCard },
          { id: 'activity', label: 'Activity & Notes', icon: History, count: recentNotes?.length },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={clsx(
                'flex items-center gap-2 px-3.5 py-3 text-xs font-bold border-b-2 transition-all whitespace-nowrap',
                isActive
                  ? 'border-brand-600 text-brand-700 bg-brand-50/50'
                  : 'border-transparent text-slate-500 hover:text-slate-800 hover:bg-slate-50'
              )}
            >
              <Icon className={clsx('w-4 h-4', isActive ? 'text-brand-600' : 'text-slate-400')} />
              <span>{tab.label}</span>
              {typeof tab.count === 'number' && tab.count > 0 && (
                <span
                  className={clsx(
                    'text-[10px] px-1.5 py-0.2 rounded-full font-extrabold',
                    isActive ? 'bg-brand-200/80 text-brand-900' : 'bg-slate-100 text-slate-600'
                  )}
                >
                  {tab.count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Tab Content Panes */}
      <div className="space-y-6">
        {/* 1. OVERVIEW TAB */}
        {activeTab === 'overview' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Left 2 Cols: Compliance Health & Services */}
            <div className="lg:col-span-2 space-y-6">
              {/* Compliance Status Cards */}
              <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-2xs space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                    <ShieldCheck className="w-4 h-4 text-brand-600" />
                    <span>Compliance Health & Filing Pulse</span>
                  </h3>
                  <button
                    onClick={() => setActiveTab('compliance')}
                    className="text-xs font-bold text-brand-600 hover:text-brand-700 flex items-center gap-1"
                  >
                    <span>View Matrix</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5">
                  {/* GST Block */}
                  <div className="p-3.5 rounded-xl border border-slate-200/80 bg-slate-50/50 space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-800">GST Filing</span>
                      <span
                        className={clsx(
                          'text-[10px] font-bold px-2 py-0.5 rounded-full',
                          complianceSummary.gstDetails?.overdueFilings ? 'bg-rose-100 text-rose-700' : 'bg-emerald-100 text-emerald-800'
                        )}
                      >
                        {complianceSummary.gstDetails?.registered ? 'Enrolled' : 'Not Enrolled'}
                      </span>
                    </div>
                    <p className="text-xs text-slate-600 font-medium">{complianceSummary.gstStatus}</p>
                    {complianceSummary.gstDetails?.nextDueDate && (
                      <p className="text-[11px] text-slate-400">
                        Next: <span className="font-semibold text-slate-700">{complianceSummary.gstDetails.nextReturnType}</span> by {complianceSummary.gstDetails.nextDueDate}
                      </p>
                    )}
                  </div>

                  {/* ITR Block */}
                  <div className="p-3.5 rounded-xl border border-slate-200/80 bg-slate-50/50 space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-800">Income Tax (ITR)</span>
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-sky-100 text-sky-800">
                        {complianceSummary.itrDetails?.registered ? 'Configured' : 'No PAN'}
                      </span>
                    </div>
                    <p className="text-xs text-slate-600 font-medium">{complianceSummary.itrStatus}</p>
                    {complianceSummary.itrDetails?.currentAssessmentYear && (
                      <p className="text-[11px] text-slate-400">
                        AY: <span className="font-semibold text-slate-700">{complianceSummary.itrDetails.currentAssessmentYear}</span>
                      </p>
                    )}
                  </div>

                  {/* TDS Block */}
                  <div className="p-3.5 rounded-xl border border-slate-200/80 bg-slate-50/50 space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-800">TDS & TCS</span>
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-purple-100 text-purple-800">
                        {complianceSummary.tdsDetails?.registered ? 'TAN Active' : 'No TAN'}
                      </span>
                    </div>
                    <p className="text-xs text-slate-600 font-medium">{complianceSummary.tdsStatus}</p>
                    {complianceSummary.tdsDetails?.currentQuarter && (
                      <p className="text-[11px] text-slate-400">
                        Quarter: <span className="font-semibold text-slate-700">{complianceSummary.tdsDetails.currentQuarter}</span>
                      </p>
                    )}
                  </div>
                </div>
              </div>

              {/* Active Engagement Services */}
              <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-2xs space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                    <Briefcase className="w-4 h-4 text-brand-600" />
                    <span>Active Services & Engagements</span>
                  </h3>
                  <button
                    onClick={() => setActiveTab('services')}
                    className="text-xs font-bold text-brand-600 hover:text-brand-700 flex items-center gap-1"
                  >
                    <span>All Services ({services?.length || 0})</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {(services || []).map((srv) => (
                    <div
                      key={srv.serviceCode}
                      className="p-3.5 rounded-xl border border-slate-200 bg-white hover:border-brand-300 transition-colors flex items-start justify-between gap-3"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold text-slate-900">{srv.serviceName}</span>
                          <span className="text-[10px] font-bold px-1.5 py-0.2 rounded bg-emerald-50 text-emerald-700 border border-emerald-200">
                            {srv.status}
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500">{srv.summary}</p>
                        {srv.identifier && (
                          <span className="font-mono text-[10px] text-slate-600 bg-slate-100 px-1.5 py-0.5 rounded">
                            {srv.identifier}
                          </span>
                        )}
                      </div>
                      {srv.routePath && (
                        <Link
                          to={srv.routePath}
                          className="p-1.5 text-slate-400 hover:text-brand-600 hover:bg-slate-50 rounded-lg shrink-0"
                          title="Open Module"
                        >
                          <ExternalLink className="w-3.5 h-3.5" />
                        </Link>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              {/* Recent Tasks Deliverables */}
              <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-2xs space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-brand-600" />
                    <span>Recent Client Tasks</span>
                  </h3>
                  <button
                    onClick={() => setActiveTab('tasks')}
                    className="text-xs font-bold text-brand-600 hover:text-brand-700 flex items-center gap-1"
                  >
                    <span>View All Tasks ({taskSummary.totalTasks})</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>

                {(taskSummary.recentTasks || []).length === 0 ? (
                  <p className="text-xs text-slate-400 py-4 text-center">No active tasks created for this client yet.</p>
                ) : (
                  <div className="divide-y divide-slate-100">
                    {(taskSummary.recentTasks || []).map((t) => (
                      <div key={t.id} className="py-2.5 flex items-center justify-between gap-3">
                        <div className="space-y-0.5">
                          <p className="text-xs font-bold text-slate-900">{t.title}</p>
                          <div className="flex items-center gap-2 text-[11px] text-slate-400">
                            <span>Due: {t.dueDate || 'No Date'}</span>
                            <span>•</span>
                            <span>Category: {t.category || 'General'}</span>
                          </div>
                        </div>
                        <StatusBadge status={t.status} size="sm" />
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Right Column: Contact & Statutory & Notes Card */}
            <div className="space-y-6">
              {/* Statutory & Master Details */}
              <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-2xs space-y-4">
                <h3 className="text-sm font-bold text-slate-900">Statutory & Contact Profile</h3>
                <div className="space-y-3 text-xs">
                  <div>
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Email Address</span>
                    <span className="font-medium text-slate-800 flex items-center gap-1.5 mt-0.5">
                      <Mail className="w-3.5 h-3.5 text-slate-400" />
                      {client.email || 'Not Provided'}
                    </span>
                  </div>

                  <div>
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Phone Number</span>
                    <span className="font-medium text-slate-800 flex items-center gap-1.5 mt-0.5">
                      <Phone className="w-3.5 h-3.5 text-slate-400" />
                      {client.phone || 'Not Provided'}
                    </span>
                  </div>

                  <div>
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Assigned Practitioner</span>
                    <span className="font-medium text-slate-800 flex items-center gap-1.5 mt-0.5">
                      <User className="w-3.5 h-3.5 text-brand-600" />
                      {client.assignedEmployeeName || 'Unassigned (Firm Pool)'}
                    </span>
                  </div>

                  <div className="pt-2 border-t border-slate-100">
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Date of Incorporation</span>
                    <span className="font-medium text-slate-800 mt-0.5 block">
                      {statutory?.dateOfIncorporation || 'Not on Record'}
                    </span>
                  </div>
                </div>
              </div>

              {/* Recent Communication Notes Preview */}
              <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-2xs space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                    <MessageSquare className="w-4 h-4 text-brand-600" />
                    <span>Recent Interaction Notes</span>
                  </h3>
                  <button
                    onClick={() => setIsNoteModalOpen(true)}
                    className="p-1 text-brand-600 hover:bg-brand-50 rounded"
                    title="Add Note"
                  >
                    <Plus className="w-4 h-4" />
                  </button>
                </div>

                {(recentNotes || []).length === 0 ? (
                  <p className="text-xs text-slate-400 py-3 text-center">No notes recorded yet.</p>
                ) : (
                  <div className="space-y-2.5">
                    {(recentNotes || []).slice(0, 3).map((n) => (
                      <div key={n.id} className="p-2.5 rounded-xl bg-slate-50 border border-slate-100 space-y-1">
                        <div className="flex items-center justify-between text-[10px]">
                          <span className="font-bold text-slate-900">{n.title}</span>
                          <span className="text-slate-400">{n.createdAt ? new Date(n.createdAt).toLocaleDateString() : ''}</span>
                        </div>
                        <p className="text-[11px] text-slate-600 line-clamp-2">{n.content}</p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* 2. SERVICES TAB */}
        {activeTab === 'services' && (
          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs space-y-6">
            {/* Services Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-5">
              <div>
                <h2 className="text-base font-black text-slate-900 flex items-center gap-2">
                  <Briefcase className="w-5 h-5 text-brand-600" />
                  <span>Client Service Engagements & Master</span>
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Normalized service engagements, operational assignees, billing parameters, and module links for {client.displayName}.
                </p>
              </div>

              <div className="flex items-center gap-2.5">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={loadServices}
                  disabled={isLoadingServices}
                  leftIcon={<RefreshCw className={clsx('w-3.5 h-3.5', isLoadingServices && 'animate-spin')} />}
                >
                  Refresh
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  leftIcon={<Plus className="w-3.5 h-3.5" />}
                  onClick={() => setIsServiceModalOpen(true)}
                >
                  Engage New Service
                </Button>
              </div>
            </div>

            {/* Filter Pills */}
            <div className="flex items-center gap-2 overflow-x-auto pb-1">
              {(['ALL', 'ACTIVE', 'SUSPENDED', 'COMPLETED', 'INACTIVE'] as const).map((filter) => {
                const count =
                  filter === 'ALL'
                    ? clientServices.length
                    : clientServices.filter((s) => s.status === filter).length;
                return (
                  <button
                    key={filter}
                    onClick={() => setServiceStatusFilter(filter)}
                    className={clsx(
                      'px-3 py-1.5 rounded-lg text-xs font-bold transition-colors whitespace-nowrap flex items-center gap-1.5',
                      serviceStatusFilter === filter
                        ? 'bg-brand-50 text-brand-700 border border-brand-200'
                        : 'bg-slate-50 text-slate-600 hover:bg-slate-100 border border-transparent'
                    )}
                  >
                    <span>{filter.charAt(0) + filter.slice(1).toLowerCase()}</span>
                    <span
                      className={clsx(
                        'text-[10px] px-1.5 py-0.2 rounded-full font-bold',
                        serviceStatusFilter === filter
                          ? 'bg-brand-200 text-brand-800'
                          : 'bg-slate-200 text-slate-600'
                      )}
                    >
                      {count}
                    </span>
                  </button>
                );
              })}
            </div>

            {/* Services Grid */}
            {isLoadingServices ? (
              <div className="py-12 flex flex-col items-center justify-center space-y-3">
                <div className="w-8 h-8 border-3 border-brand-500 border-t-transparent rounded-full animate-spin" />
                <p className="text-xs text-slate-500">Loading service engagements...</p>
              </div>
            ) : clientServices.filter(
                (s) => serviceStatusFilter === 'ALL' || s.status === serviceStatusFilter
              ).length === 0 ? (
              <div className="py-12 text-center border-2 border-dashed border-slate-200 rounded-2xl p-6">
                <Briefcase className="w-10 h-10 text-slate-300 mx-auto mb-2" />
                <h3 className="text-sm font-bold text-slate-900 mb-1">
                  {serviceStatusFilter === 'ALL'
                    ? 'No Services Configured'
                    : `No ${serviceStatusFilter.toLowerCase()} services found`}
                </h3>
                <p className="text-xs text-slate-500 max-w-sm mx-auto mb-4">
                  {serviceStatusFilter === 'ALL'
                    ? `Engage ${client.displayName} for tax compliance, accounting, audit, or advisory services.`
                    : `There are currently no engagements matching status '${serviceStatusFilter}'.`}
                </p>
                {serviceStatusFilter === 'ALL' && (
                  <Button
                    size="sm"
                    variant="primary"
                    leftIcon={<Plus className="w-3.5 h-3.5" />}
                    onClick={() => setIsServiceModalOpen(true)}
                  >
                    Engage First Service
                  </Button>
                )}
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {clientServices
                  .filter((s) => serviceStatusFilter === 'ALL' || s.status === serviceStatusFilter)
                  .map((srv) => {
                    const statusColors = {
                      ACTIVE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
                      SUSPENDED: 'bg-amber-50 text-amber-700 border-amber-200',
                      COMPLETED: 'bg-slate-100 text-slate-700 border-slate-300',
                      INACTIVE: 'bg-rose-50 text-rose-700 border-rose-200',
                    }[srv.status] || 'bg-slate-100 text-slate-700 border-slate-200';

                    const moduleLink = srv.serviceType.startsWith('GST')
                      ? `/gst`
                      : srv.serviceType.startsWith('INCOME_TAX')
                      ? `/itr`
                      : srv.serviceType.startsWith('TDS')
                      ? `/tds`
                      : srv.serviceType === 'TAX_NOTICE_MANAGEMENT'
                      ? `/tax-notices`
                      : srv.serviceType === 'COMPLIANCE_CALENDAR'
                      ? `/calendar`
                      : srv.serviceType === 'DOCUMENT_MANAGEMENT'
                      ? `/documents`
                      : srv.serviceType === 'CLIENT_BILLING'
                      ? `/billing`
                      : undefined;

                    return (
                      <div
                        key={srv.id}
                        className="p-5 rounded-2xl border border-slate-200 bg-white hover:border-brand-200 hover:shadow-xs transition-all flex flex-col justify-between space-y-4"
                      >
                        <div className="space-y-3">
                          <div className="flex items-start justify-between gap-2">
                            <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-brand-50 text-brand-700 border border-brand-200">
                              {srv.serviceType}
                            </span>
                            <span
                              className={clsx(
                                'text-[10px] font-bold px-2 py-0.5 rounded-full border',
                                statusColors
                              )}
                            >
                              {srv.status}
                            </span>
                          </div>

                          <div>
                            <h3 className="text-sm font-bold text-slate-900 leading-snug">
                              {srv.serviceName}
                            </h3>
                            {srv.engagementNotes && (
                              <p className="text-xs text-slate-500 mt-1 line-clamp-2 leading-relaxed">
                                {srv.engagementNotes}
                              </p>
                            )}
                          </div>

                          <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-100 text-[11px]">
                            <div>
                              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                                Assigned Practitioner
                              </span>
                              <span className="font-semibold text-slate-800 truncate block mt-0.5">
                                {srv.assignedEmployeeName || 'Unassigned (Firm Pool)'}
                              </span>
                            </div>

                            <div>
                              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                                Billing Cycle
                              </span>
                              <span className="font-semibold text-slate-800 block mt-0.5">
                                {srv.billingCycle || 'ONE_TIME'}
                                {srv.agreedFee != null && (
                                  <span className="text-slate-500 font-normal ml-1">
                                    (₹{srv.agreedFee.toLocaleString()})
                                  </span>
                                )}
                              </span>
                            </div>

                            {srv.startDate && (
                              <div>
                                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                                  Start Date
                                </span>
                                <span className="text-slate-700 block mt-0.5">
                                  {new Date(srv.startDate).toLocaleDateString()}
                                </span>
                              </div>
                            )}

                            {srv.endDate && (
                              <div>
                                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                                  End Date
                                </span>
                                <span className="text-slate-700 block mt-0.5">
                                  {new Date(srv.endDate).toLocaleDateString()}
                                </span>
                              </div>
                            )}
                          </div>
                        </div>

                        {/* Card Actions */}
                        <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                          <div className="flex items-center gap-1">
                            {srv.status === 'ACTIVE' && (
                              <button
                                onClick={() => handleUpdateServiceStatus(srv.id, 'SUSPENDED')}
                                className="px-2 py-1 text-[10px] font-bold text-amber-700 bg-amber-50 hover:bg-amber-100 rounded-md border border-amber-200 transition-colors"
                                title="Suspend Engagement"
                              >
                                Suspend
                              </button>
                            )}
                            {srv.status === 'SUSPENDED' && (
                              <button
                                onClick={() => handleUpdateServiceStatus(srv.id, 'ACTIVE')}
                                className="px-2 py-1 text-[10px] font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 rounded-md border border-emerald-200 transition-colors"
                                title="Resume Engagement"
                              >
                                Resume
                              </button>
                            )}
                            {srv.status === 'ACTIVE' && (
                              <button
                                onClick={() => handleUpdateServiceStatus(srv.id, 'COMPLETED')}
                                className="px-2 py-1 text-[10px] font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-md border border-slate-300 transition-colors"
                                title="Mark Completed"
                              >
                                Complete
                              </button>
                            )}
                            {srv.status !== 'INACTIVE' && (
                              <button
                                onClick={() => handleDeactivateService(srv.id, srv.serviceName)}
                                className="px-2 py-1 text-[10px] font-bold text-rose-700 bg-rose-50 hover:bg-rose-100 rounded-md border border-rose-200 transition-colors"
                                title="Deactivate Service"
                              >
                                Deactivate
                              </button>
                            )}
                          </div>

                          {moduleLink && (
                            <Link to={moduleLink}>
                              <Button
                                variant="ghost"
                                size="sm"
                                className="text-xs"
                                rightIcon={<ExternalLink className="w-3.5 h-3.5" />}
                              >
                                Module
                              </Button>
                            </Link>
                          )}
                        </div>
                      </div>
                    );
                  })}
              </div>
            )}
          </div>
        )}

        {/* 3. COMPLIANCE TAB */}
        {activeTab === 'compliance' && (
          <div className="space-y-6">
            {/* GST Card */}
            <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <h3 className="text-base font-bold text-slate-900">Goods & Services Tax (GST)</h3>
                  <p className="text-xs text-slate-500">GSTIN: {complianceSummary.gstDetails?.gstin || 'Not Enrolled'}</p>
                </div>
                <Link to="/gst">
                  <Button variant="outline" size="sm" rightIcon={<ExternalLink className="w-3.5 h-3.5" />}>
                    Open GST Hub
                  </Button>
                </Link>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-slate-50 p-4 rounded-xl border border-slate-200/80">
                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Total Filings</span>
                  <span className="text-lg font-black text-slate-900">{complianceSummary.gstDetails?.totalFilings || 0}</span>
                </div>
                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Pending</span>
                  <span className="text-lg font-black text-amber-600">{complianceSummary.gstDetails?.pendingFilings || 0}</span>
                </div>
                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Overdue</span>
                  <span className="text-lg font-black text-rose-600">{complianceSummary.gstDetails?.overdueFilings || 0}</span>
                </div>
                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Filed</span>
                  <span className="text-lg font-black text-emerald-600">{complianceSummary.gstDetails?.filedFilings || 0}</span>
                </div>
              </div>
            </div>

            {/* Income Tax Card */}
            <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <h3 className="text-base font-bold text-slate-900">Income Tax Computations & Returns</h3>
                  <p className="text-xs text-slate-500">PAN: {complianceSummary.itrDetails?.pan || 'Not On Record'}</p>
                </div>
                <Link to="/itr">
                  <Button variant="outline" size="sm" rightIcon={<ExternalLink className="w-3.5 h-3.5" />}>
                    Open ITR Hub
                  </Button>
                </Link>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-slate-50 p-4 rounded-xl border border-slate-200/80">
                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Total Returns</span>
                  <span className="text-lg font-black text-slate-900">{complianceSummary.itrDetails?.totalReturns || 0}</span>
                </div>
                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Pending</span>
                  <span className="text-lg font-black text-amber-600">{complianceSummary.itrDetails?.pendingReturns || 0}</span>
                </div>
                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Overdue</span>
                  <span className="text-lg font-black text-rose-600">{complianceSummary.itrDetails?.overdueReturns || 0}</span>
                </div>
                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Filed</span>
                  <span className="text-lg font-black text-emerald-600">{complianceSummary.itrDetails?.filedReturns || 0}</span>
                </div>
              </div>
            </div>

            {/* TDS Card */}
            <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <h3 className="text-base font-bold text-slate-900">TDS / TCS Quarterly Filings</h3>
                  <p className="text-xs text-slate-500">TAN: {complianceSummary.tdsDetails?.tan || 'No TAN Configured'}</p>
                </div>
                <Link to="/tds">
                  <Button variant="outline" size="sm" rightIcon={<ExternalLink className="w-3.5 h-3.5" />}>
                    Open TDS Hub
                  </Button>
                </Link>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-slate-50 p-4 rounded-xl border border-slate-200/80">
                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Total Returns</span>
                  <span className="text-lg font-black text-slate-900">{complianceSummary.tdsDetails?.totalReturns || 0}</span>
                </div>
                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Pending</span>
                  <span className="text-lg font-black text-amber-600">{complianceSummary.tdsDetails?.pendingReturns || 0}</span>
                </div>
                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Overdue</span>
                  <span className="text-lg font-black text-rose-600">{complianceSummary.tdsDetails?.overdueReturns || 0}</span>
                </div>
                <div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Filed</span>
                  <span className="text-lg font-black text-emerald-600">{complianceSummary.tdsDetails?.filedReturns || 0}</span>
                </div>
              </div>
            </div>

            {/* Compliance Work Items Deliverables Card */}
            <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                    <Briefcase className="w-4 h-4 text-brand-600" />
                    <span>Compliance Deliverables & Filing Work Items</span>
                  </h3>
                  <p className="text-xs text-slate-500">
                    Period-specific operational filings and compliance lifecycle items for {client.displayName}.
                  </p>
                </div>
                <Link to="/compliance-work">
                  <Button variant="outline" size="sm" rightIcon={<ExternalLink className="w-3.5 h-3.5" />}>
                    Manage All Work Items
                  </Button>
                </Link>
              </div>

              {isLoadingComplianceWork ? (
                <div className="py-6 flex justify-center">
                  <div className="w-6 h-6 border-2 border-brand-500 border-t-transparent rounded-full animate-spin" />
                </div>
              ) : complianceWorkItems.length === 0 ? (
                <div className="p-6 text-center border border-dashed border-slate-200 rounded-xl">
                  <p className="text-xs text-slate-400 mb-2">No active compliance work items tracked for this client.</p>
                  <Link to="/compliance-work">
                    <Button size="sm" variant="ghost" className="text-xs text-brand-600">
                      Create Deliverable in Worklist
                    </Button>
                  </Link>
                </div>
              ) : (
                <div className="space-y-2.5">
                  {complianceWorkItems.map((item) => (
                    <div
                      key={item.id}
                      className="p-3.5 rounded-xl border border-slate-200 bg-white hover:border-brand-300 transition-colors flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-xs text-slate-900">{item.title}</span>
                          <span className="text-[10px] font-bold px-1.5 py-0.2 rounded bg-slate-100 text-slate-700">
                            {item.workType}
                          </span>
                          <span className="text-[10px] font-bold px-2 py-0.2 rounded-full border bg-brand-50 text-brand-700 border-brand-200">
                            {item.status.replace(/_/g, ' ')}
                          </span>
                        </div>
                        <div className="text-[11px] text-slate-500 flex flex-wrap items-center gap-2">
                          <span>Period: {item.compliancePeriod || item.financialYear || 'Current'}</span>
                          {item.assignedEmployeeName && (
                            <>
                              <span>•</span>
                              <span>Assignee: {item.assignedEmployeeName}</span>
                            </>
                          )}
                          {item.statutoryDueDate && (
                            <>
                              <span>•</span>
                              <span className={clsx("font-semibold", item.overdue ? "text-rose-600" : "text-slate-700")}>
                                Due: {new Date(item.statutoryDueDate).toLocaleDateString()}
                              </span>
                            </>
                          )}
                        </div>
                      </div>

                      <Link to="/compliance-work">
                        <Button variant="ghost" size="sm" className="text-xs shrink-0">
                          View in Worklist
                        </Button>
                      </Link>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* 4. DOCUMENTS VAULT TAB */}
        {activeTab === 'documents' && (
          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-slate-900">Client Document Vault</h3>
                <p className="text-xs text-slate-500">Permanent digital archive of tax filings, computations, KYC, and vouchers.</p>
              </div>
              <Link to="/documents">
                <Button variant="outline" size="sm" rightIcon={<ExternalLink className="w-3.5 h-3.5" />}>
                  Open Documents Hub
                </Button>
              </Link>
            </div>

            {(documentsSummary.recentDocuments || []).length === 0 ? (
              <p className="text-xs text-slate-400 py-8 text-center">No documents uploaded for this client yet.</p>
            ) : (
              <div className="divide-y divide-slate-100">
                {(documentsSummary.recentDocuments || []).map((doc) => (
                  <div key={doc.id} className="py-3 flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-lg bg-slate-100 text-slate-600 flex items-center justify-center font-bold text-xs shrink-0">
                        <FileText className="w-4 h-4" />
                      </div>
                      <div className="space-y-0.5">
                        <p className="text-xs font-bold text-slate-900">{doc.fileName}</p>
                        <span className="text-[10px] text-slate-400">
                          {doc.documentCategory} • {(doc.fileSize / 1024).toFixed(1)} KB
                        </span>
                      </div>
                    </div>
                    {doc.fileUrl && (
                      <a
                        href={doc.fileUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="p-1.5 text-slate-400 hover:text-brand-600 hover:bg-slate-50 rounded-lg"
                        title="Download Document"
                      >
                        <Download className="w-4 h-4" />
                      </a>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* 5. DOCUMENT REQUESTS TAB */}
        {activeTab === 'doc_requests' && (
          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
            <ClientDocumentRequestsTab clientId={client.id} clientName={client.displayName} />
          </div>
        )}

        {/* 6. TASKS TAB */}
        {activeTab === 'tasks' && (
          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-slate-900">Client Deliverables & Tasks</h3>
                <p className="text-xs text-slate-500">Track task assignments, progress, and review stages.</p>
              </div>
              <Link to={`/tasks?clientId=${client.id}`}>
                <Button variant="outline" size="sm" rightIcon={<ExternalLink className="w-3.5 h-3.5" />}>
                  Manage in Tasks Board
                </Button>
              </Link>
            </div>

            {(taskSummary.recentTasks || []).length === 0 ? (
              <p className="text-xs text-slate-400 py-8 text-center">No tasks assigned to this client.</p>
            ) : (
              <div className="divide-y divide-slate-100">
                {(taskSummary.recentTasks || []).map((task) => (
                  <div key={task.id} className="py-3 flex items-center justify-between gap-3">
                    <div className="space-y-1">
                      <p className="text-xs font-bold text-slate-900">{task.title}</p>
                      <div className="flex items-center gap-2 text-[11px] text-slate-400">
                        <span>Due: {task.dueDate || 'No Due Date'}</span>
                        <span>•</span>
                        <span>Priority: {task.priority || 'MEDIUM'}</span>
                      </div>
                    </div>
                    <StatusBadge status={task.status} size="sm" />
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* 7. TAX NOTICES TAB */}
        {activeTab === 'notices' && (
          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-slate-900">Tax Notices & Litigation Records</h3>
                <p className="text-xs text-slate-500">Statutory tax department notices, demands, and scheduled hearings.</p>
              </div>
              <Link to="/notices">
                <Button variant="outline" size="sm" rightIcon={<ExternalLink className="w-3.5 h-3.5" />}>
                  Open Notice Center
                </Button>
              </Link>
            </div>

            {(noticeSummary.recentNotices || []).length === 0 ? (
              <p className="text-xs text-slate-400 py-8 text-center">No tax notices or litigation matters recorded.</p>
            ) : (
              <div className="divide-y divide-slate-100">
                {(noticeSummary.recentNotices || []).map((notice) => (
                  <div key={notice.id} className="py-3 flex items-center justify-between gap-3">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-900">{notice.noticeNumber}</span>
                        {notice.issuingAuthority && (
                          <span className="text-[10px] font-bold px-1.5 py-0.2 rounded bg-slate-100 text-slate-700">
                            {notice.issuingAuthority}
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-2 text-[11px] text-slate-400">
                        <span>Section: {notice.section || 'N/A'}</span>
                        <span>•</span>
                        <span>Response Due: {notice.responseDueDate || 'N/A'}</span>
                        {notice.demandAmount && (
                          <span className="text-rose-600 font-bold">
                            Demand: ₹{Number(notice.demandAmount).toLocaleString('en-IN')}
                          </span>
                        )}
                      </div>
                    </div>
                    <StatusBadge status={notice.status} size="sm" />
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* 8. BILLING TAB */}
        {activeTab === 'billing' && (
          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs space-y-6">
            {!billingSummary ? (
              <div className="p-6 bg-slate-50 border border-slate-200 rounded-xl text-center space-y-2">
                <Lock className="w-6 h-6 text-slate-400 mx-auto" />
                <h4 className="text-xs font-bold text-slate-800">Financial Data Redacted</h4>
                <p className="text-[11px] text-slate-500 max-w-sm mx-auto">
                  Under practice zero-trust policy, billing and fee details are restricted to partners and authorized accountants.
                </p>
              </div>
            ) : (
              <>
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-base font-bold text-slate-900">Billing & Fee Management</h3>
                    <p className="text-xs text-slate-500">Invoices, fee collections, and balance records.</p>
                  </div>
                  <Link to="/billing">
                    <Button variant="outline" size="sm" rightIcon={<ExternalLink className="w-3.5 h-3.5" />}>
                      Open Billing Hub
                    </Button>
                  </Link>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 bg-slate-50 p-4 rounded-xl border border-slate-200/80">
                  <div>
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Total Invoiced</span>
                    <span className="text-xl font-black text-slate-900">₹{billingSummary.totalInvoiced.toLocaleString('en-IN')}</span>
                  </div>
                  <div>
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Total Received</span>
                    <span className="text-xl font-black text-emerald-600">₹{billingSummary.totalPaid.toLocaleString('en-IN')}</span>
                  </div>
                  <div>
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Outstanding Balance</span>
                    <span className={clsx("text-xl font-black", billingSummary.outstandingBalance > 0 ? "text-rose-600" : "text-slate-900")}>
                      ₹{billingSummary.outstandingBalance.toLocaleString('en-IN')}
                    </span>
                  </div>
                </div>

                {(billingSummary.recentInvoices || []).length === 0 ? (
                  <p className="text-xs text-slate-400 py-6 text-center">No invoices issued for this client yet.</p>
                ) : (
                  <div className="divide-y divide-slate-100">
                    {(billingSummary.recentInvoices || []).map((inv) => (
                      <div key={inv.id} className="py-3 flex items-center justify-between gap-3">
                        <div className="space-y-0.5">
                          <p className="text-xs font-bold text-slate-900">{inv.invoiceNumber}</p>
                          <span className="text-[10px] text-slate-400">Date: {inv.invoiceDate} • Due: {inv.dueDate}</span>
                        </div>
                        <div className="flex items-center gap-3">
                          <div className="text-right">
                            <span className="text-xs font-black text-slate-900 block">₹{Number(inv.total).toLocaleString('en-IN')}</span>
                            {Number(inv.balanceDue) > 0 && (
                              <span className="text-[10px] font-bold text-rose-600">Due: ₹{Number(inv.balanceDue).toLocaleString('en-IN')}</span>
                            )}
                          </div>
                          <StatusBadge status={inv.status} size="sm" />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {/* 9. ACTIVITY & NOTES TAB */}
        {activeTab === 'activity' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Left 2 Cols: Unified Chronological Timeline */}
            <div className="lg:col-span-2 bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs space-y-4">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <History className="w-4 h-4 text-brand-600" />
                <span>Chronological Relationship Timeline</span>
              </h3>

              {(activityTimeline || []).length === 0 ? (
                <p className="text-xs text-slate-400 py-8 text-center">No activities recorded for this client.</p>
              ) : (
                <div className="relative pl-6 space-y-6 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200">
                  {(activityTimeline || []).map((act, idx) => (
                    <div key={act.id || idx} className="relative space-y-1">
                      <div className="absolute -left-[19px] top-1 w-3.5 h-3.5 rounded-full border-2 border-white bg-brand-500 ring-2 ring-brand-100" />
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="font-bold text-slate-900">{act.title}</span>
                        <span className="text-slate-400">
                          {act.timestamp ? new Date(act.timestamp).toLocaleString() : ''}
                        </span>
                      </div>
                      <p className="text-xs text-slate-600">{act.description}</p>
                      {act.performedBy && (
                        <span className="text-[10px] text-slate-400 font-medium">By: {act.performedBy}</span>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Right Col: Communication Notes Log & Quick Add */}
            <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-slate-900">Communication Notes</h3>
                <Button
                  size="sm"
                  variant="primary"
                  leftIcon={<Plus className="w-3.5 h-3.5" />}
                  onClick={() => setIsNoteModalOpen(true)}
                >
                  Add Note
                </Button>
              </div>

              {(recentNotes || []).length === 0 ? (
                <p className="text-xs text-slate-400 py-4 text-center">No notes recorded yet.</p>
              ) : (
                <div className="space-y-3">
                  {(recentNotes || []).map((note) => (
                    <div key={note.id} className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 space-y-1.5">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold text-slate-900">{note.title}</span>
                        <span className="text-[10px] font-bold px-1.5 py-0.2 rounded bg-slate-200 text-slate-700">
                          {note.noteType}
                        </span>
                      </div>
                      <p className="text-xs text-slate-600 leading-relaxed">{note.content}</p>
                      <div className="flex items-center justify-between pt-1 text-[10px] text-slate-400">
                        <span>{note.authorName || 'Practitioner'}</span>
                        <span>{note.createdAt ? new Date(note.createdAt).toLocaleDateString() : ''}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Add Communication Note Modal */}
      <Modal
        isOpen={isNoteModalOpen}
        onClose={() => setIsNoteModalOpen(false)}
        title="Add Communication Note"
        subtitle={`Record interaction with ${client.displayName}`}
      >
        <form onSubmit={handleAddNote} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Note Category <span className="text-rose-500">*</span>
            </label>
            <select
              value={noteType}
              onChange={(e) => setNoteType(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
            >
              <option value="GENERAL">General Note</option>
              <option value="CALL">Phone Call</option>
              <option value="MEETING">In-Person / Virtual Meeting</option>
              <option value="EMAIL">Email Communication</option>
              <option value="FOLLOW_UP">Follow-Up Item</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Subject / Title <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              placeholder="e.g. Q3 Advance Tax Discussion"
              value={noteTitle}
              onChange={(e) => setNoteTitle(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Interaction Details <span className="text-rose-500">*</span>
            </label>
            <textarea
              required
              rows={4}
              placeholder="Summarize key points discussed, client instructions, or next actions..."
              value={noteContent}
              onChange={(e) => setNoteContent(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="pt-3 flex items-center justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => setIsNoteModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmittingNote}>
              Save Note
            </Button>
          </div>
        </form>
      </Modal>

      {/* Engage New Service Modal */}
      <Modal
        isOpen={isServiceModalOpen}
        onClose={() => setIsServiceModalOpen(false)}
        title="Engage New Client Service"
        subtitle={`Provision service for ${client.displayName}`}
      >
        <form onSubmit={handleCreateService} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Service Type (From Master Catalog) <span className="text-rose-500">*</span>
            </label>
            <select
              value={newServiceType}
              onChange={(e) => {
                const sType = e.target.value as ClientServiceType;
                setNewServiceType(sType);
                const catItem = catalog.find((c) => c.serviceType === sType);
                if (catItem) {
                  setNewServiceName(catItem.displayName);
                  setNewBillingCycle(catItem.defaultBillingCycle || 'MONTHLY');
                }
              }}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
            >
              {catalog.length === 0 ? (
                <>
                  <option value="GST_COMPLIANCE">GST Compliance & Periodic Filings</option>
                  <option value="INCOME_TAX_FILING">Income Tax Computations & Filing</option>
                  <option value="TDS_COMPLIANCE">TDS / TCS Compliance & Form 26Q/27Q</option>
                  <option value="TAX_NOTICE_MANAGEMENT">Tax Notice & Dispute Management</option>
                  <option value="COMPLIANCE_CALENDAR">Statutory Compliance Calendar</option>
                  <option value="DOCUMENT_MANAGEMENT">Document & Working Paper Management</option>
                  <option value="CLIENT_BILLING">Client Invoicing & Retainer Billing</option>
                  <option value="ACCOUNTING_BOOKKEEPING">Accounting & Bookkeeping Retainer</option>
                  <option value="STATUTORY_AUDIT">Statutory Audit (Companies Act)</option>
                  <option value="TAX_AUDIT">Tax Audit u/s 44AB</option>
                  <option value="COMPANY_SECRETARIAL">Company Secretarial (RoC Filings)</option>
                  <option value="PAYROLL_PROCESSING">Payroll & Employee Compliance</option>
                  <option value="ADVISORY_CONSULTING">Advisory & Tax Planning</option>
                  <option value="OTHER">Other Professional Service</option>
                </>
              ) : (
                catalog.map((cat) => (
                  <option key={cat.serviceType} value={cat.serviceType}>
                    {cat.displayName} ({cat.category})
                  </option>
                ))
              )}
            </select>
            {(() => {
              const catItem = catalog.find((c) => c.serviceType === newServiceType);
              if (catItem?.moduleCode && !isModuleAvailable(catItem.moduleCode)) {
                return (
                  <p className="text-[11px] text-amber-600 mt-1 font-medium flex items-center gap-1">
                    <AlertTriangle className="w-3.5 h-3.5" />
                    <span>Module '{catItem.moduleCode}' is disabled or not included in current subscription plan.</span>
                  </p>
                );
              }
              return null;
            })()}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Custom Service Name (Optional Override)
            </label>
            <input
              type="text"
              placeholder="e.g. Monthly GST Retainer & 3B Filing"
              value={newServiceName}
              onChange={(e) => setNewServiceName(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Assigned Practitioner / Lead
              </label>
              <select
                value={newAssignedEmployeeId}
                onChange={(e) => setNewAssignedEmployeeId(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
              >
                <option value="">Unassigned (Firm Pool)</option>
                {employees.map((emp) => (
                  <option key={emp.id} value={emp.id}>
                    {emp.firstName} {emp.lastName || ''} ({emp.email})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Billing Cycle <span className="text-rose-500">*</span>
              </label>
              <select
                value={newBillingCycle}
                onChange={(e) => setNewBillingCycle(e.target.value as any)}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
              >
                <option value="MONTHLY">Monthly</option>
                <option value="QUARTERLY">Quarterly</option>
                <option value="ANNUAL">Annual</option>
                <option value="ONE_TIME">One-Time Engagement</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Agreed Fee (₹)
              </label>
              <input
                type="number"
                min="0"
                step="100"
                placeholder="e.g. 15000"
                value={newAgreedFee}
                onChange={(e) => setNewAgreedFee(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Engagement Start Date
              </label>
              <input
                type="date"
                value={newStartDate}
                onChange={(e) => setNewStartDate(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                End Date (Optional)
              </label>
              <input
                type="date"
                value={newEndDate}
                onChange={(e) => setNewEndDate(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Scope & Engagement Notes
            </label>
            <textarea
              rows={3}
              placeholder="Outline service scope, deliverables, agreed SLA, or recurring schedule..."
              value={newEngagementNotes}
              onChange={(e) => setNewEngagementNotes(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="pt-3 flex items-center justify-end gap-2 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                setIsServiceModalOpen(false);
                resetServiceForm();
              }}
            >
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmittingService}>
              Engage Service
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
