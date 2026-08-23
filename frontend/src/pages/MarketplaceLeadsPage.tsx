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
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { marketplacePracticeApi, marketplaceOnboardingPracticeApi, employeeApi } from '../api/endpoints';
import { MarketplaceLead, MarketplaceStats, Employee, CreateProposalRequest } from '../types';
import { useNavigate } from 'react-router-dom';
import clsx from 'clsx';

export const MarketplaceLeadsPage: React.FC = () => {
  const navigate = useNavigate();
  const [leads, setLeads] = useState<MarketplaceLead[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [stats, setStats] = useState<MarketplaceStats | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Filters
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState<string>('');

  // Proposal Modal
  const [selectedLeadForProposal, setSelectedLeadForProposal] = useState<MarketplaceLead | null>(null);
  const [proposalForm, setProposalForm] = useState<CreateProposalRequest>({
    leadId: '',
    proposalTitle: 'Statutory Tax Compliance & Advisory Engagement',
    scopeOfWork: 'Preparation and filing of monthly GST returns (GSTR-1, GSTR-3B), TDS computations, advance tax forecasting, and audit preparation.',
    deliverables: 'Filed return acknowledgements (ARN), Monthly ITC analysis report, Form 26AS/AIS reconciliation sheet.',
    feeAmount: 4999,
    pricingType: 'MONTHLY_RETAINER',
    estimatedTimelineDays: 7,
  });

  // Conversion Modal
  const [selectedLeadForConvert, setSelectedLeadForConvert] = useState<MarketplaceLead | null>(null);
  const [convertForm, setConvertForm] = useState({
    clientType: 'INDIVIDUAL',
    assignedEmployeeId: '',
    createOnboardingTask: true,
    notes: '',
  });
  const [isConverting, setIsConverting] = useState<boolean>(false);
  const [successBanner, setSuccessBanner] = useState<string | null>(null);

  const fetchLeadsData = async () => {
    setIsLoading(true);
    try {
      const [leadsRes, empRes, statsRes] = await Promise.all([
        marketplacePracticeApi.getMyLeads({
          status: statusFilter || undefined,
          search: searchTerm || undefined,
          size: 50,
        }),
        employeeApi.getAll({ size: 100 }).then((r) => r.content || []).catch(() => []),
        marketplacePracticeApi.getStats().catch(() => null),
      ]);

      setLeads(leadsRes.content || []);
      setEmployees(empRes || []);
      setStats(statsRes);
    } catch (err) {
      console.error('Failed to load marketplace leads', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchLeadsData();
  }, [statusFilter]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    fetchLeadsData();
  };

  const handleStatusChange = async (leadId: string, newStatus: string) => {
    try {
      await marketplacePracticeApi.updateLeadStatus(leadId, { status: newStatus });
      await fetchLeadsData();
    } catch (err) {
      alert('Failed to update lead status.');
    }
  };

  const handleConvertLead = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLeadForConvert) return;
    setIsConverting(true);
    try {
      const res = await marketplacePracticeApi.convertLeadToClient(selectedLeadForConvert.id, {
        clientType: convertForm.clientType,
        assignedEmployeeId: convertForm.assignedEmployeeId || undefined,
        createOnboardingTask: convertForm.createOnboardingTask,
        notes: convertForm.notes,
      });

      setSelectedLeadForConvert(null);
      await fetchLeadsData();
      setSuccessBanner(
        `Successfully converted ${selectedLeadForConvert.clientName} to an Active Practice CRM Client! Onboarding task initiated.`
      );
    } catch (err) {
      alert('Failed to convert lead to client.');
    } finally {
      setIsConverting(false);
    }
  };

  const handleSendProposal = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLeadForProposal) return;
    try {
      setIsConverting(true);
      const prop = await marketplaceOnboardingPracticeApi.sendProposal({
        ...proposalForm,
        leadId: selectedLeadForProposal.id,
      });
      setSelectedLeadForProposal(null);
      await fetchLeadsData();
      setSuccessBanner(
        `Formal engagement proposal dispatched to ${selectedLeadForProposal.clientName}! Public link: /marketplace/onboarding/${prop.accessToken}`
      );
    } catch (err) {
      alert('Failed to send engagement proposal.');
    } finally {
      setIsConverting(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6 pb-20">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2.5">
            <Users className="w-7 h-7 text-indigo-600" />
            <span>Marketplace Inbound Inquiries & CRM Leads</span>
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Track customer requests originating from the Taxoryn Marketplace directory and convert them into clients with 1 click.
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
          <button onClick={() => setSuccessBanner(null)} className="text-emerald-600">×</button>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
          <div className="text-xs font-bold uppercase tracking-wider text-slate-400">Total Inbound Leads</div>
          <div className="text-2xl font-extrabold text-slate-900 dark:text-white">{stats?.totalInboundLeads || 0}</div>
          <div className="text-[11px] text-slate-500">From public directory searches</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
          <div className="text-xs font-bold uppercase tracking-wider text-slate-400">Converted Clients</div>
          <div className="text-2xl font-extrabold text-emerald-600">{stats?.totalConvertedClients || 0}</div>
          <div className="text-[11px] text-emerald-600 font-semibold">{stats?.leadConversionRate || 0}% Conversion Rate</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
          <div className="text-xs font-bold uppercase tracking-wider text-slate-400">Consultations Booked</div>
          <div className="text-2xl font-extrabold text-indigo-600">{stats?.totalConsultationsBooked || 0}</div>
          <div className="text-[11px] text-slate-500">Direct scheduled appointments</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
          <div className="text-xs font-bold uppercase tracking-wider text-slate-400">Pipeline Value</div>
          <div className="text-2xl font-extrabold text-indigo-600">
            ₹{(stats?.estimatedMarketplacePipelineValue || 0).toLocaleString('en-IN')}
          </div>
          <div className="text-[11px] text-slate-500">Estimated compliance fees</div>
        </div>
      </div>

      {/* Privacy Protection Notice */}
      <div className="bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800/60 p-4 rounded-2xl flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl bg-emerald-100 dark:bg-emerald-900/50 flex items-center justify-center shrink-0">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />
        </div>
        <div className="text-xs">
          <span className="font-bold text-emerald-900 dark:text-emerald-200">Minimum Necessary Disclosure (Level 2 Active): </span>
          <span className="text-emerald-800/90 dark:text-emerald-300/80">
            Early marketplace inquiries provide only broad customer classification, requested tax service, and sanitized requirement summaries. Private tax documents, PAN, and detailed computations remain protected until mutual engagement.
          </span>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-2 flex-wrap w-full md:w-auto">
          {['', 'NEW', 'CONTACTED', 'PROPOSAL_SENT', 'CONVERTED', 'ARCHIVED'].map((st) => (
            <button
              key={st}
              onClick={() => setStatusFilter(st)}
              className={clsx(
                'px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all',
                statusFilter === st
                  ? 'bg-indigo-600 text-white'
                  : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-200'
              )}
            >
              {st || 'All Inquiries'}
            </button>
          ))}
        </div>

        <form onSubmit={handleSearch} className="flex items-center gap-2 w-full md:w-72">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
            <input
              type="text"
              placeholder="Search lead name, email, service..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
        </form>
      </div>

      {/* Leads Table */}
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-slate-500 text-sm">Loading inbound leads...</div>
        ) : leads.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <Users className="w-10 h-10 text-slate-400 mx-auto" />
            <h3 className="text-base font-bold text-slate-900 dark:text-white">No Marketplace Inquiries Found</h3>
            <p className="text-xs text-slate-500">
              When customers discover your practice on the Marketplace and submit requirements, they will appear here.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 dark:border-slate-800 bg-slate-50/75 dark:bg-slate-800/40 text-slate-400 font-bold uppercase tracking-wider">
                  <th className="p-4">Client / Prospect</th>
                  <th className="p-4">Contact (Masked)</th>
                  <th className="p-4">Tax Requirement Summary</th>
                  <th className="p-4">Status</th>
                  <th className="p-4">Assigned To</th>
                  <th className="p-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {leads.map((lead) => (
                  <tr key={lead.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                    <td className="p-4">
                      <div>
                        <div className="font-bold text-slate-900 dark:text-white text-sm">{lead.clientName}</div>
                        {lead.city && (
                          <div className="text-[11px] text-slate-500 flex items-center gap-1 mt-0.5">
                            <MapPin className="w-3 h-3 text-rose-500" />
                            {lead.city}
                          </div>
                        )}
                        <span className="inline-block mt-1 text-[10px] font-semibold bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 px-2 py-0.5 rounded-full border border-emerald-100 dark:border-emerald-800">
                          Level 2 Disclosure
                        </span>
                      </div>
                    </td>

                    <td className="p-4 space-y-1">
                      <div className="flex items-center gap-1.5 text-slate-700 dark:text-slate-300">
                        <Mail className="w-3.5 h-3.5 text-indigo-500" />
                        <span className="font-mono text-[11px]">{lead.clientEmail}</span>
                      </div>
                      <div className="flex items-center gap-1.5 text-slate-700 dark:text-slate-300">
                        <Phone className="w-3.5 h-3.5 text-emerald-500" />
                        <span className="font-mono text-[11px]">{lead.clientPhone}</span>
                      </div>
                    </td>

                    <td className="p-4 max-w-xs">
                      <div className="space-y-1">
                        <span className="text-[10px] font-bold uppercase tracking-wider bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 px-2 py-0.5 rounded">
                          {lead.serviceCategory || 'Tax Consulting'}
                        </span>
                        <p className="text-xs text-slate-600 dark:text-slate-300 line-clamp-2 leading-relaxed">
                          {lead.requirementDescription}
                        </p>
                      </div>
                    </td>

                    <td className="p-4">
                      <select
                        value={lead.leadStatus}
                        onChange={(e) => handleStatusChange(lead.id, e.target.value)}
                        disabled={lead.leadStatus === 'CONVERTED'}
                        className={clsx(
                          'text-xs font-bold px-2.5 py-1 rounded-xl border focus:outline-none',
                          lead.leadStatus === 'CONVERTED'
                            ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                            : lead.leadStatus === 'NEW'
                            ? 'bg-indigo-50 text-indigo-700 border-indigo-200'
                            : lead.leadStatus === 'CONTACTED'
                            ? 'bg-amber-50 text-amber-700 border-amber-200'
                            : 'bg-slate-100 text-slate-700 border-slate-200'
                        )}
                      >
                        <option value="NEW">NEW</option>
                        <option value="CONTACTED">CONTACTED</option>
                        <option value="PROPOSAL_SENT">PROPOSAL SENT</option>
                        <option value="CONVERTED" disabled>
                          CONVERTED
                        </option>
                        <option value="ARCHIVED">ARCHIVED</option>
                      </select>
                    </td>

                    <td className="p-4 text-slate-600 dark:text-slate-300">
                      {lead.assignedEmployeeName || 'Unassigned'}
                    </td>

                    <td className="p-4 text-right">
                      {lead.leadStatus === 'CONVERTED' ? (
                        <span className="inline-flex items-center gap-1 text-xs font-bold text-emerald-600">
                          <CheckCircle2 className="w-4 h-4" />
                          Client Active
                        </span>
                      ) : (
                        <div className="flex items-center justify-end gap-2">
                          <Button
                            size="sm"
                            variant="secondary"
                            onClick={() => {
                              setSelectedLeadForProposal(lead);
                              setProposalForm({
                                leadId: lead.id,
                                proposalTitle: `Engagement for ${lead.serviceCategory || 'Tax Advisory'}`,
                                scopeOfWork: lead.requirementDescription || 'Statutory tax compliance, documentation and representation.',
                                deliverables: 'Filing acknowledgements, monthly ITC reconciliations, and compliance reports.',
                                feeAmount: 3999,
                                pricingType: 'MONTHLY_RETAINER',
                                estimatedTimelineDays: 7,
                              });
                            }}
                            className="text-xs"
                          >
                            <FileText className="w-3.5 h-3.5 mr-1" />
                            Send Proposal
                          </Button>
                          <Button
                            size="sm"
                            variant="primary"
                            onClick={() => {
                              setSelectedLeadForConvert(lead);
                              setConvertForm({
                                clientType: 'INDIVIDUAL',
                                assignedEmployeeId: lead.assignedEmployeeId || '',
                                createOnboardingTask: true,
                                notes: `Acquired from Marketplace Inquiry: ${lead.requirementDescription}`,
                              });
                            }}
                            className="rounded-xl font-bold bg-emerald-600 hover:bg-emerald-700 text-white text-xs"
                          >
                            <UserPlus className="w-3.5 h-3.5 mr-1" />
                            Convert
                          </Button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

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
                  <p className="text-xs text-slate-500">For {selectedLeadForProposal.clientName} ({selectedLeadForProposal.clientEmail})</p>
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
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Pricing Model</label>
                  <select
                    value={proposalForm.pricingType}
                    onChange={(e) => setProposalForm({ ...proposalForm, pricingType: e.target.value as any })}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  >
                    <option value="FIXED">One-Time Fixed Fee</option>
                    <option value="MONTHLY_RETAINER">Monthly Retainer</option>
                    <option value="HOURLY">Hourly Advisory</option>
                  </select>
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t">
                <Button variant="secondary" onClick={() => setSelectedLeadForProposal(null)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" disabled={isConverting}>
                  {isConverting ? 'Sending...' : 'Dispatch Proposal to Client'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Convert to Client Modal */}
      {selectedLeadForConvert && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-md w-full p-6 space-y-5 shadow-2xl">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-2xl bg-emerald-50 text-emerald-600">
                <UserCheck className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900 dark:text-white">
                  Convert {selectedLeadForConvert.clientName} to CRM Client
                </h3>
                <p className="text-xs text-slate-500">Instantly provision Client Master record & Onboarding Task.</p>
              </div>
            </div>

            <form onSubmit={handleConvertLead} className="space-y-4">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Client Constitution Type *</label>
                <select
                  value={convertForm.clientType}
                  onChange={(e) => setConvertForm({ ...convertForm, clientType: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                >
                  <option value="INDIVIDUAL">Individual / Salaried</option>
                  <option value="COMPANY">Private Limited Company</option>
                  <option value="LLP">Limited Liability Partnership (LLP)</option>
                  <option value="FIRM">Partnership Firm</option>
                  <option value="HUF">Hindu Undivided Family (HUF)</option>
                  <option value="TRUST">Trust / NGO</option>
                </select>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Assign Account Manager</label>
                <select
                  value={convertForm.assignedEmployeeId}
                  onChange={(e) => setConvertForm({ ...convertForm, assignedEmployeeId: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                >
                  <option value="">Select Practitioner / Employee</option>
                  {employees.map((emp) => (
                    <option key={emp.id} value={emp.id}>
                      {emp.fullName} ({emp.designation || 'Staff'})
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex items-center gap-2 p-3 bg-emerald-50 dark:bg-emerald-950/40 rounded-xl border border-emerald-100 dark:border-emerald-900/40">
                <input
                  type="checkbox"
                  id="taskCheck"
                  checked={convertForm.createOnboardingTask}
                  onChange={(e) => setConvertForm({ ...convertForm, createOnboardingTask: e.target.checked })}
                  className="w-4 h-4 text-emerald-600 rounded"
                />
                <label htmlFor="taskCheck" className="text-xs font-semibold text-emerald-900 dark:text-emerald-200">
                  Auto-create Initial Onboarding & KYC Collection Task
                </label>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Practitioner Onboarding Notes</label>
                <textarea
                  rows={2}
                  value={convertForm.notes}
                  onChange={(e) => setConvertForm({ ...convertForm, notes: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100 dark:border-slate-800">
                <Button type="button" variant="outline" size="sm" onClick={() => setSelectedLeadForConvert(null)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isConverting} className="bg-emerald-600 hover:bg-emerald-700 text-white">
                  {isConverting ? 'Provisioning...' : 'Provision Client Record'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
