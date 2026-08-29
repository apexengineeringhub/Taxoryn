import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Bell, 
  Check, 
  CheckCheck, 
  AlertTriangle, 
  AlertOctagon, 
  Info, 
  CheckCircle2, 
  ExternalLink,
  ArrowRight,
  FileText,
  CheckSquare,
  Users,
  ShieldAlert,
  CreditCard,
  Settings
} from 'lucide-react';
import clsx from 'clsx';
import { notificationApi } from '../../api/endpoints';
import { NotificationItem, NotificationSeverity, NotificationCategory } from '../../types';

export const NotificationBellDropdown: React.FC = () => {
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'UNREAD'>('ALL');
  const dropdownRef = useRef<HTMLDivElement>(null);

  // 1. Fetch unread count
  const fetchUnreadCount = async () => {
    try {
      const data = await notificationApi.getUnreadCount();
      setUnreadCount(data.unreadCount || 0);
    } catch {
      // ignore network errors silently
    }
  };

  // 2. Fetch preview notifications when opened
  const fetchPreviewNotifications = async () => {
    setIsLoading(true);
    try {
      const res = await notificationApi.getAll({
        page: 0,
        size: 8,
        isRead: activeFilter === 'UNREAD' ? false : undefined
      });
      setNotifications(res.content || []);
    } catch {
      // ignore
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 30000); // 30s poll
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (isOpen) {
      fetchPreviewNotifications();
    }
  }, [isOpen, activeFilter]);

  // Handle outside click & escape key
  useEffect(() => {
    const handleOutsideClick = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleOutsideClick);
      document.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  const handleMarkAsRead = async (id: string, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    try {
      await notificationApi.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true, read: true } : n))
      );
      setUnreadCount((c) => Math.max(0, c - 1));
    } catch {
      // ignore
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true, read: true })));
      setUnreadCount(0);
    } catch {
      // ignore
    }
  };

  const handleNotificationClick = async (notif: NotificationItem) => {
    if (!notif.isRead && !notif.read) {
      handleMarkAsRead(notif.id);
    }
    setIsOpen(false);

    // Smart deeplink routing
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
      navigate('/notifications');
    }
  };

  const getSeverityIcon = (severity: NotificationSeverity) => {
    switch (severity) {
      case 'ACTION_REQUIRED':
        return <AlertOctagon className="w-4 h-4 text-rose-600" />;
      case 'WARNING':
        return <AlertTriangle className="w-4 h-4 text-amber-600" />;
      case 'SUCCESS':
        return <CheckCircle2 className="w-4 h-4 text-emerald-600" />;
      case 'INFO':
      default:
        return <Info className="w-4 h-4 text-sky-600" />;
    }
  };

  const getCategoryIcon = (category: NotificationCategory) => {
    switch (category) {
      case 'DOCUMENT':
        return <FileText className="w-3 h-3 text-indigo-500" />;
      case 'TASK':
        return <CheckSquare className="w-3 h-3 text-blue-500" />;
      case 'CLIENT':
        return <Users className="w-3 h-3 text-purple-500" />;
      case 'COMPLIANCE':
        return <ShieldAlert className="w-3 h-3 text-orange-500" />;
      case 'BILLING':
        return <CreditCard className="w-3 h-3 text-emerald-500" />;
      case 'ACCOUNT':
      case 'SYSTEM':
      default:
        return <Settings className="w-3 h-3 text-slate-500" />;
    }
  };

  const formatRelativeTime = (timestamp?: string) => {
    if (!timestamp) return '';
    try {
      const date = new Date(timestamp);
      const diffSeconds = Math.floor((Date.now() - date.getTime()) / 1000);
      if (diffSeconds < 60) return 'Just now';
      const diffMinutes = Math.floor(diffSeconds / 60);
      if (diffMinutes < 60) return `${diffMinutes}m ago`;
      const diffHours = Math.floor(diffMinutes / 60);
      if (diffHours < 24) return `${diffHours}h ago`;
      const diffDays = Math.floor(diffHours / 24);
      if (diffDays < 7) return `${diffDays}d ago`;
      return date.toLocaleDateString('en-IN', { month: 'short', day: 'numeric' });
    } catch {
      return '';
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Header Bell Button */}
      <button
        type="button"
        title="Notifications"
        onClick={() => setIsOpen(!isOpen)}
        className={clsx(
          'relative p-2 rounded-lg transition-colors',
          isOpen ? 'bg-slate-100 text-brand-600' : 'text-slate-500 hover:text-slate-700 hover:bg-slate-100'
        )}
      >
        <Bell className="w-4 h-4" />
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 bg-rose-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center ring-2 ring-white shadow-xs animate-in fade-in zoom-in-75">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {/* Floating Popover Dropdown */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-150">
          {/* Header */}
          <div className="px-4 py-3 bg-slate-50 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-slate-800 tracking-tight">Notifications</span>
              {unreadCount > 0 && (
                <span className="px-1.5 py-0.5 text-[10px] font-bold bg-rose-100 text-rose-700 rounded-full">
                  {unreadCount} new
                </span>
              )}
            </div>
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={handleMarkAllRead}
                className="text-[11px] font-medium text-brand-600 hover:text-brand-700 hover:underline flex items-center gap-1 transition-colors"
              >
                <CheckCheck className="w-3.5 h-3.5" />
                <span>Mark all read</span>
              </button>
            )}
          </div>

          {/* Filter Pills */}
          <div className="flex items-center gap-1 px-3 py-2 bg-slate-50/50 border-b border-slate-100">
            <button
              type="button"
              onClick={() => setActiveFilter('ALL')}
              className={clsx(
                'px-2.5 py-1 text-[11px] font-semibold rounded-md transition-colors',
                activeFilter === 'ALL'
                  ? 'bg-white text-slate-900 shadow-2xs border border-slate-200'
                  : 'text-slate-500 hover:text-slate-700 hover:bg-slate-100'
              )}
            >
              All
            </button>
            <button
              type="button"
              onClick={() => setActiveFilter('UNREAD')}
              className={clsx(
                'px-2.5 py-1 text-[11px] font-semibold rounded-md transition-colors flex items-center gap-1.5',
                activeFilter === 'UNREAD'
                  ? 'bg-white text-slate-900 shadow-2xs border border-slate-200'
                  : 'text-slate-500 hover:text-slate-700 hover:bg-slate-100'
              )}
            >
              <span>Unread</span>
              {unreadCount > 0 && (
                <span className="w-1.5 h-1.5 rounded-full bg-rose-500" />
              )}
            </button>
          </div>

          {/* Notification Items List */}
          <div className="max-h-[380px] overflow-y-auto divide-y divide-slate-100">
            {isLoading ? (
              <div className="py-8 text-center text-xs text-slate-400">
                <div className="inline-block animate-spin rounded-full h-4 w-4 border-2 border-brand-500 border-t-transparent mb-1" />
                <p>Loading alerts...</p>
              </div>
            ) : notifications.length === 0 ? (
              <div className="py-10 px-4 text-center">
                <div className="w-10 h-10 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-2 text-slate-400">
                  <Bell className="w-5 h-5" />
                </div>
                <p className="text-xs font-semibold text-slate-700">No notifications</p>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  {activeFilter === 'UNREAD' ? 'You have caught up with all alerts.' : 'No alerts recorded yet.'}
                </p>
              </div>
            ) : (
              notifications.map((notif) => {
                const isRead = notif.isRead || notif.read;
                return (
                  <div
                    key={notif.id}
                    onClick={() => handleNotificationClick(notif)}
                    className={clsx(
                      'p-3 hover:bg-slate-50/80 cursor-pointer transition-colors flex items-start gap-3 text-left relative group',
                      !isRead ? 'bg-brand-50/20' : 'bg-white'
                    )}
                  >
                    {/* Severity Icon Indicator */}
                    <div className="shrink-0 mt-0.5 p-1.5 rounded-lg bg-slate-100/80">
                      {getSeverityIcon(notif.severity)}
                    </div>

                    {/* Content */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-1.5">
                        <div className="flex items-center gap-1.5 flex-wrap">
                          <span className={clsx('text-xs font-semibold truncate', !isRead ? 'text-slate-900' : 'text-slate-700')}>
                            {notif.title}
                          </span>
                          <span className="inline-flex items-center gap-1 px-1.5 py-0.2 text-[9px] font-bold rounded bg-slate-100 text-slate-600">
                            {getCategoryIcon(notif.category)}
                            <span>{notif.category}</span>
                          </span>
                        </div>
                        <span className="text-[10px] text-slate-400 shrink-0 whitespace-nowrap">
                          {formatRelativeTime(notif.createdAt)}
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-500 line-clamp-2 mt-0.5 leading-relaxed">
                        {notif.message}
                      </p>
                    </div>

                    {/* Unread dot or mark button */}
                    {!isRead && (
                      <div className="shrink-0 flex items-center gap-1">
                        <span className="w-2 h-2 rounded-full bg-brand-600 ring-2 ring-brand-100" />
                        <button
                          type="button"
                          title="Mark as read"
                          onClick={(e) => handleMarkAsRead(notif.id, e)}
                          className="opacity-0 group-hover:opacity-100 p-1 hover:bg-slate-200 rounded text-slate-500 transition-opacity"
                        >
                          <Check className="w-3 h-3" />
                        </button>
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>

          {/* Footer View All Link */}
          <div className="p-2.5 bg-slate-50 border-t border-slate-100 text-center">
            <button
              type="button"
              onClick={() => {
                setIsOpen(false);
                navigate('/notifications');
              }}
              className="w-full py-1.5 px-3 text-xs font-semibold text-brand-600 hover:text-brand-700 hover:bg-brand-50 rounded-lg flex items-center justify-center gap-1.5 transition-colors"
            >
              <span>View All Notifications</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};