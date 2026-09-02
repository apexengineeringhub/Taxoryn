import React, { useState, useEffect } from 'react';
import {
  MessageSquare,
  Search,
  Filter,
  CheckCircle2,
  AlertTriangle,
  Clock,
  UserCheck,
  Send,
  RefreshCw,
  Tag,
  Star,
  Users,
  Building2,
  User,
  Check,
  X,
  AlertCircle,
  Copy,
  Flame,
  History,
  Lock,
  Globe,
  MessageCircle,
} from 'lucide-react';
import { adminFeedbackApi } from '../api/endpoints';
import {
  AdminApplicationFeedbackSummary,
  AdminApplicationFeedbackDetail,
  AdminFeedbackStats,
  AdminAssignee,
  ApplicationFeedbackStatus,
  ApplicationFeedbackPriority,
  ApplicationFeedbackActorType,
  FeedbackTeam,
  FeedbackNoteVisibility,
} from '../types';

export const AdminFeedbackPage: React.FC = () => {
  // State
  const [feedbackList, setFeedbackList] = useState<AdminApplicationFeedbackSummary[]>([]);
  const [selectedFeedbackId, setSelectedFeedbackId] = useState<string | null>(null);
  const [selectedDetail, setSelectedDetail] = useState<AdminApplicationFeedbackDetail | null>(null);
  const [stats, setStats] = useState<AdminFeedbackStats | null>(null);
  const [assignees, setAssignees] = useState<AdminAssignee[]>([]);
  const [teams, setTeams] = useState<FeedbackTeam[]>([]);
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedActorType, setSelectedActorType] = useState<string>('ALL');
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [selectedPriority, setSelectedPriority] = useState<string>('ALL');
  const [selectedTeam, setSelectedTeam] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Active Modals
  const [activeModal, setActiveModal] = useState<
    'NONE' | 'ASSIGN' | 'PRIORITY' | 'RESOLVE' | 'CLOSE' | 'REJECT' | 'DUPLICATE' | 'ESCALATE'
  >('NONE');

  // Form states for modals
  const [assignTeam, setAssignTeam] = useState<FeedbackTeam>('OPERATIONS');
  const [assignUserId, setAssignUserId] = useState<string>('');
  const [assignReason, setAssignReason] = useState('');

  const [newPriority, setNewPriority] = useState<ApplicationFeedbackPriority>('HIGH');
  const [priorityReason, setPriorityReason] = useState('');

  const [resolutionNote, setResolutionNote] = useState('');
  const [closeReason, setCloseReason] = useState('');
  const [rejectReason, setRejectReason] = useState('');

  const [duplicateTargetId, setDuplicateTargetId] = useState('');
  const [duplicateReason, setDuplicateReason] = useState('');

  const [escalateTitle, setEscalateTitle] = useState('');
  const [escalateDesc, setEscalateDesc] = useState('');
  const [escalatePriority, setEscalatePriority] = useState<ApplicationFeedbackPriority>('HIGH');
  const [escalateNotes, setEscalateNotes] = useState('');

  // Internal Note form state
  const [newNoteContent, setNewNoteContent] = useState('');
  const [newNoteVisibility, setNewNoteVisibility] = useState<FeedbackNoteVisibility>('INTERNAL');

  // Load initial meta and stats
  useEffect(() => {
    loadStats();
    loadAssigneesAndTeams();
  }, []);

  // Fetch feedback list on filter change
  useEffect(() => {
    fetchFeedbackList();
  }, [searchQuery, selectedActorType, selectedStatus, selectedPriority, selectedTeam, page]);

  // Fetch detail when selectedFeedbackId changes
  useEffect(() => {
    if (selectedFeedbackId) {
      fetchFeedbackDetail(selectedFeedbackId);
    } else {
      setSelectedDetail(null);
    }
  }, [selectedFeedbackId]);

  const showSuccess = (msg: string) => {
    setSuccessMessage(msg);
    setTimeout(() => setSuccessMessage(null), 4000);
  };

  const showError = (msg: string) => {
    setErrorMessage(msg);
    setTimeout(() => setErrorMessage(null), 5000);
  };

  const loadStats = async () => {
    try {
      const res = await adminFeedbackApi.getStats();
      setStats(res);
    } catch (err: any) {
      console.error('Failed to load feedback stats', err);
    }
  };

  const loadAssigneesAndTeams = async () => {
    try {
      const [assigneesRes, teamsRes] = await Promise.all([
        adminFeedbackApi.getAssignees(),
        adminFeedbackApi.getTeams(),
      ]);
      setAssignees(assigneesRes || []);
      setTeams(teamsRes || []);
    } catch (err: any) {
      console.error('Failed to load assignees/teams', err);
    }
  };

  const fetchFeedbackList = async () => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const res = await adminFeedbackApi.getFeedbackList({
        search: searchQuery.trim() || undefined,
        actorType: selectedActorType !== 'ALL' ? selectedActorType : undefined,
        status: selectedStatus !== 'ALL' ? selectedStatus : undefined,
        priority: selectedPriority !== 'ALL' ? selectedPriority : undefined,
        assignedTeam: selectedTeam !== 'ALL' ? selectedTeam : undefined,
        page,
        size: 15,
        sortBy: 'createdAt',
        sortDirection: 'DESC',
      });
      setFeedbackList(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: any) {
      showError(err.response?.data?.message || 'Failed to fetch application feedback list');
    } finally {
      setLoading(false);
    }
  };

  const fetchFeedbackDetail = async (id: string) => {
    setDetailLoading(true);
    try {
      const detail = await adminFeedbackApi.getFeedbackDetail(id);
      setSelectedDetail(detail);
      // Pre-fill escalation fields
      setEscalateTitle(`[FB Issue] ${detail.title}`);
      setEscalateDesc(`Page: ${detail.page || 'N/A'}\nFeature: ${detail.feature || 'N/A'}\n\nDescription:\n${detail.description}`);
      setEscalatePriority(detail.priority || 'HIGH');
    } catch (err: any) {
      showError(err.response?.data?.message || 'Failed to fetch feedback details');
    } finally {
      setDetailLoading(false);
    }
  };

  // Workflow Handlers
  const handleStartReview = async () => {
    if (!selectedDetail) return;
    setActionLoading(true);
    try {
      const updated = await adminFeedbackApi.startReview(selectedDetail.id);
      setSelectedDetail(updated);
      showSuccess(`Feedback ${updated.feedbackCode} status changed to UNDER REVIEW`);
      fetchFeedbackList();
      loadStats();
    } catch (err: any) {
      showError(err.response?.data?.message || 'Failed to start review');
    } finally {
      setActionLoading(false);
    }
  };

  const handleAssign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDetail) return;
    setActionLoading(true);
    try {
      const updated = await adminFeedbackApi.assignFeedback(selectedDetail.id, {
        team: assignTeam,
        assignedUserId: assignUserId || undefined,
        reason: assignReason.trim() || undefined,
      });
      setSelectedDetail(updated);
      setActiveModal('NONE');
      setAssignReason('');
      showSuccess(`Feedback assigned to ${assignTeam} team`);
      fetchFeedbackList();
      loadStats();
    } catch (err: any) {
      showError(err.response?.data?.message || 'Failed to assign feedback');
    } finally {
      setActionLoading(false);
    }
  };

  const handleUpdatePriority = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDetail) return;
    setActionLoading(true);
    try {
      const updated = await adminFeedbackApi.updatePriority(selectedDetail.id, {
        priority: newPriority,
        reason: priorityReason.trim() || undefined,
      });
      setSelectedDetail(updated);
      setActiveModal('NONE');
      setPriorityReason('');
      showSuccess(`Priority updated to ${newPriority}`);
      fetchFeedbackList();
      loadStats();
    } catch (err: any) {
      showError(err.response?.data?.message || 'Failed to update priority');
    } finally {
      setActionLoading(false);
    }
  };

  const handleResolve = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDetail) return;
    if (!resolutionNote.trim()) {
      showError('Please provide a resolution note');
      return;
    }
    setActionLoading(true);
    try {
      const updated = await adminFeedbackApi.resolveFeedback(selectedDetail.id, {
        resolutionNote: resolutionNote.trim(),
      });
      setSelectedDetail(updated);
      setActiveModal('NONE');
      setResolutionNote('');
      showSuccess(`Feedback ${updated.feedbackCode} has been marked RESOLVED`);
      fetchFeedbackList();
      loadStats();
    } catch (err: any) {
      showError(err.response?.data?.message || 'Failed to resolve feedback');
    } finally {
      setActionLoading(false);
    }
  };

  const handleClose = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDetail) return;
    setActionLoading(true);
    try {
      const updated = await adminFeedbackApi.closeFeedback(selectedDetail.id, {
        reason: closeReason.trim() || undefined,
      });
      setSelectedDetail(updated);
      setActiveModal('NONE');
      setCloseReason('');
      showSuccess(`Feedback ${updated.feedbackCode} has been CLOSED`);
      fetchFeedbackList();
      loadStats();
    } catch (err: any) {
      showError(err.response?.data?.message || 'Failed to close feedback');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDetail) return;
    if (!rejectReason.trim()) {
      showError('Please provide a rejection reason');
      return;
    }
    setActionLoading(true);
    try {
      const updated = await adminFeedbackApi.rejectFeedback(selectedDetail.id, {
        reason: rejectReason.trim(),
      });
      setSelectedDetail(updated);
      setActiveModal('NONE');
      setRejectReason('');
      showSuccess(`Feedback ${updated.feedbackCode} has been REJECTED`);
      fetchFeedbackList();
      loadStats();
    } catch (err: any) {
      showError(err.response?.data?.message || 'Failed to reject feedback');
    } finally {
      setActionLoading(false);
    }
  };

  const handleMarkDuplicate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDetail) return;
    if (!duplicateTargetId.trim()) {
      showError('Please enter the canonical feedback UUID or select one');
      return;
    }
    setActionLoading(true);
    try {
      const updated = await adminFeedbackApi.markDuplicate(selectedDetail.id, {
        duplicateOfId: duplicateTargetId.trim(),
        reason: duplicateReason.trim() || undefined,
      });
      setSelectedDetail(updated);
      setActiveModal('NONE');
      setDuplicateTargetId('');
      setDuplicateReason('');
      showSuccess(`Feedback ${updated.feedbackCode} marked as duplicate`);
      fetchFeedbackList();
      loadStats();
    } catch (err: any) {
      showError(err.response?.data?.message || 'Failed to mark feedback duplicate');
    } finally {
      setActionLoading(false);
    }
  };

  const handleEscalateToEngineering = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDetail) return;
    if (!escalateTitle.trim() || !escalateDesc.trim()) {
      showError('Title and description are required for engineering escalation');
      return;
    }
    setActionLoading(true);
    try {
      const issue = await adminFeedbackApi.escalateToEngineering(selectedDetail.id, {
        title: escalateTitle.trim(),
        description: escalateDesc.trim(),
        priority: escalatePriority,
        internalNotes: escalateNotes.trim() || undefined,
      });
      showSuccess(`Escalated to Engineering! Issue Code: ${issue.issueCode}`);
      setActiveModal('NONE');
      setEscalateNotes('');
      fetchFeedbackDetail(selectedDetail.id);
      fetchFeedbackList();
      loadStats();
    } catch (err: any) {
      showError(err.response?.data?.message || 'Failed to escalate to engineering');
    } finally {
      setActionLoading(false);
    }
  };

  const handleAddNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDetail || !newNoteContent.trim()) return;
    setActionLoading(true);
    try {
      const createdNote = await adminFeedbackApi.addNote(selectedDetail.id, {
        note: newNoteContent.trim(),
        visibility: newNoteVisibility,
      });
      setSelectedDetail({
        ...selectedDetail,
        notes: [createdNote, ...(selectedDetail.notes || [])],
      });
      setNewNoteContent('');
      showSuccess('Internal note added');
    } catch (err: any) {
      showError(err.response?.data?.message || 'Failed to add note');
    } finally {
      setActionLoading(false);
    }
  };

  // Helper Badge Renderers
  const renderStatusBadge = (status?: ApplicationFeedbackStatus) => {
    switch (status) {
      case 'NEW':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-blue-100 text-blue-800 border border-blue-200">
            New
          </span>
        );
      case 'UNDER_REVIEW':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-200">
            <Clock className="w-3 h-3 mr-1" /> Under Review
          </span>
        );
      case 'ASSIGNED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-purple-100 text-purple-800 border border-purple-200">
            <UserCheck className="w-3 h-3 mr-1" /> Assigned
          </span>
        );
      case 'IN_PROGRESS':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-indigo-100 text-indigo-800 border border-indigo-200">
            <RefreshCw className="w-3 h-3 mr-1" /> In Progress
          </span>
        );
      case 'ESCALATED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-red-100 text-red-800 border border-red-200">
            <Flame className="w-3 h-3 mr-1" /> Escalated
          </span>
        );
      case 'RESOLVED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">
            <CheckCircle2 className="w-3 h-3 mr-1" /> Resolved
          </span>
        );
      case 'CLOSED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-gray-100 text-gray-800 border border-gray-200">
            <Check className="w-3 h-3 mr-1" /> Closed
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-rose-100 text-rose-800 border border-rose-200">
            <X className="w-3 h-3 mr-1" /> Rejected
          </span>
        );
      case 'DUPLICATE':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-300">
            <Copy className="w-3 h-3 mr-1" /> Duplicate
          </span>
        );
      default:
        return <span className="text-xs text-gray-500">{status || 'N/A'}</span>;
    }
  };

  const renderPriorityBadge = (priority?: ApplicationFeedbackPriority) => {
    switch (priority) {
      case 'CRITICAL':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-red-600 text-white shadow-sm">
            <AlertTriangle className="w-3 h-3 mr-1" /> Critical
          </span>
        );
      case 'HIGH':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-orange-100 text-orange-800 border border-orange-200">
            High
          </span>
        );
      case 'MEDIUM':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-yellow-100 text-yellow-800 border border-yellow-200">
            Medium
          </span>
        );
      case 'LOW':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-gray-100 text-gray-600 border border-gray-200">
            Low
          </span>
        );
      default:
        return <span className="text-xs text-gray-500">Normal</span>;
    }
  };

  const renderActorBadge = (actorType: ApplicationFeedbackActorType) => {
    switch (actorType) {
      case 'CUSTOMER':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
            <User className="w-3 h-3 mr-1" /> Customer
          </span>
        );
      case 'PRACTITIONER':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-50 text-blue-700 border border-blue-200">
            <Building2 className="w-3 h-3 mr-1" /> Practitioner
          </span>
        );
      case 'PRACTICE_EMPLOYEE':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-violet-50 text-violet-700 border border-violet-200">
            <Users className="w-3 h-3 mr-1" /> Employee
          </span>
        );
      default:
        return <span className="text-xs text-gray-500">{actorType}</span>;
    }
  };

  const renderRatingStars = (rating?: number) => {
    if (!rating) return null;
    return (
      <div className="flex items-center text-amber-500 text-xs">
        {Array.from({ length: 5 }).map((_, i) => (
          <Star
            key={i}
            className={`w-3.5 h-3.5 ${i < rating ? 'fill-current text-amber-400' : 'text-gray-300'}`}
          />
        ))}
        <span className="ml-1 text-gray-700 font-medium">{rating}/5</span>
      </div>
    );
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Toast Notifications */}
      {successMessage && (
        <div className="fixed top-5 right-5 z-50 flex items-center bg-emerald-600 text-white px-4 py-3 rounded-lg shadow-xl animate-fade-in">
          <CheckCircle2 className="w-5 h-5 mr-2" />
          <span className="text-sm font-medium">{successMessage}</span>
        </div>
      )}
      {errorMessage && (
        <div className="fixed top-5 right-5 z-50 flex items-center bg-red-600 text-white px-4 py-3 rounded-lg shadow-xl animate-fade-in">
          <AlertCircle className="w-5 h-5 mr-2" />
          <span className="text-sm font-medium">{errorMessage}</span>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-gray-200 pb-5">
        <div>
          <div className="flex items-center gap-2">
            <div className="p-2 bg-indigo-600 text-white rounded-lg shadow-sm">
              <MessageSquare className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Admin Feedback Operations</h1>
              <p className="text-sm text-gray-500">
                Triage, review, assign, escalate, and resolve application feedback across Customer, Practitioner, and Employee portals.
              </p>
            </div>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              fetchFeedbackList();
              loadStats();
            }}
            disabled={loading}
            className="inline-flex items-center px-3.5 py-2 border border-gray-300 text-sm font-medium rounded-lg text-gray-700 bg-white hover:bg-gray-50 shadow-sm focus:outline-none"
          >
            <RefreshCw className={`w-4 h-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* KPI Stats Cards */}
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-3">
          <div className="bg-white border border-gray-200 rounded-xl p-3.5 shadow-sm text-center">
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Total</p>
            <p className="text-2xl font-bold text-gray-900 mt-1">{stats.totalCount}</p>
          </div>
          <div className="bg-blue-50 border border-blue-200 rounded-xl p-3.5 shadow-sm text-center">
            <p className="text-xs font-semibold text-blue-700 uppercase tracking-wider">New</p>
            <p className="text-2xl font-bold text-blue-900 mt-1">{stats.newCount}</p>
          </div>
          <div className="bg-amber-50 border border-amber-200 rounded-xl p-3.5 shadow-sm text-center">
            <p className="text-xs font-semibold text-amber-700 uppercase tracking-wider">Reviewing</p>
            <p className="text-2xl font-bold text-amber-900 mt-1">{stats.underReviewCount}</p>
          </div>
          <div className="bg-purple-50 border border-purple-200 rounded-xl p-3.5 shadow-sm text-center">
            <p className="text-xs font-semibold text-purple-700 uppercase tracking-wider">Assigned</p>
            <p className="text-2xl font-bold text-purple-900 mt-1">{stats.assignedCount}</p>
          </div>
          <div className="bg-indigo-50 border border-indigo-200 rounded-xl p-3.5 shadow-sm text-center">
            <p className="text-xs font-semibold text-indigo-700 uppercase tracking-wider">In Progress</p>
            <p className="text-2xl font-bold text-indigo-900 mt-1">{stats.inProgressCount}</p>
          </div>
          <div className="bg-rose-50 border border-rose-200 rounded-xl p-3.5 shadow-sm text-center">
            <p className="text-xs font-semibold text-rose-700 uppercase tracking-wider">Escalated</p>
            <p className="text-2xl font-bold text-rose-900 mt-1">{stats.escalatedCount}</p>
          </div>
          <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-3.5 shadow-sm text-center">
            <p className="text-xs font-semibold text-emerald-700 uppercase tracking-wider">Resolved</p>
            <p className="text-2xl font-bold text-emerald-900 mt-1">{stats.resolvedCount}</p>
          </div>
          <div className="bg-red-50 border border-red-200 rounded-xl p-3.5 shadow-sm text-center">
            <p className="text-xs font-semibold text-red-700 uppercase tracking-wider">Critical</p>
            <p className="text-2xl font-bold text-red-900 mt-1">{stats.criticalCount}</p>
          </div>
        </div>
      )}

      {/* Filter Toolbar */}
      <div className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
          {/* Search Box */}
          <div className="md:col-span-2 relative">
            <Search className="w-4 h-4 text-gray-400 absolute left-3 top-3" />
            <input
              type="text"
              placeholder="Search feedback code, title, reporter, or practice..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(0);
              }}
              className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>

          {/* Actor Filter */}
          <div>
            <select
              value={selectedActorType}
              onChange={(e) => {
                setSelectedActorType(e.target.value);
                setPage(0);
              }}
              className="w-full py-2 px-3 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 bg-white"
            >
              <option value="ALL">All Actors</option>
              <option value="CUSTOMER">Customer / Taxpayer</option>
              <option value="PRACTITIONER">Practitioner / Owner</option>
              <option value="PRACTICE_EMPLOYEE">Practice Employee</option>
            </select>
          </div>

          {/* Status Filter */}
          <div>
            <select
              value={selectedStatus}
              onChange={(e) => {
                setSelectedStatus(e.target.value);
                setPage(0);
              }}
              className="w-full py-2 px-3 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 bg-white"
            >
              <option value="ALL">All Statuses</option>
              <option value="NEW">New</option>
              <option value="UNDER_REVIEW">Under Review</option>
              <option value="ASSIGNED">Assigned</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="ESCALATED">Escalated</option>
              <option value="RESOLVED">Resolved</option>
              <option value="CLOSED">Closed</option>
              <option value="REJECTED">Rejected</option>
              <option value="DUPLICATE">Duplicate</option>
            </select>
          </div>

          {/* Priority Filter */}
          <div>
            <select
              value={selectedPriority}
              onChange={(e) => {
                setSelectedPriority(e.target.value);
                setPage(0);
              }}
              className="w-full py-2 px-3 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 bg-white"
            >
              <option value="ALL">All Priorities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>
        </div>

        {/* Secondary Filters: Teams */}
        <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-gray-100 text-xs text-gray-500">
          <span className="font-medium text-gray-700 flex items-center gap-1">
            <Filter className="w-3.5 h-3.5" /> Team Routing:
          </span>
          <button
            onClick={() => setSelectedTeam('ALL')}
            className={`px-2.5 py-1 rounded-full text-xs font-medium transition ${
              selectedTeam === 'ALL'
                ? 'bg-indigo-600 text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            All Teams
          </button>
          {teams.map((t) => (
            <button
              key={t}
              onClick={() => setSelectedTeam(t)}
              className={`px-2.5 py-1 rounded-full text-xs font-medium transition ${
                selectedTeam === t
                  ? 'bg-indigo-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              {t.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Main Content Layout: Table & Detail Drawer */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Feedback List Table */}
        <div className={`${selectedFeedbackId ? 'lg:col-span-7' : 'lg:col-span-12'} transition-all`}>
          <div className="bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden">
            <div className="p-4 border-b border-gray-200 flex items-center justify-between">
              <h2 className="text-sm font-semibold text-gray-900">
                Feedback Items ({totalElements})
              </h2>
              <span className="text-xs text-gray-500">Page {page + 1} of {totalPages}</span>
            </div>

            {loading ? (
              <div className="p-12 text-center text-gray-500">
                <RefreshCw className="w-8 h-8 animate-spin mx-auto text-indigo-600 mb-2" />
                <p className="text-sm">Loading application feedback...</p>
              </div>
            ) : feedbackList.length === 0 ? (
              <div className="p-12 text-center text-gray-500">
                <MessageSquare className="w-12 h-12 mx-auto text-gray-300 mb-3" />
                <p className="text-base font-medium text-gray-700">No feedback found</p>
                <p className="text-xs text-gray-400 mt-1">Try adjusting your filters or search criteria.</p>
              </div>
            ) : (
              <div className="divide-y divide-gray-200">
                {feedbackList.map((item) => {
                  const isSelected = selectedFeedbackId === item.id;
                  return (
                    <div
                      key={item.id}
                      onClick={() => setSelectedFeedbackId(item.id)}
                      className={`p-4 hover:bg-indigo-50/50 cursor-pointer transition flex flex-col gap-2 ${
                        isSelected ? 'bg-indigo-50/80 border-l-4 border-indigo-600' : ''
                      }`}
                    >
                      {/* Top Row: Code, Actor, Priority, Status */}
                      <div className="flex items-center justify-between flex-wrap gap-2">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-bold text-gray-700 bg-gray-100 px-2 py-0.5 rounded">
                            {item.feedbackCode}
                          </span>
                          {renderActorBadge(item.actorType)}
                          <span className="text-xs text-gray-400">•</span>
                          <span className="text-xs text-gray-500 font-medium">
                            {item.category?.replace('_', ' ')}
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          {renderPriorityBadge(item.priority)}
                          {renderStatusBadge(item.status)}
                        </div>
                      </div>

                      {/* Title & Rating */}
                      <div className="flex items-start justify-between gap-2">
                        <h3 className="text-sm font-semibold text-gray-900 line-clamp-1">
                          {item.title}
                        </h3>
                        {renderRatingStars(item.rating)}
                      </div>

                      {/* Excerpt */}
                      <p className="text-xs text-gray-600 line-clamp-2">
                        {item.descriptionExcerpt}
                      </p>

                      {/* Meta Footer */}
                      <div className="flex items-center justify-between text-xs text-gray-500 pt-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span>By <strong className="text-gray-700">{item.reporterName || 'Anonymous'}</strong></span>
                          {item.practiceName && (
                            <>
                              <span>•</span>
                              <span className="flex items-center text-gray-600 font-medium">
                                <Building2 className="w-3 h-3 mr-1 text-gray-400" />
                                {item.practiceName}
                              </span>
                            </>
                          )}
                          {item.assignedTeam && (
                            <>
                              <span>•</span>
                              <span className="inline-flex items-center px-1.5 py-0.5 rounded bg-purple-50 text-purple-700 font-medium">
                                <Users className="w-3 h-3 mr-1" />
                                {item.assignedTeam}
                              </span>
                            </>
                          )}
                          {item.hasEngineeringIssue && (
                            <span className="inline-flex items-center px-1.5 py-0.5 rounded bg-red-100 text-red-800 font-bold font-mono">
                              <Flame className="w-3 h-3 mr-1" />
                              {item.engineeringIssueCode || 'ENG-ISSUE'}
                            </span>
                          )}
                        </div>
                        <span className="text-gray-400 whitespace-nowrap">
                          {new Date(item.createdAt).toLocaleDateString()}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="p-3 border-t border-gray-200 flex items-center justify-between bg-gray-50 text-xs">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1.5 border border-gray-300 rounded bg-white font-medium disabled:opacity-40"
                >
                  Previous
                </button>
                <span className="text-gray-600">
                  Page {page + 1} of {totalPages}
                </span>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="px-3 py-1.5 border border-gray-300 rounded bg-white font-medium disabled:opacity-40"
                >
                  Next
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Selected Feedback Detail Panel */}
        {selectedFeedbackId && (
          <div className="lg:col-span-5">
            <div className="bg-white border border-gray-200 rounded-xl shadow-lg sticky top-6 overflow-hidden flex flex-col max-h-[calc(100vh-6rem)]">
              {/* Detail Header */}
              <div className="p-4 border-b border-gray-200 bg-gray-50 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-sm font-bold text-gray-900 bg-white px-2 py-0.5 rounded border border-gray-200">
                    {selectedDetail?.feedbackCode || 'Feedback Details'}
                  </span>
                  {selectedDetail && renderStatusBadge(selectedDetail.status)}
                </div>
                <button
                  onClick={() => setSelectedFeedbackId(null)}
                  className="p-1 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-200"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {detailLoading || !selectedDetail ? (
                <div className="p-12 text-center text-gray-500">
                  <RefreshCw className="w-8 h-8 animate-spin mx-auto text-indigo-600 mb-2" />
                  <p className="text-sm">Loading details...</p>
                </div>
              ) : (
                <div className="p-4 overflow-y-auto space-y-5 text-sm">
                  {/* Action Bar / Workflow Buttons */}
                  <div className="bg-indigo-50/50 border border-indigo-100 rounded-xl p-3 space-y-2">
                    <p className="text-xs font-bold uppercase text-indigo-900 tracking-wider">
                      Workflow Actions
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {selectedDetail.status === 'NEW' && (
                        <button
                          onClick={handleStartReview}
                          disabled={actionLoading}
                          className="px-3 py-1.5 bg-amber-600 text-white rounded-lg text-xs font-semibold hover:bg-amber-700 shadow-sm flex items-center"
                        >
                          <Clock className="w-3.5 h-3.5 mr-1" /> Start Review
                        </button>
                      )}

                      <button
                        onClick={() => {
                          setAssignTeam(selectedDetail.assignedTeam || 'OPERATIONS');
                          setAssignUserId(selectedDetail.assignedUserId || '');
                          setActiveModal('ASSIGN');
                        }}
                        disabled={actionLoading}
                        className="px-3 py-1.5 bg-purple-600 text-white rounded-lg text-xs font-semibold hover:bg-purple-700 shadow-sm flex items-center"
                      >
                        <UserCheck className="w-3.5 h-3.5 mr-1" /> Assign Team / Member
                      </button>

                      <button
                        onClick={() => {
                          setNewPriority(selectedDetail.priority || 'HIGH');
                          setActiveModal('PRIORITY');
                        }}
                        disabled={actionLoading}
                        className="px-3 py-1.5 bg-white border border-gray-300 text-gray-700 rounded-lg text-xs font-semibold hover:bg-gray-50 shadow-sm flex items-center"
                      >
                        <Tag className="w-3.5 h-3.5 mr-1" /> Change Priority
                      </button>

                      {!selectedDetail.engineeringIssue && selectedDetail.status !== 'CLOSED' && selectedDetail.status !== 'REJECTED' && (
                        <button
                          onClick={() => setActiveModal('ESCALATE')}
                          disabled={actionLoading}
                          className="px-3 py-1.5 bg-red-600 text-white rounded-lg text-xs font-semibold hover:bg-red-700 shadow-sm flex items-center"
                        >
                          <Flame className="w-3.5 h-3.5 mr-1" /> Escalate to Eng
                        </button>
                      )}

                      {selectedDetail.status !== 'RESOLVED' && selectedDetail.status !== 'CLOSED' && (
                        <button
                          onClick={() => setActiveModal('RESOLVE')}
                          disabled={actionLoading}
                          className="px-3 py-1.5 bg-emerald-600 text-white rounded-lg text-xs font-semibold hover:bg-emerald-700 shadow-sm flex items-center"
                        >
                          <CheckCircle2 className="w-3.5 h-3.5 mr-1" /> Resolve
                        </button>
                      )}

                      {selectedDetail.status !== 'CLOSED' && (
                        <button
                          onClick={() => setActiveModal('CLOSE')}
                          disabled={actionLoading}
                          className="px-3 py-1.5 bg-gray-800 text-white rounded-lg text-xs font-semibold hover:bg-gray-900 shadow-sm flex items-center"
                        >
                          <Check className="w-3.5 h-3.5 mr-1" /> Close
                        </button>
                      )}

                      {selectedDetail.status !== 'REJECTED' && selectedDetail.status !== 'RESOLVED' && selectedDetail.status !== 'CLOSED' && (
                        <button
                          onClick={() => setActiveModal('REJECT')}
                          disabled={actionLoading}
                          className="px-3 py-1.5 bg-rose-50 border border-rose-200 text-rose-700 rounded-lg text-xs font-semibold hover:bg-rose-100 shadow-sm flex items-center"
                        >
                          <X className="w-3.5 h-3.5 mr-1" /> Reject
                        </button>
                      )}

                      {selectedDetail.status !== 'DUPLICATE' && selectedDetail.status !== 'CLOSED' && (
                        <button
                          onClick={() => setActiveModal('DUPLICATE')}
                          disabled={actionLoading}
                          className="px-3 py-1.5 bg-slate-100 border border-slate-300 text-slate-700 rounded-lg text-xs font-semibold hover:bg-slate-200 shadow-sm flex items-center"
                        >
                          <Copy className="w-3.5 h-3.5 mr-1" /> Mark Duplicate
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Feedback Summary Card */}
                  <div className="bg-white border border-gray-200 rounded-xl p-4 space-y-3">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <h2 className="text-base font-bold text-gray-900">
                          {selectedDetail.title}
                        </h2>
                        <div className="flex items-center gap-2 mt-1">
                          {renderActorBadge(selectedDetail.actorType)}
                          {renderPriorityBadge(selectedDetail.priority)}
                          <span className="text-xs bg-gray-100 text-gray-700 px-2 py-0.5 rounded font-medium">
                            {selectedDetail.category?.replace('_', ' ')}
                          </span>
                        </div>
                      </div>
                      {renderRatingStars(selectedDetail.rating)}
                    </div>

                    <div className="text-xs text-gray-700 bg-gray-50 p-3 rounded-lg border border-gray-100 whitespace-pre-wrap leading-relaxed">
                      {selectedDetail.description}
                    </div>

                    {/* Context Details */}
                    <div className="grid grid-cols-2 gap-2 text-xs pt-2 border-t border-gray-100">
                      <div>
                        <span className="text-gray-400">Page Context:</span>
                        <p className="font-mono text-gray-800 font-medium">
                          {selectedDetail.page || 'N/A'}
                        </p>
                      </div>
                      <div>
                        <span className="text-gray-400">Feature Context:</span>
                        <p className="font-mono text-gray-800 font-medium">
                          {selectedDetail.feature || 'N/A'}
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* Reporter & Practice Info */}
                  <div className="bg-white border border-gray-200 rounded-xl p-4 space-y-2 text-xs">
                    <h3 className="font-bold text-gray-800 flex items-center gap-1.5">
                      <User className="w-4 h-4 text-indigo-600" /> Reporter & Practice Profile
                    </h3>
                    <div className="grid grid-cols-2 gap-2 pt-1">
                      <div>
                        <span className="text-gray-400">Reporter:</span>
                        <p className="font-semibold text-gray-900">{selectedDetail.reporterName}</p>
                        <p className="text-gray-500">{selectedDetail.reporterEmail || 'No email'}</p>
                      </div>
                      {selectedDetail.practiceName ? (
                        <div>
                          <span className="text-gray-400">Practice:</span>
                          <p className="font-semibold text-gray-900">{selectedDetail.practiceName}</p>
                          <p className="text-gray-500">Plan: {selectedDetail.practiceSubscriptionPlan || 'N/A'}</p>
                        </div>
                      ) : (
                        <div>
                          <span className="text-gray-400">Account Type:</span>
                          <p className="font-medium text-gray-700">Marketplace Individual Customer</p>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Linked Engineering Issue (if escalated) */}
                  {selectedDetail.engineeringIssue && (
                    <div className="bg-red-50 border border-red-200 rounded-xl p-4 space-y-2 text-xs">
                      <div className="flex items-center justify-between">
                        <h3 className="font-bold text-red-900 flex items-center gap-1.5">
                          <Flame className="w-4 h-4 text-red-600" /> Linked Engineering Issue
                        </h3>
                        <span className="font-mono font-bold px-2 py-0.5 rounded bg-red-200 text-red-900">
                          {selectedDetail.engineeringIssue.issueCode}
                        </span>
                      </div>
                      <p className="font-semibold text-gray-900">{selectedDetail.engineeringIssue.title}</p>
                      <p className="text-gray-700 bg-white/70 p-2 rounded border border-red-100">
                        {selectedDetail.engineeringIssue.description}
                      </p>
                      <div className="flex items-center justify-between text-red-800 pt-1">
                        <span>Status: <strong>{selectedDetail.engineeringIssue.status}</strong></span>
                        <span>Team: <strong>{selectedDetail.engineeringIssue.assignedTeam}</strong></span>
                      </div>
                    </div>
                  )}

                  {/* Resolution / Closure Information */}
                  {selectedDetail.resolutionNote && (
                    <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-4 space-y-1 text-xs">
                      <h3 className="font-bold text-emerald-900 flex items-center gap-1.5">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600" /> Resolution Note
                      </h3>
                      <p className="text-emerald-950 whitespace-pre-wrap">{selectedDetail.resolutionNote}</p>
                      <p className="text-emerald-700 text-[11px] pt-1">
                        Resolved by {selectedDetail.resolvedByName || 'Admin'} on{' '}
                        {selectedDetail.resolvedAt ? new Date(selectedDetail.resolvedAt).toLocaleString() : ''}
                      </p>
                    </div>
                  )}

                  {/* Internal Notes Thread */}
                  <div className="bg-white border border-gray-200 rounded-xl p-4 space-y-3">
                    <div className="flex items-center justify-between">
                      <h3 className="font-bold text-gray-800 flex items-center gap-1.5 text-xs">
                        <MessageCircle className="w-4 h-4 text-purple-600" /> Internal Notes & Collaboration
                      </h3>
                      <span className="text-xs text-gray-400">
                        {selectedDetail.notes?.length || 0} notes
                      </span>
                    </div>

                    {/* Add Note Form */}
                    <form onSubmit={handleAddNote} className="space-y-2 pt-1">
                      <textarea
                        rows={2}
                        value={newNoteContent}
                        onChange={(e) => setNewNoteContent(e.target.value)}
                        placeholder="Add an internal note or discussion point..."
                        className="w-full text-xs p-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-purple-500"
                      />
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2 text-xs">
                          <button
                            type="button"
                            onClick={() =>
                              setNewNoteVisibility(
                                newNoteVisibility === 'INTERNAL' ? 'CUSTOMER_VISIBLE' : 'INTERNAL'
                              )
                            }
                            className={`px-2 py-0.5 rounded text-[11px] font-medium flex items-center gap-1 ${
                              newNoteVisibility === 'INTERNAL'
                                ? 'bg-amber-100 text-amber-800 border border-amber-200'
                                : 'bg-blue-100 text-blue-800 border border-blue-200'
                            }`}
                          >
                            {newNoteVisibility === 'INTERNAL' ? (
                              <>
                                <Lock className="w-3 h-3" /> Internal Only
                              </>
                            ) : (
                              <>
                                <Globe className="w-3 h-3" /> Customer Visible
                              </>
                            )}
                          </button>
                        </div>
                        <button
                          type="submit"
                          disabled={!newNoteContent.trim() || actionLoading}
                          className="px-3 py-1.5 bg-purple-600 text-white rounded-lg text-xs font-semibold hover:bg-purple-700 disabled:opacity-50 flex items-center"
                        >
                          <Send className="w-3 h-3 mr-1" /> Post Note
                        </button>
                      </div>
                    </form>

                    {/* Notes List */}
                    <div className="space-y-2 pt-2 border-t border-gray-100 max-h-48 overflow-y-auto">
                      {(!selectedDetail.notes || selectedDetail.notes.length === 0) ? (
                        <p className="text-xs text-gray-400 text-center py-2">No internal notes yet.</p>
                      ) : (
                        selectedDetail.notes.map((note) => (
                          <div key={note.id} className="bg-gray-50 p-2.5 rounded-lg border border-gray-200/70 text-xs space-y-1">
                            <div className="flex items-center justify-between text-[11px] text-gray-500">
                              <span className="font-semibold text-gray-800">{note.authorName || 'Admin'}</span>
                              <div className="flex items-center gap-1.5">
                                <span className={`px-1.5 py-0.2 rounded text-[10px] ${
                                  note.visibility === 'INTERNAL' ? 'bg-amber-100 text-amber-800' : 'bg-blue-100 text-blue-800'
                                }`}>
                                  {note.visibility}
                                </span>
                                <span>{new Date(note.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                              </div>
                            </div>
                            <p className="text-gray-700 whitespace-pre-wrap">{note.note}</p>
                          </div>
                        ))
                      )}
                    </div>
                  </div>

                  {/* Activity Timeline */}
                  <div className="bg-white border border-gray-200 rounded-xl p-4 space-y-2">
                    <h3 className="font-bold text-gray-800 flex items-center gap-1.5 text-xs">
                      <History className="w-4 h-4 text-indigo-600" /> Status & Audit Timeline
                    </h3>
                    <div className="space-y-3 pt-2">
                      {selectedDetail.timeline?.map((item, idx) => (
                        <div key={item.id || idx} className="flex items-start gap-2 text-xs">
                          <div className="mt-1 w-2 h-2 rounded-full bg-indigo-600 flex-shrink-0" />
                          <div className="flex-1">
                            <div className="flex items-center justify-between">
                              <p className="font-semibold text-gray-800">
                                {item.oldStatus ? `${item.oldStatus} → ` : 'Created as '}
                                <span className="text-indigo-600">{item.newStatus}</span>
                              </p>
                              <span className="text-[11px] text-gray-400">
                                {new Date(item.createdAt).toLocaleDateString()}
                              </span>
                            </div>
                            {item.changedByName && (
                              <p className="text-[11px] text-gray-500">By {item.changedByName}</p>
                            )}
                            {item.reason && (
                              <p className="text-[11px] text-gray-600 italic bg-gray-50 p-1 rounded mt-0.5">
                                "{item.reason}"
                              </p>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* ========================================================================= */}
      {/* ACTION MODALS */}
      {/* ========================================================================= */}

      {/* 1. Assign Modal */}
      {activeModal === 'ASSIGN' && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                <UserCheck className="w-5 h-5 text-purple-600" /> Assign Feedback
              </h3>
              <button onClick={() => setActiveModal('NONE')} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={handleAssign} className="space-y-4 text-sm">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Target Team</label>
                <select
                  value={assignTeam}
                  onChange={(e) => setAssignTeam(e.target.value as FeedbackTeam)}
                  className="w-full p-2.5 border border-gray-300 rounded-lg bg-white"
                >
                  {teams.map((t) => (
                    <option key={t} value={t}>
                      {t.replace('_', ' ')}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Specific Assignee (Optional)</label>
                <select
                  value={assignUserId}
                  onChange={(e) => setAssignUserId(e.target.value)}
                  className="w-full p-2.5 border border-gray-300 rounded-lg bg-white"
                >
                  <option value="">Unassigned (Team Inbox)</option>
                  {assignees.map((a) => (
                    <option key={a.userId} value={a.userId}>
                      {a.name} ({a.role})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Assignment Reason / Context</label>
                <textarea
                  rows={2}
                  value={assignReason}
                  onChange={(e) => setAssignReason(e.target.value)}
                  placeholder="Why is this assigned to this team..."
                  className="w-full p-2.5 border border-gray-300 rounded-lg"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setActiveModal('NONE')}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-4 py-2 bg-purple-600 text-white rounded-lg font-semibold hover:bg-purple-700"
                >
                  Confirm Assignment
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 2. Change Priority Modal */}
      {activeModal === 'PRIORITY' && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                <Tag className="w-5 h-5 text-indigo-600" /> Update Feedback Priority
              </h3>
              <button onClick={() => setActiveModal('NONE')} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={handleUpdatePriority} className="space-y-4 text-sm">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">New Priority</label>
                <select
                  value={newPriority}
                  onChange={(e) => setNewPriority(e.target.value as ApplicationFeedbackPriority)}
                  className="w-full p-2.5 border border-gray-300 rounded-lg bg-white"
                >
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                  <option value="CRITICAL">Critical</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Reason for Change</label>
                <input
                  type="text"
                  value={priorityReason}
                  onChange={(e) => setPriorityReason(e.target.value)}
                  placeholder="e.g. Impacts multiple clients on filing deadline"
                  className="w-full p-2.5 border border-gray-300 rounded-lg"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setActiveModal('NONE')}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-4 py-2 bg-indigo-600 text-white rounded-lg font-semibold hover:bg-indigo-700"
                >
                  Save Priority
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 3. Resolve Modal */}
      {activeModal === 'RESOLVE' && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                <CheckCircle2 className="w-5 h-5 text-emerald-600" /> Resolve Feedback
              </h3>
              <button onClick={() => setActiveModal('NONE')} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={handleResolve} className="space-y-4 text-sm">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Resolution Summary / Note *</label>
                <textarea
                  rows={4}
                  required
                  value={resolutionNote}
                  onChange={(e) => setResolutionNote(e.target.value)}
                  placeholder="Detail how this was resolved or fixed..."
                  className="w-full p-2.5 border border-gray-300 rounded-lg"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setActiveModal('NONE')}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-4 py-2 bg-emerald-600 text-white rounded-lg font-semibold hover:bg-emerald-700"
                >
                  Mark as Resolved
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 4. Escalate to Engineering Modal */}
      {activeModal === 'ESCALATE' && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                <Flame className="w-5 h-5 text-red-600" /> Escalate to Engineering
              </h3>
              <button onClick={() => setActiveModal('NONE')} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={handleEscalateToEngineering} className="space-y-3 text-sm">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Issue Title *</label>
                <input
                  type="text"
                  required
                  value={escalateTitle}
                  onChange={(e) => setEscalateTitle(e.target.value)}
                  className="w-full p-2.5 border border-gray-300 rounded-lg"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Engineering Priority</label>
                <select
                  value={escalatePriority}
                  onChange={(e) => setEscalatePriority(e.target.value as ApplicationFeedbackPriority)}
                  className="w-full p-2.5 border border-gray-300 rounded-lg bg-white"
                >
                  <option value="CRITICAL">Critical / Blocker</option>
                  <option value="HIGH">High</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="LOW">Low</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Technical Details / Repro *</label>
                <textarea
                  rows={4}
                  required
                  value={escalateDesc}
                  onChange={(e) => setEscalateDesc(e.target.value)}
                  className="w-full p-2.5 border border-gray-300 rounded-lg font-mono text-xs"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Internal Note for Engineering</label>
                <input
                  type="text"
                  value={escalateNotes}
                  onChange={(e) => setEscalateNotes(e.target.value)}
                  placeholder="e.g. Target sprint v1.2"
                  className="w-full p-2.5 border border-gray-300 rounded-lg"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setActiveModal('NONE')}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-4 py-2 bg-red-600 text-white rounded-lg font-semibold hover:bg-red-700"
                >
                  Create Engineering Issue
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 5. Mark Duplicate Modal */}
      {activeModal === 'DUPLICATE' && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                <Copy className="w-5 h-5 text-slate-600" /> Mark as Duplicate
              </h3>
              <button onClick={() => setActiveModal('NONE')} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={handleMarkDuplicate} className="space-y-4 text-sm">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Canonical Feedback UUID *</label>
                <input
                  type="text"
                  required
                  value={duplicateTargetId}
                  onChange={(e) => setDuplicateTargetId(e.target.value)}
                  placeholder="Paste UUID of original feedback item"
                  className="w-full p-2.5 border border-gray-300 rounded-lg font-mono text-xs"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Duplicate Reason</label>
                <input
                  type="text"
                  value={duplicateReason}
                  onChange={(e) => setDuplicateReason(e.target.value)}
                  placeholder="e.g. Same issue reported by multiple users"
                  className="w-full p-2.5 border border-gray-300 rounded-lg"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setActiveModal('NONE')}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-4 py-2 bg-slate-700 text-white rounded-lg font-semibold hover:bg-slate-800"
                >
                  Mark Duplicate
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 6. Reject Modal */}
      {activeModal === 'REJECT' && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                <X className="w-5 h-5 text-rose-600" /> Reject Feedback
              </h3>
              <button onClick={() => setActiveModal('NONE')} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={handleReject} className="space-y-4 text-sm">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Rejection Reason *</label>
                <textarea
                  rows={3}
                  required
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  placeholder="Explain why this feedback cannot be accepted..."
                  className="w-full p-2.5 border border-gray-300 rounded-lg"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setActiveModal('NONE')}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-4 py-2 bg-rose-600 text-white rounded-lg font-semibold hover:bg-rose-700"
                >
                  Confirm Reject
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 7. Close Modal */}
      {activeModal === 'CLOSE' && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                <Check className="w-5 h-5 text-gray-800" /> Close Feedback
              </h3>
              <button onClick={() => setActiveModal('NONE')} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={handleClose} className="space-y-4 text-sm">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Closure Note / Reason (Optional)</label>
                <textarea
                  rows={3}
                  value={closeReason}
                  onChange={(e) => setCloseReason(e.target.value)}
                  placeholder="Additional closing remarks..."
                  className="w-full p-2.5 border border-gray-300 rounded-lg"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setActiveModal('NONE')}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-4 py-2 bg-gray-800 text-white rounded-lg font-semibold hover:bg-gray-900"
                >
                  Close Feedback
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
