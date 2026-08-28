package com.taxoryn.module.feedback.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.feedback.dto.ApplicationFeedbackDto;
import com.taxoryn.module.feedback.dto.CreateApplicationFeedbackRequest;
import org.springframework.data.domain.Pageable;

public interface ApplicationFeedbackService {
    ApplicationFeedbackDto createFeedback(CreateApplicationFeedbackRequest request, String page, String feature);

    PagedResponse<ApplicationFeedbackDto> getMyFeedback(Pageable pageable);
}
