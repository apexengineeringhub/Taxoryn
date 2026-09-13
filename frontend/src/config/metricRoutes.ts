export type MetricType =
  | 'CLIENTS_ACTIVE'
  | 'CLIENTS_INACTIVE'
  | 'CLIENTS_TOTAL'
  | 'GST_CLIENTS'
  | 'GST_FILED'
  | 'GST_DUE'
  | 'GST_OVERDUE'
  | 'ITR_CLIENTS'
  | 'ITR_FILED'
  | 'ITR_PENDING'
  | 'ITR_OVERDUE'
  | 'TDS_CLIENTS'
  | 'TDS_FILED'
  | 'TDS_PENDING'
  | 'TDS_OVERDUE'
  | 'BILLING_COLLECTED'
  | 'BILLING_OUTSTANDING'
  | 'BILLING_OVERDUE'
  | 'TASKS_TOTAL'
  | 'TASKS_PENDING'
  | 'TASKS_OVERDUE'
  | 'TASKS_COMPLETED'
  | 'EMPLOYEE_ASSIGNED'
  | 'EMPLOYEE_PENDING'
  | 'EMPLOYEE_OVERDUE';

export interface MetricNavigationContext {
  employeeId?: string;
  clientId?: string;
  quarter?: string;
  financialYear?: string;
  assessmentYear?: string;
  isStaff?: boolean;
}

export interface MetricNavigationConfig {
  metric: MetricType;
  label: string;
  pathname: string;
  search?: Record<string, string | number | boolean | undefined>;
  requiredPermissions?: string[];
}

export const METRIC_NAVIGATION_REGISTRY: Record<
  MetricType,
  (context?: MetricNavigationContext) => MetricNavigationConfig
