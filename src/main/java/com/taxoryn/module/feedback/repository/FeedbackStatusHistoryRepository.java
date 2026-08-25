package com.taxoryn.module.feedback.repository;

import com.taxoryn.module.feedback.entity.FeedbackStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedbackStatusHistoryRepository extends JpaRepository<FeedbackStatusHistoryEntity, UUID> {
    List<FeedbackStatusHistoryEntity> findByFeedbackIdOrderByCreatedAtAsc(UUID feedbackId);
}
