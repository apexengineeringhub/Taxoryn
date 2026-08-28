import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  FileText,
  Video,
  BookOpen,
  HelpCircle,
  Bell,
  Plus,
  Search,
  Filter,
  Eye,
  Edit,
  CheckCircle2,
  Send,
  Archive,
  RotateCcw,
  Sparkles,
  ExternalLink,
  Clock,
  Play,
  X,
  AlertCircle,
  Calendar,
  Layers,
  Palette,
  CheckSquare,
  ShieldCheck,
  Store,
  Upload,
  Copy,
  Trash2,
  History,
  AlertTriangle,
  ChevronRight,
  TrendingUp,
  FileCheck,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { WorkspacePageHeader } from '../components/layout/WorkspacePageHeader';
import { YouTubePlayer } from '../components/learn/YouTubePlayer';
import { adminLearnApi, adminMediaApi, publicLearnApi, marketplacePublicApi } from '../api/endpoints';
import {
  LearnContentDetail,
  LearnContentStatus,
  LearnContentSummary,
  LearnContentType,
  LearnPublicCategory,
  PublicTaxService,
  ContentDashboardStats,
  MediaAsset,
  ContentVersion,
} from '../types';
import clsx from 'clsx';

type StudioTab = 'dashboard' | 'content' | 'review-queue' | 'media' | 'categories' | 'tax-services';

