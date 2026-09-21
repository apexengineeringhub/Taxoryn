import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  MessageSquare,
  Hash,
  Search,
  Send,
  Paperclip,
  Check,
  CheckCheck,
  Smile,
  Shield,
  Users,
  Briefcase,
  Wifi,
  WifiOff,
  RefreshCw,
  Building,
  User,
  Sparkles,
  Info,
  X,
  FileText
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import {
  employeeChatApi,
  EmployeeChatContact,
  EmployeeChatChannel,
  EmployeeChatMessage
} from '../api/employeeChatApi';
import { useEmployeeChat } from '../hooks/useEmployeeChat';

type ChatMode = 'CHANNEL' | 'DIRECT';

export const TeamChatPage: React.FC = () => {
  const { user } = useAuth();

  // Navigation State
  const [mode, setMode] = useState<ChatMode>('CHANNEL');
  const [selectedChannelId, setSelectedChannelId] = useState<string | null>(null);
  const [selectedContactId, setSelectedContactId] = useState<string | null>(null);

  // Data State
  const [channels, setChannels] = useState<EmployeeChatChannel[]>([]);
  const [contacts, setContacts] = useState<EmployeeChatContact[]>([]);
  const [messages, setMessages] = useState<EmployeeChatMessage[]>([]);
  const [isLoadingMessages, setIsLoadingMessages] = useState<boolean>(false);
  const [isLoadingSidebar, setIsLoadingSidebar] = useState<boolean>(true);

  // Input State
  const [messageInput, setMessageInput] = useState<string>('');
  const [isSending, setIsSending] = useState<boolean>(false);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [messageSearchQuery, setMessageSearchQuery] = useState<string>('');
  const [typingMap, setTypingMap] = useState<Record<string, boolean>>({});

  // Attachment Modal
  const [isAttachmentModalOpen, setIsAttachmentModalOpen] = useState<boolean>(false);
  const [attachmentName, setAttachmentName] = useState<string>('');
  const [attachmentUrl, setAttachmentUrl] = useState<string>('');

  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const chatContainerRef = useRef<HTMLDivElement | null>(null);
  const typingTimerRef = useRef<NodeJS.Timeout | null>(null);

  // Active Target Information
  const activeChannel = channels.find((c) => c.id === selectedChannelId);
  const activeContact = contacts.find((c) => c.employeeId === selectedContactId);

  // Real-time WebSocket integration
  const handleIncomingMessage = useCallback((incoming: EmployeeChatMessage) => {
    // If incoming message belongs to currently active channel or direct conversation, append it
    if (mode === 'CHANNEL' && incoming.channelId && incoming.channelId === selectedChannelId) {
      setMessages((prev) => {
        if (prev.some((m) => m.id === incoming.id)) return prev;
        return [...prev, incoming];
      });
    } else if (
      mode === 'DIRECT' &&
      !incoming.channelId &&
      (incoming.senderEmployeeId === selectedContactId || incoming.recipientEmployeeId === selectedContactId)
    ) {
      setMessages((prev) => {
        if (prev.some((m) => m.id === incoming.id)) return prev;
        return [...prev, incoming];
      });

      // If we are currently chatting with this sender, mark as read immediately
      if (incoming.senderEmployeeId === selectedContactId && selectedContactId) {
        employeeChatApi.markDirectMessagesRead(selectedContactId).catch(() => {});
      }
    }

    // Update last message in sidebar contacts / channels
    if (incoming.channelId) {
      setChannels((prev) =>
        prev.map((ch) =>
          ch.id === incoming.channelId
            ? { ...ch, lastMessagePreview: incoming.messageBody, lastMessageTimestamp: incoming.createdAt }
            : ch
        )
      );
    } else {
      setContacts((prev) =>
        prev.map((c) => {
          if (c.employeeId === incoming.senderEmployeeId || c.employeeId === incoming.recipientEmployeeId) {
            const isUnread = incoming.senderEmployeeId === c.employeeId && selectedContactId !== c.employeeId;
            return {
              ...c,
              lastMessagePreview: incoming.messageBody,
              lastMessageTimestamp: incoming.createdAt,
              unreadCount: isUnread ? (c.unreadCount || 0) + 1 : c.unreadCount,
            };
          }
          return c;
        })
      );
    }
  }, [mode, selectedChannelId, selectedContactId]);

  const handleMessagesReadEvent = useCallback((senderEmpId?: string) => {
    // If sender read our messages, update read status to true in current message feed
    setMessages((prev) =>
      prev.map((m) => (m.isOwnMessage ? { ...m, isRead: true, readAt: new Date().toISOString() } : m))
    );
  }, []);

  const handleTypingStatusChange = useCallback((senderEmpId: string, isTyping: boolean) => {
    setTypingMap((prev) => ({ ...prev, [senderEmpId]: isTyping }));
  }, []);

  const { isConnected, isConnecting, sendTypingStart, sendTypingStop } = useEmployeeChat({
    onNewMessage: handleIncomingMessage,
    onMessagesRead: handleMessagesReadEvent,
    onTypingStatusChange: handleTypingStatusChange,
  });

  // Load initial channels & contacts
  const loadSidebarData = useCallback(async () => {
    try {
      const [fetchedChannels, fetchedContacts] = await Promise.all([
        employeeChatApi.getAccessibleChannels(),
        employeeChatApi.getEligibleContacts(),
      ]);

      setChannels(fetchedChannels);
      setContacts(fetchedContacts);

      // Default select general channel if nothing selected
      if (!selectedChannelId && !selectedContactId && fetchedChannels.length > 0) {
        setSelectedChannelId(fetchedChannels[0].id);
        setMode('CHANNEL');
      }
    } catch (err) {
      console.error('Failed to load chat channels or contacts', err);
    } finally {
      setIsLoadingSidebar(false);
    }
  }, [selectedChannelId, selectedContactId]);

  useEffect(() => {
    loadSidebarData();
  }, [loadSidebarData]);

  // Load messages when selected channel or contact changes
  const loadMessages = useCallback(async (isSilent = false) => {
    if (!isSilent) setIsLoadingMessages(true);
    try {
      if (mode === 'CHANNEL' && selectedChannelId) {
        const msgs = await employeeChatApi.getChannelMessages(selectedChannelId);
        setMessages(msgs);
      } else if (mode === 'DIRECT' && selectedContactId) {
        const msgs = await employeeChatApi.getDirectMessages(selectedContactId);
        setMessages(msgs);
        // Mark as read
        await employeeChatApi.markDirectMessagesRead(selectedContactId);
        // Reset unread count locally
        setContacts((prev) =>
          prev.map((c) => (c.employeeId === selectedContactId ? { ...c, unreadCount: 0 } : c))
        );
      }
    } catch (err) {
      console.error('Failed to load messages', err);
    } finally {
      if (!isSilent) setIsLoadingMessages(false);
    }
  }, [mode, selectedChannelId, selectedContactId]);

  useEffect(() => {
    loadMessages(false);
  }, [loadMessages]);

  // Periodic 3-second background auto-sync heartbeat
  useEffect(() => {
    const interval = setInterval(() => {
      loadMessages(true);
    }, 3000);

    return () => clearInterval(interval);
  }, [loadMessages]);

  // Scroll to bottom on message update
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, typingMap]);

  // Handle Typing indicator triggers
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setMessageInput(e.target.value);

    if (mode === 'DIRECT' && selectedContactId) {
      sendTypingStart(selectedContactId);

      if (typingTimerRef.current) clearTimeout(typingTimerRef.current);
      typingTimerRef.current = setTimeout(() => {
        sendTypingStop(selectedContactId);
      }, 2500);
    }
  };

  // Send Message
  const handleSendMessage = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const trimmed = messageInput.trim();
    if (!trimmed && !attachmentUrl) return;

    let attachmentsJson: string | undefined = undefined;
    if (attachmentUrl) {
      attachmentsJson = JSON.stringify([
        {
          name: attachmentName || 'Attachment',
          url: attachmentUrl,
        },
      ]);
    }

    setIsSending(true);
    try {
      let sent: EmployeeChatMessage;
      if (mode === 'CHANNEL' && selectedChannelId) {
        sent = await employeeChatApi.sendChannelMessage(selectedChannelId, {
          messageBody: trimmed || `Shared an attachment: ${attachmentName}`,
          attachmentsJson,
          channelId: selectedChannelId,
        });
      } else if (mode === 'DIRECT' && selectedContactId) {
        sent = await employeeChatApi.sendDirectMessage(selectedContactId, {
          messageBody: trimmed || `Shared an attachment: ${attachmentName}`,
          attachmentsJson,
          recipientEmployeeId: selectedContactId,
        });
        sendTypingStop(selectedContactId);
      } else {
        return;
      }

      setMessages((prev) => {
        if (prev.some((m) => m.id === sent.id)) return prev;
        return [...prev, sent];
      });

      setMessageInput('');
      setAttachmentName('');
      setAttachmentUrl('');
      setIsAttachmentModalOpen(false);
    } catch (err) {
      console.error('Failed to send message', err);
    } finally {
      setIsSending(false);
    }
  };

  // Filtered Contacts & Channels
  const filteredChannels = channels.filter(
    (ch) =>
      ch.displayName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      ch.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const filteredContacts = contacts.filter(
    (c) =>
      c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (c.department && c.department.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (c.designation && c.designation.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const filteredMessages = messages.filter((m) =>
    messageSearchQuery ? m.messageBody.toLowerCase().includes(messageSearchQuery.toLowerCase()) : true
  );

  const isOtherTyping = selectedContactId ? Boolean(typingMap[selectedContactId]) : false;

  return (
    <div className="flex flex-col h-[calc(100vh-4rem)] bg-slate-950 text-slate-100 overflow-hidden">
      {/* Top Banner Header */}
      <div className="flex items-center justify-between px-6 py-3 border-b border-slate-800 bg-slate-900/90 backdrop-blur-md shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-emerald-500 to-teal-400 flex items-center justify-center shadow-lg shadow-emerald-500/20">
            <MessageSquare className="w-5 h-5 text-slate-950" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base font-bold text-white tracking-tight">Organization Team Chat</h1>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                ABAC Scoped
              </span>
            </div>
            <p className="text-xs text-slate-400">Internal collaboration following firm security & departmental visibility policies</p>
          </div>
        </div>

        {/* Real-time Status Badge */}
        <div className="flex items-center gap-3">
          <div
            className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium transition-all ${
              isConnected
                ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                : isConnecting
                ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                : 'bg-slate-800 text-slate-400 border border-slate-700'
            }`}
          >
            {isConnected ? (
              <>
                <span className="relative flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
                </span>
                <span>Live Socket</span>
              </>
            ) : (
              <>
                <RefreshCw className="w-3 h-3 animate-spin text-amber-400" />
                <span>Auto-Syncing</span>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Main Split Layout */}
      <div className="flex flex-1 min-h-0 overflow-hidden">
        {/* Left Sidebar: Channels & Colleagues */}
        <div className="w-80 border-r border-slate-800/80 bg-slate-900/60 flex flex-col shrink-0">
          {/* Search Box */}
          <div className="p-3 border-b border-slate-800/80">
            <div className="relative">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
              <input
                type="text"
                placeholder="Search channels or team..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-1.5 bg-slate-950 border border-slate-800 rounded-lg text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-emerald-500/50 transition-colors"
              />
            </div>
          </div>

          {/* Sidebar Nav Items */}
          <div className="flex-1 overflow-y-auto p-3 space-y-6 scrollbar-thin scrollbar-thumb-slate-800">
            {/* Channels List */}
            <div>
              <div className="flex items-center justify-between px-2 mb-2">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Hash className="w-3.5 h-3.5 text-slate-400" />
                  Team Channels
                </span>
                <span className="text-[10px] text-slate-500 bg-slate-800/60 px-1.5 py-0.5 rounded">
                  {channels.length}
                </span>
              </div>
              <div className="space-y-1">
                {filteredChannels.map((ch) => {
                  const isSelected = mode === 'CHANNEL' && selectedChannelId === ch.id;
                  return (
                    <button
                      key={ch.id}
                      onClick={() => {
                        setMode('CHANNEL');
                        setSelectedChannelId(ch.id);
                        setSelectedContactId(null);
                      }}
                      className={`w-full flex items-center justify-between px-2.5 py-2 rounded-lg text-left text-xs transition-all ${
                        isSelected
                          ? 'bg-emerald-500/15 text-emerald-300 font-semibold border border-emerald-500/30'
                          : 'text-slate-300 hover:bg-slate-800/60 hover:text-white'
                      }`}
                    >
                      <div className="flex items-center gap-2 truncate">
                        <Hash className={`w-3.5 h-3.5 shrink-0 ${isSelected ? 'text-emerald-400' : 'text-slate-500'}`} />
                        <span className="truncate">{ch.displayName}</span>
                      </div>
                      {ch.department && (
                        <span className="text-[9px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 shrink-0">
                          {ch.department}
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Direct Messages (Colleagues Scoped by Policy) */}
            <div>
              <div className="flex items-center justify-between px-2 mb-2">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Users className="w-3.5 h-3.5 text-slate-400" />
                  Direct Messages
                </span>
                <span className="text-[10px] text-slate-500 bg-slate-800/60 px-1.5 py-0.5 rounded">
                  {contacts.filter((c) => !c.isSelf).length}
                </span>
              </div>
              <div className="space-y-1">
                {filteredContacts
                  .filter((c) => !c.isSelf)
                  .map((contact) => {
                    const isSelected = mode === 'DIRECT' && selectedContactId === contact.employeeId;
                    const hasUnread = (contact.unreadCount || 0) > 0;

                    return (
                      <button
                        key={contact.employeeId}
                        onClick={() => {
                          setMode('DIRECT');
                          setSelectedContactId(contact.employeeId);
                          setSelectedChannelId(null);
                        }}
                        className={`w-full flex items-center justify-between px-2.5 py-2 rounded-lg text-left transition-all ${
                          isSelected
                            ? 'bg-emerald-500/15 text-emerald-300 font-semibold border border-emerald-500/30'
                            : 'text-slate-300 hover:bg-slate-800/60 hover:text-white'
                        }`}
                      >
                        <div className="flex items-center gap-2.5 min-w-0">
                          {/* Avatar / Dot */}
                          <div className="relative shrink-0">
                            {contact.avatarUrl ? (
                              <img
                                src={contact.avatarUrl}
                                alt={contact.name}
                                className="w-7 h-7 rounded-full object-cover border border-slate-700"
                              />
                            ) : (
                              <div className="w-7 h-7 rounded-full bg-gradient-to-tr from-slate-700 to-slate-600 flex items-center justify-center text-[11px] font-bold text-slate-200 border border-slate-600">
                                {contact.name.charAt(0).toUpperCase()}
                              </div>
                            )}
                            <span className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full bg-emerald-500 border-2 border-slate-900" />
                          </div>

                          <div className="min-w-0 flex-1">
                            <div className="flex items-center justify-between">
                              <span className="text-xs truncate font-medium">{contact.name}</span>
                            </div>
                            <div className="flex items-center gap-1.5 text-[10px] text-slate-400 truncate">
                              <span>{contact.designation || contact.department || 'Staff'}</span>
                            </div>
                          </div>
                        </div>

                        {/* Unread Pill */}
                        {hasUnread && (
                          <span className="ml-2 px-1.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500 text-slate-950 shrink-0 animate-pulse">
                            {contact.unreadCount}
                          </span>
                        )}
                      </button>
                    );
                  })}
              </div>
            </div>
          </div>

          {/* User Profile Quick Bar in Bottom Left */}
          <div className="p-3 border-t border-slate-800/80 bg-slate-950/80 flex items-center justify-between">
            <div className="flex items-center gap-2.5 min-w-0">
              <div className="w-8 h-8 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold text-xs border border-emerald-500/30 shrink-0">
                {user?.firstName?.charAt(0) || 'U'}
              </div>
              <div className="min-w-0">
                <div className="text-xs font-semibold text-white truncate">{user?.firstName} {user?.lastName}</div>
                <div className="text-[10px] text-slate-400 truncate">
                  {typeof user?.roles?.[0] === 'string'
                    ? user?.roles?.[0]
                    : (user?.roles?.[0] as any)?.name || 'Active Practitioner'}
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right Pane: Active Chat Room */}
        <div className="flex-1 flex flex-col min-w-0 bg-slate-950">
          {/* Room Header */}
          <div className="px-6 py-3.5 border-b border-slate-800/80 bg-slate-900/50 flex items-center justify-between shrink-0">
            <div className="flex items-center gap-3 min-w-0">
              {mode === 'CHANNEL' && activeChannel ? (
                <>
                  <div className="w-9 h-9 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center shrink-0">
                    <Hash className="w-5 h-5 text-emerald-400" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h2 className="text-sm font-bold text-white truncate">{activeChannel.displayName}</h2>
                      {activeChannel.department && (
                        <span className="px-2 py-0.5 rounded text-[10px] font-medium bg-slate-800 text-emerald-300 border border-slate-700">
                          {activeChannel.department}
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-slate-400 truncate">{activeChannel.description || 'Channel collaboration feed'}</p>
                  </div>
                </>
              ) : mode === 'DIRECT' && activeContact ? (
                <>
                  <div className="relative shrink-0">
                    {activeContact.avatarUrl ? (
                      <img
                        src={activeContact.avatarUrl}
                        alt={activeContact.name}
                        className="w-9 h-9 rounded-full object-cover border border-slate-700"
                      />
                    ) : (
                      <div className="w-9 h-9 rounded-full bg-slate-800 text-emerald-400 flex items-center justify-center font-bold text-sm border border-slate-700">
                        {activeContact.name.charAt(0).toUpperCase()}
                      </div>
                    )}
                    <span className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full bg-emerald-500 border-2 border-slate-900" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h2 className="text-sm font-bold text-white truncate">{activeContact.name}</h2>
                      <span className="px-2 py-0.5 rounded text-[10px] font-medium bg-emerald-500/10 text-emerald-300 border border-emerald-500/20">
                        {activeContact.role || 'Staff'}
                      </span>
                    </div>
                    <div className="flex items-center gap-2 text-xs text-slate-400">
                      <span>{activeContact.designation || 'Practitioner'}</span>
                      {activeContact.department && (
                        <>
                          <span>•</span>
                          <span>{activeContact.department}</span>
                        </>
                      )}
                      {activeContact.email && (
                        <>
                          <span>•</span>
                          <span className="text-slate-500">{activeContact.email}</span>
                        </>
                      )}
                    </div>
                  </div>
                </>
              ) : (
                <div className="text-xs text-slate-400">Select a channel or colleague to start chatting</div>
              )}
            </div>

            {/* In-Chat Message Search */}
            <div className="flex items-center gap-2">
              <div className="relative">
                <Search className="w-3.5 h-3.5 text-slate-500 absolute left-2.5 top-2" />
                <input
                  type="text"
                  placeholder="Filter messages..."
                  value={messageSearchQuery}
                  onChange={(e) => setMessageSearchQuery(e.target.value)}
                  className="pl-8 pr-3 py-1 bg-slate-900 border border-slate-800 rounded-md text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-emerald-500/50 w-44 transition-all"
                />
              </div>
            </div>
          </div>

          {/* Messages Stream */}
          <div
            ref={chatContainerRef}
            className="flex-1 overflow-y-auto p-6 space-y-4 scrollbar-thin scrollbar-thumb-slate-800"
          >
            {isLoadingMessages ? (
              <div className="h-full flex items-center justify-center">
                <div className="flex flex-col items-center gap-2">
                  <div className="w-6 h-6 rounded-full border-2 border-emerald-400 border-t-transparent animate-spin" />
                  <span className="text-xs text-slate-400">Loading message history...</span>
                </div>
              </div>
            ) : filteredMessages.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-center p-6 text-slate-500">
                <div className="w-12 h-12 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-center mb-3">
                  <MessageSquare className="w-6 h-6 text-slate-600" />
                </div>
                <p className="text-sm font-semibold text-slate-300">No messages yet</p>
                <p className="text-xs text-slate-500 mt-1 max-w-sm">
                  {mode === 'CHANNEL'
                    ? 'This channel is ready for collaboration. Send the first message to get started!'
                    : `Start a direct conversation with ${activeContact?.name || 'your colleague'}.`}
                </p>
              </div>
            ) : (
              filteredMessages.map((msg, idx) => {
                const isOwn = msg.isOwnMessage;
                let parsedAttachments: Array<{ name: string; url: string }> = [];
                if (msg.attachmentsJson) {
                  try {
                    parsedAttachments = JSON.parse(msg.attachmentsJson);
                  } catch {}
                }

                return (
                  <div
                    key={msg.id || idx}
                    className={`flex items-start gap-3 ${isOwn ? 'flex-row-reverse' : 'flex-row'}`}
                  >
                    {/* Avatar */}
                    {!isOwn && (
                      <div className="shrink-0 mt-0.5">
                        {msg.senderAvatarUrl ? (
                          <img
                            src={msg.senderAvatarUrl}
                            alt={msg.senderName}
                            className="w-8 h-8 rounded-full object-cover border border-slate-700"
                          />
                        ) : (
                          <div className="w-8 h-8 rounded-full bg-slate-800 text-emerald-400 flex items-center justify-center font-bold text-xs border border-slate-700">
                            {msg.senderName?.charAt(0).toUpperCase()}
                          </div>
                        )}
                      </div>
                    )}

                    <div className={`max-w-[70%] space-y-1 ${isOwn ? 'items-end' : 'items-start'}`}>
                      {/* Sender Info & Time */}
                      <div className={`flex items-center gap-2 text-[11px] ${isOwn ? 'justify-end text-slate-400' : 'text-slate-400'}`}>
                        {!isOwn && <span className="font-semibold text-slate-200">{msg.senderName}</span>}
                        {msg.senderDepartment && !isOwn && (
                          <span className="text-[10px] text-slate-500 font-normal">({msg.senderDepartment})</span>
                        )}
                        <span>
                          {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>

                      {/* Bubble */}
                      <div
                        className={`p-3.5 rounded-2xl text-xs leading-relaxed break-words shadow-md ${
                          isOwn
                            ? 'bg-gradient-to-br from-emerald-600 to-teal-700 text-white rounded-tr-none'
                            : 'bg-slate-900 border border-slate-800 text-slate-200 rounded-tl-none'
                        }`}
                      >
                        <p className="whitespace-pre-wrap">{msg.messageBody}</p>

                        {/* Attachments */}
                        {parsedAttachments.length > 0 && (
                          <div className="mt-2.5 pt-2 border-t border-white/10 space-y-1.5">
                            {parsedAttachments.map((att, attIdx) => (
                              <a
                                key={attIdx}
                                href={att.url}
                                target="_blank"
                                rel="noreferrer"
                                className={`flex items-center gap-2 p-2 rounded-lg text-[11px] font-medium transition-all ${
                                  isOwn
                                    ? 'bg-black/20 hover:bg-black/30 text-white'
                                    : 'bg-slate-950 hover:bg-slate-800 text-emerald-400 border border-slate-800'
                                }`}
                              >
                                <FileText className="w-3.5 h-3.5" />
                                <span className="truncate">{att.name}</span>
                              </a>
                            ))}
                          </div>
                        )}
                      </div>

                      {/* Read status ticks for own direct messages */}
                      {isOwn && mode === 'DIRECT' && (
                        <div className="flex items-center justify-end gap-1 text-[10px] text-slate-400">
                          {msg.isRead ? (
                            <span className="flex items-center gap-0.5 text-emerald-400">
                              <CheckCheck className="w-3.5 h-3.5" />
                              <span>Read</span>
                            </span>
                          ) : (
                            <span className="flex items-center gap-0.5 text-slate-500">
                              <Check className="w-3.5 h-3.5" />
                              <span>Delivered</span>
                            </span>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })
            )}

            {/* Typing Indicator */}
            {isOtherTyping && (
              <div className="flex items-center gap-2 text-xs text-slate-400 italic py-1 animate-pulse">
                <div className="flex gap-1">
                  <span className="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-bounce"></span>
                  <span className="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-bounce [animation-delay:0.2s]"></span>
                  <span className="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-bounce [animation-delay:0.4s]"></span>
                </div>
                <span>{activeContact?.name} is typing...</span>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Message Input Box */}
          <div className="p-4 border-t border-slate-800 bg-slate-900/60">
            {attachmentUrl && (
              <div className="mb-2 p-2 rounded-lg bg-slate-800 border border-emerald-500/30 flex items-center justify-between text-xs text-emerald-300">
                <div className="flex items-center gap-2 truncate">
                  <FileText className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span className="truncate font-medium">{attachmentName || 'Attached Document'}</span>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setAttachmentName('');
                    setAttachmentUrl('');
                  }}
                  className="text-slate-400 hover:text-white"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            )}

            <form onSubmit={handleSendMessage} className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setIsAttachmentModalOpen(true)}
                title="Attach Document or Link"
                className="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition-all"
              >
                <Paperclip className="w-4 h-4" />
              </button>

              <input
                type="text"
                placeholder={
                  mode === 'CHANNEL'
                    ? `Message #${activeChannel?.name || 'channel'}...`
                    : `Direct message to ${activeContact?.name || 'colleague'}...`
                }
                value={messageInput}
                onChange={handleInputChange}
                className="flex-1 px-4 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500/50 transition-colors"
              />

              <button
                type="submit"
                disabled={isSending || (!messageInput.trim() && !attachmentUrl)}
                className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 disabled:opacity-40 disabled:pointer-events-none text-slate-950 font-bold text-xs flex items-center gap-1.5 shadow-lg shadow-emerald-500/20 transition-all"
              >
                {isSending ? (
                  <div className="w-4 h-4 rounded-full border-2 border-slate-950 border-t-transparent animate-spin" />
                ) : (
                  <>
                    <span>Send</span>
                    <Send className="w-3.5 h-3.5" />
                  </>
                )}
              </button>
            </form>
          </div>
        </div>
      </div>

      {/* Attachment Modal */}
      {isAttachmentModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <Paperclip className="w-4 h-4 text-emerald-400" />
                Add File Attachment / URL
              </h3>
              <button
                type="button"
                onClick={() => setIsAttachmentModalOpen(false)}
                className="text-slate-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Document Title</label>
                <input
                  type="text"
                  placeholder="e.g. Audit_Checklist_Q2.pdf"
                  value={attachmentName}
                  onChange={(e) => setAttachmentName(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-xs text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500/50"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">File URL / Secure Link</label>
                <input
                  type="url"
                  placeholder="https://docs.taxoryn.com/secure/..."
                  value={attachmentUrl}
                  onChange={(e) => setAttachmentUrl(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-xs text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500/50"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setIsAttachmentModalOpen(false)}
                className="px-3 py-1.5 rounded-lg bg-slate-800 text-xs text-slate-300 hover:bg-slate-700"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => setIsAttachmentModalOpen(false)}
                disabled={!attachmentUrl}
                className="px-4 py-1.5 rounded-lg bg-emerald-500 hover:bg-emerald-400 disabled:opacity-40 text-slate-950 font-bold text-xs"
              >
                Attach
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
