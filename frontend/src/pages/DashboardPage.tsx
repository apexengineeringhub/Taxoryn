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
  Percent,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { StatusBadge } from '../components/common/StatusBadge';
import { ActionableMetric } from '../components/common/ActionableMetric';
import { dashboardApi } from '../api/endpoints';
import { useAuth } from '../context/AuthContext';
import { OrganizationDashboard } from '../types';
import { ClientPortalManagementPage } from './ClientPortalManagementPage';
import { PlatformOverviewPage } from './PlatformOverviewPage';
import { SupportOverviewPage } from './SupportOverviewPage';

export const DashboardPage: React.FC = () => {
  const [dashboard, setDashboard] = useState<OrganizationDashboard | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const { user } = useAuth();

  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isTaxorynSuperAdmin = userRoleCodes.includes('TAXORYN_SUPERADMIN') || userRoleCodes.includes('SUPER_ADMIN');
  const isSupportAdmin = userRoleCodes.includes('TAXORYN_SUPPORT_ADMIN');
  const isPlatformUser = isTaxorynSuperAdmin || isSupportAdmin || userRoleCodes.some((r: string) => r.startsWith('TAXORYN_'));
  const isClientUser = userRoleCodes.some((r: string) => ['CLIENT_USER', 'PRACTICE_CLIENT', 'CLIENT_ADMIN', 'MARKETPLACE_CUSTOMER'].includes(r));
  const isFirmAdmin = !isPlatformUser && userRoleCodes.some((r: string) => ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'].includes(r));
  const isStaff = !isPlatformUser && !isFirmAdmin && userRoleCodes.some((r: string) => ['PRACTICE_EMPLOYEE', 'ARTICLE_ASSISTANT', 'STAFF', 'TRAINEE', 'ACCOUNTANT'].includes(r));
  const userPermissions = user?.permissions || [];
  const hasBillingAccess = isFirmAdmin || userPermissions.includes('BILLING_VIEW') || userPermissions.includes('BILLING_READ');

  useEffect(() => {
    if (!isClientUser && !isPlatformUser) {
      loadDashboard();
    }
  }, [isClientUser, isPlatformUser]);

  if (isSupportAdmin) {
    return <SupportOverviewPage />;
  }

  if (isPlatformUser) {
    return <PlatformOverviewPage />;
  }

  if (isClientUser) {
    return <ClientPortalManagementPage />;
  }

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
          <h1 className="text-2xl font-black tracking-tight text-slate-900">
            {isStaff ? 'Staff Operations Dashboard' : 'Executive Practice Dashboard'}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            {isStaff
              ? 'Personal assigned deliverables, client account compliance status, and tax filing workflow.'
              : 'Real-time compliance health, client workload allocation, and billing realization metrics.'}
          </p>
        </div>
        <div className="inline-flex items-center gap-2 bg-white border border-slate-200 rounded-lg px-3 py-1.5 shadow-2xs text-xs font-semibold text-slate-700">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          Live Sync Active
        </div>
      </div>

      {/* Top Row: Core Practice KPI Cards (5 Cards) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4">
        {/* 1. Active Clients Card */}
        <div className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:border-slate-300 transition-all block">
          <Link to="/clients" className="flex items-center justify-between group">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-blue-600 transition-colors">
              {isStaff ? 'My Assigned Accounts' : 'Active Clients'}
            </span>
            <div className="w-9 h-9 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center group-hover:bg-blue-600 group-hover:text-white transition-all">
              <Users className="w-5 h-5" />
            </div>
          </Link>
          <div className="mt-4 flex items-baseline justify-between">
            <ActionableMetric
              metric="CLIENTS_ACTIVE"
              value={isLoading ? '...' : dashboard?.clients?.active ?? 0}
              variant="display"
              color="default"
              ariaLabel={`View ${dashboard?.clients?.active ?? 0} active clients`}
            />
            <ActionableMetric
              metric="CLIENTS_TOTAL"
              value={isStaff ? `of ${dashboard?.clients?.total ?? 0} assigned` : `of ${dashboard?.clients?.total ?? 0} total`}
              variant="pill"
              color="default"
              ariaLabel={`View all ${dashboard?.clients?.total ?? 0} clients`}
            />
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center text-xs text-slate-500 justify-between">
            <span>{isStaff ? 'Inactive Accounts:' : 'Inactive / Prospects:'}</span>
            <ActionableMetric
              metric="CLIENTS_INACTIVE"
              value={dashboard?.clients?.inactive ?? 0}
              variant="inline"
              color="default"
              ariaLabel={`View ${dashboard?.clients?.inactive ?? 0} inactive clients`}
            />
          </div>
        </div>

        {/* 2. GST Compliance Card */}
        <div className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:border-emerald-300 transition-all block">
          <Link to="/gst" className="flex items-center justify-between group">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-emerald-700 transition-colors">
              GST Compliance
            </span>
            <div className="w-9 h-9 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:bg-emerald-600 group-hover:text-white transition-all">
              <Building2 className="w-5 h-5" />
            </div>
          </Link>
          <div className="mt-4 flex items-baseline justify-between">
            <ActionableMetric
              metric="GST_CLIENTS"
              value={isLoading ? '...' : dashboard?.gst?.totalGstClients ?? 0}
              variant="display"
              color="emerald"
              ariaLabel={`View ${dashboard?.gst?.totalGstClients ?? 0} GST clients`}
            />
            <ActionableMetric
              metric="GST_CLIENTS"
              value="GST Clients"
              variant="pill"
              color="emerald"
              ariaLabel="View GST Compliance Hub"
            />
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
            <ActionableMetric metric="GST_FILED" label="Filed" value={dashboard?.gst?.returnsFiled ?? 0} variant="inline" color="emerald" />
            <ActionableMetric metric="GST_DUE" label="Due" value={dashboard?.gst?.returnsDue ?? 0} variant="inline" color="amber" />
            <ActionableMetric metric="GST_OVERDUE" label="Overdue" value={dashboard?.gst?.returnsOverdue ?? 0} variant="inline" color="rose" />
          </div>
        </div>

        {/* 3. ITR Compliance Card */}
        <div className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:border-purple-300 transition-all block">
          <Link to="/itr" className="flex items-center justify-between group">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-purple-600 transition-colors">
              ITR Compliance
            </span>
            <div className="w-9 h-9 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center group-hover:bg-purple-600 group-hover:text-white transition-all">
              <FileSpreadsheet className="w-5 h-5" />
            </div>
          </Link>
          <div className="mt-4 flex items-baseline justify-between">
            <ActionableMetric
              metric="ITR_CLIENTS"
              value={isLoading ? '...' : dashboard?.itr?.totalItrClients ?? 0}
              variant="display"
              color="purple"
              ariaLabel={`View ${dashboard?.itr?.totalItrClients ?? 0} ITR clients`}
            />
            <ActionableMetric
              metric="ITR_CLIENTS"
              value="ITR Clients"
              variant="pill"
              color="purple"
              ariaLabel="View ITR Compliance Hub"
            />
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
            <ActionableMetric metric="ITR_FILED" label="Filed" value={dashboard?.itr?.filed ?? 0} variant="inline" color="purple" />
            <ActionableMetric metric="ITR_PENDING" label="Pending" value={dashboard?.itr?.pending ?? 0} variant="inline" color="amber" />
            <ActionableMetric metric="ITR_OVERDUE" label="Overdue" value={dashboard?.itr?.overdue ?? 0} variant="inline" color="rose" />
          </div>
        </div>

        {/* 4. TDS Compliance Card */}
        <div className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:border-indigo-300 transition-all block">
          <Link to="/tds" className="flex items-center justify-between group">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-indigo-600 transition-colors">
              TDS Compliance
            </span>
            <div className="w-9 h-9 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center group-hover:bg-indigo-600 group-hover:text-white transition-all">
              <Percent className="w-5 h-5" />
            </div>
          </Link>
          <div className="mt-4 flex items-baseline justify-between">
            <ActionableMetric
              metric="TDS_CLIENTS"
              value={isLoading ? '...' : dashboard?.tds?.totalTdsClients ?? 0}
              variant="display"
              color="indigo"
              ariaLabel={`View ${dashboard?.tds?.totalTdsClients ?? 0} TAN clients`}
            />
            <ActionableMetric
              metric="TDS_CLIENTS"
              value="TAN Clients"
              variant="pill"
              color="indigo"
              ariaLabel="View TDS Compliance Hub"
            />
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
            <ActionableMetric metric="TDS_FILED" label="Filed" value={dashboard?.tds?.filed ?? 0} variant="inline" color="indigo" />
            <ActionableMetric metric="TDS_PENDING" label="Pending" value={dashboard?.tds?.pending ?? 0} variant="inline" color="amber" />
            <ActionableMetric metric="TDS_OVERDUE" label="Overdue" value={dashboard?.tds?.overdue ?? 0} variant="inline" color="rose" />
          </div>
        </div>

        {/* 5. Fee Realization for Admins OR Assigned Deliverables for Staff */}
        {hasBillingAccess ? (
          <div className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:border-amber-300 transition-all block">
            <Link to="/billing" className="flex items-center justify-between group">
              <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-amber-600 transition-colors">
                Fee Realization
              </span>
              <div className="w-9 h-9 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center group-hover:bg-amber-600 group-hover:text-white transition-all">
                <Receipt className="w-5 h-5" />
              </div>
            </Link>
            <div className="mt-4 flex items-baseline justify-between">
              <ActionableMetric
                metric="BILLING_COLLECTED"
                value={isLoading ? '...' : formatCurrency(dashboard?.billing?.paidAmount)}
                variant="kpi"
                color="default"
                ariaLabel={`View collected invoices totaling ${formatCurrency(dashboard?.billing?.paidAmount)}`}
              />
              <ActionableMetric
                metric="BILLING_COLLECTED"
                value="Collected"
                className="text-xs font-semibold text-slate-400 hover:text-slate-600"
                ariaLabel="View collected invoices"
              />
            </div>
            <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
              <span className="text-slate-500">Outstanding:</span>
              <ActionableMetric
                metric="BILLING_OUTSTANDING"
                value={formatCurrency(dashboard?.billing?.outstandingAmount)}
                variant="inline"
                color="rose"
                ariaLabel={`View outstanding invoices totaling ${formatCurrency(dashboard?.billing?.outstandingAmount)}`}
              />
            </div>
          </div>
        ) : (
          <div className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:border-blue-300 transition-all block">
            <Link to="/tasks" className="flex items-center justify-between group">
              <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-blue-600 transition-colors">
                🎯 My Deliverables
              </span>
              <div className="w-9 h-9 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center group-hover:bg-blue-600 group-hover:text-white transition-all">
                <CheckCircle2 className="w-5 h-5" />
              </div>
            </Link>
            <div className="mt-4 flex items-baseline justify-between">
              <ActionableMetric
                metric="TASKS_PENDING"
                context={{ isStaff }}
                value={isLoading ? '...' : dashboard?.tasks?.pending ?? 0}
                variant="display"
                color="blue"
                ariaLabel={`View ${dashboard?.tasks?.pending ?? 0} pending deliverables`}
              />
              <ActionableMetric
                metric="TASKS_PENDING"
                context={{ isStaff }}
                value="Pending Tasks"
                variant="pill"
                color="blue"
                ariaLabel="View pending tasks"
              />
            </div>
            <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
              <ActionableMetric
                metric="TASKS_COMPLETED"
                context={{ isStaff }}
                label="Completed"
                value={dashboard?.tasks?.completed ?? 0}
                variant="inline"
                color="emerald"
              />
              <ActionableMetric
                metric="TASKS_OVERDUE"
                context={{ isStaff }}
                label="Overdue"
                value={dashboard?.tasks?.overdue ?? 0}
                variant="inline"
                color="rose"
              />
            </div>
          </div>
        )}
      </div>

      {/* Middle Grid: Tasks Status & Compliance Health */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Task Velocity Widget */}
        <Card
          title={isStaff ? 'My Workflow & Deliverables' : 'Workflow & Task Overview'}
          subtitle={isStaff ? 'Personal assigned deliverable load and status breakdown' : 'Real-time operational task load across organization'}
          className="lg:col-span-1"
        >
          <div className="space-y-4">
            <ActionableMetric
              metric="TASKS_TOTAL"
              context={{ isStaff }}
              value={dashboard?.tasks?.total ?? 0}
              className="block w-full"
            >
              <div className="flex items-center justify-between p-3 rounded-lg bg-slate-50 border border-slate-100 hover:border-blue-300 hover:bg-blue-50/50 transition-colors w-full">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-md bg-blue-100 text-blue-700 flex items-center justify-center font-bold text-xs">
                    {dashboard?.tasks?.total ?? 0}
                  </div>
                  <div className="text-left">
                    <p className="text-xs font-bold text-slate-800">{isStaff ? 'My Total Assigned Tasks' : 'Total Active Tasks'}</p>
                    <p className="text-[10px] text-slate-500">{isStaff ? 'Directly assigned to you' : 'Across all practice assignments'}</p>
                  </div>
                </div>
              </div>
            </ActionableMetric>

            <div className="grid grid-cols-3 gap-2 text-center">
              <ActionableMetric
                metric="TASKS_PENDING"
                context={{ isStaff }}
                value={dashboard?.tasks?.pending ?? 0}
                className="p-3 bg-amber-50/70 border border-amber-200/60 rounded-lg hover:border-amber-400 hover:bg-amber-100/70 transition-all flex flex-col items-center justify-center"
              >
                <p className="text-xs text-amber-800 font-medium">Pending</p>
                <p className="text-lg font-black text-amber-900 mt-0.5">{dashboard?.tasks?.pending ?? 0}</p>
              </ActionableMetric>

              <ActionableMetric
                metric="TASKS_OVERDUE"
                context={{ isStaff }}
                value={dashboard?.tasks?.overdue ?? 0}
                className="p-3 bg-rose-50/70 border border-rose-200/60 rounded-lg hover:border-rose-400 hover:bg-rose-100/70 transition-all flex flex-col items-center justify-center"
              >
                <p className="text-xs text-rose-800 font-medium">Overdue</p>
                <p className="text-lg font-black text-rose-900 mt-0.5">{dashboard?.tasks?.overdue ?? 0}</p>
              </ActionableMetric>

              <ActionableMetric
                metric="TASKS_COMPLETED"
                context={{ isStaff }}
                value={dashboard?.tasks?.completed ?? 0}
                className="p-3 bg-emerald-50/70 border border-emerald-200/60 rounded-lg hover:border-emerald-400 hover:bg-emerald-100/70 transition-all flex flex-col items-center justify-center"
              >
                <p className="text-xs text-emerald-800 font-medium">Completed</p>
                <p className="text-lg font-black text-emerald-900 mt-0.5">{dashboard?.tasks?.completed ?? 0}</p>
              </ActionableMetric>
            </div>
          </div>
        </Card>

        {/* Employee Workload Table */}
        <Card
          title={isStaff ? 'My Practice Workload & Tasks' : 'CA Team & Staff Workload Allocation'}
          subtitle={isStaff ? 'Breakdown of your current deliverable queue' : 'Assigned vs pending tasks per practice staff member'}
          className="lg:col-span-2"
          noPadding
        >
          <div className="hidden md:block overflow-x-auto">
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
                  dashboard.employeeWorkload.map((emp: any) => (
                    <tr key={emp.employeeId} className="table-row-hover">
                      <td className="px-5 py-3 font-semibold text-slate-900">
                        {emp.employeeName}
                        <span className="block text-[10px] font-normal text-slate-400">{emp.employeeCode} • {emp.designation}</span>
                      </td>
                      <td className="px-4 py-3 text-slate-600">{emp.department || 'General Tax'}</td>
                      <td className="px-4 py-3 text-center">
                        <ActionableMetric
                          metric="EMPLOYEE_ASSIGNED"
                          context={{ employeeId: emp.employeeId }}
                          value={emp.assignedTasks}
                          variant="table-cell"
                        />
                      </td>
                      <td className="px-4 py-3 text-center">
                        <ActionableMetric
                          metric="EMPLOYEE_PENDING"
                          context={{ employeeId: emp.employeeId }}
                          value={emp.pendingTasks}
                          variant="pill"
                          color="amber"
                        />
                      </td>
                      <td className="px-4 py-3 text-center">
                        <ActionableMetric
                          metric="EMPLOYEE_OVERDUE"
                          context={{ employeeId: emp.employeeId }}
                          value={emp.overdueTasks}
                          variant="pill"
                          color="rose"
                        />
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Mobile card list — avoids forcing horizontal scroll to read workload figures */}
          <div className="md:hidden">
            {isLoading ? (
              <div className="text-center py-8 text-slate-400 text-xs">Loading workload metrics...</div>
            ) : !dashboard?.employeeWorkload?.length ? (
              <div className="text-center py-8 text-slate-400 text-xs">No active team workload recorded</div>
            ) : (
              <ul className="divide-y divide-slate-100">
                {dashboard.employeeWorkload.map((emp: any) => (
                  <li key={emp.employeeId} className="px-4 py-3 text-xs space-y-2">
                    <div>
                      <p className="font-semibold text-slate-900">{emp.employeeName}</p>
                      <p className="text-[10px] text-slate-400">{emp.employeeCode} • {emp.designation} • {emp.department || 'General Tax'}</p>
                    </div>
                    <div className="grid grid-cols-3 gap-2 text-center">
                      <ActionableMetric
                        metric="EMPLOYEE_ASSIGNED"
                        context={{ employeeId: emp.employeeId }}
                        value={emp.assignedTasks}
                        className="p-2 rounded-lg bg-slate-50 border border-slate-100 hover:border-slate-300 transition-colors block text-center"
                      >
                        <p className="text-[10px] text-slate-500">Assigned</p>
                        <p className="font-bold text-slate-800">{emp.assignedTasks}</p>
                      </ActionableMetric>
                      <ActionableMetric
                        metric="EMPLOYEE_PENDING"
                        context={{ employeeId: emp.employeeId }}
                        value={emp.pendingTasks}
                        className="p-2 rounded-lg bg-amber-50/70 border border-amber-200/50 hover:border-amber-400 transition-colors block text-center"
                      >
                        <p className="text-[10px] text-amber-700">Pending</p>
                        <p className="font-bold text-amber-800">{emp.pendingTasks}</p>
                      </ActionableMetric>
                      <ActionableMetric
                        metric="EMPLOYEE_OVERDUE"
                        context={{ employeeId: emp.employeeId }}
                        value={emp.overdueTasks}
                        className="p-2 rounded-lg bg-rose-50/70 border border-rose-200/50 hover:border-rose-400 transition-colors block text-center"
                      >
                        <p className="text-[10px] text-rose-700">Overdue</p>
                        <p className="font-bold text-rose-800">{emp.overdueTasks}</p>
                      </ActionableMetric>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
};
