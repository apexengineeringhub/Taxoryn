package com.taxoryn.module.employee.service;

import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.OrganizationEmployeeCounterEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.employee.repository.OrganizationEmployeeCounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationEmployeeNumberGenerator {

    private static final Pattern NUMERIC_SUFFIX_PATTERN = Pattern.compile("^EMP-0*(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final OrganizationEmployeeCounterRepository counterRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Initializes organization employee counter with last_number = 0 if not yet present.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void initializeCounterIfAbsent(UUID organizationId) {
        if (organizationId != null && !counterRepository.existsById(organizationId)) {
            long maxExisting = determineMaxExistingNumericSuffix(organizationId);
            counterRepository.save(OrganizationEmployeeCounterEntity.builder()
                    .organizationId(organizationId)
                    .lastNumber(maxExisting)
                    .updatedAt(Instant.now())
                    .build());
        }
    }

    /**
     * Concurrency-safe, organization-scoped sequential employee code generator.
     * Guaranteed to return a unique, sequential code formatted as EMP-0001, EMP-0002, etc.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized String generateNextEmployeeCode(UUID organizationId) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID must not be null when generating employee code");
        }

        Optional<OrganizationEmployeeCounterEntity> counterOpt = counterRepository.findByOrganizationIdForUpdate(organizationId);
        long nextNumber;
        OrganizationEmployeeCounterEntity counter;

        if (counterOpt.isPresent()) {
            counter = counterOpt.get();
            nextNumber = counter.getLastNumber() + 1;
        } else {
            long maxExisting = determineMaxExistingNumericSuffix(organizationId);
            nextNumber = maxExisting + 1;
            counter = OrganizationEmployeeCounterEntity.builder()
                    .organizationId(organizationId)
                    .lastNumber(nextNumber)
                    .updatedAt(Instant.now())
                    .build();
        }

        // Loop to guard against any manual/imported codes that might collide
        String candidateCode = formatCode(nextNumber);
        while (employeeRepository.existsByOrganizationIdAndEmployeeCode(organizationId, candidateCode)) {
            nextNumber++;
            candidateCode = formatCode(nextNumber);
        }

        counter.setLastNumber(nextNumber);
        counter.setUpdatedAt(Instant.now());
        counterRepository.save(counter);

        log.debug("Generated organization-scoped employee code {} for tenant {}", candidateCode, organizationId);
        return candidateCode;
    }

    /**
     * Generates a batch of sequential employee codes for bulk onboarding.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized List<String> generateBatchEmployeeCodes(UUID organizationId, int count) {
        if (organizationId == null || count <= 0) {
            return List.of();
        }

        Optional<OrganizationEmployeeCounterEntity> counterOpt = counterRepository.findByOrganizationIdForUpdate(organizationId);
        long currentNumber;
        OrganizationEmployeeCounterEntity counter;

        if (counterOpt.isPresent()) {
            counter = counterOpt.get();
            currentNumber = counter.getLastNumber();
        } else {
            currentNumber = determineMaxExistingNumericSuffix(organizationId);
            counter = OrganizationEmployeeCounterEntity.builder()
                    .organizationId(organizationId)
                    .lastNumber(currentNumber)
                    .updatedAt(Instant.now())
                    .build();
        }

        List<String> generatedCodes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            currentNumber++;
            String candidateCode = formatCode(currentNumber);
            while (employeeRepository.existsByOrganizationIdAndEmployeeCode(organizationId, candidateCode)) {
                currentNumber++;
                candidateCode = formatCode(currentNumber);
            }
            generatedCodes.add(candidateCode);
        }

        counter.setLastNumber(currentNumber);
        counter.setUpdatedAt(Instant.now());
        counterRepository.save(counter);

        log.debug("Generated batch of {} employee codes for tenant {}: {}", count, organizationId, generatedCodes);
        return generatedCodes;
    }

    /**
     * Formats integer counter into standardized EMP-0001 format.
     */
    public static String formatCode(long number) {
        if (number <= 0) {
            number = 1;
        }
        return String.format("EMP-%04d", number);
    }

    private long determineMaxExistingNumericSuffix(UUID organizationId) {
        List<EmployeeEntity> existing = employeeRepository.findAllByOrganizationId(organizationId);
        long max = 0L;
        for (EmployeeEntity emp : existing) {
            String code = emp.getEmployeeCode();
            if (code != null) {
                Matcher matcher = NUMERIC_SUFFIX_PATTERN.matcher(code.trim());
                if (matcher.matches()) {
                    try {
                        long num = Long.parseLong(matcher.group(1));
                        if (num > max) {
                            max = num;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return Math.max(max, existing.size());
    }
}
