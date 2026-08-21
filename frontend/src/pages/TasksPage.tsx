import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  Plus,
  CheckSquare,
  List,
  LayoutGrid,
  AlertCircle,
  Clock,
  Sparkles,
  User,
  Building,
  CheckCircle2,
  Calendar,
  Layers,
  ChevronRight,
  UserCheck,
  Edit2,
} from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { taskApi, clientApi, employeeApi } from '../api/endpoints';
import { Task, Client, Employee } from '../types';
import { useAuth } from '../context/AuthContext';
import clsx from 'clsx';

export const TasksPage: React.FC = () => {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [viewMode, setViewMode] = useState<'list' | 'kanban'>('list');
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Edit / Reassign Task State
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editFormData, setEditFormData] = useState<{
    title: string;
    description: string;
    clientId: string;
    assignedTo: string;
    taskCategory: 'GST' | 'ITR' | 'AUDIT' | 'COMPLIANCE' | 'BILLING' | 'OTHER';
    priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
    status: 'TODO' | 'IN_PROGRESS' | 'UNDER_REVIEW' | 'COMPLETED' | 'CANCELLED';
    dueDate: string;
  }>({
    title: '',
    description: '',
    clientId: '',
    assignedTo: '',
    taskCategory: 'ITR',
    priority: 'HIGH',
    status: 'TODO',
    dueDate: '',
  });

  // Filter States
  const [taskScope, setTaskScope] = useState<'MY_TASKS' | 'ALL_TASKS'>('MY_TASKS');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'TODO' | 'IN_PROGRESS' | 'UNDER_REVIEW' | 'COMPLETED'>('ALL');
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [assigneeFilter, setAssigneeFilter] = useState<string>('ALL');

  // Form State for creating task
  const [formData, setFormData] = useState<{
    title: string;
    description: string;
    clientId: string;
    assignedTo: string;
    taskCategory: 'GST' | 'ITR' | 'AUDIT' | 'COMPLIANCE' | 'BILLING' | 'OTHER';
    priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
    dueDate: string;
  }>({
    title: '',
    description: '',
    clientId: '',
    assignedTo: '',
    taskCategory: 'ITR',
    priority: 'HIGH',
    dueDate: (() => {
      const d = new Date();
      d.setDate(d.getDate() + 7);
      return d.toISOString().split('T')[0];
    })(),
  });

  const { user } = useAuth();
  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isFirmAdmin = userRoleCodes.some((r: string) => ['ORG_ADMIN', 'SUPER_ADMIN', 'PARTNER'].includes(r));
  const isStaff = userRoleCodes.some((r: string) => ['ARTICLE_ASSISTANT', 'STAFF', 'TRAINEE'].includes(r)) && !isFirmAdmin;

  // Find linked employee for logged in user
  const currentEmployee = useMemo(() => {
    return employees.find(
      (e) =>
        (e.email && user?.email && e.email.toLowerCase() === user.email.toLowerCase()) ||
        (user?.id && (e as any).userId === user.id)
    );
  }, [employees, user]);

  useEffect(() => {
    loadTasks();
    loadClientsAndEmployees();
  }, [taskScope, statusFilter, categoryFilter, assigneeFilter]);

  const loadTasks = async () => {
    try {
      setIsLoading(true);
      const params: any = { size: 100 };
      if (taskScope === 'MY_TASKS') {
        params.myTasksOnly = true;
      }
      if (statusFilter !== 'ALL') {
        params.status = statusFilter;
      }
      if (categoryFilter !== 'ALL') {
        params.taskCategory = categoryFilter;
      }
      if (assigneeFilter !== 'ALL') {
        params.assignedTo = assigneeFilter;
      }

      const res = await taskApi.getAll(params);
      const list = Array.isArray(res) ? res : (res?.content || []);
      setTasks(list);
    } catch (err) {
      console.error('Failed to load tasks', err);
      setTasks([]);
    } finally {
      setIsLoading(false);
    }
  };

  const loadClientsAndEmployees = async () => {
    try {
      const [cRes, eRes] = await Promise.allSettled([
        clientApi.getAll({ size: 200 }),
        employeeApi.getAll({ size: 200 }),
      ]);
      if (cRes.status === 'fulfilled' && cRes.value) {
        const cList = Array.isArray(cRes.value) ? cRes.value : (cRes.value?.content || []);
        setClients(cList);
      }
      if (eRes.status === 'fulfilled' && eRes.value) {
        const eList = Array.isArray(eRes.value) ? eRes.value : (eRes.value?.content || []);
        setEmployees(eList);
      }
    } catch (err) {
      console.error('Failed to load metadata for task assignment', err);
    }
  };

  const handleUpdateStatus = async (taskId: string, newStatus: string) => {
    try {
      await taskApi.updateStatus(taskId, newStatus);
      await loadTasks();
    } catch (err) {
      console.error('Failed to update task status', err);
    }
  };

  const handleOpenEditModal = (task: Task) => {
    setEditingTask(task);
    // Find matching employee ID
    let matchingEmpId = task.assignedTo || '';
    if (task.assignedTo) {
      const foundEmp = employees.find(
        (e) => e.id === task.assignedTo || (e as any).userId === task.assignedTo || (e.email && task.assigneeEmail && e.email.toLowerCase() === task.assigneeEmail.toLowerCase())
      );
      if (foundEmp) matchingEmpId = foundEmp.id;
    }

    setEditFormData({
      title: task.title,
      description: task.description || '',
      clientId: task.clientId || '',
      assignedTo: matchingEmpId,
      taskCategory: (task.category as any) || 'ITR',
      priority: task.priority || 'MEDIUM',
      status: task.status || 'TODO',
      dueDate: task.dueDate || '',
    });
    setIsEditModalOpen(true);
  };

  const handleUpdateTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingTask) return;
    if (!editFormData.title.trim()) {
      alert('Please enter a task title.');
      return;
    }

    try {
      setIsSubmitting(true);
      await taskApi.update(editingTask.id, {
        title: editFormData.title.trim(),
        description: editFormData.description.trim() || undefined,
        clientId: editFormData.clientId || undefined,
        assignedTo: editFormData.assignedTo || undefined,
        unassign: !editFormData.assignedTo,
        category: editFormData.taskCategory,
        status: editFormData.status,
        priority: editFormData.priority,
        dueDate: editFormData.dueDate || undefined,
      });

      alert('Task updated & assigned successfully!');
      setIsEditModalOpen(false);
      setEditingTask(null);
      await loadTasks();
    } catch (err: any) {
      alert(`Failed to update task: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCreateTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.title.trim()) {
      alert('Please enter a task title.');
      return;
    }

    try {
      setIsSubmitting(true);
      await taskApi.create({
        title: formData.title.trim(),
        description: formData.description.trim() || undefined,
        clientId: formData.clientId || undefined,
        assignedTo: formData.assignedTo || undefined,
        category: formData.taskCategory,
        priority: formData.priority,
        dueDate: formData.dueDate,
      });

      alert('Task created & assigned successfully!');
      setIsModalOpen(false);
      setFormData({
        title: '',
        description: '',
        clientId: '',
        assignedTo: '',
        taskCategory: 'ITR',
        priority: 'HIGH',
        dueDate: new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0],
      });
      await loadTasks();
    } catch (err: any) {
      alert(`Failed to create task: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // KPI Calculations
  const stats = useMemo(() => {
    const total = tasks.length;
    const todo = tasks.filter((t) => t.status === 'TODO').length;
    const inProgress = tasks.filter((t) => t.status === 'IN_PROGRESS').length;
    const underReview = tasks.filter((t) => t.status === 'UNDER_REVIEW').length;
    const completed = tasks.filter((t) => t.status === 'COMPLETED').length;
    const overdue = tasks.filter((t) => t.status !== 'COMPLETED' && t.dueDate && new Date(t.dueDate) < new Date()).length;
    return { total, todo, inProgress, underReview, completed, overdue };
  }, [tasks]);

  const columns: Column<Task>[] = [
    {
      header: 'Task Title & Category',
      accessor: (row) => (
        <div className="flex flex-col">
          <div className="flex items-center gap-1.5">
            <span className="px-1.5 py-0.2 bg-slate-100 text-slate-700 text-[10px] font-bold rounded border font-mono uppercase">
              {row.category || 'COMPLIANCE'}
            </span>
            <span className="font-bold text-slate-900 text-xs">{row.title}</span>
          </div>
          {row.description && (
            <span className="text-[11px] text-slate-500 mt-0.5 line-clamp-1 max-w-md">{row.description}</span>
          )}
        </div>
      ),
    },
    {
      header: 'Client',
      accessor: (row) => (
        <div className="flex items-center gap-1 text-xs">
          <Building className="w-3.5 h-3.5 text-slate-400 shrink-0" />
          <span className="font-semibold text-slate-800">{row.clientName || 'Practice General'}</span>
        </div>
      ),
    },
    {
      header: 'Assigned To',
      accessor: (row) => {
        const isMe = row.assigneeEmail && user?.email && row.assigneeEmail.toLowerCase() === user.email.toLowerCase();
        return (
          <div className="flex items-center gap-1.5 text-xs">
            <User className={`w-3.5 h-3.5 shrink-0 ${isMe ? 'text-emerald-600' : 'text-slate-400'}`} />
            <div className="flex flex-col">
              <span className={`font-semibold ${isMe ? 'text-emerald-700 font-bold' : 'text-slate-700'}`}>
                {row.assigneeName || (row.assignedTo ? 'Assigned Staff' : 'Unassigned')}
              </span>
              {isMe && <span className="text-[9px] text-emerald-600 font-mono font-bold">Assigned to You</span>}
            </div>
          </div>
        );
      },
    },
    {
      header: 'Priority',
      accessor: (row) => {
        const colors: Record<string, string> = {
          URGENT: 'text-rose-700 bg-rose-50 border-rose-200',
          HIGH: 'text-amber-700 bg-amber-50 border-amber-200',
          MEDIUM: 'text-blue-700 bg-blue-50 border-blue-200',
          LOW: 'text-slate-700 bg-slate-50 border-slate-200',
        };
        return (
          <span className={clsx('px-2 py-0.5 text-[10px] font-bold rounded border tracking-wider uppercase', colors[row.priority] || colors.MEDIUM)}>
            {row.priority}
          </span>
        );
      },
    },
    {
      header: 'Due Date',
      accessor: (row) => {
        const isOverdue = row.dueDate && new Date(row.dueDate) < new Date() && row.status !== 'COMPLETED';
        return (
          <div className="flex items-center gap-1 font-mono text-xs">
            {isOverdue && <AlertCircle className="w-3.5 h-3.5 text-rose-600 shrink-0" />}
            <span className={isOverdue ? 'text-rose-600 font-bold' : 'text-slate-700'}>{row.dueDate || 'No Due Date'}</span>
          </div>
        );
      },
    },
    {
      header: 'Status',
      accessor: (row) => <StatusBadge status={row.status} size="sm" />,
      align: 'center',
    },
    {
      header: 'Actions',
      align: 'right',
      cell: (row) => (
        <div className="flex items-center justify-end gap-1.5">
          <button
            onClick={() => handleOpenEditModal(row)}
            className="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-300 rounded text-xs font-semibold inline-flex items-center gap-1"
            title="Edit & Reassign Task"
          >
            <Edit2 className="w-3 h-3 text-slate-500" /> Assign / Edit
          </button>
          {row.status === 'TODO' && (
            <button
              onClick={() => handleUpdateStatus(row.id, 'IN_PROGRESS')}
              className="px-2 py-1 bg-brand-50 hover:bg-brand-100 text-brand-700 border border-brand-200 rounded text-xs font-semibold"
            >
              Start Work
            </button>
          )}
          {row.status === 'IN_PROGRESS' && (
            <button
              onClick={() => handleUpdateStatus(row.id, 'UNDER_REVIEW')}
              className="px-2 py-1 bg-purple-50 hover:bg-purple-100 text-purple-700 border border-purple-200 rounded text-xs font-semibold"
            >
              Submit Review
            </button>
          )}
          {row.status === 'UNDER_REVIEW' && (
            <button
              onClick={() => handleUpdateStatus(row.id, 'COMPLETED')}
              className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded text-xs font-semibold inline-flex items-center gap-1"
            >
              <CheckCircle2 className="w-3.5 h-3.5" /> Approve & Done
            </button>
          )}
          {row.status === 'COMPLETED' && (
            <span className="text-xs text-emerald-600 font-bold inline-flex items-center gap-1">
              <CheckCircle2 className="w-3.5 h-3.5" /> Completed
            </span>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black tracking-tight text-slate-900">Tasks & Workflows</h1>
            {user?.email && (
              <span className="px-2.5 py-0.5 bg-brand-50 text-brand-700 text-xs font-bold rounded-full border border-brand-200">
                Logged in as: {user.firstName || user.email.split('@')[0]}
              </span>
            )}
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Track operational tax deliverables, assign tasks to practice staff, and monitor statutory filing deadlines.
          </p>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          {/* View Switcher */}
          <div className="bg-slate-100 p-1 rounded-lg border border-slate-200 flex items-center">
            <button
              onClick={() => setViewMode('list')}
              className={clsx('p-1.5 rounded-md text-xs font-medium transition-colors', viewMode === 'list' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-500')}
              title="List View"
            >
              <List className="w-4 h-4" />
            </button>
            <button
              onClick={() => setViewMode('kanban')}
              className={clsx('p-1.5 rounded-md text-xs font-medium transition-colors', viewMode === 'kanban' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-500')}
              title="Kanban Board"
            >
              <LayoutGrid className="w-4 h-4" />
            </button>
          </div>

          <Link to="/tasks/bulk">
            <Button variant="outline" leftIcon={<Sparkles className="w-4 h-4 text-brand-600" />}>
              ⚡ Bulk Task Generator
            </Button>
          </Link>

          <Button onClick={() => setIsModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
            Create Task
          </Button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
        <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-sm">
          <span className="text-[11px] font-semibold text-slate-500 block">Total Active</span>
          <p className="text-xl font-black text-slate-900 mt-1">{stats.total}</p>
        </div>
        <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-sm">
          <span className="text-[11px] font-semibold text-slate-500 block">To Do / Unstarted</span>
          <p className="text-xl font-black text-blue-600 mt-1">{stats.todo}</p>
        </div>
        <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-sm">
          <span className="text-[11px] font-semibold text-slate-500 block">In Progress</span>
          <p className="text-xl font-black text-amber-600 mt-1">{stats.inProgress}</p>
        </div>
        <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-sm">
          <span className="text-[11px] font-semibold text-slate-500 block">Under Review</span>
          <p className="text-xl font-black text-purple-600 mt-1">{stats.underReview}</p>
        </div>
        <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-sm">
          <span className="text-[11px] font-semibold text-slate-500 block">Completed</span>
          <p className="text-xl font-black text-emerald-600 mt-1">{stats.completed}</p>
        </div>
      </div>

      {/* Primary Scope & Filter Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-200 pb-3 flex-wrap">
        <div className="flex items-center gap-2 flex-wrap">
          <button
            onClick={() => setTaskScope('MY_TASKS')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
              taskScope === 'MY_TASKS'
                ? 'bg-brand-600 text-white shadow-sm'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            <UserCheck className="w-3.5 h-3.5" /> 🎯 {isStaff ? 'My Assigned Deliverables' : 'My Assigned Tasks'}
          </button>

          {!isStaff && (
            <button
              onClick={() => setTaskScope('ALL_TASKS')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                taskScope === 'ALL_TASKS'
                  ? 'bg-brand-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              <Building className="w-3.5 h-3.5" /> 🏢 All Practice Tasks
            </button>
          )}

          {/* Filter by Specific Assignee (for Admins & Managers) */}
          {!isStaff && employees.length > 0 && (
            <div className="flex items-center gap-1 pl-2 border-l border-slate-200">
              <span className="text-[11px] font-semibold text-slate-500">Staff:</span>
              <select
                value={assigneeFilter}
                onChange={(e) => setAssigneeFilter(e.target.value)}
                className="text-xs px-2 py-1 border border-slate-300 rounded-lg bg-white font-medium text-slate-700"
              >
                <option value="ALL">All Staff Members</option>
                {employees.map((emp) => (
                  <option key={emp.id} value={(emp as any).userId || emp.id}>
                    {emp.fullName || `${emp.firstName || ''} ${emp.lastName || ''}`.trim() || emp.email} ({emp.designation || 'Staff'})
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

        {/* Category Filter Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
          {['ALL', 'ITR', 'GST', 'AUDIT', 'COMPLIANCE', 'BILLING'].map((cat) => (
            <button
              key={cat}
              onClick={() => setCategoryFilter(cat)}
              className={`px-2.5 py-1 rounded-md text-[11px] font-semibold transition-colors ${
                categoryFilter === cat
                  ? 'bg-slate-900 text-white'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* List or Kanban View */}
      {viewMode === 'list' ? (
        <DataTable
          columns={columns}
          data={tasks}
          isLoading={isLoading}
          searchPlaceholder="Search tasks by title, description, client, assignee..."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {(['TODO', 'IN_PROGRESS', 'UNDER_REVIEW', 'COMPLETED'] as const).map((colStatus) => {
            const colTasks = tasks.filter((t) => t.status === colStatus);
            return (
              <div key={colStatus} className="bg-slate-100/80 border border-slate-200 rounded-xl p-4 flex flex-col min-h-[480px]">
                <div className="flex items-center justify-between pb-3 border-b border-slate-200 mb-3">
                  <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                    {colStatus.replace('_', ' ')}
                  </span>
                  <span className="w-5 h-5 rounded-full bg-white border border-slate-200 text-slate-700 font-bold text-[10px] flex items-center justify-center">
                    {colTasks.length}
                  </span>
                </div>

                <div className="space-y-3 flex-1 overflow-y-auto">
                  {colTasks.map((task) => (
                    <div
                      key={task.id}
                      className="bg-white p-3.5 rounded-lg border border-slate-200 shadow-2xs hover:shadow-md transition-all space-y-2.5"
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] font-bold text-slate-700 bg-slate-100 border border-slate-200 px-1.5 py-0.5 rounded uppercase font-mono">
                          {task.category || 'TASK'}
                        </span>
                        <span className="text-[10px] text-slate-500 font-mono">{task.dueDate}</span>
                      </div>

                      <p className="text-xs font-bold text-slate-900 leading-snug">{task.title}</p>
                      {task.description && (
                        <p className="text-[11px] text-slate-500 line-clamp-2">{task.description}</p>
                      )}

                      <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-600">
                        <span className="font-semibold truncate max-w-[120px]">{task.clientName || 'General'}</span>
                        <span className="font-medium text-emerald-700 truncate max-w-[100px]">
                          {task.assigneeName || 'Assigned'}
                        </span>
                      </div>

                      <div className="pt-2 flex items-center justify-between gap-1 border-t border-slate-100">
                        <button
                          onClick={() => handleOpenEditModal(task)}
                          className="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded text-[11px] font-medium inline-flex items-center gap-1 border border-slate-200"
                        >
                          <Edit2 className="w-2.5 h-2.5" /> Reassign
                        </button>
                        <select
                          value={task.status}
                          onChange={(e) => handleUpdateStatus(task.id, e.target.value)}
                          className="bg-slate-50 border border-slate-200 rounded text-[11px] px-2 py-1 font-semibold text-slate-700 cursor-pointer"
                        >
                          <option value="TODO">To Do</option>
                          <option value="IN_PROGRESS">In Progress</option>
                          <option value="UNDER_REVIEW">Under Review</option>
                          <option value="COMPLETED">Completed</option>
                        </select>
                      </div>
                    </div>
                  ))}
                  {colTasks.length === 0 && (
                    <div className="h-32 flex items-center justify-center text-xs text-slate-400 border border-dashed border-slate-300 rounded-lg">
                      No tasks in this column
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* =========================================================================
          CREATE TASK MODAL
          ========================================================================= */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create Practice Task"
        subtitle="Assign statutory compliance, audit, or tax return tasks to practice employees"
        maxWidth="lg"
      >
        <form onSubmit={handleCreateTask} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Task Title *</label>
            <input
              type="text"
              required
              placeholder="e.g. Verify Form 26AS & Draft ITR-2 for Dr. Rajesh Sharma"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Related Client (Optional)</label>
              <select
                value={formData.clientId}
                onChange={(e) => setFormData({ ...formData, clientId: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="">-- General Practice Task --</option>
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.displayName} ({c.pan || 'No PAN'})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-xs font-semibold text-slate-700">Assign to Employee</label>
                {currentEmployee && (
                  <button
                    type="button"
                    onClick={() => setFormData({ ...formData, assignedTo: currentEmployee.id })}
                    className="text-[11px] text-brand-600 hover:text-brand-800 font-bold"
                  >
                    ⚡ Assign to Me
                  </button>
                )}
              </div>
              <select
                value={formData.assignedTo}
                onChange={(e) => setFormData({ ...formData, assignedTo: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="">-- Unassigned / General Pool --</option>
                {employees.map((emp) => {
                  const isMe =
                    (emp.email && user?.email && emp.email.toLowerCase() === user.email.toLowerCase()) ||
                    (user?.id && (emp as any).userId === user.id);
                  const name = emp.fullName || `${emp.firstName || ''} ${emp.lastName || ''}`.trim() || emp.email;
                  return (
                    <option key={emp.id} value={emp.id}>
                      {name} {isMe ? '⭐ (You)' : ''} — {emp.designation || 'Staff'} ({emp.department || 'Tax'})
                    </option>
                  );
                })}
              </select>
              {employees.length === 0 && (
                <p className="text-[10px] text-amber-600 mt-1">
                  Loading team members or no staff members registered yet.
                </p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Task Category</label>
              <select
                value={formData.taskCategory}
                onChange={(e) => setFormData({ ...formData, taskCategory: e.target.value as any })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="ITR">Income Tax (ITR)</option>
                <option value="GST">GST Compliance</option>
                <option value="AUDIT">Tax Audit / 3CD</option>
                <option value="COMPLIANCE">TDS / ROC Compliance</option>
                <option value="BILLING">Billing & Fees</option>
                <option value="OTHER">Other Assignment</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Priority</label>
              <select
                value={formData.priority}
                onChange={(e) => setFormData({ ...formData, priority: e.target.value as any })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent / Statutory Deadline</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Due Date</label>
              <input
                type="date"
                value={formData.dueDate}
                onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Description / Instructions (Optional)</label>
            <textarea
              rows={3}
              placeholder="Provide specific instructions, check points, or notes for the assigned employee..."
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg"
            />
          </div>

          <div className="pt-3 flex justify-end gap-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              Create & Assign Task
            </Button>
          </div>
        </form>
      </Modal>

      {/* =========================================================================
          EDIT & REASSIGN TASK MODAL
          ========================================================================= */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => {
          setIsEditModalOpen(false);
          setEditingTask(null);
        }}
        title="Edit & Reassign Task"
        subtitle="Update assignee, statutory deadline, priority, and workflow status"
        maxWidth="lg"
      >
        <form onSubmit={handleUpdateTask} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Task Title *</label>
            <input
              type="text"
              required
              value={editFormData.title}
              onChange={(e) => setEditFormData({ ...editFormData, title: e.target.value })}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Related Client</label>
              <select
                value={editFormData.clientId}
                onChange={(e) => setEditFormData({ ...editFormData, clientId: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="">-- General Practice Task --</option>
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.displayName} ({c.pan || 'No PAN'})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-xs font-semibold text-slate-700">Assigned Employee</label>
                {currentEmployee && (
                  <button
                    type="button"
                    onClick={() => setEditFormData({ ...editFormData, assignedTo: currentEmployee.id })}
                    className="text-[11px] text-brand-600 hover:text-brand-800 font-bold"
                  >
                    ⚡ Assign to Me
                  </button>
                )}
              </div>
              <select
                value={editFormData.assignedTo}
                onChange={(e) => setEditFormData({ ...editFormData, assignedTo: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white font-medium text-slate-800"
              >
                <option value="">-- Unassigned --</option>
                {employees.map((emp) => {
                  const isMe =
                    (emp.email && user?.email && emp.email.toLowerCase() === user.email.toLowerCase()) ||
                    (user?.id && (emp as any).userId === user.id);
                  const name = emp.fullName || `${emp.firstName || ''} ${emp.lastName || ''}`.trim() || emp.email;
                  return (
                    <option key={emp.id} value={emp.id}>
                      {name} {isMe ? '⭐ (You)' : ''} — {emp.designation || 'Staff'} ({emp.department || 'Tax'})
                    </option>
                  );
                })}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Category</label>
              <select
                value={editFormData.taskCategory}
                onChange={(e) => setEditFormData({ ...editFormData, taskCategory: e.target.value as any })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="ITR">Income Tax (ITR)</option>
                <option value="GST">GST Compliance</option>
                <option value="AUDIT">Tax Audit / 3CD</option>
                <option value="COMPLIANCE">TDS / ROC Compliance</option>
                <option value="BILLING">Billing & Fees</option>
                <option value="OTHER">Other Assignment</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Priority</label>
              <select
                value={editFormData.priority}
                onChange={(e) => setEditFormData({ ...editFormData, priority: e.target.value as any })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent / Statutory Deadline</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Workflow Status</label>
              <select
                value={editFormData.status}
                onChange={(e) => setEditFormData({ ...editFormData, status: e.target.value as any })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="TODO">To Do</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="UNDER_REVIEW">Under Review</option>
                <option value="COMPLETED">Completed</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Due Date</label>
              <input
                type="date"
                value={editFormData.dueDate}
                onChange={(e) => setEditFormData({ ...editFormData, dueDate: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Description / Instructions</label>
            <textarea
              rows={3}
              placeholder="Provide specific instructions, check points, or notes for the assigned employee..."
              value={editFormData.description}
              onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg"
            />
          </div>

          <div className="pt-3 flex justify-end gap-2 border-t border-slate-200">
            <Button
              variant="outline"
              onClick={() => {
                setIsEditModalOpen(false);
                setEditingTask(null);
              }}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              Save & Update Assignment
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
