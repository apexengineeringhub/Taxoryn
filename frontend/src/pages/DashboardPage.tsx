import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Users,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Receipt,
  FileSpreadsheet,
  Building2,
  TrendingUp,
  ArrowUpRight,
  Sparkles,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { StatusBadge } from '../components/common/StatusBadge';
import { dashboardApi } from '../api/endpoints';
import { OrganizationDashboard } from '../types';

export const DashboardPage: React.FC = () => {
  const [dashboard, setDashboard] = useState<OrganizationDashboard | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    try {
      setIsLoading(true);
      const data = await dashboardApi.getOrganizationDashboard();
      setDashboard(data);
    } catch (err) {
      console.error('Failed to fetch dashboard data', err);
    } finally {
      setIsLoading(false);
    }
  };

  const formatCurrency = (val: number = 0) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(val);
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">Executive Practice Dashboard</h1>
          <p className="text-xs text-slate-500 mt-1">
            Real-time compliance health, client workload allocation, and billing realization metrics.
          </p>
        </div>
        <div className="inline-flex items-center gap-2 bg-white border border-slate-200 rounded-lg px-3 py-1.5 shadow-2xs text-xs font-semibold text-slate-700">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          Live Sync Active
        </div>
      </div>

      {/* Top Row: Core Practice KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Active Clients Card */}
        <Link
          to="/clients"
          className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:shadow-card-hover hover:border-slate-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-blue-600 transition-colors">
              Active Clients
            </span>
            <div className="w-9 h-9 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center group-hover:bg-blue-600 group-hover:text-white transition-all">
              <Users className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-slate-900">
              {isLoading ? '...' : dashboard?.clients?.active ?? 0}
            </span>
            <span className="text-xs font-semibold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
              of {dashboard?.clients?.total ?? 0} total
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center text-xs text-slate-500 justify-between">
            <span>Inactive / Prospects:</span>
            <span className="font-semibold text-slate-700">{dashboard?.clients?.inactive ?? 0}</span>
          </div>
        </Link>

        {/* GST Compliance Card */}
        <Link
          to="/gst"
          className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:shadow-card-hover hover:border-emerald-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-emerald-700 transition-colors">
              GST Compliance
            </span>
            <div className="w-9 h-9 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:bg-emerald-600 group-hover:text-white transition-all">
              <Building2 className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-emerald-600">
              {isLoading ? '...' : dashboard?.gst?.totalGstClients ?? 0}
            </span>
            <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
              GST Clients
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
            <span className="text-emerald-700 font-medium">Filed: {dashboard?.gst?.returnsFiled ?? 0}</span>
            <span className="text-amber-700 font-medium">Due: {dashboard?.gst?.returnsDue ?? 0}</span>
            <span className="text-rose-600 font-bold">Overdue: {dashboard?.gst?.returnsOverdue ?? 0}</span>
          </div>
        </Link>

        {/* ITR Compliance Card */}
        <Link
          to="/itr"
          className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:shadow-card-hover hover:border-purple-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-purple-600 transition-colors">
              ITR Compliance
            </span>
            <div className="w-9 h-9 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center group-hover:bg-purple-600 group-hover:text-white transition-all">
              <FileSpreadsheet className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-purple-600">
              {isLoading ? '...' : dashboard?.itr?.totalItrClients ?? 0}
            </span>
            <span className="text-xs font-semibold text-purple-700 bg-purple-50 px-2 py-0.5 rounded-full">
              ITR Clients
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
            <span className="text-purple-700 font-medium">Filed: {dashboard?.itr?.filed ?? 0}</span>
            <span className="text-amber-700 font-medium">Pending: {dashboard?.itr?.pending ?? 0}</span>
            <span className="text-rose-600 font-bold">Overdue: {dashboard?.itr?.overdue ?? 0}</span>
          </div>
        </Link>

        {/* Realization & Billing Card */}
        <Link
          to="/billing"
          className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:shadow-card-hover hover:border-amber-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-amber-600 transition-colors">
              Fee Realization
            </span>
            <div className="w-9 h-9 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center group-hover:bg-amber-600 group-hover:text-white transition-all">
              <Receipt className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-2xl font-black text-slate-900 truncate">
              {isLoading ? '...' : formatCurrency(dashboard?.billing?.paidAmount)}
            </span>
            <span className="text-xs font-semibold text-slate-400">Collected</span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
            <span className="text-slate-500">Outstanding:</span>
            <span className="font-bold text-rose-600">{formatCurrency(dashboard?.billing?.outstandingAmount)}</span>
          </div>
        </Link>
      </div>

      {/* Middle Grid: Tasks Status & Compliance Health */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Task Velocity Widget */}
        <Card
          title="Workflow & Task Overview"
          subtitle="Real-time operational task load across organization"
          className="lg:col-span-1"
        >
          <div className="space-y-4">
            <div className="flex items-center justify-between p-3 rounded-lg bg-slate-50 border border-slate-100">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-md bg-blue-100 text-blue-700 flex items-center justify-center font-bold text-xs">
                  {dashboard?.tasks?.total ?? 0}
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-800">Total Active Tasks</p>
                  <p className="text-[10px] text-slate-500">Across all practice assignments</p>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-3 gap-2 text-center">
              <div className="p-3 bg-amber-50/70 border border-amber-200/60 rounded-lg">
                <p className="text-xs text-amber-800 font-medium">Pending</p>
                <p className="text-lg font-black text-amber-900 mt-0.5">{dashboard?.tasks?.pending ?? 0}</p>
              </div>
              <div className="p-3 bg-rose-50/70 border border-rose-200/60 rounded-lg">
                <p className="text-xs text-rose-800 font-medium">Overdue</p>
                <p className="text-lg font-black text-rose-900 mt-0.5">{dashboard?.tasks?.overdue ?? 0}</p>
              </div>
              <div className="p-3 bg-emerald-50/70 border border-emerald-200/60 rounded-lg">
                <p className="text-xs text-emerald-800 font-medium">Completed</p>
                <p className="text-lg font-black text-emerald-900 mt-0.5">{dashboard?.tasks?.completed ?? 0}</p>
              </div>
            </div>
          </div>
        </Card>

        {/* Employee Workload Table */}
        <Card
          title="CA Team & Staff Workload Allocation"
          subtitle="Assigned vs pending tasks per practice staff member"
          className="lg:col-span-2"
          noPadding
        >
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50/80 font-semibold text-slate-500 uppercase tracking-wider">
                  <th className="px-5 py-3">Employee</th>
                  <th className="px-4 py-3">Department</th>
                  <th className="px-4 py-3 text-center">Assigned</th>
                  <th className="px-4 py-3 text-center">Pending</th>
                  <th className="px-4 py-3 text-center">Overdue</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr>
                    <td colSpan={5} className="text-center py-8 text-slate-400">Loading workload metrics...</td>
                  </tr>
                ) : !dashboard?.employeeWorkload?.length ? (
                  <tr>
                    <td colSpan={5} className="text-center py-8 text-slate-400">No active team workload recorded</td>
                  </tr>
                ) : (
                  dashboard.employeeWorkload.map((emp) => (
                    <tr key={emp.employeeId} className="table-row-hover">
                      <td className="px-5 py-3 font-semibold text-slate-900">
                        {emp.employeeName}
                        <span className="block text-[10px] font-normal text-slate-400">{emp.employeeCode} • {emp.designation}</span>
                      </td>
                      <td className="px-4 py-3 text-slate-600">{emp.department || 'General Tax'}</td>
                      <td className="px-4 py-3 text-center font-bold text-slate-800">{emp.assignedTasks}</td>
                      <td className="px-4 py-3 text-center">
                        <span className="px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 font-semibold text-xs border border-amber-200/50">
                          {emp.pendingTasks}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span className="px-2 py-0.5 rounded-full bg-rose-50 text-rose-700 font-semibold text-xs border border-rose-200/50">
                          {emp.overdueTasks}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </div>
  );
};
