import React, { useEffect, useState, useCallback } from 'react';
import {
  Inbox,
  Search,
  Filter,
  RefreshCw,
  Mail,
  Send,
  ExternalLink,
  Clock,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  UserCheck,
  Building,
  User,
  MessageSquare,
  Sparkles,
  ChevronRight,
  X,
  Tag,
  AtSign,
  Shield,
  CornerUpLeft,
} from 'lucide-react';
import { gmailApi } from '../api/endpoints';
import {
  GmailConversation,
  GmailMessageView,
  GmailAccount,
  GmailMetrics,
  GmailConversationStatus,
  GmailConversationPriority,
} from '../types';
import { Button } from '../components/common/Button';
import { WorkspacePageHeader } from '../components/layout/WorkspacePageHeader';
import clsx from 'clsx';

export const PlatformEnquiriesPage: React.FC = () => {
  const [conversations, setConversations] = useState<GmailConversation[]>([]);
  const [metrics, setMetrics] = useState<GmailMetrics | null>(null);
  const [accounts, setAccounts] = useState<GmailAccount[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'ALL' | 'PENDING' | 'REPLIED' | 'PRACTITIONER' | 'SUPPORT'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedMailbox, setSelectedMailbox] = useState<string>('ALL');

  // Drawer / Conversation Detail State
  const [selectedConv, setSelectedConv] = useState<GmailConversation | null>(null);
  const [messages, setMessages] = useState<GmailMessageView[]>([]);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);

  // Reply Composer State
  const [replyBody, setReplyBody] = useState('');
  const [replySubject, setReplySubject] = useState('');
  const [replyTo, setReplyTo] = useState('');
  const [replyAccountId, setReplyAccountId] = useState<string>('');
  const [isSendingReply, setIsSendingReply] = useState(false);
  const [replySuccessMessage, setReplySuccessMessage] = useState<string | null>(null);
  const [replyErrorMessage, setReplyErrorMessage] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      setIsLoading(true);
      const [convRes, metricsRes, accountsRes] = await Promise.allSettled([
        gmailApi.getConversations({ size: 100 }),
        gmailApi.getMetrics(),
        gmailApi.getAccounts(),
      ]);

      if (convRes.status === 'fulfilled' && convRes.value) {
        setConversations(convRes.value.content || []);
      }
      if (metricsRes.status === 'fulfilled' && metricsRes.value) {
        setMetrics(metricsRes.value);
      }
      if (accountsRes.status === 'fulfilled' && accountsRes.value) {
        setAccounts(accountsRes.value || []);
      }
    } catch (err) {
      console.error('Failed to load enquiries data', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Open Conversation Detail & Fetch Live Messages
  const handleOpenConversation = async (conv: GmailConversation) => {
    setSelectedConv(conv);
    setReplyTo(conv.senderEmail || '');
    setReplySubject(conv.subject ? (conv.subject.startsWith('Re:') ? conv.subject : `Re: ${conv.subject}`) : 'Re: Enquiry');
    setReplyAccountId(conv.gmailAccountId || (accounts.length > 0 ? accounts[0].id : ''));
    setReplyBody('');
    setReplySuccessMessage(null);
    setReplyErrorMessage(null);

    try {
      setIsLoadingMessages(true);
      const liveMsgs = await gmailApi.getConversationMessages(conv.id);
      setMessages(liveMsgs || []);
    } catch (err) {
      console.warn('Failed to load live messages, displaying fallback snippet', err);
      // Fallback message view from metadata
      setMessages([
        {
          id: conv.id,
          threadId: conv.threadId,
          from: conv.senderName ? `${conv.senderName} <${conv.senderEmail}>` : conv.senderEmail,
          to: conv.recipientEmails,
          subject: conv.subject,
          snippet: conv.snippet,
          bodyPlain: conv.snippet,
          date: conv.lastMessageAt || conv.createdAt,
          labelIds: [],
        },
      ]);
    } finally {
      setIsLoadingMessages(false);
    }
  };

  const handleCloseDrawer = () => {
    setSelectedConv(null);
    setMessages([]);
    setReplySuccessMessage(null);
    setReplyErrorMessage(null);
  };

  // Send Reply via Gmail API
  const handleSendReply = async () => {
    if (!selectedConv || !replyBody.trim()) {
      setReplyErrorMessage('Please enter a reply message.');
      return;
    }

    try {
      setIsSendingReply(true);
      setReplyErrorMessage(null);
      setReplySuccessMessage(null);

      const updated = await gmailApi.sendReply(selectedConv.id, {
        replyAccountId: replyAccountId || undefined,
        to: replyTo,
        subject: replySubject,
        body: replyBody,
        inReplyToMessageId: messages.length > 0 ? messages[messages.length - 1].id : undefined,
      });

      setReplySuccessMessage('Reply sent successfully via Google Workspace!');
      setReplyBody('');

      // Update local state
      setSelectedConv(updated);
      setConversations((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));

      // Refresh messages
      const refreshed = await gmailApi.getConversationMessages(updated.id);
      setMessages(refreshed || []);
    } catch (err: any) {
      console.error('Failed to send reply', err);
      const msg = err.response?.data?.message || err.message || 'Failed to dispatch reply via Gmail API.';
      setReplyErrorMessage(msg);
    } finally {
      setIsSendingReply(false);
    }
  };

  // Quick Status Update
  const handleUpdateStatus = async (convId: string, status: GmailConversationStatus) => {
    try {
      const updated = await gmailApi.updateConversation(convId, { status });
      setConversations((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
      if (selectedConv?.id === convId) {
        setSelectedConv(updated);
      }
    } catch (err) {
      console.error('Failed to update status', err);
    }
  };

  // Tab Filtering & Search
  const filteredConversations = conversations.filter((c) => {
    // Mailbox filter
    if (selectedMailbox !== 'ALL' && c.gmailAccountId !== selectedMailbox) {
      return false;
    }

    // Tab filter
    if (activeTab === 'PENDING') {
      if (c.status === 'REPLIED' || c.status === 'RESOLVED' || c.status === 'CLOSED') {
        return false;
      }
    } else if (activeTab === 'REPLIED') {
      if (c.status !== 'REPLIED') return false;
    } else if (activeTab === 'PRACTITIONER') {
      if (c.category !== 'PRACTITIONER_ENQUIRY') return false;
    } else if (activeTab === 'SUPPORT') {
      if (c.category !== 'SUPPORT_REQUEST') return false;
    }

    // Keyword Search
    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      const matchSubject = c.subject?.toLowerCase().includes(q);
      const matchSnippet = c.snippet?.toLowerCase().includes(q);
      const matchSender = c.senderName?.toLowerCase().includes(q) || c.senderEmail?.toLowerCase().includes(q);
      const matchRecipient = c.recipientEmails?.toLowerCase().includes(q);
      if (!matchSubject && !matchSnippet && !matchSender && !matchRecipient) {
        return false;
      }
    }

    return true;
  });

  // Category counts
  const totalCount = conversations.length;
  const pendingCount = conversations.filter((c) => c.status !== 'REPLIED' && c.status !== 'RESOLVED' && c.status !== 'CLOSED').length;
  const repliedCount = conversations.filter((c) => c.status === 'REPLIED').length;
  const practitionerCount = conversations.filter((c) => c.category === 'PRACTITIONER_ENQUIRY').length;
  const supportCount = conversations.filter((c) => c.category === 'SUPPORT_REQUEST').length;

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      {/* Header */}
      <WorkspacePageHeader
        sectionBadge="Email & Enquiries"
        sectionBadgeStyle="bg-purple-100 text-purple-800 border-purple-200"
        title="SuperAdmin Email & Enquiry Management"
        titleIcon={Inbox}
        titleIconColor="text-purple-600"
        description="Unified live communication center for incoming practitioner onboarding leads, support requests, and platform enquiries connected to Google Workspace."
      >
        <Button variant="secondary" onClick={loadData} disabled={isLoading} className="text-xs gap-1.5 font-bold shadow-2xs">
          <RefreshCw className={clsx('w-3.5 h-3.5', isLoading && 'animate-spin')} /> Refresh
        </Button>
      </WorkspacePageHeader>

      {/* Metrics Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
        <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wide">Total Enquiries</span>
            <Inbox className="w-4 h-4 text-purple-600" />
          </div>
          <div className="text-2xl font-bold text-slate-900 mt-2">{metrics?.totalConversations ?? totalCount}</div>
          <div className="text-[11px] text-slate-400 mt-0.5">Across connected mailboxes</div>
        </div>

        <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-amber-700 uppercase tracking-wide">Pending Action</span>
            <Clock className="w-4 h-4 text-amber-600" />
          </div>
          <div className="text-2xl font-bold text-amber-700 mt-2">{pendingCount}</div>
          <div className="text-[11px] text-amber-600/80 mt-0.5">Awaiting initial reply</div>
        </div>

        <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-emerald-700 uppercase tracking-wide">Replied</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="text-2xl font-bold text-emerald-700 mt-2">{repliedCount}</div>
          <div className="text-[11px] text-emerald-600/80 mt-0.5">Directly dispatched</div>
        </div>

        <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-blue-700 uppercase tracking-wide">Practitioner Leads</span>
            <UserCheck className="w-4 h-4 text-blue-600" />
          </div>
          <div className="text-2xl font-bold text-blue-700 mt-2">{practitionerCount}</div>
          <div className="text-[11px] text-blue-600/80 mt-0.5">Onboarding & Demo</div>
        </div>

        <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs col-span-2 sm:col-span-1">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-indigo-700 uppercase tracking-wide">Support Requests</span>
            <HelpCircle className="w-4 h-4 text-indigo-600" />
          </div>
          <div className="text-2xl font-bold text-indigo-700 mt-2">{supportCount}</div>
          <div className="text-[11px] text-indigo-600/80 mt-0.5">Platform help & tickets</div>
        </div>
      </div>

      {/* Filter Tabs & Search Bar */}
      <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs space-y-3">
        <div className="flex flex-col md:flex-row items-center justify-between gap-3">
          {/* Tabs */}
          <div className="flex items-center gap-1.5 w-full md:w-auto overflow-x-auto no-scrollbar pb-1">
            {[
              { id: 'ALL', label: 'All Enquiries', count: totalCount },
              { id: 'PENDING', label: 'Pending Reply', count: pendingCount },
              { id: 'REPLIED', label: 'Replied', count: repliedCount },
              { id: 'PRACTITIONER', label: 'Practitioner Leads', count: practitionerCount },
              { id: 'SUPPORT', label: 'Support Requests', count: supportCount },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as any)}
                className={clsx(
                  'px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-2 shrink-0',
                  activeTab === tab.id
                    ? 'bg-purple-600 text-white shadow-2xs'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                )}
              >
                <span>{tab.label}</span>
                <span
                  className={clsx(
                    'px-1.5 py-0.2 rounded-full text-[10px] font-mono',
                    activeTab === tab.id ? 'bg-purple-700/80 text-white' : 'bg-slate-200 text-slate-700'
                  )}
                >
                  {tab.count}
                </span>
              </button>
            ))}
          </div>

          {/* Mailbox Selector */}
          {accounts.length > 0 && (
            <div className="flex items-center gap-2 shrink-0">
              <span className="text-xs font-semibold text-slate-500">Mailbox:</span>
              <select
                value={selectedMailbox}
                onChange={(e) => setSelectedMailbox(e.target.value)}
                className="text-xs font-semibold bg-slate-50 border border-slate-300 rounded-lg px-2.5 py-1.5 focus:ring-2 focus:ring-purple-500 focus:outline-none"
              >
                <option value="ALL">All Connected Mailboxes</option>
                {accounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.emailAddress}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

        {/* Search Input */}
        <div className="relative w-full">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search enquiries by subject, sender name, email address, or content..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
          />
        </div>
      </div>

      {/* Enquiries List Table */}
      <div className="bg-white border border-slate-200 rounded-xl shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 font-semibold text-slate-500 uppercase tracking-wider">
                <th className="px-5 py-3">Sender & Organization</th>
                <th className="px-4 py-3">Subject & Snippet</th>
                <th className="px-4 py-3">Category</th>
                <th className="px-4 py-3">Mailbox</th>
                <th className="px-4 py-3">Received</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-5 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="text-center py-12 text-slate-400">
                    <RefreshCw className="w-5 h-5 animate-spin mx-auto mb-2 text-purple-600" />
                    Loading enquiries from Gmail sync...
                  </td>
                </tr>
              ) : filteredConversations.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-12 text-slate-400">
                    <Inbox className="w-6 h-6 mx-auto mb-2 text-slate-300" />
                    No enquiries found matching active filters
                  </td>
                </tr>
              ) : (
                filteredConversations.map((conv) => {
                  const isPending = conv.status !== 'REPLIED' && conv.status !== 'RESOLVED' && conv.status !== 'CLOSED';
                  return (
                    <tr
                      key={conv.id}
                      onClick={() => handleOpenConversation(conv)}
                      className={clsx(
                        'hover:bg-purple-50/40 cursor-pointer transition-colors',
                        conv.isUnread && 'bg-slate-50/80 font-medium'
                      )}
                    >
                      {/* Sender */}
                      <td className="px-5 py-3.5">
                        <div className="font-bold text-slate-900 leading-tight">
                          {conv.senderName || conv.senderEmail.split('@')[0]}
                        </div>
                        <div className="text-[11px] text-slate-500 font-mono mt-0.5">{conv.senderEmail}</div>
                        {conv.clientDisplayName && (
                          <div className="flex items-center gap-1 text-[10px] text-purple-700 font-semibold mt-1">
                            <Building className="w-3 h-3" />
                            <span>{conv.clientDisplayName}</span>
                          </div>
                        )}
                      </td>

                      {/* Subject & Snippet */}
                      <td className="px-4 py-3.5 max-w-xs md:max-w-md">
                        <div className="flex items-center gap-1.5">
                          {conv.isUnread && (
                            <span className="w-2 h-2 rounded-full bg-purple-600 shrink-0" title="Unread" />
                          )}
                          <span className="font-semibold text-slate-900 truncate">{conv.subject || '(No Subject)'}</span>
                        </div>
                        <p className="text-slate-500 text-[11px] line-clamp-1 mt-0.5">
                          {conv.snippet || 'No message preview available'}
                        </p>
                      </td>

                      {/* Category Badge */}
                      <td className="px-4 py-3.5">
                        <span
                          className={clsx(
                            'px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wide border',
                            conv.category === 'PRACTITIONER_ENQUIRY'
                              ? 'bg-blue-50 text-blue-700 border-blue-200'
                              : conv.category === 'SUPPORT_REQUEST'
                              ? 'bg-indigo-50 text-indigo-700 border-indigo-200'
                              : 'bg-slate-50 text-slate-700 border-slate-200'
                          )}
                        >
                          {conv.category === 'PRACTITIONER_ENQUIRY'
                            ? 'Practitioner'
                            : conv.category === 'SUPPORT_REQUEST'
                            ? 'Support'
                            : 'General'}
                        </span>
                      </td>

                      {/* Mailbox */}
                      <td className="px-4 py-3.5 text-slate-600 text-[11px] font-mono">
                        {conv.mailboxEmail || 'support@taxoryn.com'}
                      </td>

                      {/* Received Date */}
                      <td className="px-4 py-3.5 text-slate-500 whitespace-nowrap text-[11px]">
                        {conv.lastMessageAt
                          ? new Date(conv.lastMessageAt).toLocaleString('en-IN', {
                              dateStyle: 'short',
                              timeStyle: 'short',
                            })
                          : 'N/A'}
                      </td>

                      {/* Status */}
                      <td className="px-4 py-3.5">
                        <span
                          className={clsx(
                            'px-2.5 py-0.5 rounded-full text-[10px] font-bold border',
                            conv.status === 'REPLIED'
                              ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                              : conv.status === 'RESOLVED' || conv.status === 'CLOSED'
                              ? 'bg-slate-100 text-slate-700 border-slate-200'
                              : 'bg-amber-50 text-amber-700 border-amber-200'
                          )}
                        >
                          {conv.status}
                        </span>
                      </td>

                      {/* Actions */}
                      <td className="px-5 py-3.5 text-right whitespace-nowrap">
                        <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
                          {conv.webLink && (
                            <a
                              href={conv.webLink}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="p-1.5 text-slate-400 hover:text-purple-600 hover:bg-purple-50 rounded-md transition-colors"
                              title="Open Thread in Gmail"
                            >
                              <ExternalLink className="w-3.5 h-3.5" />
                            </a>
                          )}
                          <Button
                            size="sm"
                            variant="secondary"
                            onClick={() => handleOpenConversation(conv)}
                            className="text-xs font-semibold gap-1"
                          >
                            Open <ChevronRight className="w-3.5 h-3.5" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Conversation Detail & Live Reply Drawer */}
      {selectedConv && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex justify-end animate-fade-in">
          <div className="bg-white w-full max-w-3xl h-full shadow-2xl flex flex-col border-l border-slate-200 animate-slide-left">
            {/* Drawer Header */}
            <div className="px-6 py-4 border-b border-slate-200 bg-slate-50/80 flex items-start justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span
                    className={clsx(
                      'px-2 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wide border',
                      selectedConv.category === 'PRACTITIONER_ENQUIRY'
                        ? 'bg-blue-50 text-blue-700 border-blue-200'
                        : selectedConv.category === 'SUPPORT_REQUEST'
                        ? 'bg-indigo-50 text-indigo-700 border-indigo-200'
                        : 'bg-slate-50 text-slate-700 border-slate-200'
                    )}
                  >
                    {selectedConv.category?.replace('_', ' ')}
                  </span>
                  <span
                    className={clsx(
                      'px-2 py-0.5 rounded-full text-[10px] font-bold border',
                      selectedConv.status === 'REPLIED'
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                        : selectedConv.status === 'RESOLVED'
                        ? 'bg-slate-100 text-slate-700 border-slate-200'
                        : 'bg-amber-50 text-amber-700 border-amber-200'
                    )}
                  >
                    {selectedConv.status}
                  </span>
                </div>
                <h3 className="text-base font-bold text-slate-900 leading-snug">
                  {selectedConv.subject || '(No Subject)'}
                </h3>
                <div className="text-xs text-slate-500 flex items-center gap-3">
                  <span>From: <strong className="text-slate-800">{selectedConv.senderName || selectedConv.senderEmail}</strong> ({selectedConv.senderEmail})</span>
                  <span>•</span>
                  <span>Mailbox: <strong className="text-slate-800">{selectedConv.mailboxEmail || 'support@taxoryn.com'}</strong></span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                {selectedConv.webLink && (
                  <a
                    href={selectedConv.webLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg border border-slate-300 bg-white hover:bg-slate-50 text-slate-700 transition-colors"
                  >
                    <ExternalLink className="w-3.5 h-3.5 text-purple-600" />
                    <span>Open in Gmail</span>
                  </a>
                )}
                <button
                  onClick={handleCloseDrawer}
                  className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            {/* Drawer Body: Messages List */}
            <div className="flex-1 overflow-y-auto p-6 space-y-4 bg-slate-50/50">
              {isLoadingMessages ? (
                <div className="text-center py-16 text-slate-400">
                  <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-purple-600" />
                  Fetching live conversation thread directly from Gmail...
                </div>
              ) : messages.length === 0 ? (
                <div className="text-center py-16 text-slate-400 text-xs">
                  No live messages found in this conversation.
                </div>
              ) : (
                messages.map((msg, index) => (
                  <div
                    key={msg.id || index}
                    className="bg-white border border-slate-200/80 rounded-xl p-4 shadow-2xs space-y-2.5"
                  >
                    <div className="flex items-start justify-between gap-2 border-b border-slate-100 pb-2">
                      <div>
                        <div className="font-bold text-xs text-slate-900">{msg.from}</div>
                        <div className="text-[11px] text-slate-500">To: {msg.to || selectedConv.recipientEmails}</div>
                      </div>
                      <div className="text-[11px] text-slate-400 whitespace-nowrap">
                        {msg.date
                          ? new Date(msg.date).toLocaleString('en-IN', {
                              dateStyle: 'medium',
                              timeStyle: 'short',
                            })
                          : ''}
                      </div>
                    </div>

                    {/* Body rendering */}
                    <div className="text-xs text-slate-800 leading-relaxed whitespace-pre-wrap font-sans">
                      {msg.bodyPlain ? (
                        msg.bodyPlain
                      ) : msg.bodyHtml ? (
                        <div
                          dangerouslySetInnerHTML={{ __html: msg.bodyHtml }}
                          className="prose prose-xs max-w-none text-slate-800"
                        />
                      ) : (
                        <span className="text-slate-400 italic">{msg.snippet || '(Empty Body)'}</span>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Reply Composer Footer */}
            <div className="p-4 bg-white border-t border-slate-200 space-y-3 shadow-lg">
              {replySuccessMessage && (
                <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-lg p-2.5 flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>{replySuccessMessage}</span>
                </div>
              )}
              {replyErrorMessage && (
                <div className="bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-lg p-2.5 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                  <span>{replyErrorMessage}</span>
                </div>
              )}

              <div className="space-y-2">
                <div className="flex flex-col sm:flex-row items-center gap-2">
                  {/* From Mailbox */}
                  <div className="w-full sm:w-1/2">
                    <label className="block text-[11px] font-semibold text-slate-500 mb-0.5">Send From:</label>
                    <select
                      value={replyAccountId}
                      onChange={(e) => setReplyAccountId(e.target.value)}
                      className="w-full text-xs font-semibold bg-slate-50 border border-slate-300 rounded-lg p-1.5 focus:ring-2 focus:ring-purple-500"
                    >
                      {accounts.map((acc) => (
                        <option key={acc.id} value={acc.id}>
                          {acc.emailAddress}
                        </option>
                      ))}
                      {accounts.length === 0 && (
                        <option value="">{selectedConv.mailboxEmail || 'support@taxoryn.com'}</option>
                      )}
                    </select>
                  </div>

                  {/* To Recipient */}
                  <div className="w-full sm:w-1/2">
                    <label className="block text-[11px] font-semibold text-slate-500 mb-0.5">To Recipient:</label>
                    <input
                      type="email"
                      value={replyTo}
                      onChange={(e) => setReplyTo(e.target.value)}
                      className="w-full text-xs bg-slate-50 border border-slate-300 rounded-lg p-1.5 focus:ring-2 focus:ring-purple-500"
                    />
                  </div>
                </div>

                {/* Reply Body Textarea */}
                <div>
                  <textarea
                    rows={4}
                    value={replyBody}
                    onChange={(e) => setReplyBody(e.target.value)}
                    placeholder="Type your reply here... (Sent directly through connected Google Workspace mailbox)"
                    className="w-full text-xs p-3 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent font-sans"
                  />
                </div>

                {/* Send & Status Actions */}
                <div className="flex items-center justify-between pt-1">
                  <div className="flex items-center gap-1.5">
                    <span className="text-[11px] text-slate-400">Mark as:</span>
                    <button
                      type="button"
                      onClick={() => handleUpdateStatus(selectedConv.id, 'RESOLVED')}
                      className="text-[11px] font-semibold text-slate-600 hover:text-purple-700 bg-slate-100 hover:bg-slate-200 px-2 py-1 rounded"
                    >
                      Resolved
                    </button>
                    <button
                      type="button"
                      onClick={() => handleUpdateStatus(selectedConv.id, 'CLOSED')}
                      className="text-[11px] font-semibold text-slate-600 hover:text-purple-700 bg-slate-100 hover:bg-slate-200 px-2 py-1 rounded"
                    >
                      Closed
                    </button>
                  </div>

                  <Button
                    variant="primary"
                    size="sm"
                    onClick={handleSendReply}
                    disabled={isSendingReply || !replyBody.trim()}
                    className="text-xs font-bold gap-1.5"
                  >
                    <Send className={clsx('w-3.5 h-3.5', isSendingReply && 'animate-spin')} />
                    {isSendingReply ? 'Sending via Gmail...' : 'Send Reply'}
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
