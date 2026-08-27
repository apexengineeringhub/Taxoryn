package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.EnquiryStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnquiryTimelineItemDto {
    private EnquiryStatus status;
    private String title;
    private String description;
    private Instant timestamp;
    private boolean completed;
    private boolean current;
}
