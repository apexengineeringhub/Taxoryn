import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  AlertTriangle,
  Clock,
  Building,
  User,
  Zap,
  ExternalLink,
  Filter,
  CheckCircle,
  XCircle,
  Layers,
  ArrowRight,
  List,
  Grid,
  Search,
  RefreshCw,
  Info,
  CalendarDays,
  ShieldCheck,
  Flame,
  FileText,
  HelpCircle,
  Compass,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { complianceCalendarApi } from '../api/endpoints';
import {
  ComplianceDeadlineDto,
  ComplianceDeadlineSummaryDto,
  ComplianceDeadlineRadarDto,
  ComplianceRuleDomain,
  DeadlineStatus,
  ComplianceCalendarQueryFilter,
} from '../types';
import clsx from 'clsx';

export const ComplianceCalendarPage: React.FC = () => {
  // Reference date defaults to today (ISO YYYY-MM-DD)
  const todayStr = useMemo(() => new Date().toISOString().split('T')[0], []);
  const [referenceDate, setReferenceDate] = useState<string>(todayStr);

  // View Mode: RADAR (Agenda cards), GRID (Monthly calendar), TABLE (Compact matrix)
  const [viewMode, setViewMode] = useState<'RADAR' | 'GRID' | 'TABLE'>('RADAR');

  // Query & Filter State
  const [selectedDomain, setSelectedDomain] = useState<string>('ALL');
  const [selectedStatusTab, setSelectedStatusTab] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [currentMonthDate, setCurrentMonthDate] = useState<Date>(() => new Date());

  // Data State
  const [deadlines, setDeadlines] = useState<ComplianceDeadlineDto[]>([]);
  const [radar, setRadar] = useState<ComplianceDeadlineRadarDto | null>(null);
  const [summary, setSummary] = useState<ComplianceDeadlineSummaryDto | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadCalendarData();
  }, [referenceDate, selectedDomain, selectedStatusTab]);

  const loadCalendarData = async () => {
    try {
      setIsLoading(true);
      setError(null);

      const filter: ComplianceCalendarQueryFilter = {
        referenceDate: referenceDate || undefined,
        size: 200,
      };

      if (selectedDomain !== 'ALL') {
        filter.domain = selectedDomain as ComplianceRuleDomain;
      }
      if (selectedStatusTab !== 'ALL') {
        filter.status = selectedStatusTab as DeadlineStatus;
      }

      const [radarRes, calendarRes, summaryRes] = await Promise.allSettled([
        complianceCalendarApi.getRadar(referenceDate),
        complianceCalendarApi.getCalendar(filter),
        complianceCalendarApi.getSummary(referenceDate),
      ]);

      if (radarRes.status === 'fulfilled' && radarRes.value) {
        setRadar(radarRes.value);
      }
      if (summaryRes.status === 'fulfilled' && summaryRes.value) {
        setSummary(summaryRes.value);
      } else if (radarRes.status === 'fulfilled' && radarRes.value?.summary) {
        setSummary(radarRes.value.summary);
      }
      if (calendarRes.status === 'fulfilled' && calendarRes.value) {
        setDeadlines(calendarRes.value);
      } else {
        setDeadlines([]);
      }
    } catch (err: any) {
      console.error('Failed to load compliance calendar radar data', err);
      setError(err.response?.data?.message || 'Failed to load compliance deadlines.');
    } finally {
      setIsLoading(false);
    }
  };

  // Steppers for reference date
  const adjustReferenceDate = (days: number) => {
    const current = new Date(referenceDate);
    current.setDate(current.getDate() + days);
    setReferenceDate(current.toISOString().split('T')[0]);
  };

  const handleSetToday = () => {
    setReferenceDate(todayStr);
    setCurrentMonthDate(new Date());
  };

  // Month navigation for Grid view
  const handlePrevMonth = () => {
    setCurrentMonthDate(new Date(currentMonthDate.getFullYear(), currentMonthDate.getMonth() - 1, 1));
  };
  const handleNextMonth = () => {
    setCurrentMonthDate(new Date(currentMonthDate.getFullYear(), currentMonthDate.getMonth() + 1, 1));
  };

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];
  const currentMonthYear = `${monthNames[currentMonthDate.getMonth()]} ${currentMonthDate.getFullYear()}`;

  // Filtered Deadlines
  const filteredDeadlines = useMemo(() => {
    if (!searchQuery.trim()) return deadlines;
    const q = searchQuery.toLowerCase();
    return deadlines.filter((d) => {
      return (
        d.ruleName.toLowerCase().includes(q) ||
        d.ruleCode.toLowerCase().includes(q) ||
        (d.clientDisplayName && d.clientDisplayName.toLowerCase().includes(q)) ||
        (d.clientPan && d.clientPan.toLowerCase().includes(q)) ||
        (d.periodLabel && d.periodLabel.toLowerCase().includes(q)) ||
        (d.explanation && d.explanation.toLowerCase().includes(q)) ||
        (d.statutoryAct && d.statutoryAct.toLowerCase().includes(q)) ||
        (d.statutoryFormCode && d.statutoryFormCode.toLowerCase().includes(q))
      );
    });
  }, [deadlines, searchQuery]);

  // Helpers for Status Pill styling
  const getStatusBadge = (status: DeadlineStatus, daysRemaining?: number, daysOverdue?: number) => {
    switch (status) {
      case 'OVERDUE':
        return (
          <span className="inline-flex items-center gap-1 text-[10px] font-black px-2 py-0.5 rounded-md uppercase bg-rose-100 text-rose-800 border border-rose-200">
            <Flame className="w-3 h-3 text-rose-600" />
            {daysOverdue !== undefined ? `${daysOverdue}d Overdue` : 'Overdue'}
          </span>
        );
      case 'DUE_TODAY':
        return (
          <span className="inline-flex items-center gap-1 text-[10px] font-black px-2 py-0.5 rounded-md uppercase bg-amber-100 text-amber-900 border border-amber-300 animate-pulse">
            <Clock className="w-3 h-3 text-amber-700" />
            Due Today
          </span>
        );
      case 'DUE_TOMORROW':
        return (
          <span className="inline-flex items-center gap-1 text-[10px] font-black px-2 py-0.5 rounded-md uppercase bg-orange-100 text-orange-900 border border-orange-200">
            <Clock className="w-3 h-3 text-orange-700" />
            Due Tomorrow
          </span>
        );
      case 'DUE_WITHIN_3_DAYS':
        return (
          <span className="inline-flex items-center gap-1 text-[10px] font-black px-2 py-0.5 rounded-md uppercase bg-yellow-100 text-yellow-900 border border-yellow-200">
            <Zap className="w-3 h-3 text-yellow-700" />
            {daysRemaining !== undefined ? `In ${daysRemaining} Days` : 'Within 3 Days'}
          </span>
        );
      case 'DUE_THIS_WEEK':
        return (
          <span className="inline-flex items-center gap-1 text-[10px] font-black px-2 py-0.5 rounded-md uppercase bg-blue-100 text-blue-800 border border-blue-200">
            <CalendarDays className="w-3 h-3 text-blue-600" />
            This Week
          </span>
        );
      case 'UPCOMING':
        return (
          <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-md uppercase bg-slate-100 text-slate-700 border border-slate-200">
            <Clock className="w-3 h-3 text-slate-500" />
            {daysRemaining !== undefined ? `In ${daysRemaining}d` : 'Upcoming'}
          </span>
        );
      case 'NO_DUE_DATE':
        return (
          <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-md uppercase bg-slate-100 text-slate-500 border border-slate-200">
            <HelpCircle className="w-3 h-3 text-slate-400" />
            No Due Date
          </span>
        );
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-md uppercase bg-emerald-100 text-emerald-800 border border-emerald-200">
            <CheckCircle className="w-3 h-3 text-emerald-600" />
            Completed
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-md uppercase bg-slate-100 text-slate-400 line-through border border-slate-200">
            <XCircle className="w-3 h-3 text-slate-400" />
            Cancelled
          </span>
        );
      default:
        return null;
    }
  };

  const getPriorityBadge = (priority: string) => {
    switch (priority) {
      case 'URGENT':
        return <span className="text-[9px] font-black px-1.5 py-0.2 rounded bg-rose-600 text-white uppercase tracking-wider">URGENT</span>;
      case 'HIGH':
        return <span className="text-[9px] font-black px-1.5 py-0.2 rounded bg-amber-500 text-white uppercase tracking-wider">HIGH</span>;
      case 'MEDIUM':
        return <span className="text-[9px] font-semibold px-1.5 py-0.2 rounded bg-slate-200 text-slate-700 uppercase tracking-wider">MED</span>;
      case 'LOW':
        return <span className="text-[9px] font-medium px-1.5 py-0.2 rounded bg-slate-100 text-slate-500 uppercase tracking-wider">LOW</span>;
      default:
        return null;
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900 flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-brand-50 text-brand-600">
              <CalendarIcon className="w-6 h-6" />
            </div>
            <span>Compliance Calendar & Deadline Radar</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Authoritative statutory compliance timeline, deadline radar prioritization, and client obligations overview.
          </p>
        </div>

        <div className="flex items-center flex-wrap gap-2.5">
          {/* Reference Date Stepper */}
          <div className="flex items-center gap-1.5 bg-white border border-slate-200 rounded-xl p-1 shadow-2xs">
            <button
              onClick={() => adjustReferenceDate(-1)}
              className="p-1 hover:bg-slate-100 rounded text-slate-600 transition-colors"
              title="Previous Day"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <input
              type="date"
              value={referenceDate}
              onChange={(e) => setReferenceDate(e.target.value)}
              className="text-xs font-mono font-bold text-slate-800 bg-transparent border-0 focus:outline-none px-1"
            />
            <button
              onClick={() => adjustReferenceDate(1)}
              className="p-1 hover:bg-slate-100 rounded text-slate-600 transition-colors"
              title="Next Day"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
            {referenceDate !== todayStr && (
              <button
                onClick={handleSetToday}
                className="ml-1 px-2 py-0.5 text-[10px] font-bold bg-brand-50 text-brand-700 hover:bg-brand-100 rounded-md transition-colors"
              >
                Today
              </button>
            )}
          </div>

          {/* Refresh Button */}
          <Button
            variant="outline"
            size="sm"
            onClick={loadCalendarData}
            disabled={isLoading}
            leftIcon={<RefreshCw className={clsx('w-3.5 h-3.5', isLoading && 'animate-spin')} />}
          >
            Refresh
          </Button>

          {/* View Mode Toggle */}
          <div className="flex items-center bg-slate-100 p-1 rounded-xl border border-slate-200">
            <button
              onClick={() => setViewMode('RADAR')}
              className={clsx(
                'px-2.5 py-1 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5',
                viewMode === 'RADAR' ? 'bg-white shadow-2xs text-brand-600' : 'text-slate-600 hover:text-slate-900'
              )}
              title="Deadline Radar View"
            >
              <Compass className="w-3.5 h-3.5" />
              <span>Radar</span>
            </button>
            <button
              onClick={() => setViewMode('GRID')}
              className={clsx(
                'px-2.5 py-1 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5',
                viewMode === 'GRID' ? 'bg-white shadow-2xs text-brand-600' : 'text-slate-600 hover:text-slate-900'
              )}
              title="Monthly Calendar Grid"
            >
              <Grid className="w-3.5 h-3.5" />
              <span>Calendar</span>
            </button>
            <button
              onClick={() => setViewMode('TABLE')}
              className={clsx(
                'px-2.5 py-1 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5',
                viewMode === 'TABLE' ? 'bg-white shadow-2xs text-brand-600' : 'text-slate-600 hover:text-slate-900'
              )}
              title="Matrix Table View"
            >
              <List className="w-3.5 h-3.5" />
              <span>Table</span>
            </button>
          </div>
        </div>
      </div>

      {/* Deadline Radar KPI Metric Buckets */}
      {summary && (
        <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-2.5">
          {/* Overdue */}
          <div
            onClick={() => setSelectedStatusTab(selectedStatusTab === 'OVERDUE' ? 'ALL' : 'OVERDUE')}
            className={clsx(
              'p-3 rounded-2xl border transition-all cursor-pointer shadow-2xs',
              selectedStatusTab === 'OVERDUE'
                ? 'bg-rose-100 border-rose-400 ring-2 ring-rose-400'
                : 'bg-rose-50/40 border-rose-200 hover:bg-rose-50'
            )}
          >
            <span className="text-[10px] font-black text-rose-700 uppercase block tracking-wider">🚨 Overdue</span>
            <p className="text-xl font-black text-rose-700 mt-1">{summary.overdueCount}</p>
          </div>

          {/* Due Today */}
          <div
            onClick={() => setSelectedStatusTab(selectedStatusTab === 'DUE_TODAY' ? 'ALL' : 'DUE_TODAY')}
            className={clsx(
              'p-3 rounded-2xl border transition-all cursor-pointer shadow-2xs',
              selectedStatusTab === 'DUE_TODAY'
                ? 'bg-amber-100 border-amber-400 ring-2 ring-amber-400'
                : 'bg-amber-50/40 border-amber-200 hover:bg-amber-50'
            )}
          >
            <span className="text-[10px] font-black text-amber-700 uppercase block tracking-wider">📅 Today</span>
            <p className="text-xl font-black text-amber-700 mt-1">{summary.dueTodayCount}</p>
          </div>

          {/* Due Tomorrow */}
          <div
            onClick={() => setSelectedStatusTab(selectedStatusTab === 'DUE_TOMORROW' ? 'ALL' : 'DUE_TOMORROW')}
            className={clsx(
              'p-3 rounded-2xl border transition-all cursor-pointer shadow-2xs',
              selectedStatusTab === 'DUE_TOMORROW'
                ? 'bg-orange-100 border-orange-400 ring-2 ring-orange-400'
                : 'bg-orange-50/40 border-orange-200 hover:bg-orange-50'
            )}
          >
            <span className="text-[10px] font-black text-orange-700 uppercase block tracking-wider">⏰ Tomorrow</span>
            <p className="text-xl font-black text-orange-700 mt-1">{summary.dueTomorrowCount}</p>
          </div>

          {/* Within 3 Days */}
          <div
            onClick={() => setSelectedStatusTab(selectedStatusTab === 'DUE_WITHIN_3_DAYS' ? 'ALL' : 'DUE_WITHIN_3_DAYS')}
            className={clsx(
              'p-3 rounded-2xl border transition-all cursor-pointer shadow-2xs',
              selectedStatusTab === 'DUE_WITHIN_3_DAYS'
                ? 'bg-yellow-100 border-yellow-400 ring-2 ring-yellow-400'
                : 'bg-yellow-50/40 border-yellow-200 hover:bg-yellow-50'
            )}
          >
            <span className="text-[10px] font-black text-yellow-800 uppercase block tracking-wider">⚡ 3 Days</span>
            <p className="text-xl font-black text-yellow-800 mt-1">{summary.dueWithin3DaysCount}</p>
          </div>

          {/* This Week */}
          <div
            onClick={() => setSelectedStatusTab(selectedStatusTab === 'DUE_THIS_WEEK' ? 'ALL' : 'DUE_THIS_WEEK')}
            className={clsx(
              'p-3 rounded-2xl border transition-all cursor-pointer shadow-2xs',
              selectedStatusTab === 'DUE_THIS_WEEK'
                ? 'bg-blue-100 border-blue-400 ring-2 ring-blue-400'
                : 'bg-blue-50/40 border-blue-200 hover:bg-blue-50'
            )}
          >
            <span className="text-[10px] font-black text-blue-700 uppercase block tracking-wider">🗓️ This Week</span>
            <p className="text-xl font-black text-blue-700 mt-1">{summary.dueThisWeekCount}</p>
          </div>

          {/* Upcoming */}
          <div
            onClick={() => setSelectedStatusTab(selectedStatusTab === 'UPCOMING' ? 'ALL' : 'UPCOMING')}
            className={clsx(
              'p-3 rounded-2xl border transition-all cursor-pointer shadow-2xs',
              selectedStatusTab === 'UPCOMING'
                ? 'bg-indigo-100 border-indigo-400 ring-2 ring-indigo-400'
                : 'bg-indigo-50/40 border-indigo-200 hover:bg-indigo-50'
            )}
          >
            <span className="text-[10px] font-black text-indigo-700 uppercase block tracking-wider">⏳ Upcoming</span>
            <p className="text-xl font-black text-indigo-700 mt-1">{summary.upcomingCount}</p>
          </div>

          {/* No Due Date */}
          <div
            onClick={() => setSelectedStatusTab(selectedStatusTab === 'NO_DUE_DATE' ? 'ALL' : 'NO_DUE_DATE')}
            className={clsx(
              'p-3 rounded-2xl border transition-all cursor-pointer shadow-2xs',
              selectedStatusTab === 'NO_DUE_DATE'
                ? 'bg-slate-200 border-slate-400 ring-2 ring-slate-400'
                : 'bg-slate-50 border-slate-200 hover:bg-slate-100'
            )}
          >
            <span className="text-[10px] font-bold text-slate-500 uppercase block tracking-wider">❓ No Due Date</span>
            <p className="text-xl font-black text-slate-700 mt-1">{summary.noDueDateCount}</p>
          </div>

          {/* Completed */}
          <div
            onClick={() => setSelectedStatusTab(selectedStatusTab === 'COMPLETED' ? 'ALL' : 'COMPLETED')}
            className={clsx(
              'p-3 rounded-2xl border transition-all cursor-pointer shadow-2xs',
              selectedStatusTab === 'COMPLETED'
                ? 'bg-emerald-100 border-emerald-400 ring-2 ring-emerald-400'
                : 'bg-emerald-50/40 border-emerald-200 hover:bg-emerald-50'
            )}
          >
            <span className="text-[10px] font-black text-emerald-700 uppercase block tracking-wider">✅ Completed</span>
            <p className="text-xl font-black text-emerald-700 mt-1">{summary.completedCount}</p>
          </div>
        </div>
      )}

      {/* Filter Tabs & Search Bar */}
      <div className="space-y-3 bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs">
        {/* Status Filter Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 border-b border-slate-100">
          {[
            { key: 'ALL', label: 'All Active Deadlines' },
            { key: 'OVERDUE', label: 'Overdue' },
            { key: 'DUE_TODAY', label: 'Due Today' },
            { key: 'DUE_TOMORROW', label: 'Due Tomorrow' },
            { key: 'DUE_WITHIN_3_DAYS', label: 'Within 3 Days' },
            { key: 'DUE_THIS_WEEK', label: 'This Week' },
            { key: 'UPCOMING', label: 'Upcoming' },
            { key: 'NO_DUE_DATE', label: 'No Due Date' },
            { key: 'COMPLETED', label: 'Completed' },
          ].map((tab) => (
            <button
              key={tab.key}
              onClick={() => setSelectedStatusTab(tab.key)}
              className={clsx(
                'px-3 py-1.5 rounded-lg text-xs font-bold transition-all whitespace-nowrap',
                selectedStatusTab === tab.key
                  ? 'bg-brand-600 text-white shadow-xs'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Domain Filter Pills & Search */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-1">
          <div className="flex items-center gap-1.5 overflow-x-auto w-full sm:w-auto">
            <span className="text-xs font-bold text-slate-400 mr-1 flex items-center gap-1">
              <Filter className="w-3.5 h-3.5" /> Domain:
            </span>
            {[
              { key: 'ALL', label: 'All' },
              { key: 'GST', label: 'GST' },
              { key: 'TDS', label: 'TDS/TCS' },
              { key: 'INCOME_TAX', label: 'Income Tax' },
              { key: 'MCA_ROC', label: 'MCA / ROC' },
              { key: 'STATUTORY_AUDIT', label: 'Audit' },
              { key: 'PAYROLL_LABOUR', label: 'Payroll & Labour' },
              { key: 'OTHER', label: 'Other' },
            ].map((d) => (
              <button
                key={d.key}
                onClick={() => setSelectedDomain(d.key)}
                className={clsx(
                  'px-2.5 py-1 rounded-md text-[11px] font-bold transition-all whitespace-nowrap',
                  selectedDomain === d.key
                    ? 'bg-slate-900 text-white shadow-xs'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                )}
              >
                {d.label}
              </button>
            ))}
          </div>

          <div className="relative w-full sm:w-72">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
            <input
              type="text"
              placeholder="Search client, PAN, rule, or explanation..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 font-medium"
            />
          </div>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-2xl flex items-center justify-between text-xs text-rose-800">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span className="font-semibold">{error}</span>
          </div>
          <Button size="sm" variant="outline" onClick={loadCalendarData}>
            Retry
          </Button>
        </div>
      )}

      {/* Loading Skeleton */}
      {isLoading && (
        <div className="p-12 text-center bg-white border border-slate-200 rounded-2xl shadow-2xs space-y-3">
          <div className="w-8 h-8 border-3 border-brand-500 border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Evaluating Deadline Radar & Obligations...</p>
        </div>
      )}

      {/* 1. RADAR / PRIORITY AGENDA VIEW */}
      {!isLoading && viewMode === 'RADAR' && (
        <div className="space-y-3">
          {filteredDeadlines.length === 0 ? (
            <div className="p-12 text-center bg-white border border-slate-200 rounded-2xl shadow-2xs space-y-2">
              <ShieldCheck className="w-10 h-10 text-slate-300 mx-auto" />
              <p className="text-sm font-bold text-slate-700">No compliance deadlines found</p>
              <p className="text-xs text-slate-400">
                No statutory obligations match the selected reference date ({referenceDate}), domain, or status filter.
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {filteredDeadlines.map((item) => (
                <div
                  key={item.obligationId}
                  className={clsx(
                    'p-4 bg-white border rounded-2xl shadow-2xs space-y-3 flex flex-col justify-between hover:shadow-md transition-all',
                    item.status === 'OVERDUE'
                      ? 'border-rose-300 bg-rose-50/10'
                      : item.status === 'DUE_TODAY'
                      ? 'border-amber-300 bg-amber-50/10 ring-1 ring-amber-300'
                      : item.status === 'DUE_TOMORROW'
                      ? 'border-orange-300'
                      : 'border-slate-200'
                  )}
                >
                  <div className="space-y-2">
                    {/* Header: Domain & Status */}
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded uppercase bg-indigo-50 text-indigo-700 border border-indigo-200 font-mono">
                        {item.domainDisplayName || item.domain}
                      </span>
                      <div className="flex items-center gap-1.5">
                        {getPriorityBadge(item.priority)}
                        {getStatusBadge(item.status, item.daysRemaining, item.daysOverdue)}
                      </div>
                    </div>

                    {/* Rule Title & Code */}
                    <div>
                      <h4 className="font-bold text-xs text-slate-900 leading-snug">{item.ruleName}</h4>
                      <div className="flex items-center gap-2 mt-0.5 text-[10px] text-slate-400 font-mono">
                        <span>{item.ruleCode}</span>
                        {item.statutoryFormCode && (
                          <>
                            <span>•</span>
                            <span className="font-semibold text-slate-600">{item.statutoryFormCode}</span>
                          </>
                        )}
                        {item.periodLabel && (
                          <>
                            <span>•</span>
                            <span>{item.periodLabel}</span>
                          </>
                        )}
                      </div>
                    </div>

                    {/* Client Info */}
                    <div className="p-2 bg-slate-50 border border-slate-100 rounded-xl flex items-center justify-between text-xs">
                      <div>
                        <span className="text-[10px] text-slate-400 block">Client:</span>
                        <span className="font-bold text-slate-800">{item.clientDisplayName || 'Client'}</span>
                      </div>
                      {item.clientPan && (
                        <div className="text-right">
                          <span className="text-[10px] text-slate-400 block">PAN:</span>
                          <span className="font-mono text-[11px] font-semibold text-slate-600">{item.clientPan}</span>
                        </div>
                      )}
                    </div>

                    {/* Dual Due Date & Grace Days */}
                    <div className="grid grid-cols-2 gap-2 text-[11px] bg-slate-50/70 p-2.5 rounded-xl border border-slate-100">
                      <div>
                        <span className="text-[10px] font-semibold text-slate-400 block">Statutory Due:</span>
                        <span className="font-bold text-rose-700 font-mono">
                          {item.statutoryDueDate || 'Not Configured'}
                        </span>
                      </div>
                      <div>
                        <span className="text-[10px] font-semibold text-slate-400 block">Effective Due:</span>
                        <span className="font-bold text-slate-900 font-mono">
                          {item.dueDate || item.statutoryDueDate || '—'}
                        </span>
                      </div>
                    </div>

                    {/* Explanation */}
                    {item.explanation && (
                      <p className="text-[11px] text-slate-500 italic bg-slate-50/40 p-2 rounded-lg border border-slate-100/60 leading-relaxed">
                        ℹ️ {item.explanation}
                      </p>
                    )}
                  </div>

                  {/* Footer Action */}
                  <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
                    {item.statutoryAct && (
                      <span className="text-[10px] font-semibold text-slate-400">
                        {item.statutoryAct} {item.statutorySection ? `§${item.statutorySection}` : ''}
                      </span>
                    )}
                    <Link
                      to={`/clients/${item.clientId}?tab=compliance`}
                      className="text-xs font-bold text-brand-600 hover:text-brand-800 inline-flex items-center gap-1 ml-auto"
                    >
                      <span>Client 360</span>
                      <ChevronRight className="w-3.5 h-3.5" />
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* 2. CALENDAR MONTHLY GRID VIEW */}
      {!isLoading && viewMode === 'GRID' && (
        <Card
          title="Monthly Statutory Compliance Calendar"
          subtitle={`Statutory Deadlines for ${currentMonthYear}`}
          action={
            <div className="flex items-center gap-2">
              <button
                onClick={handlePrevMonth}
                className="p-1 hover:bg-slate-100 rounded text-slate-600 transition-colors"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <span className="text-xs font-bold text-slate-800 font-mono px-2">{currentMonthYear}</span>
              <button
                onClick={handleNextMonth}
                className="p-1 hover:bg-slate-100 rounded text-slate-600 transition-colors"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          }
          noPadding
        >
          <div className="p-3 sm:p-4 grid grid-cols-7 gap-px bg-slate-200 text-center text-xs font-semibold">
            {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((d) => (
              <div key={d} className="bg-slate-50 py-2 text-slate-500 font-bold uppercase text-[10px]">
                {d}
              </div>
            ))}
            {Array.from({ length: 35 }).map((_, i) => {
              const dayNum = i - 5;
              const isCurrentMonth = dayNum >= 1 && dayNum <= 31;
              const dateStr = isCurrentMonth
                ? `${currentMonthDate.getFullYear()}-${String(currentMonthDate.getMonth() + 1).padStart(2, '0')}-${String(dayNum).padStart(2, '0')}`
                : null;

              const dayDeadlines = isCurrentMonth && dateStr
                ? filteredDeadlines.filter((d) => d.statutoryDueDate === dateStr || d.dueDate === dateStr)
                : [];

              const isSelectedRefDate = dateStr === referenceDate;

              return (
                <div
                  key={i}
                  onClick={() => dateStr && setReferenceDate(dateStr)}
                  className={clsx(
                    'bg-white min-h-[95px] p-2 text-left flex flex-col justify-between transition-colors cursor-pointer hover:bg-brand-50/30',
                    !isCurrentMonth && 'bg-slate-50/40 text-slate-300',
                    isSelectedRefDate && 'ring-2 ring-brand-500 bg-brand-50/40'
                  )}
                >
                  <div className="flex items-center justify-between">
                    <span className={clsx('text-xs font-bold', isSelectedRefDate ? 'text-brand-600' : 'text-slate-700')}>
                      {isCurrentMonth ? dayNum : ''}
                    </span>
                    {dayDeadlines.length > 0 && (
                      <span className="px-1.5 py-0.2 rounded-full bg-brand-600 text-white font-black text-[9px]">
                        {dayDeadlines.length}
                      </span>
                    )}
                  </div>

                  <div className="space-y-1 my-1">
                    {dayDeadlines.slice(0, 2).map((d) => (
                      <div
                        key={d.obligationId}
                        className={clsx(
                          'p-1 rounded text-[9px] font-bold truncate border',
                          d.status === 'OVERDUE'
                            ? 'bg-rose-50 border-rose-200 text-rose-800'
                            : d.status === 'DUE_TODAY'
                            ? 'bg-amber-50 border-amber-200 text-amber-800'
                            : 'bg-indigo-50 border-indigo-200 text-indigo-800'
                        )}
                        title={`${d.ruleName} - ${d.clientDisplayName || 'Client'}`}
                      >
                        {d.ruleCode} ({d.clientDisplayName || 'Client'})
                      </div>
                    ))}
                    {dayDeadlines.length > 2 && (
                      <span className="text-[9px] font-bold text-slate-400">
                        +{dayDeadlines.length - 2} more
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </Card>
      )}

      {/* 3. TABLE MATRIX VIEW */}
      {!isLoading && viewMode === 'TABLE' && (
        <Card title="Compliance Deadlines Matrix" subtitle="Authoritative statutory tracking & deadline radar list">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-[10px] font-black uppercase text-slate-500">
                  <th className="py-3 px-3">Domain</th>
                  <th className="py-3 px-3">Rule & Period</th>
                  <th className="py-3 px-3">Client</th>
                  <th className="py-3 px-3">Statutory Due Date</th>
                  <th className="py-3 px-3">Effective Due Date</th>
                  <th className="py-3 px-3">Priority</th>
                  <th className="py-3 px-3">Deadline Status</th>
                  <th className="py-3 px-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredDeadlines.map((item) => (
                  <tr key={item.obligationId} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-3">
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded uppercase bg-indigo-50 text-indigo-700 border border-indigo-200 font-mono">
                        {item.domainDisplayName || item.domain}
                      </span>
                    </td>
                    <td className="py-3 px-3">
                      <div className="font-bold text-slate-900">{item.ruleName}</div>
                      <div className="text-[10px] text-slate-400 font-mono">{item.ruleCode} • Period: {item.periodLabel || item.periodKey}</div>
                    </td>
                    <td className="py-3 px-3">
                      <div className="font-semibold text-slate-800">{item.clientDisplayName || 'Client'}</div>
                      {item.clientPan && <span className="font-mono text-[10px] text-slate-400">PAN: {item.clientPan}</span>}
                    </td>
                    <td className="py-3 px-3 font-mono font-bold text-rose-700">
                      {item.statutoryDueDate || '—'}
                    </td>
                    <td className="py-3 px-3 font-mono font-bold text-slate-800">
                      {item.dueDate || item.statutoryDueDate || '—'}
                    </td>
                    <td className="py-3 px-3">
                      {getPriorityBadge(item.priority)}
                    </td>
                    <td className="py-3 px-3">
                      {getStatusBadge(item.status, item.daysRemaining, item.daysOverdue)}
                    </td>
                    <td className="py-3 px-3 text-right">
                      <Link
                        to={`/clients/${item.clientId}?tab=compliance`}
                        className="px-2 py-1 bg-brand-50 hover:bg-brand-100 text-brand-700 rounded text-[11px] font-bold inline-flex items-center gap-1"
                      >
                        <span>Client 360</span>
                        <ChevronRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>
                ))}

                {filteredDeadlines.length === 0 && (
                  <tr>
                    <td colSpan={8} className="py-8 text-center text-xs text-slate-400">
                      No compliance deadlines found matching current criteria.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
};
