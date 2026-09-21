package com.taxoryn.module.employee.chat.service;

import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.employee.chat.dto.EmployeeChatChannelDto;
import com.taxoryn.module.employee.chat.dto.EmployeeChatContactDto;
import com.taxoryn.module.employee.chat.dto.EmployeeChatMessageDto;
import com.taxoryn.module.employee.chat.dto.EmployeeChatWebSocketMessage;
import com.taxoryn.module.employee.chat.dto.SendEmployeeChatMessageRequest;
import com.taxoryn.module.employee.chat.entity.EmployeeChatChannelEntity;
import com.taxoryn.module.employee.chat.entity.EmployeeChatMessageEntity;
import com.taxoryn.module.employee.chat.repository.EmployeeChatChannelRepository;
import com.taxoryn.module.employee.chat.repository.EmployeeChatMessageRepository;
import com.taxoryn.module.employee.chat.websocket.EmployeeChatWebSocketHandler;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeChatServiceImpl implements EmployeeChatService {

    private final EmployeeChatMessageRepository messageRepository;
    private final EmployeeChatChannelRepository channelRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final EmployeeChatWebSocketHandler webSocketHandler;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeChatContactDto> getEligibleContacts() {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        UUID organizationId = scope.getOrganizationId();
        if (organizationId == null) {
            return Collections.emptyList();
        }

        EmployeeEntity currentEmployee = resolveCurrentEmployee(scope);
        UUID currentEmployeeId = currentEmployee != null ? currentEmployee.getId() : null;

        List<EmployeeEntity> allEmployees = employeeRepository.findAllByOrganizationId(organizationId);
        List<UserEntity> orgUsers = userRepository.findAllByOrganizationId(organizationId);
        Map<UUID, UserEntity> userMap = orgUsers.stream()
                .filter(u -> u.getId() != null)
                .collect(Collectors.toMap(UserEntity::getId, Function.identity(), (existing, replacing) -> existing));

        // Filter employees based on Security Visibility Policy
        List<EmployeeEntity> accessible = allEmployees.stream()
                .filter(e -> isEmployeeAccessibleToUser(e, currentEmployee, scope))
                .toList();

        return accessible.stream()
                .map(emp -> {
                    UUID empId = emp.getId();
                    boolean isSelf = Objects.equals(empId, currentEmployeeId);

                    long unreadCount = 0;
                    String lastMessagePreview = null;
                    Instant lastMessageTimestamp = null;

                    if (currentEmployeeId != null && !isSelf) {
                        unreadCount = messageRepository.countUnreadDirectFromSender(organizationId, empId, currentEmployeeId);
                        List<EmployeeChatMessageEntity> latest = messageRepository.findLatestDirectMessage(
                                organizationId, currentEmployeeId, empId, PageRequest.of(0, 1));
                        if (!latest.isEmpty()) {
                            lastMessagePreview = latest.get(0).getMessageBody();
                            lastMessageTimestamp = latest.get(0).getCreatedAt();
                        }
                    }

                    UserEntity user = emp.getUserId() != null ? userMap.get(emp.getUserId()) : null;
                    String roleName = (user != null && user.getRoles() != null && !user.getRoles().isEmpty())
                            ? user.getRoles().iterator().next().getName()
                            : "STAFF";

                    return EmployeeChatContactDto.builder()
                            .employeeId(emp.getId())
                            .userId(emp.getUserId())
                            .employeeCode(emp.getEmployeeCode())
                            .name(emp.getFullName())
                            .email(emp.getEmail())
                            .phone(emp.getPhone())
                            .avatarUrl(emp.getAvatarUrl())
                            .designation(emp.getDesignation())
                            .department(emp.getDepartment())
                            .role(roleName)
                            .status(emp.getStatus() != null ? emp.getStatus().name() : "ACTIVE")
                            .lastMessagePreview(lastMessagePreview)
                            .lastMessageTimestamp(lastMessageTimestamp)
                            .unreadCount(unreadCount)
                            .isManager(isManagerOrAboveRole(roleName))
                            .isSelf(isSelf)
                            .build();
                })
                .sorted((a, b) -> {
                    // Sort contacts by latest message timestamp descending, then by name
                    if (a.getLastMessageTimestamp() != null && b.getLastMessageTimestamp() != null) {
                        return b.getLastMessageTimestamp().compareTo(a.getLastMessageTimestamp());
                    }
                    if (a.getLastMessageTimestamp() != null) return -1;
                    if (b.getLastMessageTimestamp() != null) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .toList();
    }

    @Override
    @Transactional
    public List<EmployeeChatChannelDto> getAccessibleChannels() {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        UUID organizationId = scope.getOrganizationId();
        if (organizationId == null) {
            return Collections.emptyList();
        }

        // Ensure default general and department channels exist
        ensureDefaultChannels(organizationId);

        List<EmployeeChatChannelEntity> channels;
        if (scope.isFirmAdmin()) {
            channels = channelRepository.findAllByOrganizationIdAndIsArchivedFalseOrderByCreatedAtAsc(organizationId);
        } else if (StringUtils.hasText(scope.getDepartment())) {
            channels = channelRepository.findAccessibleChannelsForDepartment(organizationId, scope.getDepartment().trim());
        } else {
            channels = channelRepository.findGeneralChannels(organizationId);
        }

        return channels.stream()
                .map(c -> {
                    String lastMessage = null;
                    Instant lastTimestamp = null;
                    List<EmployeeChatMessageEntity> latest = messageRepository.findLatestChannelMessage(
                            organizationId, c.getId(), PageRequest.of(0, 1));
                    if (!latest.isEmpty()) {
                        lastMessage = latest.get(0).getMessageBody();
                        lastTimestamp = latest.get(0).getCreatedAt();
                    }

                    return EmployeeChatChannelDto.builder()
                            .id(c.getId())
                            .organizationId(c.getOrganizationId())
                            .name(c.getName())
                            .displayName(c.getDisplayName())
                            .description(c.getDescription())
                            .channelType(c.getChannelType())
                            .department(c.getDepartment())
                            .isDefault(c.getIsDefault())
                            .isArchived(c.getIsArchived())
                            .createdAt(c.getCreatedAt())
                            .lastMessagePreview(lastMessage)
                            .lastMessageTimestamp(lastTimestamp)
                            .unreadCount(0L)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeChatMessageDto> getDirectMessages(UUID recipientEmployeeId) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        UUID organizationId = scope.getOrganizationId();
        EmployeeEntity currentEmployee = resolveCurrentEmployee(scope);

        if (currentEmployee == null || recipientEmployeeId == null) {
            return Collections.emptyList();
        }

        EmployeeEntity recipientEmployee = employeeRepository.findByIdAndOrganizationId(recipientEmployeeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee recipient not found"));

        // Verify that current user is authorized to chat with recipient
        if (!isEmployeeAccessibleToUser(recipientEmployee, currentEmployee, scope)) {
            throw new ForbiddenException("You are not authorized to access chat with this employee under organization policy");
        }

        List<EmployeeChatMessageEntity> messages = messageRepository.findDirectMessagesBetween(
                organizationId, currentEmployee.getId(), recipientEmployee.getId());

        Map<UUID, EmployeeEntity> empCache = Map.of(
                currentEmployee.getId(), currentEmployee,
                recipientEmployee.getId(), recipientEmployee
        );

        return messages.stream()
                .map(m -> mapToDto(m, currentEmployee.getId(), empCache))
                .toList();
    }

    @Override
    @Transactional
    public EmployeeChatMessageDto sendDirectMessage(UUID recipientEmployeeId, SendEmployeeChatMessageRequest request) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        UUID organizationId = scope.getOrganizationId();
        EmployeeEntity currentEmployee = resolveCurrentEmployee(scope);

        if (currentEmployee == null) {
            throw new ForbiddenException("Active employee profile required to send internal messages");
        }

        EmployeeEntity recipientEmployee = employeeRepository.findByIdAndOrganizationId(recipientEmployeeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee recipient not found"));

        if (!isEmployeeAccessibleToUser(recipientEmployee, currentEmployee, scope)) {
            throw new ForbiddenException("You are not authorized to send messages to this employee under organization policy");
        }

        EmployeeChatMessageEntity message = EmployeeChatMessageEntity.builder()
                .senderEmployeeId(currentEmployee.getId())
                .recipientEmployeeId(recipientEmployee.getId())
                .messageBody(request.getMessageBody().trim())
                .attachmentsJson(request.getAttachmentsJson())
                .isRead(false)
                .build();
        message.setOrganizationId(organizationId);

        message = messageRepository.save(message);

        Map<UUID, EmployeeEntity> empCache = Map.of(
                currentEmployee.getId(), currentEmployee,
                recipientEmployee.getId(), recipientEmployee
        );

        EmployeeChatMessageDto dto = mapToDto(message, currentEmployee.getId(), empCache);

        // Broadcast real-time event to recipient and sender via WebSocket
        EmployeeChatWebSocketMessage wsEvent = EmployeeChatWebSocketMessage.builder()
                .type(EmployeeChatWebSocketMessage.Type.NEW_MESSAGE)
                .organizationId(organizationId)
                .senderEmployeeId(currentEmployee.getId())
                .senderName(currentEmployee.getFullName())
                .recipientEmployeeId(recipientEmployee.getId())
                .message(dto)
                .timestamp(Instant.now().toString())
                .build();

        webSocketHandler.sendToEmployee(organizationId, recipientEmployee.getId(), wsEvent);
        webSocketHandler.sendToEmployee(organizationId, currentEmployee.getId(), wsEvent);

        return dto;
    }

    @Override
    @Transactional
    public void markDirectMessagesRead(UUID senderEmployeeId) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        UUID organizationId = scope.getOrganizationId();
        EmployeeEntity currentEmployee = resolveCurrentEmployee(scope);

        if (currentEmployee == null || senderEmployeeId == null) {
            return;
        }

        Instant now = Instant.now();
        int updated = messageRepository.markDirectMessagesRead(organizationId, senderEmployeeId, currentEmployee.getId(), now);

        if (updated > 0) {
            // Notify sender in real-time that their messages were read
            EmployeeChatWebSocketMessage readEvent = EmployeeChatWebSocketMessage.builder()
                    .type(EmployeeChatWebSocketMessage.Type.MESSAGES_READ)
                    .organizationId(organizationId)
                    .senderEmployeeId(senderEmployeeId)
                    .recipientEmployeeId(currentEmployee.getId())
                    .timestamp(now.toString())
                    .build();
            webSocketHandler.sendToEmployee(organizationId, senderEmployeeId, readEvent);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeChatMessageDto> getChannelMessages(UUID channelId) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        UUID organizationId = scope.getOrganizationId();
        EmployeeEntity currentEmployee = resolveCurrentEmployee(scope);

        if (channelId == null || organizationId == null) {
            return Collections.emptyList();
        }

        EmployeeChatChannelEntity channel = channelRepository.findByIdAndOrganizationId(channelId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found"));

        validateChannelAccess(channel, scope);

        List<EmployeeChatMessageEntity> messages = messageRepository.findChannelMessages(organizationId, channelId);

        // Resolve sender employee details
        Set<UUID> senderIds = messages.stream().map(EmployeeChatMessageEntity::getSenderEmployeeId).collect(Collectors.toSet());
        Map<UUID, EmployeeEntity> senderMap = employeeRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(EmployeeEntity::getId, Function.identity()));

        UUID currentEmpId = currentEmployee != null ? currentEmployee.getId() : null;

        return messages.stream()
                .map(m -> mapToDto(m, currentEmpId, senderMap))
                .toList();
    }

    @Override
    @Transactional
    public EmployeeChatMessageDto sendChannelMessage(UUID channelId, SendEmployeeChatMessageRequest request) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        UUID organizationId = scope.getOrganizationId();
        EmployeeEntity currentEmployee = resolveCurrentEmployee(scope);

        if (currentEmployee == null) {
            throw new ForbiddenException("Active employee profile required to participate in team channels");
        }

        EmployeeChatChannelEntity channel = channelRepository.findByIdAndOrganizationId(channelId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found"));

        validateChannelAccess(channel, scope);

        EmployeeChatMessageEntity message = EmployeeChatMessageEntity.builder()
                .channelId(channel.getId())
                .senderEmployeeId(currentEmployee.getId())
                .messageBody(request.getMessageBody().trim())
                .attachmentsJson(request.getAttachmentsJson())
                .isRead(true) // Channels do not have single read receipt
                .build();
        message.setOrganizationId(organizationId);

        message = messageRepository.save(message);

        Map<UUID, EmployeeEntity> empCache = Map.of(currentEmployee.getId(), currentEmployee);
        EmployeeChatMessageDto dto = mapToDto(message, currentEmployee.getId(), empCache);

        // Find allowed channel member employees to broadcast
        Set<UUID> memberEmployeeIds = getChannelMemberIds(channel, organizationId);

        EmployeeChatWebSocketMessage wsEvent = EmployeeChatWebSocketMessage.builder()
                .type(EmployeeChatWebSocketMessage.Type.NEW_MESSAGE)
                .organizationId(organizationId)
                .channelId(channel.getId())
                .senderEmployeeId(currentEmployee.getId())
                .senderName(currentEmployee.getFullName())
                .message(dto)
                .timestamp(Instant.now().toString())
                .build();

        webSocketHandler.broadcastToChannelMembers(organizationId, memberEmployeeIds, wsEvent);

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public long getGlobalUnreadCount() {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        UUID organizationId = scope.getOrganizationId();
        EmployeeEntity currentEmployee = resolveCurrentEmployee(scope);

        if (organizationId == null || currentEmployee == null) {
            return 0;
        }

        return messageRepository.countTotalUnreadForRecipient(organizationId, currentEmployee.getId());
    }

    private void validateChannelAccess(EmployeeChatChannelEntity channel, PracticeSecurityScope scope) {
        if (scope.isFirmAdmin()) {
            return; // Admins have access to all channels
        }

        if (channel.getChannelType() == EmployeeChatChannelEntity.ChannelType.GENERAL) {
            return; // General channels are accessible to all organization members
        }

        if (channel.getChannelType() == EmployeeChatChannelEntity.ChannelType.DEPARTMENT) {
            if (StringUtils.hasText(scope.getDepartment()) &&
                    scope.getDepartment().trim().equalsIgnoreCase(channel.getDepartment())) {
                return;
            }
        }

        throw new ForbiddenException("You do not have access to this department channel under organization security policy");
    }

    private Set<UUID> getChannelMemberIds(EmployeeChatChannelEntity channel, UUID organizationId) {
        List<EmployeeEntity> allEmployees = employeeRepository.findAllByOrganizationId(organizationId);
        if (channel.getChannelType() == EmployeeChatChannelEntity.ChannelType.GENERAL) {
            return allEmployees.stream().map(EmployeeEntity::getId).collect(Collectors.toSet());
        }

        Set<UUID> memberIds = new HashSet<>();
        String dept = channel.getDepartment();

        for (EmployeeEntity emp : allEmployees) {
            if (emp.getDepartment() != null && emp.getDepartment().equalsIgnoreCase(dept)) {
                memberIds.add(emp.getId());
            }
        }
        return memberIds;
    }

    private boolean isEmployeeAccessibleToUser(EmployeeEntity target, EmployeeEntity current, PracticeSecurityScope scope) {
        if (target == null) return false;
        if (scope.isFirmAdmin()) return true;

        if (current != null && Objects.equals(target.getId(), current.getId())) {
            return true;
        }

        // Check if target is a firm admin / partner (any staff can message firm admins)
        if (target.getUserId() != null) {
            Optional<UserEntity> userOpt = userRepository.findById(target.getUserId());
            if (userOpt.isPresent() && userOpt.get().getRoles() != null) {
                boolean hasAdminRole = userOpt.get().getRoles().stream()
                        .anyMatch(r -> isFirmAdminRole(r.getCode()) || isFirmAdminRole(r.getName()));
                if (hasAdminRole) {
                    return true;
                }
            }
        }

        // If current user is department manager: can access same department or direct reportees
        if (scope.isDepartmentManager()) {
            if (current != null && Objects.equals(target.getManagerId(), current.getId())) {
                return true;
            }
            if (StringUtils.hasText(scope.getDepartment()) &&
                    scope.getDepartment().trim().equalsIgnoreCase(target.getDepartment())) {
                return true;
            }
        }

        // If current user is staff: can access same department peers and their direct manager
        if (scope.isStaff()) {
            if (current != null && current.getManagerId() != null &&
                    Objects.equals(target.getId(), current.getManagerId())) {
                return true;
            }
            if (StringUtils.hasText(scope.getDepartment()) &&
                    scope.getDepartment().trim().equalsIgnoreCase(target.getDepartment())) {
                return true;
            }
        }

        return false;
    }

    private boolean isFirmAdminRole(String roleName) {
        if (roleName == null) return false;
        String r = roleName.toUpperCase();
        return r.contains("ADMIN") || r.contains("PARTNER") || r.contains("OWNER");
    }

    private boolean isManagerOrAboveRole(String roleName) {
        if (roleName == null) return false;
        String r = roleName.toUpperCase();
        return r.contains("MANAGER") || isFirmAdminRole(roleName);
    }

    private EmployeeEntity resolveCurrentEmployee(PracticeSecurityScope scope) {
        if (scope.getEmployee() != null) {
            return scope.getEmployee();
        }
        UUID organizationId = scope.getOrganizationId();
        UUID userId = scope.getUserId();
        String email = scope.getUserEmail();

        if (organizationId == null) return null;

        if (userId != null) {
            Optional<EmployeeEntity> emp = employeeRepository.findByOrganizationIdAndUserId(organizationId, userId);
            if (emp.isPresent()) return emp.get();
        }

        if (email != null) {
            Optional<EmployeeEntity> emp = employeeRepository.findByOrganizationIdAndEmail(organizationId, email);
            if (emp.isPresent()) return emp.get();
        }

        // Fallback: If firm admin doesn't have an EmployeeEntity row, auto-create a linked profile
        if (scope.isFirmAdmin() && userId != null) {
            Optional<UserEntity> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                UserEntity u = userOpt.get();
                EmployeeEntity created = EmployeeEntity.builder()
                        .userId(u.getId())
                        .employeeCode("EMP-ADM-01")
                        .firstName(u.getFirstName() != null ? u.getFirstName() : "Practice")
                        .lastName(u.getLastName() != null ? u.getLastName() : "Admin")
                        .email(u.getEmail())
                        .designation("Firm Administrator")
                        .department("Management")
                        .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                        .build();
                created.setOrganizationId(organizationId);
                return employeeRepository.save(created);
            }
        }

        return null;
    }

    private void ensureDefaultChannels(UUID organizationId) {
        Optional<EmployeeChatChannelEntity> general = channelRepository.findByOrganizationIdAndNameAndIsArchivedFalse(organizationId, "general");
        if (general.isEmpty()) {
            EmployeeChatChannelEntity chan = EmployeeChatChannelEntity.builder()
                    .name("general")
                    .displayName("General Firm Practice")
                    .description("Firm-wide announcements and team collaboration")
                    .channelType(EmployeeChatChannelEntity.ChannelType.GENERAL)
                    .isDefault(true)
                    .isArchived(false)
                    .build();
            chan.setOrganizationId(organizationId);
            channelRepository.save(chan);
        }

        // Ensure key practice department channels
        String[] depts = {"GST Compliance", "Direct Tax & ITR", "Audit & Assurance", "Client Advisory"};
        String[] slugs = {"gst-compliance", "itr-compliance", "audit-assurance", "client-advisory"};

        for (int i = 0; i < depts.length; i++) {
            String slug = slugs[i];
            String deptName = depts[i];
            if (channelRepository.findByOrganizationIdAndNameAndIsArchivedFalse(organizationId, slug).isEmpty()) {
                EmployeeChatChannelEntity deptChan = EmployeeChatChannelEntity.builder()
                        .name(slug)
                        .displayName(deptName)
                        .description("Department collaboration channel for " + deptName)
                        .channelType(EmployeeChatChannelEntity.ChannelType.DEPARTMENT)
                        .department(deptName)
                        .isDefault(true)
                        .isArchived(false)
                        .build();
                deptChan.setOrganizationId(organizationId);
                channelRepository.save(deptChan);
            }
        }
    }

    private EmployeeChatMessageDto mapToDto(EmployeeChatMessageEntity entity, UUID currentEmployeeId, Map<UUID, EmployeeEntity> empCache) {
        EmployeeEntity sender = empCache.get(entity.getSenderEmployeeId());
        EmployeeEntity recipient = entity.getRecipientEmployeeId() != null ? empCache.get(entity.getRecipientEmployeeId()) : null;

        return EmployeeChatMessageDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .channelId(entity.getChannelId())
                .senderEmployeeId(entity.getSenderEmployeeId())
                .senderName(sender != null ? sender.getFullName() : "Employee")
                .senderEmail(sender != null ? sender.getEmail() : null)
                .senderAvatarUrl(sender != null ? sender.getAvatarUrl() : null)
                .senderDesignation(sender != null ? sender.getDesignation() : null)
                .senderDepartment(sender != null ? sender.getDepartment() : null)
                .recipientEmployeeId(entity.getRecipientEmployeeId())
                .recipientName(recipient != null ? recipient.getFullName() : null)
                .messageBody(entity.getMessageBody())
                .attachmentsJson(entity.getAttachmentsJson())
                .isRead(entity.getIsRead())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .isOwnMessage(currentEmployeeId != null && Objects.equals(entity.getSenderEmployeeId(), currentEmployeeId))
                .build();
    }
}
