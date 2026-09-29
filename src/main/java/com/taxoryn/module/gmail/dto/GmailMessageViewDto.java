package com.taxoryn.module.gmail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Gmail Live Message View for Conversation Thread")
public class GmailMessageViewDto {

    private String id;
    private String threadId;
    private String from;
    private String to;
    private String cc;
    private String subject;
    private String snippet;
    private String bodyPlain;
    private String bodyHtml;
    private Instant date;
    @Builder.Default
    private List<String> labelIds = new ArrayList<>();
    private Boolean isDraft;
    private Boolean isStarred;
}
