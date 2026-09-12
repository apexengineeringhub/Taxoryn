package com.taxoryn.module.notice.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.notice.enums.NoticeActivityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "notice_activities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeActivityEntity extends TenantAuditableEntity {

    @Column(name = "notice_id", nullable = false)
    private UUID noticeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private NoticeActivityType activityType;

    @Column(name = "performed_by_user_id")
    private UUID performedByUserId;

    @Column(name = "performer_name", length = 150)
    private String performerName;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "from_state", length = 50)
    private String fromState;

    @Column(name = "to_state", length = 50)
    private String toState;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}
