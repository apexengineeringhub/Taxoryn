import { describe, it } from 'node:test';
import assert from 'node:assert';
import {
  METRIC_NAVIGATION_REGISTRY,
  buildMetricUrl,
  getMetricNavigationConfig,
  type MetricType,
} from '../config/metricRoutes.ts';

describe('Taxoryn Global Actionable Metric Navigation Standard', () => {
  describe('Metric Registry Completeness', () => {
    const requiredMetrics: MetricType[] = [
      'CLIENTS_ACTIVE',
      'CLIENTS_INACTIVE',
      'CLIENTS_TOTAL',
      'GST_CLIENTS',
      'GST_FILED',
      'GST_DUE',
      'GST_OVERDUE',
      'ITR_CLIENTS',
      'ITR_FILED',
      'ITR_PENDING',
      'ITR_OVERDUE',
      'TDS_CLIENTS',
      'TDS_FILED',
      'TDS_PENDING',
      'TDS_OVERDUE',
      'BILLING_COLLECTED',
      'BILLING_OUTSTANDING',
      'BILLING_OVERDUE',
      'TASKS_TOTAL',
      'TASKS_PENDING',
      'TASKS_OVERDUE',
      'TASKS_COMPLETED',
      'EMPLOYEE_ASSIGNED',
      'EMPLOYEE_PENDING',
      'EMPLOYEE_OVERDUE',
    ];

    it('should have all 25 actionable metrics registered in METRIC_NAVIGATION_REGISTRY', () => {
      assert.strictEqual(requiredMetrics.length, 25);
      for (const metric of requiredMetrics) {
        const config = getMetricNavigationConfig(metric);
        assert.ok(config, `Metric config for ${metric} must exist`);
        assert.ok(config.pathname, `Metric ${metric} must have a destination pathname`);
        assert.ok(config.label, `Metric ${metric} must have a default label`);
        assert.ok(config.requiredPermissions && config.requiredPermissions.length > 0, `Metric ${metric} must define requiredPermissions`);
      }
    });
  });

  describe('Client Metric Routing', () => {
    it('CLIENTS_ACTIVE navigates to /clients?status=ACTIVE', () => {
      const url = buildMetricUrl('CLIENTS_ACTIVE');
      assert.strictEqual(url, '/clients?status=ACTIVE');
    });

    it('CLIENTS_INACTIVE navigates to /clients?status=INACTIVE', () => {
      const url = buildMetricUrl('CLIENTS_INACTIVE');
      assert.strictEqual(url, '/clients?status=INACTIVE');
    });

    it('CLIENTS_TOTAL navigates to /clients', () => {
      const url = buildMetricUrl('CLIENTS_TOTAL');
      assert.strictEqual(url, '/clients');
    });
  });

  describe('GST Compliance Metric Routing', () => {
    it('GST_CLIENTS navigates to /gst', () => {
      const url = buildMetricUrl('GST_CLIENTS');
      assert.strictEqual(url, '/gst');
    });

    it('GST_FILED navigates to /gst?status=FILED', () => {
      const url = buildMetricUrl('GST_FILED');
      assert.strictEqual(url, '/gst?status=FILED');
    });

    it('GST_DUE navigates to /gst?status=PENDING', () => {
      const url = buildMetricUrl('GST_DUE');
      assert.strictEqual(url, '/gst?status=PENDING');
    });

    it('GST_OVERDUE navigates to /gst?status=OVERDUE', () => {
      const url = buildMetricUrl('GST_OVERDUE');
      assert.strictEqual(url, '/gst?status=OVERDUE');
    });
  });

  describe('ITR Compliance Metric Routing', () => {
    it('ITR_CLIENTS navigates to /itr', () => {
      const url = buildMetricUrl('ITR_CLIENTS');
      assert.strictEqual(url, '/itr');
    });

    it('ITR_FILED navigates to /itr?status=FILED', () => {
      const url = buildMetricUrl('ITR_FILED');
      assert.strictEqual(url, '/itr?status=FILED');
    });

    it('ITR_PENDING navigates to /itr?status=PENDING', () => {
      const url = buildMetricUrl('ITR_PENDING');
      assert.strictEqual(url, '/itr?status=PENDING');
    });

    it('ITR_OVERDUE navigates to /itr?status=OVERDUE', () => {
      const url = buildMetricUrl('ITR_OVERDUE');
      assert.strictEqual(url, '/itr?status=OVERDUE');
    });
  });

  describe('TDS Compliance Metric Routing', () => {
    it('TDS_CLIENTS navigates to /tds', () => {
      const url = buildMetricUrl('TDS_CLIENTS');
      assert.strictEqual(url, '/tds');
    });

    it('TDS_FILED navigates to /tds?status=FILED', () => {
      const url = buildMetricUrl('TDS_FILED');
      assert.strictEqual(url, '/tds?status=FILED');
    });

    it('TDS_PENDING navigates to /tds?status=PENDING', () => {
      const url = buildMetricUrl('TDS_PENDING');
      assert.strictEqual(url, '/tds?status=PENDING');
    });

    it('TDS_OVERDUE navigates to /tds?status=OVERDUE', () => {
      const url = buildMetricUrl('TDS_OVERDUE');
      assert.strictEqual(url, '/tds?status=OVERDUE');
    });
  });

  describe('Billing & Invoices Metric Routing', () => {
    it('BILLING_COLLECTED navigates to /billing?status=PAID', () => {
      const url = buildMetricUrl('BILLING_COLLECTED');
      assert.strictEqual(url, '/billing?status=PAID');
    });

    it('BILLING_OUTSTANDING navigates to /billing?status=ISSUED', () => {
      const url = buildMetricUrl('BILLING_OUTSTANDING');
      assert.strictEqual(url, '/billing?status=ISSUED');
    });

    it('BILLING_OVERDUE navigates to /billing?status=OVERDUE', () => {
      const url = buildMetricUrl('BILLING_OVERDUE');
      assert.strictEqual(url, '/billing?status=OVERDUE');
    });
  });

  describe('Task & Velocity Metric Routing', () => {
    it('TASKS_TOTAL navigates to /tasks?tab=ALL_TASKS for practice admin', () => {
      const url = buildMetricUrl('TASKS_TOTAL', { isStaff: false });
      assert.strictEqual(url, '/tasks?tab=ALL_TASKS');
    });

    it('TASKS_TOTAL navigates to /tasks?tab=WORKLIST&scope=MY_WORK for staff member', () => {
      const url = buildMetricUrl('TASKS_TOTAL', { isStaff: true });
      assert.strictEqual(url, '/tasks?tab=WORKLIST&scope=MY_WORK');
    });

    it('TASKS_PENDING navigates to /tasks?tab=ALL_TASKS&status=TODO for practice admin', () => {
      const url = buildMetricUrl('TASKS_PENDING', { isStaff: false });
      assert.strictEqual(url, '/tasks?tab=ALL_TASKS&status=TODO');
    });

    it('TASKS_PENDING navigates to /tasks?tab=WORKLIST&scope=MY_WORK for staff member', () => {
      const url = buildMetricUrl('TASKS_PENDING', { isStaff: true });
      assert.strictEqual(url, '/tasks?tab=WORKLIST&scope=MY_WORK');
    });

    it('TASKS_OVERDUE navigates to /tasks?tab=WORKLIST&bucket=OVERDUE for practice admin', () => {
      const url = buildMetricUrl('TASKS_OVERDUE', { isStaff: false });
      assert.strictEqual(url, '/tasks?tab=WORKLIST&bucket=OVERDUE');
    });

    it('TASKS_OVERDUE navigates to /tasks?tab=WORKLIST&scope=MY_WORK&bucket=OVERDUE for staff member', () => {
      const url = buildMetricUrl('TASKS_OVERDUE', { isStaff: true });
      assert.strictEqual(url, '/tasks?tab=WORKLIST&scope=MY_WORK&bucket=OVERDUE');
    });

    it('TASKS_COMPLETED navigates to /tasks?tab=WORKLIST&bucket=COMPLETED for practice admin', () => {
      const url = buildMetricUrl('TASKS_COMPLETED', { isStaff: false });
      assert.strictEqual(url, '/tasks?tab=WORKLIST&bucket=COMPLETED');
    });

    it('TASKS_COMPLETED navigates to /tasks?tab=WORKLIST&scope=MY_WORK&bucket=COMPLETED for staff member', () => {
      const url = buildMetricUrl('TASKS_COMPLETED', { isStaff: true });
      assert.strictEqual(url, '/tasks?tab=WORKLIST&scope=MY_WORK&bucket=COMPLETED');
    });
  });

  describe('Employee Workload Allocation Routing', () => {
    const employeeId = 'emp-uuid-101';

    it('EMPLOYEE_ASSIGNED navigates to /tasks?tab=ALL_TASKS&assignedTo={employeeId}', () => {
      const url = buildMetricUrl('EMPLOYEE_ASSIGNED', { employeeId });
      assert.strictEqual(url, `/tasks?tab=ALL_TASKS&assignedTo=${employeeId}`);
    });

    it('EMPLOYEE_PENDING navigates to /tasks?tab=ALL_TASKS&assignedTo={employeeId}&status=TODO', () => {
      const url = buildMetricUrl('EMPLOYEE_PENDING', { employeeId });
      assert.strictEqual(url, `/tasks?tab=ALL_TASKS&assignedTo=${employeeId}&status=TODO`);
    });

    it('EMPLOYEE_OVERDUE navigates to /tasks?tab=WORKLIST&scope=TEAM_WORK&assignedTo={employeeId}&bucket=OVERDUE', () => {
      const url = buildMetricUrl('EMPLOYEE_OVERDUE', { employeeId });
      assert.strictEqual(url, `/tasks?tab=WORKLIST&scope=TEAM_WORK&assignedTo=${employeeId}&bucket=OVERDUE`);
    });

    it('falls back cleanly if employeeId is missing', () => {
      const urlAssigned = buildMetricUrl('EMPLOYEE_ASSIGNED');
      assert.strictEqual(urlAssigned, '/tasks?tab=ALL_TASKS');

      const urlPending = buildMetricUrl('EMPLOYEE_PENDING');
      assert.strictEqual(urlPending, '/tasks?tab=ALL_TASKS&status=TODO');

      const urlOverdue = buildMetricUrl('EMPLOYEE_OVERDUE');
      assert.strictEqual(urlOverdue, '/tasks?tab=WORKLIST&bucket=OVERDUE');
    });
  });

  describe('Zero Count Navigation Handling', () => {
    it('All metrics generate navigable URLs even when value is 0', () => {
      // In Taxoryn UX standard, 0 values must remain clickable to display destination empty state
      const metrics: MetricType[] = [
        'CLIENTS_ACTIVE',
        'GST_FILED',
        'GST_DUE',
        'GST_OVERDUE',
        'ITR_FILED',
        'ITR_PENDING',
        'ITR_OVERDUE',
        'TDS_FILED',
        'TDS_PENDING',
        'TDS_OVERDUE',
        'BILLING_OUTSTANDING',
        'BILLING_OVERDUE',
        'BILLING_COLLECTED',
        'TASKS_TOTAL',
        'TASKS_PENDING',
        'TASKS_OVERDUE',
        'TASKS_COMPLETED',
      ];

      for (const metric of metrics) {
        const url = buildMetricUrl(metric);
        assert.ok(url.startsWith('/'), `URL for ${metric} must be a valid path starting with '/'`);
      }
    });
  });
});
