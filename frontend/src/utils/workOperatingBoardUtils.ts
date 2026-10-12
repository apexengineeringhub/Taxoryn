import type { WorkInventoryItemDto, WorkInventoryType, WorkInventoryStatus } from '../types/index.ts';

export interface KanbanColumnConfig {
  id: string;
  title: string;
  statuses: WorkInventoryStatus[];
  headerBg: string;
  badgeBg: string;
  textColor: string;
  dotColor: string;
}

export const KANBAN_COLUMNS: KanbanColumnConfig[] = [
  {
    id: 'not_started',
    title: 'To Do / Not Started',
    statuses: ['NOT_STARTED'],
    headerBg: 'bg-slate-800/80 border-slate-700/60',
    badgeBg: 'bg-slate-700 text-slate-300',
    textColor: 'text-slate-300',
    dotColor: 'bg-slate-400',
  },
  {
    id: 'in_progress',
    title: 'In Progress',
    statuses: ['IN_PROGRESS'],
    headerBg: 'bg-blue-950/40 border-blue-800/40',
    badgeBg: 'bg-blue-900/60 text-blue-300',
    textColor: 'text-blue-300',
    dotColor: 'bg-blue-400',
  },
  {
    id: 'blocked',
    title: 'Blocked / Action Needed',
    statuses: ['BLOCKED'],
    headerBg: 'bg-rose-950/40 border-rose-800/40',
    badgeBg: 'bg-rose-900/60 text-rose-300',
    textColor: 'text-rose-300',
    dotColor: 'bg-rose-500 animate-pulse',
  },
  {
    id: 'under_review',
    title: 'Under Review',
    statuses: ['UNDER_REVIEW'],
    headerBg: 'bg-purple-950/40 border-purple-800/40',
    badgeBg: 'bg-purple-900/60 text-purple-300',
    textColor: 'text-purple-300',
    dotColor: 'bg-purple-400',
  },
  {
    id: 'ready_for_filing',
    title: 'Ready for Filing',
    statuses: ['READY_FOR_FILING'],
    headerBg: 'bg-amber-950/40 border-amber-800/40',
    badgeBg: 'bg-amber-900/60 text-amber-300',
    textColor: 'text-amber-300',
    dotColor: 'bg-amber-400',
  },
  {
    id: 'completed',
    title: 'Completed / Filed',
    statuses: ['FILED', 'COMPLETED', 'CANCELLED'],
    headerBg: 'bg-emerald-950/40 border-emerald-800/40',
    badgeBg: 'bg-emerald-900/60 text-emerald-300',
    textColor: 'text-emerald-300',
    dotColor: 'bg-emerald-400',
  },
];

export const isWorkItemOverdue = (item: WorkInventoryItemDto): boolean => {
  if (!item.dueDate) return false;
  if (
    item.normalizedStatus === 'COMPLETED' ||
    item.normalizedStatus === 'FILED' ||
    item.normalizedStatus === 'CANCELLED'
  ) {
    return false;
  }
  const today = new Date().toISOString().split('T')[0];
  return item.dueDate < today;
};

export const getAuthoritativeNavigationUrl = (item: WorkInventoryItemDto): string => {
  switch (item.workType) {
    case 'WORK_INSTANCE':
      if (item.engagementId) {
        return `/engagements/${item.engagementId}`;
      }
      return '/compliance/workbench';
    case 'COMPLIANCE_OBLIGATION':
      if (item.clientId) {
        return `/clients/${item.clientId}?tab=compliance`;
      }
      return '/compliance/workbench';
    case 'TASK':
      return `/tasks?tab=ALL_TASKS&taskId=${item.id}`;
    default:
      return '/tasks';
  }
};
