import { LearnContentDetail, LearnContentType } from '../types';

export interface BreadcrumbItem {
  name: string;
  url: string;
}

/**
 * Converts seconds into ISO 8601 Duration format (e.g., PT5M30S).
 */
export function formatIsoDuration(seconds?: number): string | undefined {
  if (!seconds || seconds <= 0) return undefined;
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `PT${mins > 0 ? `${mins}M` : ''}${secs > 0 ? `${secs}S` : ''}`;
}

/**
 * Generates Schema.org JSON-LD structured data for Articles and Step-by-Step Guides.
 */
export function generateArticleSchema(content: LearnContentDetail, canonicalUrl: string) {
  const isGuide = content.contentType === 'GUIDE';
  return {
    '@context': 'https://schema.org',
    '@type': isGuide ? 'HowTo' : 'Article',
    headline: content.seoTitle || content.title,
    description: content.metaDescription || content.summary || content.title,
    image: content.featuredImageUrl || content.thumbnailUrl ? [content.featuredImageUrl || content.thumbnailUrl] : undefined,
    datePublished: content.publishedAt || content.createdAt,
    dateModified: content.updatedAt || content.publishedAt || content.createdAt,
    author: {
      '@type': content.authorName ? 'Person' : 'Organization',
      name: content.authorName || 'Taxoryn Editorial Team',
    },
    publisher: {
      '@type': 'Organization',
      name: 'Taxoryn',
      url: 'https://taxoryn.com',
      logo: {
        '@type': 'ImageObject',
        url: 'https://taxoryn.com/taxoryn-logo.png',
      },
    },
    mainEntityOfPage: {
      '@type': 'WebPage',
      '@id': canonicalUrl,
    },
  };
}

/**
 * Generates Schema.org JSON-LD structured data for YouTube-backed educational videos.
 */
export function generateVideoSchema(content: LearnContentDetail, canonicalUrl: string) {
  if (!content.youtubeVideoId) return null;

  return {
    '@context': 'https://schema.org',
    '@type': 'VideoObject',
    name: content.seoTitle || content.title,
    description: content.metaDescription || content.summary || content.title,
    thumbnailUrl: content.thumbnailUrl ? [content.thumbnailUrl] : undefined,
    uploadDate: content.publishedAt || content.createdAt,
    duration: formatIsoDuration(content.videoDurationSeconds),
    embedUrl: content.youtubeEmbedUrl || `https://www.youtube.com/embed/${content.youtubeVideoId}`,
    contentUrl: content.youtubeWatchUrl || `https://www.youtube.com/watch?v=${content.youtubeVideoId}`,
    publisher: {
      '@type': 'Organization',
      name: 'Taxoryn',
      url: 'https://taxoryn.com',
      logo: {
        '@type': 'ImageObject',
        url: 'https://taxoryn.com/taxoryn-logo.png',
      },
    },
  };
}

/**
 * Generates Schema.org JSON-LD structured data for FAQ pages.
 */
export function generateFaqSchema(content: LearnContentDetail) {
  if (content.contentType !== 'FAQ') return null;

  return {
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    mainEntity: [
      {
        '@type': 'Question',
        name: content.title,
        acceptedAnswer: {
          '@type': 'Answer',
          text: content.summary || content.body,
        },
      },
    ],
  };
}

/**
 * Generates Schema.org JSON-LD structured data for Breadcrumbs.
 */
export function generateBreadcrumbSchema(items: BreadcrumbItem[]) {
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: items.map((item, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      name: item.name,
      item: item.url.startsWith('http') ? item.url : `https://taxoryn.com${item.url}`,
    })),
  };
}
