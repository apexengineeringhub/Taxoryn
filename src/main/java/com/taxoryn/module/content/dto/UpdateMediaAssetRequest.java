package com.taxoryn.module.content.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMediaAssetRequest {

    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    private String altText;
}
