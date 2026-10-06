package com.taxoryn.module.compliance.work.port;

import com.taxoryn.module.compliance.work.dto.ComplianceWorkGenerationRequest;
import com.taxoryn.module.compliance.work.dto.ComplianceWorkGenerationResultDto;

/**
 * Integration boundary port — defined in compliance, implemented in the work module.
 *
 * <p>The compliance module MUST NOT directly inject work-module repositories or internal services.
 * All work generation crosses this boundary. The Spring bean implementing this port
 * ({@code ComplianceWorkGenerationServiceImpl}) is defined in the work module and injected
 * by Spring's standard DI at runtime.
 *
 * <p>Design invariants:
 * <ul>
 *   <li>Compliance orchestrates via this port — it does NOT know about WorkInstanceRepository.</li>
 *   <li>The work module knows nothing about compliance rule evaluation.</li>
 *   <li>Generation is idempotent — calling twice returns ALREADY_EXISTS on the second call.</li>
 * </ul>
 */
public interface ComplianceWorkGenerationPort {

    /**
     * Generates (or retrieves) a work instance for the given compliance obligation.
     *
     * @param request  All data needed to generate a work instance.
     * @return         Deterministic result with status and workInstanceId.
     */
    ComplianceWorkGenerationResultDto generateWork(ComplianceWorkGenerationRequest request);
}
