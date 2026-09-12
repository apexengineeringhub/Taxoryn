import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Scale,
  Plus,
  Search,
  Filter,
  AlertTriangle,
  Clock,
  CheckCircle2,
  Calendar,
  IndianRupee,
  FileText,
  User,
  Building,
  ChevronRight,
  Eye,
  ArrowUpRight,
  ShieldAlert,
  Flame,
  Gavel,
  CheckCheck,
  Send,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { noticesApi, clientApi, employeeApi } from '../api/endpoints';
import {
  TaxNotice,
  NoticeDashboardStats,
  NoticeDepartment,
  NoticeStatus,
  NoticePriority,
  CreateTaxNoticeRequest,
  Client,
  Employee,
} from '../types';
import clsx from 'clsx';

export const NoticeCenterPage: React.FC = () => {
  const navigate = useNavigate();
  const [notices, setNotices] = useState<TaxNotice[]>([]);
  const [stats, setStats] = useState<NoticeDashboardStats | null>(null);
  const [clients, setClients] = useState<Client[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isStatsLoading, setIsStatsLoading] = useState(true);

  // Filters
  const [search, setSearch] = useState('');
  const [selectedDept, setSelectedDept] = useState<string>('ALL');
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [selectedPriority, setSelectedPriority] = useState<string>('ALL');
  const [overdueOnly, setOverdueOnly] = useState(false);
  const [upcomingHearingOnly, setUpcomingHearingOnly] = useState(false);

  // Create Modal State
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const [formData, setFormData] = useState<CreateTaxNoticeRequest>({
    clientId: '',
    noticeNumber: '',
    dinNumber: '',
    department: 'INCOME_TAX',
    noticeType: '143(1) - Summary Intimation',
    section: '143(1)',
    subject: '',
    description: '',
    assessmentYear: '2024-25',
    financialYear: '2023-24',
    taxPeriod: '',
    demandAmount: 0,
    receivedDate: new Date().toISOString().split('T')[0],
    responseDueDate: new Date(Date.now() + 15 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    hearingDate: '',
    hearingTime: '',
    priority: 'MEDIUM',
    assignedEmployeeId: '',
    reviewerEmployeeId: '',
    partnerEmployeeId: '',
    issuingAuthority: 'Income Tax Department, CPC Bengaluru',
    issuingOfficerName: '',
    internalNotes: '',
    createIntakeTask: true,
  });

  useEffect(() => {
    loadDashboardStats();
    loadLookupData();
  }, []);

  useEffect(() => {
    loadNotices();
  }, [search, selectedDept, selectedStatus, selectedPriority, overdueOnly, upcomingHearingOnly]);

  const loadDashboardStats = async () => {
    try {
      setIsStatsLoading(true);
      const data = await noticesApi.getDashboardStats();
      setStats(data);
    } catch (err) {
      console.error('Failed to load notice dashboard statistics', err);
    } finally {
      setIsStatsLoading(false);
    }
  };

  const loadLookupData = async () => {
    try {
      const [clientsRes, empsRes] = await Promise.allSettled([
        clientApi.getAll({ size: 100 }),
        employeeApi.getAll({ size: 100 }),
      ]);
      if (clientsRes.status === 'fulfilled' && clientsRes.value) {
        setClients(clientsRes.value.content || []);
      }
      if (empsRes.status === 'fulfilled' && empsRes.value) {
        setEmployees(empsRes.value.content || []);
      }
    } catch (err) {
      console.error('Failed to load client/employee lookup data', err);
    }
  };

  const loadNotices = async () => {
    try {
      setIsLoading(true);
      const params: any = {
        size: 50,
        sortBy: 'responseDueDate',
        sortDirection: 'ASC',
      };
      if (search.trim()) params.search = search.trim();
      if (selectedDept !== 'ALL') params.department = selectedDept;
      if (selectedStatus !== 'ALL') params.status = selectedStatus;
      if (selectedPriority !== 'ALL') params.priority = selectedPriority;
      if (overdueOnly) params.overdueOnly = true;
      if (upcomingHearingOnly) params.upcomingHearing = true;

      const res = await noticesApi.getNotices(params);
      setNotices(res.content || []);
    } catch (err) {
      console.error('Failed to load tax notices', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateNotice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.clientId) {
      setFormError('Please select a client');
      return;
    }
    if (!formData.noticeNumber.trim()) {
      setFormError('Notice number is required');
      return;
    }
    if (!formData.subject.trim()) {
      setFormError('Subject / Matter is required');
      return;
    }
    if (!formData.responseDueDate) {
      setFormError('Response due date is required');
      return;
    }

    try {
      setIsSubmitting(true);
      setFormError(null);
      const payload: CreateTaxNoticeRequest = {
        ...formData,
        demandAmount: formData.demandAmount ? Number(formData.demandAmount) : 0,
        hearingDate: formData.hearingDate ? formData.hearingDate : undefined,
        hearingTime: formData.hearingTime ? formData.hearingTime : undefined,
        assignedEmployeeId: formData.assignedEmployeeId ? formData.assignedEmployeeId : undefined,
        reviewerEmployeeId: formData.reviewerEmployeeId ? formData.reviewerEmployeeId : undefined,
        partnerEmployeeId: formData.partnerEmployeeId ? formData.partnerEmployeeId : undefined,
      };

      const created = await noticesApi.createNotice(payload);
      setIsCreateModalOpen(false);
      await Promise.all([loadNotices(), loadDashboardStats()]);
      navigate(`/notices/${created.id}`);
    } catch (err: any) {
      setFormError(err.response?.data?.message || err.message || 'Failed to create notice case');
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatCurrency = (amount?: number) => {
    if (!amount && amount !== 0) return '—';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const getStatusBadge = (status: NoticeStatus) => {
    switch (status) {
      case 'RECEIVED':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800 border border-blue-200">Received</span>;
      case 'UNDER_REVIEW':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-purple-100 text-purple-800 border border-purple-200">Under Review</span>;
      case 'INFO_REQUESTED':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-amber-100 text-amber-800 border border-amber-200">Info Requested</span>;
      case 'RESPONSE_DRAFTING':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-indigo-100 text-indigo-800 border border-indigo-200">Drafting Reply</span>;
      case 'INTERNAL_REVIEW':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-orange-100 text-orange-800 border border-orange-200 animate-pulse">Internal Review</span>;
      case 'PARTNER_APPROVED':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-teal-100 text-teal-800 border border-teal-200">Partner Approved</span>;
      case 'SUBMITTED':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200">Filed / Submitted</span>;
      case 'HEARING_SCHEDULED':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-rose-100 text-rose-800 border border-rose-200">Hearing Scheduled</span>;
      case 'RESOLVED':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800 border border-green-200">Resolved</span>;
      case 'DEMAND_DROPPED':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800 border border-green-200">Demand Dropped</span>;
      case 'APPEAL_FILED':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-amber-100 text-amber-800 border border-amber-200">Appeal Filed</span>;
      case 'CLOSED':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-800 border border-gray-200">Closed</span>;
      default:
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-800">{status}</span>;
    }
  };

  const getPriorityBadge = (priority: NoticePriority, isOverdue?: boolean) => {
    if (isOverdue) {
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-bold rounded bg-red-600 text-white animate-bounce">
          <Flame className="w-3 h-3" /> OVERDUE
        </span>
      );
    }
    switch (priority) {
      case 'CRITICAL':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-bold rounded bg-rose-100 text-rose-800 border border-rose-300">
            <AlertTriangle className="w-3 h-3 text-rose-600" /> CRITICAL
          </span>
        );
      case 'HIGH':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-semibold rounded bg-amber-100 text-amber-800 border border-amber-300">
            HIGH
          </span>
        );
      case 'MEDIUM':
        return <span className="px-2 py-0.5 text-xs font-medium rounded bg-blue-50 text-blue-700 border border-blue-200">MEDIUM</span>;
      case 'LOW':
        return <span className="px-2 py-0.5 text-xs font-medium rounded bg-gray-100 text-gray-700">LOW</span>;
      default:
        return null;
    }
  };

  const getDepartmentIcon = (dept: NoticeDepartment) => {
    switch (dept) {
      case 'INCOME_TAX':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded bg-sky-100 text-sky-800">ITD</span>;
      case 'GST':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded bg-emerald-100 text-emerald-800">GST</span>;
      case 'TDS':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded bg-purple-100 text-purple-800">TDS</span>;
      case 'CUSTOMS':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded bg-amber-100 text-amber-800">Customs</span>;
      default:
        return <span className="px-2 py-0.5 text-xs font-semibold rounded bg-gray-100 text-gray-800">Other</span>;
    }
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Scale className="w-7 h-7 text-indigo-600" />
            <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Tax Notice Management & Scrutiny Cases</h1>
          </div>
          <p className="text-sm text-gray-500 mt-1">
            End-to-end management of Income Tax, GST & TDS notices, maker-checker draft reviews, virtual hearings, and portal submissions.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            onClick={() => setIsCreateModalOpen(true)}
            className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm"
          >
            <Plus className="w-4 h-4" />
            Log New Notice Case
          </Button>
        </div>
      </div>

      {/* KPI Stats Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
        <Card className="p-4 bg-white border border-gray-200 shadow-sm hover:border-indigo-300 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-gray-500 uppercase tracking-wider">Active Notices</span>
            <FileText className="w-4 h-4 text-indigo-600" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-gray-900">{stats?.totalActiveNotices ?? '—'}</span>
            <span className="text-xs text-gray-400">cases</span>
          </div>
        </Card>

        <Card className="p-4 bg-white border border-gray-200 shadow-sm hover:border-rose-300 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-rose-600 uppercase tracking-wider">Overdue</span>
            <Flame className="w-4 h-4 text-rose-600" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-rose-600">{stats?.overdueNotices ?? '—'}</span>
            <span className="text-xs text-rose-400">breached</span>
          </div>
        </Card>

        <Card className="p-4 bg-white border border-gray-200 shadow-sm hover:border-amber-300 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-amber-600 uppercase tracking-wider">Due This Week</span>
            <Clock className="w-4 h-4 text-amber-600" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-amber-700">{stats?.dueThisWeekNotices ?? '—'}</span>
            <span className="text-xs text-amber-500">urgent</span>
          </div>
        </Card>

        <Card className="p-4 bg-white border border-gray-200 shadow-sm hover:border-orange-300 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-orange-600 uppercase tracking-wider">In Review</span>
            <CheckCheck className="w-4 h-4 text-orange-600" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-orange-600">{stats?.pendingReviewNotices ?? '—'}</span>
            <span className="text-xs text-orange-400">maker-checker</span>
          </div>
        </Card>

        <Card className="p-4 bg-white border border-gray-200 shadow-sm hover:border-purple-300 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-purple-600 uppercase tracking-wider">Hearings</span>
            <Gavel className="w-4 h-4 text-purple-600" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-purple-700">{stats?.upcomingHearingsCount ?? '—'}</span>
            <span className="text-xs text-purple-400">scheduled</span>
          </div>
        </Card>

        <Card className="p-4 bg-white border border-gray-200 shadow-sm hover:border-teal-300 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-teal-700 uppercase tracking-wider">Disputed Demand</span>
            <IndianRupee className="w-4 h-4 text-teal-600" />
          </div>
          <div className="mt-2">
            <span className="text-lg font-bold text-teal-700 truncate block">
              {formatCurrency(stats?.totalDemandUnderDispute)}
            </span>
          </div>
        </Card>
      </div>

      {/* Filter & Search Bar */}
      <Card className="p-4 bg-white border border-gray-200 shadow-sm space-y-3">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          {/* Search */}
          <div className="relative md:col-span-1">
            <Search className="w-4 h-4 absolute left-3 top-3 text-gray-400" />
            <input
              type="text"
              placeholder="Search by notice #, DIN, client, section..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>

          {/* Department Filter */}
          <div>
            <select
              value={selectedDept}
              onChange={(e) => setSelectedDept(e.target.value)}
              className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value="ALL">All Departments</option>
              <option value="INCOME_TAX">Income Tax (ITD)</option>
              <option value="GST">GST Department</option>
              <option value="TDS">TDS / Traces</option>
              <option value="CUSTOMS">Customs</option>
              <option value="OTHER">Other Statutory</option>
            </select>
          </div>

          {/* Status Filter */}
          <div>
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value="ALL">All Statuses</option>
              <option value="RECEIVED">Received</option>
              <option value="UNDER_REVIEW">Under Review</option>
              <option value="RESPONSE_DRAFTING">Response Drafting</option>
              <option value="INTERNAL_REVIEW">Internal Review (Maker-Checker)</option>
              <option value="PARTNER_APPROVED">Partner Approved</option>
              <option value="SUBMITTED">Filed / Submitted</option>
              <option value="HEARING_SCHEDULED">Hearing Scheduled</option>
              <option value="RESOLVED">Resolved</option>
              <option value="DEMAND_DROPPED">Demand Dropped</option>
              <option value="CLOSED">Closed</option>
            </select>
          </div>

          {/* Priority Filter */}
          <div>
            <select
              value={selectedPriority}
              onChange={(e) => setSelectedPriority(e.target.value)}
              className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value="ALL">All Priorities</option>
              <option value="CRITICAL">Critical (1-3 days)</option>
              <option value="HIGH">High (4-7 days)</option>
              <option value="MEDIUM">Medium (8-15 days)</option>
              <option value="LOW">Low (&gt;15 days)</option>
            </select>
          </div>
        </div>

        {/* Quick Toggles */}
        <div className="flex flex-wrap items-center gap-4 pt-2 border-t border-gray-100 text-xs font-medium text-gray-600">
          <label className="flex items-center gap-1.5 cursor-pointer">
            <input
              type="checkbox"
              checked={overdueOnly}
              onChange={(e) => setOverdueOnly(e.target.checked)}
              className="rounded border-gray-300 text-rose-600 focus:ring-rose-500"
            />
            <span className="text-rose-700 font-semibold">Overdue Only</span>
          </label>

          <label className="flex items-center gap-1.5 cursor-pointer">
            <input
              type="checkbox"
              checked={upcomingHearingOnly}
              onChange={(e) => setUpcomingHearingOnly(e.target.checked)}
              className="rounded border-gray-300 text-purple-600 focus:ring-purple-500"
            />
            <span className="text-purple-700 font-semibold">Upcoming Hearings Only</span>
          </label>

          <span className="text-gray-400">|</span>
          <span className="text-gray-500">Showing {notices.length} notices</span>
        </div>
      </Card>

      {/* Notices List */}
      {isLoading ? (
        <div className="py-16 text-center text-gray-500">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 mb-3" />
          <p className="text-sm">Loading tax notices and cases...</p>
        </div>
      ) : notices.length === 0 ? (
        <Card className="p-12 text-center bg-white border border-gray-200">
          <Scale className="w-12 h-12 mx-auto text-gray-300 mb-3" />
          <h3 className="text-base font-semibold text-gray-900">No Tax Notices Found</h3>
          <p className="text-sm text-gray-500 mt-1 max-w-md mx-auto">
            No notice records match the selected filters. Log a new tax notice case or adjust your search criteria.
          </p>
          <Button
            onClick={() => setIsCreateModalOpen(true)}
            className="mt-4 inline-flex items-center gap-2 bg-indigo-600 text-white"
          >
            <Plus className="w-4 h-4" /> Log Notice Case
          </Button>
        </Card>
      ) : (
        <div className="space-y-3">
          {notices.map((notice) => (
            <Card
              key={notice.id}
              className={clsx(
                'p-4 bg-white border transition-all hover:shadow-md cursor-pointer',
                notice.isOverdue
                  ? 'border-rose-300 bg-rose-50/20'
                  : notice.priority === 'CRITICAL'
                  ? 'border-orange-300'
                  : 'border-gray-200'
              )}
              onClick={() => navigate(`/notices/${notice.id}`)}
            >
              <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
                {/* Left: Notice & Client Info */}
                <div className="space-y-1.5 flex-1 min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    {getDepartmentIcon(notice.department)}
                    <span className="font-bold text-gray-900 text-base">{notice.noticeNumber}</span>
                    {notice.section && (
                      <span className="px-2 py-0.5 text-xs font-medium rounded bg-gray-100 text-gray-700 border border-gray-200">
                        u/s {notice.section}
                      </span>
                    )}
                    {getPriorityBadge(notice.priority, notice.isOverdue)}
                    {getStatusBadge(notice.status)}
                  </div>

                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-600">
                    <div className="flex items-center gap-1 font-semibold text-gray-800">
                      <Building className="w-3.5 h-3.5 text-gray-400" />
                      {notice.clientName || 'Unassigned Client'}
                      {notice.clientPan && <span className="text-gray-400 font-normal">({notice.clientPan})</span>}
                    </div>

                    <div className="text-gray-500">
                      Subject: <span className="font-medium text-gray-700">{notice.subject}</span>
                    </div>

                    {notice.assessmentYear && (
                      <div className="text-gray-500">
                        AY: <span className="font-medium text-gray-700">{notice.assessmentYear}</span>
                      </div>
                    )}
                  </div>

                  {notice.dinNumber && (
                    <div className="text-xs text-gray-400 font-mono">
                      DIN: {notice.dinNumber}
                    </div>
                  )}
                </div>

                {/* Right: Demand & Deadlines */}
                <div className="flex flex-wrap items-center gap-4 lg:gap-6 justify-between lg:justify-end border-t lg:border-t-0 pt-3 lg:pt-0 border-gray-100">
                  {/* Demand Amount */}
                  <div className="text-right">
                    <div className="text-xs text-gray-400">Demand Disputed</div>
                    <div className="text-sm font-bold text-gray-900">
                      {formatCurrency(notice.demandAmount)}
                    </div>
                  </div>

                  {/* Due Date & Countdown */}
                  <div className="text-right min-w-[130px]">
                    <div className="text-xs text-gray-400">Response Due</div>
                    <div className={clsx('text-sm font-semibold flex items-center gap-1 justify-end', notice.isOverdue ? 'text-rose-600' : 'text-gray-900')}>
                      <Clock className="w-3.5 h-3.5" />
                      {notice.responseDueDate}
                    </div>
                    <div className="text-[11px] text-gray-500">
                      {notice.daysRemaining !== undefined && (
                        notice.daysRemaining < 0 ? (
                          <span className="text-rose-600 font-bold">{Math.abs(notice.daysRemaining)} days overdue</span>
                        ) : notice.daysRemaining === 0 ? (
                          <span className="text-rose-600 font-bold">Due today</span>
                        ) : (
                          <span>{notice.daysRemaining} days remaining</span>
                        )
                      )}
                    </div>
                  </div>

                  {/* Assigned Staff */}
                  <div className="hidden sm:block text-right min-w-[120px]">
                    <div className="text-xs text-gray-400">Assigned To</div>
                    <div className="text-xs font-medium text-gray-700 truncate max-w-[140px]">
                      {notice.assignedEmployeeName || <span className="text-gray-400 italic">Unassigned</span>}
                    </div>
                  </div>

                  {/* Action Link */}
                  <div>
                    <Button
                      variant="outline"
                      size="sm"
                      className="flex items-center gap-1 text-xs"
                      onClick={(e) => {
                        e.stopPropagation();
                        navigate(`/notices/${notice.id}`);
                      }}
                    >
                      <Eye className="w-3.5 h-3.5" /> View Case
                    </Button>
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Create Notice Modal */}
      {isCreateModalOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-3xl w-full max-h-[90vh] flex flex-col">
            <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Scale className="w-5 h-5 text-indigo-600" />
                <h2 className="text-lg font-bold text-gray-900">Log Tax Notice Case</h2>
              </div>
              <button
                onClick={() => setIsCreateModalOpen(false)}
                className="text-gray-400 hover:text-gray-600 text-lg font-bold"
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleCreateNotice} className="p-6 overflow-y-auto space-y-4 flex-1">
              {formError && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 flex-shrink-0 text-red-600" />
                  {formError}
                </div>
              )}

              {/* Section 1: Client & Identification */}
              <div className="space-y-3">
                <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider">1. Client & Notice Number</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Client <span className="text-red-500">*</span>
                    </label>
                    <select
                      value={formData.clientId}
                      onChange={(e) => setFormData({ ...formData, clientId: e.target.value })}
                      required
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    >
                      <option value="">Select a Client...</option>
                      {clients.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.displayName} {c.pan ? `(${c.pan})` : ''}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Notice Reference Number <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. ITBA/AST/S/143(1)/2024-25/..."
                      value={formData.noticeNumber}
                      onChange={(e) => setFormData({ ...formData, noticeNumber: e.target.value })}
                      required
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      DIN (Document Identification Number)
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. DIN: 202410198736152"
                      value={formData.dinNumber || ''}
                      onChange={(e) => setFormData({ ...formData, dinNumber: e.target.value })}
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 font-mono text-xs"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Department <span className="text-red-500">*</span>
                    </label>
                    <select
                      value={formData.department}
                      onChange={(e) => setFormData({ ...formData, department: e.target.value as NoticeDepartment })}
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    >
                      <option value="INCOME_TAX">Income Tax Department (ITD)</option>
                      <option value="GST">GST Department</option>
                      <option value="TDS">TDS / TRACES</option>
                      <option value="CUSTOMS">Customs & Excise</option>
                      <option value="OTHER">Other Authority</option>
                    </select>
                  </div>
                </div>
              </div>

              {/* Section 2: Statutory Provisions & Matter */}
              <div className="space-y-3 pt-2 border-t border-gray-100">
                <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider">2. Tax Section & Matter</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Notice Type / Category <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. 143(1) Intimation, DRC-01 SCN"
                      value={formData.noticeType}
                      onChange={(e) => setFormData({ ...formData, noticeType: e.target.value })}
                      required
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">Section</label>
                    <input
                      type="text"
                      placeholder="e.g. 143(1), 142(1), 148, 73"
                      value={formData.section || ''}
                      onChange={(e) => setFormData({ ...formData, section: e.target.value })}
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">Assessment Year</label>
                    <input
                      type="text"
                      placeholder="e.g. 2024-25"
                      value={formData.assessmentYear || ''}
                      onChange={(e) => setFormData({ ...formData, assessmentYear: e.target.value })}
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div className="md:col-span-3">
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Subject / Matter Description <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. Discrepancy in 26AS vs ITR Revenue & Section 80C Claim verification"
                      value={formData.subject}
                      onChange={(e) => setFormData({ ...formData, subject: e.target.value })}
                      required
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>
                </div>
              </div>

              {/* Section 3: Demand & Deadlines */}
              <div className="space-y-3 pt-2 border-t border-gray-100">
                <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider">3. Demand & Deadlines</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">Demand Amount (₹)</label>
                    <input
                      type="number"
                      placeholder="0"
                      value={formData.demandAmount || 0}
                      onChange={(e) => setFormData({ ...formData, demandAmount: Number(e.target.value) })}
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Received Date <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="date"
                      value={formData.receivedDate}
                      onChange={(e) => setFormData({ ...formData, receivedDate: e.target.value })}
                      required
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Response Due Date <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="date"
                      value={formData.responseDueDate}
                      onChange={(e) => setFormData({ ...formData, responseDueDate: e.target.value })}
                      required
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">Hearing Date (Optional)</label>
                    <input
                      type="date"
                      value={formData.hearingDate || ''}
                      onChange={(e) => setFormData({ ...formData, hearingDate: e.target.value })}
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">Hearing Time</label>
                    <input
                      type="text"
                      placeholder="e.g. 11:30 AM"
                      value={formData.hearingTime || ''}
                      onChange={(e) => setFormData({ ...formData, hearingTime: e.target.value })}
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">Issuing Authority</label>
                    <input
                      type="text"
                      placeholder="e.g. ITO Ward 3(1), Mumbai"
                      value={formData.issuingAuthority || ''}
                      onChange={(e) => setFormData({ ...formData, issuingAuthority: e.target.value })}
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>
                </div>
              </div>

              {/* Section 4: Assignments & Task Automation */}
              <div className="space-y-3 pt-2 border-t border-gray-100">
                <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider">4. Team Assignments & Tasks</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">Assigned Staff / Maker</label>
                    <select
                      value={formData.assignedEmployeeId || ''}
                      onChange={(e) => setFormData({ ...formData, assignedEmployeeId: e.target.value })}
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    >
                      <option value="">Select Employee...</option>
                      {employees.map((emp) => (
                        <option key={emp.id} value={emp.id}>
                          {emp.firstName} {emp.lastName || ''} ({emp.designation || 'Staff'})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">Reviewer (Checker)</label>
                    <select
                      value={formData.reviewerEmployeeId || ''}
                      onChange={(e) => setFormData({ ...formData, reviewerEmployeeId: e.target.value })}
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    >
                      <option value="">Select Reviewer...</option>
                      {employees.map((emp) => (
                        <option key={emp.id} value={emp.id}>
                          {emp.firstName} {emp.lastName || ''} ({emp.designation || 'Reviewer'})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">Signing Partner</label>
                    <select
                      value={formData.partnerEmployeeId || ''}
                      onChange={(e) => setFormData({ ...formData, partnerEmployeeId: e.target.value })}
                      className="w-full py-2 px-3 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    >
                      <option value="">Select Partner...</option>
                      {employees.map((emp) => (
                        <option key={emp.id} value={emp.id}>
                          {emp.firstName} {emp.lastName || ''} ({emp.designation || 'Partner'})
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="pt-2">
                  <label className="flex items-center gap-2 cursor-pointer text-xs font-medium text-gray-700">
                    <input
                      type="checkbox"
                      checked={formData.createIntakeTask ?? true}
                      onChange={(e) => setFormData({ ...formData, createIntakeTask: e.target.checked })}
                      className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                    />
                    <span>Automatically generate an Intake & Response Preparation task in the Worklist for assigned staff</span>
                  </label>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="pt-4 border-t border-gray-200 flex items-center justify-end gap-3">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setIsCreateModalOpen(false)}
                  disabled={isSubmitting}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  disabled={isSubmitting}
                  className="bg-indigo-600 hover:bg-indigo-700 text-white"
                >
                  {isSubmitting ? 'Logging Notice...' : 'Create Notice Case'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
