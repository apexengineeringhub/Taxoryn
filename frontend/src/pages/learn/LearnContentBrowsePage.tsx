import React, { useEffect, useState, useMemo } from 'react';
import { useSearchParams, Link, useNavigate, useLocation } from 'react-router-dom';
import {
  Search,
  Filter,
  SlidersHorizontal,
  X,
  BookOpen,
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  FileText,
  Video,
  HelpCircle,
  Bell,
  Sparkles,
  Users,
  RotateCcw,
} from 'lucide-react';
import { Button } from '../../components/common/Button';
import { LearnHeader } from '../../components/learn/LearnHeader';
import { LearnContentCard } from '../../components/learn/LearnContentCard';
import { SeoHead } from '../../components/common/SeoHead';
import { publicLearnApi } from '../../api/endpoints';
import {
  LearnContentSummary,
  LearnContentType,
  LearnPublicCategory,
  PagedResponse,
} from '../../types';
import clsx from 'clsx';

export const LearnContentBrowsePage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const location = useLocation();

  // Determine path-based content type filter
  const routeType = useMemo<LearnContentType | ''>(() => {
    if (location.pathname.includes('/learn/articles')) return 'ARTICLE';
    if (location.pathname.includes('/learn/videos')) return 'VIDEO';
    if (location.pathname.includes('/learn/guides')) return 'GUIDE';
    if (location.pathname.includes('/learn/faqs')) return 'FAQ';
    if (location.pathname.includes('/learn/tax-updates')) return 'TAX_UPDATE';
    return '';
  }, [location.pathname]);

  // URL query params state
  const currentSearch = searchParams.get('q') || '';
  const currentCategory = searchParams.get('categoryId') || '';
  const currentType = routeType || searchParams.get('contentType') || '';
  const currentPage = parseInt(searchParams.get('page') || '0', 10);

  // Component state
  const [searchInput, setSearchInput] = useState(currentSearch);
  const [categories, setCategories] = useState<LearnPublicCategory[]>([]);
  const [pagedData, setPagedData] = useState<PagedResponse<LearnContentSummary> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Sync state with URL params
  useEffect(() => {
    setSearchInput(currentSearch);
  }, [currentSearch]);

  // Load Categories on mount
  useEffect(() => {
    publicLearnApi.getCategories()
      .then(setCategories)
      .catch(() => setCategories([]));
  }, []);

  // Fetch content whenever query params change
  useEffect(() => {
    loadContent();
  }, [currentSearch, currentCategory, currentType, currentPage]);

  const loadContent = async () => {
    try {
      setIsLoading(true);
      setLoadError(null);
      const res = await publicLearnApi.getContentList({
        search: currentSearch || undefined,
        categoryId: currentCategory || undefined,
        contentType: currentType || undefined,
        page: currentPage,
        size: 9,
        sortBy: 'publishedAt',
        sortDirection: 'DESC',
      });
      setPagedData(res);
    } catch (err) {
      console.error('Failed to load learn content', err);
      setLoadError('Sorry, we could not load the tax topics at this moment.');
    } finally {
      setIsLoading(false);
    }
  };

  const updateFilters = (updates: {
    q?: string;
    categoryId?: string;
    contentType?: string;
    page?: number;
  }) => {
    const params = new URLSearchParams(searchParams);

    if (updates.q !== undefined) {
      if (updates.q) params.set('q', updates.q);
      else params.delete('q');
      params.set('page', '0');
    }

    if (updates.categoryId !== undefined) {
      if (updates.categoryId) params.set('categoryId', updates.categoryId);
      else params.delete('categoryId');
      params.set('page', '0');
    }

    if (updates.contentType !== undefined) {
      if (updates.contentType) params.set('contentType', updates.contentType);
      else params.delete('contentType');
      params.set('page', '0');
    }

    if (updates.page !== undefined) {
      params.set('page', updates.page.toString());
    }

    setSearchParams(params);
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateFilters({ q: searchInput.trim() });
  };

  const clearAllFilters = () => {
    setSearchInput('');
    setSearchParams(new URLSearchParams());
  };

  const hasActiveFilters = Boolean(currentSearch || currentCategory || currentType);

  const contentTypes: { id: string; label: string; icon: React.FC<{ className?: string }> }[] = [
    { id: '', label: 'All Formats', icon: BookOpen },
    { id: 'ARTICLE', label: 'Articles', icon: FileText },
    { id: 'GUIDE', label: 'Guides', icon: BookOpen },
    { id: 'VIDEO', label: 'Videos', icon: Video },
    { id: 'FAQ', label: 'FAQs', icon: HelpCircle },
    { id: 'TAX_UPDATE', label: 'Tax Updates', icon: Bell },
  ];

  const pageTitle = routeType === 'ARTICLE'
    ? 'Tax Articles & Advisory Guides'
    : routeType === 'VIDEO'
    ? 'Tax Video Tutorials & Walkthroughs'
    : routeType === 'GUIDE'
    ? 'Step-by-Step Filing Guides'
    : routeType === 'FAQ'
    ? 'Tax FAQs & Expert Answers'
    : routeType === 'TAX_UPDATE'
    ? 'Official Tax Updates & Circulars'
    : 'Browse All Tax Topics & Guides';

  const canonicalPath = routeType === 'ARTICLE'
    ? 'https://taxoryn.com/learn/articles'
    : routeType === 'VIDEO'
    ? 'https://taxoryn.com/learn/videos'
    : routeType === 'GUIDE'
    ? 'https://taxoryn.com/learn/guides'
    : routeType === 'FAQ'
    ? 'https://taxoryn.com/learn/faqs'
    : routeType === 'TAX_UPDATE'
    ? 'https://taxoryn.com/learn/tax-updates'
    : 'https://taxoryn.com/learn/content';

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col selection:bg-brand-500 selection:text-white">
      <SeoHead
        title={pageTitle}
        description="Search and explore verified GST, Income Tax, TDS, and business compliance guides written by tax domain professionals on Taxoryn Learn."
        canonicalUrl={canonicalPath}
      />
      {/* Header */}
      <LearnHeader initialSearch={currentSearch} onSearch={(q) => updateFilters({ q })} />

      {/* Main Container */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 w-full space-y-8 flex-1">
        {/* Breadcrumb & Title */}
        <div className="space-y-3">
          <div className="flex items-center gap-2 text-xs font-semibold text-slate-400">
            <Link to="/learn" className="hover:text-brand-600 transition-colors">
              Taxoryn Learn
            </Link>
            <span>/</span>
            <span className="text-slate-700">Browse Knowledge Base</span>
          </div>

          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
              <h1 className="text-2xl sm:text-4xl font-black text-slate-900 tracking-tight">
                Tax Guides, Articles & Updates
              </h1>
              <p className="text-xs sm:text-sm text-slate-500 mt-1">
                Explore plain-English answers to Indian tax regulations and return filing requirements.
              </p>
            </div>

            {/* Mobile Filter Trigger Button */}
            <div className="flex sm:hidden items-center gap-2">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setMobileFilterOpen(true)}
                className="w-full justify-center gap-2 font-bold text-xs"
              >
                <SlidersHorizontal className="w-4 h-4" />
                <span>Filters {hasActiveFilters && '(Active)'}</span>
              </Button>
            </div>
          </div>
        </div>

        {/* Filter Controls Bar */}
        <div className="bg-white rounded-2xl p-5 border border-slate-200/90 shadow-card space-y-5">
          {/* Top Search Input */}
          <form onSubmit={handleSearchSubmit} className="flex gap-2">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
              <input
                type="text"
                placeholder="Search topics (e.g. GST return filing, 80C deductions, TDS rates)..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="w-full pl-10 pr-10 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs sm:text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
              />
              {searchInput && (
                <button
                  type="button"
                  onClick={() => {
                    setSearchInput('');
                    updateFilters({ q: '' });
                  }}
                  className="absolute right-3 top-3 text-slate-400 hover:text-slate-600"
                  aria-label="Clear search query"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>
            <Button type="submit" variant="primary" className="bg-brand-600 text-white font-bold rounded-xl px-5 text-xs sm:text-sm">
              <span>Search</span>
            </Button>
          </form>

          {/* Content Type Filter Pills */}
          <div className="space-y-2">
            <label className="text-[11px] font-black uppercase tracking-wider text-slate-400">Content Format:</label>
            <div className="flex flex-wrap gap-2">
              {contentTypes.map((type) => {
                const Icon = type.icon;
                const isSelected = currentType === type.id;
                return (
                  <button
                    key={type.id}
                    type="button"
                    onClick={() => updateFilters({ contentType: type.id })}
                    className={clsx(
                      'inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all',
                      isSelected
                        ? 'bg-brand-600 text-white shadow-xs'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    )}
                  >
                    <Icon className="w-3.5 h-3.5" />
                    <span>{type.label}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Category Filter Pills (if categories available) */}
          {categories.length > 0 && (
            <div className="space-y-2 pt-2 border-t border-slate-100">
              <label className="text-[11px] font-black uppercase tracking-wider text-slate-400">Tax Category:</label>
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => updateFilters({ categoryId: '' })}
                  className={clsx(
                    'px-3 py-1.5 rounded-xl text-xs font-bold transition-all',
                    !currentCategory
                      ? 'bg-slate-900 text-white shadow-xs'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  )}
                >
                  All Categories
                </button>
                {categories.map((cat) => {
                  const isSelected = currentCategory === cat.id;
                  return (
                    <button
                      key={cat.id}
                      type="button"
                      onClick={() => updateFilters({ categoryId: cat.id })}
                      className={clsx(
                        'px-3 py-1.5 rounded-xl text-xs font-bold transition-all',
                        isSelected
                          ? 'bg-slate-900 text-white shadow-xs'
                          : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                      )}
                    >
                      {cat.name}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Active Filters Clear Bar */}
          {hasActiveFilters && (
            <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-xs">
              <span className="text-slate-500 font-medium">
                Showing filtered tax topics
              </span>
              <button
                type="button"
                onClick={clearAllFilters}
                className="inline-flex items-center gap-1 font-bold text-brand-600 hover:text-brand-700"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Reset All Filters</span>
              </button>
            </div>
          )}
        </div>

        {/* Content Results Section */}
        {isLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3, 4, 5, 6].map((n) => (
              <div key={n} className="bg-slate-100 rounded-2xl h-80 animate-pulse border border-slate-200" />
            ))}
          </div>
        ) : loadError ? (
          <div className="text-center py-16 bg-white rounded-3xl border border-rose-200 p-8 space-y-4 shadow-card">
            <h3 className="text-base font-bold text-slate-800">Something went wrong</h3>
            <p className="text-xs text-slate-500">{loadError}</p>
            <Button variant="secondary" onClick={loadContent} className="font-bold text-xs">
              Try Again
            </Button>
          </div>
        ) : !pagedData || pagedData.content.length === 0 ? (
          <div className="text-center py-16 bg-white rounded-3xl border border-slate-200/90 p-8 space-y-4 shadow-card max-w-lg mx-auto">
            <div className="w-14 h-14 rounded-2xl bg-brand-50 text-brand-600 flex items-center justify-center mx-auto">
              <BookOpen className="w-7 h-7" />
            </div>
            <h3 className="text-lg font-black text-slate-900">No Tax Articles Found</h3>
            <p className="text-xs sm:text-sm text-slate-500 leading-relaxed">
              We couldn't find any published topics matching your search or filters. Try a different keyword or reset filters.
            </p>
            <div className="pt-2 flex justify-center gap-3">
              <Button variant="primary" onClick={clearAllFilters} className="bg-brand-600 text-white font-bold text-xs rounded-xl px-5">
                Clear Filters
              </Button>
              <Link to="/marketplace">
                <Button variant="secondary" className="font-bold text-xs rounded-xl">
                  Ask a Tax Professional
                </Button>
              </Link>
            </div>
          </div>
        ) : (
          <div className="space-y-10">
            {/* Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {pagedData.content.map((item) => (
                <LearnContentCard key={item.id} content={item} />
              ))}
            </div>

            {/* Pagination Controls */}
            {pagedData.totalPages > 1 && (
              <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-4 border-t border-slate-200">
                <span className="text-xs font-medium text-slate-500">
                  Showing {currentPage * pagedData.pageSize + 1} to{' '}
                  {Math.min((currentPage + 1) * pagedData.pageSize, pagedData.totalElements)} of{' '}
                  {pagedData.totalElements} topics
                </span>

                <div className="flex items-center gap-2">
                  <Button
                    variant="secondary"
                    size="sm"
                    disabled={currentPage === 0}
                    onClick={() => updateFilters({ page: currentPage - 1 })}
                    className="font-bold text-xs gap-1 rounded-xl"
                  >
                    <ChevronLeft className="w-3.5 h-3.5" />
                    <span>Previous</span>
                  </Button>

                  <span className="px-3 py-1 bg-white border border-slate-200 rounded-xl text-xs font-bold text-slate-700">
                    Page {currentPage + 1} of {pagedData.totalPages}
                  </span>

                  <Button
                    variant="secondary"
                    size="sm"
                    disabled={currentPage >= pagedData.totalPages - 1}
                    onClick={() => updateFilters({ page: currentPage + 1 })}
                    className="font-bold text-xs gap-1 rounded-xl"
                  >
                    <span>Next</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </Button>
                </div>
              </div>
            )}
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-slate-200/80 py-8 text-xs text-slate-400">
        <div className="max-w-7xl mx-auto px-4 text-center">
          © {new Date().getFullYear()} Taxoryn Learn. All educational content is reviewed for informational clarity.
        </div>
      </footer>
    </div>
  );
};
