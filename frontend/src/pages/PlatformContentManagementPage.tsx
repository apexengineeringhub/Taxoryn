import React, { useEffect, useState } from 'react';
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
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { WorkspacePageHeader } from '../components/layout/WorkspacePageHeader';
import { YouTubePlayer } from '../components/learn/YouTubePlayer';
import { adminLearnApi, publicLearnApi } from '../api/endpoints';
import {
  LearnContentDetail,
  LearnContentStatus,
  LearnContentSummary,
  LearnContentType,
  LearnPublicCategory,
  PagedResponse,
} from '../types';
import clsx from 'clsx';

export const PlatformContentManagementPage: React.FC = () => {
  // Filters and state
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

  // Modal states
  const [isEditorOpen, setIsEditorOpen] = useState<boolean>(false);
  const [editingContentId, setEditingContentId] = useState<string | null>(null);
  const [isPreviewOpen, setIsPreviewOpen] = useState<boolean>(false);
  const [previewContent, setPreviewContent] = useState<LearnContentDetail | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // Form State
  const [formType, setFormType] = useState<LearnContentType>('VIDEO');
  const [formTitle, setFormTitle] = useState<string>('');
  const [formSlug, setFormSlug] = useState<string>('');
  const [formSummary, setFormSummary] = useState<string>('');
  const [formBody, setFormBody] = useState<string>('');
  const [formThumbnailUrl, setFormThumbnailUrl] = useState<string>('');
  const [formYoutubeUrl, setFormYoutubeUrl] = useState<string>('');
  const [formDurationSeconds, setFormDurationSeconds] = useState<string>('300');
  const [formCategoryId, setFormCategoryId] = useState<string>('');
  const [formTags, setFormTags] = useState<string>('');

  useEffect(() => {
    loadCategories();
  }, []);

  useEffect(() => {
    loadContent();
  }, [page, typeFilter, statusFilter, categoryFilter]);

  const loadCategories = async () => {
    try {
      const cats = await publicLearnApi.getCategories();
      setCategories(cats || []);
    } catch {
      setCategories([]);
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

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    loadContent();
  };

  const openCreateModal = (defaultType: LearnContentType = 'VIDEO') => {
    setEditingContentId(null);
    setFormType(defaultType);
    setFormTitle('');
    setFormSlug('');
    setFormSummary('');
    setFormBody('');
    setFormThumbnailUrl('');
    setFormYoutubeUrl('');
    setFormDurationSeconds('300');
    setFormCategoryId(categories.length > 0 ? categories[0].id : '');
    setFormTags('');
    setActionError(null);
    setIsEditorOpen(true);
  };

  const openEditModal = async (item: LearnContentSummary) => {
    try {
      const detail = await adminLearnApi.getContentById(item.id);
      setEditingContentId(detail.id);
      setFormType(detail.contentType);
      setFormTitle(detail.title);
      setFormSlug(detail.slug);
      setFormSummary(detail.summary || '');
      setFormBody(detail.body);
      setFormThumbnailUrl(detail.thumbnailUrl || '');
      setFormYoutubeUrl(detail.youtubeWatchUrl || detail.youtubeVideoId || '');
      setFormDurationSeconds(detail.videoDurationSeconds ? detail.videoDurationSeconds.toString() : '');
      setFormCategoryId(detail.categoryId || '');
      setFormTags(detail.tags ? detail.tags.map((t) => t.name).join(', ') : '');
      setActionError(null);
      setIsEditorOpen(true);
    } catch (err: any) {
      alert('Failed to load content details for editing.');
    }
  };

  const handleSaveContent = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionError(null);
    setIsSubmitting(true);

    const tagsArray = formTags
      .split(',')
      .map((t) => t.trim())
      .filter((t) => t.length > 0);

    const durationInt = formDurationSeconds ? parseInt(formDurationSeconds, 10) : undefined;

    try {
      if (editingContentId) {
        await adminLearnApi.updateContent(editingContentId, {
          contentType: formType,
          title: formTitle,
          slug: formSlug || undefined,
          summary: formSummary || undefined,
          body: formBody,
          thumbnailUrl: formThumbnailUrl || undefined,
          youtubeUrl: formYoutubeUrl || undefined,
          videoDurationSeconds: durationInt,
          categoryId: formCategoryId || undefined,
          tags: tagsArray,
        });
      } else {
        await adminLearnApi.createContent({
          contentType: formType,
          title: formTitle,
          slug: formSlug || undefined,
          summary: formSummary || undefined,
          body: formBody,
          thumbnailUrl: formThumbnailUrl || undefined,
          youtubeUrl: formYoutubeUrl || undefined,
          videoDurationSeconds: durationInt,
          categoryId: formCategoryId || undefined,
          tags: tagsArray,
        });
      }
      setIsEditorOpen(false);
      loadContent();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to save content';
      setActionError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handlePreview = async (id: string) => {
    try {
      const data = await adminLearnApi.previewContent(id);
      setPreviewContent(data);
      setIsPreviewOpen(true);
    } catch (err: any) {
      alert('Failed to load preview');
    }
  };

  const handleLifecycleAction = async (
    id: string,
    action: 'submit' | 'approve' | 'publish' | 'archive'
  ) => {
    try {
      if (action === 'submit') await adminLearnApi.submitForReview(id);
      if (action === 'approve') await adminLearnApi.approveContent(id);
      if (action === 'publish') await adminLearnApi.publishContent(id);
      if (action === 'archive') await adminLearnApi.archiveContent(id);
      loadContent();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Lifecycle transition failed';
      alert(msg);
    }
  };

  const getStatusBadge = (status: LearnContentStatus) => {
    switch (status) {
      case 'DRAFT':
        return <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700">DRAFT</span>;
      case 'UNDER_REVIEW':
        return <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200">IN REVIEW</span>;
      case 'APPROVED':
        return <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">APPROVED</span>;
      case 'PUBLISHED':
        return <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">PUBLISHED</span>;
      case 'ARCHIVED':
        return <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-50 text-rose-700 border border-rose-200">ARCHIVED</span>;
      default:
        return null;
    }
  };

  return (
    <div className="space-y-6">
      {/* 1. Header */}
      <WorkspacePageHeader
        sectionBadge="LEARN & CONTENT"
        sectionBadgeStyle="bg-teal-100 text-teal-800 border-teal-200"
        title="Knowledge & Video Hub"
        titleIcon={BookOpen}
        titleIconColor="text-teal-600"
        description="Manage educational articles, YouTube video walkthroughs, guides, FAQs, and tax updates"
      >
        <div className="flex items-center gap-2">
          <Button
            variant="primary"
            size="sm"
            onClick={() => openCreateModal('VIDEO')}
            className="bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs gap-1.5 rounded-xl shadow-xs"
          >
            <Video className="w-3.5 h-3.5" />
            <span>+ Create Video</span>
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => openCreateModal('ARTICLE')}
            className="font-bold text-xs gap-1.5 rounded-xl"
          >
            <FileText className="w-3.5 h-3.5" />
            <span>+ Create Article</span>
          </Button>
        </div>
      </WorkspacePageHeader>

      {/* 2. Filter & Search Bar */}
      <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-card flex flex-col md:flex-row items-center justify-between gap-4">
        <form onSubmit={handleSearchSubmit} className="flex-1 w-full flex items-center gap-2">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-2.5" />
            <input
              type="text"
              placeholder="Search content by title, slug, or keywords..."
              value={searchFilter}
              onChange={(e) => setSearchFilter(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-500/20"
            />
          </div>
          <Button type="submit" variant="primary" size="sm" className="font-bold text-xs rounded-xl">
            Search
          </Button>
        </form>

        <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
          {/* Format Filter */}
          <select
            value={typeFilter}
            onChange={(e) => {
              setTypeFilter(e.target.value);
              setPage(0);
            }}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none"
          >
            <option value="">All Formats</option>
            <option value="VIDEO">Videos</option>
            <option value="ARTICLE">Articles</option>
            <option value="GUIDE">Guides</option>
            <option value="FAQ">FAQs</option>
            <option value="TAX_UPDATE">Tax Updates</option>
          </select>

          {/* Status Filter */}
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none"
          >
            <option value="">All Statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="UNDER_REVIEW">In Review</option>
            <option value="APPROVED">Approved</option>
            <option value="PUBLISHED">Published</option>
            <option value="ARCHIVED">Archived</option>
          </select>

          {/* Category Filter */}
          <select
            value={categoryFilter}
            onChange={(e) => {
              setCategoryFilter(e.target.value);
              setPage(0);
            }}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none"
          >
            <option value="">All Categories</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* 3. Content Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-400 font-bold uppercase tracking-wider text-[10px]">
              <tr>
                <th className="py-3 px-4">Content</th>
                <th className="py-3 px-4">Format</th>
                <th className="py-3 px-4">Category / Service</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Author / Date</th>
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
                  <td colSpan={6} className="py-12 text-center text-slate-400 space-y-2">
                    <BookOpen className="w-8 h-8 mx-auto text-slate-300" />
                    <div>No content found matching filters.</div>
                  </td>
                </tr>
              ) : (
                contentList.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-4 max-w-xs">
                      <div className="flex items-center gap-3">
                        {item.thumbnailUrl ? (
                          <img
                            src={item.thumbnailUrl}
                            alt=""
                            className="w-12 h-8 rounded object-cover border border-slate-200 shrink-0"
                          />
                        ) : (
                          <div className="w-12 h-8 rounded bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-400 shrink-0">
                            {item.contentType === 'VIDEO' ? <Video className="w-4 h-4 text-rose-500" /> : <FileText className="w-4 h-4 text-blue-500" />}
                          </div>
                        )}
                        <div className="min-w-0">
                          <div className="font-bold text-slate-900 truncate">{item.title}</div>
                          <div className="text-[10px] text-slate-400 truncate">/{item.slug}</div>
                        </div>
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <span className="font-semibold text-slate-600">{item.contentType}</span>
                      {item.videoDurationFormatted && (
                        <div className="text-[10px] text-slate-400">{item.videoDurationFormatted}</div>
                      )}
                    </td>
                    <td className="py-3 px-4">
                      <div className="font-medium text-slate-800">{item.categoryName || 'General'}</div>
                      {item.taxServiceName && (
                        <div className="text-[10px] text-amber-600 font-semibold truncate">{item.taxServiceName}</div>
                      )}
                    </td>
                    <td className="py-3 px-4">{getStatusBadge(item.status)}</td>
                    <td className="py-3 px-4 text-slate-500">
                      <div>{item.authorName || 'Taxoryn Admin'}</div>
                      <div className="text-[10px] text-slate-400">
                        {item.publishedAt ? new Date(item.publishedAt).toLocaleDateString() : new Date(item.createdAt).toLocaleDateString()}
                      </div>
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handlePreview(item.id)}
                          className="px-2 py-1 text-[11px] font-bold"
                          title="Preview"
                        >
                          <Eye className="w-3 h-3 text-slate-500" />
                        </Button>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => openEditModal(item)}
                          className="px-2 py-1 text-[11px] font-bold"
                          title="Edit"
                        >
                          <Edit className="w-3 h-3 text-slate-500" />
                        </Button>

                        {/* Lifecycle action buttons */}
                        {item.status === 'DRAFT' && (
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => handleLifecycleAction(item.id, 'submit')}
                            className="px-2 py-1 text-[10px] font-bold text-amber-700 bg-amber-50 hover:bg-amber-100"
                            title="Submit for Review"
                          >
                            Submit
                          </Button>
                        )}
                        {item.status === 'UNDER_REVIEW' && (
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => handleLifecycleAction(item.id, 'approve')}
                            className="px-2 py-1 text-[10px] font-bold text-blue-700 bg-blue-50 hover:bg-blue-100"
                            title="Approve"
                          >
                            Approve
                          </Button>
                        )}
                        {item.status === 'APPROVED' && (
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleLifecycleAction(item.id, 'publish')}
                            className="px-2 py-1 text-[10px] font-bold bg-emerald-600 hover:bg-emerald-700 text-white"
                            title="Publish"
                          >
                            Publish
                          </Button>
                        )}
                        {item.status === 'PUBLISHED' && (
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => handleLifecycleAction(item.id, 'archive')}
                            className="px-2 py-1 text-[10px] font-bold text-rose-700 bg-rose-50 hover:bg-rose-100"
                            title="Archive"
                          >
                            Archive
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="p-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>
              Showing {page * 10 + 1} to {Math.min((page + 1) * 10, totalElements)} of {totalElements} items
            </span>
            <div className="flex items-center gap-2">
              <Button
                variant="secondary"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
                className="text-xs font-bold"
              >
                Previous
              </Button>
              <span className="font-bold">
                {page + 1} / {totalPages}
              </span>
              <Button
                variant="secondary"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(page + 1)}
                className="text-xs font-bold"
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* ========================================================================= */}
      {/* Create / Edit Content Modal */}
      {/* ========================================================================= */}
      <Modal
        isOpen={isEditorOpen}
        onClose={() => setIsEditorOpen(false)}
        title={editingContentId ? 'Edit Content Item' : 'Create New Knowledge Content'}
        maxWidth="2xl"
      >
        <form onSubmit={handleSaveContent} className="space-y-4">
          {actionError && (
            <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 text-rose-600" />
              <span>{actionError}</span>
            </div>
          )}

          {/* Content Type Selector */}
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-700">Content Format</label>
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
              {(['VIDEO', 'ARTICLE', 'GUIDE', 'FAQ', 'TAX_UPDATE'] as LearnContentType[]).map((t) => (
                <button
                  key={t}
                  type="button"
                  onClick={() => setFormType(t)}
                  className={clsx(
                    'p-2 rounded-xl border text-xs font-bold transition-all text-center',
                    formType === t
                      ? 'bg-brand-50 border-brand-500 text-brand-700'
                      : 'bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100'
                  )}
                >
                  {t}
                </button>
              ))}
            </div>
          </div>

          {/* Title & Slug */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-xs font-bold text-slate-700">Title *</label>
              <input
                type="text"
                required
                placeholder="e.g. GST Return Filing Explained"
                value={formTitle}
                onChange={(e) => setFormTitle(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:outline-none"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-bold text-slate-700">Slug (Auto-generated if blank)</label>
              <input
                type="text"
                placeholder="e.g. gst-return-filing-explained"
                value={formSlug}
                onChange={(e) => setFormSlug(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:outline-none"
              />
            </div>
          </div>

          {/* Video Specific: YouTube URL & Duration */}
          {formType === 'VIDEO' && (
            <div className="p-4 rounded-2xl bg-rose-50/50 border border-rose-200/80 space-y-3">
              <div className="flex items-center gap-1.5 text-xs font-bold text-rose-800">
                <Video className="w-4 h-4 text-rose-600" />
                <span>YouTube Video Configuration</span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="sm:col-span-2 space-y-1">
                  <label className="text-[11px] font-bold text-slate-700">YouTube Video Link or ID *</label>
                  <input
                    type="text"
                    required={formType === 'VIDEO'}
                    placeholder="https://www.youtube.com/watch?v=abc123xyz or https://youtu.be/..."
                    value={formYoutubeUrl}
                    onChange={(e) => setFormYoutubeUrl(e.target.value)}
                    className="w-full px-3 py-2 bg-white border border-rose-200 rounded-xl text-xs text-slate-900 focus:outline-none"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[11px] font-bold text-slate-700">Duration (Seconds)</label>
                  <input
                    type="number"
                    placeholder="300"
                    value={formDurationSeconds}
                    onChange={(e) => setFormDurationSeconds(e.target.value)}
                    className="w-full px-3 py-2 bg-white border border-rose-200 rounded-xl text-xs text-slate-900 focus:outline-none"
                  />
                </div>
              </div>
            </div>
          )}

          {/* Category & Tags */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-xs font-bold text-slate-700">Tax Category</label>
              <select
                value={formCategoryId}
                onChange={(e) => setFormCategoryId(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:outline-none"
              >
                <option value="">None / General</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1">
              <label className="text-xs font-bold text-slate-700">Tags (Comma-separated)</label>
              <input
                type="text"
                placeholder="GST, Invoicing, Compliance"
                value={formTags}
                onChange={(e) => setFormTags(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:outline-none"
              />
            </div>
          </div>

          {/* Summary */}
          <div className="space-y-1">
            <label className="text-xs font-bold text-slate-700">Summary (Short plain-language description)</label>
            <textarea
              rows={2}
              placeholder="Simple explanation for taxpayers..."
              value={formSummary}
              onChange={(e) => setFormSummary(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:outline-none"
            />
          </div>

          {/* Body Content / Video Transcript / Guide Steps */}
          <div className="space-y-1">
            <label className="text-xs font-bold text-slate-700">Content Body / Video Notes *</label>
            <textarea
              rows={6}
              required
              placeholder="Detailed explanation, step-by-step points, or video key takeaways..."
              value={formBody}
              onChange={(e) => setFormBody(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:outline-none"
            />
          </div>

          {/* Footer Action Buttons */}
          <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-3">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsEditorOpen(false)}
              className="text-xs font-bold"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              disabled={isSubmitting}
              className="bg-brand-600 hover:bg-brand-700 text-white font-bold text-xs px-5 rounded-xl"
            >
              {isSubmitting ? 'Saving...' : editingContentId ? 'Update Content' : 'Save as Draft'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* ========================================================================= */}
      {/* Admin Live Preview Modal */}
      {/* ========================================================================= */}
      {previewContent && (
        <Modal
          isOpen={isPreviewOpen}
          onClose={() => setIsPreviewOpen(false)}
          title={`Admin Preview: ${previewContent.title}`}
          maxWidth="2xl"
        >
          <div className="space-y-6 max-h-[75vh] overflow-y-auto pr-2">
            <div className="flex items-center justify-between gap-3 text-xs">
              <span className="px-2.5 py-1 rounded-full font-bold bg-brand-50 text-brand-700 border border-brand-200">
                {previewContent.contentType}
              </span>
              <span className="font-semibold text-slate-400">
                Status: {previewContent.status}
              </span>
            </div>

            {previewContent.contentType === 'VIDEO' && (
              <YouTubePlayer
                videoId={previewContent.youtubeVideoId}
                title={previewContent.title}
              />
            )}

            {previewContent.summary && (
              <div className="p-4 rounded-2xl bg-brand-50/70 border border-brand-100 text-xs sm:text-sm text-slate-700">
                <div className="font-bold text-brand-900 mb-1">Summary</div>
                <div>{previewContent.summary}</div>
              </div>
            )}

            <div className="text-xs sm:text-sm text-slate-800 whitespace-pre-line leading-relaxed">
              {previewContent.body}
            </div>

            <div className="pt-4 border-t border-slate-100 flex justify-end">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setIsPreviewOpen(false)}
                className="text-xs font-bold"
              >
                Close Preview
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
