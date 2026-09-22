import React, { useState, useEffect } from 'react';
import {
  Scale,
  Shield,
  Clock,
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
  Save,
  Bell,
  LayoutDashboard,
  CheckSquare,
  FileCheck,
  AlertCircle,
  Sparkles,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { taxNoticeConfigApi } from '../api/endpoints';
import { TaxNoticeConfig, UpdateTaxNoticeConfigRequest, NoticePriority } from '../types';
import clsx from 'clsx';

export const TaxNoticeSettingsPage: React.FC = () => {
  const [config, setConfig] = useState<TaxNoticeConfig | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    fetchConfig();
  }, []);

  const fetchConfig = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await taxNoticeConfigApi.getConfig();
      setConfig(data);
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to load tax notice configuration');
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = (field: keyof UpdateTaxNoticeConfigRequest) => {
    if (!config) return;
    setConfig({
      ...config,
      [field]: !config[field as keyof TaxNoticeConfig],
    });
  };

  const handleNumberChange = (field: keyof UpdateTaxNoticeConfigRequest, value: number) => {
    if (!config) return;
    setConfig({
      ...config,
      [field]: isNaN(value) ? 0 : value,
    });
  };

  const handlePriorityChange = (priority: NoticePriority) => {
    if (!config) return;
    setConfig({
      ...config,
      defaultPriority: priority,
    });
  };

  const handleSave = async () => {
    if (!config) return;
    try {
      setSaving(true);
      setError(null);
      setSuccessMessage(null);

      const payload: UpdateTaxNoticeConfigRequest = {
        responseReviewRequired: config.responseReviewRequired,
        partnerApprovalRequired: config.partnerApprovalRequired,
        hearingTrackingEnabled: config.hearingTrackingEnabled,
        responseSubmissionTrackingEnabled: config.responseSubmissionTrackingEnabled,
        defaultResponseDueDays: config.defaultResponseDueDays,
        reminderDaysBeforeDue: config.reminderDaysBeforeDue,
        escalationDaysAfterDue: config.escalationDaysAfterDue,
        autoCreateResponseTask: config.autoCreateResponseTask,
        defaultPriority: config.defaultPriority,
        assignmentRequired: config.assignmentRequired,
        notifyOnAssignment: config.notifyOnAssignment,
        notifyOnDueSoon: config.notifyOnDueSoon,
        notifyOnOverdue: config.notifyOnOverdue,
        notifyOnSubmission: config.notifyOnSubmission,
        notifyOnHearing: config.notifyOnHearing,
        showDueSoon: config.showDueSoon,
        showOverdue: config.showOverdue,
        showAwaitingResponse: config.showAwaitingResponse,
        showAwaitingHearing: config.showAwaitingHearing,
        showAwaitingOrder: config.showAwaitingOrder,
      };

      const updated = await taxNoticeConfigApi.updateConfig(payload);
      setConfig(updated);
      setSuccessMessage('Tax notice operations settings updated successfully.');
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to save configuration');
    } finally {
      setSaving(false);
    }
  };

  const handleReset = async () => {
    if (!window.confirm('Reset tax notice configuration back to persona defaults? Custom settings will be removed.')) {
      return;
    }
    try {
      setResetting(true);
      setError(null);
      setSuccessMessage(null);
      const reset = await taxNoticeConfigApi.resetToPersonaDefaults();
      setConfig(reset);
      setSuccessMessage('Tax notice configuration reset to persona defaults.');
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to reset configuration');
    } finally {
      setResetting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-12 text-slate-400">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-500 mr-3" />
        <span>Loading notice operations settings...</span>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2.5">
            <Scale className="w-6 h-6 text-brand-500" />
            Tax Notice Operations Settings
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Configure response workflow stages, automated due date calculation, staff review requirements, and alerts.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            onClick={handleReset}
            disabled={saving || resetting || !config?.isCustomized}
            className="flex items-center gap-2"
          >
            <RotateCcw className="w-4 h-4" />
            Reset to Persona Defaults
          </Button>

          <Button
            onClick={handleSave}
            disabled={saving || resetting}
            className="flex items-center gap-2 bg-brand-500 hover:bg-brand-600 text-white"
          >
            <Save className="w-4 h-4" />
            {saving ? 'Saving...' : 'Save Settings'}
          </Button>
        </div>
      </div>

      {/* Persona Context Banner */}
      {config && (
        <div className={clsx(
          'p-4 rounded-xl border flex items-center justify-between',
          config.isCustomized
            ? 'bg-amber-500/10 border-amber-500/20 text-amber-900 dark:text-amber-200'
            : 'bg-indigo-500/10 border-indigo-500/20 text-indigo-900 dark:text-indigo-200'
        )}>
          <div className="flex items-center gap-3">
            <Sparkles className="w-5 h-5 text-brand-500 shrink-0" />
            <div>
              <span className="font-semibold text-sm">
                Configuration Mode: {config.isCustomized ? 'Custom Organization Overrides' : `Persona Defaults (${config.organizationType || 'Standard'})`}
              </span>
              <p className="text-xs opacity-90 mt-0.5">
                {config.isCustomized
                  ? 'Your organization has tailored notice settings. Changing your Organization Type will not overwrite these custom settings.'
                  : 'Currently applying optimal workflow presets for your organization profile. Modify any setting below to create a custom override.'}
              </p>
            </div>
          </div>
          <span className="text-xs px-2.5 py-1 rounded-full font-medium bg-slate-900/10 dark:bg-white/10 uppercase tracking-wider">
            {config.organizationType || 'DEFAULT'}
          </span>
        </div>
      )}

      {/* Notifications / Alerts */}
      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-sm flex items-center gap-2">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {successMessage && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-sm flex items-center gap-2">
          <CheckCircle2 className="w-5 h-5 shrink-0" />
          <span>{successMessage}</span>
        </div>
      )}

      {config && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Section 1: Response & Review Workflow */}
          <Card className="p-6 space-y-5">
            <div className="flex items-center gap-2.5 pb-3 border-b border-slate-200 dark:border-slate-800">
              <Shield className="w-5 h-5 text-indigo-500" />
              <h2 className="text-base font-semibold text-slate-900 dark:text-white">
                Response Workflow & Sign-Offs
              </h2>
            </div>

            <div className="space-y-4">
              <label className="flex items-start justify-between gap-4 cursor-pointer">
                <div>
                  <span className="font-medium text-sm text-slate-900 dark:text-slate-200">
                    Require Internal Response Review
                  </span>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Response drafts must be reviewed by a designated reviewer before submission.
                  </p>
                </div>
                <input
                  type="checkbox"
                  checked={config.responseReviewRequired}
                  onChange={() => handleToggle('responseReviewRequired')}
                  className="mt-1 h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
              </label>

              <label className="flex items-start justify-between gap-4 cursor-pointer">
                <div>
                  <span className="font-medium text-sm text-slate-900 dark:text-slate-200">
                    Mandatory Partner Approval
                  </span>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Enforces Partner sign-off on response drafts prior to official submission to the department.
                  </p>
                </div>
                <input
                  type="checkbox"
                  checked={config.partnerApprovalRequired}
                  onChange={() => handleToggle('partnerApprovalRequired')}
                  className="mt-1 h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
              </label>

              <label className="flex items-start justify-between gap-4 cursor-pointer">
                <div>
                  <span className="font-medium text-sm text-slate-900 dark:text-slate-200">
                    Hearing & Proceedings Tracking
                  </span>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Enable virtual VC and in-person hearing schedule management and outcome recording.
                  </p>
                </div>
                <input
                  type="checkbox"
                  checked={config.hearingTrackingEnabled}
                  onChange={() => handleToggle('hearingTrackingEnabled')}
                  className="mt-1 h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
              </label>

              <label className="flex items-start justify-between gap-4 cursor-pointer">
                <div>
                  <span className="font-medium text-sm text-slate-900 dark:text-slate-200">
                    Response Submission Proof Tracking
                  </span>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Track portal acknowledgement receipts and upload submission proof documents.
                  </p>
                </div>
                <input
                  type="checkbox"
                  checked={config.responseSubmissionTrackingEnabled}
                  onChange={() => handleToggle('responseSubmissionTrackingEnabled')}
                  className="mt-1 h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
              </label>
            </div>
          </Card>

          {/* Section 2: Deadline & Reminder Management */}
          <Card className="p-6 space-y-5">
            <div className="flex items-center gap-2.5 pb-3 border-b border-slate-200 dark:border-slate-800">
              <Clock className="w-5 h-5 text-amber-500" />
              <h2 className="text-base font-semibold text-slate-900 dark:text-white">
                Deadline & Reminder Defaults
              </h2>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1">
                  Default Response Due Window (Days)
                </label>
                <input
                  type="number"
                  min="1"
                  max="365"
                  value={config.defaultResponseDueDays}
                  onChange={(e) => handleNumberChange('defaultResponseDueDays', parseInt(e.target.value, 10))}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-brand-500"
                />
                <p className="text-xs text-slate-500 mt-1">
                  Used when no explicit statutory response deadline is specified on notice intake.
                </p>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1">
                  Due Soon Alert Threshold (Days Before Due)
                </label>
                <input
                  type="number"
                  min="1"
                  max="60"
                  value={config.reminderDaysBeforeDue}
                  onChange={(e) => handleNumberChange('reminderDaysBeforeDue', parseInt(e.target.value, 10))}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-brand-500"
                />
                <p className="text-xs text-slate-500 mt-1">
                  Flags notice as upcoming priority and dispatches reminder notifications.
                </p>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1">
                  Overdue Escalation Grace Period (Days After Due)
                </label>
                <input
                  type="number"
                  min="0"
                  max="60"
                  value={config.escalationDaysAfterDue}
                  onChange={(e) => handleNumberChange('escalationDaysAfterDue', parseInt(e.target.value, 10))}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-brand-500"
                />
              </div>
            </div>
          </Card>

          {/* Section 3: Task Automation & Assignment */}
          <Card className="p-6 space-y-5">
            <div className="flex items-center gap-2.5 pb-3 border-b border-slate-200 dark:border-slate-800">
              <CheckSquare className="w-5 h-5 text-emerald-500" />
              <h2 className="text-base font-semibold text-slate-900 dark:text-white">
                Task Automation & Assignment
              </h2>
            </div>

            <div className="space-y-4">
              <label className="flex items-start justify-between gap-4 cursor-pointer">
                <div>
                  <span className="font-medium text-sm text-slate-900 dark:text-slate-200">
                    Auto-Create Response Prep Task
                  </span>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Automatically generate a linked practice task for assigned staff upon notice registration.
                  </p>
                </div>
                <input
                  type="checkbox"
                  checked={config.autoCreateResponseTask}
                  onChange={() => handleToggle('autoCreateResponseTask')}
                  className="mt-1 h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
              </label>

              <label className="flex items-start justify-between gap-4 cursor-pointer">
                <div>
                  <span className="font-medium text-sm text-slate-900 dark:text-slate-200">
                    Require Employee Assignment on Intake
                  </span>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Enforces assigning a staff member before a new notice can be registered.
                  </p>
                </div>
                <input
                  type="checkbox"
                  checked={config.assignmentRequired}
                  onChange={() => handleToggle('assignmentRequired')}
                  className="mt-1 h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
              </label>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1">
                  Default Intake Priority
                </label>
                <select
                  value={config.defaultPriority}
                  onChange={(e) => handlePriorityChange(e.target.value as NoticePriority)}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-brand-500"
                >
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                  <option value="CRITICAL">Critical</option>
                </select>
              </div>
            </div>
          </Card>

          {/* Section 4: Compliance Notifications */}
          <Card className="p-6 space-y-5">
            <div className="flex items-center gap-2.5 pb-3 border-b border-slate-200 dark:border-slate-800">
              <Bell className="w-5 h-5 text-purple-500" />
              <h2 className="text-base font-semibold text-slate-900 dark:text-white">
                Compliance Notifications
              </h2>
            </div>

            <div className="space-y-3">
              <label className="flex items-center justify-between gap-4 cursor-pointer py-1">
                <span className="text-sm text-slate-900 dark:text-slate-200">Alert on Notice Assignment</span>
                <input
                  type="checkbox"
                  checked={config.notifyOnAssignment}
                  onChange={() => handleToggle('notifyOnAssignment')}
                  className="h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
              </label>

              <label className="flex items-center justify-between gap-4 cursor-pointer py-1">
                <span className="text-sm text-slate-900 dark:text-slate-200">Alert When Response Due Soon</span>
                <input
                  type="checkbox"
                  checked={config.notifyOnDueSoon}
                  onChange={() => handleToggle('notifyOnDueSoon')}
                  className="h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
              </label>

              <label className="flex items-center justify-between gap-4 cursor-pointer py-1">
                <span className="text-sm text-slate-900 dark:text-slate-200">Alert on Overdue Notice</span>
                <input
                  type="checkbox"
                  checked={config.notifyOnOverdue}
                  onChange={() => handleToggle('notifyOnOverdue')}
                  className="h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
              </label>

              <label className="flex items-center justify-between gap-4 cursor-pointer py-1">
                <span className="text-sm text-slate-900 dark:text-slate-200">Alert on Response Submission</span>
                <input
                  type="checkbox"
                  checked={config.notifyOnSubmission}
                  onChange={() => handleToggle('notifyOnSubmission')}
                  className="h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
              </label>

              <label className="flex items-center justify-between gap-4 cursor-pointer py-1">
                <span className="text-sm text-slate-900 dark:text-slate-200">Alert on Hearing Scheduled / Updated</span>
                <input
                  type="checkbox"
                  checked={config.notifyOnHearing}
                  onChange={() => handleToggle('notifyOnHearing')}
                  className="h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
              </label>
            </div>
          </Card>

          {/* Section 5: Notice Dashboard Widgets (Full Width) */}
          <Card className="p-6 space-y-5 lg:col-span-2">
            <div className="flex items-center gap-2.5 pb-3 border-b border-slate-200 dark:border-slate-800">
              <LayoutDashboard className="w-5 h-5 text-cyan-500" />
              <h2 className="text-base font-semibold text-slate-900 dark:text-white">
                Notice Center Dashboard Metrics & Widgets
              </h2>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
              <label className="flex items-center gap-3 p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={config.showDueSoon}
                  onChange={() => handleToggle('showDueSoon')}
                  className="h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
                <span className="text-xs font-medium text-slate-800 dark:text-slate-200">Due Soon Widget</span>
              </label>

              <label className="flex items-center gap-3 p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={config.showOverdue}
                  onChange={() => handleToggle('showOverdue')}
                  className="h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
                <span className="text-xs font-medium text-slate-800 dark:text-slate-200">Overdue Widget</span>
              </label>

              <label className="flex items-center gap-3 p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={config.showAwaitingResponse}
                  onChange={() => handleToggle('showAwaitingResponse')}
                  className="h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
                <span className="text-xs font-medium text-slate-800 dark:text-slate-200">Awaiting Response</span>
              </label>

              <label className="flex items-center gap-3 p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={config.showAwaitingHearing}
                  onChange={() => handleToggle('showAwaitingHearing')}
                  className="h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
                <span className="text-xs font-medium text-slate-800 dark:text-slate-200">Awaiting Hearing</span>
              </label>

              <label className="flex items-center gap-3 p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={config.showAwaitingOrder}
                  onChange={() => handleToggle('showAwaitingOrder')}
                  className="h-4 w-4 rounded border-slate-300 text-brand-500 focus:ring-brand-500"
                />
                <span className="text-xs font-medium text-slate-800 dark:text-slate-200">Awaiting Final Order</span>
              </label>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
};
