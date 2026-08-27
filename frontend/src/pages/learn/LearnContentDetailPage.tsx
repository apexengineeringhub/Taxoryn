import React, { useEffect, useState, useMemo } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Calendar,
  Clock,
  User,
  Share2,
  Bookmark,
  CheckCircle2,
  Sparkles,
  BookOpen,
  ArrowRight,
  ShieldCheck,
  FileText,
  Video,
  HelpCircle,
  Bell,
  Users,
  ChevronRight,
  MessageSquare,
  AlertCircle,
  Play,
  ExternalLink,
} from 'lucide-react';
import { Button } from '../../components/common/Button';
import { LearnHeader } from '../../components/learn/LearnHeader';
import { LearnContentCard } from '../../components/learn/LearnContentCard';
import { YouTubePlayer } from '../../components/learn/YouTubePlayer';
import { SeoHead } from '../../components/common/SeoHead';
import {
  generateArticleSchema,
  generateVideoSchema,
  generateFaqSchema,
  generateBreadcrumbSchema,
} from '../../utils/schemaGenerators';
import { publicLearnApi } from '../../api/endpoints';
import { LearnContentDetail, LearnContentSummary, LearnContentType } from '../../types';
import clsx from 'clsx';

export const LearnContentDetailPage: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();

  const [content, setContent] = useState<LearnContentDetail | null>(null);
  const [relatedContent, setRelatedContent] = useState<LearnContentSummary[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [notFound, setNotFound] = useState<boolean>(false);
  const [copiedLink, setCopiedLink] = useState<boolean>(false);

  useEffect(() => {
    if (slug) {
      loadArticle(slug);
    }
  }, [slug]);

  const loadArticle = async (articleSlug: string) => {
    try {
      setIsLoading(true);
      setNotFound(false);

      const [data, related] = await Promise.all([
        publicLearnApi.getContentBySlug(articleSlug),
        publicLearnApi.getRelatedContent(articleSlug, 4).catch(() => []),
      ]);

      // 301 Permanent Redirect handling: if backend reports redirectSlug, update URL seamlessly
      if (data.redirectSlug && data.redirectSlug.toLowerCase() !== articleSlug.toLowerCase()) {
        navigate(`/learn/${data.redirectSlug}`, { replace: true });
        return;
      }

      setContent(data);
      setRelatedContent(related || []);
    } catch (err: any) {
      console.error('Failed to load learn article', err);
      setNotFound(true);
    } finally {
      setIsLoading(false);
    }
  };

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2500);
    }
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return null;
    try {
      return new Date(dateStr).toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'long',
        year: 'numeric',
      });
    } catch {
      return null;
    }
  };

  const estimateReadingTime = (text?: string) => {
    if (!text) return '3 min read';
    const words = text.trim().split(/\s+/).length;
    const minutes = Math.ceil(words / 180);
    return `${Math.max(1, minutes)} min read`;
  };

  const getTypeMetadata = (type?: LearnContentType) => {
    switch (type) {
      case 'ARTICLE':
        return { label: 'Tax Article', icon: FileText, style: 'bg-blue-50 text-blue-700 border-blue-200' };
      case 'VIDEO':
        return { label: 'Video Walkthrough', icon: Video, style: 'bg-rose-50 text-rose-700 border-rose-200' };
      case 'GUIDE':
        return { label: 'Step-by-Step Guide', icon: BookOpen, style: 'bg-purple-50 text-purple-700 border-purple-200' };
      case 'FAQ':
        return { label: 'Tax FAQ & Answer', icon: HelpCircle, style: 'bg-amber-50 text-amber-800 border-amber-200' };
      case 'TAX_UPDATE':
        return { label: 'Official Tax Update', icon: Bell, style: 'bg-emerald-50 text-emerald-800 border-emerald-200' };
      default:
        return { label: 'Educational Guide', icon: FileText, style: 'bg-slate-50 text-slate-700 border-slate-200' };
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <LearnHeader />
        <main className="max-w-4xl mx-auto px-4 py-12 w-full space-y-6 flex-1">
          <div className="h-6 w-48 bg-slate-200 rounded animate-pulse" />
          <div className="h-10 w-full bg-slate-200 rounded animate-pulse" />
          <div className="h-64 w-full bg-slate-200 rounded-2xl animate-pulse" />
          <div className="space-y-3">
            <div className="h-4 w-full bg-slate-200 rounded animate-pulse" />
            <div className="h-4 w-5/6 bg-slate-200 rounded animate-pulse" />
            <div className="h-4 w-4/6 bg-slate-200 rounded animate-pulse" />
          </div>
        </main>
      </div>
    );
  }

  if (notFound || !content) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <SeoHead
          title="Tax Guide Not Found"
          description="The requested tax guide or video could not be found."
          robots="noindex, nofollow"
        />
        <LearnHeader />
        <main className="max-w-xl mx-auto px-4 py-20 text-center space-y-6 flex-1 flex flex-col justify-center">
          <div className="w-16 h-16 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center mx-auto border border-amber-200">
            <AlertCircle className="w-8 h-8" />
          </div>
          <h1 className="text-2xl font-black text-slate-900">Tax Guide Not Found</h1>
          <p className="text-sm text-slate-500 leading-relaxed">
            The topic you are looking for may have been updated, moved, or is still under review by our editorial team.
          </p>
          <div className="flex justify-center gap-3">
            <Link to="/learn/content">
              <Button variant="primary" className="bg-brand-600 text-white font-bold text-xs rounded-xl px-5">
                Browse All Guides
              </Button>
            </Link>
            <Link to="/marketplace">
              <Button variant="secondary" className="font-bold text-xs rounded-xl">
                Find a Tax Professional
              </Button>
            </Link>
          </div>
        </main>
      </div>
    );
  }

  const meta = getTypeMetadata(content.contentType);
  const MetaIcon = meta.icon;

  const canonicalUrl = content.canonicalUrl || `https://taxoryn.com/learn/${content.slug}`;
  const seoTitle = content.seoTitle || `${content.title} | Taxoryn Learn`;
  const seoDescription = content.metaDescription || content.summary || `Read expert advice and compliance steps for ${content.title} on Taxoryn Learn.`;
  const seoImage = content.featuredImageUrl || content.thumbnailUrl || 'https://taxoryn.com/taxoryn-og-banner.png';

  const breadcrumbs = [
    { name: 'Taxoryn Learn', url: '/learn' },
    ...(content.categoryName ? [{ name: content.categoryName, url: `/learn/content?categoryId=${content.categoryId}` }] : []),
    { name: content.title, url: `/learn/${content.slug}` },
  ];

  const structuredSchemas = [
    generateBreadcrumbSchema(breadcrumbs),
    content.contentType !== 'VIDEO' && content.contentType !== 'FAQ' ? generateArticleSchema(content, canonicalUrl) : null,
    content.contentType === 'VIDEO' ? generateVideoSchema(content, canonicalUrl) : null,
    content.contentType === 'FAQ' ? generateFaqSchema(content) : null,
  ].filter(Boolean);

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col selection:bg-brand-500 selection:text-white">
      {/* Search Engine Optimization Metadata & Schema.org JSON-LD */}
      <SeoHead
        title={seoTitle}
        description={seoDescription}
        canonicalUrl={canonicalUrl}
        ogImage={seoImage}
        ogType={content.contentType === 'VIDEO' ? 'video.other' : 'article'}
        structuredData={structuredSchemas}
      />

      {/* 1. Header */}
      <LearnHeader />

      {/* 2. Main Article Content Container */}
      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 w-full space-y-10 flex-1">
        {/* Breadcrumb Navigation */}
        <nav className="flex items-center gap-2 text-xs font-semibold text-slate-400 flex-wrap" aria-label="Breadcrumb">
          <Link to="/learn" className="hover:text-brand-600 transition-colors">
            Taxoryn Learn
          </Link>
          <span>/</span>
          {content.categoryName ? (
            <>
              <Link
                to={`/learn/content?categoryId=${content.categoryId}`}
                className="hover:text-brand-600 transition-colors"
              >
                {content.categoryName}
              </Link>
              <span>/</span>
            </>
          ) : (
            <>
              <Link to="/learn/content" className="hover:text-brand-600 transition-colors">
                Guides
              </Link>
              <span>/</span>
            </>
          )}
          <span className="text-slate-700 font-bold truncate max-w-xs sm:max-w-sm">
            {content.title}
          </span>
        </nav>

        {/* Article Header Card */}
        <header className="bg-white rounded-3xl p-6 sm:p-10 border border-slate-200/90 shadow-card space-y-6">
          {/* Content Type & Reading Time */}
          <div className="flex flex-wrap items-center justify-between gap-3">
            <span
              className={clsx(
                'inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider border',
                meta.style
              )}
            >
              <MetaIcon className="w-3.5 h-3.5" />
              <span>{meta.label}</span>
            </span>

            <div className="flex items-center gap-4 text-xs text-slate-400 font-medium">
              <span className="flex items-center gap-1">
                <Clock className="w-3.5 h-3.5" />
                <span>{estimateReadingTime(content.body)}</span>
              </span>
              {content.publishedAt && (
                <span className="flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5" />
                  <span>{formatDate(content.publishedAt)}</span>
                </span>
              )}
            </div>
          </div>

          {/* Title */}
          <h1 className="text-2xl sm:text-4xl font-black text-slate-900 tracking-tight leading-snug">
            {content.title}
          </h1>

          {/* Summary Lead Box */}
          {content.summary && (
            <div className="p-4 sm:p-5 rounded-2xl bg-brand-50/60 border border-brand-100 text-slate-700 text-sm sm:text-base leading-relaxed">
              <p className="font-semibold text-brand-900 mb-1 flex items-center gap-1.5">
                <Sparkles className="w-4 h-4 text-brand-600" />
                <span>Summary in Simple Words</span>
              </p>
              <p>{content.summary}</p>
            </div>
          )}

          {/* Author & Share Bar */}
          <div className="pt-4 border-t border-slate-100 flex flex-wrap items-center justify-between gap-4 text-xs">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-600 font-bold">
                {content.authorName ? content.authorName.charAt(0) : 'T'}
              </div>
              <div>
                <div className="font-bold text-slate-800">{content.authorName || 'Taxoryn Editorial Team'}</div>
                <div className="text-[11px] text-slate-400">Reviewed for accuracy</div>
              </div>
            </div>

            <div className="flex items-center gap-2">
              {content.contentType === 'VIDEO' && content.youtubeVideoId && (
                <a
                  href={`https://www.youtube.com/watch?v=${content.youtubeVideoId}`}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <Button
                    variant="secondary"
                    size="sm"
                    className="text-xs font-bold gap-1.5 rounded-xl border-rose-200 text-rose-700 bg-rose-50 hover:bg-rose-100"
                  >
                    <ExternalLink className="w-3.5 h-3.5" />
                    <span>Watch on YouTube</span>
                  </Button>
                </a>
              )}
              <Button
                variant="secondary"
                size="sm"
                onClick={handleShare}
                className="text-xs font-bold gap-1.5 rounded-xl"
              >
                <Share2 className="w-3.5 h-3.5 text-slate-500" />
                <span>{copiedLink ? 'Link Copied!' : 'Share'}</span>
              </Button>
            </div>
          </div>
        </header>

        {/* Video Player or Thumbnail Banner */}
        {content.contentType === 'VIDEO' ? (
          <div className="space-y-3">
            <YouTubePlayer videoId={content.youtubeVideoId} title={content.title} />
          </div>
        ) : content.thumbnailUrl ? (
          <div className="rounded-3xl overflow-hidden border border-slate-200 shadow-md">
            <img
              src={content.thumbnailUrl}
              alt={content.altText || content.title}
              className="w-full h-auto max-h-[420px] object-cover"
              loading="lazy"
            />
          </div>
        ) : null}

        {/* Article Body Content */}
        <article className="bg-white rounded-3xl p-6 sm:p-10 border border-slate-200/90 shadow-card">
          {/* FAQ Special Formatting */}
          {content.contentType === 'FAQ' ? (
            <div className="space-y-6">
              <div className="p-4 rounded-2xl bg-amber-50/70 border border-amber-200 text-amber-900 font-bold text-base flex items-start gap-2.5">
                <HelpCircle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                <div>
                  <span className="text-xs uppercase tracking-wider text-amber-600 block">Question</span>
                  <span>{content.title}</span>
                </div>
              </div>
              <div className="space-y-4 text-slate-700 text-base leading-relaxed whitespace-pre-line">
                <h3 className="text-xs uppercase tracking-wider font-black text-slate-400">Direct Answer & Rules:</h3>
                <div>{content.body}</div>
              </div>
            </div>
          ) : content.contentType === 'TAX_UPDATE' ? (
            <div className="space-y-6">
              <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center justify-between gap-3 text-emerald-900">
                <div className="flex items-center gap-2">
                  <Bell className="w-5 h-5 text-emerald-600" />
                  <span className="font-bold text-sm">Official Notification Overview</span>
                </div>
                {content.publishedAt && (
                  <span className="text-xs font-bold text-emerald-700 bg-emerald-100 px-3 py-1 rounded-full">
                    Published: {formatDate(content.publishedAt)}
                  </span>
                )}
              </div>
              <div className="prose prose-slate max-w-none text-slate-800 leading-relaxed whitespace-pre-line text-sm sm:text-base">
                {content.body}
              </div>
            </div>
          ) : (
            <div className="prose prose-slate max-w-none text-slate-800 leading-relaxed whitespace-pre-line text-sm sm:text-base space-y-4">
              {content.body}
            </div>
          )}

          {/* Tags */}
          {content.tags && content.tags.length > 0 && (
            <div className="pt-8 mt-8 border-t border-slate-100 flex items-center gap-2 flex-wrap">
              <span className="text-xs font-bold text-slate-400">Related Tags:</span>
              {content.tags.map((tag) => (
                <Link
                  key={tag.id}
                  to={`/learn/content?tag=${encodeURIComponent(tag.name)}`}
                  className="px-3 py-1 rounded-full bg-slate-100 hover:bg-brand-50 hover:text-brand-700 text-slate-600 text-xs font-semibold border border-slate-200/60 transition-colors"
                >
                  #{tag.name}
                </Link>
              ))}
            </div>
          )}
        </article>

        {/* 3. "Need Help With This?" Related Tax Services Marketplace Integration */}
        {content.marketplaceCtaEnabled && content.taxServices && content.taxServices.length > 0 ? (
          content.taxServices.length === 1 ? (
            <section className="bg-gradient-to-br from-indigo-950 via-slate-900 to-brand-950 rounded-3xl p-6 sm:p-10 text-white shadow-xl space-y-4 relative overflow-hidden">
              <div className="relative z-10 max-w-2xl space-y-3">
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 text-xs font-bold border border-emerald-500/30">
                  <ShieldCheck className="w-3.5 h-3.5" />
                  <span>Verified Tax Practitioners</span>
                </div>

                <h2 className="text-2xl sm:text-3xl font-black tracking-tight">
                  Need help with {content.taxServices[0].name}?
                </h2>

                <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
                  {content.taxServices[0].description ||
                    `Connect with verified Chartered Accountants and Tax Experts specializing in ${content.taxServices[0].name} for filing, documentation, and compliance.`}
                </p>

                <div className="pt-3 flex flex-wrap items-center gap-3">
                  <Link
                    to={`/marketplace?taxServiceId=${content.taxServices[0].id}&source=TAXORYN_LEARN&contentSlug=${encodeURIComponent(content.slug)}`}
                  >
                    <Button
                      variant="primary"
                      className="bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-black text-xs sm:text-sm px-6 rounded-xl gap-2 shadow-lg shadow-emerald-950/40"
                    >
                      <span>Find a Tax Professional</span>
                      <ArrowRight className="w-4 h-4" />
                    </Button>
                  </Link>
                  <Link to="/learn/content">
                    <Button
                      variant="secondary"
                      className="bg-white/10 hover:bg-white/20 text-white border-white/20 font-bold text-xs sm:text-sm rounded-xl"
                    >
                      <span>Read More Guides</span>
                    </Button>
                  </Link>
                </div>
              </div>
            </section>
          ) : (
            <section className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 text-xs font-bold border border-emerald-200 mb-1">
                    <ShieldCheck className="w-3.5 h-3.5" />
                    <span>Verified Tax Practitioners</span>
                  </div>
                  <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
                    How can we help with this?
                  </h2>
                  <p className="text-xs text-slate-500">
                    Connect with specialized tax professionals for the services mentioned in this guide:
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {content.taxServices.map((svc) => (
                  <div
                    key={svc.id}
                    className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm hover:shadow-md transition-all flex flex-col justify-between space-y-4"
                  >
                    <div className="space-y-1.5">
                      {svc.categoryName && (
                        <span className="text-[10px] font-black uppercase tracking-wider text-brand-600 bg-brand-50 px-2 py-0.5 rounded-md border border-brand-100">
                          {svc.categoryName}
                        </span>
                      )}
                      <h3 className="text-base font-bold text-slate-900 leading-snug">
                        {svc.name}
                      </h3>
                      <p className="text-xs text-slate-500 line-clamp-2">
                        {svc.description || `Get professional assistance and compliance support for ${svc.name}.`}
                      </p>
                    </div>

                    <Link
                      to={`/marketplace?taxServiceId=${svc.id}&source=TAXORYN_LEARN&contentSlug=${encodeURIComponent(content.slug)}`}
                      className="block"
                    >
                      <Button
                        variant="primary"
                        size="sm"
                        className="w-full bg-brand-600 hover:bg-brand-700 text-white font-bold text-xs rounded-xl justify-between"
                      >
                        <span>Find Professional</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </Button>
                    </Link>
                  </div>
                ))}
              </div>
            </section>
          )
        ) : null}

        {/* 4. Related Tax Topics Section */}
        {relatedContent.length > 0 && (
          <section className="space-y-6 pt-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
                  Related Tax Topics
                </h3>
                <p className="text-xs text-slate-500">More guides in {content.categoryName || 'Tax Knowledge'}</p>
              </div>
              <Link to="/learn/content" className="text-xs font-bold text-brand-600 hover:text-brand-700 flex items-center gap-1">
                <span>View All</span>
                <ChevronRight className="w-4 h-4" />
              </Link>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {relatedContent.slice(0, 3).map((item) => (
                <LearnContentCard key={item.id} content={item} />
              ))}
            </div>
          </section>
        )}
      </main>

      {/* 5. Footer */}
      <footer className="bg-white border-t border-slate-200/80 py-8 text-xs text-slate-400 mt-12">
        <div className="max-w-7xl mx-auto px-4 text-center">
          © {new Date().getFullYear()} Taxoryn Learn • Content is for educational guidance only.
        </div>
      </footer>
    </div>
  );
};
