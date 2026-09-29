import React, { useState, useEffect, useCallback } from 'react';
import {
  Layers,
  Plus,
  Search,
  Filter,
  CheckCircle2,
  Clock,
  Briefcase,
  Edit3,
  Trash2,
  ArrowUp,
  ArrowDown,
  AlertCircle,
  MoreVertical,
  Calendar,
  ShieldCheck,
  Tag,
  ChevronRight,
  Sparkles,
  Check,
  X,
} from 'lucide-react';
import { workTemplatesApi, servicesApi } from '../api/endpoints';
import {
  WorkTemplateDto,
  WorkTemplateTaskDto,
  WorkTemplateStatusType,
  WorkTemplateType,
  RecurrenceType,
  ServiceDto,
  ServiceCategoryType,
  CreateWorkTemplatePayload,
  UpdateWorkTemplatePayload,
  CreateWorkTemplateTaskPayload,
  UpdateWorkTemplateTaskPayload,
} from '../types';
import { StatusBadge } from '../components/common/StatusBadge';
import { Modal } from '../components/common/Modal';
import clsx from 'clsx';

export const WorkTemplatesPage: React.FC = () => {
  // Filter States
  const [search, setSearch] = useState<string>('');
  const [categoryFilter, setCategoryFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [recurrenceFilter, setRecurrenceFilter] = useState<string>('');

  // Data States
  const [templates, setTemplates] = useState<WorkTemplateDto[]>([]);
  const [services, setServices] = useState<ServiceDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Create / Edit Template Modal
  const [isTemplateModalOpen, setIsTemplateModalOpen] = useState<boolean>(false);
  const [editingTemplate, setEditingTemplate] = useState<WorkTemplateDto | null>(null);
  const [templateForm, setTemplateForm] = useState<CreateWorkTemplatePayload>({
    serviceId: '',
    name: '',
    description: '',
    category: 'GST',
    status: 'ACTIVE',
    templateType: 'STATUTORY_COMPLIANCE',
    recurrenceType: 'MONTHLY',
    recurrenceInterval: 1,
    recurrenceEnabled: true,
  });
  const [isSubmittingTemplate, setIsSubmittingTemplate] = useState<boolean>(false);

  // Task Configuration Modal
  const [activeConfigTemplate, setActiveConfigTemplate] = useState<WorkTemplateDto | null>(null);
  const [templateTasks, setTemplateTasks] = useState<WorkTemplateTaskDto[]>([]);
  const [isLoadingTasks, setIsLoadingTasks] = useState<boolean>(false);
  const [isAddingTask, setIsAddingTask] = useState<boolean>(false);
  const [newTaskForm, setNewTaskForm] = useState<CreateWorkTemplateTaskPayload>({
    name: '',
    description: '',
    defaultAssigneeRole: 'STAFF',
    defaultPriority: 'MEDIUM',
    relativeDueDays: 5,
    mandatory: true,
  });

  // Load Metadata
  useEffect(() => {
    servicesApi.getAll().then(setServices).catch(console.error);
  }, []);

  // Load Templates
  const loadTemplates = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await workTemplatesApi.getAll({
        search: search.trim() || undefined,
        category: (categoryFilter as ServiceCategoryType) || undefined,
        status: (statusFilter as WorkTemplateStatusType) || undefined,
        recurrenceType: (recurrenceFilter as RecurrenceType) || undefined,
        size: 50,
      });
      setTemplates(response.content || []);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load work templates');
    } finally {
      setIsLoading(false);
    }
  }, [search, categoryFilter, statusFilter, recurrenceFilter]);

  useEffect(() => {
    loadTemplates();
  }, [loadTemplates]);

  // Load Tasks for a Template
  const openTaskConfig = async (tpl: WorkTemplateDto) => {
    setActiveConfigTemplate(tpl);
    setIsLoadingTasks(true);
    try {
      const tasks = await workTemplatesApi.getTasks(tpl.id);
      setTemplateTasks(tasks || []);
    } catch (err) {
      console.error('Failed to load template tasks', err);
    } finally {
      setIsLoadingTasks(false);
    }
  };

  // Open Create Template Modal
  const openCreateModal = () => {
    setEditingTemplate(null);
    setTemplateForm({
      serviceId: services[0]?.id || '',
      name: '',
      description: '',
      category: 'GST',
      status: 'ACTIVE',
      templateType: 'STATUTORY_COMPLIANCE',
      recurrenceType: 'MONTHLY',
      recurrenceInterval: 1,
      recurrenceEnabled: true,
    });
    setIsTemplateModalOpen(true);
  };

  // Open Edit Template Modal
  const openEditModal = (tpl: WorkTemplateDto) => {
    setEditingTemplate(tpl);
    setTemplateForm({
      serviceId: tpl.serviceId,
      templateCode: tpl.templateCode,
      name: tpl.name,
      description: tpl.description || '',
      category: tpl.category,
      status: tpl.status,
      templateType: tpl.templateType,
      recurrenceType: tpl.recurrenceType,
      recurrenceInterval: tpl.recurrenceInterval,
      dayOfMonth: tpl.dayOfMonth,
      monthOfYear: tpl.monthOfYear,
      recurrenceEnabled: tpl.recurrenceEnabled,
    });
    setIsTemplateModalOpen(true);
  };

  // Submit Template Form
  const handleSaveTemplate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!templateForm.serviceId || !templateForm.name.trim()) {
      alert('Please select a service and provide a template name.');
      return;
    }

    try {
      setIsSubmittingTemplate(true);
      if (editingTemplate) {
        await workTemplatesApi.update(editingTemplate.id, templateForm);
      } else {
        await workTemplatesApi.create(templateForm);
      }
      setIsTemplateModalOpen(false);
      loadTemplates();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to save template');
    } finally {
      setIsSubmittingTemplate(false);
    }
  };

  // Toggle Template Status
  const handleToggleStatus = async (tpl: WorkTemplateDto, newStatus: WorkTemplateStatusType) => {
    try {
      await workTemplatesApi.updateStatus(tpl.id, newStatus);
      loadTemplates();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update template status');
    }
  };

  // Add Task
  const handleAddTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeConfigTemplate || !newTaskForm.name.trim()) return;
    try {
      const created = await workTemplatesApi.addTask(activeConfigTemplate.id, newTaskForm);
      setTemplateTasks([...templateTasks, created]);
      setNewTaskForm({
        name: '',
        description: '',
        defaultAssigneeRole: 'STAFF',
        defaultPriority: 'MEDIUM',
        relativeDueDays: 5,
        mandatory: true,
      });
      setIsAddingTask(false);
      loadTemplates();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to add task');
    }
  };

  // Delete Task
  const handleDeleteTask = async (taskId: string) => {
    if (!activeConfigTemplate || !confirm('Are you sure you want to remove this task?')) return;
    try {
      await workTemplatesApi.deleteTask(activeConfigTemplate.id, taskId);
      setTemplateTasks(templateTasks.filter((t) => t.id !== taskId));
      loadTemplates();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to delete task');
    }
  };

  // Move Task Up / Down
  const handleMoveTask = async (index: number, direction: 'up' | 'down') => {
    if (!activeConfigTemplate) return;
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= templateTasks.length) return;

    const newTasks = [...templateTasks];
    const temp = newTasks[index];
    newTasks[index] = newTasks[targetIndex];
    newTasks[targetIndex] = temp;

    const orderedIds = newTasks.map((t) => t.id);
    try {
      const reordered = await workTemplatesApi.reorderTasks(activeConfigTemplate.id, orderedIds);
      setTemplateTasks(reordered);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to reorder tasks');
    }
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight flex items-center gap-2.5">
            <Layers className="w-7 h-7 text-brand-600" />
            <span>Practice Work Templates</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Define standard compliance workflows, recurring filing schedules, and structured task templates.
          </p>
        </div>
        <button
          onClick={openCreateModal}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors"
        >
          <Plus className="w-4 h-4" />
          <span>New Work Template</span>
        </button>
      </div>

      {/* Filter Bar */}
      <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-2xs space-y-3">
        <div className="flex flex-col lg:flex-row items-center gap-3">
          <div className="relative flex-1 w-full">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by template name, code, or description..."
              className="w-full pl-9 pr-4 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-brand-500 bg-slate-50/50"
            />
          </div>

          <div className="flex items-center gap-2 w-full lg:w-auto flex-wrap">
            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              className="px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="">All Categories</option>
              <option value="GST">GST</option>
              <option value="TDS">TDS</option>
              <option value="ITR">Income Tax (ITR)</option>
              <option value="AUDIT">Tax Audit</option>
              <option value="NOTICE">Notice & Representation</option>
              <option value="ADVISORY">Advisory</option>
              <option value="OTHER">Other Compliance</option>
            </select>

            <select
              value={recurrenceFilter}
              onChange={(e) => setRecurrenceFilter(e.target.value)}
              className="px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="">All Frequencies</option>
              <option value="MONTHLY">Monthly</option>
              <option value="QUARTERLY">Quarterly</option>
              <option value="YEARLY">Yearly</option>
              <option value="ONCE">Once / Ad-hoc</option>
            </select>

            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="DRAFT">Draft</option>
              <option value="INACTIVE">Inactive</option>
              <option value="ARCHIVED">Archived</option>
            </select>
          </div>
        </div>
      </div>

      {/* Templates Grid */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-44 rounded-2xl bg-white border border-slate-200 p-5 animate-pulse space-y-3">
              <div className="h-4 bg-slate-200 rounded w-1/3" />
              <div className="h-6 bg-slate-200 rounded w-3/4" />
              <div className="h-4 bg-slate-100 rounded w-1/2" />
            </div>
          ))}
        </div>
      ) : error ? (
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-6 text-center space-y-2">
          <AlertCircle className="w-8 h-8 text-rose-500 mx-auto" />
          <p className="text-xs font-bold text-rose-800">{error}</p>
        </div>
      ) : templates.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-12 text-center space-y-4 shadow-2xs">
          <div className="w-14 h-14 bg-brand-50 text-brand-600 rounded-2xl flex items-center justify-center mx-auto border border-brand-100">
            <Layers className="w-7 h-7" />
          </div>
          <div className="max-w-md mx-auto space-y-1">
            <h3 className="text-sm font-bold text-slate-900">No Work Templates Found</h3>
            <p className="text-xs text-slate-500">
              Create reusable work templates with predefined task sequences and relative due dates to standardize your practice deliverables.
            </p>
          </div>
          <button
            onClick={openCreateModal}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors"
          >
            <Plus className="w-4 h-4" />
            <span>Create First Template</span>
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {templates.map((tpl) => (
            <div
              key={tpl.id}
              className="bg-white border border-slate-200 hover:border-brand-300 rounded-2xl p-5 shadow-2xs hover:shadow-xs transition-all flex flex-col justify-between space-y-4 group"
            >
              <div className="space-y-3">
                {/* Header: Code, System Badge & Status */}
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5 flex-wrap">
                    <span className="font-mono text-[11px] font-bold text-brand-700 bg-brand-50 border border-brand-200 px-2 py-0.5 rounded-lg">
                      {tpl.templateCode}
                    </span>
                    {tpl.isSystemDefault && (
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-purple-50 text-purple-700 border border-purple-200">
                        SYSTEM
                      </span>
                    )}
                  </div>
                  <StatusBadge status={tpl.status} size="sm" />
                </div>

                {/* Template Name & Scope */}
                <div>
                  <h3 className="text-sm font-bold text-slate-900 group-hover:text-brand-600 transition-colors line-clamp-1">
                    {tpl.name}
                  </h3>
                  {tpl.description && (
                    <p className="text-xs text-slate-500 line-clamp-2 mt-1">{tpl.description}</p>
                  )}
                </div>

                {/* Meta Attributes */}
                <div className="p-3 bg-slate-50 rounded-xl space-y-2 border border-slate-100 text-xs">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400 text-[11px]">Service:</span>
                    <span className="font-bold text-slate-800 truncate max-w-[170px]">
                      {tpl.serviceName || tpl.category}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400 text-[11px]">Recurrence:</span>
                    <span className="font-semibold text-brand-700 bg-brand-50 px-2 py-0.5 rounded-md text-[11px]">
                      {tpl.recurrenceType}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400 text-[11px]">Tasks in Workflow:</span>
                    <span className="font-bold text-slate-800">
                      {tpl.taskCount} {tpl.taskCount === 1 ? 'task' : 'tasks'}
                    </span>
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-xs gap-2">
                <button
                  onClick={() => openTaskConfig(tpl)}
                  className="inline-flex items-center gap-1.5 font-bold text-brand-600 hover:text-brand-700 bg-brand-50 hover:bg-brand-100 px-3 py-1.5 rounded-xl transition-colors"
                >
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>Configure Tasks ({tpl.taskCount})</span>
                </button>

                {!tpl.isSystemDefault && (
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => openEditModal(tpl)}
                      className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg"
                      title="Edit Template Header"
                    >
                      <Edit3 className="w-3.5 h-3.5" />
                    </button>
                    {tpl.status === 'ACTIVE' ? (
                      <button
                        onClick={() => handleToggleStatus(tpl, 'INACTIVE')}
                        className="px-2 py-1 text-[11px] font-bold text-amber-600 hover:bg-amber-50 rounded-lg"
                        title="Deactivate"
                      >
                        Deactivate
                      </button>
                    ) : (
                      <button
                        onClick={() => handleToggleStatus(tpl, 'ACTIVE')}
                        className="px-2 py-1 text-[11px] font-bold text-emerald-600 hover:bg-emerald-50 rounded-lg"
                        title="Activate"
                      >
                        Activate
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create / Edit Template Modal */}
      {isTemplateModalOpen && (
        <Modal
          isOpen={isTemplateModalOpen}
          onClose={() => setIsTemplateModalOpen(false)}
          title={editingTemplate ? 'Edit Work Template' : 'Create Practice Work Template'}
          maxWidth="xl"
        >
          <form onSubmit={handleSaveTemplate} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Associated Service <span className="text-rose-500">*</span>
              </label>
              <select
                required
                value={templateForm.serviceId}
                onChange={(e) => {
                  const selected = services.find((s) => s.id === e.target.value);
                  setTemplateForm({
                    ...templateForm,
                    serviceId: e.target.value,
                    category: selected ? (selected.category as ServiceCategoryType) : templateForm.category,
                  });
                }}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white font-medium focus:ring-2 focus:ring-brand-500"
              >
                <option value="">Select Service Catalog Master</option>
                {services.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.serviceName} ({s.category})
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Template Name <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. GST Monthly Compliance Standard"
                  value={templateForm.name}
                  onChange={(e) => setTemplateForm({ ...templateForm, name: e.target.value })}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-brand-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Recurrence Frequency
                </label>
                <select
                  value={templateForm.recurrenceType}
                  onChange={(e) =>
                    setTemplateForm({ ...templateForm, recurrenceType: e.target.value as RecurrenceType })
                  }
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white font-medium"
                >
                  <option value="MONTHLY">Monthly</option>
                  <option value="QUARTERLY">Quarterly</option>
                  <option value="YEARLY">Yearly</option>
                  <option value="ONCE">Once / Ad-hoc</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Description</label>
              <textarea
                rows={2}
                placeholder="Workflow scope, standard deliverables, and execution instructions..."
                value={templateForm.description}
                onChange={(e) => setTemplateForm({ ...templateForm, description: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-brand-500"
              />
            </div>

            <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setIsTemplateModalOpen(false)}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:text-slate-800 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmittingTemplate}
                className="px-5 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors disabled:opacity-50"
              >
                {isSubmittingTemplate ? 'Saving...' : editingTemplate ? 'Update Template' : 'Create Template'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Task Configuration Modal */}
      {activeConfigTemplate && (
        <Modal
          isOpen={!!activeConfigTemplate}
          onClose={() => {
            setActiveConfigTemplate(null);
            setIsAddingTask(false);
          }}
          title={`Configure Tasks: ${activeConfigTemplate.name}`}
          maxWidth="2xl"
        >
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <p className="text-xs text-slate-500">
                Define the sequence of tasks created whenever this work template generates a work occurrence.
              </p>
              {!activeConfigTemplate.isSystemDefault && !isAddingTask && (
                <button
                  onClick={() => setIsAddingTask(true)}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Add Task</span>
                </button>
              )}
            </div>

            {/* Task Add Form */}
            {isAddingTask && (
              <form onSubmit={handleAddTask} className="p-4 bg-slate-50 border border-slate-200 rounded-2xl space-y-3 text-xs">
                <div className="font-bold text-slate-900 flex items-center justify-between">
                  <span>New Template Task</span>
                  <button
                    type="button"
                    onClick={() => setIsAddingTask(false)}
                    className="p-1 text-slate-400 hover:text-slate-600"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div className="sm:col-span-2">
                    <label className="block text-[11px] font-bold text-slate-700 mb-1">Task Name *</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Collect Purchase Invoices"
                      value={newTaskForm.name}
                      onChange={(e) => setNewTaskForm({ ...newTaskForm, name: e.target.value })}
                      className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-bold text-slate-700 mb-1">Relative Due (Days)</label>
                    <input
                      type="number"
                      required
                      placeholder="+5 days"
                      value={newTaskForm.relativeDueDays}
                      onChange={(e) => setNewTaskForm({ ...newTaskForm, relativeDueDays: Number(e.target.value) })}
                      className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[11px] font-bold text-slate-700 mb-1">Default Priority</label>
                    <select
                      value={newTaskForm.defaultPriority}
                      onChange={(e) => setNewTaskForm({ ...newTaskForm, defaultPriority: e.target.value as any })}
                      className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                    >
                      <option value="LOW">Low</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="HIGH">High</option>
                      <option value="URGENT">Urgent</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-[11px] font-bold text-slate-700 mb-1">Default Role</label>
                    <select
                      value={newTaskForm.defaultAssigneeRole}
                      onChange={(e) => setNewTaskForm({ ...newTaskForm, defaultAssigneeRole: e.target.value })}
                      className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                    >
                      <option value="STAFF">Staff Preparer</option>
                      <option value="MANAGER">Manager</option>
                      <option value="PARTNER">Partner Reviewer</option>
                    </select>
                  </div>
                </div>

                <div className="flex items-center justify-end gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setIsAddingTask(false)}
                    className="px-3 py-1.5 text-xs font-bold text-slate-600 hover:text-slate-800"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-1.5 rounded-xl bg-brand-600 text-white font-bold hover:bg-brand-700"
                  >
                    Save Task
                  </button>
                </div>
              </form>
            )}

            {/* Task Sequence List */}
            {isLoadingTasks ? (
              <div className="py-8 text-center text-xs text-slate-500">Loading task sequence...</div>
            ) : templateTasks.length === 0 ? (
              <div className="py-8 text-center text-xs text-slate-500 bg-slate-50 rounded-2xl">
                No tasks defined in this template yet. Click "Add Task" to start.
              </div>
            ) : (
              <div className="space-y-2 max-h-[50vh] overflow-y-auto pr-1">
                {templateTasks.map((t, idx) => (
                  <div
                    key={t.id}
                    className="flex items-center justify-between p-3 bg-white border border-slate-200 rounded-xl hover:border-slate-300 transition-colors text-xs"
                  >
                    <div className="flex items-center gap-3">
                      <span className="w-6 h-6 rounded-full bg-slate-100 text-slate-600 font-bold text-[11px] flex items-center justify-center shrink-0">
                        {idx + 1}
                      </span>
                      <div className="space-y-0.5">
                        <p className="font-bold text-slate-800">{t.name}</p>
                        <div className="flex items-center gap-2 text-[11px] text-slate-400">
                          <span className="flex items-center gap-1 font-medium text-brand-600">
                            <Clock className="w-3 h-3" />
                            <span>+{t.relativeDueDays} days offset</span>
                          </span>
                          <span>•</span>
                          <span>Priority: <span className="font-semibold text-slate-600">{t.defaultPriority}</span></span>
                          {t.defaultAssigneeRole && (
                            <>
                              <span>•</span>
                              <span>Role: {t.defaultAssigneeRole}</span>
                            </>
                          )}
                        </div>
                      </div>
                    </div>

                    {!activeConfigTemplate.isSystemDefault && (
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => handleMoveTask(idx, 'up')}
                          disabled={idx === 0}
                          className="p-1 text-slate-400 hover:text-slate-700 disabled:opacity-30"
                          title="Move Up"
                        >
                          <ArrowUp className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => handleMoveTask(idx, 'down')}
                          disabled={idx === templateTasks.length - 1}
                          className="p-1 text-slate-400 hover:text-slate-700 disabled:opacity-30"
                          title="Move Down"
                        >
                          <ArrowDown className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => handleDeleteTask(t.id)}
                          className="p-1 text-rose-400 hover:text-rose-600 ml-1"
                          title="Delete Task"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}

            <div className="pt-3 border-t border-slate-200 flex justify-end">
              <button
                type="button"
                onClick={() => setActiveConfigTemplate(null)}
                className="px-5 py-2 rounded-xl bg-slate-800 hover:bg-slate-900 text-white text-xs font-bold"
              >
                Close
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default WorkTemplatesPage;
