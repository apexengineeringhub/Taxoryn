package com.taxoryn.module.feedback.repository;

import com.taxoryn.module.feedback.entity.FeedbackNoteEntity;
import com.taxoryn.module.feedback.entity.FeedbackNoteVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedbackNoteRepository extends JpaRepository<FeedbackNoteEntity, UUID> {
    List<FeedbackNoteEntity> findByFeedbackIdOrderByCreatedAtAsc(UUID feedbackId);

    List<FeedbackNoteEntity> findByFeedbackIdAndVisibilityOrderByCreatedAtAsc(UUID feedbackId, FeedbackNoteVisibility visibility);
}
