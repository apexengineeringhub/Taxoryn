import React, { useEffect } from 'react';

export interface SeoHeadProps {
  title: string;
  description?: string;
  canonicalUrl?: string;
  robots?: 'index, follow' | 'noindex, nofollow' | 'noindex, follow';
  ogImage?: string;
  ogType?: 'website' | 'article' | 'video.other';
  structuredData?: (Record<string, any> | null)[] | Record<string, any>;
}

export const SeoHead: React.FC<SeoHeadProps> = ({
  title,
  description = 'Taxoryn — Modern Multi-Tenant Tax Practice Management & Verified Professional Marketplace.',
  canonicalUrl,
  robots = 'index, follow',
  ogImage = 'https://taxoryn.com/taxoryn-og-banner.png',
  ogType = 'website',
  structuredData,
}) => {
  useEffect(() => {
    // 1. Page Title (Clean, human-readable, safe fallback without null/undefined)
    const originalTitle = document.title;
    const safeTitle = (title && typeof title === 'string' && title.trim().length > 0 && title !== 'undefined' && title !== 'null')
      ? title.trim()
      : 'Tax Practice Management & Verified Marketplace';
    const finalTitle = safeTitle.toLowerCase().includes('taxoryn') ? safeTitle : `${safeTitle} | Taxoryn`;
    document.title = finalTitle;

    // Helper to get or create a tag
    const setMetaTag = (attrName: string, attrValue: string, content: string) => {
      let element = document.querySelector(`meta[${attrName}="${attrValue}"]`) as HTMLMetaElement | null;
      if (!element) {
        element = document.createElement('meta');
        element.setAttribute(attrName, attrValue);
        document.head.appendChild(element);
      }
      element.setAttribute('content', content);
    };

    // 2. Standard Metadata (Accurate, human-readable, safe fallback)
    const safeDescription = (description && typeof description === 'string' && description.trim().length > 0 && description !== 'undefined' && description !== 'null')
      ? description.trim()
      : 'Taxoryn — Modern Multi-Tenant Tax Practice Management & Verified Professional Marketplace.';
    setMetaTag('name', 'description', safeDescription);
    setMetaTag('name', 'robots', robots);

    // 3. Canonical Link (Clean preferred public URL without query parameters or hash)
    let rawCanonical = canonicalUrl;
    if (!rawCanonical && typeof window !== 'undefined') {
      rawCanonical = window.location.origin + window.location.pathname;
    }
    const cleanCanonical = (rawCanonical || 'https://taxoryn.com').split('?')[0].split('#')[0];

    let canonicalLink = document.querySelector('link[rel="canonical"]') as HTMLLinkElement | null;
    if (!canonicalLink) {
      canonicalLink = document.createElement('link');
      canonicalLink.setAttribute('rel', 'canonical');
      document.head.appendChild(canonicalLink);
    }
    canonicalLink.setAttribute('href', cleanCanonical);

    // 4. Open Graph Tags
    setMetaTag('property', 'og:title', finalTitle);
    setMetaTag('property', 'og:description', safeDescription);
    setMetaTag('property', 'og:url', cleanCanonical);
    setMetaTag('property', 'og:type', ogType);
    setMetaTag('property', 'og:site_name', 'Taxoryn');
    if (ogImage) {
      setMetaTag('property', 'og:image', ogImage);
    }

    // 5. Twitter Card Tags
    setMetaTag('name', 'twitter:card', 'summary_large_image');
    setMetaTag('name', 'twitter:title', finalTitle);
    setMetaTag('name', 'twitter:description', safeDescription);
    if (ogImage) {
      setMetaTag('name', 'twitter:image', ogImage);
    }

    // 6. JSON-LD Structured Data
    const scriptId = 'taxoryn-seo-structured-data';
    let scriptElement = document.getElementById(scriptId) as HTMLScriptElement | null;

    if (structuredData) {
      const validSchemas = Array.isArray(structuredData)
        ? structuredData.filter(Boolean)
        : [structuredData];

      if (validSchemas.length > 0) {
        if (!scriptElement) {
          scriptElement = document.createElement('script');
          scriptElement.id = scriptId;
          scriptElement.type = 'application/ld+json';
          document.head.appendChild(scriptElement);
        }
        scriptElement.text = JSON.stringify(validSchemas.length === 1 ? validSchemas[0] : validSchemas);
      } else if (scriptElement) {
        scriptElement.remove();
      }
    } else if (scriptElement) {
      scriptElement.remove();
    }

    return () => {
      // Revert title on unmount if needed
      document.title = originalTitle || 'Taxoryn — Modern Tax Practice Management';
    };
  }, [title, description, canonicalUrl, robots, ogImage, ogType, structuredData]);

  return null; // Side-effect only component
};
