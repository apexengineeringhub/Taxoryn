package com.taxoryn.module.content.dto;

import com.taxoryn.module.content.entity.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicSitemapItemDto {

    private String loc;
    private Instant lastmod;
    private String changefreq;
    private Double priority;
    private ContentType contentType;
}