export const PlatformContentManagementPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab: StudioTab = (searchParams.get('tab') as StudioTab) || 'dashboard';

  const setTab = (tab: StudioTab) => {
    setSearchParams({ tab });
  };

  // Dashboard Stats State
  const [stats, setStats] = useState<ContentDashboardStats | null>(null);
  const [isStatsLoading, setIsStatsLoading] = useState<boolean>(false);

  // Content List State
  const [contentList, setContentList] = useState<LearnContentSummary[]>([]);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const [searchFilter, setSearchFilter] = useState<string>('');
  const [typeFilter, setTypeFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [categoryFilter, setCategoryFilter] = useState<string>('');

  const [categories, setCategories] = useState<LearnPublicCategory[]>([]);
  const [masterTaxServices, setMasterTaxServices] = useState<PublicTaxService[]>([]);

  // Review Queue State
  const [reviewQueue, setReviewQueue] = useState<LearnContentSummary[]>([]);
  const [isReviewQueueLoading, setIsReviewQueueLoading] = useState<boolean>(false);

  // Media Library State
  const [mediaAssets, setMediaAssets] = useState<MediaAsset[]>([]);
  const [mediaSearch, setMediaSearch] = useState<string>('');
  const [isMediaLoading, setIsMediaLoading] = useState<boolean>(false);
  const [isMediaUploadOpen, setIsMediaUploadOpen] = useState<boolean>(false);
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadAltText, setUploadAltText] = useState<string>('');
  const [isUploadingMedia, setIsUploadingMedia] = useState<boolean>(false);
  const [mediaUploadError, setMediaUploadError] = useState<string | null>(null);
  const [isMediaPickerOpen, setIsMediaPickerOpen] = useState<boolean>(false);
  const [mediaPickerTarget, setMediaPickerTarget] = useState<'thumbnail' | 'featured'>('thumbnail');

  // Modal States
  const [isEditorOpen, setIsEditorOpen] = useState<boolean>(false);
  const [editingContentId, setEditingContentId] = useState<string | null>(null);
  const [isPreviewOpen, setIsPreviewOpen] = useState<boolean>(false);
  const [previewContent, setPreviewContent] = useState<LearnContentDetail | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // Rejection Modal State
  const [isRejectModalOpen, setIsRejectModalOpen] = useState<boolean>(false);
  const [rejectContentId, setRejectContentId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState<string>('');

  // Schedule Modal State
  const [isScheduleModalOpen, setIsScheduleModalOpen] = useState<boolean>(false);
  const [scheduleContentId, setScheduleContentId] = useState<string | null>(null);
  const [scheduleDateTime, setScheduleDateTime] = useState<string>('');

  // Version History Modal State
  const [isVersionModalOpen, setIsVersionModalOpen] = useState<boolean>(false);
  const [versionHistory, setVersionHistory] = useState<ContentVersion[]>([]);
  const [isVersionLoading, setIsVersionLoading] = useState<boolean>(false);
  const [versionContentTitle, setVersionContentTitle] = useState<string>('');

  // Editor Form State
  const [formType, setFormType] = useState<LearnContentType>('ARTICLE');
  const [formTitle, setFormTitle] = useState<string>('');
  const [formSlug, setFormSlug] = useState<string>('');
  const [formSummary, setFormSummary] = useState<string>('');
  const [formBody, setFormBody] = useState<string>('');
  const [formThumbnailUrl, setFormThumbnailUrl] = useState<string>('');
  const [formFeaturedImageUrl, setFormFeaturedImageUrl] = useState<string>('');
  const [formAltText, setFormAltText] = useState<string>('');
  const [formSeoTitle, setFormSeoTitle] = useState<string>('');
  const [formMetaDescription, setFormMetaDescription] = useState<string>('');
  const [formCanonicalUrl, setFormCanonicalUrl] = useState<string>('');
  const [formYoutubeUrl, setFormYoutubeUrl] = useState<string>('');
  const [formDurationSeconds, setFormDurationSeconds] = useState<string>('300');
  const [formCategoryId, setFormCategoryId] = useState<string>('');
  const [formTaxServiceIds, setFormTaxServiceIds] = useState<string[]>([]);
  const [formTags, setFormTags] = useState<string>('');

  useEffect(() => {
    loadCategories();
    loadTaxServices();
  }, []);

  useEffect(() => {
    if (activeTab === 'dashboard') {
      loadDashboardStats();
    } else if (activeTab === 'content') {
      loadContent();
    } else if (activeTab === 'review-queue') {
      loadReviewQueue();
    } else if (activeTab === 'media') {
      loadMediaAssets();
    }
  }, [activeTab, page, typeFilter, statusFilter, categoryFilter, mediaSearch]);

  const loadTaxServices = async () => {
    try {
      const services = await marketplacePublicApi.getTaxServices();
      setMasterTaxServices(services || []);
    } catch {
      setMasterTaxServices([]);
    }
  };

  const loadCategories = async () => {
    try {
      const cats = await publicLearnApi.getCategories();
      setCategories(cats || []);
    } catch {
      setCategories([]);
    }
  };

  const loadDashboardStats = async () => {
    try {
      setIsStatsLoading(true);
      const data = await adminLearnApi.getDashboardStats();
      setStats(data || null);
    } catch (err) {
      console.error('Failed to load dashboard stats', err);
    } finally {
      setIsStatsLoading(false);
    }
  };

  const loadContent = async () => {
    try {
      setIsLoading(true);
      const res = await adminLearnApi.getContentList({
        page,
        size: 10,
        search: searchFilter || undefined,
        contentType: typeFilter || undefined,
        status: statusFilter || undefined,
        categoryId: categoryFilter || undefined,
      });
      setContentList(res?.content || []);
      setTotalElements(res?.totalElements || 0);
      setTotalPages(res?.totalPages || 1);
    } catch (err) {
      console.error('Failed to load admin content', err);
    } finally {
      setIsLoading(false);
    }
  };

  const loadReviewQueue = async () => {
    try {
      setIsReviewQueueLoading(true);
      const res = await adminLearnApi.getReviewQueue({ page: 0, size: 20 });
      setReviewQueue(res?.content || []);
    } catch (err) {
      console.error('Failed to load review queue', err);
    } finally {
      setIsReviewQueueLoading(false);
    }
  };

  const loadMediaAssets = async () => {
    try {
      setIsMediaLoading(true);
      const res = await adminMediaApi.getMediaAssets({
        search: mediaSearch || undefined,
        page: 0,
        size: 24,
      });
      setMediaAssets(res?.content || []);
    } catch (err) {
      console.error('Failed to load media assets', err);
    } finally {
      setIsMediaLoading(false);
    }
  };

  // Reset form
  const resetForm = () => {
    setEditingContentId(null);
    setFormType('ARTICLE');
    setFormTitle('');
    setFormSlug('');
    setFormSummary('');
    setFormBody('');
    setFormThumbnailUrl('');
    setFormFeaturedImageUrl('');
    setFormAltText('');
    setFormSeoTitle('');
    setFormMetaDescription('');
    setFormCanonicalUrl('');
    setFormYoutubeUrl('');
    setFormDurationSeconds('300');
    setFormCategoryId(categories[0]?.id || '');
    setFormTaxServiceIds([]);
    setFormTags('');
    setActionError(null);
  };

  // Open modal for Create
  const handleOpenCreate = (type: LearnContentType = 'ARTICLE') => {
    resetForm();
    setFormType(type);
    setIsEditorOpen(true);
  };

  // Open modal for Edit
  const handleOpenEdit = async (item: LearnContentSummary) => {
    try {
      resetForm();
      setActionError(null);
      const detail = await adminLearnApi.getContentById(item.id);
      setEditingContentId(detail.id);
      setFormType(detail.contentType);
      setFormTitle(detail.title);
      setFormSlug(detail.slug);
      setFormSummary(detail.summary || '');
      setFormBody(detail.body || '');
      setFormThumbnailUrl(detail.thumbnailUrl || '');
      setFormFeaturedImageUrl(detail.featuredImageUrl || '');
      setFormAltText(detail.altText || '');
      setFormSeoTitle(detail.seoTitle || '');
      setFormMetaDescription(detail.metaDescription || '');
      setFormCanonicalUrl(detail.canonicalUrl || '');
      setFormYoutubeUrl(detail.youtubeWatchUrl || '');
      setFormDurationSeconds(detail.videoDurationSeconds ? String(detail.videoDurationSeconds) : '300');
      setFormCategoryId(detail.categoryId || '');

      const attachedIds: string[] = [];
      if (detail.taxServices && detail.taxServices.length > 0) {
        attachedIds.push(...detail.taxServices.map((s) => s.id));
      } else if (detail.taxServiceId) {
        attachedIds.push(detail.taxServiceId);
      }
      setFormTaxServiceIds(attachedIds);

      setFormTags((detail.tags || []).map((t) => t.name).join(', '));
      setIsEditorOpen(true);
    } catch (err: any) {
      console.error('Failed to load item for edit', err);
      setActionError(err.response?.data?.message || 'Failed to load content for editing');
    }
  };

  // Open Preview Modal
  const handleOpenPreview = async (id: string) => {
    try {
      const detail = await adminLearnApi.previewContent(id);
      setPreviewContent(detail);
      setIsPreviewOpen(true);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to generate preview');
    }
  };

  // Save Content (Draft)
  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formTitle.trim()) {
      setActionError('Title is required');
      return;
    }
    if (!formBody.trim()) {
      setActionError('Content body is required');
      return;
    }
    if (formType === 'VIDEO' && !formYoutubeUrl.trim()) {
      setActionError('YouTube video URL is required for Video content.');
      return;
    }

    try {
      setIsSubmitting(true);
      setActionError(null);

      const parsedTags = formTags
        .split(',')
        .map((t) => t.trim())
        .filter((t) => t.length > 0);

      const payload = {
        contentType: formType,
        title: formTitle.trim(),
        slug: formSlug.trim() || undefined,
        summary: formSummary.trim() || undefined,
        body: formBody.trim(),
        thumbnailUrl: formThumbnailUrl.trim() || undefined,
        featuredImageUrl: formFeaturedImageUrl.trim() || undefined,
        altText: formAltText.trim() || undefined,
        seoTitle: formSeoTitle.trim() || undefined,
        metaDescription: formMetaDescription.trim() || undefined,
        canonicalUrl: formCanonicalUrl.trim() || undefined,
        youtubeUrl: formType === 'VIDEO' ? formYoutubeUrl.trim() : undefined,
        videoDurationSeconds: formType === 'VIDEO' && formDurationSeconds ? parseInt(formDurationSeconds, 10) : undefined,
        categoryId: formCategoryId || undefined,
        taxServiceId: formTaxServiceIds.length > 0 ? formTaxServiceIds[0] : undefined,
        taxServiceIds: formTaxServiceIds,
        tags: parsedTags,
      };

      if (editingContentId) {
        await adminLearnApi.updateContent(editingContentId, payload);
      } else {
        await adminLearnApi.createContent(payload);
      }

      setIsEditorOpen(false);
      resetForm();
      loadContent();
      if (activeTab === 'dashboard') loadDashboardStats();
    } catch (err: any) {
      console.error('Save failed', err);
      setActionError(err.response?.data?.message || 'Failed to save content item');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Lifecycle Actions
  const handleSubmitReview = async (id: string) => {
    try {
      await adminLearnApi.submitForReview(id);
      refreshData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to submit for review');
    }
  };

  const handleStartReview = async (id: string) => {
    try {
      await adminLearnApi.startReview(id);
      refreshData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to start review');
    }
  };

  const handleApprove = async (id: string) => {
    try {
      await adminLearnApi.approveContent(id);
      refreshData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to approve content');
    }
  };

  const handleOpenRejectModal = (id: string) => {
    setRejectContentId(id);
    setRejectReason('');
    setIsRejectModalOpen(true);
  };

  const handleConfirmReject = async () => {
    if (!rejectContentId || !rejectReason.trim()) {
      alert('Please provide a reason for rejection.');
      return;
    }
    try {
      await adminLearnApi.rejectContent(rejectContentId, rejectReason.trim());
      setIsRejectModalOpen(false);
      setRejectContentId(null);
      setRejectReason('');
      refreshData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to reject content');
    }
  };

  const handleOpenScheduleModal = (id: string) => {
    setScheduleContentId(id);
    // Set default schedule time to tomorrow 09:00 AM
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(9, 0, 0, 0);
    setScheduleDateTime(tomorrow.toISOString().slice(0, 16));
    setIsScheduleModalOpen(true);
  };

  const handleConfirmSchedule = async () => {
    if (!scheduleContentId || !scheduleDateTime) {
      alert('Please select a valid future date and time.');
      return;
    }
    try {
      const scheduledIso = new Date(scheduleDateTime).toISOString();
      await adminLearnApi.scheduleContent(scheduleContentId, scheduledIso);
      setIsScheduleModalOpen(false);
      setScheduleContentId(null);
      refreshData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to schedule content publication');
    }
  };

  const handlePublish = async (id: string) => {
    try {
      await adminLearnApi.publishContent(id);
      refreshData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to publish content');
    }
  };

  const handleArchive = async (id: string) => {
    if (!confirm('Are you sure you want to archive this content? It will be removed from public view.')) return;
    try {
      await adminLearnApi.archiveContent(id);
      refreshData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to archive content');
    }
  };

  const handleRestore = async (id: string) => {
    try {
      await adminLearnApi.restoreContent(id);
      refreshData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to restore content');
    }
  };

  const handleOpenVersionHistory = async (item: LearnContentSummary) => {
    try {
      setVersionContentTitle(item.title);
      setIsVersionLoading(true);
      setIsVersionModalOpen(true);
      const versions = await adminLearnApi.getVersionHistory(item.id);
      setVersionHistory(versions || []);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to fetch version history');
    } finally {
      setIsVersionLoading(false);
    }
  };

  const refreshData = () => {
    if (activeTab === 'dashboard') loadDashboardStats();
    if (activeTab === 'content') loadContent();
    if (activeTab === 'review-queue') loadReviewQueue();
  };

  // Media Library Handlers
  const handleUploadMedia = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadFile) {
      setMediaUploadError('Please select a file to upload.');
      return;
    }
    try {
      setIsUploadingMedia(true);
      setMediaUploadError(null);
      await adminMediaApi.uploadMedia(uploadFile, uploadAltText.trim() || undefined);
      setIsMediaUploadOpen(false);
      setUploadFile(null);
      setUploadAltText('');
      loadMediaAssets();
    } catch (err: any) {
      setMediaUploadError(err.response?.data?.message || 'Failed to upload media asset');
    } finally {
      setIsUploadingMedia(false);
    }
  };

  const handleDeleteMedia = async (id: string) => {
    if (!confirm('Are you sure you want to delete this media asset?')) return;
    try {
      await adminMediaApi.deleteMedia(id);
      loadMediaAssets();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to delete media asset');
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    alert('Copied to clipboard: ' + text);
  };

  const handleSelectMediaForEditor = (asset: MediaAsset) => {
    if (mediaPickerTarget === 'thumbnail') {
      setFormThumbnailUrl(asset.publicUrl);
    } else {
      setFormFeaturedImageUrl(asset.publicUrl);
    }
    if (asset.altText && !formAltText) {
      setFormAltText(asset.altText);
    }
    setIsMediaPickerOpen(false);
  };

  const toggleTaxService = (serviceId: string) => {
    setFormTaxServiceIds((prev) =>
      prev.includes(serviceId) ? prev.filter((id) => id !== serviceId) : [...prev, serviceId]
    );
  };

  // Helper for Status Badges
  const renderStatusBadge = (status: LearnContentStatus) => {
    switch (status) {
      case 'PUBLISHED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
            PUBLISHED
          </span>
        );
      case 'SCHEDULED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200">
            <Calendar className="w-3.5 h-3.5" />
            SCHEDULED
          </span>
        );
      case 'APPROVED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
            <CheckCircle2 className="w-3.5 h-3.5" />
            APPROVED
          </span>
        );
      case 'IN_REVIEW':
      case 'SUBMITTED':
      case 'UNDER_REVIEW':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
            <Clock className="w-3.5 h-3.5" />
            IN REVIEW
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200">
            <AlertCircle className="w-3.5 h-3.5" />
            REJECTED
          </span>
        );
      case 'ARCHIVED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-600 border border-slate-200">
            <Archive className="w-3.5 h-3.5" />
            ARCHIVED
          </span>
        );
      case 'DRAFT':
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-50 text-slate-700 border border-slate-300">
            DRAFT
          </span>
        );
    }
  };

  const renderTypeIcon = (type: LearnContentType) => {
    switch (type) {
      case 'ARTICLE':
        return <FileText className="w-4 h-4 text-blue-600" />;
      case 'VIDEO':
        return <Video className="w-4 h-4 text-rose-600" />;
      case 'GUIDE':
        return <BookOpen className="w-4 h-4 text-emerald-600" />;
      case 'FAQ':
        return <HelpCircle className="w-4 h-4 text-purple-600" />;
      case 'TAX_UPDATE':
        return <Bell className="w-4 h-4 text-amber-600" />;
      default:
        return <FileText className="w-4 h-4 text-slate-600" />;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <WorkspacePageHeader
        sectionBadge="Studio & Marketing"
        title="Taxoryn Content & Marketing Studio"
        description="Manage customer-facing educational content, review pipelines, media library, and tax service attribution."
      >
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            onClick={() => handleOpenCreate('VIDEO')}
            className="gap-1.5 border-rose-200 text-rose-700 hover:bg-rose-50"
          >
            <Video className="w-4 h-4 text-rose-600" />
            New Video
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={() => handleOpenCreate('ARTICLE')}
            className="gap-1.5 bg-teal-600 hover:bg-teal-700 text-white"
          >
            <Plus className="w-4 h-4" />
            New Content
          </Button>
        </div>
      </WorkspacePageHeader>

      {/* Navigation Tabs */}
      <div className="flex border-b border-slate-200 bg-white rounded-t-xl px-4 gap-1 shadow-xs">
        <button
          onClick={() => setTab('dashboard')}
          className={clsx(
            'flex items-center gap-2 py-3.5 px-4 text-sm font-medium border-b-2 transition-colors',
            activeTab === 'dashboard'
              ? 'border-teal-600 text-teal-700 font-semibold'
              : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
          )}
        >
          <TrendingUp className="w-4 h-4" />
          Dashboard
        </button>
        <button
          onClick={() => setTab('content')}
          className={clsx(
            'flex items-center gap-2 py-3.5 px-4 text-sm font-medium border-b-2 transition-colors',
            activeTab === 'content'
              ? 'border-teal-600 text-teal-700 font-semibold'
              : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
          )}
        >
          <BookOpen className="w-4 h-4" />
          All Content
        </button>
        <button
          onClick={() => setTab('review-queue')}
          className={clsx(
            'flex items-center gap-2 py-3.5 px-4 text-sm font-medium border-b-2 transition-colors',
            activeTab === 'review-queue'
              ? 'border-teal-600 text-teal-700 font-semibold'
              : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
          )}
        >
          <CheckSquare className="w-4 h-4" />
          Review Queue
          {stats && stats.inReviewCount > 0 && (
            <span className="ml-1.5 px-2 py-0.5 text-xs rounded-full bg-amber-100 text-amber-800 font-bold">
              {stats.inReviewCount}
            </span>
          )}
        </button>
        <button
          onClick={() => setTab('media')}
          className={clsx(
            'flex items-center gap-2 py-3.5 px-4 text-sm font-medium border-b-2 transition-colors',
            activeTab === 'media'
              ? 'border-teal-600 text-teal-700 font-semibold'
              : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
          )}
        >
          <Palette className="w-4 h-4" />
          Media Library
        </button>
        <button
          onClick={() => setTab('categories')}
          className={clsx(
            'flex items-center gap-2 py-3.5 px-4 text-sm font-medium border-b-2 transition-colors',
            activeTab === 'categories'
              ? 'border-teal-600 text-teal-700 font-semibold'
              : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
          )}
        >
          <Layers className="w-4 h-4" />
          Categories
        </button>
        <button
          onClick={() => setTab('tax-services')}
          className={clsx(
            'flex items-center gap-2 py-3.5 px-4 text-sm font-medium border-b-2 transition-colors',
            activeTab === 'tax-services'
              ? 'border-teal-600 text-teal-700 font-semibold'
              : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
          )}
        >
          <Store className="w-4 h-4" />
          Tax Services (Master)
        </button>
      </div>

      {/* ========================================================================= */}
      {/* TAB 1: STUDIO DASHBOARD */}
      {/* ========================================================================= */}
      {activeTab === 'dashboard' && (
        <div className="space-y-6">
          {/* Operational Metrics Cards */}
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
              <div className="text-xs font-semibold text-slate-500 uppercase">Total Items</div>
              <div className="text-2xl font-bold text-slate-900 mt-1">{stats?.totalContent ?? '-'}</div>
              <div className="text-xs text-slate-500 mt-1">Across all categories</div>
            </div>
            <div className="bg-white p-4 rounded-xl border border-emerald-100 shadow-xs bg-gradient-to-br from-white to-emerald-50/30">
              <div className="text-xs font-semibold text-emerald-700 uppercase">Published</div>
              <div className="text-2xl font-bold text-emerald-800 mt-1">{stats?.publishedCount ?? '-'}</div>
              <div className="text-xs text-emerald-600 mt-1">Live in Learn portal</div>
            </div>
            <div className="bg-white p-4 rounded-xl border border-amber-100 shadow-xs bg-gradient-to-br from-white to-amber-50/30">
              <div className="text-xs font-semibold text-amber-700 uppercase">In Review</div>
              <div className="text-2xl font-bold text-amber-800 mt-1">{stats?.inReviewCount ?? '-'}</div>
              <div className="text-xs text-amber-600 mt-1">Awaiting approval</div>
            </div>
            <div className="bg-white p-4 rounded-xl border border-indigo-100 shadow-xs bg-gradient-to-br from-white to-indigo-50/30">
              <div className="text-xs font-semibold text-indigo-700 uppercase">Scheduled</div>
              <div className="text-2xl font-bold text-indigo-800 mt-1">{stats?.scheduledCount ?? '-'}</div>
              <div className="text-xs text-indigo-600 mt-1">Auto-publishing</div>
            </div>
            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
              <div className="text-xs font-semibold text-slate-600 uppercase">Drafts</div>
              <div className="text-2xl font-bold text-slate-800 mt-1">{stats?.draftCount ?? '-'}</div>
              <div className="text-xs text-slate-500 mt-1">Work in progress</div>
            </div>
            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
              <div className="text-xs font-semibold text-slate-500 uppercase">Archived</div>
              <div className="text-2xl font-bold text-slate-700 mt-1">{stats?.archivedCount ?? '-'}</div>
              <div className="text-xs text-slate-400 mt-1">Deprecations</div>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Needs Attention Queue */}
            <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
              <div className="p-4 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <AlertCircle className="w-5 h-5 text-amber-600" />
                  <h3 className="font-bold text-slate-900">Needs Attention</h3>
                </div>
                <button
                  onClick={() => setTab('review-queue')}
                  className="text-xs font-semibold text-teal-600 hover:text-teal-800 flex items-center gap-1"
                >
                  View Review Queue <ChevronRight className="w-3.5 h-3.5" />
                </button>
              </div>
              <div className="divide-y divide-slate-100">
                {stats?.needsAttention && stats.needsAttention.length > 0 ? (
                  stats.needsAttention.map((item) => (
                    <div key={item.id} className="p-4 hover:bg-slate-50 flex items-center justify-between">
                      <div className="min-w-0 pr-4">
                        <div className="flex items-center gap-2 mb-1">
                          <span className="text-xs font-medium px-2 py-0.5 bg-slate-100 text-slate-700 rounded">
                            {item.contentType}
                          </span>
                          <span className="text-xs text-amber-700 font-semibold">{item.status}</span>
                        </div>
                        <h4 className="font-semibold text-slate-900 text-sm truncate">{item.title}</h4>
                        <p className="text-xs text-slate-500 mt-0.5">{item.message}</p>
                      </div>
                      <div className="flex items-center gap-2 shrink-0">
                        <Button size="sm" variant="outline" onClick={() => handleOpenPreview(item.id)}>
                          <Eye className="w-3.5 h-3.5 mr-1" /> Preview
                        </Button>
                        <Button
                          size="sm"
                          variant="primary"
                          className="bg-emerald-600 hover:bg-emerald-700 text-white"
                          onClick={() => handleApprove(item.id)}
                        >
                          Approve
                        </Button>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="p-8 text-center text-slate-500 text-sm">
                    <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto mb-2 opacity-80" />
                    All content queues are clear. No pending reviews require attention.
                  </div>
                )}
              </div>
            </div>

            {/* Quick Actions & Recent Activity */}
            <div className="space-y-6">
              <div className="bg-gradient-to-br from-teal-800 to-slate-900 text-white rounded-xl p-5 shadow-sm">
                <h3 className="font-bold text-base mb-1 flex items-center gap-2">
                  <Sparkles className="w-5 h-5 text-teal-300" />
                  Taxoryn Content Studio
                </h3>
                <p className="text-xs text-teal-100 mb-4 leading-relaxed">
                  Deliver high-impact educational compliance articles, tutorials, and videos attached directly to
                  verified CA/CS tax services.
                </p>
                <div className="space-y-2">
                  <Button
                    size="sm"
                    className="w-full justify-start bg-teal-600 hover:bg-teal-700 text-white gap-2"
                    onClick={() => handleOpenCreate('ARTICLE')}
                  >
                    <FileText className="w-4 h-4" /> Draft New Compliance Article
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    className="w-full justify-start bg-white/10 hover:bg-white/20 border-white/20 text-white gap-2"
                    onClick={() => handleOpenCreate('VIDEO')}
                  >
                    <Video className="w-4 h-4 text-rose-300" /> Draft YouTube Video
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    className="w-full justify-start bg-white/10 hover:bg-white/20 border-white/20 text-white gap-2"
                    onClick={() => setTab('media')}
                  >
                    <Palette className="w-4 h-4 text-teal-300" /> Upload Images to Media Library
                  </Button>
                </div>
              </div>

              {/* Recent Activity */}
              <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-xs">
                <h4 className="font-bold text-slate-900 text-sm mb-3">Recent Activity</h4>
                <div className="space-y-3">
                  {stats?.recentActivity && stats.recentActivity.length > 0 ? (
                    stats.recentActivity.map((act) => (
                      <div key={act.id} className="text-xs border-l-2 border-teal-500 pl-2.5 py-0.5">
                        <div className="font-semibold text-slate-800 truncate">{act.contentTitle}</div>
                        <div className="text-slate-500 text-[11px] mt-0.5">
                          Status updated to <span className="font-medium text-teal-700">{act.action}</span> by {act.userName}
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="text-xs text-slate-400 italic">No recent activity recorded.</div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 2: ALL CONTENT */}
      {/* ========================================================================= */}
      {activeTab === 'content' && (
        <div className="space-y-4">
          {/* Search & Filters */}
          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs flex flex-wrap items-center gap-3">
            <div className="relative flex-1 min-w-[240px]">
              <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
              <input
                type="text"
                placeholder="Search title, summary, or slug..."
                value={searchFilter}
                onChange={(e) => setSearchFilter(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && loadContent()}
                className="w-full pl-9 pr-4 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
              />
            </div>

            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="py-2 px-3 border border-slate-300 rounded-lg text-sm bg-white text-slate-700"
            >
              <option value="">All Content Types</option>
              <option value="ARTICLE">Article</option>
              <option value="VIDEO">Video</option>
              <option value="GUIDE">Guide</option>
              <option value="FAQ">FAQ</option>
              <option value="TAX_UPDATE">Tax Update</option>
            </select>

            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="py-2 px-3 border border-slate-300 rounded-lg text-sm bg-white text-slate-700"
            >
              <option value="">All Statuses</option>
              <option value="DRAFT">Draft</option>
              <option value="SUBMITTED">Submitted</option>
              <option value="IN_REVIEW">In Review</option>
              <option value="APPROVED">Approved</option>
              <option value="SCHEDULED">Scheduled</option>
              <option value="PUBLISHED">Published</option>
              <option value="REJECTED">Rejected</option>
              <option value="ARCHIVED">Archived</option>
            </select>

            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              className="py-2 px-3 border border-slate-300 rounded-lg text-sm bg-white text-slate-700"
            >
              <option value="">All Categories</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>

            <Button variant="secondary" size="sm" onClick={() => loadContent()}>
              Apply Filter
            </Button>
          </div>

          {/* Data Table */}
          <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-600">
                <thead className="bg-slate-50 text-slate-700 font-semibold border-b border-slate-200 text-xs uppercase tracking-wider">
                  <tr>
                    <th className="py-3 px-4">Content</th>
                    <th className="py-3 px-4">Category & Tax Service</th>
                    <th className="py-3 px-4">Status & Version</th>
                    <th className="py-3 px-4">Author</th>
                    <th className="py-3 px-4">Updated</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {isLoading ? (
                    <tr>
                      <td colSpan={6} className="py-12 text-center text-slate-400">
                        Loading content items...
                      </td>
                    </tr>
                  ) : contentList.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="py-12 text-center text-slate-400">
                        No content found matching filter criteria.
                      </td>
                    </tr>
                  ) : (
                    contentList.map((item) => (
                      <tr key={item.id} className="hover:bg-slate-50 transition-colors">
                        {/* Title & Type */}
                        <td className="py-3 px-4">
                          <div className="flex items-start gap-3 max-w-sm">
                            <div className="mt-1 shrink-0">{renderTypeIcon(item.contentType)}</div>
                            <div className="min-w-0">
                              <div className="font-semibold text-slate-900 truncate">{item.title}</div>
                              <div className="text-xs text-slate-400 truncate">/{item.slug}</div>
                              {item.rejectionReason && (
                                <div className="mt-1 text-xs text-rose-600 flex items-center gap-1 font-medium">
                                  <AlertCircle className="w-3 h-3" /> Rejection: {item.rejectionReason}
                                </div>
                              )}
                              {item.scheduledPublishAt && (
                                <div className="mt-1 text-xs text-indigo-600 flex items-center gap-1 font-medium">
                                  <Calendar className="w-3 h-3" /> Scheduled:{' '}
                                  {new Date(item.scheduledPublishAt).toLocaleString()}
                                </div>
                              )}
                            </div>
                          </div>
                        </td>

                        {/* Category & Tax Service */}
                        <td className="py-3 px-4">
                          <div className="space-y-1">
                            <span className="inline-block px-2 py-0.5 text-xs rounded bg-slate-100 text-slate-700 font-medium">
                              {item.categoryName || 'Unassigned'}
                            </span>
                            {item.taxServices && item.taxServices.length > 0 ? (
                              <div className="flex flex-wrap gap-1">
                                {item.taxServices.map((s) => (
                                  <span
                                    key={s.id}
                                    className="px-1.5 py-0.5 text-[11px] rounded bg-purple-50 text-purple-700 border border-purple-200 font-medium"
                                  >
                                    {s.name}
                                  </span>
                                ))}
                              </div>
                            ) : item.taxServiceName ? (
                              <div className="text-xs text-purple-700 font-medium">{item.taxServiceName}</div>
                            ) : (
                              <div className="text-xs text-slate-400 italic">No tax service attached</div>
                            )}
                          </div>
                        </td>

                        {/* Status & Version */}
                        <td className="py-3 px-4">
                          <div className="flex flex-col gap-1 items-start">
                            {renderStatusBadge(item.status)}
                            <button
                              onClick={() => handleOpenVersionHistory(item)}
                              className="text-[11px] text-slate-400 hover:text-teal-700 flex items-center gap-0.5"
                            >
                              <History className="w-3 h-3" /> v{item.versionNumber || 1}
                            </button>
                          </div>
                        </td>

                        {/* Author */}
                        <td className="py-3 px-4 text-xs">
                          <div className="font-medium text-slate-800">{item.authorName || 'Admin'}</div>
                          {item.reviewerName && (
                            <div className="text-slate-400 text-[11px]">Reviewed by: {item.reviewerName}</div>
                          )}
                        </td>

                        {/* Updated */}
                        <td className="py-3 px-4 text-xs text-slate-500">
                          {new Date(item.updatedAt).toLocaleDateString()}
                        </td>

                        {/* Actions */}
                        <td className="py-3 px-4 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            <button
                              onClick={() => handleOpenPreview(item.id)}
                              className="p-1.5 text-slate-500 hover:text-teal-700 hover:bg-slate-100 rounded"
                              title="Live Preview"
                            >
                              <Eye className="w-4 h-4" />
                            </button>

                            <button
                              onClick={() => handleOpenEdit(item)}
                              className="p-1.5 text-slate-500 hover:text-blue-700 hover:bg-slate-100 rounded"
                              title="Edit Content"
                            >
                              <Edit className="w-4 h-4" />
                            </button>

                            {/* Status transitions */}
                            {(item.status === 'DRAFT' || item.status === 'REJECTED') && (
                              <button
                                onClick={() => handleSubmitReview(item.id)}
                                className="p-1.5 text-amber-600 hover:text-amber-800 hover:bg-amber-50 rounded"
                                title="Submit for Review"
                              >
                                <Send className="w-4 h-4" />
                              </button>
                            )}

                            {(item.status === 'SUBMITTED' || item.status === 'IN_REVIEW' || item.status === 'UNDER_REVIEW') && (
                              <>
                                <button
                                  onClick={() => handleApprove(item.id)}
                                  className="p-1.5 text-emerald-600 hover:text-emerald-800 hover:bg-emerald-50 rounded"
                                  title="Approve Content"
                                >
                                  <CheckCircle2 className="w-4 h-4" />
                                </button>
                                <button
                                  onClick={() => handleOpenRejectModal(item.id)}
                                  className="p-1.5 text-rose-600 hover:text-rose-800 hover:bg-rose-50 rounded"
                                  title="Reject with Reason"
                                >
                                  <AlertCircle className="w-4 h-4" />
                                </button>
                              </>
                            )}

                            {item.status === 'APPROVED' && (
                              <>
                                <button
                                  onClick={() => handleOpenScheduleModal(item.id)}
                                  className="p-1.5 text-indigo-600 hover:text-indigo-800 hover:bg-indigo-50 rounded"
                                  title="Schedule Publication"
                                >
                                  <Calendar className="w-4 h-4" />
                                </button>
                                <button
                                  onClick={() => handlePublish(item.id)}
                                  className="p-1.5 text-emerald-600 hover:text-emerald-800 hover:bg-emerald-50 rounded"
                                  title="Publish Now"
                                >
                                  <Sparkles className="w-4 h-4" />
                                </button>
                              </>
                            )}

                            {item.status === 'PUBLISHED' && (
                              <button
                                onClick={() => handleArchive(item.id)}
                                className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-slate-100 rounded"
                                title="Archive Content"
                              >
                                <Archive className="w-4 h-4" />
                              </button>
                            )}

                            {item.status === 'ARCHIVED' && (
                              <button
                                onClick={() => handleRestore(item.id)}
                                className="p-1.5 text-teal-600 hover:text-teal-800 hover:bg-teal-50 rounded"
                                title="Restore to Draft"
                              >
                                <RotateCcw className="w-4 h-4" />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            <div className="p-4 border-t border-slate-200 flex items-center justify-between text-xs text-slate-500">
              <div>
                Showing {contentList.length} of {totalElements} items
              </div>
              <div className="flex items-center gap-2">
                <Button
                  size="sm"
                  variant="outline"
                  disabled={page === 0}
                  onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                >
                  Previous
                </Button>
                <span>
                  Page {page + 1} of {totalPages || 1}
                </span>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={page + 1 >= totalPages}
                  onClick={() => setPage((prev) => prev + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 3: REVIEW QUEUE */}
      {/* ========================================================================= */}
      {activeTab === 'review-queue' && (
        <div className="space-y-4">
          <div className="bg-amber-50/60 border border-amber-200 p-4 rounded-xl flex items-start gap-3">
            <ShieldCheck className="w-5 h-5 text-amber-700 shrink-0 mt-0.5" />
            <div className="text-sm text-amber-900">
              <span className="font-semibold">Review & Quality Assurance Queue:</span> Inspect submitted compliance
              articles and video materials for regulatory accuracy before approving for publication.
            </div>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 shadow-xs divide-y divide-slate-100">
            {isReviewQueueLoading ? (
              <div className="p-12 text-center text-slate-400">Loading review queue...</div>
            ) : reviewQueue.length === 0 ? (
              <div className="p-12 text-center text-slate-500">
                <CheckCircle2 className="w-10 h-10 text-emerald-500 mx-auto mb-2 opacity-80" />
                <h4 className="font-semibold text-slate-800">Review Queue is Empty</h4>
                <p className="text-xs text-slate-400 mt-1">No articles or videos are currently waiting for review.</p>
              </div>
            ) : (
              reviewQueue.map((item) => (
                <div key={item.id} className="p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div className="space-y-1 min-w-0 max-w-2xl">
                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 text-xs font-semibold rounded bg-slate-100 text-slate-700 flex items-center gap-1">
                        {renderTypeIcon(item.contentType)} {item.contentType}
                      </span>
                      {renderStatusBadge(item.status)}
                      <span className="text-xs text-slate-400">by {item.authorName || 'Author'}</span>
                    </div>
                    <h4 className="font-bold text-slate-900 text-base">{item.title}</h4>
                    {item.summary && <p className="text-xs text-slate-600 line-clamp-2">{item.summary}</p>}
                    {item.taxServices && item.taxServices.length > 0 && (
                      <div className="flex items-center gap-1.5 pt-1">
                        <span className="text-[11px] text-slate-400">Attached Services:</span>
                        {item.taxServices.map((s) => (
                          <span
                            key={s.id}
                            className="px-2 py-0.5 text-[11px] rounded bg-purple-50 text-purple-700 border border-purple-200 font-medium"
                          >
                            {s.name}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <Button size="sm" variant="outline" onClick={() => handleOpenPreview(item.id)}>
                      <Eye className="w-4 h-4 mr-1.5" /> Live Preview
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      className="border-rose-200 text-rose-700 hover:bg-rose-50"
                      onClick={() => handleOpenRejectModal(item.id)}
                    >
                      <AlertCircle className="w-4 h-4 mr-1.5 text-rose-600" /> Reject
                    </Button>
                    <Button
                      size="sm"
                      variant="primary"
                      className="bg-emerald-600 hover:bg-emerald-700 text-white"
                      onClick={() => handleApprove(item.id)}
                    >
                      <CheckCircle2 className="w-4 h-4 mr-1.5" /> Approve
                    </Button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 4: MEDIA LIBRARY */}
      {/* ========================================================================= */}
      {activeTab === 'media' && (
        <div className="space-y-4">
          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs flex items-center justify-between gap-4">
            <div className="relative flex-1 max-w-md">
              <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
              <input
                type="text"
                placeholder="Search images by filename or alt text..."
                value={mediaSearch}
                onChange={(e) => setMediaSearch(e.target.value)}
                className="w-full pl-9 pr-4 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
              />
            </div>
            <Button
              variant="primary"
              size="sm"
              onClick={() => setIsMediaUploadOpen(true)}
              className="gap-1.5 bg-teal-600 hover:bg-teal-700 text-white"
            >
              <Upload className="w-4 h-4" /> Upload Image
            </Button>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
            {isMediaLoading ? (
              <div className="col-span-full py-16 text-center text-slate-400">Loading media library...</div>
            ) : mediaAssets.length === 0 ? (
              <div className="col-span-full py-16 text-center text-slate-500">
                <Palette className="w-10 h-10 text-slate-300 mx-auto mb-2" />
                <h4 className="font-semibold text-slate-800">Media Library is Empty</h4>
                <p className="text-xs text-slate-400 mt-1">Upload images, thumbnails, and banners for your articles.</p>
              </div>
            ) : (
              mediaAssets.map((asset) => (
                <div
                  key={asset.id}
                  className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs group hover:shadow-md transition-shadow flex flex-col"
                >
                  <div className="h-32 bg-slate-100 relative overflow-hidden flex items-center justify-center">
                    <img src={asset.publicUrl} alt={asset.altText || asset.filename} className="w-full h-full object-cover" />
                  </div>
                  <div className="p-3 flex-1 flex flex-col justify-between">
                    <div>
                      <div className="font-semibold text-slate-900 text-xs truncate" title={asset.filename}>
                        {asset.filename}
                      </div>
                      <div className="text-[11px] text-slate-400 mt-0.5 truncate">{asset.altText || 'No alt text'}</div>
                      <div className="text-[10px] text-slate-400 mt-1">
                        {(asset.fileSize / 1024).toFixed(1)} KB • {asset.contentType.split('/')[1]?.toUpperCase()}
                      </div>
                    </div>
                    <div className="pt-3 flex items-center justify-between border-t border-slate-100 mt-2">
                      <button
                        onClick={() => copyToClipboard(asset.publicUrl)}
                        className="text-xs text-teal-600 hover:text-teal-800 font-semibold flex items-center gap-1"
                        title="Copy Public URL"
                      >
                        <Copy className="w-3.5 h-3.5" /> URL
                      </button>
                      <button
                        onClick={() => handleDeleteMedia(asset.id)}
                        className="text-slate-400 hover:text-rose-600 p-1 rounded"
                        title="Delete asset"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 5: CATEGORIES */}
      {/* ========================================================================= */}
      {activeTab === 'categories' && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {categories.map((cat) => (
            <div key={cat.id} className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs text-teal-700 bg-teal-50 px-2 py-0.5 rounded font-semibold">
                  {cat.code}
                </span>
                <span className="text-xs text-slate-500 font-medium">
                  {cat.publishedContentCount || 0} published items
                </span>
              </div>
              <h4 className="font-bold text-slate-900 text-base">{cat.name}</h4>
              <p className="text-xs text-slate-600 leading-relaxed">{cat.description}</p>
            </div>
          ))}
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 6: TAX SERVICES (MASTER REFERENCE) */}
      {/* ========================================================================= */}
      {activeTab === 'tax-services' && (
        <div className="space-y-4">
          <div className="bg-purple-50/60 border border-purple-200 p-4 rounded-xl flex items-start gap-3">
            <Store className="w-5 h-5 text-purple-700 shrink-0 mt-0.5" />
            <div className="text-sm text-purple-900">
              <span className="font-semibold">Controlled Tax Service Master Reference:</span> Below are the verified,
              marketplace-enabled tax services. All educational content must attach strictly to these master services to
              maintain consistent customer matching and CRM attribution.
            </div>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-slate-700 font-semibold border-b border-slate-200 text-xs uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Service Code</th>
                  <th className="py-3 px-4">Service Name</th>
                  <th className="py-3 px-4">Category</th>
                  <th className="py-3 px-4">Description</th>
                  <th className="py-3 px-4 text-center">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {masterTaxServices.map((svc) => (
                  <tr key={svc.id} className="hover:bg-slate-50">
                    <td className="py-3 px-4 font-mono text-xs text-slate-700 font-semibold">{svc.code}</td>
                    <td className="py-3 px-4 font-semibold text-slate-900">{svc.name}</td>
                    <td className="py-3 px-4 text-xs text-slate-600">{svc.categoryName || svc.category}</td>
                    <td className="py-3 px-4 text-xs text-slate-500 max-w-xs truncate">{svc.description}</td>
                    <td className="py-3 px-4 text-center">
                      <span className="inline-block px-2 py-0.5 text-xs rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 font-semibold">
                        ACTIVE
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL: CREATE / EDIT CONTENT */}
      {/* ========================================================================= */}
      {isEditorOpen && (
        <Modal
          isOpen={isEditorOpen}
          onClose={() => setIsEditorOpen(false)}
          title={editingContentId ? 'Edit Content Item' : `Create New ${formType}`}
          maxWidth="2xl"
        >
          <form onSubmit={handleSave} className="space-y-4">
            {actionError && (
              <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0" />
                {actionError}
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Content Type */}
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Content Type</label>
                <select
                  value={formType}
                  onChange={(e) => setFormType(e.target.value as LearnContentType)}
                  className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm bg-white"
                >
                  <option value="ARTICLE">Article (Educational)</option>
                  <option value="VIDEO">Video (YouTube)</option>
                  <option value="GUIDE">Step-by-step Guide</option>
                  <option value="FAQ">Frequently Asked Question</option>
                  <option value="TAX_UPDATE">Regulatory Tax Update</option>
                </select>
              </div>

              {/* Category */}
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Compliance Category</label>
                <select
                  value={formCategoryId}
                  onChange={(e) => setFormCategoryId(e.target.value)}
                  className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm bg-white"
                >
                  <option value="">Select Category...</option>
                  {categories.map((cat) => (
                    <option key={cat.id} value={cat.id}>
                      {cat.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Title */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Title *</label>
              <input
                type="text"
                value={formTitle}
                onChange={(e) => setFormTitle(e.target.value)}
                placeholder="e.g., Complete Guide to GST Return Filing for E-Commerce Sellers"
                className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                required
              />
            </div>

            {/* Slug */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase mb-1">
                URL Slug <span className="text-slate-400 font-normal">(Optional - auto-generated if empty)</span>
              </label>
              <input
                type="text"
                value={formSlug}
                onChange={(e) => setFormSlug(e.target.value)}
                placeholder="complete-guide-to-gst-return-filing"
                className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
              />
            </div>

            {/* YouTube Video Specific Fields */}
            {formType === 'VIDEO' && (
              <div className="p-4 bg-rose-50/50 rounded-xl border border-rose-200 space-y-3">
                <div>
                  <label className="block text-xs font-bold text-rose-900 uppercase mb-1">YouTube Video Link *</label>
                  <input
                    type="text"
                    value={formYoutubeUrl}
                    onChange={(e) => setFormYoutubeUrl(e.target.value)}
                    placeholder="https://www.youtube.com/watch?v=..."
                    className="w-full py-2 px-3 border border-rose-300 rounded-lg text-sm bg-white"
                    required={formType === 'VIDEO'}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-rose-900 uppercase mb-1">Duration (Seconds)</label>
                  <input
                    type="number"
                    value={formDurationSeconds}
                    onChange={(e) => setFormDurationSeconds(e.target.value)}
                    placeholder="300"
                    className="w-full py-2 px-3 border border-rose-300 rounded-lg text-sm bg-white"
                  />
                </div>
              </div>
            )}

            {/* Summary */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Summary / Excerpt</label>
              <textarea
                value={formSummary}
                onChange={(e) => setFormSummary(e.target.value)}
                rows={2}
                placeholder="Brief summary shown on customer cards and search results..."
                className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
              />
            </div>

            {/* Body */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Content Body (Markdown) *</label>
              <textarea
                value={formBody}
                onChange={(e) => setFormBody(e.target.value)}
                rows={8}
                placeholder="Write your comprehensive educational article or video transcript here..."
                className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm font-mono focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                required
              />
            </div>

            {/* Images & Media Library Picker */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-bold text-slate-700 uppercase">Thumbnail URL</label>
                  <button
                    type="button"
                    onClick={() => {
                      setMediaPickerTarget('thumbnail');
                      setIsMediaPickerOpen(true);
                      loadMediaAssets();
                    }}
                    className="text-xs text-teal-600 hover:text-teal-800 font-semibold"
                  >
                    Select from Media
                  </button>
                </div>
                <input
                  type="text"
                  value={formThumbnailUrl}
                  onChange={(e) => setFormThumbnailUrl(e.target.value)}
                  placeholder="https://.../thumbnail.jpg"
                  className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm"
                />
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-bold text-slate-700 uppercase">Featured Banner Image</label>
                  <button
                    type="button"
                    onClick={() => {
                      setMediaPickerTarget('featured');
                      setIsMediaPickerOpen(true);
                      loadMediaAssets();
                    }}
                    className="text-xs text-teal-600 hover:text-teal-800 font-semibold"
                  >
                    Select from Media
                  </button>
                </div>
                <input
                  type="text"
                  value={formFeaturedImageUrl}
                  onChange={(e) => setFormFeaturedImageUrl(e.target.value)}
                  placeholder="https://.../banner.jpg"
                  className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm"
                />
              </div>
            </div>

            {/* Controlled Tax Service Multi-Selector */}
            <div className="p-4 bg-purple-50/50 rounded-xl border border-purple-200">
              <label className="block text-xs font-bold text-purple-900 uppercase mb-1">
                Link to Controlled Tax Services (Marketplace Matching & CTA)
              </label>
              <p className="text-xs text-purple-700 mb-3">
                Select one or more verified master Tax Services. Readers of this article will see direct booking CTA buttons
                linked to these services.
              </p>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 max-h-40 overflow-y-auto p-2 bg-white rounded-lg border border-purple-200">
                {masterTaxServices.map((svc) => {
                  const isChecked = formTaxServiceIds.includes(svc.id);
                  return (
                    <label
                      key={svc.id}
                      className={clsx(
                        'flex items-center gap-2 p-2 rounded text-xs cursor-pointer border transition-colors',
                        isChecked
                          ? 'bg-purple-100 border-purple-400 font-semibold text-purple-900'
                          : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
                      )}
                    >
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => toggleTaxService(svc.id)}
                        className="rounded text-purple-600 focus:ring-purple-500"
                      />
                      <span className="truncate">{svc.name}</span>
                    </label>
                  );
                })}
              </div>
            </div>

            {/* SEO Settings & Google Search Preview */}
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h4 className="text-xs font-black text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-teal-600" />
                    <span>SEO Settings & Google Search Discovery</span>
                  </h4>
                  <p className="text-[11px] text-slate-500 mt-0.5">
                    Customize how this article appears on Google and social media search results.
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="block text-xs font-bold text-slate-700 uppercase">
                      SEO Title
                    </label>
                    <span className={clsx(
                      'text-[10px] font-semibold',
                      formSeoTitle.length > 60 ? 'text-amber-600' : 'text-slate-400'
                    )}>
                      {formSeoTitle.length}/60 chars
                    </span>
                  </div>
                  <input
                    type="text"
                    value={formSeoTitle}
                    onChange={(e) => setFormSeoTitle(e.target.value)}
                    placeholder="Leave empty to use main title"
                    className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm bg-white"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">
                    Canonical URL Override
                  </label>
                  <input
                    type="text"
                    value={formCanonicalUrl}
                    onChange={(e) => setFormCanonicalUrl(e.target.value)}
                    placeholder="https://taxoryn.com/learn/your-slug"
                    className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm bg-white"
                  />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-bold text-slate-700 uppercase">
                    Meta Description
                  </label>
                  <span className={clsx(
                    'text-[10px] font-semibold',
                    formMetaDescription.length > 160 ? 'text-amber-600' : 'text-slate-400'
                  )}>
                    {formMetaDescription.length}/160 chars
                  </span>
                </div>
                <textarea
                  value={formMetaDescription}
                  onChange={(e) => setFormMetaDescription(e.target.value)}
                  rows={2}
                  placeholder="Accurate, concise 1-2 sentence description for search engines..."
                  className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm bg-white"
                />
              </div>

              {/* Google Search Result Simulation Snippet */}
              <div className="p-3 bg-white rounded-lg border border-slate-200 shadow-sm space-y-1">
                <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                  Google Search Snippet Preview:
                </div>
                <div className="text-xs text-emerald-800 font-medium truncate">
                  taxoryn.com &rsaquo; learn &rsaquo; {formSlug || 'sample-tax-guide'}
                </div>
                <div className="text-sm font-semibold text-blue-700 hover:underline cursor-pointer truncate">
                  {formSeoTitle || formTitle || 'Tax Guide Title'} | Taxoryn Learn
                </div>
                <div className="text-xs text-slate-600 line-clamp-2 leading-relaxed">
                  {formMetaDescription || formSummary || (formBody ? formBody.substring(0, 140) + '...' : 'Explore practical tax guidelines, return filing deadlines, and expert advice on Taxoryn Learn.')}
                </div>
              </div>

              {/* SEO Readiness Checklist */}
              <div className="pt-2 border-t border-slate-200 flex flex-wrap items-center gap-2 text-[11px]">
                <span className="font-bold text-slate-500">SEO Readiness:</span>
                <span className={clsx(
                  'px-2 py-0.5 rounded font-semibold flex items-center gap-1',
                  formTitle.trim().length > 5 ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                )}>
                  {formTitle.trim().length > 5 ? '✓' : '⚠'} Title
                </span>
                <span className={clsx(
                  'px-2 py-0.5 rounded font-semibold flex items-center gap-1',
                  formMetaDescription.trim().length > 0 || formSummary.trim().length > 0
                    ? 'bg-emerald-100 text-emerald-800'
                    : 'bg-amber-100 text-amber-800'
                )}>
                  {formMetaDescription.trim().length > 0 || formSummary.trim().length > 0 ? '✓' : '⚠'} Description
                </span>
                <span className={clsx(
                  'px-2 py-0.5 rounded font-semibold flex items-center gap-1',
                  formCategoryId ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                )}>
                  {formCategoryId ? '✓' : '⚠'} Category
                </span>
                <span className={clsx(
                  'px-2 py-0.5 rounded font-semibold flex items-center gap-1',
                  formTaxServiceIds.length > 0 ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'
                )}>
                  {formTaxServiceIds.length > 0 ? '✓' : '○'} Tax Service Link
                </span>
                <span className={clsx(
                  'px-2 py-0.5 rounded font-semibold flex items-center gap-1',
                  formThumbnailUrl || formFeaturedImageUrl ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'
                )}>
                  {formThumbnailUrl || formFeaturedImageUrl ? '✓' : '○'} Media Asset
                </span>
              </div>
            </div>

            {/* Tags */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase mb-1">
                Tags <span className="text-slate-400 font-normal">(Comma-separated)</span>
              </label>
              <input
                type="text"
                value={formTags}
                onChange={(e) => setFormTags(e.target.value)}
                placeholder="GST, GSTR-1, Tax Returns, Small Business"
                className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm"
              />
            </div>

            <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-3">
              <Button variant="outline" type="button" onClick={() => setIsEditorOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                type="submit"
                disabled={isSubmitting}
                className="bg-teal-600 hover:bg-teal-700 text-white"
              >
                {isSubmitting ? 'Saving...' : editingContentId ? 'Update Content' : 'Save Draft'}
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {/* ========================================================================= */}
      {/* MODAL: LIVE CUSTOMER PREVIEW */}
      {/* ========================================================================= */}
      {isPreviewOpen && previewContent && (
        <Modal
          isOpen={isPreviewOpen}
          onClose={() => setIsPreviewOpen(false)}
          title="Customer-Fidelity Live Preview"
          maxWidth="2xl"
        >
          <div className="space-y-6">
            <div className="flex items-center justify-between pb-3 border-b border-slate-200">
              <div className="flex items-center gap-2">
                <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-teal-50 text-teal-800 border border-teal-200">
                  {previewContent.contentType}
                </span>
                {renderStatusBadge(previewContent.status)}
              </div>
              <div className="text-xs text-slate-400">/{previewContent.slug}</div>
            </div>

            {/* Video Player if Video */}
            {previewContent.contentType === 'VIDEO' && previewContent.youtubeVideoId && (
              <div className="rounded-xl overflow-hidden shadow-md">
                <YouTubePlayer
                  videoId={previewContent.youtubeVideoId}
                  title={previewContent.title}
                />
              </div>
            )}

            {/* Featured Image if Article */}
            {previewContent.contentType !== 'VIDEO' && previewContent.featuredImageUrl && (
              <div className="rounded-xl overflow-hidden h-64 bg-slate-100">
                <img
                  src={previewContent.featuredImageUrl}
                  alt={previewContent.altText || previewContent.title}
                  className="w-full h-full object-cover"
                />
              </div>
            )}

            <div>
              <h1 className="text-2xl font-black text-slate-900 mb-2">{previewContent.title}</h1>
              {previewContent.summary && (
                <p className="text-sm text-slate-600 font-medium leading-relaxed mb-4">{previewContent.summary}</p>
              )}
              <div className="prose prose-slate max-w-none text-sm leading-relaxed whitespace-pre-wrap">
                {previewContent.body}
              </div>
            </div>

            {/* Marketplace CTA Banner in Preview */}
            {previewContent.taxServices && previewContent.taxServices.length > 0 && (
              <div className="p-5 rounded-xl bg-gradient-to-r from-purple-900 to-indigo-900 text-white flex items-center justify-between gap-4 shadow-md">
                <div>
                  <div className="text-xs font-bold uppercase tracking-wider text-purple-300 mb-0.5">
                    Need Professional Tax Assistance?
                  </div>
                  <div className="text-base font-bold">
                    Book a verified CA/CS for {previewContent.taxServices[0]?.name}
                  </div>
                </div>
                <Button size="sm" className="bg-white text-purple-900 hover:bg-purple-50 font-bold shrink-0">
                  Find Professionals
                </Button>
              </div>
            )}

            <div className="pt-4 border-t border-slate-200 flex justify-end">
              <Button variant="outline" onClick={() => setIsPreviewOpen(false)}>
                Close Preview
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* ========================================================================= */}
      {/* MODAL: REJECT WITH REASON */}
      {/* ========================================================================= */}
      {isRejectModalOpen && (
        <Modal
          isOpen={isRejectModalOpen}
          onClose={() => setIsRejectModalOpen(false)}
          title="Reject Content with Feedback"
          maxWidth="md"
        >
          <div className="space-y-4">
            <p className="text-xs text-slate-600">
              Please provide clear feedback explaining why this content is being rejected so the author can make the
              necessary corrections.
            </p>
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Rejection Reason *</label>
              <textarea
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                rows={4}
                placeholder="e.g. Please update the GST tax slab rates to reflect the latest FY 2026-27 statutory notifications."
                className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-rose-500 focus:border-rose-500"
                required
              />
            </div>
            <div className="flex items-center justify-end gap-3 pt-2">
              <Button variant="outline" onClick={() => setIsRejectModalOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleConfirmReject}
                className="bg-rose-600 hover:bg-rose-700 text-white"
              >
                Confirm Rejection
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* ========================================================================= */}
      {/* MODAL: SCHEDULE PUBLICATION */}
      {/* ========================================================================= */}
      {isScheduleModalOpen && (
        <Modal
          isOpen={isScheduleModalOpen}
          onClose={() => setIsScheduleModalOpen(false)}
          title="Schedule Content Publication"
          maxWidth="md"
        >
          <div className="space-y-4">
            <p className="text-xs text-slate-600">
              Choose a future date and time for automatic publication to the Taxoryn Learn portal.
            </p>
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Publication Date & Time *</label>
              <input
                type="datetime-local"
                value={scheduleDateTime}
                onChange={(e) => setScheduleDateTime(e.target.value)}
                className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm"
                required
              />
            </div>
            <div className="flex items-center justify-end gap-3 pt-2">
              <Button variant="outline" onClick={() => setIsScheduleModalOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleConfirmSchedule}
                className="bg-indigo-600 hover:bg-indigo-700 text-white"
              >
                Schedule Publication
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* ========================================================================= */}
      {/* MODAL: VERSION HISTORY */}
      {/* ========================================================================= */}
      {isVersionModalOpen && (
        <Modal
          isOpen={isVersionModalOpen}
          onClose={() => setIsVersionModalOpen(false)}
          title={`Version History — ${versionContentTitle}`}
          maxWidth="lg"
        >
          <div className="space-y-4">
            {isVersionLoading ? (
              <div className="p-8 text-center text-slate-400">Loading version history...</div>
            ) : versionHistory.length === 0 ? (
              <div className="p-8 text-center text-slate-500 text-sm">
                No previous snapshots found. Current version is the original draft.
              </div>
            ) : (
              <div className="space-y-3">
                {versionHistory.map((ver) => (
                  <div key={ver.id} className="p-4 rounded-xl border border-slate-200 bg-slate-50 space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="px-2 py-0.5 rounded text-xs font-bold bg-teal-100 text-teal-800">
                        Version {ver.versionNumber}
                      </span>
                      <span className="text-xs text-slate-400">
                        {new Date(ver.createdAt).toLocaleString()} by {ver.createdBy || 'Admin'}
                      </span>
                    </div>
                    <h5 className="font-semibold text-slate-900 text-sm mt-1">{ver.title}</h5>
                    {ver.changeSummary && <p className="text-xs text-slate-600 italic">{ver.changeSummary}</p>}
                  </div>
                ))}
              </div>
            )}
            <div className="flex justify-end pt-2">
              <Button variant="outline" onClick={() => setIsVersionModalOpen(false)}>
                Close
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* ========================================================================= */}
      {/* MODAL: MEDIA UPLOAD */}
      {/* ========================================================================= */}
      {isMediaUploadOpen && (
        <Modal isOpen={isMediaUploadOpen} onClose={() => setIsMediaUploadOpen(false)} title="Upload Media Image" maxWidth="md">
          <form onSubmit={handleUploadMedia} className="space-y-4">
            {mediaUploadError && (
              <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs">
                {mediaUploadError}
              </div>
            )}

            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Image File (Max 5MB) *</label>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif,image/svg+xml"
                onChange={(e) => setUploadFile(e.target.files ? e.target.files[0] : null)}
                className="w-full text-xs text-slate-600 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-teal-50 file:text-teal-700 hover:file:bg-teal-100"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Alt Text (Accessibility)</label>
              <input
                type="text"
                value={uploadAltText}
                onChange={(e) => setUploadAltText(e.target.value)}
                placeholder="e.g. GST Return Filing Portal Screenshot"
                className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm"
              />
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <Button variant="outline" type="button" onClick={() => setIsMediaUploadOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                type="submit"
                disabled={isUploadingMedia}
                className="bg-teal-600 hover:bg-teal-700 text-white"
              >
                {isUploadingMedia ? 'Uploading...' : 'Upload Image'}
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {/* ========================================================================= */}
      {/* MODAL: MEDIA PICKER FOR EDITOR */}
      {/* ========================================================================= */}
      {isMediaPickerOpen && (
        <Modal
          isOpen={isMediaPickerOpen}
          onClose={() => setIsMediaPickerOpen(false)}
          title={`Select ${mediaPickerTarget === 'thumbnail' ? 'Thumbnail' : 'Featured Banner'} Image`}
          maxWidth="xl"
        >
          <div className="space-y-4">
            <div className="grid grid-cols-3 sm:grid-cols-4 gap-3 max-h-96 overflow-y-auto p-1">
              {mediaAssets.map((asset) => (
                <div
                  key={asset.id}
                  onClick={() => handleSelectMediaForEditor(asset)}
                  className="bg-slate-50 border border-slate-200 rounded-lg overflow-hidden cursor-pointer hover:border-teal-500 hover:shadow-md transition-all group"
                >
                  <div className="h-24 bg-slate-200 overflow-hidden">
                    <img src={asset.publicUrl} alt={asset.altText || asset.filename} className="w-full h-full object-cover" />
                  </div>
                  <div className="p-2">
                    <div className="text-xs font-medium text-slate-800 truncate">{asset.filename}</div>
                  </div>
                </div>
              ))}
            </div>
            <div className="flex justify-end pt-2">
              <Button variant="outline" onClick={() => setIsMediaPickerOpen(false)}>
                Cancel
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
