package com.taxoryn.module.gov.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload to update government connection metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGovConnectionRequest {

    private String displayName;
    private String description;
    private String environment;
    private String metadata;
}
