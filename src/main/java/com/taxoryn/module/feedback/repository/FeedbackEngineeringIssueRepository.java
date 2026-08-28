package com.taxoryn.module.feedback.repository;

import com.taxoryn.module.feedback.entity.FeedbackEngineeringIssueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedbackEngineeringIssueRepository extends JpaRepository<FeedbackEngineeringIssueEntity, UUID> {
    Optional<FeedbackEngineeringIssueEntity> findByFeedbackId(UUID feedbackId);

    Optional<FeedbackEngineeringIssueEntity> findByIssueCode(String issueCode);
}
