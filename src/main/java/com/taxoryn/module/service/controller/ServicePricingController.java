package com.taxoryn.module.service.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.service.dto.PracticeServicePriceDto;
import com.taxoryn.module.service.dto.UpdatePracticeServicePricingRequest;
import com.taxoryn.module.service.service.ServicePricingService;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/practice/service-pricing")
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
public class ServicePricingController {
    private final ServicePricingService servicePricingService;

    @GetMapping
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PARTNER')")
    public ResponseEntity<ApiResponse<List<PracticeServicePriceDto>>> getPricing() {
        return ResponseEntity.ok(ApiResponse.success(servicePricingService.getPracticePricing()));
    }

    @PutMapping("/{serviceCode}")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PARTNER')")
    public ResponseEntity<ApiResponse<PracticeServicePriceDto>> updatePricing(@PathVariable String serviceCode,
            @Valid @RequestBody UpdatePracticeServicePricingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Practice service pricing updated", servicePricingService.updatePracticePricing(serviceCode, request)));
    }
}
