import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  FileSpreadsheet,
  CheckCircle2,
  AlertCircle,
  Sparkles,
  Plus,
  ArrowRight,
  Clock,
  ShieldCheck,
  Calendar,
  Layers,
  FileCheck,
  Filter,
  Calculator,
  Receipt,
  Building2,
  Download,
  Percent,
  Search,
  ExternalLink,
  DollarSign,
  UserCheck,
} from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { tdsApi, clientApi } from '../api/endpoints';
import { useBranding } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import {
  TdsReturn,
  TdsProfile,
  TdsChallan,
  TdsCertificate,
  TdsSectionRate,
  TdsComputationRequest,
  TdsComputationResult,
  TdsWorkloadDashboard,
  Client,
} from '../types';
import clsx from 'clsx';

export const TdsCompliancePage: React.FC = () => {
  // Main State
  const [activeMainTab, setActiveMainTab] = useState<'RETURNS' | 'PROFILES' | 'CHALLANS' | 'CALCULATOR' | 'CERTIFICATES'>('RETURNS');
  const [selectedQuarter, setSelectedQuarter] = useState<string>('Q1');
  const [financialYear, setFinancialYear] = useState<string>('2026-27');
  const [formFilter, setFormFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Data Collections
  const [dashboardData, setDashboardData] = useState<TdsWorkloadDashboard | null>(null);
  const [returns, setReturns] = useState<TdsReturn[]>([]);
  const [profiles, setProfiles] = useState<TdsProfile[]>([]);
  const [challans, setChallans] = useState<TdsChallan[]>([]);
  const [certificates, setCertificates] = useState<TdsCertificate[]>([]);
  const [sectionRates, setSectionRates] = useState<TdsSectionRate[]>([]);
  const [clients, setClients] = useState<Client[]>([]);

  // Modals
  const [isRecordFilingModalOpen, setIsRecordFilingModalOpen] = useState(false);
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [isBatchModalOpen, setIsBatchModalOpen] = useState(false);
  const [isNewReturnModalOpen, setIsNewReturnModalOpen] = useState(false);
  const [isNewProfileModalOpen, setIsNewProfileModalOpen] = useState(false);
  const [isNewChallanModalOpen, setIsNewChallanModalOpen] = useState(false);
  const [isNewCertModalOpen, setIsNewCertModalOpen] = useState(false);
  const [selectedReturn, setSelectedReturn] = useState<TdsReturn | null>(null);

  // Form States - Filing & Status
  const [filingToken, setFilingToken] = useState('');
  const [filingReceipt, setFilingReceipt] = useState('');
  const [filingDate, setFilingDate] = useState(new Date().toISOString().split('T')[0]);
  const [newStatus, setNewStatus] = useState<any>('FILED');
  const [statusNotes, setStatusNotes] = useState('');

  // Form States - Batch Generation
  const [batchQuarter, setBatchQuarter] = useState('Q1');
  const [batchFy, setBatchFy] = useState('2026-27');
  const [batchDueDate, setBatchDueDate] = useState('2026-07-31');

  // Form States - New TAN Profile
  const [newProfileClientId, setNewProfileClientId] = useState('');
  const [newProfileTan, setNewProfileTan] = useState('');
  const [newProfileDeductorType, setNewProfileDeductorType] = useState<any>('COMPANY');
  const [newProfileRespPerson, setNewProfileRespPerson] = useState('');
  const [newProfileRespPan, setNewProfileRespPan] = useState('');
  const [newProfileRespDesignation, setNewProfileRespDesignation] = useState('Director');
  const [newProfileRespEmail, setNewProfileRespEmail] = useState('');
  const [newProfileRespMobile, setNewProfileRespMobile] = useState('');

  // Form States - New Challan 281
  const [newChallanProfileId, setNewChallanProfileId] = useState('');
  const [newChallanBsr, setNewChallanBsr] = useState('0510304');
  const [newChallanDate, setNewChallanDate] = useState(new Date().toISOString().split('T')[0]);
  const [newChallanSerial, setNewChallanSerial] = useState('00101');
  const [newChallanMajorHead, setNewChallanMajorHead] = useState<any>('HEAD_0021_NON_COMPANY');
  const [newChallanSection, setNewChallanSection] = useState('194C');
  const [newChallanTdsAmount, setNewChallanTdsAmount] = useState<number>(0);
  const [newChallanQuarter, setNewChallanQuarter] = useState<any>('Q1');

  // Calculator Interactive State
  const [calcSection, setCalcSection] = useState<string>('194C');
  const [calcAmount, setCalcAmount] = useState<number>(150000);
  const [calcDeducteeType, setCalcDeducteeType] = useState<'COMPANY' | 'NON_COMPANY'>('NON_COMPANY');
  const [calcValidPan, setCalcValidPan] = useState<boolean>(true);
  const [calcNonFiler206AB, setCalcNonFiler206AB] = useState<boolean>(false);
  const [calcLowerRate, setCalcLowerRate] = useState<string>('');
  const [calcPaymentDate, setCalcPaymentDate] = useState<string>('2026-06-15');
  const [calcDeductionDate, setCalcDeductionDate] = useState<string>('2026-06-15');
  const [calcDepositDate, setCalcDepositDate] = useState<string>('2026-07-05');
  const [calcFilingDueDate, setCalcFilingDueDate] = useState<string>('2026-07-31');
  const [calcActualFilingDate, setCalcActualFilingDate] = useState<string>('2026-07-30');
  const [calcResult, setCalcResult] = useState<TdsComputationResult | null>(null);

  const { currentTheme } = useBranding();
  const { practiceName } = useAuth();

  useEffect(() => {
    loadAllData();
  }, [selectedQuarter, financialYear]);

  useEffect(() => {
    runCalculation();
  }, [calcSection, calcAmount, calcDeducteeType, calcValidPan, calcNonFiler206AB, calcLowerRate, calcPaymentDate, calcDeductionDate, calcDepositDate, calcFilingDueDate, calcActualFilingDate]);

  const loadAllData = async () => {
    try {
      setIsLoading(true);
      const [dashRes, returnRes, profRes, chalRes, rateRes, clientRes] = await Promise.all([
        tdsApi.getWorkloadDashboard(selectedQuarter, financialYear).catch(() => null),
        tdsApi.getReturns({ quarter: selectedQuarter !== 'ALL' ? selectedQuarter : undefined, financialYear, size: 100 }).catch(() => ({ content: [] })),
        tdsApi.getProfiles({ size: 100 }).catch(() => ({ content: [] })),
        tdsApi.getChallans({ quarter: selectedQuarter !== 'ALL' ? selectedQuarter : undefined, financialYear, size: 100 }).catch(() => ({ content: [] })),
        tdsApi.getSectionRates().catch(() => []),
        clientApi.getAll({ size: 100 }).catch(() => ({ content: [] })),
      ]);

      setDashboardData(dashRes);
      setReturns(Array.isArray(returnRes) ? returnRes : (returnRes?.content || []));
      setProfiles(Array.isArray(profRes) ? profRes : (profRes?.content || []));
      setChallans(Array.isArray(chalRes) ? chalRes : (chalRes?.content || []));
      setSectionRates(rateRes || []);
      setClients(Array.isArray(clientRes) ? clientRes : (clientRes?.content || []));
    } catch (err) {
      console.error('Failed to load TDS data', err);
    } finally {
      setIsLoading(false);
    }
  };

  const runCalculation = async () => {
    try {
      const payload: TdsComputationRequest = {
        sectionCode: calcSection,
        amount: Number(calcAmount) || 0,
        deducteeType: calcDeducteeType,
        validPanProvided: calcValidPan,
        specifiedNonFiler206AB: calcNonFiler206AB,
        lowerDeductionRate: calcLowerRate ? Number(calcLowerRate) : undefined,
        paymentCreditDate: calcPaymentDate || undefined,
        deductionDate: calcDeductionDate || undefined,
        depositDate: calcDepositDate || undefined,
        filingDueDate: calcFilingDueDate || undefined,
        actualFilingDate: calcActualFilingDate || undefined,
      };
      const res = await tdsApi.computeTds(payload);
      setCalcResult(res);
    } catch (err) {
      console.error('Error running TDS calculation', err);
    }
  };

  const handleSeedDemo = async () => {
    try {
      setIsLoading(true);
      await tdsApi.seedDemo();
      await loadAllData();
    } catch (err) {
      console.error('Failed to seed demo data', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleBatchGenerate = async () => {
    try {
      setIsLoading(true);
      await tdsApi.batchGenerateReturns({
        quarter: batchQuarter,
        financialYear: batchFy,
        dueDate: batchDueDate,
        formTypes: ['FORM_24Q', 'FORM_26Q'],
      });
      setIsBatchModalOpen(false);
      await loadAllData();
    } catch (err) {
      console.error('Failed batch generation', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRecordFilingSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedReturn) return;
    try {
      setIsLoading(true);
      await tdsApi.recordFiling(selectedReturn.id, {
        filingDate,
        tokenNumber: filingToken,
        receiptNumber: filingReceipt,
        notes: statusNotes,
      });
      setIsRecordFilingModalOpen(false);
      await loadAllData();
    } catch (err) {
      console.error('Failed recording filing', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleStatusUpdateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedReturn) return;
    try {
      setIsLoading(true);
      await tdsApi.updateReturnStatus(selectedReturn.id, {
        filingStatus: newStatus,
        notes: statusNotes,
      });
      setIsStatusModalOpen(false);
      await loadAllData();
    } catch (err) {
      console.error('Failed updating status', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setIsLoading(true);
      await tdsApi.createProfile({
        clientId: newProfileClientId,
        tan: newProfileTan.toUpperCase().trim(),
        deductorType: newProfileDeductorType,
        responsiblePersonName: newProfileRespPerson,
        responsiblePersonPan: newProfileRespPan.toUpperCase().trim(),
        responsiblePersonDesignation: newProfileRespDesignation,
        responsiblePersonEmail: newProfileRespEmail,
        responsiblePersonMobile: newProfileRespMobile,
        status: 'ACTIVE',
      });
      setIsNewProfileModalOpen(false);
      setNewProfileTan('');
      setNewProfileRespPerson('');
      await loadAllData();
    } catch (err) {
      console.error('Failed creating profile', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateChallanSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setIsLoading(true);
      await tdsApi.createChallan({
        tdsProfileId: newChallanProfileId,
        bsrCode: newChallanBsr,
        challanDate: newChallanDate,
        challanSerialNo: newChallanSerial,
        majorHead: newChallanMajorHead,
        sectionCode: newChallanSection,
        tdsAmount: Number(newChallanTdsAmount),
        quarter: newChallanQuarter,
        financialYear: financialYear,
        paymentMode: 'NET_BANKING',
      });
      setIsNewChallanModalOpen(false);
      await loadAllData();
    } catch (err) {
      console.error('Failed creating challan', err);
    } finally {
      setIsLoading(false);
    }
  };

  // Filtered Returns
  const filteredReturns = returns.filter((r) => {
    const matchForm = formFilter === 'ALL' || r.formType === formFilter;
    const matchSearch =
      !searchQuery ||
      r.clientName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.tan?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.tokenNumber?.toLowerCase().includes(searchQuery.toLowerCase());
    return matchForm && matchSearch;
  });

  // Table Columns - Quarterly Returns
  const returnColumns: Column<TdsReturn>[] = [
    {
      header: 'Form / Quarter',
      accessor: (r) => (
        <div>
          <div className="flex items-center space-x-2">
            <span className="font-semibold text-slate-900 dark:text-white">{r.formType.replace('FORM_', 'Form ')}</span>
            <span className="px-2 py-0.5 text-xs font-medium rounded bg-indigo-50 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300">
              {r.quarter}
            </span>
          </div>
          <div className="text-xs text-slate-500">FY {r.financialYear} (AY {r.assessmentYear})</div>
        </div>
      ),
    },
    {
      header: 'Client & TAN',
      accessor: (r) => (
        <div>
          <div className="font-medium text-slate-900 dark:text-white">{r.clientName || 'Direct Deductor'}</div>
          <div className="text-xs font-mono text-slate-500 uppercase tracking-wider">{r.tan || 'TAN Master Linked'}</div>
        </div>
      ),
    },
    {
      header: 'TDS Deducted / Deposited',
      accessor: (r) => (
        <div>
          <div className="font-medium text-slate-900 dark:text-white">₹{(r.totalTaxDeducted || 0).toLocaleString('en-IN')}</div>
          <div className="text-xs text-emerald-600 dark:text-emerald-400">Paid: ₹{(r.totalTaxDeposited || 0).toLocaleString('en-IN')}</div>
        </div>
      ),
    },
    {
      header: 'Due Date',
      accessor: (r) => (
        <div className="text-sm">
          <div className={clsx(
            'font-medium',
            r.filingStatus !== 'FILED' && new Date(r.dueDate || '') < new Date() ? 'text-rose-600 dark:text-rose-400 font-semibold' : 'text-slate-700 dark:text-slate-300'
          )}>
            {r.dueDate || '31-Jul-2026'}
          </div>
          {r.filingDate && <div className="text-xs text-slate-500">Filed: {r.filingDate}</div>}
        </div>
      ),
    },
    {
      header: 'Workflow Status',
      accessor: (r) => (
        <div>
          <StatusBadge status={r.filingStatus} />
          {r.tokenNumber && (
            <div className="mt-1 text-[11px] font-mono text-slate-500 flex items-center gap-1">
              <ShieldCheck className="w-3 h-3 text-emerald-500" />
              PRN: {r.tokenNumber}
            </div>
          )}
        </div>
      ),
    },
    {
      header: 'Actions',
      accessor: (r) => (
        <div className="flex items-center space-x-2">
          {r.filingStatus !== 'FILED' ? (
            <Button
              size="sm"
              variant="primary"
              onClick={() => {
                setSelectedReturn(r);
                setIsRecordFilingModalOpen(true);
              }}
            >
              Record Filing
            </Button>
          ) : (
            <Button
              size="sm"
              variant="outline"
              onClick={() => {
                setSelectedReturn(r);
                setIsStatusModalOpen(true);
              }}
            >
              Update
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header & Quick Action Hub */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
        <div>
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400">
              <FileSpreadsheet className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-slate-900 dark:text-white">TDS & TCS Compliance Hub</h1>
              <p className="text-sm text-slate-500 dark:text-slate-400">
                End-to-end Indian Tax Deducted at Source practice management for {practiceName || 'Taxoryn CA Practice'}
              </p>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Button variant="outline" size="sm" onClick={handleSeedDemo}>
            <Sparkles className="w-4 h-4 mr-1.5 text-amber-500" />
            Seed Demo Practice
          </Button>

          <Link to="/tds/migration">
            <Button variant="outline" size="sm">
              <Download className="w-4 h-4 mr-1.5" />
              Migration Hub
            </Button>
          </Link>

          <Button variant="outline" size="sm" onClick={() => setIsBatchModalOpen(true)}>
            <Layers className="w-4 h-4 mr-1.5" />
            Batch Returns
          </Button>

          <Button variant="primary" size="sm" onClick={() => setIsNewProfileModalOpen(true)}>
            <Plus className="w-4 h-4 mr-1.5" />
            New TAN Profile
          </Button>
        </div>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        <div className="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Active TAN Clients</span>
            <Building2 className="w-4 h-4 text-indigo-500" />
          </div>
          <div className="mt-2 text-2xl font-bold text-slate-900 dark:text-white">
            {dashboardData?.activeTanProfiles ?? profiles.length}
          </div>
          <div className="mt-1 text-xs text-slate-500">Total TAN Master Records</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Returns Scheduled</span>
            <Calendar className="w-4 h-4 text-blue-500" />
          </div>
          <div className="mt-2 text-2xl font-bold text-slate-900 dark:text-white">
            {dashboardData?.totalScheduledReturns ?? returns.length}
          </div>
          <div className="mt-1 text-xs text-blue-600 dark:text-blue-400 font-medium">
            {dashboardData?.filedReturns ?? returns.filter((r) => r.filingStatus === 'FILED').length} Filed (
            {dashboardData?.totalScheduledReturns ? Math.round(((dashboardData.filedReturns || 0) / dashboardData.totalScheduledReturns) * 100) : 0}%)
          </div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Overdue Filings</span>
            <AlertCircle className="w-4 h-4 text-rose-500" />
          </div>
          <div className="mt-2 text-2xl font-bold text-rose-600 dark:text-rose-400">
            {dashboardData?.overdueReturns ?? returns.filter((r) => r.filingStatus === 'OVERDUE').length}
          </div>
          <div className="mt-1 text-xs text-slate-500">Sec 234E late fees accruing</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Total TDS Deducted</span>
            <DollarSign className="w-4 h-4 text-emerald-500" />
          </div>
          <div className="mt-2 text-2xl font-bold text-slate-900 dark:text-white">
            ₹{(dashboardData?.totalPracticeTdsDeducted || returns.reduce((acc, r) => acc + (r.totalTaxDeducted || 0), 0)).toLocaleString('en-IN')}
          </div>
          <div className="mt-1 text-xs text-slate-500">Quarterly Deductions</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Challan 281 Paid</span>
            <Receipt className="w-4 h-4 text-teal-500" />
          </div>
          <div className="mt-2 text-2xl font-bold text-slate-900 dark:text-white">
            ₹{(dashboardData?.totalPracticeChallansPaid || challans.reduce((acc, c) => acc + (c.totalAmount || 0), 0)).toLocaleString('en-IN')}
          </div>
          <div className="mt-1 text-xs text-emerald-600 dark:text-emerald-400 font-medium">CIN Verified Deposits</div>
        </div>
      </div>

      {/* Main Navigation Tabs */}
      <div className="border-b border-slate-200 dark:border-slate-800">
        <nav className="flex space-x-6 overflow-x-auto" aria-label="Tabs">
          {[
            { id: 'RETURNS', label: 'Quarterly Statements (24Q / 26Q / 27Q)', icon: FileSpreadsheet },
            { id: 'PROFILES', label: 'TAN Master Register', icon: Building2 },
            { id: 'CHALLANS', label: 'Challan ITNS 281 Register', icon: Receipt },
            { id: 'CALCULATOR', label: 'Rate Engine & Late Fee Calculator', icon: Calculator },
            { id: 'CERTIFICATES', label: 'Form 16 / 16A Dispatch', icon: FileCheck },
          ].map((tab) => {
            const Icon = tab.icon;
            const isActive = activeMainTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveMainTab(tab.id as any)}
                className={clsx(
                  'flex items-center py-3.5 px-1 border-b-2 font-medium text-sm whitespace-nowrap transition-colors',
                  isActive
                    ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 dark:border-indigo-400'
                    : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200'
                )}
              >
                <Icon className="w-4 h-4 mr-2" />
                {tab.label}
              </button>
            );
          })}
        </nav>
      </div>

      {/* TAB 1: QUARTERLY RETURNS */}
      {activeMainTab === 'RETURNS' && (
        <div className="space-y-4">
          {/* Filter Bar */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-white dark:bg-slate-900 p-4 rounded-xl border border-slate-200 dark:border-slate-800">
            <div className="flex flex-wrap items-center gap-2">
              <div className="flex items-center bg-slate-100 dark:bg-slate-800 rounded-lg p-1">
                {['ALL', 'Q1', 'Q2', 'Q3', 'Q4'].map((q) => (
                  <button
                    key={q}
                    onClick={() => setSelectedQuarter(q)}
                    className={clsx(
                      'px-3 py-1.5 text-xs font-semibold rounded-md transition-all',
                      selectedQuarter === q
                        ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-sm'
                        : 'text-slate-500 hover:text-slate-700 dark:text-slate-400'
                    )}
                  >
                    {q === 'ALL' ? 'All Quarters' : q}
                  </button>
                ))}
              </div>

              <select
                value={financialYear}
                onChange={(e) => setFinancialYear(e.target.value)}
                className="text-xs bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg px-3 py-2 text-slate-900 dark:text-white font-medium"
              >
                <option value="2026-27">FY 2026-27</option>
                <option value="2025-26">FY 2025-26</option>
                <option value="2024-25">FY 2024-25</option>
              </select>

              <select
                value={formFilter}
                onChange={(e) => setFormFilter(e.target.value)}
                className="text-xs bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg px-3 py-2 text-slate-900 dark:text-white font-medium"
              >
                <option value="ALL">All Forms</option>
                <option value="FORM_24Q">Form 24Q (Salary)</option>
                <option value="FORM_26Q">Form 26Q (Domestic Non-Salary)</option>
                <option value="FORM_27Q">Form 27Q (Non-Resident)</option>
                <option value="FORM_27EQ">Form 27EQ (TCS)</option>
              </select>
            </div>

            <div className="relative w-full sm:w-64">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                placeholder="Search Client, TAN, Token..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>

          {/* DataTable */}
          <DataTable
            columns={returnColumns}
            data={filteredReturns}
            isLoading={isLoading}
            emptyMessage="No quarterly TDS statements found matching your filter."
          />
        </div>
      )}

      {/* TAB 2: TAN MASTER PROFILES */}
      {activeMainTab === 'PROFILES' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center bg-white dark:bg-slate-900 p-4 rounded-xl border border-slate-200 dark:border-slate-800">
            <div>
              <h2 className="text-base font-semibold text-slate-900 dark:text-white">Registered TAN Deductor Master</h2>
              <p className="text-xs text-slate-500">All 10-character Tax Deduction Account Numbers with Principal Officers</p>
            </div>
            <Button variant="primary" size="sm" onClick={() => setIsNewProfileModalOpen(true)}>
              <Plus className="w-4 h-4 mr-1.5" />
              Add TAN Profile
            </Button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {profiles.map((p) => (
              <div key={p.id} className="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-3">
                <div className="flex justify-between items-start">
                  <div>
                    <span className="font-mono text-sm font-bold text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950 px-2 py-1 rounded">
                      {p.tan}
                    </span>
                    <h3 className="mt-2 font-semibold text-slate-900 dark:text-white">{p.clientName || 'Client Deductor'}</h3>
                  </div>
                  <span className={clsx(
                    'px-2 py-0.5 text-xs font-semibold rounded',
                    p.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300' : 'bg-slate-100 text-slate-600'
                  )}>
                    {p.status}
                  </span>
                </div>

                <div className="text-xs text-slate-600 dark:text-slate-400 space-y-1">
                  <div><span className="text-slate-400">Category:</span> {p.deductorType}</div>
                  <div><span className="text-slate-400">Principal Officer:</span> {p.responsiblePersonName || 'Not Set'} ({p.responsiblePersonDesignation || 'Director'})</div>
                  {p.responsiblePersonPan && <div><span className="text-slate-400">Officer PAN:</span> <span className="font-mono">{p.responsiblePersonPan}</span></div>}
                  {p.responsiblePersonEmail && <div><span className="text-slate-400">Email:</span> {p.responsiblePersonEmail}</div>}
                  {p.responsiblePersonMobile && <div><span className="text-slate-400">Phone:</span> {p.responsiblePersonMobile}</div>}
                </div>

                <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex justify-between items-center text-xs">
                  <div className="text-slate-500">TRACES: <span className="font-medium text-slate-700 dark:text-slate-300">{p.tracesStatus}</span></div>
                  <Button size="sm" variant="ghost" onClick={() => {}}>View Statement History</Button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* TAB 3: CHALLAN 281 REGISTER */}
      {activeMainTab === 'CHALLANS' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center bg-white dark:bg-slate-900 p-4 rounded-xl border border-slate-200 dark:border-slate-800">
            <div>
              <h2 className="text-base font-semibold text-slate-900 dark:text-white">Challan ITNS 281 Deposit Register</h2>
              <p className="text-xs text-slate-500">Track BSR Code, Challan Date, Serial No, CIN, and Deductee Allocation Balances</p>
            </div>
            <Button variant="primary" size="sm" onClick={() => setIsNewChallanModalOpen(true)}>
              <Plus className="w-4 h-4 mr-1.5" />
              Record Challan 281
            </Button>
          </div>

          <div className="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 overflow-hidden shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 dark:bg-slate-800/60 text-xs uppercase font-semibold text-slate-500 border-b border-slate-200 dark:border-slate-700">
                  <tr>
                    <th className="py-3 px-4">CIN / BSR / Serial</th>
                    <th className="py-3 px-4">Client / TAN</th>
                    <th className="py-3 px-4">Deposit Date</th>
                    <th className="py-3 px-4">Section Code</th>
                    <th className="py-3 px-4">Total Amount</th>
                    <th className="py-3 px-4">Utilized / Balance</th>
                    <th className="py-3 px-4">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {challans.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="py-8 text-center text-slate-500 text-sm">
                        No ITNS 281 Challans recorded yet for this period.
                      </td>
                    </tr>
                  ) : (
                    challans.map((c) => (
                      <tr key={c.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40">
                        <td className="py-3 px-4 font-mono text-xs">
                          <div className="font-semibold text-slate-900 dark:text-white">{c.cin || `${c.bsrCode}${c.challanDate.replace(/-/g, '')}${c.challanSerialNo}`}</div>
                          <div className="text-slate-500">BSR: {c.bsrCode} | Ser: {c.challanSerialNo}</div>
                        </td>
                        <td className="py-3 px-4">
                          <div className="font-medium text-slate-900 dark:text-white">{c.clientName || 'Practice Client'}</div>
                          <div className="font-mono text-xs text-slate-500">{c.tan}</div>
                        </td>
                        <td className="py-3 px-4 text-slate-700 dark:text-slate-300 font-medium">{c.challanDate}</td>
                        <td className="py-3 px-4">
                          <span className="px-2 py-0.5 text-xs font-semibold rounded bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300">
                            Sec {c.sectionCode}
                          </span>
                        </td>
                        <td className="py-3 px-4 font-semibold text-slate-900 dark:text-white">
                          ₹{(c.totalAmount || 0).toLocaleString('en-IN')}
                        </td>
                        <td className="py-3 px-4 text-xs">
                          <div className="text-emerald-600 font-medium">Utilized: ₹{(c.utilizedAmount || 0).toLocaleString('en-IN')}</div>
                          <div className="text-slate-500">Balance: ₹{(c.balanceAmount || 0).toLocaleString('en-IN')}</div>
                        </td>
                        <td className="py-3 px-4">
                          <span className={clsx(
                            'px-2 py-0.5 text-xs font-semibold rounded',
                            c.challanStatus === 'FULLY_UTILIZED' ? 'bg-emerald-50 text-emerald-700' :
                            c.challanStatus === 'PARTIALLY_UTILIZED' ? 'bg-blue-50 text-blue-700' : 'bg-amber-50 text-amber-700'
                          )}>
                            {c.challanStatus}
                          </span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: RATE ENGINE & CALCULATOR */}
      {activeMainTab === 'CALCULATOR' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Interactive Calculator Engine */}
          <div className="lg:col-span-5 bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
            <div className="flex items-center space-x-2">
              <Calculator className="w-5 h-5 text-indigo-600" />
              <h2 className="text-base font-bold text-slate-900 dark:text-white">Instant TDS & Late Fee Calculator</h2>
            </div>
            <p className="text-xs text-slate-500">
              Computes base TDS, 206AA penalty rates, Sec 201(1A) delay interest, and Sec 234E late filing fees.
            </p>

            <div className="space-y-3 text-xs">
              <div>
                <label className="font-semibold text-slate-700 dark:text-slate-300 block mb-1">Select Section</label>
                <select
                  value={calcSection}
                  onChange={(e) => setCalcSection(e.target.value)}
                  className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg p-2 text-slate-900 dark:text-white font-medium"
                >
                  {sectionRates.map((s) => (
                    <option key={s.sectionCode} value={s.sectionCode}>
                      Sec {s.sectionCode} - {s.title}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="font-semibold text-slate-700 dark:text-slate-300 block mb-1">Gross Payment / Credit Amount (₹)</label>
                <input
                  type="number"
                  value={calcAmount}
                  onChange={(e) => setCalcAmount(Number(e.target.value))}
                  className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg p-2 text-slate-900 dark:text-white font-semibold text-sm"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <div>
                  <label className="font-semibold text-slate-700 dark:text-slate-300 block mb-1">Deductee Type</label>
                  <select
                    value={calcDeducteeType}
                    onChange={(e) => setCalcDeducteeType(e.target.value as any)}
                    className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg p-2 text-slate-900 dark:text-white"
                  >
                    <option value="NON_COMPANY">Individual / HUF / Firm</option>
                    <option value="COMPANY">Domestic / Foreign Company</option>
                  </select>
                </div>

                <div>
                  <label className="font-semibold text-slate-700 dark:text-slate-300 block mb-1">Sec 197 Lower Rate (%)</label>
                  <input
                    type="number"
                    placeholder="Optional lower rate"
                    value={calcLowerRate}
                    onChange={(e) => setCalcLowerRate(e.target.value)}
                    className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg p-2 text-slate-900 dark:text-white"
                  />
                </div>
              </div>

              <div className="flex items-center space-x-4 pt-1">
                <label className="flex items-center space-x-1.5 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={calcValidPan}
                    onChange={(e) => setCalcValidPan(e.target.checked)}
                    className="rounded text-indigo-600 focus:ring-indigo-500"
                  />
                  <span className="text-slate-700 dark:text-slate-300">Valid PAN Available</span>
                </label>

                <label className="flex items-center space-x-1.5 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={calcNonFiler206AB}
                    onChange={(e) => setCalcNonFiler206AB(e.target.checked)}
                    className="rounded text-rose-600 focus:ring-rose-500"
                  />
                  <span className="text-rose-600 dark:text-rose-400 font-medium">Sec 206AB Non-Filer</span>
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-2">
                <div>
                  <label className="font-semibold text-slate-700 dark:text-slate-300 block mb-1">Payment Date</label>
                  <input
                    type="date"
                    value={calcPaymentDate}
                    onChange={(e) => setCalcPaymentDate(e.target.value)}
                    className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg p-2 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="font-semibold text-slate-700 dark:text-slate-300 block mb-1">Challan Deposit Date</label>
                  <input
                    type="date"
                    value={calcDepositDate}
                    onChange={(e) => setCalcDepositDate(e.target.value)}
                    className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg p-2 text-slate-900 dark:text-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <div>
                  <label className="font-semibold text-slate-700 dark:text-slate-300 block mb-1">Filing Due Date</label>
                  <input
                    type="date"
                    value={calcFilingDueDate}
                    onChange={(e) => setCalcFilingDueDate(e.target.value)}
                    className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg p-2 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="font-semibold text-slate-700 dark:text-slate-300 block mb-1">Actual Filing Date</label>
                  <input
                    type="date"
                    value={calcActualFilingDate}
                    onChange={(e) => setCalcActualFilingDate(e.target.value)}
                    className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg p-2 text-slate-900 dark:text-white"
                  />
                </div>
              </div>
            </div>

            {/* Result Box */}
            {calcResult && (
              <div className="mt-4 p-4 rounded-xl bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 space-y-2 text-xs">
                <div className="flex justify-between items-center">
                  <span className="text-slate-500">Effective TDS Rate:</span>
                  <span className="font-bold text-indigo-600 dark:text-indigo-400 text-sm">{calcResult.effectiveRate}%</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-slate-500">Base TDS Amount:</span>
                  <span className="font-semibold text-slate-900 dark:text-white">₹{calcResult.baseTdsAmount.toLocaleString('en-IN')}</span>
                </div>
                {calcResult.totalInterest > 0 && (
                  <div className="flex justify-between items-center text-amber-600 dark:text-amber-400">
                    <span>Sec 201(1A) Interest:</span>
                    <span className="font-semibold">+ ₹{calcResult.totalInterest.toLocaleString('en-IN')}</span>
                  </div>
                )}
                {calcResult.lateFee234E > 0 && (
                  <div className="flex justify-between items-center text-rose-600 dark:text-rose-400">
                    <span>Sec 234E Late Fee ({calcResult.delayDays} days @ ₹200/day):</span>
                    <span className="font-semibold">+ ₹{calcResult.lateFee234E.toLocaleString('en-IN')}</span>
                  </div>
                )}
                <div className="pt-2 border-t border-slate-200 dark:border-slate-700 flex justify-between items-center font-bold text-sm">
                  <span className="text-slate-900 dark:text-white">Total Statutory Payable:</span>
                  <span className="text-indigo-600 dark:text-indigo-400">₹{calcResult.totalPayableWithPenalties.toLocaleString('en-IN')}</span>
                </div>
                <div className="mt-2 text-[11px] text-slate-500 italic bg-white dark:bg-slate-900 p-2 rounded border border-slate-200 dark:border-slate-700">
                  {calcResult.remarks}
                </div>
              </div>
            )}
          </div>

          {/* Master Rate Catalog Table */}
          <div className="lg:col-span-7 bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
            <div>
              <h2 className="text-base font-bold text-slate-900 dark:text-white">Indian TDS & TCS Statutory Rate Catalog</h2>
              <p className="text-xs text-slate-500">Standard rates, exemption threshold limits, and form mappings under the Income Tax Act, 1961</p>
            </div>

            <div className="overflow-y-auto max-h-[600px] border border-slate-200 dark:border-slate-700 rounded-xl">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 dark:bg-slate-800 sticky top-0 font-semibold text-slate-500 border-b border-slate-200 dark:border-slate-700">
                  <tr>
                    <th className="py-2.5 px-3">Section</th>
                    <th className="py-2.5 px-3">Nature of Payment</th>
                    <th className="py-2.5 px-3">Ind / HUF</th>
                    <th className="py-2.5 px-3">Company</th>
                    <th className="py-2.5 px-3">Threshold Limit</th>
                    <th className="py-2.5 px-3">Form</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {sectionRates.map((s) => (
                    <tr key={s.sectionCode} className="hover:bg-slate-50 dark:hover:bg-slate-800/40">
                      <td className="py-2.5 px-3 font-mono font-bold text-indigo-600 dark:text-indigo-400">
                        Sec {s.sectionCode}
                      </td>
                      <td className="py-2.5 px-3 font-medium text-slate-900 dark:text-white">
                        {s.title}
                        <div className="text-[10px] text-slate-400 line-clamp-1">{s.statutoryNotes}</div>
                      </td>
                      <td className="py-2.5 px-3 font-semibold text-slate-700 dark:text-slate-300">{s.rateIndividual}%</td>
                      <td className="py-2.5 px-3 font-semibold text-slate-700 dark:text-slate-300">{s.rateOthers}%</td>
                      <td className="py-2.5 px-3 text-slate-600 dark:text-slate-400 font-mono">
                        {s.thresholdLimit > 0 ? `₹${s.thresholdLimit.toLocaleString('en-IN')}` : 'Nil'}
                      </td>
                      <td className="py-2.5 px-3">
                        <span className="px-1.5 py-0.5 text-[10px] font-semibold rounded bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                          {s.returnForm.replace('FORM_', '')}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 5: CERTIFICATES FORM 16 / 16A */}
      {activeMainTab === 'CERTIFICATES' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center bg-white dark:bg-slate-900 p-4 rounded-xl border border-slate-200 dark:border-slate-800">
            <div>
              <h2 className="text-base font-semibold text-slate-900 dark:text-white">Form 16 & Form 16A Certificate Dispatch</h2>
              <p className="text-xs text-slate-500">TRACES certificate request status, digital signatures, and email dispatch to payees</p>
            </div>
            <Button variant="primary" size="sm" onClick={() => setIsNewCertModalOpen(true)}>
              <Plus className="w-4 h-4 mr-1.5" />
              Register Certificate
            </Button>
          </div>

          <div className="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-8 text-center space-y-3">
            <FileCheck className="w-12 h-12 text-indigo-500 mx-auto opacity-80" />
            <h3 className="text-base font-semibold text-slate-900 dark:text-white">Automated TRACES Certificate Hub</h3>
            <p className="text-xs text-slate-500 max-w-md mx-auto">
              Bulk download Form 16 (Part A & B) for salaried employees and Form 16A quarterly certificates for contractors & professionals. Automatically digitally sign and email password-protected PDFs.
            </p>
            <div className="pt-2">
              <Button size="sm" variant="outline">
                <ExternalLink className="w-4 h-4 mr-1.5" />
                Connect TRACES Portal
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: Record e-Filing (Token / PRN) */}
      <Modal
        isOpen={isRecordFilingModalOpen}
        onClose={() => setIsRecordFilingModalOpen(false)}
        title={`Record e-Filing: ${selectedReturn?.formType.replace('FORM_', 'Form ')} ${selectedReturn?.quarter}`}
      >
        <form onSubmit={handleRecordFilingSubmit} className="space-y-4 text-sm">
          <div>
            <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">
              Provisional Receipt Number (15-Digit Token Number) *
            </label>
            <input
              type="text"
              required
              placeholder="e.g. 010020304050607"
              value={filingToken}
              onChange={(e) => setFilingToken(e.target.value)}
              className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-mono"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Filing Date *</label>
              <input
                type="date"
                required
                value={filingDate}
                onChange={(e) => setFilingDate(e.target.value)}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
              />
            </div>
            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Receipt / Ack No</label>
              <input
                type="text"
                placeholder="Optional TIN receipt"
                value={filingReceipt}
                onChange={(e) => setFilingReceipt(e.target.value)}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
              />
            </div>
          </div>

          <div>
            <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Practitioner Notes</label>
            <textarea
              rows={2}
              placeholder="Notes on challan reconciliation, NSDL submission..."
              value={statusNotes}
              onChange={(e) => setStatusNotes(e.target.value)}
              className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
            />
          </div>

          <div className="flex justify-end space-x-2 pt-2">
            <Button variant="outline" onClick={() => setIsRecordFilingModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit">
              <CheckCircle2 className="w-4 h-4 mr-1.5" />
              Confirm Filing
            </Button>
          </div>
        </form>
      </Modal>

      {/* MODAL: Update Return Status */}
      <Modal
        isOpen={isStatusModalOpen}
        onClose={() => setIsStatusModalOpen(false)}
        title="Update TDS Statement Status"
      >
        <form onSubmit={handleStatusUpdateSubmit} className="space-y-4 text-sm">
          <div>
            <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Workflow Status</label>
            <select
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value)}
              className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-medium"
            >
              <option value="PENDING">PENDING</option>
              <option value="DRAFT">DRAFT</option>
              <option value="CHALLANS_ATTACHED">CHALLANS ATTACHED</option>
              <option value="UNDER_REVIEW">UNDER REVIEW</option>
              <option value="READY_TO_FILE">READY TO FILE</option>
              <option value="FILED">FILED</option>
              <option value="OVERDUE">OVERDUE</option>
            </select>
          </div>

          <div>
            <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Notes</label>
            <textarea
              rows={3}
              value={statusNotes}
              onChange={(e) => setStatusNotes(e.target.value)}
              className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
            />
          </div>

          <div className="flex justify-end space-x-2 pt-2">
            <Button variant="outline" onClick={() => setIsStatusModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit">
              Save Changes
            </Button>
          </div>
        </form>
      </Modal>

      {/* MODAL: Batch Generate Returns */}
      <Modal
        isOpen={isBatchModalOpen}
        onClose={() => setIsBatchModalOpen(false)}
        title="Batch Generate Quarterly TDS Statements"
      >
        <div className="space-y-4 text-sm">
          <p className="text-xs text-slate-500">
            Auto-generate Form 24Q (Salary) and Form 26Q (Domestic Non-Salary) quarterly return schedules for all active TAN clients.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Quarter</label>
              <select
                value={batchQuarter}
                onChange={(e) => setBatchQuarter(e.target.value)}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-medium"
              >
                <option value="Q1">Q1 (Apr - Jun)</option>
                <option value="Q2">Q2 (Jul - Sep)</option>
                <option value="Q3">Q3 (Oct - Dec)</option>
                <option value="Q4">Q4 (Jan - Mar)</option>
              </select>
            </div>

            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Financial Year</label>
              <select
                value={batchFy}
                onChange={(e) => setBatchFy(e.target.value)}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-medium"
              >
                <option value="2026-27">2026-27</option>
                <option value="2025-26">2025-26</option>
              </select>
            </div>
          </div>

          <div>
            <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Statutory Due Date</label>
            <input
              type="date"
              value={batchDueDate}
              onChange={(e) => setBatchDueDate(e.target.value)}
              className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
            />
          </div>

          <div className="flex justify-end space-x-2 pt-2">
            <Button variant="outline" onClick={() => setIsBatchModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" onClick={handleBatchGenerate}>
              <Layers className="w-4 h-4 mr-1.5" />
              Generate Batch
            </Button>
          </div>
        </div>
      </Modal>

      {/* MODAL: Register New TAN Profile */}
      <Modal
        isOpen={isNewProfileModalOpen}
        onClose={() => setIsNewProfileModalOpen(false)}
        title="Register Client TAN Profile"
      >
        <form onSubmit={handleCreateProfileSubmit} className="space-y-4 text-sm">
          <div>
            <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Select Client *</label>
            <select
              required
              value={newProfileClientId}
              onChange={(e) => setNewProfileClientId(e.target.value)}
              className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-medium"
            >
              <option value="">-- Choose Client --</option>
              {clients.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.displayName || c.legalName} ({c.pan || 'No PAN'})
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">10-Digit TAN *</label>
              <input
                type="text"
                required
                maxLength={10}
                placeholder="e.g. BLRP12345A"
                value={newProfileTan}
                onChange={(e) => setNewProfileTan(e.target.value.toUpperCase())}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-mono uppercase"
              />
            </div>

            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Deductor Category</label>
              <select
                value={newProfileDeductorType}
                onChange={(e) => setNewProfileDeductorType(e.target.value as any)}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
              >
                <option value="COMPANY">Company (Domestic / Foreign)</option>
                <option value="INDIVIDUAL_HUF">Individual / HUF (Tax Audit)</option>
                <option value="FIRM">Partnership Firm</option>
                <option value="LLP">Limited Liability Partnership</option>
                <option value="GOVERNMENT_CENTRAL">Government - Central (PAO/DDO)</option>
                <option value="GOVERNMENT_STATE">Government - State</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Responsible Person Name</label>
              <input
                type="text"
                placeholder="Managing Director / Authorized Officer"
                value={newProfileRespPerson}
                onChange={(e) => setNewProfileRespPerson(e.target.value)}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
              />
            </div>

            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Responsible Person PAN</label>
              <input
                type="text"
                maxLength={10}
                placeholder="e.g. ABCDE1234F"
                value={newProfileRespPan}
                onChange={(e) => setNewProfileRespPan(e.target.value.toUpperCase())}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-mono uppercase"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Designation</label>
              <input
                type="text"
                placeholder="Director / Partner"
                value={newProfileRespDesignation}
                onChange={(e) => setNewProfileRespDesignation(e.target.value)}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
              />
            </div>

            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Mobile</label>
              <input
                type="text"
                placeholder="9876543210"
                value={newProfileRespMobile}
                onChange={(e) => setNewProfileRespMobile(e.target.value)}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
              />
            </div>
          </div>

          <div className="flex justify-end space-x-2 pt-2">
            <Button variant="outline" onClick={() => setIsNewProfileModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit">
              Register TAN Profile
            </Button>
          </div>
        </form>
      </Modal>

      {/* MODAL: Record Challan 281 */}
      <Modal
        isOpen={isNewChallanModalOpen}
        onClose={() => setIsNewChallanModalOpen(false)}
        title="Record ITNS 281 Challan Deposit"
      >
        <form onSubmit={handleCreateChallanSubmit} className="space-y-4 text-sm">
          <div>
            <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">TAN Profile *</label>
            <select
              required
              value={newChallanProfileId}
              onChange={(e) => setNewChallanProfileId(e.target.value)}
              className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-medium"
            >
              <option value="">-- Choose TAN --</option>
              {profiles.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.tan} - {p.clientName}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">7-Digit BSR Code *</label>
              <input
                type="text"
                required
                maxLength={7}
                value={newChallanBsr}
                onChange={(e) => setNewChallanBsr(e.target.value)}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-mono"
              />
            </div>
            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Challan Date *</label>
              <input
                type="date"
                required
                value={newChallanDate}
                onChange={(e) => setNewChallanDate(e.target.value)}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
              />
            </div>
            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">5-Digit Serial *</label>
              <input
                type="text"
                required
                maxLength={5}
                value={newChallanSerial}
                onChange={(e) => setNewChallanSerial(e.target.value)}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-mono"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">Section Code *</label>
              <select
                value={newChallanSection}
                onChange={(e) => setNewChallanSection(e.target.value)}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-medium"
              >
                {sectionRates.map((s) => (
                  <option key={s.sectionCode} value={s.sectionCode}>
                    Sec {s.sectionCode} - {s.title}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="font-medium text-slate-700 dark:text-slate-300 block mb-1">TDS Deposit Amount (₹) *</label>
              <input
                type="number"
                required
                value={newChallanTdsAmount}
                onChange={(e) => setNewChallanTdsAmount(Number(e.target.value))}
                className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-semibold"
              />
            </div>
          </div>

          <div className="flex justify-end space-x-2 pt-2">
            <Button variant="outline" onClick={() => setIsNewChallanModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit">
              Save Challan 281
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
export default TdsCompliancePage;
