import React, { useState, useEffect, useMemo } from 'react';
import {
  Search,
  Filter,
  Plus,
  BookOpen,
  CheckCircle2,
  AlertCircle,
  Clock,
  ShieldCheck,
  Building2,
  FileText,
  Calendar,
  Layers,
  ChevronRight,
  Info,
  X,
  Edit2,
  Trash2,
  Sparkles,
} from 'lucide-react';
import {
  ComplianceRuleDto,
  ComplianceRuleCatalogSummaryDto,
  ComplianceRuleDomain,
  ComplianceRuleFrequency,
  ComplianceRuleStatus,
  DueDateRuleType,
  CompliancePeriodType,
  CreateComplianceRuleRequest,
  UpdateComplianceRuleRequest,
} from '../types';
import { complianceRulesApi } from '../api/endpoints';

export const ComplianceRuleCatalogPage: React.FC = () => {
  const [rules, setRules] = useState<ComplianceRuleDto[]>([]);
  const [summary, setSummary] = useState<ComplianceRuleCatalogSummaryDto | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ message, type });
    setTimeout(() => {
      setToast(null);
    }, 4000);
  };

  // Filters
  const [search, setSearch] = useState<string>('');
  const [selectedDomain, setSelectedDomain] = useState<string>('ALL');
  const [selectedFrequency, setSelectedFrequency] = useState<string>('ALL');
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [includeSystem, setIncludeSystem] = useState<boolean>(true);

  // Modals & Detail state
  const [viewingRule, setViewingRule] = useState<ComplianceRuleDto | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false);
  const [editingRule, setEditingRule] = useState<ComplianceRuleDto | null>(null);

  // Create Form State
  const [formCode, setFormCode] = useState<string>('');
  const [formName, setFormName] = useState<string>('');
  const [formDomain, setFormDomain] = useState<ComplianceRuleDomain>('GST');
  const [formFrequency, setFormFrequency] = useState<ComplianceRuleFrequency>('MONTHLY');
  const [formPeriodType, setFormPeriodType] = useState<CompliancePeriodType>('MONTH');
  const [formDescription, setFormDescription] = useState<string>('');
  const [formAct, setFormAct] = useState<string>('');
  const [formSection, setFormSection] = useState<string>('');
  const [formFormCode, setFormFormCode] = useState<string>('');
  const [formPenalty, setFormPenalty] = useState<string>('');
  const [formDueDateType, setFormDueDateType] = useState<DueDateRuleType>('DAY_OF_FOLLOWING_MONTH');
  const [formDueDayOffset, setFormDueDayOffset] = useState<number>(20);
  const [formDueMonthOffset, setFormDueMonthOffset] = useState<number>(1);
  const [formFixedMonth, setFormFixedMonth] = useState<number>(7);
  const [formFixedDay, setFormFixedDay] = useState<number>(31);
  const [formDueDateDesc, setFormDueDateDesc] = useState<string>('');
  const [formWorkTemplate, setFormWorkTemplate] = useState<string>('');
  const [submitting, setSubmitting] = useState<boolean>(false);

  const fetchCatalog = async () => {
    try {
      setLoading(true);
      const [rulesData, summaryData] = await Promise.all([
        complianceRulesApi.getRules({
          domain: selectedDomain !== 'ALL' ? (selectedDomain as ComplianceRuleDomain) : undefined,
          frequency: selectedFrequency !== 'ALL' ? (selectedFrequency as ComplianceRuleFrequency) : undefined,
          status: selectedStatus !== 'ALL' ? (selectedStatus as ComplianceRuleStatus) : undefined,
          search: search.trim() ? search.trim() : undefined,
          includeSystem: includeSystem,
        }),
        complianceRulesApi.getCatalogSummary(),
      ]);
      setRules(rulesData || []);
      setSummary(summaryData || null);
    } catch (err: any) {
      showToast(err.response?.data?.message || 'Failed to load compliance rule catalog', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCatalog();
  }, [selectedDomain, selectedFrequency, selectedStatus, includeSystem]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchCatalog();
  };

  const handleOpenCreate = () => {
    setFormCode('');
    setFormName('');
    setFormDomain('GST');
    setFormFrequency('MONTHLY');
    setFormPeriodType('MONTH');
    setFormDescription('');
    setFormAct('');
    setFormSection('');
    setFormFormCode('');
    setFormPenalty('');
    setFormDueDateType('DAY_OF_FOLLOWING_MONTH');
    setFormDueDayOffset(20);
    setFormDueMonthOffset(1);
    setFormFixedMonth(7);
    setFormFixedDay(31);
    setFormDueDateDesc('');
    setFormWorkTemplate('');
    setEditingRule(null);
    setIsCreateModalOpen(true);
  };

  const handleOpenEdit = (rule: ComplianceRuleDto) => {
    setEditingRule(rule);
    setFormCode(rule.ruleCode);
    setFormName(rule.ruleName);
    setFormDomain(rule.domain);
    setFormFrequency(rule.frequency);
    setFormPeriodType(rule.periodType);
    setFormDescription(rule.description || '');
    setFormAct(rule.statutoryAct || '');
    setFormSection(rule.statutorySection || '');
    setFormFormCode(rule.statutoryFormCode || '');
    setFormPenalty(rule.penaltyDetails || '');
    setFormDueDateType(rule.dueDateRuleType || 'DAY_OF_FOLLOWING_MONTH');
    setFormDueDayOffset(rule.dueDayOffset || 20);
    setFormDueMonthOffset(rule.dueMonthOffset || 1);
    setFormFixedMonth(rule.fixedMonth || 7);
    setFormFixedDay(rule.fixedDay || 31);
    setFormDueDateDesc(rule.dueDateDescription || '');
    setFormWorkTemplate(rule.defaultWorkTemplateCode || '');
    setIsCreateModalOpen(true);
  };

  const handleSaveRule = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formName.trim()) {
      showToast('Rule name is required', 'error');
      return;
    }

    try {
      setSubmitting(true);
      if (editingRule) {
        const payload: UpdateComplianceRuleRequest = {
          ruleName: formName.trim(),
          domain: formDomain,
          frequency: formFrequency,
          periodType: formPeriodType,
          description: formDescription.trim() || undefined,
          statutoryAct: formAct.trim() || undefined,
          statutorySection: formSection.trim() || undefined,
          statutoryFormCode: formFormCode.trim() || undefined,
          penaltyDetails: formPenalty.trim() || undefined,
          dueDateRuleType: formDueDateType,
          dueDayOffset: formDueDayOffset,
          dueMonthOffset: formDueMonthOffset,
          fixedMonth: formFixedMonth,
          fixedDay: formFixedDay,
          dueDateDescription: formDueDateDesc.trim() || undefined,
          defaultWorkTemplateCode: formWorkTemplate.trim() || undefined,
        };
        await complianceRulesApi.updateRule(editingRule.id, payload);
        showToast(`Rule '${formCode}' updated successfully`, 'success');
      } else {
        if (!formCode.trim()) {
          showToast('Rule code is required', 'error');
          return;
        }
        const payload: CreateComplianceRuleRequest = {
          ruleCode: formCode.trim().toUpperCase(),
          ruleName: formName.trim(),
          domain: formDomain,
          frequency: formFrequency,
          periodType: formPeriodType,
          description: formDescription.trim() || undefined,
          statutoryAct: formAct.trim() || undefined,
          statutorySection: formSection.trim() || undefined,
          statutoryFormCode: formFormCode.trim() || undefined,
          penaltyDetails: formPenalty.trim() || undefined,
          dueDateRuleType: formDueDateType,
          dueDayOffset: formDueDayOffset,
          dueMonthOffset: formDueMonthOffset,
          fixedMonth: formFixedMonth,
          fixedDay: formFixedDay,
          dueDateDescription: formDueDateDesc.trim() || undefined,
          defaultWorkTemplateCode: formWorkTemplate.trim() || undefined,
        };
        await complianceRulesApi.createCustomRule(payload);
        showToast(`Custom rule '${formCode.toUpperCase()}' created successfully`, 'success');
      }
      setIsCreateModalOpen(false);
      fetchCatalog();
    } catch (err: any) {
      showToast(err.response?.data?.message || 'Failed to save compliance rule', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeactivate = async (rule: ComplianceRuleDto) => {
    if (!window.confirm(`Are you sure you want to deactivate rule '${rule.ruleCode}'?`)) return;
    try {
      await complianceRulesApi.deleteRule(rule.id);
      showToast(`Rule '${rule.ruleCode}' deactivated`, 'success');
      fetchCatalog();
    } catch (err: any) {
      showToast(err.response?.data?.message || 'Failed to deactivate rule', 'error');
    }
  };

  const getDomainBadgeColor = (domain: ComplianceRuleDomain) => {
    switch (domain) {
      case 'GST':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'TDS':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'INCOME_TAX':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'MCA_ROC':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'STATUTORY_AUDIT':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'PAYROLL_LABOUR':
        return 'bg-teal-50 text-teal-700 border-teal-200';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  };

  const domainCount = useMemo<Record<string, number>>(() => {
    return (summary?.rulesByDomain as Record<string, number>) || {};
  }, [summary]);

  return (
    <div className="space-y-6">
      {/* Toast Notification Banner */}
      {toast && (
        <div
          className={`p-3.5 rounded-xl border flex items-center justify-between text-sm transition-all shadow-sm ${
            toast.type === 'error'
              ? 'bg-rose-50 border-rose-200 text-rose-800'
              : 'bg-emerald-50 border-emerald-200 text-emerald-800'
          }`}
        >
          <div className="flex items-center gap-2">
            {toast.type === 'error' ? (
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            ) : (
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            )}
            <span>{toast.message}</span>
          </div>
          <button
            type="button"
            onClick={() => setToast(null)}
            className="p-1 hover:bg-black/5 rounded-md transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Compliance Rule Catalog</h1>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200">
              <Sparkles className="w-3 h-3" /> Phase 29.2 Standard & Custom
            </span>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Master repository of Indian statutory compliance definitions, frequencies, due-date rules, and work templates.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={handleOpenCreate}
            className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg shadow-sm transition-colors"
          >
            <Plus className="w-4 h-4" />
            Add Custom Rule
          </button>
        </div>
      </div>

      {/* Summary KPI Cards */}
      {summary && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">Total Rules</span>
              <BookOpen className="w-4 h-4 text-indigo-600" />
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold text-slate-900">{summary.totalRules}</span>
              <span className="text-xs text-slate-500 font-medium">in catalog</span>
            </div>
          </div>

          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">Active Rules</span>
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold text-emerald-600">{summary.activeRules}</span>
              <span className="text-xs text-slate-500 font-medium">ready for applicability</span>
            </div>
          </div>

          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">Standard System Rules</span>
              <ShieldCheck className="w-4 h-4 text-blue-600" />
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold text-blue-600">{summary.systemRules}</span>
              <span className="text-xs text-slate-500 font-medium">master Indian rules</span>
            </div>
          </div>

          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">Practice Custom Rules</span>
              <Building2 className="w-4 h-4 text-purple-600" />
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold text-purple-600">{summary.customRules}</span>
              <span className="text-xs text-slate-500 font-medium">tenant tailored</span>
            </div>
          </div>
        </div>
      )}

      {/* Domain Breakdown Pills */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1">
        <button
          type="button"
          onClick={() => setSelectedDomain('ALL')}
          className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-all whitespace-nowrap ${
            selectedDomain === 'ALL'
              ? 'bg-slate-900 text-white border-slate-900 shadow-sm'
              : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
          }`}
        >
          All Domains ({summary?.totalRules || 0})
        </button>
        {[
          { key: 'GST', label: 'GST', count: domainCount['GST'] || 0 },
          { key: 'TDS', label: 'TDS / TCS', count: domainCount['TDS'] || 0 },
          { key: 'INCOME_TAX', label: 'Income Tax', count: domainCount['INCOME_TAX'] || 0 },
          { key: 'MCA_ROC', label: 'MCA / ROC', count: domainCount['MCA_ROC'] || 0 },
          { key: 'STATUTORY_AUDIT', label: 'Statutory Audit', count: domainCount['STATUTORY_AUDIT'] || 0 },
          { key: 'PAYROLL_LABOUR', label: 'Payroll & Labour', count: domainCount['PAYROLL_LABOUR'] || 0 },
          { key: 'OTHER', label: 'Other', count: domainCount['OTHER'] || 0 },
        ].map((d) => (
          <button
            key={d.key}
            type="button"
            onClick={() => setSelectedDomain(d.key)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-all whitespace-nowrap ${
              selectedDomain === d.key
                ? 'bg-indigo-600 text-white border-indigo-600 shadow-sm'
                : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
            }`}
          >
            {d.label} ({d.count})
          </button>
        ))}
      </div>

      {/* Filter & Search Bar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm space-y-4">
        <div className="flex flex-col md:flex-row gap-3 items-center justify-between">
          <form onSubmit={handleSearchSubmit} className="relative flex-1 w-full">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search rules by code (e.g. GST_GSTR3B), name, statutory act, or form code..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-sm text-slate-900 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
            />
          </form>

          <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
            <select
              aria-label="Filter by frequency"
              value={selectedFrequency}
              onChange={(e) => setSelectedFrequency(e.target.value)}
              className="px-3 py-2 border border-slate-200 rounded-lg text-xs text-slate-700 bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
            >
              <option value="ALL">All Frequencies</option>
              <option value="MONTHLY">Monthly</option>
              <option value="QUARTERLY">Quarterly</option>
              <option value="ANNUAL">Annual</option>
              <option value="HALF_YEARLY">Half Yearly</option>
              <option value="EVENT_BASED">Event Based</option>
              <option value="ONE_TIME">One Time</option>
            </select>

            <select
              aria-label="Filter by status"
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="px-3 py-2 border border-slate-200 rounded-lg text-xs text-slate-700 bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
            >
              <option value="ALL">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="DRAFT">Draft</option>
            </select>

            <label className="flex items-center gap-2 text-xs font-medium text-slate-700 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={includeSystem}
                onChange={(e) => setIncludeSystem(e.target.checked)}
                className="w-4 h-4 text-indigo-600 rounded border-slate-300 focus:ring-indigo-500"
              />
              Include Standard System Rules
            </label>
          </div>
        </div>
      </div>

      {/* Rules Table / Catalog View */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-500 text-sm">
            <div className="inline-block animate-spin w-6 h-6 border-2 border-indigo-600 border-t-transparent rounded-full mb-2"></div>
            <p>Loading compliance rules...</p>
          </div>
        ) : rules.length === 0 ? (
          <div className="p-12 text-center">
            <BookOpen className="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <h3 className="text-base font-semibold text-slate-800">No compliance rules found</h3>
            <p className="text-sm text-slate-500 mt-1 max-w-sm mx-auto">
              No rules matched your search query and filter criteria. Try adjusting filters or creating a custom rule.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  <th className="py-3.5 px-4">Rule Code & Classification</th>
                  <th className="py-3.5 px-4">Rule Name & Statutory Act</th>
                  <th className="py-3.5 px-4">Frequency / Period</th>
                  <th className="py-3.5 px-4">Due Date Rule</th>
                  <th className="py-3.5 px-4">Work Template</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {rules.map((rule) => (
                  <tr key={rule.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3.5 px-4">
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-semibold text-slate-900 text-xs">
                          {rule.ruleCode}
                        </span>
                        {rule.systemRule ? (
                          <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-blue-50 text-blue-700 border border-blue-200">
                            STANDARD
                          </span>
                        ) : (
                          <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-purple-50 text-purple-700 border border-purple-200">
                            CUSTOM
                          </span>
                        )}
                      </div>
                      <div className="mt-1">
                        <span className={`inline-block px-2 py-0.5 rounded-full text-[11px] font-medium border ${getDomainBadgeColor(rule.domain)}`}>
                          {rule.domainDisplayName || rule.domain}
                        </span>
                      </div>
                    </td>

                    <td className="py-3.5 px-4">
                      <div className="font-medium text-slate-900">{rule.ruleName}</div>
                      <div className="text-xs text-slate-500 mt-0.5 flex items-center gap-2">
                        {rule.statutoryFormCode && (
                          <span className="font-mono bg-slate-100 px-1.5 py-0.5 rounded text-slate-700 text-[11px]">
                            {rule.statutoryFormCode}
                          </span>
                        )}
                        {rule.statutoryAct && <span>{rule.statutoryAct}</span>}
                        {rule.statutorySection && <span>(Sec {rule.statutorySection})</span>}
                      </div>
                    </td>

                    <td className="py-3.5 px-4 text-slate-700 text-xs">
                      <div className="font-semibold text-slate-800">{rule.frequency}</div>
                      <div className="text-slate-500">Period: {rule.periodType}</div>
                    </td>

                    <td className="py-3.5 px-4 text-slate-700 text-xs">
                      <div className="font-medium text-slate-800">{rule.dueDateDescription || rule.dueDateRuleType}</div>
                      <div className="text-slate-500 text-[11px]">
                        {rule.dueDateRuleType === 'DAY_OF_FOLLOWING_MONTH' && `Day ${rule.dueDayOffset || 20} of next month`}
                        {rule.dueDateRuleType === 'DAY_OF_FOLLOWING_QUARTER_END_MONTH' && `Day ${rule.dueDayOffset || 31} of quarter end`}
                        {rule.dueDateRuleType === 'FIXED_DATE_IN_YEAR' && `${rule.fixedDay}/${rule.fixedMonth} annually`}
                      </div>
                    </td>

                    <td className="py-3.5 px-4 text-xs font-mono text-slate-600">
                      {rule.defaultWorkTemplateCode || '—'}
                    </td>

                    <td className="py-3.5 px-4">
                      {rule.status === 'ACTIVE' ? (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
                          <CheckCircle2 className="w-3 h-3" /> Active
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-600">
                          {rule.status}
                        </span>
                      )}
                    </td>

                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          type="button"
                          onClick={() => setViewingRule(rule)}
                          className="p-1.5 text-slate-600 hover:text-indigo-600 hover:bg-indigo-50 rounded-md transition-colors"
                          title="View Rule Details"
                        >
                          <Info className="w-4 h-4" />
                        </button>
                        {!rule.systemRule && (
                          <>
                            <button
                              type="button"
                              onClick={() => handleOpenEdit(rule)}
                              className="p-1.5 text-slate-600 hover:text-amber-600 hover:bg-amber-50 rounded-md transition-colors"
                              title="Edit Custom Rule"
                            >
                              <Edit2 className="w-4 h-4" />
                            </button>
                            {rule.status === 'ACTIVE' && (
                              <button
                                type="button"
                                onClick={() => handleDeactivate(rule)}
                                className="p-1.5 text-slate-600 hover:text-rose-600 hover:bg-rose-50 rounded-md transition-colors"
                                title="Deactivate Rule"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            )}
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Rule Detail Modal */}
      {viewingRule && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-mono text-base font-bold text-slate-900">{viewingRule.ruleCode}</span>
                  {viewingRule.systemRule ? (
                    <span className="px-2 py-0.5 rounded text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
                      Standard Master Rule
                    </span>
                  ) : (
                    <span className="px-2 py-0.5 rounded text-xs font-semibold bg-purple-50 text-purple-700 border border-purple-200">
                      Practice Custom Rule
                    </span>
                  )}
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium border ${getDomainBadgeColor(viewingRule.domain)}`}>
                    {viewingRule.domainDisplayName || viewingRule.domain}
                  </span>
                </div>
                <h2 className="text-xl font-bold text-slate-900 mt-1">{viewingRule.ruleName}</h2>
              </div>
              <button
                type="button"
                onClick={() => setViewingRule(null)}
                className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6">
              {/* Description */}
              {viewingRule.description && (
                <div>
                  <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Description</h4>
                  <p className="text-sm text-slate-700 bg-slate-50 p-3 rounded-lg border border-slate-100">
                    {viewingRule.description}
                  </p>
                </div>
              )}

              {/* Statutory Reference */}
              <div>
                <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">Statutory Reference</h4>
                <div className="grid grid-cols-3 gap-3 bg-slate-50 p-3.5 rounded-xl border border-slate-100 text-xs">
                  <div>
                    <span className="text-slate-500 block">Statutory Act:</span>
                    <span className="font-semibold text-slate-800">{viewingRule.statutoryAct || '—'}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Section / Rule:</span>
                    <span className="font-semibold text-slate-800">{viewingRule.statutorySection || '—'}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Form Code:</span>
                    <span className="font-semibold text-slate-800 font-mono">{viewingRule.statutoryFormCode || '—'}</span>
                  </div>
                </div>
              </div>

              {/* Frequency & Due Date Engine */}
              <div>
                <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">Due Date & Frequency Engine</h4>
                <div className="grid grid-cols-2 gap-3 bg-indigo-50/50 p-3.5 rounded-xl border border-indigo-100 text-xs">
                  <div>
                    <span className="text-slate-500 block">Frequency:</span>
                    <span className="font-bold text-slate-800">{viewingRule.frequency}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Period Type:</span>
                    <span className="font-bold text-slate-800">{viewingRule.periodType}</span>
                  </div>
                  <div className="col-span-2">
                    <span className="text-slate-500 block">Due Date Rule:</span>
                    <span className="font-semibold text-indigo-900">{viewingRule.dueDateDescription || viewingRule.dueDateRuleType}</span>
                  </div>
                  {viewingRule.statutoryGraceDays ? (
                    <div>
                      <span className="text-slate-500 block">Grace Days:</span>
                      <span className="font-semibold text-slate-800">{viewingRule.statutoryGraceDays} days</span>
                    </div>
                  ) : null}
                </div>
              </div>

              {/* Penalties & Work Template */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Penalty / Late Fee Details</h4>
                  <p className="text-xs text-rose-700 bg-rose-50 p-3 rounded-lg border border-rose-100">
                    {viewingRule.penaltyDetails || 'Standard late fee under relevant section.'}
                  </p>
                </div>
                <div>
                  <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Work Template Linkage</h4>
                  <div className="bg-slate-50 p-3 rounded-lg border border-slate-100 text-xs">
                    <span className="text-slate-500 block">Default Template:</span>
                    <span className="font-mono font-semibold text-slate-800">{viewingRule.defaultWorkTemplateCode || '—'}</span>
                  </div>
                </div>
              </div>

              {/* Metadata */}
              <div className="pt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
                <span>Rule Version: {viewingRule.version || 1}</span>
                <span>Rule ID: {viewingRule.id}</span>
              </div>
            </div>

            <div className="p-4 bg-slate-50 border-t border-slate-100 flex justify-end">
              <button
                type="button"
                onClick={() => setViewingRule(null)}
                className="px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-700 text-sm font-medium rounded-lg transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create / Edit Custom Rule Modal */}
      {isCreateModalOpen && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <form onSubmit={handleSaveRule}>
              <div className="p-6 border-b border-slate-100 flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-bold text-slate-900">
                    {editingRule ? `Edit Custom Rule: ${editingRule.ruleCode}` : 'Create Practice Custom Rule'}
                  </h2>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Define custom compliance rules tailored to your practice and client contracts.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setIsCreateModalOpen(false)}
                  className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100 transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="p-6 space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Rule Code <span className="text-rose-500">*</span>
                    </label>
                    <input
                      type="text"
                      disabled={!!editingRule}
                      placeholder="e.g. CUSTOM_MONTHLY_MIS"
                      value={formCode}
                      onChange={(e) => setFormCode(e.target.value.toUpperCase())}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm font-mono text-slate-900 focus:ring-2 focus:ring-indigo-500 outline-none disabled:bg-slate-100"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Rule Name <span className="text-rose-500">*</span>
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. Monthly Client MIS Reporting"
                      value={formName}
                      onChange={(e) => setFormName(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-900 focus:ring-2 focus:ring-indigo-500 outline-none"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-3 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Domain</label>
                    <select
                      value={formDomain}
                      onChange={(e) => setFormDomain(e.target.value as ComplianceRuleDomain)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs text-slate-900 bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
                    >
                      <option value="GST">GST</option>
                      <option value="TDS">TDS / TCS</option>
                      <option value="INCOME_TAX">Income Tax</option>
                      <option value="MCA_ROC">MCA / ROC</option>
                      <option value="STATUTORY_AUDIT">Statutory Audit</option>
                      <option value="PAYROLL_LABOUR">Payroll & Labour</option>
                      <option value="OTHER">Other Practice Rule</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Frequency</label>
                    <select
                      value={formFrequency}
                      onChange={(e) => setFormFrequency(e.target.value as ComplianceRuleFrequency)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs text-slate-900 bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
                    >
                      <option value="MONTHLY">Monthly</option>
                      <option value="QUARTERLY">Quarterly</option>
                      <option value="ANNUAL">Annual</option>
                      <option value="HALF_YEARLY">Half Yearly</option>
                      <option value="EVENT_BASED">Event Based</option>
                      <option value="ONE_TIME">One Time</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Period Type</label>
                    <select
                      value={formPeriodType}
                      onChange={(e) => setFormPeriodType(e.target.value as CompliancePeriodType)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs text-slate-900 bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
                    >
                      <option value="MONTH">Month</option>
                      <option value="QUARTER">Quarter</option>
                      <option value="FINANCIAL_YEAR">Financial Year</option>
                      <option value="ASSESSMENT_YEAR">Assessment Year</option>
                      <option value="HALF_YEAR">Half Year</option>
                      <option value="EVENT">Event</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Description</label>
                  <textarea
                    rows={2}
                    placeholder="Provide details and scope of this compliance rule..."
                    value={formDescription}
                    onChange={(e) => setFormDescription(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs text-slate-900 focus:ring-2 focus:ring-indigo-500 outline-none"
                  />
                </div>

                <div className="grid grid-cols-3 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Statutory Act</label>
                    <input
                      type="text"
                      placeholder="e.g. CGST Act, 2017"
                      value={formAct}
                      onChange={(e) => setFormAct(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs text-slate-900 outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Section</label>
                    <input
                      type="text"
                      placeholder="e.g. Sec 39(1)"
                      value={formSection}
                      onChange={(e) => setFormSection(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs text-slate-900 outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Form Code</label>
                    <input
                      type="text"
                      placeholder="e.g. GSTR-3B"
                      value={formFormCode}
                      onChange={(e) => setFormFormCode(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs font-mono text-slate-900 outline-none"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Due Date Calculation Rule</label>
                    <select
                      value={formDueDateType}
                      onChange={(e) => setFormDueDateType(e.target.value as DueDateRuleType)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs text-slate-900 bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
                    >
                      <option value="DAY_OF_FOLLOWING_MONTH">Day of Following Month</option>
                      <option value="DAY_OF_FOLLOWING_QUARTER_END_MONTH">Day of Following Quarter-End Month</option>
                      <option value="FIXED_DATE_IN_YEAR">Fixed Date in Year</option>
                      <option value="CUSTOM_OFFSET_DAYS">Custom Offset Days</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Due Day Offset / Day</label>
                    <input
                      type="number"
                      value={formDueDayOffset}
                      onChange={(e) => setFormDueDayOffset(parseInt(e.target.value) || 0)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs text-slate-900 outline-none"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Due Date Description</label>
                  <input
                    type="text"
                    placeholder="e.g. 20th of the following month"
                    value={formDueDateDesc}
                    onChange={(e) => setFormDueDateDesc(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs text-slate-900 outline-none"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Penalty / Late Fee Details</label>
                    <input
                      type="text"
                      placeholder="e.g. ₹50/day up to maximum ₹5,000"
                      value={formPenalty}
                      onChange={(e) => setFormPenalty(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs text-slate-900 outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Work Template Code</label>
                    <input
                      type="text"
                      placeholder="e.g. TEMPLATE_GST_FILING"
                      value={formWorkTemplate}
                      onChange={(e) => setFormWorkTemplate(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs font-mono text-slate-900 outline-none"
                    />
                  </div>
                </div>
              </div>

              <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsCreateModalOpen(false)}
                  className="px-4 py-2 text-slate-600 hover:text-slate-800 text-sm font-medium rounded-lg hover:bg-slate-200/60 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg shadow-sm transition-colors disabled:opacity-50"
                >
                  {submitting ? 'Saving...' : editingRule ? 'Update Rule' : 'Create Custom Rule'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
export default ComplianceRuleCatalogPage;
