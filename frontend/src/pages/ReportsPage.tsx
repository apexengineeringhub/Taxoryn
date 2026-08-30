import React, { useState, useEffect, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  BarChart3,
  Building2,
  FileSpreadsheet,
  Percent,
  CheckSquare,
  Calendar,
  Users,
  Receipt,
  Download,
  RefreshCw,
  Clock,
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  FileText,
  Filter,
  DollarSign,
  TrendingUp,
  ArrowUpRight,
  ShieldAlert,
  UserCheck,
  ChevronRight,
} from 'lucide-react';
import { reportsApi } from '../api/endpoints';
import { useAuth } from '../context/AuthContext';
import { exportToCsv } from '../utils/exportUtils';
import {
  PracticeOverviewReport,
  TaxWorkReport,
  ClientReport,
  WorkManagementReport,
  FinancialReport,
} from '../types';

type ReportTab = 'overview' | 'tax-work' | 'clients' | 'work' | 'financial';

export const ReportsPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = (searchParams.get('tab') as ReportTab) || 'overview';

  // Filters State
  const [periodPreset, setPeriodPreset] = useState<string>('all');
  const [financialYear, setFinancialYear] = useState<string>('2026-27');
  const [assessmentYear, setAssessmentYear] = useState<string>('2026-27');
  const [quarter, setQuarter] = useState<string>('');
  const [fromDate, setFromDate] = useState<string>('');
  const [toDate, setToDate] = useState<string>('');

  // Data States
  const [overviewData, setOverviewData] = useState<PracticeOverviewReport | null>(null);
  const [taxWorkData, setTaxWorkData] = useState<TaxWorkReport | null>(null);
  const [clientData, setClientData] = useState<ClientReport | null>(null);
  const [workData, setWorkData] = useState<WorkManagementReport | null>(null);
  const [financialData, setFinancialData] = useState<FinancialReport | null>(null);

  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const { user } = useAuth();
  const userRoles = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isFirmAdmin = userRoles.some((r: string) => ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'SUPER_ADMIN'].includes(r));
  const userPermissions = user?.permissions || [];
  const hasBillingAccess = isFirmAdmin || userPermissions.includes('BILLING_VIEW') || userPermissions.includes('BILLING_READ');

  const setTab = (tab: ReportTab) => {
    setSearchParams({ tab });
  };

  // Load data based on active tab & filters
  const loadActiveReportData = async (showRefreshSpinner = false) => {
    try {
      if (showRefreshSpinner) {
        setIsRefreshing(true);
      } else {
        setIsLoading(true);
      }
      setError(null);

      const dateParams = {
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
      };

      if (activeTab === 'overview') {
        const data = await reportsApi.getOverview(dateParams);
        setOverviewData(data);
      } else if (activeTab === 'tax-work') {
        const data = await reportsApi.getTaxWork({
          ...dateParams,
          financialYear: financialYear || undefined,
          assessmentYear: assessmentYear || undefined,
          quarter: quarter || undefined,
        });
        setTaxWorkData(data);
      } else if (activeTab === 'clients') {
        const data = await reportsApi.getClients(dateParams);
        setClientData(data);
      } else if (activeTab === 'work') {
        const data = await reportsApi.getWork(dateParams);
        setWorkData(data);
      } else if (activeTab === 'financial') {
        const data = await reportsApi.getFinancial(dateParams);
        setFinancialData(data);
      }
    } catch (err: any) {
      console.error('Failed to load report data', err);
      setError(err?.response?.data?.message || 'Failed to load central report metrics. Please try again.');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    loadActiveReportData();
  }, [activeTab, financialYear, assessmentYear, quarter, fromDate, toDate]);

  // Handle Period Presets
  const handlePeriodChange = (preset: string) => {
    setPeriodPreset(preset);
    const today = new Date();
    const formatDateStr = (d: Date) => d.toISOString().slice(0, 10);

    if (preset === 'today') {
      const dStr = formatDateStr(today);
      setFromDate(dStr);
      setToDate(dStr);
    } else if (preset === 'this-week') {
      const start = new Date(today);
      start.setDate(today.getDate() - today.getDay());
      const end = new Date(start);
      end.setDate(start.getDate() + 6);
      setFromDate(formatDateStr(start));
      setToDate(formatDateStr(end));
    } else if (preset === 'this-month') {
      const start = new Date(today.getFullYear(), today.getMonth(), 1);
      const end = new Date(today.getFullYear(), today.getMonth() + 1, 0);
      setFromDate(formatDateStr(start));
      setToDate(formatDateStr(end));
    } else if (preset === 'this-quarter') {
      const qStartMonth = Math.floor(today.getMonth() / 3) * 3;
      const start = new Date(today.getFullYear(), qStartMonth, 1);
      const end = new Date(today.getFullYear(), qStartMonth + 3, 0);
      setFromDate(formatDateStr(start));
      setToDate(formatDateStr(end));
    } else if (preset === 'this-fy') {
      setFinancialYear('2026-27');
      setAssessmentYear('2027-28');
      setFromDate('2026-04-01');
      setToDate('2027-03-31');
    } else if (preset === 'last-fy') {
      setFinancialYear('2025-26');
      setAssessmentYear('2026-27');
      setFromDate('2025-04-01');
      setToDate('2026-03-31');
    } else {
      // All time
      setFromDate('');
      setToDate('');
    }
  };

  const formatCurrency = (val: number = 0) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(val);
  };

  // CSV Export Handler
  const handleExportCsv = () => {
    if (activeTab === 'overview' && overviewData) {
      exportToCsv(
        'Taxoryn_Practice_Overview_Report',
        ['Metric Category', 'Metric Name', 'Value'],
        [
          ['Clients', 'Total Clients', overviewData.totalClients],
          ['Clients', 'Active Clients', overviewData.activeClients],
          ['Clients', 'Inactive Clients', overviewData.inactiveClients],
          ['Tax Work', 'Active Tax Jobs', overviewData.activeTaxJobs],
          ['Tasks', 'Open Tasks', overviewData.openTasks],
          ['Tasks', 'Review Tasks', overviewData.reviewTasks],
          ['Tasks', 'Overdue Tasks', overviewData.overdueTasks],
          ['Tasks', 'Completed Tasks', overviewData.completedTasks],
          ['Compliance', 'Due Today', overviewData.complianceDueToday],
          ['Compliance', 'Due This Week', overviewData.complianceDueThisWeek],
          ['Compliance', 'Overdue', overviewData.complianceOverdue],
          ['Compliance', 'Completed', overviewData.complianceCompleted],
          ['Documents', 'Requests Pending Upload', overviewData.documentRequestsPending],
          ['Documents', 'Total Open Requests', overviewData.documentRequestsOpen],
          ...(overviewData.hasBillingAccess
            ? [
                ['Financials', 'Total Invoiced (INR)', overviewData.totalInvoiced || 0],
                ['Financials', 'Total Collected (INR)', overviewData.totalCollected || 0],
                ['Financials', 'Total Outstanding (INR)', overviewData.totalOutstanding || 0],
              ]
            : []),
        ]
      );
    } else if (activeTab === 'tax-work' && taxWorkData) {
      exportToCsv(
        'Taxoryn_Tax_Work_Report',
        ['Tax Category', 'Pending Preparation', 'Under Review', 'Filed / Completed', 'Overdue', 'Total Filings'],
        taxWorkData.taxWorkSummary.map((row) => [
          row.taxType,
          row.pending,
          row.review,
          row.filed,
          row.overdue,
          row.total,
        ])
      );
    } else if (activeTab === 'clients' && clientData) {
      exportToCsv(
        'Taxoryn_Clients_Attention_Report',
        ['Client Name', 'PAN', 'Client Type', 'Assigned Staff', 'Open Tasks', 'Overdue Tasks', 'Pending Docs', 'Overdue Compliance'],
        clientData.clientsRequiringAttention.map((c) => [
          c.displayName,
          c.pan,
          c.clientType,
          c.assignedStaffName,
          c.openTasks,
          c.overdueTasks,
          c.pendingDocRequests,
          c.hasOverdueCompliance ? 'YES' : 'NO',
        ])
      );
    } else if (activeTab === 'work' && workData) {
      exportToCsv(
        'Taxoryn_Employee_Productivity_Report',
        ['Employee Code', 'Staff Name', 'Department', 'Designation', 'Assigned Tasks', 'Open', 'In Progress', 'Under Review', 'Overdue', 'Completed', 'Completion Rate (%)'],
        workData.employeeProductivity.map((e) => [
          e.employeeCode,
          e.employeeName,
          e.department || 'N/A',
          e.designation || 'Staff',
          e.assignedTasks,
          e.openTasks,
          e.inProgressTasks,
          e.underReviewTasks,
          e.overdueTasks,
          e.completedTasks,
          `${e.completionRate}%`,
        ])
      );
    } else if (activeTab === 'financial' && financialData) {
      exportToCsv(
        'Taxoryn_Outstanding_Aging_Invoices_Report',
        ['Invoice Number', 'Client Name', 'Invoice Date', 'Due Date', 'Total Amount (INR)', 'Paid Amount (INR)', 'Balance Due (INR)', 'Status', 'Days Overdue/Due'],
        financialData.outstandingInvoices.map((inv) => [
          inv.invoiceNumber,
          inv.clientName,
          inv.invoiceDate || 'N/A',
          inv.dueDate || 'N/A',
          inv.totalAmount,
          inv.paidAmount,
          inv.balanceDue,
          inv.status,
          inv.isOverdue ? `${inv.daysDueOrOverdue} Days Overdue` : `${inv.daysDueOrOverdue} Days Due Soon`,
        ])
      );
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Top Header & Export Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/90 shadow-xs">
        <div>
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-600 text-white flex items-center justify-center shadow-indigo-100 shadow-md">
              <BarChart3 className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-2xl font-black tracking-tight text-slate-900">
                Central Reports & Analytics
              </h1>
              <p className="text-xs text-slate-500 mt-0.5">
                Consolidated real-time intelligence across Tax Filings, Client Portfolio, Workload Distribution, and Billing Realization.
              </p>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <button
            onClick={() => loadActiveReportData(true)}
            disabled={isLoading || isRefreshing}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold rounded-lg bg-slate-100 text-slate-700 hover:bg-slate-200 transition-colors disabled:opacity-50"
            title="Refresh Report Data"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>

          <button
            onClick={handleExportCsv}
            disabled={isLoading}
            className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-bold rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 shadow-sm transition-all hover:shadow-emerald-100 disabled:opacity-50"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 flex flex-wrap items-center justify-between gap-4 text-xs">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-bold text-slate-600 flex items-center gap-1">
            <Filter className="w-3.5 h-3.5 text-slate-400" /> Period:
          </span>

          {[
            { id: 'all', label: 'All Time' },
            { id: 'this-fy', label: 'This FY (26-27)' },
            { id: 'this-quarter', label: 'This Quarter' },
            { id: 'this-month', label: 'This Month' },
            { id: 'this-week', label: 'This Week' },
            { id: 'today', label: 'Today' },
          ].map((preset) => (
            <button
              key={preset.id}
              onClick={() => handlePeriodChange(preset.id)}
              className={`px-3 py-1.5 rounded-lg font-semibold transition-all ${
                periodPreset === preset.id
                  ? 'bg-indigo-600 text-white shadow-2xs'
                  : 'bg-white text-slate-600 hover:bg-slate-200 border border-slate-200'
              }`}
            >
              {preset.label}
            </button>
          ))}
        </div>

        {activeTab === 'tax-work' && (
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-1.5">
              <span className="font-semibold text-slate-500">FY:</span>
              <select
                value={financialYear}
                onChange={(e) => setFinancialYear(e.target.value)}
                className="bg-white border border-slate-200 rounded-md px-2 py-1 font-semibold text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-indigo-500"
              >
                <option value="2026-27">FY 2026-27</option>
                <option value="2025-26">FY 2025-26</option>
                <option value="2024-25">FY 2024-25</option>
              </select>
            </div>

            <div className="flex items-center gap-1.5">
              <span className="font-semibold text-slate-500">AY:</span>
              <select
                value={assessmentYear}
                onChange={(e) => setAssessmentYear(e.target.value)}
                className="bg-white border border-slate-200 rounded-md px-2 py-1 font-semibold text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-indigo-500"
              >
                <option value="2026-27">AY 2026-27</option>
                <option value="2027-28">AY 2027-28</option>
                <option value="2025-26">AY 2025-26</option>
              </select>
            </div>

            <div className="flex items-center gap-1.5">
              <span className="font-semibold text-slate-500">Quarter:</span>
              <select
                value={quarter}
                onChange={(e) => setQuarter(e.target.value)}
                className="bg-white border border-slate-200 rounded-md px-2 py-1 font-semibold text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-indigo-500"
              >
                <option value="">All Quarters</option>
                <option value="Q1">Q1 (Apr - Jun)</option>
                <option value="Q2">Q2 (Jul - Sep)</option>
                <option value="Q3">Q3 (Oct - Dec)</option>
                <option value="Q4">Q4 (Jan - Mar)</option>
              </select>
            </div>
          </div>
        )}
      </div>

      {/* Standard Tab Navigation (5 Tabs) */}
      <div className="flex border-b border-slate-200 overflow-x-auto no-scrollbar gap-2">
        <button
          onClick={() => setTab('overview')}
          className={`flex items-center gap-2 px-4 py-3 text-xs font-bold border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'overview'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/50 rounded-t-lg'
              : 'border-transparent text-slate-500 hover:text-slate-900 hover:border-slate-300'
          }`}
        >
          <BarChart3 className="w-4 h-4" />
          <span>Practice Overview</span>
        </button>

        <button
          onClick={() => setTab('tax-work')}
          className={`flex items-center gap-2 px-4 py-3 text-xs font-bold border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'tax-work'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/50 rounded-t-lg'
              : 'border-transparent text-slate-500 hover:text-slate-900 hover:border-slate-300'
          }`}
        >
          <FileSpreadsheet className="w-4 h-4" />
          <span>Tax Work (GST / ITR / TDS)</span>
        </button>

        <button
          onClick={() => setTab('clients')}
          className={`flex items-center gap-2 px-4 py-3 text-xs font-bold border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'clients'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/50 rounded-t-lg'
              : 'border-transparent text-slate-500 hover:text-slate-900 hover:border-slate-300'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>Clients & Follow-up</span>
        </button>

        <button
          onClick={() => setTab('work')}
          className={`flex items-center gap-2 px-4 py-3 text-xs font-bold border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'work'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/50 rounded-t-lg'
              : 'border-transparent text-slate-500 hover:text-slate-900 hover:border-slate-300'
          }`}
        >
          <CheckSquare className="w-4 h-4" />
          <span>Work & Team Productivity</span>
        </button>

        <button
          onClick={() => setTab('financial')}
          className={`flex items-center gap-2 px-4 py-3 text-xs font-bold border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'financial'
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50/50 rounded-t-lg'
              : 'border-transparent text-slate-500 hover:text-slate-900 hover:border-slate-300'
          }`}
        >
          <Receipt className="w-4 h-4" />
          <span>Financial & Aging Invoices</span>
          {!hasBillingAccess && (
            <span className="text-[10px] bg-slate-200 text-slate-600 px-1.5 py-0.5 rounded-full">
              Restricted
            </span>
          )}
        </button>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 flex items-center gap-3 text-xs text-red-800">
          <AlertCircle className="w-5 h-5 text-red-500 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Loading Skeleton */}
      {isLoading ? (
        <div className="h-64 flex flex-col items-center justify-center bg-white rounded-2xl border border-slate-200 space-y-3">
          <div className="w-8 h-8 border-3 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-xs font-semibold text-slate-500">Aggregating real-time report telemetry...</p>
        </div>
      ) : (
        <>
          {/* =========================================================================
              TAB 1: PRACTICE OVERVIEW
             ========================================================================= */}
          {activeTab === 'overview' && overviewData && (
            <div className="space-y-6 animate-fade-in">
              {/* Core KPI Cards Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
                {/* 1. Clients */}
                <Link
                  to="/clients"
                  className="bg-white border border-slate-200/90 rounded-xl p-4.5 shadow-xs hover:border-blue-300 transition-all group block"
                >
                  <div className="flex items-center justify-between text-xs text-slate-500 font-bold">
                    <span>Active Clients</span>
                    <Users className="w-4 h-4 text-blue-600" />
                  </div>
                  <div className="mt-3 flex items-baseline justify-between">
                    <span className="text-2xl font-black text-slate-900">{overviewData.activeClients}</span>
                    <span className="text-[11px] font-semibold text-slate-400">/ {overviewData.totalClients} Total</span>
                  </div>
                  <div className="mt-2 text-[11px] text-slate-500 flex justify-between pt-2 border-t border-slate-100">
                    <span>Inactive Accounts:</span>
                    <span className="font-semibold text-slate-700">{overviewData.inactiveClients}</span>
                  </div>
                </Link>

                {/* 2. Active Tax Jobs */}
                <Link
                  to="/tasks"
                  className="bg-white border border-slate-200/90 rounded-xl p-4.5 shadow-xs hover:border-indigo-300 transition-all group block"
                >
                  <div className="flex items-center justify-between text-xs text-slate-500 font-bold">
                    <span>Active Tax Jobs</span>
                    <FileSpreadsheet className="w-4 h-4 text-indigo-600" />
                  </div>
                  <div className="mt-3 flex items-baseline justify-between">
                    <span className="text-2xl font-black text-slate-900">{overviewData.activeTaxJobs}</span>
                    <span className="text-[11px] font-semibold bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-full">
                      GST + ITR + TDS
                    </span>
                  </div>
                  <div className="mt-2 text-[11px] text-slate-500 flex justify-between pt-2 border-t border-slate-100">
                    <span>Pending Filings:</span>
                    <span className="font-semibold text-indigo-600">Active Pipeline</span>
                  </div>
                </Link>

                {/* 3. Tasks */}
                <Link
                  to="/tasks"
                  className="bg-white border border-slate-200/90 rounded-xl p-4.5 shadow-xs hover:border-amber-300 transition-all group block"
                >
                  <div className="flex items-center justify-between text-xs text-slate-500 font-bold">
                    <span>Open Tasks</span>
                    <CheckSquare className="w-4 h-4 text-amber-600" />
                  </div>
                  <div className="mt-3 flex items-baseline justify-between">
                    <span className="text-2xl font-black text-slate-900">{overviewData.openTasks}</span>
                    <span className="text-[11px] font-semibold text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full">
                      {overviewData.overdueTasks} Overdue
                    </span>
                  </div>
                  <div className="mt-2 text-[11px] text-slate-500 flex justify-between pt-2 border-t border-slate-100">
                    <span>In Review / Done:</span>
                    <span className="font-semibold text-slate-700">{overviewData.reviewTasks} / {overviewData.completedTasks}</span>
                  </div>
                </Link>

                {/* 4. Compliance */}
                <Link
                  to="/calendar"
                  className="bg-white border border-slate-200/90 rounded-xl p-4.5 shadow-xs hover:border-rose-300 transition-all group block"
                >
                  <div className="flex items-center justify-between text-xs text-slate-500 font-bold">
                    <span>Statutory Obligations</span>
                    <Calendar className="w-4 h-4 text-rose-600" />
                  </div>
                  <div className="mt-3 flex items-baseline justify-between">
                    <span className="text-2xl font-black text-slate-900">{overviewData.complianceDueToday + overviewData.complianceDueThisWeek}</span>
                    <span className="text-[11px] font-semibold text-rose-600 bg-rose-50 px-2 py-0.5 rounded-full">
                      This Week
                    </span>
                  </div>
                  <div className="mt-2 text-[11px] text-slate-500 flex justify-between pt-2 border-t border-slate-100">
                    <span>Overdue Deadlines:</span>
                    <span className="font-semibold text-rose-600">{overviewData.complianceOverdue}</span>
                  </div>
                </Link>

                {/* 5. Documents */}
                <Link
                  to="/documents"
                  className="bg-white border border-slate-200/90 rounded-xl p-4.5 shadow-xs hover:border-teal-300 transition-all group block"
                >
                  <div className="flex items-center justify-between text-xs text-slate-500 font-bold">
                    <span>Document Pipeline</span>
                    <FileText className="w-4 h-4 text-teal-600" />
                  </div>
                  <div className="mt-3 flex items-baseline justify-between">
                    <span className="text-2xl font-black text-slate-900">{overviewData.documentRequestsPending}</span>
                    <span className="text-[11px] font-semibold text-teal-700 bg-teal-50 px-2 py-0.5 rounded-full">
                      Awaiting Docs
                    </span>
                  </div>
                  <div className="mt-2 text-[11px] text-slate-500 flex justify-between pt-2 border-t border-slate-100">
                    <span>Total Open Requests:</span>
                    <span className="font-semibold text-slate-700">{overviewData.documentRequestsOpen}</span>
                  </div>
                </Link>

                {/* 6. Financials */}
                <Link
                  to="/billing"
                  className="bg-white border border-slate-200/90 rounded-xl p-4.5 shadow-xs hover:border-emerald-300 transition-all group block"
                >
                  <div className="flex items-center justify-between text-xs text-slate-500 font-bold">
                    <span>Collections & Balance</span>
                    <Receipt className="w-4 h-4 text-emerald-600" />
                  </div>
                  <div className="mt-3 flex items-baseline justify-between">
                    <span className="text-xl font-black text-slate-900">
                      {overviewData.hasBillingAccess ? formatCurrency(overviewData.totalCollected || 0) : '🔒 Isolated'}
                    </span>
                  </div>
                  <div className="mt-2 text-[11px] text-slate-500 flex justify-between pt-2 border-t border-slate-100">
                    <span>Outstanding Due:</span>
                    <span className="font-semibold text-rose-600">
                      {overviewData.hasBillingAccess ? formatCurrency(overviewData.totalOutstanding || 0) : 'Restricted'}
                    </span>
                  </div>
                </Link>
              </div>

              {/* Quick Drill-down Workspaces Hub */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="bg-white p-6 rounded-2xl border border-slate-200/90 shadow-xs space-y-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                      <Building2 className="w-4 h-4 text-indigo-600" /> Tax Compliance Suites
                    </h3>
                    <span className="text-[11px] font-semibold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-full">
                      Direct Workflows
                    </span>
                  </div>
                  <div className="space-y-2.5">
                    <Link
                      to="/gst"
                      className="flex items-center justify-between p-3 rounded-xl bg-slate-50 hover:bg-slate-100 transition-all text-xs font-semibold text-slate-800"
                    >
                      <div className="flex items-center gap-2.5">
                        <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                        <span>GST Compliance Workspace</span>
                      </div>
                      <ChevronRight className="w-4 h-4 text-slate-400" />
                    </Link>

                    <Link
                      to="/itr"
                      className="flex items-center justify-between p-3 rounded-xl bg-slate-50 hover:bg-slate-100 transition-all text-xs font-semibold text-slate-800"
                    >
                      <div className="flex items-center gap-2.5">
                        <span className="w-2 h-2 rounded-full bg-blue-500"></span>
                        <span>ITR Compliance & e-Filing</span>
                      </div>
                      <ChevronRight className="w-4 h-4 text-slate-400" />
                    </Link>

                    <Link
                      to="/tds"
                      className="flex items-center justify-between p-3 rounded-xl bg-slate-50 hover:bg-slate-100 transition-all text-xs font-semibold text-slate-800"
                    >
                      <div className="flex items-center gap-2.5">
                        <span className="w-2 h-2 rounded-full bg-purple-500"></span>
                        <span>TDS Statements & Challans</span>
                      </div>
                      <ChevronRight className="w-4 h-4 text-slate-400" />
                    </Link>
                  </div>
                </div>

                <div className="bg-white p-6 rounded-2xl border border-slate-200/90 shadow-xs space-y-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                      <CheckSquare className="w-4 h-4 text-amber-600" /> Work & Client Operations
                    </h3>
                    <span className="text-[11px] font-semibold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full">
                      Deliverables
                    </span>
                  </div>
                  <div className="space-y-2.5">
                    <Link
                      to="/tasks"
                      className="flex items-center justify-between p-3 rounded-xl bg-slate-50 hover:bg-slate-100 transition-all text-xs font-semibold text-slate-800"
                    >
                      <div className="flex items-center gap-2.5">
                        <span className="w-2 h-2 rounded-full bg-amber-500"></span>
                        <span>Task Board & Worklist</span>
                      </div>
                      <ChevronRight className="w-4 h-4 text-slate-400" />
                    </Link>

                    <Link
                      to="/clients"
                      className="flex items-center justify-between p-3 rounded-xl bg-slate-50 hover:bg-slate-100 transition-all text-xs font-semibold text-slate-800"
                    >
                      <div className="flex items-center gap-2.5">
                        <span className="w-2 h-2 rounded-full bg-indigo-500"></span>
                        <span>Clients 360° Directory</span>
                      </div>
                      <ChevronRight className="w-4 h-4 text-slate-400" />
                    </Link>

                    <Link
                      to="/documents"
                      className="flex items-center justify-between p-3 rounded-xl bg-slate-50 hover:bg-slate-100 transition-all text-xs font-semibold text-slate-800"
                    >
                      <div className="flex items-center gap-2.5">
                        <span className="w-2 h-2 rounded-full bg-teal-500"></span>
                        <span>Document Requests Vault</span>
                      </div>
                      <ChevronRight className="w-4 h-4 text-slate-400" />
                    </Link>
                  </div>
                </div>

                <div className="bg-white p-6 rounded-2xl border border-slate-200/90 shadow-xs space-y-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                      <Receipt className="w-4 h-4 text-emerald-600" /> Financial & Governance
                    </h3>
                    <span className="text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
                      Practice Health
                    </span>
                  </div>
                  <div className="space-y-2.5">
                    <Link
                      to="/billing"
                      className="flex items-center justify-between p-3 rounded-xl bg-slate-50 hover:bg-slate-100 transition-all text-xs font-semibold text-slate-800"
                    >
                      <div className="flex items-center gap-2.5">
                        <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                        <span>Invoices & Payments Ledger</span>
                      </div>
                      <ChevronRight className="w-4 h-4 text-slate-400" />
                    </Link>

                    <Link
                      to="/calendar"
                      className="flex items-center justify-between p-3 rounded-xl bg-slate-50 hover:bg-slate-100 transition-all text-xs font-semibold text-slate-800"
                    >
                      <div className="flex items-center gap-2.5">
                        <span className="w-2 h-2 rounded-full bg-rose-500"></span>
                        <span>Tax Calendar & Deadlines</span>
                      </div>
                      <ChevronRight className="w-4 h-4 text-slate-400" />
                    </Link>

                    <Link
                      to="/team"
                      className="flex items-center justify-between p-3 rounded-xl bg-slate-50 hover:bg-slate-100 transition-all text-xs font-semibold text-slate-800"
                    >
                      <div className="flex items-center gap-2.5">
                        <span className="w-2 h-2 rounded-full bg-purple-500"></span>
                        <span>Team Management & Workload</span>
                      </div>
                      <ChevronRight className="w-4 h-4 text-slate-400" />
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* =========================================================================
              TAB 2: TAX WORK REPORT
             ========================================================================= */}
          {activeTab === 'tax-work' && taxWorkData && (
            <div className="space-y-6 animate-fade-in">
              {/* Consolidated Tax Work Summary Table */}
              <div className="bg-white rounded-2xl border border-slate-200/90 shadow-xs overflow-hidden">
                <div className="p-5 border-b border-slate-200 flex items-center justify-between">
                  <div>
                    <h3 className="text-sm font-bold text-slate-900">Consolidated Tax Work Summary</h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Cross-domain returns status matrix across GST, Income Tax (ITR), and TDS statements.
                    </p>
                  </div>
                  <span className="text-xs font-semibold bg-slate-100 text-slate-700 px-3 py-1 rounded-lg">
                    FY: {financialYear}
                  </span>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-600 font-bold border-b border-slate-200">
                      <tr>
                        <th className="py-3.5 px-5">Tax Domain</th>
                        <th className="py-3.5 px-4 text-amber-700">Pending Preparation</th>
                        <th className="py-3.5 px-4 text-indigo-700">Under Review</th>
                        <th className="py-3.5 px-4 text-emerald-700">Filed / Completed</th>
                        <th className="py-3.5 px-4 text-rose-700">Overdue</th>
                        <th className="py-3.5 px-4 font-black text-slate-900">Total Filings</th>
                        <th className="py-3.5 px-5 text-right">Direct Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium">
                      {taxWorkData.taxWorkSummary.map((row) => (
                        <tr key={row.taxType} className="hover:bg-slate-50/80 transition-colors">
                          <td className="py-4 px-5 font-bold text-slate-900 flex items-center gap-2">
                            <span
                              className={`w-2.5 h-2.5 rounded-full ${
                                row.taxType === 'GST'
                                  ? 'bg-emerald-500'
                                  : row.taxType === 'ITR'
                                  ? 'bg-blue-500'
                                  : 'bg-purple-500'
                              }`}
                            ></span>
                            {row.taxType} Filings
                          </td>
                          <td className="py-4 px-4 text-amber-700 font-bold">{row.pending}</td>
                          <td className="py-4 px-4 text-indigo-700 font-bold">{row.review}</td>
                          <td className="py-4 px-4 text-emerald-700 font-bold">{row.filed}</td>
                          <td className="py-4 px-4 text-rose-700 font-bold">{row.overdue}</td>
                          <td className="py-4 px-4 font-black text-slate-900">{row.total}</td>
                          <td className="py-4 px-5 text-right">
                            <Link
                              to={
                                row.taxType === 'GST'
                                  ? '/gst'
                                  : row.taxType === 'ITR'
                                  ? '/itr'
                                  : '/tds'
                              }
                              className="inline-flex items-center gap-1 text-xs font-bold text-indigo-600 hover:text-indigo-800 bg-indigo-50 hover:bg-indigo-100 px-3 py-1 rounded-md transition-colors"
                            >
                              Open {row.taxType} <ArrowUpRight className="w-3 h-3" />
                            </Link>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Detailed Breakdown Grid */}
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* GST Return Types */}
                <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs space-y-4">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                    <h4 className="text-xs font-bold text-slate-900 flex items-center gap-2">
                      <Building2 className="w-4 h-4 text-emerald-600" /> GST by Return Type
                    </h4>
                    <span className="text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
                      {taxWorkData.gstTotalClients} GST Clients
                    </span>
                  </div>
                  <div className="space-y-2">
                    {Object.entries(taxWorkData.gstByReturnType).length > 0 ? (
                      Object.entries(taxWorkData.gstByReturnType).map(([type, count]) => (
                        <div key={type} className="flex items-center justify-between text-xs p-2 rounded-lg bg-slate-50">
                          <span className="font-semibold text-slate-700">{type}</span>
                          <span className="font-bold text-slate-900 bg-white px-2.5 py-0.5 rounded border border-slate-200 shadow-2xs">
                            {count} Filings
                          </span>
                        </div>
                      ))
                    ) : (
                      <p className="text-xs text-slate-400 py-4 text-center">No GST returns for this period</p>
                    )}
                  </div>
                </div>

                {/* ITR Form Types */}
                <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs space-y-4">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                    <h4 className="text-xs font-bold text-slate-900 flex items-center gap-2">
                      <FileSpreadsheet className="w-4 h-4 text-blue-600" /> ITR by Form Type
                    </h4>
                    <span className="text-[11px] font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded-full">
                      {taxWorkData.itrTotalClients} ITR Clients
                    </span>
                  </div>
                  <div className="space-y-2">
                    {Object.entries(taxWorkData.itrByFormType).length > 0 ? (
                      Object.entries(taxWorkData.itrByFormType).map(([form, count]) => (
                        <div key={form} className="flex items-center justify-between text-xs p-2 rounded-lg bg-slate-50">
                          <span className="font-semibold text-slate-700">{form}</span>
                          <span className="font-bold text-slate-900 bg-white px-2.5 py-0.5 rounded border border-slate-200 shadow-2xs">
                            {count} Returns
                          </span>
                        </div>
                      ))
                    ) : (
                      <p className="text-xs text-slate-400 py-4 text-center">No ITR returns for this AY</p>
                    )}
                  </div>
                </div>

                {/* TDS Quarter & Form */}
                <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs space-y-4">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                    <h4 className="text-xs font-bold text-slate-900 flex items-center gap-2">
                      <Percent className="w-4 h-4 text-purple-600" /> TDS by Form & Quarter
                    </h4>
                    <span className="text-[11px] font-bold text-purple-700 bg-purple-50 px-2 py-0.5 rounded-full">
                      {taxWorkData.tdsTotalClients} TDS Profiles
                    </span>
                  </div>
                  <div className="space-y-2">
                    {Object.entries(taxWorkData.tdsByFormType).length > 0 ? (
                      Object.entries(taxWorkData.tdsByFormType).map(([form, count]) => (
                        <div key={form} className="flex items-center justify-between text-xs p-2 rounded-lg bg-slate-50">
                          <span className="font-semibold text-slate-700">{form}</span>
                          <span className="font-bold text-slate-900 bg-white px-2.5 py-0.5 rounded border border-slate-200 shadow-2xs">
                            {count} Statements
                          </span>
                        </div>
                      ))
                    ) : (
                      <p className="text-xs text-slate-400 py-4 text-center">No TDS statements for this period</p>
                    )}
                  </div>
                </div>
              </div>

              {/* Statutory Compliance Obligation Calendar Summary */}
              <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs space-y-4">
                <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                  <div>
                    <h4 className="text-xs font-bold text-slate-900 flex items-center gap-2">
                      <Calendar className="w-4 h-4 text-rose-600" /> Statutory Compliance Obligations Pipeline
                    </h4>
                    <p className="text-[11px] text-slate-500 mt-0.5">
                      Statutory tax due date compliance obligations tracked across GST, Income Tax, TDS, ROC, and Advance Tax.
                    </p>
                  </div>
                  <Link
                    to="/calendar"
                    className="text-xs font-bold text-rose-600 hover:text-rose-800 bg-rose-50 px-3 py-1 rounded-md transition-colors"
                  >
                    Open Calendar &rarr;
                  </Link>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
                  <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 text-center">
                    <span className="text-slate-500 text-[11px] font-semibold">Total Obligations</span>
                    <p className="text-xl font-black text-slate-900 mt-1">{taxWorkData.complianceTotal}</p>
                  </div>
                  <div className="p-3 rounded-xl bg-amber-50 border border-amber-100 text-center">
                    <span className="text-amber-800 text-[11px] font-semibold">Due Today</span>
                    <p className="text-xl font-black text-amber-900 mt-1">{taxWorkData.complianceDueToday}</p>
                  </div>
                  <div className="p-3 rounded-xl bg-blue-50 border border-blue-100 text-center">
                    <span className="text-blue-800 text-[11px] font-semibold">Due This Week</span>
                    <p className="text-xl font-black text-blue-900 mt-1">{taxWorkData.complianceDueThisWeek}</p>
                  </div>
                  <div className="p-3 rounded-xl bg-rose-50 border border-rose-100 text-center">
                    <span className="text-rose-800 text-[11px] font-semibold">Overdue Deadlines</span>
                    <p className="text-xl font-black text-rose-900 mt-1">{taxWorkData.complianceOverdue}</p>
                  </div>
                  <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-100 text-center">
                    <span className="text-emerald-800 text-[11px] font-semibold">Completed</span>
                    <p className="text-xl font-black text-emerald-900 mt-1">{taxWorkData.complianceCompleted}</p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* =========================================================================
              TAB 3: CLIENTS & FOLLOW-UP REPORT
             ========================================================================= */}
          {activeTab === 'clients' && clientData && (
            <div className="space-y-6 animate-fade-in">
              {/* Top Client KPIs */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
                <div className="bg-white p-4.5 rounded-xl border border-slate-200 shadow-xs">
                  <span className="text-xs font-bold text-slate-500">Total Clients</span>
                  <p className="text-2xl font-black text-slate-900 mt-1">{clientData.totalClients}</p>
                  <span className="text-[11px] text-emerald-600 font-semibold">{clientData.activeClients} Active</span>
                </div>

                <div className="bg-white p-4.5 rounded-xl border border-slate-200 shadow-xs">
                  <span className="text-xs font-bold text-slate-500">Clients with Pending Work</span>
                  <p className="text-2xl font-black text-amber-700 mt-1">{clientData.clientsWithPendingWork}</p>
                  <span className="text-[11px] text-slate-400 font-semibold">Open tasks/filings</span>
                </div>

                <div className="bg-white p-4.5 rounded-xl border border-slate-200 shadow-xs">
                  <span className="text-xs font-bold text-slate-500">Overdue Deliverables</span>
                  <p className="text-2xl font-black text-rose-700 mt-1">{clientData.clientsWithOverdueWork}</p>
                  <span className="text-[11px] text-rose-600 font-semibold">Action Required</span>
                </div>

                <div className="bg-white p-4.5 rounded-xl border border-slate-200 shadow-xs">
                  <span className="text-xs font-bold text-slate-500">Pending Docs from Clients</span>
                  <p className="text-2xl font-black text-teal-700 mt-1">{clientData.clientsWithPendingDocs}</p>
                  <span className="text-[11px] text-teal-600 font-semibold">Awaiting Upload</span>
                </div>

                <div className="bg-white p-4.5 rounded-xl border border-slate-200 shadow-xs">
                  <span className="text-xs font-bold text-slate-500">Follow-up Items Due Today</span>
                  <p className="text-2xl font-black text-indigo-700 mt-1">{clientData.clientActionsDueToday}</p>
                  <span className="text-[11px] text-slate-400 font-semibold">Reminders Active</span>
                </div>

                <div className="bg-white p-4.5 rounded-xl border border-slate-200 shadow-xs">
                  <span className="text-xs font-bold text-slate-500">Total Document Requests</span>
                  <p className="text-2xl font-black text-slate-900 mt-1">{clientData.totalDocRequests}</p>
                  <span className="text-[11px] text-slate-500 font-semibold">
                    {clientData.docRequestsAccepted} Completed
                  </span>
                </div>
              </div>

              {/* Document Request Pipeline Status */}
              <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs space-y-4">
                <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                  <div>
                    <h3 className="text-sm font-bold text-slate-900">Client Document Request Pipeline</h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Status of required verification documents requested from clients across tax return filings.
                    </p>
                  </div>
                  <Link
                    to="/documents"
                    className="text-xs font-bold text-indigo-600 hover:text-indigo-800 bg-indigo-50 px-3 py-1 rounded-md transition-colors"
                  >
                    View All Document Requests &rarr;
                  </Link>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div className="p-3.5 rounded-xl bg-amber-50/80 border border-amber-200 text-center">
                    <span className="text-amber-800 text-xs font-semibold">Awaiting Client Upload</span>
                    <p className="text-2xl font-black text-amber-900 mt-1">{clientData.docRequestsAwaitingUpload}</p>
                  </div>
                  <div className="p-3.5 rounded-xl bg-blue-50/80 border border-blue-200 text-center">
                    <span className="text-blue-800 text-xs font-semibold">Uploaded / In Review</span>
                    <p className="text-2xl font-black text-blue-900 mt-1">{clientData.docRequestsUploaded}</p>
                  </div>
                  <div className="p-3.5 rounded-xl bg-emerald-50/80 border border-emerald-200 text-center">
                    <span className="text-emerald-800 text-xs font-semibold">Accepted / Verified</span>
                    <p className="text-2xl font-black text-emerald-900 mt-1">{clientData.docRequestsAccepted}</p>
                  </div>
                  <div className="p-3.5 rounded-xl bg-rose-50/80 border border-rose-200 text-center">
                    <span className="text-rose-800 text-xs font-semibold">Rejected / Re-upload Required</span>
                    <p className="text-2xl font-black text-rose-900 mt-1">{clientData.docRequestsRejected}</p>
                  </div>
                </div>
              </div>

              {/* Priority Follow-up Table: Clients Requiring Attention */}
              <div className="bg-white rounded-2xl border border-slate-200/90 shadow-xs overflow-hidden">
                <div className="p-5 border-b border-slate-200 flex items-center justify-between">
                  <div>
                    <h3 className="text-sm font-bold text-slate-900">Clients Requiring Attention (Priority Queue)</h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Client accounts with overdue tasks, pending document uploads, or urgent compliance deadlines.
                    </p>
                  </div>
                  <span className="text-xs font-bold text-amber-800 bg-amber-100 px-3 py-1 rounded-full">
                    {clientData.clientsRequiringAttention.length} Accounts
                  </span>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-600 font-bold border-b border-slate-200">
                      <tr>
                        <th className="py-3.5 px-5">Client Name & PAN</th>
                        <th className="py-3.5 px-4">Entity Type</th>
                        <th className="py-3.5 px-4">Assigned Staff</th>
                        <th className="py-3.5 px-4">Open Tasks</th>
                        <th className="py-3.5 px-4 text-rose-700">Overdue Tasks</th>
                        <th className="py-3.5 px-4 text-teal-700">Pending Docs</th>
                        <th className="py-3.5 px-4">Compliance Status</th>
                        <th className="py-3.5 px-5 text-right">Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium">
                      {clientData.clientsRequiringAttention.length > 0 ? (
                        clientData.clientsRequiringAttention.map((client) => (
                          <tr key={client.clientId} className="hover:bg-slate-50/80 transition-colors">
                            <td className="py-4 px-5">
                              <p className="font-bold text-slate-900">{client.displayName}</p>
                              <span className="text-[11px] font-mono text-slate-400">PAN: {client.pan || 'N/A'}</span>
                            </td>
                            <td className="py-4 px-4 text-slate-600 font-semibold">{client.clientType}</td>
                            <td className="py-4 px-4 text-slate-700 font-semibold">{client.assignedStaffName}</td>
                            <td className="py-4 px-4 font-bold text-slate-900">{client.openTasks}</td>
                            <td className="py-4 px-4 font-bold text-rose-700">
                              {client.overdueTasks > 0 ? `${client.overdueTasks} Overdue` : '-'}
                            </td>
                            <td className="py-4 px-4 font-bold text-teal-700">
                              {client.pendingDocRequests > 0 ? `${client.pendingDocRequests} Pending` : '-'}
                            </td>
                            <td className="py-4 px-4">
                              {client.hasOverdueCompliance ? (
                                <span className="inline-flex items-center gap-1 text-[11px] font-bold text-rose-700 bg-rose-50 px-2 py-0.5 rounded-full">
                                  <AlertTriangle className="w-3 h-3" /> Overdue
                                </span>
                              ) : (
                                <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
                                  <CheckCircle2 className="w-3 h-3" /> On Track
                                </span>
                              )}
                            </td>
                            <td className="py-4 px-5 text-right">
                              <Link
                                to={`/clients`}
                                className="inline-flex items-center gap-1 text-xs font-bold text-indigo-600 hover:text-indigo-800 bg-indigo-50 hover:bg-indigo-100 px-3 py-1 rounded-md transition-colors"
                              >
                                View 360° <ArrowUpRight className="w-3 h-3" />
                              </Link>
                            </td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td colSpan={8} className="py-8 text-center text-slate-400">
                            No clients requiring urgent attention. All deliverables on track!
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* =========================================================================
              TAB 4: WORK & TEAM PRODUCTIVITY
             ========================================================================= */}
          {activeTab === 'work' && workData && (
            <div className="space-y-6 animate-fade-in">
              {/* Task Breakdown Badges */}
              <div className="grid grid-cols-2 sm:grid-cols-6 gap-4">
                <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs text-center">
                  <span className="text-slate-500 text-xs font-semibold">Total Tasks</span>
                  <p className="text-2xl font-black text-slate-900 mt-1">{workData.totalTasks}</p>
                </div>
                <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs text-center">
                  <span className="text-blue-600 text-xs font-semibold">To Do (Open)</span>
                  <p className="text-2xl font-black text-blue-700 mt-1">{workData.openTasks}</p>
                </div>
                <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs text-center">
                  <span className="text-amber-600 text-xs font-semibold">In Progress</span>
                  <p className="text-2xl font-black text-amber-700 mt-1">{workData.inProgressTasks}</p>
                </div>
                <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs text-center">
                  <span className="text-indigo-600 text-xs font-semibold">Under Review</span>
                  <p className="text-2xl font-black text-indigo-700 mt-1">{workData.underReviewTasks}</p>
                </div>
                <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs text-center">
                  <span className="text-rose-600 text-xs font-semibold">Overdue</span>
                  <p className="text-2xl font-black text-rose-700 mt-1">{workData.overdueTasks}</p>
                </div>
                <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs text-center">
                  <span className="text-emerald-600 text-xs font-semibold">Completed</span>
                  <p className="text-2xl font-black text-emerald-700 mt-1">{workData.completedTasks}</p>
                </div>
              </div>

              {/* Employee Productivity Matrix Table */}
              <div className="bg-white rounded-2xl border border-slate-200/90 shadow-xs overflow-hidden">
                <div className="p-5 border-b border-slate-200 flex items-center justify-between">
                  <div>
                    <h3 className="text-sm font-bold text-slate-900">Staff Workload & Task Completion Productivity</h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Individual workload distribution, in-progress deliverables, and performance completion rates.
                    </p>
                  </div>
                  <span className="text-xs font-semibold text-slate-600 bg-slate-100 px-3 py-1 rounded-lg">
                    {workData.employeeProductivity.length} Team Members
                  </span>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-600 font-bold border-b border-slate-200">
                      <tr>
                        <th className="py-3.5 px-5">Staff Member</th>
                        <th className="py-3.5 px-4">Department</th>
                        <th className="py-3.5 px-4">Assigned Tasks</th>
                        <th className="py-3.5 px-4 text-blue-700">Open</th>
                        <th className="py-3.5 px-4 text-amber-700">In Progress</th>
                        <th className="py-3.5 px-4 text-indigo-700">Review</th>
                        <th className="py-3.5 px-4 text-rose-700">Overdue</th>
                        <th className="py-3.5 px-4 text-emerald-700">Completed</th>
                        <th className="py-3.5 px-5 text-right">Completion Rate</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium">
                      {workData.employeeProductivity.map((emp) => (
                        <tr key={emp.employeeId} className="hover:bg-slate-50/80 transition-colors">
                          <td className="py-4 px-5">
                            <p className="font-bold text-slate-900">{emp.employeeName}</p>
                            <span className="text-[11px] font-mono text-slate-400">{emp.employeeCode} &bull; {emp.designation || 'Staff'}</span>
                          </td>
                          <td className="py-4 px-4 text-slate-600 font-semibold">{emp.department || 'Direct Tax'}</td>
                          <td className="py-4 px-4 font-black text-slate-900">{emp.assignedTasks}</td>
                          <td className="py-4 px-4 font-bold text-blue-700">{emp.openTasks}</td>
                          <td className="py-4 px-4 font-bold text-amber-700">{emp.inProgressTasks}</td>
                          <td className="py-4 px-4 font-bold text-indigo-700">{emp.underReviewTasks}</td>
                          <td className="py-4 px-4 font-bold text-rose-700">{emp.overdueTasks}</td>
                          <td className="py-4 px-4 font-bold text-emerald-700">{emp.completedTasks}</td>
                          <td className="py-4 px-5 text-right">
                            <div className="inline-flex items-center gap-2">
                              <div className="w-20 bg-slate-100 rounded-full h-2 overflow-hidden">
                                <div
                                  className={`h-full rounded-full ${
                                    emp.completionRate >= 80
                                      ? 'bg-emerald-500'
                                      : emp.completionRate >= 50
                                      ? 'bg-amber-500'
                                      : 'bg-rose-500'
                                  }`}
                                  style={{ width: `${Math.min(100, emp.completionRate)}%` }}
                                ></div>
                              </div>
                              <span className="font-bold text-slate-900 text-xs">{emp.completionRate}%</span>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* =========================================================================
              TAB 5: FINANCIAL & AGING INVOICES
             ========================================================================= */}
          {activeTab === 'financial' && (
            <div className="space-y-6 animate-fade-in">
              {!hasBillingAccess ? (
                <div className="bg-amber-50 border border-amber-200 rounded-2xl p-8 text-center space-y-3">
                  <ShieldAlert className="w-10 h-10 text-amber-600 mx-auto" />
                  <h3 className="text-base font-bold text-amber-900">Confidential Financial Data Restricted</h3>
                  <p className="text-xs text-amber-700 max-w-md mx-auto">
                    Financial reports and invoice aging realizations are restricted to Firm Admins, Practice Owners, and Billing Managers under firm governance policies.
                  </p>
                </div>
              ) : financialData ? (
                <>
                  {/* Financial KPI Cards */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
                    <div className="bg-white p-4.5 rounded-xl border border-slate-200 shadow-xs">
                      <span className="text-xs font-bold text-slate-500">Total Invoiced</span>
                      <p className="text-2xl font-black text-slate-900 mt-1">
                        {formatCurrency(financialData.totalInvoiced)}
                      </p>
                      <span className="text-[11px] text-slate-400 font-semibold">
                        {financialData.totalInvoices} Invoices Issued
                      </span>
                    </div>

                    <div className="bg-white p-4.5 rounded-xl border border-slate-200 shadow-xs">
                      <span className="text-xs font-bold text-emerald-700">Total Collected</span>
                      <p className="text-2xl font-black text-emerald-700 mt-1">
                        {formatCurrency(financialData.totalCollected)}
                      </p>
                      <span className="text-[11px] text-emerald-600 font-semibold">
                        {financialData.totalPaymentsCount} Payment Receipts
                      </span>
                    </div>

                    <div className="bg-white p-4.5 rounded-xl border border-slate-200 shadow-xs">
                      <span className="text-xs font-bold text-rose-700">Total Outstanding Due</span>
                      <p className="text-2xl font-black text-rose-700 mt-1">
                        {formatCurrency(financialData.totalOutstanding)}
                      </p>
                      <span className="text-[11px] text-rose-600 font-semibold">
                        {financialData.overdueInvoices} Overdue Invoices
                      </span>
                    </div>

                    <div className="bg-white p-4.5 rounded-xl border border-slate-200 shadow-xs">
                      <span className="text-xs font-bold text-blue-700">Collected This Month</span>
                      <p className="text-2xl font-black text-blue-700 mt-1">
                        {formatCurrency(financialData.collectedThisMonth)}
                      </p>
                      <span className="text-[11px] text-slate-400 font-semibold">Current Month</span>
                    </div>

                    <div className="bg-white p-4.5 rounded-xl border border-slate-200 shadow-xs">
                      <span className="text-xs font-bold text-indigo-700">Collected This Quarter</span>
                      <p className="text-2xl font-black text-indigo-700 mt-1">
                        {formatCurrency(financialData.collectedThisQuarter)}
                      </p>
                      <span className="text-[11px] text-slate-400 font-semibold">Current Quarter</span>
                    </div>
                  </div>

                  {/* Outstanding Invoices Aging Table */}
                  <div className="bg-white rounded-2xl border border-slate-200/90 shadow-xs overflow-hidden">
                    <div className="p-5 border-b border-slate-200 flex items-center justify-between">
                      <div>
                        <h3 className="text-sm font-bold text-slate-900">Outstanding Invoices Aging Ledger</h3>
                        <p className="text-xs text-slate-500 mt-0.5">
                          Unpaid and partially paid client invoices sorted by overdue severity and outstanding balance.
                        </p>
                      </div>
                      <Link
                        to="/billing"
                        className="text-xs font-bold text-emerald-700 hover:text-emerald-900 bg-emerald-50 px-3 py-1 rounded-md transition-colors"
                      >
                        Open Billing Ledger &rarr;
                      </Link>
                    </div>

                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-xs">
                        <thead className="bg-slate-50 text-slate-600 font-bold border-b border-slate-200">
                          <tr>
                            <th className="py-3.5 px-5">Invoice Number</th>
                            <th className="py-3.5 px-4">Client Name</th>
                            <th className="py-3.5 px-4">Invoice Date</th>
                            <th className="py-3.5 px-4">Due Date</th>
                            <th className="py-3.5 px-4">Total Amount</th>
                            <th className="py-3.5 px-4 text-emerald-700">Paid Amount</th>
                            <th className="py-3.5 px-4 text-rose-700">Balance Due</th>
                            <th className="py-3.5 px-4">Aging Status</th>
                            <th className="py-3.5 px-5 text-right">Action</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 font-medium">
                          {financialData.outstandingInvoices.length > 0 ? (
                            financialData.outstandingInvoices.map((inv) => (
                              <tr key={inv.invoiceId} className="hover:bg-slate-50/80 transition-colors">
                                <td className="py-4 px-5 font-mono font-bold text-slate-900">{inv.invoiceNumber}</td>
                                <td className="py-4 px-4 font-bold text-slate-800">{inv.clientName}</td>
                                <td className="py-4 px-4 text-slate-500">{inv.invoiceDate || 'N/A'}</td>
                                <td className="py-4 px-4 text-slate-500">{inv.dueDate || 'N/A'}</td>
                                <td className="py-4 px-4 font-semibold text-slate-900">{formatCurrency(inv.totalAmount)}</td>
                                <td className="py-4 px-4 font-semibold text-emerald-700">{formatCurrency(inv.paidAmount)}</td>
                                <td className="py-4 px-4 font-black text-rose-700">{formatCurrency(inv.balanceDue)}</td>
                                <td className="py-4 px-4">
                                  {inv.isOverdue ? (
                                    <span className="inline-flex items-center gap-1 text-[11px] font-bold text-rose-700 bg-rose-50 px-2.5 py-0.5 rounded-full">
                                      <AlertCircle className="w-3 h-3" /> {inv.daysDueOrOverdue} Days Overdue
                                    </span>
                                  ) : (
                                    <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-amber-700 bg-amber-50 px-2.5 py-0.5 rounded-full">
                                      <Clock className="w-3 h-3" /> Due in {inv.daysDueOrOverdue} Days
                                    </span>
                                  )}
                                </td>
                                <td className="py-4 px-5 text-right">
                                  <Link
                                    to={`/billing`}
                                    className="inline-flex items-center gap-1 text-xs font-bold text-indigo-600 hover:text-indigo-800 bg-indigo-50 hover:bg-indigo-100 px-3 py-1 rounded-md transition-colors"
                                  >
                                    View Invoice <ArrowUpRight className="w-3 h-3" />
                                  </Link>
                                </td>
                              </tr>
                            ))
                          ) : (
                            <tr>
                              <td colSpan={9} className="py-8 text-center text-slate-400">
                                No outstanding invoices found. All bills are settled!
                              </td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </>
              ) : null}
            </div>
          )}
        </>
      )}
    </div>
  );
};
