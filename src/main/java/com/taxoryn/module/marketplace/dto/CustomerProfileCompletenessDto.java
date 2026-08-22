package com.taxoryn.module.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileCompletenessDto {

    private int percentage;

    private List<String> completedItems;

    private List<String> missingItems;
}
