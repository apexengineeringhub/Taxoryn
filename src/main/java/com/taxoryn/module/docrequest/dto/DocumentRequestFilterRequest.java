package com.taxoryn.module.docrequest.dto;

import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document Request Query Filters")
public class DocumentRequestFilterRequest {

    private UUID clientId;
    private RequestStatus status;
    private String search;
    private Integer page;
    private Integer size;
}