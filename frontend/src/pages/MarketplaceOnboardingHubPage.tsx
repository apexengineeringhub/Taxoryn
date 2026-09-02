import React, { useState, useEffect } from 'react';
import {
  FileText,
  CheckCircle2,
  Clock,
  Send,
  UserCheck,
  Building2,
  Search,
  Upload,
  Eye,
  AlertCircle,
  XCircle,
  ArrowRight,
  ShieldCheck,
  Sparkles,
  ExternalLink,
  ChevronRight,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { marketplaceOnboardingPracticeApi, employeeApi } from '../api/endpoints';
import {
  MarketplaceOnboarding,
  MarketplaceProposal,
  OnboardingStatus,
  Employee,
  ApproveAndPromoteClientRequest,
  CreateProposalRequest,
} from '../types';
import clsx from 'clsx';

export const MarketplaceOnboardingHubPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'pipeline' | 'proposals'>('pipeline');
  const [onboardings, setOnboardings] = useState<MarketplaceOnboarding[]>([]);
  const [proposals, setProposals] = useState<MarketplaceProposal[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [employees, setEmployees] = useState<Employee[]>([]);

  // Selected record for details/verification modal
  const [selectedOnboarding, setSelectedOnboarding] = useState<MarketplaceOnboarding | null>(null);
  const [promoteModalOpen, setPromoteModalOpen] = useState(false);
  const [promoteForm, setPromoteForm] = useState<ApproveAndPromoteClientRequest>({
    createOnboardingTask: true,
    provisionClientPortalUser: true,
    reviewerNotes: '',
  });

  // Rejection modal for a document
  const [rejectDocModalOpen, setRejectDocModalOpen] = useState(false);
  const [selectedDocId, setSelectedDocId] = useState<string | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');

  // Proposal modal
  const [proposalModalOpen, setProposalModalOpen] = useState(false);
  const [proposalForm, setProposalForm] = useState<CreateProposalRequest>({
    leadId: '',
    proposalTitle: 'Statutory Tax Compliance & Advisory Engagement',
    scopeOfWork: 'Preparation and filing of monthly GST returns (GSTR-1, GSTR-3B), TDS computations, advance tax forecasting, and audit preparation.',
    deliverables: 'Filed return acknowledgements (ARN), Monthly ITC analysis report, Form 26AS/AIS reconciliation sheet.',
    feeAmount: 4999,
    pricingType: 'MONTHLY_RETAINER',
    estimatedTimelineDays: 7,
  });

  const [actionLoading, setActionLoading] = useState(false);

  const fetchOnboardings = async () => {
    try {
      setLoading(true);
      const data = await marketplaceOnboardingPracticeApi.getOnboardings({
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        search: searchTerm || undefined,
      });
      setOnboardings(data?.content || []);
    } catch (err) {
      console.error('Failed to load onboardings', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchProposals = async () => {
    try {
      const data = await marketplaceOnboardingPracticeApi.getProposals({ size: 50 });
      setProposals(data?.content || []);
    } catch (err) {
      console.error('Failed to load proposals', err);
    }
  };

  const fetchEmployees = async () => {
    try {
      const data = await employeeApi.getAll({ size: 100 });
      setEmployees(data?.content || []);
    } catch (err) {
      console.error('Failed to load employees', err);
    }
  };

  useEffect(() => {
    fetchEmployees();
  }, []);

  useEffect(() => {
    if (activeTab === 'pipeline') {
      fetchOnboardings();
    } else {
      fetchProposals();
    }
  }, [activeTab, statusFilter, searchTerm]);

  const handleVerifyDoc = async (documentId: string, status: 'VERIFIED' | 'REJECTED', reason?: string) => {
    if (!selectedOnboarding) return;
    try {
      setActionLoading(true);
      await marketplaceOnboardingPracticeApi.verifyDocument(selectedOnboarding.id, documentId, {
        verificationStatus: status,
        rejectionReason: reason,
      });
      const updated = await marketplaceOnboardingPracticeApi.getOnboardingById(selectedOnboarding.id);
      setSelectedOnboarding(updated);
      fetchOnboardings();
    } catch (err) {
      console.error('Failed to verify document', err);
    } finally {
      setActionLoading(false);
      setRejectDocModalOpen(false);
      setRejectionReason('');
    }
  };

  const handlePromoteToClient = async () => {
    if (!selectedOnboarding) return;
    try {
      setActionLoading(true);
      const promoted = await marketplaceOnboardingPracticeApi.promoteToClient(selectedOnboarding.id, promoteForm);
      setSelectedOnboarding(promoted);
      setPromoteModalOpen(false);
      fetchOnboardings();
      alert(`Success! ${promoted.clientName} has been officially promoted to Client Master and provisioned for Client Portal.`);
    } catch (err) {
      console.error('Promotion failed', err);
      alert('Promotion failed. Please check required fields.');
    } finally {
      setActionLoading(false);
    }
  };

  const getStatusBadge = (status: OnboardingStatus) => {
    switch (status) {
      case 'INITIATED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">Initiated</span>;
      case 'DOCUMENTS_PENDING':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-800">Docs Pending</span>;
      case 'UNDER_REVIEW':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">Under Review</span>;
      case 'APPROVED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">Promoted to Client</span>;
      case 'REJECTED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">Rejected</span>;
      default:
        return null;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white p-6 rounded-xl border border-gray-200 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold text-gray-900">Client Onboarding & KYC Pipeline</h1>
            <span className="bg-emerald-100 text-emerald-800 text-xs px-2.5 py-0.5 rounded-full font-semibold">
              Strict Separation Architecture
            </span>
          </div>
          <p className="text-sm text-gray-500 mt-1">
            Prospects undergo formal proposal acceptance, KYC document checks, and engagement sign-off before being promoted to Client Master.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex bg-gray-100 p-1 rounded-lg">
            <button
              onClick={() => setActiveTab('pipeline')}
              className={clsx('px-4 py-2 text-sm font-medium rounded-md transition-colors', {
                'bg-white text-gray-900 shadow-sm': activeTab === 'pipeline',
                'text-gray-600 hover:text-gray-900': activeTab !== 'pipeline',
              })}
            >
              Onboarding Pipeline ({onboardings.length})
            </button>
            <button
              onClick={() => setActiveTab('proposals')}
              className={clsx('px-4 py-2 text-sm font-medium rounded-md transition-colors', {
                'bg-white text-gray-900 shadow-sm': activeTab === 'proposals',
                'text-gray-600 hover:text-gray-900': activeTab !== 'proposals',
              })}
            >
              Proposals ({proposals.length})
            </button>
          </div>
        </div>
      </div>

      {/* Pipeline View */}
      {activeTab === 'pipeline' && (
        <div className="space-y-4">
          {/* Filter Bar */}
          <div className="flex flex-wrap items-center justify-between gap-4 bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
            <div className="flex items-center gap-2">
              <div className="relative">
                <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Search by prospect name, email, PAN..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-9 pr-4 py-2 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent w-72"
                />
              </div>
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="text-sm border border-gray-300 rounded-lg px-3 py-2 bg-white"
              >
                <option value="ALL">All Stages</option>
                <option value="INITIATED">Initiated</option>
                <option value="DOCUMENTS_PENDING">Documents Pending</option>
                <option value="UNDER_REVIEW">Under Review</option>
                <option value="APPROVED">Promoted (Approved)</option>
              </select>
            </div>
          </div>

          {/* Table of Onboarding records & Mobile Cards */}
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
            {/* Desktop Table */}
            <div className="hidden md:block overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Prospect / Business</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Entity & Identifiers</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Engagement & KYC</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Stage</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Portal Access Token</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Action</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {loading ? (
                    <tr>
                      <td colSpan={6} className="px-6 py-8 text-center text-sm text-gray-500">Loading onboarding pipeline...</td>
                    </tr>
                  ) : onboardings.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="px-6 py-8 text-center text-sm text-gray-500">
                        No active onboarding records found. Accept an inbound lead or send a proposal to initiate onboarding.
                      </td>
                    </tr>
                  ) : (
                    onboardings.map((onb) => (
                      <tr key={onb.id} className="hover:bg-gray-50 transition-colors">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="font-semibold text-gray-900">{onb.clientName}</div>
                          <div className="text-xs text-gray-500">{onb.clientEmail} • {onb.clientPhone}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-xs font-medium text-gray-800">{onb.entityType}</div>
                          <div className="text-xs text-gray-500">
                            PAN: <span className="font-mono text-gray-700">{onb.pan || 'Not submitted'}</span>
                          </div>
                          {onb.gstin && (
                            <div className="text-xs text-gray-500">
                              GST: <span className="font-mono text-gray-700">{onb.gstin}</span>
                            </div>
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center gap-1.5 text-xs">
                            {onb.engagementLetterSigned ? (
                              <span className="text-green-600 flex items-center gap-1"><CheckCircle2 className="w-3.5 h-3.5" /> Engagement Signed</span>
                            ) : (
                              <span className="text-amber-600 flex items-center gap-1"><Clock className="w-3.5 h-3.5" /> Awaiting Signature</span>
                            )}
                          </div>
                          <div className="text-xs text-gray-500 mt-1">
                            KYC Docs: {onb.documents?.filter((d) => d.verificationStatus === 'VERIFIED').length || 0} / {onb.documents?.length || 0} Verified
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          {getStatusBadge(onb.onboardingStatus)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center gap-2">
                            <span className="font-mono text-xs text-gray-600 bg-gray-100 px-2 py-1 rounded">
                              {onb.accessToken.slice(0, 12)}...
                            </span>
                            <a
                              href={`/marketplace/onboarding/${onb.accessToken}`}
                              target="_blank"
                              rel="noreferrer"
                              className="text-primary-600 hover:text-primary-800"
                              title="Open Customer Self-Serve Onboarding Portal"
                            >
                              <ExternalLink className="w-4 h-4" />
                            </a>
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                          <Button
                            size="sm"
                            variant="secondary"
                            onClick={() => setSelectedOnboarding(onb)}
                          >
                            Review & Verify
                          </Button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Mobile Cards */}
            <div className="md:hidden divide-y divide-gray-200">
              {loading ? (
                <div className="p-8 text-center text-xs text-gray-500">Loading onboarding pipeline...</div>
              ) : onboardings.length === 0 ? (
                <div className="p-8 text-center text-xs text-gray-500">
                  No active onboarding records found. Accept an inbound lead or send a proposal to initiate onboarding.
                </div>
              ) : (
                onboardings.map((onb) => (
                  <div key={onb.id} className="p-4 space-y-2.5 text-xs hover:bg-gray-50/70 transition-colors">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="font-bold text-gray-900 text-sm">{onb.clientName}</div>
                        <div className="text-[11px] text-gray-500 mt-0.5">{onb.clientEmail} • {onb.clientPhone}</div>
                      </div>
                      <div className="shrink-0">{getStatusBadge(onb.onboardingStatus)}</div>
                    </div>

                    <div className="grid grid-cols-2 gap-2 pt-2 border-t border-gray-100 text-[11px]">
                      <div>
                        <span className="text-gray-400 block font-medium uppercase text-[10px]">Entity / PAN</span>
                        <span className="font-medium text-gray-800">{onb.entityType}</span>
                        <span className="font-mono text-gray-600 block">{onb.pan || 'Pending'}</span>
                      </div>
                      <div>
                        <span className="text-gray-400 block font-medium uppercase text-[10px]">KYC Progress</span>
                        <span className="text-gray-700 font-medium">
                          {onb.documents?.filter((d) => d.verificationStatus === 'VERIFIED').length || 0} / {onb.documents?.length || 0} Docs Verified
                        </span>
                        <span className={clsx('block font-medium', onb.engagementLetterSigned ? 'text-green-600' : 'text-amber-600')}>
                          {onb.engagementLetterSigned ? 'Signed' : 'Awaiting Sign'}
                        </span>
                      </div>
                    </div>

                    <div className="pt-2 border-t border-gray-100 flex items-center justify-between">
                      <a
                        href={`/marketplace/onboarding/${onb.accessToken}`}
                        target="_blank"
                        rel="noreferrer"
                        className="text-primary-600 hover:text-primary-800 inline-flex items-center gap-1 font-mono text-[11px]"
                      >
                        <span>Portal Link</span>
                        <ExternalLink className="w-3 h-3" />
                      </a>
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => setSelectedOnboarding(onb)}
                      >
                        Review & Verify
                      </Button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}

      {/* Proposals View */}
      {activeTab === 'proposals' && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
          {/* Desktop Table */}
          <div className="hidden md:block overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Proposal Title</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Client / Lead</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Fee Structure</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Public Access Token</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {proposals.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-6 py-8 text-center text-sm text-gray-500">No proposals sent yet.</td>
                  </tr>
                ) : (
                  proposals.map((prop) => (
                    <tr key={prop.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="font-semibold text-gray-900">{prop.proposalTitle}</div>
                        <div className="text-xs text-gray-500">Timeline: {prop.estimatedTimelineDays} days</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900">{prop.clientName || 'Prospect'}</div>
                        <div className="text-xs text-gray-500">{prop.clientEmail}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-bold text-gray-900">₹{prop.feeAmount.toLocaleString('en-IN')}</div>
                        <div className="text-xs text-gray-500">{prop.pricingType}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={clsx('px-2.5 py-0.5 rounded-full text-xs font-medium', {
                          'bg-blue-100 text-blue-800': prop.proposalStatus === 'SENT',
                          'bg-green-100 text-green-800': prop.proposalStatus === 'ACCEPTED',
                          'bg-red-100 text-red-800': prop.proposalStatus === 'REJECTED',
                        })}>
                          {prop.proposalStatus}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <a
                          href={`/marketplace/onboarding/${prop.accessToken}`}
                          target="_blank"
                          rel="noreferrer"
                          className="text-xs text-primary-600 hover:underline flex items-center gap-1 font-mono"
                        >
                          {prop.accessToken.slice(0, 14)}... <ExternalLink className="w-3 h-3" />
                        </a>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Mobile Cards */}
          <div className="md:hidden divide-y divide-gray-200">
            {proposals.length === 0 ? (
              <div className="p-8 text-center text-xs text-gray-500">No proposals sent yet.</div>
            ) : (
              proposals.map((prop) => (
                <div key={prop.id} className="p-4 space-y-2.5 text-xs hover:bg-gray-50/70 transition-colors">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <div className="font-bold text-gray-900 text-sm">{prop.proposalTitle}</div>
                      <div className="text-[11px] text-gray-500 mt-0.5">{prop.clientName || 'Prospect'} ({prop.clientEmail})</div>
                    </div>
                    <span className={clsx('px-2.5 py-0.5 rounded-full text-xs font-medium shrink-0', {
                      'bg-blue-100 text-blue-800': prop.proposalStatus === 'SENT',
                      'bg-green-100 text-green-800': prop.proposalStatus === 'ACCEPTED',
                      'bg-red-100 text-red-800': prop.proposalStatus === 'REJECTED',
                    })}>
                      {prop.proposalStatus}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 pt-2 border-t border-gray-100 text-[11px]">
                    <div>
                      <span className="text-gray-400 block font-medium uppercase text-[10px]">Fee Structure</span>
                      <span className="font-bold text-gray-900 text-xs">₹{prop.feeAmount.toLocaleString('en-IN')}</span>
                      <span className="text-gray-500 block">{prop.pricingType}</span>
                    </div>
                    <div>
                      <span className="text-gray-400 block font-medium uppercase text-[10px]">Timeline</span>
                      <span className="font-medium text-gray-700">{prop.estimatedTimelineDays} days</span>
                    </div>
                  </div>

                  <div className="pt-2 border-t border-gray-100">
                    <a
                      href={`/marketplace/onboarding/${prop.accessToken}`}
                      target="_blank"
                      rel="noreferrer"
                      className="text-xs text-primary-600 hover:underline inline-flex items-center gap-1 font-mono"
                    >
                      <span>View Public Proposal</span>
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* Selected Onboarding Review & Document Verification Modal */}
      {selectedOnboarding && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl max-w-3xl w-full max-h-[90vh] overflow-y-auto p-6 space-y-6 shadow-2xl">
            <div className="flex items-center justify-between border-b pb-4">
              <div>
                <h3 className="text-xl font-bold text-gray-900">
                  Onboarding Verification: {selectedOnboarding.clientName}
                </h3>
                <p className="text-xs text-gray-500 mt-0.5">
                  Review KYC documentation, entity details, and approve promotion to Client Master.
                </p>
              </div>
              <button
                onClick={() => setSelectedOnboarding(null)}
                className="text-gray-400 hover:text-gray-600 text-xl font-bold"
              >
                &times;
              </button>
            </div>

            {/* Profile Info Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 bg-gray-50 p-4 rounded-xl text-xs">
              <div>
                <span className="text-gray-500 block">Entity Type:</span>
                <span className="font-semibold text-gray-900">{selectedOnboarding.entityType}</span>
              </div>
              <div>
                <span className="text-gray-500 block">PAN:</span>
                <span className="font-mono font-semibold text-gray-900">{selectedOnboarding.pan || 'Pending'}</span>
              </div>
              <div>
                <span className="text-gray-500 block">GSTIN:</span>
                <span className="font-mono font-semibold text-gray-900">{selectedOnboarding.gstin || 'None / Pending'}</span>
              </div>
              <div>
                <span className="text-gray-500 block">Email:</span>
                <span className="text-gray-900">{selectedOnboarding.clientEmail}</span>
              </div>
              <div>
                <span className="text-gray-500 block">Phone:</span>
                <span className="text-gray-900">{selectedOnboarding.clientPhone}</span>
              </div>
              <div>
                <span className="text-gray-500 block">City, State:</span>
                <span className="text-gray-900">{selectedOnboarding.city || 'N/A'}, {selectedOnboarding.state || ''}</span>
              </div>
            </div>

            {/* Engagement Status */}
            <div className="p-4 rounded-xl border border-gray-200 bg-white space-y-2">
              <h4 className="text-xs font-bold uppercase tracking-wider text-gray-500">Engagement & Legal Sign-Off</h4>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  {selectedOnboarding.engagementLetterSigned ? (
                    <CheckCircle2 className="w-5 h-5 text-green-600" />
                  ) : (
                    <Clock className="w-5 h-5 text-amber-500" />
                  )}
                  <div>
                    <div className="text-sm font-semibold text-gray-900">
                      {selectedOnboarding.engagementLetterSigned ? 'Engagement Letter Signed' : 'Awaiting Engagement Letter Signature'}
                    </div>
                    <div className="text-xs text-gray-500">
                      Fee terms agreement: {selectedOnboarding.feeAgreementAgreed ? 'Agreed' : 'Pending agreement'}
                    </div>
                  </div>
                </div>
                {selectedOnboarding.engagementSignedAt && (
                  <span className="text-xs text-gray-400">
                    Signed on {new Date(selectedOnboarding.engagementSignedAt).toLocaleDateString()}
                  </span>
                )}
              </div>
            </div>

            {/* KYC Document Checklist */}
            <div className="space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-gray-500">KYC Document Verification</h4>
              <div className="space-y-2">
                {selectedOnboarding.documents?.map((doc) => (
                  <div
                    key={doc.id}
                    className="flex flex-col sm:flex-row sm:items-center justify-between p-3 rounded-lg border border-gray-200 bg-white hover:bg-gray-50 gap-2"
                  >
                    <div className="flex items-center gap-3">
                      <FileText className="w-5 h-5 text-gray-400 shrink-0" />
                      <div>
                        <div className="text-sm font-medium text-gray-900">{doc.documentName}</div>
                        <div className="text-xs text-gray-500">
                          Type: <span className="font-semibold">{doc.documentType}</span> • {doc.isRequired ? 'Mandatory' : 'Optional'}
                          {doc.filePath ? ' • File Submitted' : ' • No File Uploaded Yet'}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 self-end sm:self-auto">
                      <span className={clsx('px-2 py-0.5 text-xs rounded-full font-medium', {
                        'bg-amber-100 text-amber-800': doc.verificationStatus === 'PENDING',
                        'bg-green-100 text-green-800': doc.verificationStatus === 'VERIFIED',
                        'bg-red-100 text-red-800': doc.verificationStatus === 'REJECTED',
                      })}>
                        {doc.verificationStatus}
                      </span>
                      {doc.verificationStatus !== 'VERIFIED' && (
                        <button
                          onClick={() => handleVerifyDoc(doc.id, 'VERIFIED')}
                          disabled={actionLoading}
                          className="px-2.5 py-1 text-xs font-medium text-green-700 bg-green-50 hover:bg-green-100 rounded border border-green-200"
                        >
                          Approve
                        </button>
                      )}
                      {doc.verificationStatus !== 'REJECTED' && (
                        <button
                          onClick={() => {
                            setSelectedDocId(doc.id);
                            setRejectDocModalOpen(true);
                          }}
                          disabled={actionLoading}
                          className="px-2.5 py-1 text-xs font-medium text-red-700 bg-red-50 hover:bg-red-100 rounded border border-red-200"
                        >
                          Reject
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Promotion Action */}
            <div className="pt-4 border-t flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
              <div>
                {selectedOnboarding.promotedClientId ? (
                  <span className="text-xs font-semibold text-green-600 flex items-center gap-1">
                    <ShieldCheck className="w-4 h-4" /> Officially Promoted to Client Master (Client ID: {selectedOnboarding.promotedClientId.slice(0, 8)}...)
                  </span>
                ) : (
                  <span className="text-xs text-gray-500">
                    Promoting creates the official record in Client Master and provisions Client Portal credentials.
                  </span>
                )}
              </div>
              <div className="flex items-center justify-end gap-3">
                <Button variant="secondary" onClick={() => setSelectedOnboarding(null)}>
                  Close
                </Button>
                {!selectedOnboarding.promotedClientId && (
                  <Button
                    variant="primary"
                    onClick={() => setPromoteModalOpen(true)}
                    className="bg-emerald-600 hover:bg-emerald-700 text-white flex items-center gap-1.5"
                  >
                    <UserCheck className="w-4 h-4" /> Approve & Promote
                  </Button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Promote to Client Master Confirmation Modal */}
      {promoteModalOpen && selectedOnboarding && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl max-w-md w-full p-6 space-y-5 shadow-2xl max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center gap-3 text-emerald-600">
              <ShieldCheck className="w-8 h-8" />
              <div>
                <h3 className="text-lg font-bold text-gray-900">Promote to Client Master</h3>
                <p className="text-xs text-gray-500">Official engagement transition</p>
              </div>
            </div>

            <div className="bg-emerald-50 border border-emerald-200 p-3 rounded-lg text-xs text-emerald-800">
              This action creates an active record in your practice&apos;s Client database for <strong>{selectedOnboarding.clientName}</strong>.
            </div>

            <div className="space-y-3">
              <div>
                <label className="text-xs font-medium text-gray-700 block mb-1">Assign Primary Staff / Manager</label>
                <select
                  value={promoteForm.assignedEmployeeId || ''}
                  onChange={(e) => setPromoteForm({ ...promoteForm, assignedEmployeeId: e.target.value || undefined })}
                  className="w-full text-sm border border-gray-300 rounded-lg p-2 bg-white"
                >
                  <option value="">-- Unassigned (Auto-Assign to Partner) --</option>
                  {employees.map((emp) => (
                    <option key={emp.id} value={emp.id}>
                      {emp.firstName} {emp.lastName} ({emp.designation || 'Staff'})
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="taskCheck"
                  checked={promoteForm.createOnboardingTask}
                  onChange={(e) => setPromoteForm({ ...promoteForm, createOnboardingTask: e.target.checked })}
                  className="rounded text-primary-600 focus:ring-primary-500"
                />
                <label htmlFor="taskCheck" className="text-xs text-gray-700">
                  Auto-create initial onboarding compliance task
                </label>
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="portalCheck"
                  checked={promoteForm.provisionClientPortalUser}
                  onChange={(e) => setPromoteForm({ ...promoteForm, provisionClientPortalUser: e.target.checked })}
                  className="rounded text-primary-600 focus:ring-primary-500"
                />
                <label htmlFor="portalCheck" className="text-xs text-gray-700">
                  Auto-provision Client Portal user account
                </label>
              </div>

              <div>
                <label className="text-xs font-medium text-gray-700 block mb-1">Partner Review Notes</label>
                <textarea
                  rows={2}
                  placeholder="e.g., Verified physical documents. Approved for GST retainership."
                  value={promoteForm.reviewerNotes}
                  onChange={(e) => setPromoteForm({ ...promoteForm, reviewerNotes: e.target.value })}
                  className="w-full text-xs border border-gray-300 rounded-lg p-2"
                />
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <Button variant="secondary" onClick={() => setPromoteModalOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handlePromoteToClient}
                disabled={actionLoading}
                className="bg-emerald-600 hover:bg-emerald-700 text-white"
              >
                {actionLoading ? 'Promoting...' : 'Confirm Promotion'}
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Reject Document Modal */}
      {rejectDocModalOpen && selectedDocId && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl max-w-sm w-full p-6 space-y-4 shadow-2xl max-h-[90dvh] overflow-y-auto">
            <h3 className="text-base font-bold text-gray-900">Specify Rejection Reason</h3>
            <textarea
              rows={3}
              placeholder="e.g. Document image is blurry or expired."
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              className="w-full text-xs border border-gray-300 rounded-lg p-2"
            />
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setRejectDocModalOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="danger"
                size="sm"
                onClick={() => handleVerifyDoc(selectedDocId, 'REJECTED', rejectionReason)}
              >
                Confirm Rejection
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
