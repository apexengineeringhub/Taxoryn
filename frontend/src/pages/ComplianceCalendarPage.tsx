import React, { useState } from 'react';
import { Calendar as CalendarIcon, ChevronLeft, ChevronRight, AlertCircle, CheckCircle2, Clock } from 'lucide-react';
import { Card } from '../components/common/Card';
import clsx from 'clsx';

export const ComplianceCalendarPage: React.FC = () => {
  const [currentMonth, setCurrentMonth] = useState('August 2026');

  const statutoryDeadlines = [
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
            Statutory tax calendar under Income Tax Act 1961, CGST/SGST Acts, and MCA deadlines.
          </p>
        </div>

        {/* Month Selector */}
        <div className="flex items-center gap-2 bg-white border border-slate-200 rounded-lg p-1 shadow-2xs">
          <button className="p-1.5 hover:bg-slate-100 rounded text-slate-600">
            <ChevronLeft className="w-4 h-4" />
          </button>
          <span className="px-3 font-bold text-xs text-slate-800">{currentMonth}</span>
          <button className="p-1.5 hover:bg-slate-100 rounded text-slate-600">
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Statutory Deadlines List & Calendar Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Key Statutory Deadlines */}
        <Card
          title="Statutory Deadlines Schedule"
          subtitle="Mandatory dates for August 2026"
          className="lg:col-span-1"
        >
          <div className="space-y-3">
            {statutoryDeadlines.map((item, idx) => (
              <div key={idx} className="p-3 bg-slate-50 border border-slate-200/80 rounded-xl flex items-start gap-3">
                <div className="w-10 h-10 rounded-lg bg-white border border-slate-200 flex flex-col items-center justify-center shrink-0 shadow-2xs">
                  <span className="text-[9px] font-bold text-slate-400 uppercase">AUG</span>
                  <span className="text-sm font-black text-slate-900 leading-none">{item.day}</span>
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-xs text-slate-900 truncate">{item.title}</span>
                    <span
                      className={clsx(
                        'text-[9px] font-bold px-1.5 py-0.5 rounded uppercase',
                        item.urgency === 'CRITICAL' ? 'bg-rose-100 text-rose-700' : 'bg-blue-100 text-blue-700'
                      )}
                    >
                      {item.type}
                    </span>
                  </div>
                  <p className="text-[10px] text-slate-500 mt-1 leading-snug">{item.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </Card>

        {/* Month View Grid */}
        <Card
          title="Interactive Monthly Compliance View"
          subtitle="Client filings overlaid on calendar grid"
          className="lg:col-span-2"
          noPadding
        >
          <div className="p-4 grid grid-cols-7 gap-px bg-slate-200 text-center text-xs font-semibold">
            {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((d) => (
              <div key={d} className="bg-slate-50 py-2 text-slate-500 font-bold uppercase text-[10px]">
                {d}
              </div>
            ))}
            {/* Days 1 to 31 */}
            {Array.from({ length: 35 }).map((_, i) => {
              const dayNum = i - 5; // offset
              const isCurrentMonth = dayNum >= 1 && dayNum <= 31;
              const hasDeadline = statutoryDeadlines.find((d) => d.day === dayNum);

              return (
                <div
                  key={i}
                  className={clsx(
                    'bg-white min-h-[90px] p-2 text-left flex flex-col justify-between transition-colors hover:bg-slate-50/80',
                    !isCurrentMonth && 'bg-slate-50/40 text-slate-300'
                  )}
                >
                  <span className={clsx('text-xs font-bold', hasDeadline ? 'text-brand-600' : 'text-slate-700')}>
                    {isCurrentMonth ? dayNum : ''}
                  </span>
                  {hasDeadline && (
                    <div className="p-1 rounded bg-amber-50 border border-amber-200 text-[10px] font-bold text-amber-800 truncate">
                      {hasDeadline.title}
                    </div>
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
