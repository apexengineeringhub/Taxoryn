package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Slot availability for a marketplace practitioner profile on a given date")
public class MarketplaceSlotAvailabilityDto {

    private UUID marketplaceProfileId;
    private LocalDate date;
    private List<String> bookedSlots;
    private List<String> availableSlots;
}
