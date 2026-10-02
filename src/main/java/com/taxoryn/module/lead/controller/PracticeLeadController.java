package com.taxoryn.module.lead.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.lead.dto.ConvertPracticeLeadRequest;
import com.taxoryn.module.lead.dto.PracticeLeadDto;
import com.taxoryn.module.lead.dto.PracticeLeadRequest;
import com.taxoryn.module.lead.dto.PracticeLeadActivityDto;
import com.taxoryn.module.lead.dto.PracticeLeadActivityRequest;
import com.taxoryn.module.lead.entity.PracticeLeadEntity.*;
import com.taxoryn.module.lead.service.PracticeLeadService;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
public class PracticeLeadController {
    private final PracticeLeadService leadService;

    @GetMapping @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<ApiResponse<PagedResponse<PracticeLeadDto>>> list(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) LeadPriority priority,
            @RequestParam(required = false) LeadSource source,
            @RequestParam(required = false) UUID assignedEmployeeId,
            @RequestParam(required = false) String interestedServiceCode,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Leads retrieved", leadService.list(status, priority, source, assignedEmployeeId, interestedServiceCode, search, page, size)));
    }

    @PostMapping @PreAuthorize("hasAuthority('LEAD_CREATE')")
    public ResponseEntity<ApiResponse<PracticeLeadDto>> create(@Valid @RequestBody PracticeLeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Lead created", leadService.create(request)));
    }

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<ApiResponse<PracticeLeadDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Lead retrieved", leadService.get(id)));
    }

    @GetMapping("/{id}/activities") @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<ApiResponse<java.util.List<PracticeLeadActivityDto>>> activities(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Lead activity retrieved", leadService.getActivities(id)));
    }

    @PostMapping("/{id}/activities") @PreAuthorize("hasAuthority('LEAD_UPDATE')")
    public ResponseEntity<ApiResponse<PracticeLeadActivityDto>> addActivity(@PathVariable UUID id, @Valid @RequestBody PracticeLeadActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Lead activity recorded", leadService.addActivity(id, request)));
    }

    @PutMapping("/{id}") @PreAuthorize("hasAuthority('LEAD_UPDATE')")
    public ResponseEntity<ApiResponse<PracticeLeadDto>> update(@PathVariable UUID id, @Valid @RequestBody PracticeLeadRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Lead updated", leadService.update(id, request)));
    }

    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('LEAD_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        leadService.delete(id); return ResponseEntity.ok(ApiResponse.success("Lead deleted", null));
    }

    @PostMapping("/{id}/assign") @PreAuthorize("hasAuthority('LEAD_ASSIGN')")
    public ResponseEntity<ApiResponse<PracticeLeadDto>> assign(@PathVariable UUID id, @Valid @RequestBody AssignRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Lead assigned", leadService.assign(id, request.getEmployeeId())));
    }

    @PostMapping("/{id}/convert") @PreAuthorize("hasAuthority('LEAD_CONVERT')")
    public ResponseEntity<ApiResponse<PracticeLeadDto>> convert(@PathVariable UUID id, @Valid @RequestBody ConvertPracticeLeadRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Lead converted to client", leadService.convert(id, request)));
    }

    @PostMapping("/{id}/lost") @PreAuthorize("hasAuthority('LEAD_UPDATE')")
    public ResponseEntity<ApiResponse<PracticeLeadDto>> lost(@PathVariable UUID id, @RequestBody(required = false) LostRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Lead marked lost", leadService.markLost(id, request == null ? null : request.getReason())));
    }

    @Data public static class AssignRequest { @jakarta.validation.constraints.NotNull private UUID employeeId; }
    @Data public static class LostRequest { private String reason; }
}
