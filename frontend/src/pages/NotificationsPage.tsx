import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Bell,
  Check,
  CheckCheck,
  RotateCcw,
  Trash2,
  Filter,
  AlertTriangle,
  AlertOctagon,
  Info,
  CheckCircle2,
  FileText,
  CheckSquare,
  Users,
  ShieldAlert,
  CreditCard,
  Settings,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  Search,
  Sparkles,
  Inbox
} from 'lucide-react';
import clsx from 'clsx';
import { notificationApi } from '../api/endpoints';
import { 
  NotificationItem, 
  NotificationSeverity, 
  NotificationCategory,
  NotificationType 
} from '../types';

export const NotificationsPage: React.FC = () => {
  const navigate = useNavigate();

  // State
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [currentPage, setCurrentPage] = useState<number>(0);
  const pageSize = 15;

  // Filters
  const [tabFilter, setTabFilter] = useState<'ALL' | 'UNREAD'>('ALL');
  const [categoryFilter, setCategoryFilter] = useState<NotificationCategory | 'ALL'>('ALL');
  const [severityFilter, setSeverityFilter] = useState<NotificationSeverity | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Fetch Unread Count
  const loadUnreadCount = useCallback(async () => {
    try {
      const res = await notificationApi.getUnreadCount();
      setUnreadCount(res.unreadCount || 0);
    } catch {
      // ignore
    }
  }, []);

  // Fetch Notifications
  const loadNotifications = useCallback(async () => {
    setIsLoading(true);
    try {
      const res = await notificationApi.getAll({
        page: currentPage,
        size: pageSize,
        isRead: tabFilter === 'UNREAD' ? false : undefined,
        category: categoryFilter === 'ALL' ? undefined : categoryFilter,
        severity: severityFilter === 'ALL' ? undefined : severityFilter,
      });

      setNotifications(res.content || []);
      setTotalElements(res.totalElements || 0);
      setTotalPages(res.totalPages || 1);
    } catch (err) {
      console.error('Failed to load notifications', err);
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, tabFilter, categoryFilter, severityFilter]);

  useEffect(() => {
    loadUnreadCount();
  }, [loadUnreadCount]);

  useEffect(() => {
    loadNotifications();
  }, [loadNotifications]);

  // Actions
  const handleMarkAsRead = async (id: string) => {
    try {
      await notificationApi.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true, read: true, readAt: new Date().toISOString() } : n))
      );
      setUnreadCount((c) => Math.max(0, c - 1));
      if (tabFilter === 'UNREAD') {
        setNotifications((prev) => prev.filter((n) => n.id !== id));
        setTotalElements((t) => Math.max(0, t - 1));
      }
    } catch (err) {
      console.error('Failed to mark as read', err);
    }
  };

  const handleMarkAsUnread = async (id: string) => {
    try {
      await notificationApi.markAsUnread(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: false, read: false, readAt: undefined } : n))
      );
      setUnreadCount((c) => c + 1);
    } catch (err) {
      console.error('Failed to mark as unread', err);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, isRead: true, read: true, readAt: new Date().toISOString() }))
      );
      setUnreadCount(0);
      if (tabFilter === 'UNREAD') {
        setNotifications([]);
        setTotalElements(0);
      }
    } catch (err) {
      console.error('Failed to mark all as read', err);
    }
  };

  const handleDismiss = async (id: string) => {
    try {
      await notificationApi.dismiss(id);
      setNotifications((prev) => prev.filter((n) => n.id !== id));
      setTotalElements((t) => Math.max(0, t - 1));
      loadUnreadCount();
    } catch (err) {
      console.error('Failed to dismiss notification', err);
    }
  };

  const handleNavigateToEntity = (notif: NotificationItem) => {
    if (!notif.isRead && !notif.read) {
      handleMarkAsRead(notif.id);
    }

    if (notif.actionUrl) {
      navigate(notif.actionUrl);
      return;
    }

    if (notif.entityType === 'DOCUMENT_REQUEST' || notif.category === 'DOCUMENT') {
      navigate('/documents');
    } else if (notif.entityType === 'TASK' || notif.category === 'TASK') {
      navigate('/tasks');
    } else if (notif.entityType === 'CLIENT' || notif.category === 'CLIENT') {
      navigate('/clients');
    } else if (notif.category === 'COMPLIANCE') {
      navigate('/calendar');
    } else if (notif.category === 'BILLING') {
      navigate('/billing');
    } else {
      // stay on notifications
    }
  };

  const getActionLabel = (notif: NotificationItem) => {
    if (notif.entityType === 'DOCUMENT_REQUEST' || notif.category === 'DOCUMENT') {
      return 'View Documents';
    }
    if (notif.entityType === 'TASK' || notif.category === 'TASK') {
      return 'Open Task';
    }
    if (notif.entityType === 'CLIENT' || notif.category === 'CLIENT') {
      return 'View Client';
    }
    if (notif.category === 'COMPLIANCE') {
      return 'View Calendar';
    }
    if (notif.category === 'BILLING') {
      return 'View Invoices';
    }
    return 'View Details';
  };

  const getSeverityBadge = (severity: NotificationSeverity) => {
    switch (severity) {
      case 'ACTION_REQUIRED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 text-[10px] font-bold rounded-full bg-rose-50 text-rose-700 border border-rose-200">
            <AlertOctagon className="w-3 h-3 text-rose-600" />
            <span>Action Required</span>
          </span>
        );
      case 'WARNING':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 text-[10px] font-bold rounded-full bg-amber-50 text-amber-700 border border-amber-200">
            <AlertTriangle className="w-3 h-3 text-amber-600" />
            <span>Warning</span>
          </span>
        );
      case 'SUCCESS':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 text-[10px] font-bold rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
            <span>Success</span>
          </span>
        );
      case 'INFO':
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 text-[10px] font-bold rounded-full bg-sky-50 text-sky-700 border border-sky-200">
            <Info className="w-3 h-3 text-sky-600" />
            <span>Info</span>
          </span>
        );
    }
  };

  const getCategoryChip = (category: NotificationCategory) => {
    let icon = <Settings className="w-3 h-3 text-slate-500" />;
    let label: string = category;
    let colorClass = 'bg-slate-100 text-slate-700 border-slate-200';

    if (category === 'DOCUMENT') {
      icon = <FileText className="w-3 h-3 text-indigo-600" />;
      label = 'Documents';
      colorClass = 'bg-indigo-50 text-indigo-700 border-indigo-200';
    } else if (category === 'TASK') {
      icon = <CheckSquare className="w-3 h-3 text-blue-600" />;
      label = 'Tasks';
      colorClass = 'bg-blue-50 text-blue-700 border-blue-200';
    } else if (category === 'CLIENT') {
      icon = <Users className="w-3 h-3 text-purple-600" />;
      label = 'Clients';
      colorClass = 'bg-purple-50 text-purple-700 border-purple-200';
    } else if (category === 'COMPLIANCE') {
      icon = <ShieldAlert className="w-3 h-3 text-orange-600" />;
      label = 'Compliance';
      colorClass = 'bg-orange-50 text-orange-700 border-orange-200';
    } else if (category === 'BILLING') {
      icon = <CreditCard className="w-3 h-3 text-emerald-600" />;
      label = 'Billing';
      colorClass = 'bg-emerald-50 text-emerald-700 border-emerald-200';
    } else if (category === 'ACCOUNT') {
      label = 'Security';
      colorClass = 'bg-teal-50 text-teal-700 border-teal-200';
    }

    return (
      <span className={clsx('inline-flex items-center gap-1 px-2 py-0.5 text-[10px] font-semibold rounded-md border', colorClass)}>
        {icon}
        <span>{label}</span>
      </span>
    );
  };

  const formatFullDate = (timestamp?: string) => {
    if (!timestamp) return '';
    try {
      const d = new Date(timestamp);
      return d.toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return '';
    }
  };

  const filteredNotifications = notifications.filter((notif) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      notif.title.toLowerCase().includes(q) ||
      notif.message.toLowerCase().includes(q) ||
      notif.category.toLowerCase().includes(q) ||
      (notif.entityType && notif.entityType.toLowerCase().includes(q))
    );
  });

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      {/* Header & Metric Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Notification Center</h1>
            <span className="px-2 py-0.5 text-xs font-bold bg-brand-50 text-brand-700 rounded-full border border-brand-200">
              V1 Hub
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Real-time feed of all important alerts, client activities, compliance deadlines, and document status updates.
          </p>
        </div>

        {/* Global Action */}
        <div className="flex items-center gap-2">
          {unreadCount > 0 && (
            <button
              type="button"
              onClick={handleMarkAllAsRead}
              className="px-3.5 py-2 text-xs font-semibold bg-white text-slate-700 border border-slate-200 rounded-xl shadow-2xs hover:bg-slate-50 hover:text-brand-600 flex items-center gap-1.5 transition-colors"
            >
              <CheckCheck className="w-4 h-4 text-brand-600" />
              <span>Mark all as read ({unreadCount})</span>
            </button>
          )}
        </div>
      </div>

      {/* Main Filter & Content Card */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
        {/* Top Control Bar: Tabs + Search */}
        <div className="p-4 border-b border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-4 bg-slate-50/50">
          {/* Status Tabs */}
          <div className="flex items-center gap-1 bg-slate-200/70 p-1 rounded-xl w-fit">
            <button
              type="button"
              onClick={() => {
                setTabFilter('ALL');
                setCurrentPage(0);
              }}
              className={clsx(
                'px-4 py-1.5 text-xs font-semibold rounded-lg transition-all',
                tabFilter === 'ALL'
                  ? 'bg-white text-slate-900 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              )}
            >
              All Alerts
            </button>
            <button
              type="button"
              onClick={() => {
                setTabFilter('UNREAD');
                setCurrentPage(0);
              }}
              className={clsx(
                'px-4 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5',
                tabFilter === 'UNREAD'
                  ? 'bg-white text-slate-900 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              )}
            >
              <span>Unread</span>
              {unreadCount > 0 && (
                <span className="px-1.5 py-0.2 text-[10px] font-bold rounded-full bg-rose-500 text-white">
                  {unreadCount}
                </span>
              )}
            </button>
          </div>

          {/* Quick Search */}
          <div className="relative w-full md:w-72">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search notifications..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 text-xs bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all placeholder:text-slate-400"
            />
          </div>
        </div>

        {/* Category & Severity Filter Chips */}
        <div className="px-4 py-3 border-b border-slate-100 flex items-center justify-between gap-3 flex-wrap bg-white">
          {/* Categories */}
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mr-1">Category:</span>
            {[
              { id: 'ALL', label: 'All Categories' },
              { id: 'TASK', label: 'Tasks' },
              { id: 'DOCUMENT', label: 'Documents' },
              { id: 'COMPLIANCE', label: 'Compliance' },
              { id: 'CLIENT', label: 'Clients' },
              { id: 'BILLING', label: 'Billing' },
              { id: 'ACCOUNT', label: 'Security' },
            ].map((cat) => (
              <button
                key={cat.id}
                type="button"
                onClick={() => {
                  setCategoryFilter(cat.id as any);
                  setCurrentPage(0);
                }}
                className={clsx(
                  'px-3 py-1 text-xs font-semibold rounded-lg border transition-all',
                  categoryFilter === cat.id
                    ? 'bg-slate-900 text-white border-slate-900 shadow-2xs'
                    : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
                )}
              >
                {cat.label}
              </button>
            ))}
          </div>

          {/* Severity Filter Dropdown */}
          <div className="flex items-center gap-1.5">
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Severity:</span>
            <select
              value={severityFilter}
              onChange={(e) => {
                setSeverityFilter(e.target.value as any);
                setCurrentPage(0);
              }}
              className="text-xs bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1 text-slate-700 font-medium focus:outline-none focus:ring-1 focus:ring-brand-500"
            >
              <option value="ALL">All Severities</option>
              <option value="ACTION_REQUIRED">Action Required</option>
              <option value="WARNING">Warning</option>
              <option value="SUCCESS">Success</option>
              <option value="INFO">Info</option>
            </select>
          </div>
        </div>

        {/* Notifications Feed */}
        <div className="divide-y divide-slate-100">
          {isLoading ? (
            <div className="py-20 text-center text-xs text-slate-400">
              <div className="inline-block animate-spin rounded-full h-6 w-6 border-2 border-brand-500 border-t-transparent mb-2" />
              <p className="font-semibold text-slate-600">Loading Notification Center feed...</p>
            </div>
          ) : filteredNotifications.length === 0 ? (
            <div className="py-20 px-4 text-center">
              <div className="w-14 h-14 bg-slate-100 rounded-2xl flex items-center justify-center mx-auto mb-3 text-slate-400 shadow-2xs">
                <Inbox className="w-7 h-7" />
              </div>
              <h3 className="text-sm font-bold text-slate-800">No notifications found</h3>
              <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
                {tabFilter === 'UNREAD'
                  ? 'Great job! You have no unread notifications matching the selected filters.'
                  : 'There are no notifications matching your search or filter criteria.'}
              </p>
            </div>
          ) : (
            filteredNotifications.map((notif) => {
              const isRead = notif.isRead || notif.read;
              return (
                <div
                  key={notif.id}
                  className={clsx(
                    'p-4 transition-colors flex flex-col sm:flex-row sm:items-start justify-between gap-4 group',
                    !isRead ? 'bg-brand-50/20' : 'bg-white hover:bg-slate-50/60'
                  )}
                >
                  {/* Left Column: Icon + Meta + Title + Body */}
                  <div className="flex items-start gap-3 flex-1 min-w-0">
                    {/* Unread Accent Dot & Severity Bar */}
                    <div className="shrink-0 mt-1 flex items-center gap-2">
                      <div
                        className={clsx(
                          'w-2 h-2 rounded-full',
                          !isRead ? 'bg-brand-600 ring-4 ring-brand-100' : 'bg-transparent'
                        )}
                      />
                    </div>

                    <div className="space-y-1.5 flex-1 min-w-0">
                      {/* Badge Tags & Timestamp */}
                      <div className="flex items-center gap-2 flex-wrap">
                        {getSeverityBadge(notif.severity)}
                        {getCategoryChip(notif.category)}
                        <span className="text-[11px] text-slate-400">
                          {formatFullDate(notif.createdAt)}
                        </span>
                      </div>

                      {/* Notification Title */}
                      <h4 className={clsx('text-xs sm:text-sm font-bold leading-snug', !isRead ? 'text-slate-900' : 'text-slate-700')}>
                        {notif.title}
                      </h4>

                      {/* Message Content */}
                      <p className="text-xs text-slate-600 leading-relaxed max-w-3xl">
                        {notif.message}
                      </p>
                    </div>
                  </div>

                  {/* Right Column: Actions */}
                  <div className="shrink-0 flex items-center gap-1.5 self-end sm:self-start pt-1">
                    {/* Deep link button */}
                    <button
                      type="button"
                      onClick={() => handleNavigateToEntity(notif)}
                      className="px-3 py-1.5 text-xs font-semibold bg-brand-50 text-brand-700 hover:bg-brand-100 rounded-lg border border-brand-200 flex items-center gap-1 transition-colors"
                    >
                      <span>{getActionLabel(notif)}</span>
                      <ExternalLink className="w-3 h-3" />
                    </button>

                    {/* Toggle Read / Unread */}
                    {isRead ? (
                      <button
                        type="button"
                        title="Mark as unread"
                        onClick={() => handleMarkAsUnread(notif.id)}
                        className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors"
                      >
                        <RotateCcw className="w-4 h-4" />
                      </button>
                    ) : (
                      <button
                        type="button"
                        title="Mark as read"
                        onClick={() => handleMarkAsRead(notif.id)}
                        className="p-1.5 text-slate-400 hover:text-brand-600 hover:bg-brand-50 rounded-lg transition-colors"
                      >
                        <Check className="w-4 h-4" />
                      </button>
                    )}

                    {/* Dismiss Button */}
                    <button
                      type="button"
                      title="Dismiss notification"
                      onClick={() => handleDismiss(notif.id)}
                      className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Server-side Pagination Footer */}
        {totalPages > 1 && (
          <div className="px-4 py-3 bg-slate-50 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <div>
              Showing page <span className="font-semibold text-slate-800">{currentPage + 1}</span> of{' '}
              <span className="font-semibold text-slate-800">{totalPages}</span> ({totalElements} total notifications)
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                disabled={currentPage === 0}
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                className="px-2.5 py-1 text-xs font-semibold bg-white border border-slate-200 rounded-lg text-slate-700 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50 flex items-center gap-1 transition-colors"
              >
                <ChevronLeft className="w-3.5 h-3.5" />
                <span>Previous</span>
              </button>
              <button
                type="button"
                disabled={currentPage >= totalPages - 1}
                onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
                className="px-2.5 py-1 text-xs font-semibold bg-white border border-slate-200 rounded-lg text-slate-700 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50 flex items-center gap-1 transition-colors"
              >
                <span>Next</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};