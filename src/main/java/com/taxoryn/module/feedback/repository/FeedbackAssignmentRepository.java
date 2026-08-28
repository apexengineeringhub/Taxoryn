package com.taxoryn.module.feedback.repository;

import com.taxoryn.module.feedback.entity.FeedbackAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedbackAssignmentRepository extends JpaRepository<FeedbackAssignmentEntity, UUID> {
    List<FeedbackAssignmentEntity> findByFeedbackIdOrderByAssignedAtDesc(UUID feedbackId);

    Optional<FeedbackAssignmentEntity> findFirstByFeedbackIdAndActiveTrueOrderByAssignedAtDesc(UUID feedbackId);

    List<FeedbackAssignmentEntity> findByFeedbackIdAndActiveTrue(UUID feedbackId);
}
