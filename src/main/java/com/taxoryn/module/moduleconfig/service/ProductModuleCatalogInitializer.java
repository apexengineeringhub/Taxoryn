package com.taxoryn.module.moduleconfig.service;

import com.taxoryn.module.moduleconfig.entity.ProductModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCategory;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.ProductModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ProductModuleCatalogInitializer implements ApplicationRunner {

    private final ProductModuleRepository productModuleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        syncCatalog();
    }

    @Transactional
    public void syncCatalog() {
        int order = 1;
        for (ProductModuleCode code : ProductModuleCode.values()) {
            ProductModuleCategory defaultCategory = code.getDefaultCategory();
            boolean mandatory = code.isMandatory();
            boolean configurable = code.isConfigurable();
            boolean subscriptionControlled = code.isSubscriptionControlled();
            boolean usageControlled = code.isUsageControlled();
            String displayName = code.getDisplayName();

            Optional<ProductModuleEntity> existingOpt = productModuleRepository.findByCode(code);
            if (existingOpt.isEmpty()) {
                ProductModuleEntity entity = ProductModuleEntity.builder()
                        .code(code)
                        .name(displayName)
                        .description(displayName)
                        .category(defaultCategory)
                        .status("ACTIVE")
                        .enabledByDefault(true)
                        .displayOrder(order++)
                        .mandatory(mandatory)
                        .configurable(configurable)
                        .subscriptionControlled(subscriptionControlled)
                        .usageControlled(usageControlled)
                        .build();
                productModuleRepository.save(entity);
                log.debug("Seeded product module: {}", code);
            } else {
                ProductModuleEntity entity = existingOpt.get();
                boolean changed = false;
                if (entity.getCategory() != defaultCategory) {
                    entity.setCategory(defaultCategory);
                    changed = true;
                }
                if (entity.isMandatory() != mandatory) {
                    entity.setMandatory(mandatory);
                    changed = true;
                }
                if (entity.isConfigurable() != configurable) {
                    entity.setConfigurable(configurable);
                    changed = true;
                }
                if (entity.isSubscriptionControlled() != subscriptionControlled) {
                    entity.setSubscriptionControlled(subscriptionControlled);
                    changed = true;
                }
                if (changed) {
                    productModuleRepository.save(entity);
                    log.debug("Updated catalog metadata for product module: {}", code);
                }
            }
        }
    }
}
