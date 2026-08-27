package com.taxoryn.module.content.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.content.dto.PublicSitemapItemDto;
import com.taxoryn.module.content.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Taxoryn SEO & Crawling Endpoints", description = "Public, search-engine endpoints for robots.txt and sitemap.xml")
public class PublicSeoController {

    private final ContentService contentService;

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Get Robots.txt", description = "Provides crawler instructions allowing Taxoryn Learn and Marketplace discovery while disallowing internal admin and private customer APIs.")
    public ResponseEntity<String> getRobotsTxt() {
        String body = contentService.getRobotsTxtContent();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(body);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "Get XML Sitemap", description = "Generates standard XML sitemap containing only eligible published platform topics with lastmod and changefreq metadata.")
    public ResponseEntity<String> getSitemapXml() {
        String xml = contentService.generateSitemapXml();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/xml;charset=UTF-8")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(xml);
    }

    @GetMapping("/api/v1/public/seo/sitemap")
    @Operation(summary = "Get Sitemap Structured Entries JSON", description = "Structured JSON listing of sitemap items for client-side routing & head generation.")
    public ResponseEntity<ApiResponse<List<PublicSitemapItemDto>>> getSitemapJson() {
        List<PublicSitemapItemDto> items = contentService.getPublicSitemapItems();
        return ResponseEntity.ok(ApiResponse.success(items));
    }
}
