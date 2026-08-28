import React, { useState, useEffect } from 'react';
import {
  Users,
  Search,
  Filter,
  UserCheck,
  UserPlus,
  Phone,
  Mail,
  MapPin,
  Calendar,
  CheckCircle2,
  Clock,
  ArrowRight,
  MoreHorizontal,
  FileText,
  DollarSign,
  TrendingUp,
  AlertCircle,
  Building,
  CheckCircle,
  XCircle,
  PlayCircle,
  UserCog,
  History,
  X,
  ShieldCheck,
  Tag,
  MessageSquare,
  Send,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { marketplacePracticeApi, marketplaceOnboardingPracticeApi, employeeApi } from '../api/endpoints';
import {
  MarketplaceLead,
  MarketplaceStats,
  Employee,
  CreateProposalRequest,
  EnquiryDetail,
  EnquiryStatus,
  EnquiryRejectionReason,
  AcceptEnquiryRequest,
  RejectEnquiryRequest,
  AssignEnquiryRequest,
  EnquiryMessage,
  EnquiryMessageThread,
} from '../types';
import { useNavigate } from 'react-router-dom';
import clsx from 'clsx';

export const MarketplaceLeadsPage: React.FC = () => {
  const navigate = useNavigate();
  const [enquiries, setEnquiries] = useState<EnquiryDetail[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [stats, setStats] = useState<MarketplaceStats | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Filters
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState<string>('');

  // Selected Enquiry for Timeline Drawer
  const [selectedEnquiryForTimeline, setSelectedEnquiryForTimeline] = useState<EnquiryDetail | null>(null);

  // Lifecycle Action Modals
  const [acceptEnquiryTarget, setAcceptEnquiryTarget] = useState<EnquiryDetail | null>(null);
  const [acceptForm, setAcceptForm] = useState<AcceptEnquiryRequest>({ notes: '', estimatedDaysToComplete: 5 });

  const [rejectEnquiryTarget, setRejectEnquiryTarget] = useState<EnquiryDetail | null>(null);
  const [rejectForm, setRejectForm] = useState<RejectEnquiryRequest>({
    rejectionReason: 'SERVICE_NOT_AVAILABLE',
    rejectionNote: '',
  });

  const [assignEnquiryTarget, setAssignEnquiryTarget] = useState<EnquiryDetail | null>(null);
  const [assignForm, setAssignForm] = useState<AssignEnquiryRequest>({
    assignedEmployeeId: '',
    assignmentNotes: '',
  });

  // Proposal Modal
  const [selectedLeadForProposal, setSelectedLeadForProposal] = useState<EnquiryDetail | null>(null);
  const [proposalForm, setProposalForm] = useState<CreateProposalRequest>({
    leadId: '',
    proposalTitle: 'Statutory Tax Compliance & Advisory Engagement',
    scopeOfWork: 'Preparation and filing of tax returns, computations, reconciliations, and representation.',
    deliverables: 'Filed return acknowledgements (ARN), Monthly ITC analysis report, Form 26AS/AIS reconciliation sheet.',
    feeAmount: 4999,
    pricingType: 'MONTHLY_RETAINER',
    estimatedTimelineDays: 7,
  });

  // Conversion Modal
  const [selectedLeadForConvert, setSelectedLeadForConvert] = useState<EnquiryDetail | null>(null);
  const [convertForm, setConvertForm] = useState({
    clientType: 'INDIVIDUAL',
    assignedEmployeeId: '',
    createOnboardingTask: true,
    notes: '',
  });
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [successBanner, setSuccessBanner] = useState<string | null>(null);

  // Secure Messaging Drawer
  const [selectedEnquiryForMessages, setSelectedEnquiryForMessages] = useState<EnquiryDetail | null>(null);
  const [messageThread, setMessageThread] = useState<EnquiryMessageThread | null>(null);
  const [messageText, setMessageText] = useState<string>('');
  const [isLoadingMessages, setIsLoadingMessages] = useState<boolean>(false);
  const [isSendingMessage, setIsSendingMessage] = useState<boolean>(false);

  const openMessages = async (enquiry: EnquiryDetail) => {
    setSelectedEnquiryForMessages(enquiry);
    setMessageText('');
    setIsLoadingMessages(true);
    try {
      const thread = await marketplacePracticeApi.getEnquiryMessages(enquiry.id);
      setMessageThread(thread);
      await marketplacePracticeApi.markMessagesRead(enquiry.id);
    } catch (err: any) {
      console.error('Failed to load enquiry messages:', err);
    } finally {
      setIsLoadingMessages(false);
    }
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedEnquiryForMessages || !messageText.trim() || isSendingMessage) return;
    setIsSendingMessage(true);
    try {
      const newMsg = await marketplacePracticeApi.sendEnquiryMessage(selectedEnquiryForMessages.id, {
        messageBody: messageText.trim(),
      });
      setMessageText('');
      if (messageThread) {
        setMessageThread({
          ...messageThread,
          messages: [...messageThread.messages, newMsg],
        });
      }
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to send message.');
    } finally {
      setIsSendingMessage(false);
    }
  };

  const fetchEnquiriesData = async () => {
    setIsLoading(true);
    try {
      const [enquiryRes, empRes, statsRes] = await Promise.all([
        marketplacePracticeApi.getPracticeEnquiries({
          status: statusFilter || undefined,
          search: searchTerm || undefined,
          size: 50,
        }),
        employeeApi.getAll({ size: 100 }).then((r) => r.content || []).catch(() => []),
        marketplacePracticeApi.getStats().catch(() => null),
      ]);

      setEnquiries(enquiryRes.content || []);
      setEmployees(empRes || []);
      setStats(statsRes);
    } catch (err) {
      console.error('Failed to load marketplace enquiries', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchEnquiriesData();
  }, [statusFilter]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    fetchEnquiriesData();
  };

  const handleAccept = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!acceptEnquiryTarget) return;
    try {
      setIsSubmitting(true);
      await marketplacePracticeApi.acceptEnquiry(acceptEnquiryTarget.id, acceptForm);
      setAcceptEnquiryTarget(null);
      await fetchEnquiriesData();
      setSuccessBanner(`Enquiry ${acceptEnquiryTarget.referenceNumber} has been accepted!`);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to accept enquiry.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rejectEnquiryTarget) return;
    try {
      setIsSubmitting(true);
      await marketplacePracticeApi.rejectEnquiry(rejectEnquiryTarget.id, rejectForm);
      setRejectEnquiryTarget(null);
      await fetchEnquiriesData();
      setSuccessBanner(`Enquiry ${rejectEnquiryTarget.referenceNumber} has been marked as rejected.`);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to reject enquiry.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleAssign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!assignEnquiryTarget || !assignForm.assignedEmployeeId) return;
    try {
      setIsSubmitting(true);
      await marketplacePracticeApi.assignEnquiry(assignEnquiryTarget.id, assignForm);
      setAssignEnquiryTarget(null);
      await fetchEnquiriesData();
      setSuccessBanner(`Enquiry ${assignEnquiryTarget.referenceNumber} assigned successfully!`);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to assign enquiry.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleStartWork = async (enquiry: EnquiryDetail) => {
    try {
      await marketplacePracticeApi.startEnquiry(enquiry.id);
      await fetchEnquiriesData();
      setSuccessBanner(`Work marked in progress for enquiry ${enquiry.referenceNumber}`);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to start enquiry work.');
    }
  };

  const handleCompleteWork = async (enquiry: EnquiryDetail) => {
    try {
      await marketplacePracticeApi.completeEnquiry(enquiry.id);
      await fetchEnquiriesData();
      setSuccessBanner(`Enquiry ${enquiry.referenceNumber} marked completed! Verified review request dispatched to client.`);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to complete enquiry.');
    }
  };

  const handleConvertLead = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLeadForConvert) return;
    setIsSubmitting(true);
    try {
      await marketplacePracticeApi.convertLeadToClient(selectedLeadForConvert.id, {
        clientType: convertForm.clientType,
        assignedEmployeeId: convertForm.assignedEmployeeId || undefined,
        createOnboardingTask: convertForm.createOnboardingTask,
        notes: convertForm.notes,
      });

      setSelectedLeadForConvert(null);
      await fetchEnquiriesData();
      setSuccessBanner(
        `Successfully converted ${selectedLeadForConvert.clientName} to an Active Practice CRM Client! Onboarding task initiated.`
      );
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to convert lead to client.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSendProposal = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLeadForProposal) return;
    try {
      setIsSubmitting(true);
      const prop = await marketplaceOnboardingPracticeApi.sendProposal({
        ...proposalForm,
        leadId: selectedLeadForProposal.id,
      });
      setSelectedLeadForProposal(null);
      await fetchEnquiriesData();
      setSuccessBanner(
        `Formal engagement proposal dispatched to ${selectedLeadForProposal.clientName}! Public link: /marketplace/onboarding/${prop.accessToken}`
      );
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to send engagement proposal.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const getStatusBadge = (status: EnquiryStatus) => {
    switch (status) {
      case 'NEW':
        return <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-blue-100 text-blue-800 border border-blue-200">New</span>;
      case 'RECEIVED':
        return <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-sky-100 text-sky-800 border border-sky-200">Received</span>;
      case 'ACCEPTED':
        return <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-indigo-100 text-indigo-800 border border-indigo-200">Accepted</span>;
      case 'IN_PROGRESS':
        return <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-100 text-amber-800 border border-amber-200 animate-pulse">In Progress</span>;
      case 'COMPLETED':
        return <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">Completed</span>;
      case 'REJECTED':
        return <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-rose-100 text-rose-800 border border-rose-200">Declined</span>;
      case 'CANCELLED':
        return <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-slate-100 text-slate-700 border border-slate-200">Cancelled</span>;
      default:
        return <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-slate-100 text-slate-700">{status}</span>;
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6 pb-20">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2.5">
            <Users className="w-7 h-7 text-brand-600" />
            <span>Marketplace Enquiry Management & Client Inbox</span>
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Accept, assign, execute, and complete verified customer tax service enquiries in real-time.
          </p>
        </div>
      </div>

      {/* Success Banner */}
      {successBanner && (
        <div className="bg-emerald-50 dark:bg-emerald-950/50 p-4 rounded-2xl border border-emerald-200 dark:border-emerald-800 flex items-center justify-between text-sm text-emerald-800 dark:text-emerald-200">
          <div className="flex items-center gap-2 font-medium">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span>{successBanner}</span>
          </div>
          <button onClick={() => setSuccessBanner(null)} className="text-emerald-600 font-bold">×</button>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
          <div className="text-xs font-bold uppercase tracking-wider text-slate-400">Total Enquiries</div>
          <div className="text-2xl font-extrabold text-slate-900 dark:text-white">{stats?.totalInboundLeads || enquiries.length}</div>
          <div className="text-[11px] text-slate-500">From public discovery & profiles</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
          <div className="text-xs font-bold uppercase tracking-wider text-slate-400">In Progress / Active</div>
          <div className="text-2xl font-extrabold text-indigo-600">
            {enquiries.filter((e) => e.enquiryStatus === 'ACCEPTED' || e.enquiryStatus === 'IN_PROGRESS').length}
          </div>
          <div className="text-[11px] text-indigo-600 font-semibold">Active taxpayer engagements</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
          <div className="text-xs font-bold uppercase tracking-wider text-slate-400">Completed & Verified</div>
          <div className="text-2xl font-extrabold text-emerald-600">
            {enquiries.filter((e) => e.enquiryStatus === 'COMPLETED').length}
          </div>
          <div className="text-[11px] text-emerald-600 font-semibold">Eligible for verified reviews</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
          <div className="text-xs font-bold uppercase tracking-wider text-slate-400">Conversion Rate</div>
          <div className="text-2xl font-extrabold text-brand-600">{stats?.leadConversionRate || 0}%</div>
          <div className="text-[11px] text-slate-500">Inquiry to client conversion</div>
        </div>
      </div>

      {/* Privacy Protection Notice */}
      <div className="bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800/60 p-4 rounded-2xl flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl bg-emerald-100 dark:bg-emerald-900/50 flex items-center justify-center shrink-0">
          <ShieldCheck className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />
        </div>
        <div className="text-xs">
          <span className="font-bold text-emerald-900 dark:text-emerald-200">Least-Privilege Privacy & Anti-Spam Active: </span>
          <span className="text-emerald-800/90 dark:text-emerald-300/80">
            Early enquiries contain verified reference IDs and necessary requirement scope. Raw identity credentials remain protected until mutual acceptance. Duplicate enquiries are automatically deduplicated.
          </span>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-2 flex-wrap w-full md:w-auto">
          {[
            { label: 'All Enquiries', value: '' },
            { label: 'New / Received', value: 'NEW' },
            { label: 'Accepted', value: 'ACCEPTED' },
            { label: 'In Progress', value: 'IN_PROGRESS' },
            { label: 'Completed', value: 'COMPLETED' },
            { label: 'Declined / Cancelled', value: 'REJECTED' },
          ].map((tab) => (
            <button
              key={tab.value}
              onClick={() => setStatusFilter(tab.value)}
              className={clsx(
                'px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all',
                statusFilter === tab.value
                  ? 'bg-brand-600 text-white shadow-sm'
                  : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-200'
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <form onSubmit={handleSearch} className="flex items-center gap-2 w-full md:w-72">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
            <input
              type="text"
              placeholder="Search reference #, client, service..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
        </form>
      </div>

      {/* Enquiries Table */}
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-slate-500 text-sm">Loading enquiry lifecycle data...</div>
        ) : enquiries.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <Users className="w-10 h-10 text-slate-400 mx-auto" />
            <h3 className="text-base font-bold text-slate-900 dark:text-white">No Marketplace Enquiries Found</h3>
            <p className="text-xs text-slate-500">
              When customers discover your practice and submit requirements, they will appear here with automated status tracking.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 dark:border-slate-800 bg-slate-50/75 dark:bg-slate-800/40 text-slate-400 font-bold uppercase tracking-wider">
                  <th className="p-4">Reference & Client</th>
                  <th className="p-4">Requested Tax Service</th>
                  <th className="p-4">Status & Stage</th>
                  <th className="p-4">Assigned Member</th>
                  <th className="p-4 text-right">Lifecycle Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {enquiries.map((enquiry) => (
                  <tr key={enquiry.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                    <td className="p-4">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-bold text-indigo-600 bg-indigo-50 dark:bg-indigo-950/60 px-2 py-0.5 rounded">
                            {enquiry.referenceNumber}
                          </span>
                          <span className="font-bold text-slate-900 dark:text-white text-sm">{enquiry.clientName}</span>
                        </div>
                        <div className="text-[11px] text-slate-500 flex items-center gap-2 mt-1">
                          <span>{enquiry.clientEmail}</span>
                          <span>•</span>
                          <span>{enquiry.clientPhone}</span>
                          {enquiry.city && <span>• {enquiry.city}</span>}
                        </div>
                      </div>
                    </td>

                    <td className="p-4 max-w-xs">
                      <div className="space-y-1">
                        <div className="flex items-center gap-1.5">
                          <span className="text-[10px] font-bold uppercase tracking-wider bg-brand-50 text-brand-700 dark:bg-brand-950/60 dark:text-brand-300 px-2 py-0.5 rounded">
                            {enquiry.taxServiceName || enquiry.serviceCategory || 'Tax Advisory'}
                          </span>
                          {enquiry.financialYear && (
                            <span className="text-[10px] font-medium text-slate-500">FY: {enquiry.financialYear}</span>
                          )}
                        </div>
                        <p className="text-xs text-slate-600 dark:text-slate-300 line-clamp-2 leading-relaxed">
                          {enquiry.earlyEnquiryMessage || enquiry.requirementDescription || 'Tax service enquiry'}
                        </p>
                      </div>
                    </td>

                    <td className="p-4">
                      <div className="space-y-1">
                        {getStatusBadge(enquiry.enquiryStatus)}
                        {enquiry.rejectionReason && (
                          <div className="text-[10px] text-rose-600 font-semibold">
                            Reason: {enquiry.rejectionReason.replace(/_/g, ' ')}
                          </div>
                        )}
                        <button
                          onClick={() => setSelectedEnquiryForTimeline(enquiry)}
                          className="text-[10px] text-indigo-600 hover:underline flex items-center gap-0.5 font-bold pt-0.5"
                        >
                          <History className="w-3 h-3" />
                          View Timeline
                        </button>
                      </div>
                    </td>

                    <td className="p-4 text-slate-600 dark:text-slate-300">
                      <div className="flex items-center gap-1.5">
                        <UserCog className="w-3.5 h-3.5 text-slate-400" />
                        <span className="font-medium">{enquiry.assignedEmployeeName || 'Unassigned'}</span>
                        <button
                          onClick={() => {
                            setAssignEnquiryTarget(enquiry);
                            setAssignForm({ assignedEmployeeId: enquiry.assignedEmployeeId || '', assignmentNotes: '' });
                          }}
                          className="ml-1 text-[10px] text-brand-600 hover:underline"
                        >
                          Change
                        </button>
                      </div>
                    </td>

                    <td className="p-4 text-right">
                      <div className="flex items-center justify-end gap-1.5 flex-wrap">
                        {/* Status = NEW or RECEIVED -> Accept or Reject */}
                        {(enquiry.enquiryStatus === 'NEW' || enquiry.enquiryStatus === 'RECEIVED') && (
                          <>
                            <Button
                              size="sm"
                              variant="primary"
                              onClick={() => {
                                setAcceptEnquiryTarget(enquiry);
                                setAcceptForm({ notes: '', estimatedDaysToComplete: 5 });
                              }}
                              className="text-xs bg-emerald-600 hover:bg-emerald-700 text-white font-bold"
                            >
                              <CheckCircle className="w-3.5 h-3.5 mr-1" />
                              Accept
                            </Button>
                            <Button
                              size="sm"
                              variant="secondary"
                              onClick={() => {
                                setRejectEnquiryTarget(enquiry);
                                setRejectForm({ rejectionReason: 'SERVICE_NOT_AVAILABLE', rejectionNote: '' });
                              }}
                              className="text-xs text-rose-600 hover:bg-rose-50 border-rose-200"
                            >
                              <XCircle className="w-3.5 h-3.5 mr-1" />
                              Decline
                            </Button>
                          </>
                        )}

                        {/* Status = ACCEPTED -> Start Work */}
                        {enquiry.enquiryStatus === 'ACCEPTED' && (
                          <Button
                            size="sm"
                            variant="primary"
                            onClick={() => handleStartWork(enquiry)}
                            className="text-xs bg-indigo-600 hover:bg-indigo-700 text-white font-bold"
                          >
                            <PlayCircle className="w-3.5 h-3.5 mr-1" />
                            Start Work
                          </Button>
                        )}

                        {/* Status = IN_PROGRESS -> Mark Completed or Convert */}
                        {enquiry.enquiryStatus === 'IN_PROGRESS' && (
                          <>
                            <Button
                              size="sm"
                              variant="primary"
                              onClick={() => handleCompleteWork(enquiry)}
                              className="text-xs bg-emerald-600 hover:bg-emerald-700 text-white font-bold"
                            >
                              <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
                              Mark Completed
                            </Button>
                            <Button
                              size="sm"
                              variant="secondary"
                              onClick={() => {
                                setSelectedLeadForConvert(enquiry);
                                setConvertForm({
                                  clientType: 'INDIVIDUAL',
                                  assignedEmployeeId: enquiry.assignedEmployeeId || '',
                                  createOnboardingTask: true,
                                  notes: `Converted from completed enquiry ${enquiry.referenceNumber}`,
                                });
                              }}
                              className="text-xs"
                            >
                              <UserPlus className="w-3.5 h-3.5 mr-1" />
                              Add to CRM
                            </Button>
                          </>
                        )}

                        {/* Status = COMPLETED */}
                        {enquiry.enquiryStatus === 'COMPLETED' && (
                          <span className="inline-flex items-center gap-1 text-xs font-bold text-emerald-600">
                            <CheckCircle2 className="w-4 h-4" />
                            Completed
                          </span>
                        )}

                        {/* Send Formal Proposal option */}
                        {enquiry.enquiryStatus !== 'COMPLETED' &&
                          enquiry.enquiryStatus !== 'REJECTED' &&
                          enquiry.enquiryStatus !== 'CANCELLED' && (
                            <Button
                              size="sm"
                              variant="secondary"
                              onClick={() => {
                                setSelectedLeadForProposal(enquiry);
                                setProposalForm({
                                  leadId: enquiry.id,
                                  proposalTitle: `Engagement for ${enquiry.taxServiceName || enquiry.serviceCategory || 'Tax Advisory'}`,
                                  scopeOfWork: enquiry.earlyEnquiryMessage || enquiry.requirementDescription || 'Statutory tax compliance, documentation and representation.',
                                  deliverables: 'Filing acknowledgements, monthly reconciliations, and compliance reports.',
                                  feeAmount: 3999,
                                  pricingType: 'MONTHLY_RETAINER',
                                  estimatedTimelineDays: 7,
                                });
                              }}
                              className="text-xs"
                            >
                              <FileText className="w-3.5 h-3.5 mr-1" />
                              Proposal
                            </Button>
                          )}

                        {/* Secure Messages Button */}
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() => openMessages(enquiry)}
                          className="text-xs bg-indigo-50/80 hover:bg-indigo-100 text-indigo-700 border-indigo-200"
                        >
                          <MessageSquare className="w-3.5 h-3.5 mr-1 text-indigo-600" />
                          Messages
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Timeline Drawer / Modal */}
      {selectedEnquiryForTimeline && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-lg w-full p-6 space-y-5 shadow-2xl">
            <div className="flex items-center justify-between border-b pb-3">
              <div>
                <span className="font-mono text-xs font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">
                  {selectedEnquiryForTimeline.referenceNumber}
                </span>
                <h3 className="text-base font-bold text-slate-900 dark:text-white mt-1">
                  Enquiry Lifecycle Timeline
                </h3>
              </div>
              <button
                onClick={() => setSelectedEnquiryForTimeline(null)}
                className="text-gray-400 hover:text-gray-600 p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4 max-h-96 overflow-y-auto pr-2">
              {selectedEnquiryForTimeline.timeline?.map((item, idx) => (
                <div key={idx} className="flex gap-3">
                  <div className="flex flex-col items-center">
                    <div
                      className={clsx(
                        'w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0',
                        item.completed
                          ? 'bg-emerald-600 text-white'
                          : item.current
                          ? 'bg-indigo-600 text-white ring-4 ring-indigo-100'
                          : 'bg-slate-200 text-slate-500'
                      )}
                    >
                      {item.completed ? '✓' : idx + 1}
                    </div>
                    {idx < selectedEnquiryForTimeline.timeline.length - 1 && (
                      <div
                        className={clsx(
                          'w-0.5 h-10 my-1',
                          item.completed ? 'bg-emerald-500' : 'bg-slate-200'
                        )}
                      />
                    )}
                  </div>
                  <div className="space-y-0.5 pb-2">
                    <div className="text-xs font-bold text-slate-900 dark:text-white">{item.title}</div>
                    <p className="text-[11px] text-slate-500">{item.description}</p>
                    {item.timestamp && (
                      <div className="text-[10px] text-slate-400 font-mono">
                        {new Date(item.timestamp).toLocaleString()}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>

            <div className="pt-3 border-t text-right">
              <Button size="sm" variant="secondary" onClick={() => setSelectedEnquiryForTimeline(null)}>
                Close
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Accept Enquiry Modal */}
      {acceptEnquiryTarget && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2">
                <CheckCircle className="w-5 h-5 text-emerald-600" />
                <h3 className="text-base font-bold text-slate-900 dark:text-white">Accept Enquiry</h3>
              </div>
              <button onClick={() => setAcceptEnquiryTarget(null)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-xs text-slate-600 dark:text-slate-400">
              Accepting <strong>{acceptEnquiryTarget.referenceNumber}</strong> from{' '}
              <strong>{acceptEnquiryTarget.clientName}</strong> will notify the customer and prepare the engagement for active execution.
            </p>

            <form onSubmit={handleAccept} className="space-y-3">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Estimated Turnaround (Days)</label>
                <input
                  type="number"
                  min="1"
                  max="90"
                  value={acceptForm.estimatedDaysToComplete}
                  onChange={(e) => setAcceptForm({ ...acceptForm, estimatedDaysToComplete: parseInt(e.target.value) || 5 })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Practitioner Notes (Internal or Client)</label>
                <textarea
                  rows={2}
                  placeholder="e.g. Scope reviewed. Please have Form 16 / bank statements ready."
                  value={acceptForm.notes}
                  onChange={(e) => setAcceptForm({ ...acceptForm, notes: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <Button variant="secondary" size="sm" onClick={() => setAcceptEnquiryTarget(null)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" disabled={isSubmitting} className="bg-emerald-600 hover:bg-emerald-700 text-white font-bold">
                  {isSubmitting ? 'Accepting...' : 'Confirm Acceptance'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reject Enquiry Modal */}
      {rejectEnquiryTarget && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2">
                <XCircle className="w-5 h-5 text-rose-600" />
                <h3 className="text-base font-bold text-slate-900 dark:text-white">Decline Enquiry</h3>
              </div>
              <button onClick={() => setRejectEnquiryTarget(null)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleReject} className="space-y-3">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Reason for Declining *</label>
                <select
                  value={rejectForm.rejectionReason}
                  onChange={(e) => setRejectForm({ ...rejectForm, rejectionReason: e.target.value as EnquiryRejectionReason })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs font-medium"
                >
                  <option value="SERVICE_NOT_AVAILABLE">Service Not Offered in Current Quarter</option>
                  <option value="OUTSIDE_SERVICE_AREA">Outside Practice Service Jurisdiction</option>
                  <option value="CURRENTLY_UNAVAILABLE">Practitioner Currently Unavailable</option>
                  <option value="CAPACITY_FULL">Filing Capacity Full for Current Cycle</option>
                  <option value="OTHER">Other Reason</option>
                </select>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Explanation Note</label>
                <textarea
                  rows={2}
                  placeholder="Optional context to share respectfully with the customer..."
                  value={rejectForm.rejectionNote}
                  onChange={(e) => setRejectForm({ ...rejectForm, rejectionNote: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <Button variant="secondary" size="sm" onClick={() => setRejectEnquiryTarget(null)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" disabled={isSubmitting} className="bg-rose-600 hover:bg-rose-700 text-white font-bold">
                  {isSubmitting ? 'Declining...' : 'Decline Enquiry'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Assign Team Member Modal */}
      {assignEnquiryTarget && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2">
                <UserCog className="w-5 h-5 text-indigo-600" />
                <h3 className="text-base font-bold text-slate-900 dark:text-white">Assign Team Member</h3>
              </div>
              <button onClick={() => setAssignEnquiryTarget(null)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleAssign} className="space-y-3">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Select Practice Employee *</label>
                <select
                  required
                  value={assignForm.assignedEmployeeId}
                  onChange={(e) => setAssignForm({ ...assignForm, assignedEmployeeId: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs font-medium"
                >
                  <option value="">-- Choose Employee --</option>
                  {employees.map((emp) => (
                    <option key={emp.id} value={emp.id}>
                      {emp.firstName} {emp.lastName} ({emp.designation || 'Staff'})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Assignment Note</label>
                <textarea
                  rows={2}
                  placeholder="e.g. Please lead client communication and tax computation."
                  value={assignForm.assignmentNotes}
                  onChange={(e) => setAssignForm({ ...assignForm, assignmentNotes: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <Button variant="secondary" size="sm" onClick={() => setAssignEnquiryTarget(null)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" disabled={isSubmitting || !assignForm.assignedEmployeeId} className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold">
                  {isSubmitting ? 'Assigning...' : 'Assign Member'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Convert to CRM Client Modal */}
      {selectedLeadForConvert && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2">
                <UserPlus className="w-5 h-5 text-emerald-600" />
                <h3 className="text-base font-bold text-slate-900 dark:text-white">Convert to Practice Client</h3>
              </div>
              <button onClick={() => setSelectedLeadForConvert(null)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleConvertLead} className="space-y-3">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Client Type</label>
                <select
                  value={convertForm.clientType}
                  onChange={(e) => setConvertForm({ ...convertForm, clientType: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                >
                  <option value="INDIVIDUAL">Individual Taxpayer</option>
                  <option value="COMPANY">Private Limited Company</option>
                  <option value="LLP">Limited Liability Partnership (LLP)</option>
                  <option value="PARTNERSHIP_FIRM">Partnership Firm</option>
                  <option value="TRUST">Trust / NGO</option>
                </select>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Assign Engagement Lead</label>
                <select
                  value={convertForm.assignedEmployeeId}
                  onChange={(e) => setConvertForm({ ...convertForm, assignedEmployeeId: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                >
                  <option value="">-- Practice Unassigned --</option>
                  {employees.map((emp) => (
                    <option key={emp.id} value={emp.id}>
                      {emp.firstName} {emp.lastName} ({emp.designation || 'Staff'})
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="createTask"
                  checked={convertForm.createOnboardingTask}
                  onChange={(e) => setConvertForm({ ...convertForm, createOnboardingTask: e.target.checked })}
                  className="rounded text-brand-600 focus:ring-brand-500"
                />
                <label htmlFor="createTask" className="text-xs text-slate-700 dark:text-slate-300">
                  Automatically create Client Onboarding Task
                </label>
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <Button variant="secondary" size="sm" onClick={() => setSelectedLeadForConvert(null)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" disabled={isSubmitting} className="bg-emerald-600 hover:bg-emerald-700 text-white font-bold">
                  {isSubmitting ? 'Converting...' : 'Complete Conversion'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Send Proposal Modal */}
      {selectedLeadForProposal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-lg w-full p-6 space-y-5 shadow-2xl">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-2xl bg-indigo-50 text-indigo-600">
                  <FileText className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900 dark:text-white">
                    Send Engagement Proposal
                  </h3>
                  <p className="text-xs text-slate-500">For {selectedLeadForProposal.clientName} ({selectedLeadForProposal.referenceNumber})</p>
                </div>
              </div>
              <button onClick={() => setSelectedLeadForProposal(null)} className="text-gray-400 hover:text-gray-600 font-bold">&times;</button>
            </div>

            <form onSubmit={handleSendProposal} className="space-y-3">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Proposal Title *</label>
                <input
                  type="text"
                  required
                  value={proposalForm.proposalTitle}
                  onChange={(e) => setProposalForm({ ...proposalForm, proposalTitle: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Scope of Work *</label>
                <textarea
                  rows={3}
                  required
                  value={proposalForm.scopeOfWork}
                  onChange={(e) => setProposalForm({ ...proposalForm, scopeOfWork: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Proposed Fee (₹) *</label>
                  <input
                    type="number"
                    required
                    value={proposalForm.feeAmount}
                    onChange={(e) => setProposalForm({ ...proposalForm, feeAmount: parseFloat(e.target.value) || 0 })}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  />
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Estimated Days *</label>
                  <input
                    type="number"
                    required
                    value={proposalForm.estimatedTimelineDays}
                    onChange={(e) => setProposalForm({ ...proposalForm, estimatedTimelineDays: parseInt(e.target.value) || 7 })}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t">
                <Button variant="secondary" size="sm" onClick={() => setSelectedLeadForProposal(null)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" disabled={isSubmitting} className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold">
                  {isSubmitting ? 'Sending...' : 'Dispatch Engagement Proposal'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Secure Messages Drawer Modal */}
      {selectedEnquiryForMessages && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-xl w-full flex flex-col max-h-[85vh] shadow-2xl overflow-hidden">
            {/* Modal Header */}
            <div className="p-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between bg-slate-50 dark:bg-slate-800/50">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-2xl bg-indigo-50 dark:bg-indigo-900/40 text-indigo-600">
                  <MessageSquare className="w-5 h-5" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-bold text-indigo-600 bg-indigo-50 dark:bg-indigo-900/30 px-2 py-0.5 rounded">
                      {selectedEnquiryForMessages.referenceNumber}
                    </span>
                    <h3 className="text-sm font-bold text-slate-900 dark:text-white">
                      Conversation with {selectedEnquiryForMessages.clientName}
                    </h3>
                  </div>
                  <p className="text-[11px] text-slate-500 mt-0.5">
                    {selectedEnquiryForMessages.taxServiceName || selectedEnquiryForMessages.serviceCategory} • Status: <span className="font-semibold">{selectedEnquiryForMessages.enquiryStatus}</span>
                  </p>
                </div>
              </div>
              <button
                onClick={() => setSelectedEnquiryForMessages(null)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Messages Thread Content */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-slate-50/50 dark:bg-slate-900/50 min-h-[250px]">
              {isLoadingMessages ? (
                <div className="flex flex-col items-center justify-center h-48 text-slate-400">
                  <Clock className="w-6 h-6 animate-spin mb-2" />
                  <span className="text-xs">Loading secure conversation...</span>
                </div>
              ) : !messageThread || messageThread.messages.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-48 text-center p-4">
                  <div className="p-3 bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 rounded-full mb-2">
                    <MessageSquare className="w-6 h-6" />
                  </div>
                  <p className="text-xs font-bold text-slate-700 dark:text-slate-300">No messages yet</p>
                  <p className="text-[11px] text-slate-500 mt-1 max-w-xs">
                    Start the conversation with {selectedEnquiryForMessages.clientName} regarding their tax requirement.
                  </p>
                </div>
              ) : (
                messageThread.messages.map((msg) => {
                  const isPractice = msg.senderType === 'PRACTICE_USER';
                  return (
                    <div
                      key={msg.id}
                      className={clsx('flex flex-col max-w-[80%]', isPractice ? 'ml-auto items-end' : 'mr-auto items-start')}
                    >
                      <div className="flex items-center gap-1.5 mb-1 px-1">
                        <span className="text-[10px] font-bold text-slate-600 dark:text-slate-400">
                          {msg.senderName}
                        </span>
                        {isPractice && (
                          <span className="text-[9px] font-semibold bg-indigo-100 dark:bg-indigo-900/40 text-indigo-700 dark:text-indigo-300 px-1.5 py-0.2 rounded">
                            Practice Staff
                          </span>
                        )}
                        <span className="text-[9px] text-slate-400">
                          {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                      <div
                        className={clsx(
                          'p-3 rounded-2xl text-xs leading-relaxed shadow-sm',
                          isPractice
                            ? 'bg-indigo-600 text-white rounded-tr-xs'
                            : 'bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 border border-slate-200 dark:border-slate-700 rounded-tl-xs'
                        )}
                      >
                        {msg.messageBody}
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Message Reply Input Box */}
            <div className="p-3 border-t border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900">
              {selectedEnquiryForMessages.enquiryStatus === 'CANCELLED' || selectedEnquiryForMessages.enquiryStatus === 'REJECTED' ? (
                <div className="p-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 text-center text-xs text-slate-500 font-medium">
                  This enquiry is {selectedEnquiryForMessages.enquiryStatus.toLowerCase()}. New messages cannot be sent.
                </div>
              ) : (
                <form onSubmit={handleSendMessage} className="flex items-center gap-2">
                  <input
                    type="text"
                    placeholder={`Reply to ${selectedEnquiryForMessages.clientName}...`}
                    value={messageText}
                    onChange={(e) => setMessageText(e.target.value)}
                    className="flex-1 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                  <Button
                    type="submit"
                    variant="primary"
                    size="sm"
                    disabled={!messageText.trim() || isSendingMessage}
                    className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold px-4 py-2.5"
                  >
                    {isSendingMessage ? (
                      <Clock className="w-4 h-4 animate-spin" />
                    ) : (
                      <Send className="w-4 h-4" />
                    )}
                  </Button>
                </form>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
