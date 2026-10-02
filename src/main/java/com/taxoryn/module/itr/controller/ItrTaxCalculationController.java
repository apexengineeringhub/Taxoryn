package com.taxoryn.module.itr.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.itr.tax.TaxCalculationRequest;
import com.taxoryn.module.itr.tax.TaxCalculationResponse;
import com.taxoryn.module.itr.tax.TaxCalculationService;
import com.taxoryn.module.itr.tax.TaxRegimeComparisonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/itr/calculation")
@RequiredArgsConstructor
public class ItrTaxCalculationController {
    private final TaxCalculationService taxCalculationService;

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<TaxCalculationResponse>> calculate(@Valid @RequestBody TaxCalculationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Estimated tax calculated", taxCalculationService.calculateTax(request)));
    }

    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<TaxRegimeComparisonResponse>> compare(@Valid @RequestBody TaxCalculationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tax regimes calculated", taxCalculationService.compare(request)));
    }
}
