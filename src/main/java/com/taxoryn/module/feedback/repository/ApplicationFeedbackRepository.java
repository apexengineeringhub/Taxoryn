package com.taxoryn.module.feedback.repository;

import com.taxoryn.module.feedback.entity.ApplicationFeedbackEntity;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApplicationFeedbackRepository extends JpaRepository<ApplicationFeedbackEntity, UUID>, JpaSpecificationExecutor<ApplicationFeedbackEntity> {
    Page<ApplicationFeedbackEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<ApplicationFeedbackEntity> findByUserIdAndPracticeIdOrderByCreatedAtDesc(UUID userId, UUID practiceId, Pageable pageable);

    long countByStatus(ApplicationFeedbackStatus status);
}
