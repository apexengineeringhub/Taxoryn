package com.taxoryn.module.service.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.marketplace.entity.TaxServiceCategoryEntity;
import com.taxoryn.module.marketplace.entity.TaxServiceEntity;
import com.taxoryn.module.marketplace.repository.TaxServiceRepository;
import com.taxoryn.module.service.entity.PracticeServicePricingEntity;
import com.taxoryn.module.service.model.ServicePricingMode;
import com.taxoryn.module.service.repository.PracticeServicePricingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicePricingServiceTest {
    @Mock private TaxServiceRepository serviceRepository;
    @Mock private PracticeServicePricingRepository pricingRepository;
    @Mock private AuditService auditService;

    private ServicePricingService service;
    private UUID serviceId;
    private UUID practiceA;
    private UUID practiceB;
    private TaxServiceEntity catalogService;

    @BeforeEach
    void setUp() {
        service = new ServicePricingService(serviceRepository, pricingRepository, auditService);
        serviceId = UUID.randomUUID();
        practiceA = UUID.randomUUID();
        practiceB = UUID.randomUUID();
        catalogService = TaxServiceEntity.builder()
                .code("ITR_1")
                .name("ITR-1 Filing")
                .isActive(true)
                .suggestedPrice(new BigDecimal("999.00"))
                .currency("INR")
                .billingType("PER_RETURN")
                .category(TaxServiceCategoryEntity.builder().code("INCOME_TAX").build())
                .build();
        catalogService.setId(serviceId);
        when(serviceRepository.findByCodeIgnoreCase("ITR_1")).thenReturn(Optional.of(catalogService));
    }

    @Test
    void defaultModeUsesTaxorynSuggestedPrice() {
        when(pricingRepository.findByOrganizationIdAndTaxServiceId(practiceA, serviceId)).thenReturn(Optional.empty());

        var price = service.getEffectiveServicePrice(practiceA, "ITR_1");

        assertThat(price.suggestedPrice()).isEqualByComparingTo("999.00");
        assertThat(price.effectivePrice()).isEqualByComparingTo("999.00");
        assertThat(price.pricingMode()).isEqualTo(ServicePricingMode.DEFAULT);
    }

    @Test
    void customPracticePriceOverridesSuggestedPrice() {
        var config = PracticeServicePricingEntity.builder()
                .pricingMode(ServicePricingMode.CUSTOM)
                .customPrice(new BigDecimal("1499.00"))
                .enabled(true)
                .build();
        when(pricingRepository.findByOrganizationIdAndTaxServiceId(practiceA, serviceId)).thenReturn(Optional.of(config));

        var price = service.getEffectiveServicePrice(practiceA, "ITR_1");

        assertThat(price.suggestedPrice()).isEqualByComparingTo("999.00");
        assertThat(price.practicePrice()).isEqualByComparingTo("1499.00");
        assertThat(price.effectivePrice()).isEqualByComparingTo("1499.00");
    }

    @Test
    void pricingLookupIsScopedToRequestedPractice() {
        var custom = PracticeServicePricingEntity.builder().pricingMode(ServicePricingMode.CUSTOM)
                .customPrice(new BigDecimal("1499.00")).enabled(true).build();
        when(pricingRepository.findByOrganizationIdAndTaxServiceId(practiceA, serviceId)).thenReturn(Optional.of(custom));
        when(pricingRepository.findByOrganizationIdAndTaxServiceId(practiceB, serviceId)).thenReturn(Optional.empty());

        assertThat(service.getEffectiveServicePrice(practiceA, "ITR_1").effectivePrice()).isEqualByComparingTo("1499.00");
        assertThat(service.getEffectiveServicePrice(practiceB, "ITR_1").effectivePrice()).isEqualByComparingTo("999.00");
    }

    @Test
    void disabledServiceCannotBeResolvedForBillingOrProposals() {
        var disabled = PracticeServicePricingEntity.builder().pricingMode(ServicePricingMode.DEFAULT).enabled(false).build();
        when(pricingRepository.findByOrganizationIdAndTaxServiceId(practiceA, serviceId)).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> service.getEffectiveServicePrice(practiceA, "ITR_1"))
                .isInstanceOf(BadRequestException.class);
    }
}
