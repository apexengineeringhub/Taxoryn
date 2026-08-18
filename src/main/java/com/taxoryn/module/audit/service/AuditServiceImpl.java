package com.taxoryn.module.audit.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.dto.AuditLogDto;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getAuditLogs(PageRequestDto pageRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Page<AuditLogEntity> page = auditLogRepository.findAllByOrganizationId(organizationId, pageRequest.toPageable());
        return PagedResponse.of(page, this::toDto);
    }

    @Override
    @Transactional
    public void recordAudit(UUID organizationId, UUID userId, String action, String entityName, String entityId, String oldValue, String newValue, String ipAddress, String userAgent) {
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .organizationId(organizationId)
                .userId(userId)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(auditLog);
    }

    private AuditLogDto toDto(AuditLogEntity entity) {
        return AuditLogDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .userId(entity.getUserId())
                .action(entity.getAction())
                .entityName(entity.getEntityName())
                .entityId(entity.getEntityId())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
