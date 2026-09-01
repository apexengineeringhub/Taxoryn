import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  CheckCircle2,
  Clock,
  Flame,
  Building,
  User,
  Zap,
  ExternalLink,
  Plus,
  Filter,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { complianceApi } from '../api/endpoints';
import { ComplianceObligation, ComplianceDashboardStats } from '../types';
import clsx from 'clsx';

export const ComplianceCalendarPage: React.FC = () => {
  const [currentDate, setCurrentDate] = useState(new Date(2026, 7, 1)); // August 2026
  const [obligations, setObligations] = useState<ComplianceObligation[]>([]);
  const [stats, setStats] = useState<ComplianceDashboardStats | null>(null);
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [convertingId, setConvertingId] = useState<string | null>(null);

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  const currentMonthYear = `${monthNames[currentDate.getMonth()]} ${currentDate.getFullYear()}`;
  const periodStr = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}`;

  useEffect(() => {
    loadComplianceData();
  }, [currentDate, selectedType]);

  const loadComplianceData = async () => {
    try {
      setIsLoading(true);
      const params: any = { size: 100 };
      if (selectedType !== 'ALL') {
        params.complianceType = selectedType;
      }

      const [calRes, statsRes] = await Promise.allSettled([
        complianceApi.getCalendar(params),
        complianceApi.getDashboardStats(),
      ]);

      if (calRes.status === 'fulfilled' && calRes.value) {
        const list = Array.isArray(calRes.value) ? calRes.value : (calRes.value?.content || []);
        setObligations(list);
      } else {
        setObligations([]);
      }

      if (statsRes.status === 'fulfilled' && statsRes.value) {
        setStats(statsRes.value);
      }
    } catch (err) {
      console.error('Failed to load compliance calendar', err);
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

  const handleCreateTask = async (obligationId: string) => {
    try {
      setConvertingId(obligationId);
      await complianceApi.createTaskForObligation(obligationId);
      alert('Task generated and linked to statutory compliance obligation successfully!');
      await loadComplianceData();
    } catch (err: any) {
      alert(`Failed to create task: ${err.response?.data?.message || err.message}`);
    } finally {
      setConvertingId(null);
    }
  };

  // Fallback statutory deadlines for August 2026 calendar overlay
  const statutoryPresets = [
    { day: 7, title: 'TDS/TCS Payment', desc: 'Deposit of Tax Deducted at Source for July 2026', type: 'TDS', urgency: 'HIGH' },
    { day: 11, title: 'GSTR-1 Monthly', desc: 'Outward supplies statement for July 2026 (> 5 Cr or Monthly filers)', type: 'GST', urgency: 'CRITICAL' },
    { day: 13, title: 'GSTR-1 IFF (QRMP)', desc: 'Invoice Furnishing Facility for July 2026 QRMP filers', type: 'GST', urgency: 'MEDIUM' },
    { day: 15, title: 'Form 24G / TCS Cert', desc: 'Quarterly TCS certificate issuance for Q1', type: 'TDS', urgency: 'NORMAL' },
    { day: 20, title: 'GSTR-3B Monthly', desc: 'Summary return & tax payment for July 2026', type: 'GST', urgency: 'CRITICAL' },
    { day: 25, title: 'PMT-06 (QRMP)', desc: 'Challan payment of 35% tax under QRMP scheme for July', type: 'GST', urgency: 'HIGH' },
    { day: 31, title: 'ITR Audit Filing Phase 1', desc: 'Preparation of Tax Audit accounts & statements', type: 'ITR', urgency: 'NORMAL' },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">Tax Compliance Calendar</h1>
          <p className="text-xs text-slate-500 mt-1">
            Statutory tax calendar under Income Tax Act 1961, CGST/SGST Acts, and MCA deadlines linked to operational practice tasks.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Link to="/tasks">
            <Button variant="outline" leftIcon={<Zap className="w-4 h-4 text-amber-500" />}>
              Open Worklist
            </Button>
          </Link>

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
        </div>
      </div>

      {/* KPI Stats Bar */}
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-2xs">
            <span className="text-[11px] font-bold text-slate-500 uppercase block">🚨 Overdue Statutory</span>
            <p className="text-2xl font-black text-rose-600 mt-1">{stats.overdueCount}</p>
          </div>
          <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-2xs">
            <span className="text-[11px] font-bold text-slate-500 uppercase block">📅 Due Today</span>
            <p className="text-2xl font-black text-amber-600 mt-1">{stats.dueTodayCount}</p>
          </div>
          <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-2xs">
            <span className="text-[11px] font-bold text-slate-500 uppercase block">⏳ Due This Week</span>
            <p className="text-2xl font-black text-blue-600 mt-1">{stats.dueThisWeekCount}</p>
          </div>
          <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-2xs">
            <span className="text-[11px] font-bold text-slate-500 uppercase block">✅ Completed Filings</span>
            <p className="text-2xl font-black text-emerald-600 mt-1">{stats.completedCount}</p>
          </div>
        </div>
      )}

      {/* Domain Category Filter */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1 border-b border-slate-200">
        <span className="text-xs font-semibold text-slate-500 mr-2 flex items-center gap-1">
          <Filter className="w-3.5 h-3.5" /> Domain:
        </span>
        {['ALL', 'GST', 'ITR', 'TDS', 'ROC_MCA', 'ADVANCE_TAX', 'AUDIT'].map((cat) => (
          <button
            key={cat}
            onClick={() => setSelectedType(cat)}
            className={clsx(
              'px-3 py-1 rounded-md text-xs font-bold transition-colors whitespace-nowrap',
              selectedType === cat
                ? 'bg-slate-900 text-white shadow-xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            )}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* Statutory Deadlines List & Calendar Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Active Statutory Obligations */}
        <Card
          title="Active Statutory Obligations"
          subtitle={`Client obligations for ${currentMonthYear}`}
          className="lg:col-span-1"
        >
          <div className="space-y-3 max-h-[600px] overflow-y-auto">
            {obligations.map((item) => (
              <div key={item.id} className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <span className="text-[10px] font-bold px-1.5 py-0.5 rounded uppercase bg-indigo-100 text-indigo-800 font-mono">
                      {item.complianceType}
                    </span>
                    <h4 className="font-bold text-xs text-slate-900 mt-1">{item.title}</h4>
                  </div>
                  <span className={clsx(
                    'text-[10px] font-bold px-1.5 py-0.5 rounded uppercase',
                    item.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-800' :
                    item.status === 'OVERDUE' ? 'bg-rose-100 text-rose-800' : 'bg-amber-100 text-amber-800'
                  )}>
                    {item.status}
                  </span>
                </div>

                <div className="flex items-center justify-between text-[11px] text-slate-600 pt-1 border-t border-slate-200/60">
                  <span className="font-semibold">{item.clientName || 'General Client'}</span>
                  <span className="font-mono text-slate-700">Due: {item.dueDate}</span>
                </div>

                {/* Linked Task Action */}
                <div className="pt-2 flex items-center justify-between">
                  {item.taskId ? (
                    <span className="text-[11px] text-emerald-700 font-bold inline-flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5" /> Task Linked
                    </span>
                  ) : (
                    <button
                      onClick={() => handleCreateTask(item.id)}
                      disabled={convertingId === item.id}
                      className="px-2.5 py-1 bg-brand-600 hover:bg-brand-700 text-white rounded text-[11px] font-bold inline-flex items-center gap-1 shadow-2xs"
                    >
                      <Zap className="w-3 h-3" /> Convert to Task
                    </button>
                  )}
                  <span className="text-[10px] text-slate-400 font-medium">Period: {item.period}</span>
                </div>
              </div>
            ))}

            {obligations.length === 0 && (
              <div className="p-6 text-center text-xs text-slate-400">
                No active obligations found for this selection.
              </div>
            )}
          </div>
        </Card>

        {/* Month View Grid */}
        <Card
          title="Interactive Monthly Compliance Grid"
          subtitle="Statutory calendar dates & client obligations"
          className="lg:col-span-2"
          noPadding
        >
          {/* Compact 7-column grid: cell height/padding/labels scale down on mobile
              instead of squeezing full desktop cell content into ~45px-wide columns. */}
          <div className="p-2 sm:p-4 grid grid-cols-7 gap-px bg-slate-200 text-center text-xs font-semibold">
            {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((d) => (
              <div key={d} className="bg-slate-50 py-1.5 sm:py-2 text-slate-500 font-bold uppercase text-[8px] sm:text-[10px]">
                <span className="sm:hidden">{d.charAt(0)}</span>
                <span className="hidden sm:inline">{d}</span>
              </div>
            ))}
            {/* Days 1 to 31 */}
            {Array.from({ length: 35 }).map((_, i) => {
              const dayNum = i - 5; // offset for August 2026
              const isCurrentMonth = dayNum >= 1 && dayNum <= 31;
              const hasPreset = statutoryPresets.find((d) => d.day === dayNum);

              return (
                <div
                  key={i}
                  title={hasPreset ? `${hasPreset.title}: ${hasPreset.desc}` : undefined}
                  className={clsx(
                    'bg-white min-h-[44px] sm:min-h-[95px] p-1 sm:p-2 text-left flex flex-col justify-between transition-colors hover:bg-slate-50/80',
                    !isCurrentMonth && 'bg-slate-50/40 text-slate-300'
                  )}
                >
                  <span className={clsx('text-[10px] sm:text-xs font-bold', hasPreset ? 'text-brand-600' : 'text-slate-700')}>
                    {isCurrentMonth ? dayNum : ''}
                  </span>
                  {hasPreset && (
                    <>
                      {/* Mobile: dot indicator only, avoids clipped/illegible chip text at ~45px cell width */}
                      <span className="sm:hidden self-start w-1.5 h-1.5 rounded-full bg-amber-500" aria-label={hasPreset.title} />
                      <div className="hidden sm:block p-1 rounded bg-amber-50 border border-amber-200 text-[10px] font-bold text-amber-900 truncate">
                        {hasPreset.title}
                      </div>
                    </>
                  )}
                </div>
              );
            })}
          </div>
        </Card>
      </div>
    </div>
  );
};
