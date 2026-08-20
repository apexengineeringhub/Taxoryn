import React, { useState, useEffect } from 'react';
import { Plus, CheckSquare, List, LayoutGrid, AlertCircle, Clock } from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { taskApi } from '../api/endpoints';
import { Task } from '../types';
import clsx from 'clsx';

export const TasksPage: React.FC = () => {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [isLoading, setIsLoading] = useState(true);
  const [viewMode, setViewMode] = useState<'list' | 'kanban'>('list');
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Form State
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    priority: 'HIGH',
    dueDate: new Date().toISOString().split('T')[0],
  });

  useEffect(() => {
    loadTasks();
  }, [page, pageSize]);

  const loadTasks = async () => {
    try {
      setIsLoading(true);
      const res = await taskApi.getAll({ page, size: pageSize });
      setTasks(res.content);
      setTotalElements(res.totalElements);
    } catch (err) {
      console.error('Failed to load tasks', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpdateStatus = async (taskId: string, newStatus: string) => {
    try {
      await taskApi.updateStatus(taskId, newStatus);
      loadTasks();
    } catch (err) {
      console.error('Failed to update task status', err);
    }
  };

  const columns: Column<Task>[] = [
    {
      header: 'Task Title',
      accessor: (row) => (
        <div>
          <span className="font-bold text-slate-900 block">{row.title}</span>
          {row.description && <span className="text-[10px] text-slate-400 block truncate max-w-xs">{row.description}</span>}
        </div>
      ),
    },
    {
      header: 'Client',
      accessor: (row) => row.clientName || <span className="text-slate-400">General Task</span>,
    },
    {
      header: 'Priority',
      accessor: (row) => {
        const colors = {
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
        const isOverdue = new Date(row.dueDate) < new Date() && row.status !== 'COMPLETED';
        return (
          <div className="flex items-center gap-1.5 font-mono text-xs">
            {isOverdue && <AlertCircle className="w-3.5 h-3.5 text-rose-600" />}
            <span className={isOverdue ? 'text-rose-600 font-bold' : 'text-slate-700'}>{row.dueDate}</span>
          </div>
        );
      },
    },
    {
      header: 'Status',
      accessor: (row) => <StatusBadge status={row.status} size="sm" />,
      align: 'center',
    },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">Tasks & Workflows</h1>
          <p className="text-xs text-slate-500 mt-1">
            Track operational tax deliverables, client review stages, and statutory deadlines.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="bg-slate-100 p-1 rounded-lg border border-slate-200 flex items-center">
            <button
              onClick={() => setViewMode('list')}
              className={clsx('p-1.5 rounded-md text-xs font-medium transition-colors', viewMode === 'list' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-500')}
            >
              <List className="w-4 h-4" />
            </button>
            <button
              onClick={() => setViewMode('kanban')}
              className={clsx('p-1.5 rounded-md text-xs font-medium transition-colors', viewMode === 'kanban' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-500')}
            >
              <LayoutGrid className="w-4 h-4" />
            </button>
          </div>
          <Button onClick={() => setIsModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
            Create Task
          </Button>
        </div>
      </div>

      {/* List or Kanban View */}
      {viewMode === 'list' ? (
        <DataTable
          columns={columns}
          data={tasks}
          totalElements={totalElements}
          pageSize={pageSize}
          pageNumber={page}
          onPageChange={setPage}
          onPageSizeChange={setPageSize}
          isLoading={isLoading}
          searchPlaceholder="Search tasks by title or client..."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {(['TODO', 'IN_PROGRESS', 'UNDER_REVIEW', 'COMPLETED'] as const).map((colStatus) => {
            const colTasks = tasks.filter((t) => t.status === colStatus);
            return (
              <div key={colStatus} className="bg-slate-100/70 border border-slate-200/80 rounded-xl p-4 flex flex-col min-h-[450px]">
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
                      className="bg-white p-3.5 rounded-lg border border-slate-200 shadow-2xs hover:shadow-card transition-all space-y-2"
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] font-bold text-amber-700 bg-amber-50 border border-amber-200 px-1.5 py-0.5 rounded uppercase">
                          {task.priority}
                        </span>
                        <span className="text-[10px] text-slate-400 font-mono">{task.dueDate}</span>
                      </div>
                      <p className="text-xs font-bold text-slate-900 leading-snug">{task.title}</p>
                      <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[10px] text-slate-500">
                        <span>{task.clientName || 'Practice Task'}</span>
                        <select
                          value={task.status}
                          onChange={(e) => handleUpdateStatus(task.id, e.target.value)}
                          className="bg-slate-50 border border-slate-200 rounded text-[10px] px-1 py-0.5 font-medium"
                        >
                          <option value="TODO">To Do</option>
                          <option value="IN_PROGRESS">In Progress</option>
                          <option value="UNDER_REVIEW">Review</option>
                          <option value="COMPLETED">Completed</option>
                        </select>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create Task Modal Placeholder */}
      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title="Create New Task" subtitle="Assign statutory or practice assignment">
        <div className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Task Title *</label>
            <input
              type="text"
              placeholder="e.g. Prepare GSTR-1 for August 2026"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Priority</label>
              <select
                value={formData.priority}
                onChange={(e) => setFormData({ ...formData, priority: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Due Date</label>
              <input
                type="date"
                value={formData.dueDate}
                onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg"
              >
              </input>
            </div>
          </div>
          <div className="pt-4 flex justify-end gap-2">
            <Button variant="outline" onClick={() => setIsModalOpen(false)}>Cancel</Button>
            <Button onClick={() => { setIsModalOpen(false); alert('Task created'); }}>Save Task</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