> = {
  CLIENTS_ACTIVE: () => ({
    metric: 'CLIENTS_ACTIVE',
    label: 'Active Clients',
    pathname: '/clients',
    search: { status: 'ACTIVE' },
    requiredPermissions: ['CLIENT_VIEW', 'CLIENT_READ'],
  }),
  CLIENTS_INACTIVE: () => ({
    metric: 'CLIENTS_INACTIVE',
    label: 'Inactive Clients',
    pathname: '/clients',
    search: { status: 'INACTIVE' },
    requiredPermissions: ['CLIENT_VIEW', 'CLIENT_READ'],
  }),
  CLIENTS_TOTAL: () => ({
    metric: 'CLIENTS_TOTAL',
    label: 'All Clients',
    pathname: '/clients',
    requiredPermissions: ['CLIENT_VIEW', 'CLIENT_READ'],
  }),
  GST_CLIENTS: () => ({
    metric: 'GST_CLIENTS',
    label: 'GST Clients',
    pathname: '/gst',
    requiredPermissions: ['GST_VIEW', 'GST_READ'],
  }),
  GST_FILED: () => ({
    metric: 'GST_FILED',
    label: 'Filed GST Returns',
    pathname: '/gst',
    search: { status: 'FILED' },
    requiredPermissions: ['GST_VIEW', 'GST_READ'],
  }),
  GST_DUE: () => ({
    metric: 'GST_DUE',
    label: 'Due GST Returns',
    pathname: '/gst',
    search: { status: 'PENDING' },
    requiredPermissions: ['GST_VIEW', 'GST_READ'],
  }),
  GST_OVERDUE: () => ({
    metric: 'GST_OVERDUE',
    label: 'Overdue GST Returns',
    pathname: '/gst',
    search: { status: 'OVERDUE' },
    requiredPermissions: ['GST_VIEW', 'GST_READ'],
  }),
  ITR_CLIENTS: () => ({
    metric: 'ITR_CLIENTS',
    label: 'ITR Clients',
    pathname: '/itr',
    requiredPermissions: ['ITR_VIEW', 'ITR_READ'],
  }),
  ITR_FILED: () => ({
    metric: 'ITR_FILED',
    label: 'Filed ITR Returns',
    pathname: '/itr',
    search: { status: 'FILED' },
    requiredPermissions: ['ITR_VIEW', 'ITR_READ'],
  }),
  ITR_PENDING: () => ({
    metric: 'ITR_PENDING',
    label: 'Pending ITR Returns',
    pathname: '/itr',
    search: { status: 'PENDING' },
    requiredPermissions: ['ITR_VIEW', 'ITR_READ'],
  }),
  ITR_OVERDUE: () => ({
    metric: 'ITR_OVERDUE',
    label: 'Overdue ITR Returns',
    pathname: '/itr',
    search: { status: 'OVERDUE' },
    requiredPermissions: ['ITR_VIEW', 'ITR_READ'],
  }),
  TDS_CLIENTS: () => ({
    metric: 'TDS_CLIENTS',
    label: 'TAN / TDS Clients',
    pathname: '/tds',
    requiredPermissions: ['TDS_VIEW', 'TDS_READ'],
  }),
  TDS_FILED: () => ({
    metric: 'TDS_FILED',
    label: 'Filed TDS Returns',
    pathname: '/tds',
    search: { status: 'FILED' },
    requiredPermissions: ['TDS_VIEW', 'TDS_READ'],
  }),
  TDS_PENDING: () => ({
    metric: 'TDS_PENDING',
    label: 'Pending TDS Returns',
    pathname: '/tds',
    search: { status: 'PENDING' },
    requiredPermissions: ['TDS_VIEW', 'TDS_READ'],
  }),
  TDS_OVERDUE: () => ({
    metric: 'TDS_OVERDUE',
    label: 'Overdue TDS Returns',
    pathname: '/tds',
    search: { status: 'OVERDUE' },
    requiredPermissions: ['TDS_VIEW', 'TDS_READ'],
  }),
  BILLING_COLLECTED: () => ({
    metric: 'BILLING_COLLECTED',
    label: 'Fee Collected',
    pathname: '/billing',
    search: { status: 'PAID' },
    requiredPermissions: ['BILLING_VIEW', 'BILLING_READ'],
  }),
  BILLING_OUTSTANDING: () => ({
    metric: 'BILLING_OUTSTANDING',
    label: 'Fee Outstanding',
    pathname: '/billing',
    search: { status: 'ISSUED' },
    requiredPermissions: ['BILLING_VIEW', 'BILLING_READ'],
  }),
  BILLING_OVERDUE: () => ({
    metric: 'BILLING_OVERDUE',
    label: 'Overdue Invoices',
    pathname: '/billing',
    search: { status: 'OVERDUE' },
    requiredPermissions: ['BILLING_VIEW', 'BILLING_READ'],
  }),
  TASKS_TOTAL: (ctx) => ({
    metric: 'TASKS_TOTAL',
    label: 'Total Active Tasks',
    pathname: '/tasks',
    search: ctx?.isStaff ? { tab: 'WORKLIST', scope: 'MY_WORK' } : { tab: 'ALL_TASKS' },
    requiredPermissions: ['TASK_VIEW', 'TASK_READ'],
  }),
  TASKS_PENDING: (ctx) => ({
    metric: 'TASKS_PENDING',
    label: 'Pending Tasks',
    pathname: '/tasks',
    search: ctx?.isStaff ? { tab: 'WORKLIST', scope: 'MY_WORK' } : { tab: 'ALL_TASKS', status: 'TODO' },
    requiredPermissions: ['TASK_VIEW', 'TASK_READ'],
  }),
  TASKS_OVERDUE: (ctx) => ({
    metric: 'TASKS_OVERDUE',
    label: 'Overdue Tasks',
    pathname: '/tasks',
    search: ctx?.isStaff ? { tab: 'WORKLIST', scope: 'MY_WORK', bucket: 'OVERDUE' } : { tab: 'WORKLIST', bucket: 'OVERDUE' },
    requiredPermissions: ['TASK_VIEW', 'TASK_READ'],
  }),
  TASKS_COMPLETED: (ctx) => ({
    metric: 'TASKS_COMPLETED',
    label: 'Completed Tasks',
    pathname: '/tasks',
    search: ctx?.isStaff ? { tab: 'WORKLIST', scope: 'MY_WORK', bucket: 'COMPLETED' } : { tab: 'WORKLIST', bucket: 'COMPLETED' },
    requiredPermissions: ['TASK_VIEW', 'TASK_READ'],
  }),
  EMPLOYEE_ASSIGNED: (ctx) => ({
    metric: 'EMPLOYEE_ASSIGNED',
    label: 'Employee Assigned Tasks',
    pathname: '/tasks',
    search: ctx?.employeeId
      ? { tab: 'ALL_TASKS', assignedTo: ctx.employeeId }
      : { tab: 'ALL_TASKS' },
    requiredPermissions: ['TASK_VIEW', 'TASK_READ'],
  }),
  EMPLOYEE_PENDING: (ctx) => ({
    metric: 'EMPLOYEE_PENDING',
    label: 'Employee Pending Tasks',
    pathname: '/tasks',
    search: ctx?.employeeId
      ? { tab: 'ALL_TASKS', assignedTo: ctx.employeeId, status: 'TODO' }
      : { tab: 'ALL_TASKS', status: 'TODO' },
    requiredPermissions: ['TASK_VIEW', 'TASK_READ'],
  }),
  EMPLOYEE_OVERDUE: (ctx) => ({
    metric: 'EMPLOYEE_OVERDUE',
    label: 'Employee Overdue Tasks',
    pathname: '/tasks',
    search: ctx?.employeeId
      ? { tab: 'WORKLIST', scope: 'TEAM_WORK', assignedTo: ctx.employeeId, bucket: 'OVERDUE' }
      : { tab: 'WORKLIST', bucket: 'OVERDUE' },
    requiredPermissions: ['TASK_VIEW', 'TASK_READ'],
  }),
};

export const getMetricNavigationConfig = (
  metric: MetricType,
  context?: MetricNavigationContext
): MetricNavigationConfig => {
  const resolver = METRIC_NAVIGATION_REGISTRY[metric];
  if (!resolver) {
    throw new Error(`Unknown metric type: ${metric}`);
  }
  return resolver(context);
};

export const buildMetricUrl = (
  metric: MetricType,
  context?: MetricNavigationContext
): string => {
  const config = getMetricNavigationConfig(metric, context);
  if (!config.search || Object.keys(config.search).length === 0) {
    return config.pathname;
  }
  const params = new URLSearchParams();
  Object.entries(config.search).forEach(([key, val]) => {
    if (val !== undefined && val !== null && val !== '') {
      params.set(key, String(val));
    }
  });
  const queryString = params.toString();
  return queryString ? `${config.pathname}?${queryString}` : config.pathname;
};
