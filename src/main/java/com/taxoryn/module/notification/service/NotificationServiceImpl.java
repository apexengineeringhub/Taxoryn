package com.taxoryn.module.notification.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.notification.dto.NotificationDto;
import com.taxoryn.module.notification.dto.NotificationFilterRequest;
import com.taxoryn.module.notification.dto.SendNotificationRequest;
import com.taxoryn.module.notification.entity.NotificationEntity;
import com.taxoryn.module.notification.entity.NotificationEntity.DeliveryStatus;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.mapper.NotificationMapper;
import com.taxoryn.module.notification.repository.NotificationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationDispatchService notificationDispatchService;

    @Override
    @Transactional
    public NotificationDto send(SendNotificationRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return notify(
                organizationId,
                request.getUserId(),
                request.getClientId(),
                request.getNotificationType(),
                request.getTitle(),
                request.getMessage(),
                request.getChannels(),
                request.getActionUrl(),
                request.getMetadata()
        );
    }

    @Override
    @Transactional
    public NotificationDto notify(UUID organizationId,
                                   UUID userId,
                                   UUID clientId,
                                   NotificationType notificationType,
                                   String title,
                                   String message,
                                   Set<NotificationChannel> channels,
                                   String actionUrl,
                                   String metadata) {

        if (userId == null && clientId == null) {
            throw new BusinessValidationException("Either userId or clientId must be provided to target a notification");
        }

        Set<NotificationChannel> resolvedChannels = (channels == null || channels.isEmpty())
                ? Set.of(NotificationChannel.IN_APP)
                : channels;

        RecipientContact recipient = resolveRecipient(organizationId, userId, clientId);

        UUID targetUserId = userId;
        if (userId != null && userRepository.findByIdAndOrganizationId(userId, organizationId).isEmpty()) {
            Optional<EmployeeEntity> empOpt = employeeRepository.findByIdAndOrganizationId(userId, organizationId);
            if (empOpt.isPresent() && empOpt.get().getUserId() != null) {
                targetUserId = empOpt.get().getUserId();
            }
        }

        NotificationEntity entity = NotificationEntity.builder()
                .organizationId(organizationId)
                .userId(targetUserId)
                .clientId(clientId)
                .notificationType(notificationType != null ? notificationType : NotificationType.GENERAL)
                .title(title)
                .message(message)
                .channels(resolvedChannels.stream().map(Enum::name).collect(Collectors.joining(",")))
                .isRead(false)
                .actionUrl(actionUrl)
                .metadata(metadata)
                .emailStatus(resolvedChannels.contains(NotificationChannel.EMAIL) ? DeliveryStatus.PENDING : DeliveryStatus.NOT_REQUESTED)
                .smsStatus(resolvedChannels.contains(NotificationChannel.SMS) ? DeliveryStatus.PENDING : DeliveryStatus.NOT_REQUESTED)
                .whatsappStatus(resolvedChannels.contains(NotificationChannel.WHATSAPP) ? DeliveryStatus.PENDING : DeliveryStatus.NOT_REQUESTED)
                .build();

        NotificationEntity saved = notificationRepository.save(entity);
        log.info("Recorded in-app notification: id={}, type={}, org={}, userId={}, clientId={}, channels={}",
                saved.getId(), saved.getNotificationType(), organizationId, userId, clientId, saved.getChannels());

        // Fan out to any additional requested channels asynchronously; in-app delivery is
        // already satisfied by the persisted row above.
        if (resolvedChannels.contains(NotificationChannel.EMAIL)) {
            notificationDispatchService.dispatchEmail(saved.getId(), recipient.email(), recipient.name(), title, message);
        }
        if (resolvedChannels.contains(NotificationChannel.SMS)) {
            notificationDispatchService.dispatchSms(saved.getId(), recipient.phone(), message);
        }
        if (resolvedChannels.contains(NotificationChannel.WHATSAPP)) {
            notificationDispatchService.dispatchWhatsApp(saved.getId(), recipient.phone(), message);
        }

        return notificationMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationDto> getNotifications(NotificationFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        RecipientTarget target = resolveCurrentTarget(organizationId);

        Specification<NotificationEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (target.userId() != null) {
                predicates.add(cb.equal(root.get("userId"), target.userId()));
            } else {
                predicates.add(cb.equal(root.get("clientId"), target.clientId()));
            }

            if (filterRequest.getIsRead() != null) {
                predicates.add(cb.equal(root.get("isRead"), filterRequest.getIsRead()));
            }

            if (filterRequest.getNotificationType() != null) {
                predicates.add(cb.equal(root.get("notificationType"), filterRequest.getNotificationType()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<NotificationEntity> page = notificationRepository.findAll(spec, filterRequest.toPageable());
        return PagedResponse.of(page, notificationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        RecipientTarget target = resolveCurrentTarget(organizationId);

        return target.userId() != null
                ? notificationRepository.countByOrganizationIdAndUserIdAndIsReadFalse(organizationId, target.userId())
                : notificationRepository.countByOrganizationIdAndClientIdAndIsReadFalse(organizationId, target.clientId());
    }

    @Override
    @Transactional
    public NotificationDto markAsRead(UUID notificationId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        NotificationEntity notification = notificationRepository.findByIdAndOrganizationId(notificationId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        assertOwnership(notification);

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }
        return notificationMapper.toDto(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        RecipientTarget target = resolveCurrentTarget(organizationId);
        Instant now = Instant.now();

        int updated = target.userId() != null
                ? notificationRepository.markAllAsReadForUser(organizationId, target.userId(), now)
                : notificationRepository.markAllAsReadForClient(organizationId, target.clientId(), now);

        log.info("Marked {} notifications as read for org={}, userId={}, clientId={}", updated, organizationId, target.userId(), target.clientId());
        return updated;
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        NotificationEntity notification = notificationRepository.findByIdAndOrganizationId(notificationId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        assertOwnership(notification);
        notificationRepository.delete(notification);
    }

    private void assertOwnership(NotificationEntity notification) {
        if (SecurityUtils.isClientPortalUser()) {
            UUID currentClientId = SecurityUtils.requireCurrentClientId();
            if (!Objects.equals(notification.getClientId(), currentClientId)) {
                throw new ForbiddenException("You do not have access to this notification");
            }
        } else {
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            if (!Objects.equals(notification.getUserId(), currentUserId)) {
                throw new ForbiddenException("You do not have access to this notification");
            }
        }
    }

    private RecipientTarget resolveCurrentTarget(UUID organizationId) {
        if (SecurityUtils.isClientPortalUser()) {
            return new RecipientTarget(null, SecurityUtils.requireCurrentClientId());
        }
        return new RecipientTarget(SecurityUtils.getCurrentUserId(), null);
    }

    /**
     * Resolves best-effort contact details for a recipient. {@code userId} may reference either
     * a firm {@code UserEntity} or, since several modules assign work at the employee level, an
     * {@code EmployeeEntity} id - both are attempted. {@code clientId} resolves against the
     * client record's registered email/phone.
     */
    private RecipientContact resolveRecipient(UUID organizationId, UUID userId, UUID clientId) {
        if (userId != null) {
            Optional<UserEntity> userOpt = userRepository.findByIdAndOrganizationId(userId, organizationId);
            if (userOpt.isPresent()) {
                UserEntity user = userOpt.get();
                String name = joinName(user.getFirstName(), user.getLastName());
                return new RecipientContact(name, user.getEmail(), user.getPhone());
            }

            Optional<EmployeeEntity> employeeOpt = employeeRepository.findByIdAndOrganizationId(userId, organizationId);
            if (employeeOpt.isPresent()) {
                EmployeeEntity employee = employeeOpt.get();
                String name = joinName(employee.getFirstName(), employee.getLastName());
                return new RecipientContact(name, employee.getEmail(), employee.getPhone());
            }

            log.warn("Could not resolve contact details for recipient userId={} in org={}", userId, organizationId);
        }

        if (clientId != null) {
            Optional<ClientEntity> clientOpt = clientRepository.findByIdAndOrganizationId(clientId, organizationId);
            if (clientOpt.isPresent()) {
                ClientEntity client = clientOpt.get();
                return new RecipientContact(client.getDisplayName(), client.getEmail(), client.getPhone());
            }
            log.warn("Could not resolve contact details for recipient clientId={} in org={}", clientId, organizationId);
        }

        return new RecipientContact(null, null, null);
    }

    private String joinName(String firstName, String lastName) {
        String full = (StringUtils.hasText(firstName) ? firstName : "") + (StringUtils.hasText(lastName) ? " " + lastName : "");
        return full.trim();
    }

    private record RecipientContact(String name, String email, String phone) {
    }

    private record RecipientTarget(UUID userId, UUID clientId) {
    }
}
