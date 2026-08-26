package com.taxoryn.module.content.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.content.entity.ContentStatus;
import com.taxoryn.module.content.entity.ContentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ContentFilterRequest extends PageRequestDto {

    private ContentType contentType;
    private ContentStatus status;
    private UUID categoryId;
    private UUID taxServiceId;
    private UUID authorId;
    private UUID reviewerId;
    private String tag;
    private String search;
}
