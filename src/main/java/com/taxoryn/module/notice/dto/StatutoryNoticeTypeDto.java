package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.NoticeDepartment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatutoryNoticeTypeDto {
    private String code;
    private NoticeDepartment department;
    private String defaultSection;
    private String label;
    private String description;
}
