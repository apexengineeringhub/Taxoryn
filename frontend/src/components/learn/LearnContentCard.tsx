import React from 'react';
import { Link } from 'react-router-dom';
import {
  FileText,
  Video,
  BookOpen,
  HelpCircle,
  Bell,
  ArrowRight,
  Clock,
  Calendar,
  Sparkles,
  Play,
  Tag,
} from 'lucide-react';
import { LearnContentSummary, LearnContentType } from '../../types';
import clsx from 'clsx';

interface LearnContentCardProps {
  content: LearnContentSummary;
  className?: string;
}

export const LearnContentCard: React.FC<LearnContentCardProps> = ({ content, className }) => {
  const getTypeBadge = (type: LearnContentType) => {
    switch (type) {
      case 'ARTICLE':
        return {
          label: 'Article',
          icon: FileText,
          style: 'bg-blue-50 text-blue-700 border-blue-200/80',
          actionText: 'Read Article',
        };
      case 'VIDEO':
        return {
          label: 'Video Guide',
          icon: Video,
          style: 'bg-rose-50 text-rose-700 border-rose-200/80',
          actionText: 'Watch Video',
        };
      case 'GUIDE':
        return {
          label: 'Step-by-Step Guide',
          icon: BookOpen,
          style: 'bg-purple-50 text-purple-700 border-purple-200/80',
          actionText: 'Read Guide',
        };
      case 'FAQ':
        return {
          label: 'Tax FAQ',
          icon: HelpCircle,
          style: 'bg-amber-50 text-amber-800 border-amber-200/80',
          actionText: 'View Answer',
        };
      case 'TAX_UPDATE':
        return {
          label: 'Tax Update',
          icon: Bell,
          style: 'bg-emerald-50 text-emerald-800 border-emerald-200/80',
          actionText: 'Read Update',
        };
      default:
        return {
          label: 'Tax Topic',
          icon: FileText,
          style: 'bg-slate-50 text-slate-700 border-slate-200',
          actionText: 'Read More',
        };
    }
  };

  const badge = getTypeBadge(content.contentType);
  const Icon = badge.icon;

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return null;
    try {
      return new Date(dateStr).toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
      });
    } catch {
      return null;
    }
  };

  const formattedDate = formatDate(content.publishedAt || content.createdAt);

  return (
    <article
      className={clsx(
        'group bg-white rounded-2xl border border-slate-200/90 shadow-card hover:shadow-hover hover:border-brand-300/80 transition-all duration-300 flex flex-col justify-between overflow-hidden',
        className
      )}
    >
      <div>
        {/* Thumbnail or Visual Top */}
        <div className="relative aspect-16/9 w-full bg-gradient-to-br from-slate-100 via-slate-50 to-slate-200 overflow-hidden">
          {content.thumbnailUrl ? (
            <img
              src={content.thumbnailUrl}
              alt={content.title}
              loading="lazy"
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
            />
          ) : (
            <div className="w-full h-full flex flex-col items-center justify-center p-6 text-center">
              <div className="w-12 h-12 rounded-2xl bg-white/90 shadow-xs border border-slate-200/60 flex items-center justify-center text-brand-600 mb-2 group-hover:scale-110 transition-transform">
                <Icon className="w-6 h-6" />
              </div>
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                {content.categoryName || 'Tax Knowledge'}
              </span>
            </div>
          )}

          {/* Video Play Overlay if Video */}
          {content.contentType === 'VIDEO' && (
            <div className="absolute inset-0 bg-slate-900/30 backdrop-blur-[1px] flex items-center justify-center group-hover:bg-slate-900/40 transition-colors">
              <div className="w-12 h-12 rounded-full bg-white/95 text-rose-600 flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
                <Play className="w-5 h-5 fill-current ml-0.5" />
              </div>
            </div>
          )}

          {/* Top Floating Badge */}
          <div className="absolute top-3 left-3 flex items-center gap-1.5">
            <span
              className={clsx(
                'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider backdrop-blur-md border shadow-2xs',
                badge.style
              )}
            >
              <Icon className="w-3 h-3" />
              <span>{badge.label}</span>
            </span>
          </div>

          {/* Duration Badge for Videos */}
          {content.contentType === 'VIDEO' && content.videoDurationFormatted && (
            <div className="absolute bottom-3 right-3">
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-black bg-slate-900/80 text-white backdrop-blur-md border border-white/10 shadow-xs">
                <Clock className="w-3 h-3" />
                <span>{content.videoDurationFormatted}</span>
              </span>
            </div>
          )}
        </div>

        {/* Card Body */}
        <div className="p-5 sm:p-6 space-y-3">
          {/* Metadata Row */}
          <div className="flex items-center justify-between text-xs text-slate-400 gap-2">
            {content.categoryName && (
              <span className="font-semibold text-slate-500 truncate">
                {content.categoryName}
              </span>
            )}
            {formattedDate && (
              <span className="flex items-center gap-1 shrink-0">
                <Calendar className="w-3 h-3" />
                <span>{formattedDate}</span>
              </span>
            )}
          </div>

          {/* Title */}
          <h3 className="text-base sm:text-lg font-black text-slate-900 leading-snug group-hover:text-brand-600 transition-colors line-clamp-2">
            <Link to={`/learn/content/${content.slug}`}>
              {content.title}
            </Link>
          </h3>

          {/* Summary */}
          {content.summary && (
            <p className="text-xs sm:text-sm text-slate-500 leading-relaxed line-clamp-2">
              {content.summary}
            </p>
          )}

          {/* Associated Tax Service Badge if available */}
          {content.taxServiceName && (
            <div className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-slate-100 text-slate-600 text-[10px] font-bold">
              <Sparkles className="w-3 h-3 text-amber-500" />
              <span className="truncate">Tax Service: {content.taxServiceName}</span>
            </div>
          )}
        </div>
      </div>

      {/* Card Footer Action */}
      <div className="px-5 sm:px-6 pb-5 pt-2 border-t border-slate-100 flex items-center justify-between">
        <Link
          to={`/learn/content/${content.slug}`}
          className="inline-flex items-center gap-1.5 text-xs font-bold text-brand-600 group-hover:text-brand-700 transition-colors"
        >
          <span>{badge.actionText}</span>
          <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
        </Link>

        {content.tags && content.tags.length > 0 && (
          <span className="text-[10px] font-semibold text-slate-400 bg-slate-50 px-2 py-0.5 rounded-full border border-slate-200/50">
            #{content.tags[0].name}
          </span>
        )}
      </div>
    </article>
  );
};
