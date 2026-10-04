package com.taxoryn.module.gst.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request to continue or complete an interactive GST authorization challenge.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to continue or complete an interactive GST authorization challenge")
public class GstAuthContinueRequest {

    @Schema(description = "Safe non-secret action reference or correlation identifier returned from previous step")
    private String actionReference;

    @Schema(description = "Optional execution options or mock directives")
    private Map<String, Object> options;
}
