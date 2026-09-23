import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  Briefcase,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Filter,
  Plus,
  Search,
  User,
  Calendar,
  ChevronRight,
  ArrowUpDown,
  RefreshCw,
  FileText,
  ShieldAlert,
  ExternalLink,
  Edit2,
  Check,
  X,
  Eye,
} from 'lucide-react';
import { complianceWorkApi, clientApi, clientServicesApi, employeeApi } from '../api/endpoints';
import { useAuth } from '../context/AuthContext';
import {
  ComplianceWorkItem,
  ComplianceWorkStatus,
  ComplianceWorkType,
  Client,
  ClientServiceDto,
  Employee,
} from '../types';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import clsx from 'clsx';

export const ComplianceWorkPage: React.FC = () => {
  const { user } = useAuth();

  const [workItems, setWorkItems] = useState<ComplianceWorkItem[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // View Filter: ALL | MY_WORK | PENDING_REVIEW | OVERDUE
  const [activeView, setActiveView] = useState<'ALL' | 'MY_WORK' | 'PENDING_REVIEW' | 'OVERDUE'>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [workTypeFilter, setWorkTypeFilter] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  // Auxiliary data for modals & filters
  const [clients, setClients] = useState<Client[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);

  // Create Modal State
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedClientId, setSelectedClientId] = useState('');
  const [clientServices, setClientServices] = useState<ClientServiceDto[]>([]);
  const [selectedServiceId, setSelectedServiceId] = useState('');
  const [workType, setWorkType] = useState<ComplianceWorkType>('GST_RETURN');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [financialYear, setFinancialYear] = useState('2026-27');
  const [assessmentYear, setAssessmentYear] = useState('2027-28');
  const [compliancePeriod, setCompliancePeriod] = useState('');
  const [statutoryDueDate, setStatutoryDueDate] = useState('');
  const [internalTargetDate, setInternalTargetDate] = useState('');
  const [assignedEmployeeId, setAssignedEmployeeId] = useState('');
  const [reviewerEmployeeId, setReviewerEmployeeId] = useState('');
  const [isSubmittingCreate, setIsSubmittingCreate] = useState(false);

  // Status Change Modal State
  const [selectedWorkItem, setSelectedWorkItem] = useState<ComplianceWorkItem | null>(null);
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [newStatus, setNewStatus] = useState<ComplianceWorkStatus>('IN_PREPARATION');
  const [statusNotes, setStatusNotes] = useState('');
  const [isSubmittingStatus, setIsSubmittingStatus] = useState(false);

  // Assign Modal State
  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [assigneeId, setAssigneeId] = useState('');
  const [reviewerId, setReviewerId] = useState('');
  const [isSubmittingAssign, setIsSubmittingAssign] = useState(false);

  const fetchWorkItems = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);

      const params: any = {
        page: currentPage,
        size: 15,
        search: searchTerm.trim() || undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        workType: workTypeFilter !== 'ALL' ? workTypeFilter : undefined,
      };

      if (activeView === 'MY_WORK') params.myWorkOnly = true;
      if (activeView === 'PENDING_REVIEW') params.pendingReviewOnly = true;
      if (activeView === 'OVERDUE') params.overdue = true;

      const res = await complianceWorkApi.getAll(params);
      setWorkItems(res.content || []);
      setTotalElements(res.totalElements || 0);
      setTotalPages(res.totalPages || 0);
    } catch (err: any) {
      console.error('Failed to load compliance work items', err);
      setError(err.response?.data?.message || 'Failed to load compliance work items');
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, activeView, statusFilter, workTypeFilter, searchTerm]);

  useEffect(() => {
    fetchWorkItems();
  }, [fetchWorkItems]);

  useEffect(() => {
    // Load clients & employees once
    clientApi.getAll({ size: 100 }).then((res) => setClients(res.content || [])).catch(() => {});
    employeeApi.getAll({ size: 100 }).then((res) => setEmployees(res.content || [])).catch(() => {});
  }, []);

  // When client is selected in create modal, load their services
  useEffect(() => {
    if (selectedClientId) {
      clientServicesApi.getByClientId(selectedClientId).then((services) => {
        setClientServices(services || []);
        if (services && services.length > 0) {
          setSelectedServiceId(services[0].id);
        } else {
          setSelectedServiceId('');
        }
      }).catch(() => setClientServices([]));
    } else {
      setClientServices([]);
      setSelectedServiceId('');
    }
  }, [selectedClientId]);

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedClientId || !selectedServiceId || !title.trim()) return;

    try {
      setIsSubmittingCreate(true);
      await complianceWorkApi.create({
        clientId: selectedClientId,
        clientServiceId: selectedServiceId,
        workType,
        title: title.trim(),
        description: description.trim() || undefined,
        financialYear: financialYear || undefined,
        assessmentYear: assessmentYear || undefined,
        compliancePeriod: compliancePeriod.trim() || undefined,
        statutoryDueDate: statutoryDueDate || undefined,
        internalTargetDate: internalTargetDate || undefined,
        assignedEmployeeId: assignedEmployeeId || undefined,
        reviewerEmployeeId: reviewerEmployeeId || undefined,
      });

      setIsCreateModalOpen(false);
      resetCreateForm();
      fetchWorkItems();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create compliance work item');
    } finally {
      setIsSubmittingCreate(false);
    }
  };

  const handleStatusSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedWorkItem) return;

    try {
      setIsSubmittingStatus(true);
      await complianceWorkApi.updateStatus(selectedWorkItem.id, {
        status: newStatus,
        notes: statusNotes.trim() || undefined,
      });

      setIsStatusModalOpen(false);
      setSelectedWorkItem(null);
      fetchWorkItems();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update work item status');
    } finally {
      setIsSubmittingStatus(false);
    }
  };

  const handleAssignSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedWorkItem) return;

    try {
      setIsSubmittingAssign(true);
      await complianceWorkApi.assign(selectedWorkItem.id, {
        assignedEmployeeId: assigneeId || undefined,
        reviewerEmployeeId: reviewerId || undefined,
      });

      setIsAssignModalOpen(false);
      setSelectedWorkItem(null);
      fetchWorkItems();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to assign compliance work item');
    } finally {
      setIsSubmittingAssign(false);
    }
  };

  const resetCreateForm = () => {
    setSelectedClientId('');
    setSelectedServiceId('');
    setWorkType('GST_RETURN');
    setTitle('');
    setDescription('');
    setFinancialYear('2026-27');
    setAssessmentYear('2027-28');
    setCompliancePeriod('');
    setStatutoryDueDate('');
    setInternalTargetDate('');
    setAssignedEmployeeId('');
    setReviewerEmployeeId('');
  };

  const getStatusBadgeClass = (status: ComplianceWorkStatus) => {
    switch (status) {
      case 'NOT_STARTED':
        return 'bg-slate-100 text-slate-700 border-slate-200';
      case 'DOCUMENTS_PENDING':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'IN_PREPARATION':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'IN_REVIEW':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'READY_TO_FILE':
        return 'bg-teal-50 text-teal-700 border-teal-200';
      case 'FILED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'COMPLETED':
        return 'bg-emerald-100 text-emerald-800 border-emerald-300';
      case 'ON_HOLD':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'CANCELLED':
        return 'bg-zinc-100 text-zinc-500 border-zinc-200';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-slate-900 flex items-center gap-2.5">
            <Briefcase className="w-6 h-6 text-brand-600" />
            <span>Compliance Work Management</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Operational deliverables, return preparation lifecycles, and statutory filing workflows.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Button
            variant="outline"
            size="sm"
            onClick={fetchWorkItems}
            disabled={isLoading}
            leftIcon={<RefreshCw className={clsx('w-3.5 h-3.5', isLoading && 'animate-spin')} />}
          >
            Refresh
          </Button>
          <Button
            variant="primary"
            size="sm"
            leftIcon={<Plus className="w-4 h-4" />}
            onClick={() => setIsCreateModalOpen(true)}
          >
            New Work Item
          </Button>
        </div>
      </div>

      {/* View Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-200 pb-2 overflow-x-auto">
        {[
          { id: 'ALL', label: 'All Work' },
          { id: 'MY_WORK', label: 'My Worklist' },
          { id: 'PENDING_REVIEW', label: 'Pending Review' },
          { id: 'OVERDUE', label: 'Overdue Deliverables' },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => {
              setActiveView(tab.id as any);
              setCurrentPage(0);
            }}
            className={clsx(
              'px-3.5 py-1.5 rounded-lg text-xs font-bold transition-colors whitespace-nowrap',
              activeView === tab.id
                ? 'bg-brand-50 text-brand-700 border border-brand-200'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Filter Bar */}
      <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-2xs grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3">
        <div>
          <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
            Search
          </label>
          <div className="relative">
            <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-2.5" />
            <input
              type="text"
              placeholder="Search title or client..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setCurrentPage(0);
              }}
              className="w-full text-xs pl-8 pr-3 py-1.5 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
        </div>

        <div>
          <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
            Status
          </label>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setCurrentPage(0);
            }}
            className="w-full text-xs px-3 py-1.5 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
          >
            <option value="ALL">All Statuses</option>
            <option value="NOT_STARTED">Not Started</option>
            <option value="DOCUMENTS_PENDING">Documents Pending</option>
            <option value="IN_PREPARATION">In Preparation</option>
            <option value="IN_REVIEW">In Review</option>
            <option value="READY_TO_FILE">Ready to File</option>
            <option value="FILED">Filed</option>
            <option value="COMPLETED">Completed</option>
            <option value="ON_HOLD">On Hold</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>

        <div>
          <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
            Work Type
          </label>
          <select
            value={workTypeFilter}
            onChange={(e) => {
              setWorkTypeFilter(e.target.value);
              setCurrentPage(0);
            }}
            className="w-full text-xs px-3 py-1.5 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
          >
            <option value="ALL">All Work Types</option>
            <option value="GST_RETURN">GST Return Filing</option>
            <option value="ITR_RETURN">Income Tax Return</option>
            <option value="TDS_RETURN">TDS / TCS Return</option>
            <option value="TAX_NOTICE">Tax Notice & Hearing</option>
            <option value="COMPLIANCE_TASK">Statutory Compliance Task</option>
            <option value="DOCUMENT_COLLECTION">Document Collection</option>
            <option value="OTHER">Other Professional Work</option>
          </select>
        </div>

        <div className="flex items-end">
          <Button
            variant="ghost"
            size="sm"
            className="w-full text-xs text-slate-600 justify-center"
            onClick={() => {
              setStatusFilter('ALL');
              setWorkTypeFilter('ALL');
              setSearchTerm('');
              setCurrentPage(0);
            }}
          >
            Reset Filters
          </Button>
        </div>
      </div>

      {/* Main List Table / Cards */}
      {isLoading ? (
        <div className="min-h-[40vh] flex flex-col items-center justify-center space-y-3">
          <div className="w-8 h-8 border-3 border-brand-500 border-t-transparent rounded-full animate-spin" />
          <p className="text-xs text-slate-500">Loading compliance deliverables...</p>
        </div>
      ) : error ? (
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-6 text-center text-rose-700 space-y-2">
          <AlertTriangle className="w-8 h-8 mx-auto text-rose-600" />
          <p className="text-xs font-semibold">{error}</p>
        </div>
      ) : workItems.length === 0 ? (
        <div className="bg-white border-2 border-dashed border-slate-200 rounded-2xl p-12 text-center space-y-3">
          <Briefcase className="w-10 h-10 text-slate-300 mx-auto" />
          <h3 className="text-sm font-bold text-slate-900">No Compliance Work Items Found</h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">
            {searchTerm || statusFilter !== 'ALL'
              ? 'No records match your active search filters.'
              : 'Create period-specific compliance work items to track filing deadlines, preparation, and review workflows.'}
          </p>
          <Button
            size="sm"
            variant="primary"
            leftIcon={<Plus className="w-3.5 h-3.5" />}
            onClick={() => setIsCreateModalOpen(true)}
          >
            Create Work Item
          </Button>
        </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-2xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-[10px] font-black text-slate-400 uppercase tracking-wider">
                  <th className="py-3 px-4">Deliverable Title & Client</th>
                  <th className="py-3 px-4">Work Type</th>
                  <th className="py-3 px-4">Period / AY</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Statutory Due</th>
                  <th className="py-3 px-4">Assignee & Reviewer</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-xs">
                {workItems.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="py-3 px-4">
                      <div className="font-bold text-slate-900">{item.title}</div>
                      <div className="text-[11px] text-slate-500 flex items-center gap-1.5 mt-0.5">
                        <Link
                          to={`/clients/${item.clientId}`}
                          className="font-medium text-brand-600 hover:underline flex items-center gap-1"
                        >
                          <span>{item.clientName || 'Client Profile'}</span>
                        </Link>
                        {item.serviceName && (
                          <>
                            <span>•</span>
                            <span className="text-slate-400">{item.serviceName}</span>
                          </>
                        )}
                      </div>
                    </td>

                    <td className="py-3 px-4">
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-slate-100 text-slate-700">
                        {item.workType}
                      </span>
                    </td>

                    <td className="py-3 px-4">
                      <div className="font-medium text-slate-800">
                        {item.compliancePeriod || item.financialYear || '—'}
                      </div>
                      {item.assessmentYear && (
                        <div className="text-[10px] text-slate-400">AY {item.assessmentYear}</div>
                      )}
                    </td>

                    <td className="py-3 px-4">
                      <span
                        className={clsx(
                          'text-[10px] font-bold px-2 py-0.5 rounded-full border',
                          getStatusBadgeClass(item.status)
                        )}
                      >
                        {item.status.replace(/_/g, ' ')}
                      </span>
                    </td>

                    <td className="py-3 px-4">
                      {item.statutoryDueDate ? (
                        <div>
                          <span
                            className={clsx(
                              'font-semibold block',
                              item.overdue ? 'text-rose-600' : 'text-slate-800'
                            )}
                          >
                            {new Date(item.statutoryDueDate).toLocaleDateString()}
                          </span>
                          {item.overdue && (
                            <span className="text-[9px] font-bold text-rose-600 uppercase">
                              Overdue
                            </span>
                          )}
                        </div>
                      ) : (
                        <span className="text-slate-400">—</span>
                      )}
                    </td>

                    <td className="py-3 px-4">
                      <div className="space-y-0.5 text-[11px]">
                        <div className="flex items-center gap-1 text-slate-700">
                          <User className="w-3 h-3 text-brand-500" />
                          <span>{item.assignedEmployeeName || 'Unassigned'}</span>
                        </div>
                        {item.reviewerEmployeeName && (
                          <div className="text-[10px] text-slate-400">
                            Rev: {item.reviewerEmployeeName}
                          </div>
                        )}
                      </div>
                    </td>

                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => {
                            setSelectedWorkItem(item);
                            setNewStatus(item.status);
                            setStatusNotes('');
                            setIsStatusModalOpen(true);
                          }}
                          className="px-2 py-1 text-[10px] font-bold text-brand-700 bg-brand-50 hover:bg-brand-100 rounded-md border border-brand-200 transition-colors"
                          title="Change Status"
                        >
                          Status
                        </button>
                        <button
                          onClick={() => {
                            setSelectedWorkItem(item);
                            setAssigneeId(item.assignedEmployeeId || '');
                            setReviewerId(item.reviewerEmployeeId || '');
                            setIsAssignModalOpen(true);
                          }}
                          className="px-2 py-1 text-[10px] font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-md border border-slate-300 transition-colors"
                          title="Reassign Practitioner"
                        >
                          Assign
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="py-3 px-4 bg-slate-50/50 border-t border-slate-200 flex items-center justify-between text-xs text-slate-500">
              <div>
                Showing page <span className="font-bold text-slate-800">{currentPage + 1}</span> of{' '}
                <span className="font-bold text-slate-800">{totalPages}</span> ({totalElements} total items)
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage === 0}
                  onClick={() => setCurrentPage((p) => p - 1)}
                >
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage >= totalPages - 1}
                  onClick={() => setCurrentPage((p) => p + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* 1. CREATE WORK ITEM MODAL */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Create Compliance Work Item"
        subtitle="Initiate a period-specific compliance deliverable"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Select Client <span className="text-rose-500">*</span>
            </label>
            <select
              required
              value={selectedClientId}
              onChange={(e) => setSelectedClientId(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
            >
              <option value="">-- Choose Client --</option>
              {clients.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.displayName}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Client Service Engagement <span className="text-rose-500">*</span>
            </label>
            <select
              required
              disabled={!selectedClientId || clientServices.length === 0}
              value={selectedServiceId}
              onChange={(e) => setSelectedServiceId(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white disabled:bg-slate-50"
            >
              {clientServices.length === 0 ? (
                <option value="">{selectedClientId ? 'No active services for client' : 'Select client first'}</option>
              ) : (
                clientServices.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.serviceName} ({s.serviceType})
                  </option>
                ))
              )}
            </select>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Work Type <span className="text-rose-500">*</span>
              </label>
              <select
                value={workType}
                onChange={(e) => setWorkType(e.target.value as any)}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
              >
                <option value="GST_RETURN">GST Return Filing</option>
                <option value="ITR_RETURN">Income Tax Return</option>
                <option value="TDS_RETURN">TDS / TCS Return</option>
                <option value="TAX_NOTICE">Tax Notice Response / Hearing</option>
                <option value="COMPLIANCE_TASK">General Statutory Compliance</option>
                <option value="DOCUMENT_COLLECTION">Document & Data Collection</option>
                <option value="OTHER">Other Professional Work</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Compliance Period / Quarter
              </label>
              <input
                type="text"
                placeholder="e.g. September 2026 or Q2"
                value={compliancePeriod}
                onChange={(e) => setCompliancePeriod(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Deliverable Title <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              placeholder="e.g. GSTR-3B Filing September 2026"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Statutory Due Date
              </label>
              <input
                type="date"
                value={statutoryDueDate}
                onChange={(e) => setStatutoryDueDate(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Internal Target Date
              </label>
              <input
                type="date"
                value={internalTargetDate}
                onChange={(e) => setInternalTargetDate(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Assigned Practitioner
              </label>
              <select
                value={assignedEmployeeId}
                onChange={(e) => setAssignedEmployeeId(e.target.value)}
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
                Reviewer / Partner
              </label>
              <select
                value={reviewerEmployeeId}
                onChange={(e) => setReviewerEmployeeId(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
              >
                <option value="">No Reviewer Assigned</option>
                {employees.map((emp) => (
                  <option key={emp.id} value={emp.id}>
                    {emp.firstName} {emp.lastName || ''} ({emp.email})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="pt-3 flex items-center justify-end gap-2 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsCreateModalOpen(false)}
            >
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmittingCreate}>
              Create Work Item
            </Button>
          </div>
        </form>
      </Modal>

      {/* 2. CHANGE STATUS MODAL */}
      <Modal
        isOpen={isStatusModalOpen}
        onClose={() => setIsStatusModalOpen(false)}
        title="Update Lifecycle Status"
        subtitle={selectedWorkItem ? `${selectedWorkItem.title} (${selectedWorkItem.clientName})` : ''}
      >
        <form onSubmit={handleStatusSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Target Lifecycle Status <span className="text-rose-500">*</span>
            </label>
            <select
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value as any)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
            >
              <option value="NOT_STARTED">Not Started</option>
              <option value="DOCUMENTS_PENDING">Documents Pending</option>
              <option value="IN_PREPARATION">In Preparation</option>
              <option value="IN_REVIEW">In Review</option>
              <option value="READY_TO_FILE">Ready to File</option>
              <option value="FILED">Filed</option>
              <option value="COMPLETED">Completed</option>
              <option value="ON_HOLD">On Hold</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Transition Note / Audit Remark
            </label>
            <textarea
              rows={3}
              placeholder="e.g. Prepared computation, awaiting partner signoff..."
              value={statusNotes}
              onChange={(e) => setStatusNotes(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="pt-3 flex items-center justify-end gap-2 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsStatusModalOpen(false)}
            >
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmittingStatus}>
              Update Status
            </Button>
          </div>
        </form>
      </Modal>

      {/* 3. ASSIGN PRACTITIONER MODAL */}
      <Modal
        isOpen={isAssignModalOpen}
        onClose={() => setIsAssignModalOpen(false)}
        title="Assign Practitioner & Reviewer"
        subtitle={selectedWorkItem ? selectedWorkItem.title : ''}
      >
        <form onSubmit={handleAssignSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Assigned Practitioner / Preparer
            </label>
            <select
              value={assigneeId}
              onChange={(e) => setAssigneeId(e.target.value)}
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
              Reviewer / Quality Lead
            </label>
            <select
              value={reviewerId}
              onChange={(e) => setReviewerId(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
            >
              <option value="">No Reviewer Assigned</option>
              {employees.map((emp) => (
                <option key={emp.id} value={emp.id}>
                  {emp.firstName} {emp.lastName || ''} ({emp.email})
                </option>
              ))}
            </select>
          </div>

          <div className="pt-3 flex items-center justify-end gap-2 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsAssignModalOpen(false)}
            >
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmittingAssign}>
              Save Assignment
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
