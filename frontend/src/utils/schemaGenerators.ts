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

/**
 * Generates Schema.org JSON-LD structured data for Tax Practices and Professional Services.
 */
export function generatePracticeProfileSchema(profile: any, canonicalUrl: string) {
  const primaryLoc = profile.primaryLocation || (profile.locations && profile.locations.length > 0 ? profile.locations[0] : null);
  
  return {
    '@context': 'https://schema.org',
    '@type': 'ProfessionalService',
    name: profile.displayName,
    description: profile.metaDescription || profile.description || profile.headline || `${profile.displayName} - Verified Tax Practice`,
    url: canonicalUrl,
    image: profile.avatarUrl || profile.bannerUrl ? [profile.avatarUrl || profile.bannerUrl] : undefined,
    telephone: profile.phone || undefined,
    email: profile.email || undefined,
    priceRange: profile.startingFee ? `₹${profile.startingFee}+` : '₹₹',
    address: primaryLoc ? {
      '@type': 'PostalAddress',
      streetAddress: [primaryLoc.addressLine1, primaryLoc.addressLine2, primaryLoc.landmark].filter(Boolean).join(', ') || undefined,
      addressLocality: primaryLoc.city,
      addressRegion: primaryLoc.state,
      postalCode: primaryLoc.pincode,
      addressCountry: primaryLoc.countryCode || 'IN',
    } : (profile.city ? {
      '@type': 'PostalAddress',
      addressLocality: profile.city,
      addressRegion: profile.state,
      postalCode: profile.pincode,
      addressCountry: 'IN',
    } : undefined),
    geo: primaryLoc && primaryLoc.latitude && primaryLoc.longitude ? {
      '@type': 'GeoCoordinates',
      latitude: primaryLoc.latitude,
      longitude: primaryLoc.longitude,
    } : undefined,
    aggregateRating: profile.totalReviews > 0 && profile.averageRating ? {
      '@type': 'AggregateRating',
      ratingValue: Number(profile.averageRating).toFixed(1),
      reviewCount: profile.totalReviews,
      bestRating: '5',
      worstRating: '1',
    } : undefined,
  };
}
