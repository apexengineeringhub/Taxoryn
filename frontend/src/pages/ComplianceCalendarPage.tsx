import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  CheckCircle2,
  Clock,
  Building,
  User,
  Zap,
  ExternalLink,
  Plus,
  Filter,
  CheckCircle,
  XCircle,
  Layers,
  ArrowRight,
  List,
  Grid,
  FileCheck,
  Search,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { complianceApi, clientApi, employeeApi } from '../api/endpoints';
import {
  ComplianceObligationDto,
  ComplianceCalendarSummaryDto,
  ComplianceCycleTemplateDto,
  ComplianceObligationType,
  ComplianceObligationStatus,
  Client,
  Employee,
  CreateComplianceObligationRequest,
  UpdateObligationStatusRequest,
  AssignObligationRequest,
} from '../types';
import clsx from 'clsx';

export const ComplianceCalendarPage: React.FC = () => {
  const [currentDate, setCurrentDate] = useState(new Date(2026, 7, 1)); // August 2026
  const [viewMode, setViewMode] = useState<'GRID' | 'LIST'>('GRID');
  const [obligations, setObligations] = useState<ComplianceObligationDto[]>([]);
  const [summary, setSummary] = useState<ComplianceCalendarSummaryDto | null>(null);
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [selectedStatusTab, setSelectedStatusTab] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [isLoading, setIsLoading] = useState(true);

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [selectedObligation, setSelectedObligation] = useState<ComplianceObligationDto | null>(null);

  // Lists for dropdowns
  const [clients, setClients] = useState<Client[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [cycleTemplates, setCycleTemplates] = useState<ComplianceCycleTemplateDto[]>([]);

  // Create Form State
  const [formClientId, setFormClientId] = useState('');
  const [formType, setFormType] = useState<ComplianceObligationType>('GST_RETURN');
  const [formTitle, setFormTitle] = useState('');
  const [formPeriodLabel, setFormPeriodLabel] = useState('');
  const [formStatutoryDueDate, setFormStatutoryDueDate] = useState('');
  const [formInternalTargetDate, setFormInternalTargetDate] = useState('');
  const [formPriority, setFormPriority] = useState<'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT' | 'CRITICAL'>('HIGH');
  const [formAssignedEmployeeId, setFormAssignedEmployeeId] = useState('');
  const [formRemarks, setFormRemarks] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Status Form State
  const [targetStatus, setTargetStatus] = useState<ComplianceObligationStatus>('FILED');
  const [statusAckNumber, setStatusAckNumber] = useState('');
  const [statusRemarks, setStatusRemarks] = useState('');

  // Assign Form State
  const [targetAssigneeId, setTargetAssigneeId] = useState('');

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  const currentMonthYear = `${monthNames[currentDate.getMonth()]} ${currentDate.getFullYear()}`;

  useEffect(() => {
    loadComplianceData();
    loadDropdownData();
  }, [currentDate, selectedType, selectedStatusTab]);

  const loadDropdownData = async () => {
    try {
      const [clientsRes, empsRes, templatesRes] = await Promise.allSettled([
        clientApi.getAll({ size: 100 }),
        employeeApi.getAll({ size: 100 }),
        complianceApi.getCycleTemplates(),
      ]);

      if (clientsRes.status === 'fulfilled' && clientsRes.value) {
        setClients(clientsRes.value.content || []);
      }
      if (empsRes.status === 'fulfilled' && empsRes.value) {
        setEmployees(empsRes.value.content || []);
      }
      if (templatesRes.status === 'fulfilled' && templatesRes.value) {
        setCycleTemplates(templatesRes.value || []);
      }
    } catch (err) {
      console.warn('Failed to load dropdown reference data', err);
    }
  };

  const loadComplianceData = async () => {
    try {
      setIsLoading(true);
      const startOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1)
        .toISOString().split('T')[0];
      const endOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0)
        .toISOString().split('T')[0];

      const params: any = {
        dueFrom: startOfMonth,
        dueTo: endOfMonth,
        size: 100,
      };

      if (selectedType !== 'ALL') {
        params.obligationType = selectedType as ComplianceObligationType;
      }

      if (selectedStatusTab === 'DUE_TODAY') {
        params.dueToday = true;
      } else if (selectedStatusTab === 'UPCOMING') {
        params.status = 'UPCOMING';
      } else if (selectedStatusTab === 'OVERDUE') {
        params.overdue = true;
      } else if (selectedStatusTab === 'WAITING_FOR_CLIENT') {
        params.waitingForClient = true;
      } else if (selectedStatusTab === 'READY_FOR_FILING') {
        params.readyForFiling = true;
      } else if (selectedStatusTab === 'FILED') {
        params.status = 'FILED';
      } else if (selectedStatusTab === 'COMPLETED') {
        params.status = 'COMPLETED';
      }

      const [obligationsRes, summaryRes] = await Promise.allSettled([
        complianceApi.getCalendarObligations(params),
        complianceApi.getCalendarSummary(params),
      ]);

      if (obligationsRes.status === 'fulfilled' && obligationsRes.value) {
        setObligations(obligationsRes.value.content || []);
      } else {
        setObligations([]);
      }

      if (summaryRes.status === 'fulfilled' && summaryRes.value) {
        setSummary(summaryRes.value);
      }
    } catch (err) {
      console.error('Failed to load compliance calendar data', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handlePrevMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
  };

  const handleNextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));
  };

  const handleCreateObligation = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formClientId || !formTitle || !formStatutoryDueDate) {
      alert('Please fill all required fields: Client, Title, Statutory Due Date');
      return;
    }

    try {
      setIsSubmitting(true);
      const payload: CreateComplianceObligationRequest = {
        clientId: formClientId,
        obligationType: formType,
        title: formTitle,
        periodLabel: formPeriodLabel || undefined,
        statutoryDueDate: formStatutoryDueDate,
        internalTargetDate: formInternalTargetDate || undefined,
        priority: formPriority,
        assignedEmployeeId: formAssignedEmployeeId || undefined,
        remarks: formRemarks || undefined,
      };

      await complianceApi.createObligation(payload);
      setIsCreateModalOpen(false);
      resetCreateForm();
      await loadComplianceData();
    } catch (err: any) {
      alert(`Failed to create obligation: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdateStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedObligation) return;

    try {
      setIsSubmitting(true);
      const payload: UpdateObligationStatusRequest = {
        status: targetStatus,
        acknowledgementNumber: statusAckNumber || undefined,
        remarks: statusRemarks || undefined,
      };

      await complianceApi.updateObligationStatus(selectedObligation.id, payload);
      setIsStatusModalOpen(false);
      setSelectedObligation(null);
      await loadComplianceData();
    } catch (err: any) {
      alert(`Failed to update status: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleAssign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedObligation) return;

    try {
      setIsSubmitting(true);
      const payload: AssignObligationRequest = {
        assignedToId: targetAssigneeId || undefined,
      };

      await complianceApi.assignObligation(selectedObligation.id, payload);
      setIsAssignModalOpen(false);
      setSelectedObligation(null);
      await loadComplianceData();
    } catch (err: any) {
      alert(`Failed to assign obligation: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetCreateForm = () => {
    setFormClientId('');
    setFormType('GST_RETURN');
    setFormTitle('');
    setFormPeriodLabel('');
    setFormStatutoryDueDate('');
    setFormInternalTargetDate('');
    setFormPriority('HIGH');
    setFormAssignedEmployeeId('');
    setFormRemarks('');
  };

  // Statutory presets for calendar display overlay
  const statutoryPresets = [
    { day: 7, title: 'TDS/TCS Deposit', desc: 'Monthly payment of TDS deducted in previous month', type: 'TDS_RETURN' },
    { day: 11, title: 'GSTR-1 Monthly', desc: 'Outward supplies statement for monthly filers', type: 'GST_RETURN' },
    { day: 13, title: 'GSTR-1 IFF (QRMP)', desc: 'Invoice Furnishing Facility for QRMP filers', type: 'GST_RETURN' },
    { day: 15, title: 'Advance Tax / TCS Cert', desc: 'Quarterly Advance Tax installment / TCS cert', type: 'ADVANCE_TAX' },
    { day: 20, title: 'GSTR-3B Monthly', desc: 'Summary return & tax payment for monthly filers', type: 'GST_RETURN' },
    { day: 25, title: 'PMT-06 (QRMP)', desc: 'Challan payment under QRMP scheme', type: 'GST_RETURN' },
    { day: 31, title: 'ITR / Audit Deadlines', desc: 'Annual filing milestones and tax audits', type: 'ITR_FILING' },
  ];

  const filteredObligations = obligations.filter((ob) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      ob.title.toLowerCase().includes(q) ||
      (ob.clientDisplayName && ob.clientDisplayName.toLowerCase().includes(q)) ||
      (ob.clientPan && ob.clientPan.toLowerCase().includes(q)) ||
      (ob.periodLabel && ob.periodLabel.toLowerCase().includes(q))
    );
  });

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900 flex items-center gap-2">
            <CalendarIcon className="w-6 h-6 text-brand-600" />
            Compliance Calendar & Recurring Cycles
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Statutory tax obligations, internal operational milestones, and recurring compliance cycles with zero-leakage portfolio scoping.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="primary"
            leftIcon={<Plus className="w-4 h-4" />}
            onClick={() => {
              resetCreateForm();
              setIsCreateModalOpen(true);
            }}
          >
            New Obligation
          </Button>

          {/* Month Selector */}
          <div className="flex items-center gap-2 bg-white border border-slate-200 rounded-lg p-1 shadow-2xs">
            <button onClick={handlePrevMonth} className="p-1.5 hover:bg-slate-100 rounded text-slate-600">
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="px-3 font-bold text-xs text-slate-800 font-mono">{currentMonthYear}</span>
            <button onClick={handleNextMonth} className="p-1.5 hover:bg-slate-100 rounded text-slate-600">
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          {/* View Mode Toggle */}
          <div className="flex items-center bg-slate-100 p-1 rounded-lg border border-slate-200">
            <button
              onClick={() => setViewMode('GRID')}
              className={clsx(
                'p-1.5 rounded text-xs font-bold transition-all',
                viewMode === 'GRID' ? 'bg-white shadow-2xs text-brand-600' : 'text-slate-600 hover:text-slate-900'
              )}
              title="Calendar Grid View"
            >
              <Grid className="w-4 h-4" />
            </button>
            <button
              onClick={() => setViewMode('LIST')}
              className={clsx(
                'p-1.5 rounded text-xs font-bold transition-all',
                viewMode === 'LIST' ? 'bg-white shadow-2xs text-brand-600' : 'text-slate-600 hover:text-slate-900'
              )}
              title="Obligations List View"
            >
              <List className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* KPI Metrics Summary */}
      {summary && (
        <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-3">
          <div className="bg-white p-3 rounded-xl border border-slate-200 shadow-2xs">
            <span className="text-[10px] font-bold text-slate-500 uppercase block">Total Obligations</span>
            <p className="text-xl font-black text-slate-900 mt-1">{summary.totalObligations}</p>
          </div>
          <div className="bg-white p-3 rounded-xl border border-rose-200 bg-rose-50/20 shadow-2xs">
            <span className="text-[10px] font-bold text-rose-600 uppercase block">🚨 Overdue</span>
            <p className="text-xl font-black text-rose-600 mt-1">{summary.overdue}</p>
          </div>
          <div className="bg-white p-3 rounded-xl border border-amber-200 bg-amber-50/20 shadow-2xs">
            <span className="text-[10px] font-bold text-amber-600 uppercase block">📅 Due Today</span>
            <p className="text-xl font-black text-amber-600 mt-1">{summary.dueToday}</p>
          </div>
          <div className="bg-white p-3 rounded-xl border border-blue-200 bg-blue-50/20 shadow-2xs">
            <span className="text-[10px] font-bold text-blue-600 uppercase block">⏳ Upcoming</span>
            <p className="text-xl font-black text-blue-600 mt-1">{summary.upcoming}</p>
          </div>
          <div className="bg-white p-3 rounded-xl border border-purple-200 bg-purple-50/20 shadow-2xs">
            <span className="text-[10px] font-bold text-purple-600 uppercase block">👥 Client Action</span>
            <p className="text-xl font-black text-purple-600 mt-1">{summary.waitingForClient}</p>
          </div>
          <div className="bg-white p-3 rounded-xl border border-indigo-200 bg-indigo-50/20 shadow-2xs">
            <span className="text-[10px] font-bold text-indigo-600 uppercase block">🚀 Ready to File</span>
            <p className="text-xl font-black text-indigo-600 mt-1">{summary.readyForFiling}</p>
          </div>
          <div className="bg-white p-3 rounded-xl border border-emerald-200 bg-emerald-50/20 shadow-2xs">
            <span className="text-[10px] font-bold text-emerald-600 uppercase block">✅ Completed / Filed</span>
            <p className="text-xl font-black text-emerald-600 mt-1">{summary.filed + summary.completed}</p>
          </div>
        </div>
      )}

      {/* Filter Tabs & Search */}
      <div className="space-y-3">
        {/* Status Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 border-b border-slate-200">
          {[
            { key: 'ALL', label: 'All Items' },
            { key: 'DUE_TODAY', label: 'Due Today' },
            { key: 'UPCOMING', label: 'Upcoming' },
            { key: 'OVERDUE', label: 'Overdue' },
            { key: 'WAITING_FOR_CLIENT', label: 'Waiting for Client' },
            { key: 'READY_FOR_FILING', label: 'Ready for Filing' },
            { key: 'FILED', label: 'Filed' },
            { key: 'COMPLETED', label: 'Completed' },
          ].map((tab) => (
            <button
              key={tab.key}
              onClick={() => setSelectedStatusTab(tab.key)}
              className={clsx(
                'px-3 py-1.5 rounded-lg text-xs font-bold transition-colors whitespace-nowrap',
                selectedStatusTab === tab.key
                  ? 'bg-brand-600 text-white shadow-xs'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Domain Filter & Search Bar */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-1.5 overflow-x-auto w-full sm:w-auto">
            <span className="text-xs font-semibold text-slate-500 mr-2 flex items-center gap-1">
              <Filter className="w-3.5 h-3.5" /> Domain:
            </span>
            {[
              { key: 'ALL', label: 'All Domains' },
              { key: 'GST_RETURN', label: 'GST' },
              { key: 'ITR_FILING', label: 'ITR' },
              { key: 'TDS_RETURN', label: 'TDS' },
              { key: 'TAX_AUDIT', label: 'Audit' },
              { key: 'ADVANCE_TAX', label: 'Advance Tax' },
              { key: 'ROC_ANNUAL_FILING', label: 'ROC / MCA' },
              { key: 'TAX_NOTICE_RESPONSE', label: 'Notices' },
            ].map((cat) => (
              <button
                key={cat.key}
                onClick={() => setSelectedType(cat.key)}
                className={clsx(
                  'px-2.5 py-1 rounded-md text-[11px] font-bold transition-colors whitespace-nowrap',
                  selectedType === cat.key
                    ? 'bg-slate-900 text-white shadow-xs'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                )}
              >
                {cat.label}
              </button>
            ))}
          </div>

          <div className="relative w-full sm:w-64">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
            <input
              type="text"
              placeholder="Search obligations or clients..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 text-xs bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 font-medium"
            />
          </div>
        </div>
      </div>

      {/* Main View Area */}
      {viewMode === 'GRID' ? (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Active Obligations List (Left Column) */}
          <Card
            title="Obligations in Scope"
            subtitle={`${filteredObligations.length} items for ${currentMonthYear}`}
            className="lg:col-span-1"
          >
            <div className="space-y-3 max-h-[650px] overflow-y-auto pr-1">
              {filteredObligations.map((item) => (
                <div key={item.id} className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl space-y-2.5 hover:border-slate-300 transition-colors">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <span className="text-[10px] font-bold px-1.5 py-0.5 rounded uppercase bg-indigo-100 text-indigo-800 font-mono">
                        {item.obligationType}
                      </span>
                      <h4 className="font-bold text-xs text-slate-900 mt-1">{item.title}</h4>
                    </div>
                    <span className={clsx(
                      'text-[10px] font-bold px-1.5 py-0.5 rounded uppercase',
                      item.status === 'COMPLETED' || item.status === 'FILED' ? 'bg-emerald-100 text-emerald-800' :
                      item.status === 'OVERDUE' ? 'bg-rose-100 text-rose-800' :
                      item.status === 'WAITING_FOR_CLIENT' ? 'bg-purple-100 text-purple-800' :
                      item.status === 'READY_FOR_FILING' ? 'bg-indigo-100 text-indigo-800' :
                      'bg-amber-100 text-amber-800'
                    )}>
                      {item.status.replace(/_/g, ' ')}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-[11px] text-slate-600 pt-1 border-t border-slate-200/60">
                    <span className="font-semibold text-slate-800">{item.clientDisplayName || 'Client'}</span>
                    {item.clientPan && <span className="font-mono text-[10px] text-slate-500">PAN: {item.clientPan}</span>}
                  </div>

                  {/* Dual Dates Display */}
                  <div className="grid grid-cols-2 gap-2 text-[10px] bg-white p-2 rounded-lg border border-slate-100">
                    <div>
                      <span className="text-slate-400 font-semibold block">Statutory Due:</span>
                      <span className="font-bold text-rose-700 font-mono">{item.statutoryDueDate}</span>
                    </div>
                    <div>
                      <span className="text-slate-400 font-semibold block">Internal Target:</span>
                      <span className="font-bold text-blue-700 font-mono">{item.internalTargetDate || '—'}</span>
                    </div>
                  </div>

                  {/* Workflow / Assignee / Action */}
                  <div className="pt-1 flex items-center justify-between gap-2">
                    <div className="flex items-center gap-1.5">
                      {item.workflowId ? (
                        <Link
                          to={`/workflows?workflowId=${item.workflowId}`}
                          className="text-[10px] font-bold text-brand-600 hover:text-brand-800 inline-flex items-center gap-1"
                        >
                          <Layers className="w-3 h-3" /> Operational Workflow
                        </Link>
                      ) : (
                        <span className="text-[10px] text-slate-400 font-medium">No workflow</span>
                      )}
                    </div>

                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => {
                          setSelectedObligation(item);
                          setTargetStatus(item.status);
                          setIsStatusModalOpen(true);
                        }}
                        className="px-2 py-0.5 bg-slate-200 hover:bg-slate-300 text-slate-700 rounded text-[10px] font-bold"
                      >
                        Status
                      </button>
                      <button
                        onClick={() => {
                          setSelectedObligation(item);
                          setTargetAssigneeId(item.assignedEmployeeId || '');
                          setIsAssignModalOpen(true);
                        }}
                        className="px-2 py-0.5 bg-brand-50 hover:bg-brand-100 text-brand-700 rounded text-[10px] font-bold"
                      >
                        Assign
                      </button>
                    </div>
                  </div>
                </div>
              ))}

              {filteredObligations.length === 0 && (
                <div className="p-8 text-center text-xs text-slate-400">
                  No active obligations found matching the criteria.
                </div>
              )}
            </div>
          </Card>

          {/* Interactive Monthly Grid (Right 2 Columns) */}
          <Card
            title="Monthly Compliance Grid"
            subtitle={`Statutory & Internal Deadlines for ${currentMonthYear}`}
            className="lg:col-span-2"
            noPadding
          >
            <div className="p-3 sm:p-4 grid grid-cols-7 gap-px bg-slate-200 text-center text-xs font-semibold">
              {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((d) => (
                <div key={d} className="bg-slate-50 py-1.5 sm:py-2 text-slate-500 font-bold uppercase text-[9px] sm:text-[11px]">
                  {d}
                </div>
              ))}
              {Array.from({ length: 35 }).map((_, i) => {
                const dayNum = i - 5; // Offset for calendar month
                const isCurrentMonth = dayNum >= 1 && dayNum <= 31;
                const dateStr = isCurrentMonth
                  ? `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}-${String(dayNum).padStart(2, '0')}`
                  : null;

                const dayObligations = isCurrentMonth && dateStr
                  ? obligations.filter((o) => o.statutoryDueDate === dateStr || o.internalTargetDate === dateStr)
                  : [];

                const preset = statutoryPresets.find((p) => p.day === dayNum);

                return (
                  <div
                    key={i}
                    className={clsx(
                      'bg-white min-h-[60px] sm:min-h-[105px] p-1.5 sm:p-2 text-left flex flex-col justify-between transition-colors hover:bg-slate-50',
                      !isCurrentMonth && 'bg-slate-50/40 text-slate-300'
                    )}
                  >
                    <div className="flex items-center justify-between">
                      <span className={clsx('text-[10px] sm:text-xs font-bold', preset ? 'text-brand-600' : 'text-slate-700')}>
                        {isCurrentMonth ? dayNum : ''}
                      </span>
                      {dayObligations.length > 0 && (
                        <span className="px-1.5 py-0.2 rounded-full bg-brand-600 text-white font-black text-[9px]">
                          {dayObligations.length}
                        </span>
                      )}
                    </div>

                    {/* Presets & Obligations Tags */}
                    <div className="space-y-1 my-1">
                      {preset && (
                        <div className="hidden sm:block p-1 rounded bg-amber-50 border border-amber-200 text-[9px] font-bold text-amber-900 truncate" title={preset.desc}>
                          📌 {preset.title}
                        </div>
                      )}

                      {dayObligations.slice(0, 2).map((ob) => (
                        <div
                          key={ob.id}
                          className={clsx(
                            'hidden sm:block p-1 rounded text-[9px] font-bold truncate border',
                            ob.status === 'COMPLETED' || ob.status === 'FILED' ? 'bg-emerald-50 border-emerald-200 text-emerald-800' :
                            ob.status === 'OVERDUE' ? 'bg-rose-50 border-rose-200 text-rose-800' :
                            'bg-blue-50 border-blue-200 text-blue-800'
                          )}
                          title={`${ob.title} - ${ob.clientDisplayName}`}
                        >
                          {ob.title}
                        </div>
                      ))}
                      {dayObligations.length > 2 && (
                        <span className="hidden sm:block text-[9px] font-bold text-slate-400">
                          +{dayObligations.length - 2} more
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </Card>
        </div>
      ) : (
        /* List View Mode */
        <Card title="All Compliance Obligations" subtitle="Detailed statutory tracking & status matrix">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-[10px] font-black uppercase text-slate-500">
                  <th className="py-2.5 px-3">Domain</th>
                  <th className="py-2.5 px-3">Title & Period</th>
                  <th className="py-2.5 px-3">Client</th>
                  <th className="py-2.5 px-3">Statutory Due</th>
                  <th className="py-2.5 px-3">Internal Target</th>
                  <th className="py-2.5 px-3">Status</th>
                  <th className="py-2.5 px-3">Assignee</th>
                  <th className="py-2.5 px-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredObligations.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-3">
                      <span className="text-[10px] font-bold px-1.5 py-0.5 rounded uppercase bg-indigo-100 text-indigo-800 font-mono">
                        {item.obligationType}
                      </span>
                    </td>
                    <td className="py-3 px-3">
                      <div className="font-bold text-slate-900">{item.title}</div>
                      <div className="text-[10px] text-slate-400 font-medium">Period: {item.periodLabel || '—'}</div>
                    </td>
                    <td className="py-3 px-3">
                      <div className="font-semibold text-slate-800">{item.clientDisplayName || 'Client'}</div>
                      {item.clientPan && <span className="font-mono text-[10px] text-slate-400">PAN: {item.clientPan}</span>}
                    </td>
                    <td className="py-3 px-3 font-mono font-bold text-rose-700">
                      {item.statutoryDueDate}
                    </td>
                    <td className="py-3 px-3 font-mono font-bold text-blue-700">
                      {item.internalTargetDate || '—'}
                    </td>
                    <td className="py-3 px-3">
                      <span className={clsx(
                        'text-[10px] font-bold px-2 py-0.5 rounded uppercase',
                        item.status === 'COMPLETED' || item.status === 'FILED' ? 'bg-emerald-100 text-emerald-800' :
                        item.status === 'OVERDUE' ? 'bg-rose-100 text-rose-800' :
                        item.status === 'WAITING_FOR_CLIENT' ? 'bg-purple-100 text-purple-800' :
                        item.status === 'READY_FOR_FILING' ? 'bg-indigo-100 text-indigo-800' :
                        'bg-amber-100 text-amber-800'
                      )}>
                        {item.status.replace(/_/g, ' ')}
                      </span>
                    </td>
                    <td className="py-3 px-3 text-slate-600 font-medium">
                      {item.assignedEmployeeName || 'Unassigned'}
                    </td>
                    <td className="py-3 px-3 text-right space-x-2">
                      <button
                        onClick={() => {
                          setSelectedObligation(item);
                          setTargetStatus(item.status);
                          setIsStatusModalOpen(true);
                        }}
                        className="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded text-[11px] font-bold"
                      >
                        Status
                      </button>
                      <button
                        onClick={() => {
                          setSelectedObligation(item);
                          setTargetAssigneeId(item.assignedEmployeeId || '');
                          setIsAssignModalOpen(true);
                        }}
                        className="px-2 py-1 bg-brand-50 hover:bg-brand-100 text-brand-700 rounded text-[11px] font-bold"
                      >
                        Assign
                      </button>
                    </td>
                  </tr>
                ))}

                {filteredObligations.length === 0 && (
                  <tr>
                    <td colSpan={8} className="py-8 text-center text-xs text-slate-400">
                      No obligations found matching current filters.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Modal: Create Obligation */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Create Statutory Compliance Obligation"
      >
        <form onSubmit={handleCreateObligation} className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">Select Client *</label>
            <select
              value={formClientId}
              onChange={(e) => setFormClientId(e.target.value)}
              required
              className="w-full text-xs p-2 border border-slate-200 rounded-lg bg-white"
            >
              <option value="">-- Choose Client --</option>
              {clients.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.displayName || c.legalName} ({c.pan || 'No PAN'})
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Domain Type *</label>
              <select
                value={formType}
                onChange={(e) => setFormType(e.target.value as ComplianceObligationType)}
                className="w-full text-xs p-2 border border-slate-200 rounded-lg bg-white"
              >
                <option value="GST_RETURN">GST Return</option>
                <option value="ITR_FILING">ITR Filing</option>
                <option value="TDS_RETURN">TDS Return</option>
                <option value="TAX_AUDIT">Tax Audit</option>
                <option value="ADVANCE_TAX">Advance Tax</option>
                <option value="ROC_ANNUAL_FILING">ROC / MCA Annual</option>
                <option value="TAX_NOTICE_RESPONSE">Tax Notice Response</option>
                <option value="CUSTOM">Custom Obligation</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Period Label</label>
              <input
                type="text"
                placeholder="e.g. July 2026 or AY 2026-27"
                value={formPeriodLabel}
                onChange={(e) => setFormPeriodLabel(e.target.value)}
                className="w-full text-xs p-2 border border-slate-200 rounded-lg bg-white"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">Title *</label>
            <input
              type="text"
              placeholder="e.g. GSTR-3B Monthly Return Filing"
              value={formTitle}
              onChange={(e) => setFormTitle(e.target.value)}
              required
              className="w-full text-xs p-2 border border-slate-200 rounded-lg bg-white"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Statutory Due Date *</label>
              <input
                type="date"
                value={formStatutoryDueDate}
                onChange={(e) => {
                  setFormStatutoryDueDate(e.target.value);
                  if (e.target.value && !formInternalTargetDate) {
                    const due = new Date(e.target.value);
                    due.setDate(due.getDate() - 3);
                    setFormInternalTargetDate(due.toISOString().split('T')[0]);
                  }
                }}
                required
                className="w-full text-xs p-2 border border-slate-200 rounded-lg bg-white font-mono"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Internal Target Date</label>
              <input
                type="date"
                value={formInternalTargetDate}
                onChange={(e) => setFormInternalTargetDate(e.target.value)}
                className="w-full text-xs p-2 border border-slate-200 rounded-lg bg-white font-mono"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Priority</label>
              <select
                value={formPriority}
                onChange={(e) => setFormPriority(e.target.value as any)}
                className="w-full text-xs p-2 border border-slate-200 rounded-lg bg-white"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Assigned Employee</label>
              <select
                value={formAssignedEmployeeId}
                onChange={(e) => setFormAssignedEmployeeId(e.target.value)}
                className="w-full text-xs p-2 border border-slate-200 rounded-lg bg-white"
              >
                <option value="">-- Unassigned --</option>
                {employees.map((emp) => (
                  <option key={emp.id} value={emp.id}>
                    {emp.fullName || `${emp.firstName} ${emp.lastName || ''}`} ({emp.designation})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">Remarks / Internal Notes</label>
            <textarea
              rows={2}
              value={formRemarks}
              onChange={(e) => setFormRemarks(e.target.value)}
              className="w-full text-xs p-2 border border-slate-200 rounded-lg bg-white"
              placeholder="Optional notes for the execution team..."
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="outline" onClick={() => setIsCreateModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Creating...' : 'Create Obligation'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal: Update Status */}
      <Modal
        isOpen={isStatusModalOpen}
        onClose={() => setIsStatusModalOpen(false)}
        title="Update Obligation Status"
      >
        <form onSubmit={handleUpdateStatus} className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">Obligation Title</label>
            <p className="text-xs font-semibold text-slate-900 bg-slate-50 p-2 rounded border border-slate-200">
              {selectedObligation?.title}
            </p>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">New Status *</label>
            <select
              value={targetStatus}
              onChange={(e) => setTargetStatus(e.target.value as ComplianceObligationStatus)}
              className="w-full text-xs p-2 border border-slate-200 rounded-lg bg-white font-bold"
            >
              <option value="UPCOMING">Upcoming</option>
              <option value="WAITING_FOR_CLIENT">Waiting for Client</option>
              <option value="READY_FOR_FILING">Ready for Filing</option>
              <option value="FILED">Filed</option>
              <option value="COMPLETED">Completed</option>
              <option value="WAIVED">Waived</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>

          {(targetStatus === 'FILED' || targetStatus === 'COMPLETED') && (
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Acknowledgement / ARN Number</label>
              <input
                type="text"
                placeholder="e.g. AA2707260012345"
                value={statusAckNumber}
                onChange={(e) => setStatusAckNumber(e.target.value)}
                className="w-full text-xs p-2 border border-slate-200 rounded-lg bg-white font-mono"
              />
            </div>
          )}

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">Resolution Remarks</label>
            <textarea
              rows={2}
              value={statusRemarks}
              onChange={(e) => setStatusRemarks(e.target.value)}
              className="w-full text-xs p-2 border border-slate-200 rounded-lg bg-white"
              placeholder="Optional notes regarding filing or completion..."
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="outline" onClick={() => setIsStatusModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Updating...' : 'Save Status'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal: Assign Obligation */}
      <Modal
        isOpen={isAssignModalOpen}
        onClose={() => setIsAssignModalOpen(false)}
        title="Assign Compliance Obligation"
      >
        <form onSubmit={handleAssign} className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">Obligation Title</label>
            <p className="text-xs font-semibold text-slate-900 bg-slate-50 p-2 rounded border border-slate-200">
              {selectedObligation?.title}
            </p>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">Assign To Employee</label>
            <select
              value={targetAssigneeId}
              onChange={(e) => setTargetAssigneeId(e.target.value)}
              className="w-full text-xs p-2 border border-slate-200 rounded-lg bg-white"
            >
              <option value="">-- Unassigned --</option>
              {employees.map((emp) => (
                <option key={emp.id} value={emp.id}>
                  {emp.fullName || `${emp.firstName} ${emp.lastName || ''}`} ({emp.designation})
                </option>
              ))}
            </select>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="outline" onClick={() => setIsAssignModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Saving...' : 'Confirm Assignment'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
