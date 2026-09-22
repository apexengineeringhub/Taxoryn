package com.taxoryn.module.notice.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.notice.dto.AdjournHearingRequest;
import com.taxoryn.module.notice.dto.ClientNoticeDto;
import com.taxoryn.module.notice.dto.CloseNoticeRequest;
import com.taxoryn.module.notice.dto.CreateNoticeResponseRequest;
import com.taxoryn.module.notice.dto.CreateTaxNoticeRequest;
import com.taxoryn.module.notice.dto.NoticeActivityDto;
import com.taxoryn.module.notice.dto.NoticeDashboardStatsDto;
import com.taxoryn.module.notice.dto.NoticeHearingDto;
import com.taxoryn.module.notice.dto.NoticeResponseDto;
import com.taxoryn.module.notice.dto.RecordHearingOutcomeRequest;
import com.taxoryn.module.notice.dto.ReviewNoticeResponseRequest;
import com.taxoryn.module.notice.dto.ScheduleHearingRequest;
import com.taxoryn.module.notice.dto.SubmitNoticeRequest;
import com.taxoryn.module.notice.dto.TaxNoticeDto;
import com.taxoryn.module.notice.dto.TaxNoticeFilterRequest;
import com.taxoryn.module.notice.dto.UpdateNoticeHearingRequest;
import com.taxoryn.module.notice.dto.UpdateNoticeResponseRequest;
import com.taxoryn.module.notice.dto.UpdateTaxNoticeRequest;

import java.util.List;
import java.util.UUID;

public interface TaxNoticeService {

    PagedResponse<TaxNoticeDto> getNotices(TaxNoticeFilterRequest filterRequest, PageRequestDto pageRequest);

    TaxNoticeDto getNoticeById(UUID noticeId);

    TaxNoticeDto getNoticeByNumber(String noticeNumber);

    TaxNoticeDto createNotice(CreateTaxNoticeRequest request);

    TaxNoticeDto updateNotice(UUID noticeId, UpdateTaxNoticeRequest request);

    void deleteNotice(UUID noticeId);

    NoticeDashboardStatsDto getDashboardStats();

    List<TaxNoticeDto> getNoticesByClient(UUID clientId);

    PagedResponse<ClientNoticeDto> getClientPortalNotices(UUID clientId, PageRequestDto pageRequest);

    // Response Drafting & Review
    List<NoticeResponseDto> getResponses(UUID noticeId);

    NoticeResponseDto getResponseById(UUID noticeId, UUID responseId);

    NoticeResponseDto createResponse(UUID noticeId, CreateNoticeResponseRequest request);

    NoticeResponseDto reviewResponse(UUID noticeId, UUID responseId, ReviewNoticeResponseRequest request);

    TaxNoticeDto updateNoticeResponse(UUID noticeId, UpdateNoticeResponseRequest request);

    NoticeResponseDto draftNoticeResponse(UUID noticeId, CreateNoticeResponseRequest request);

    TaxNoticeDto submitNoticeResponse(UUID noticeId, SubmitNoticeRequest request);

    // Hearings & Proceedings
    List<NoticeHearingDto> getHearings(UUID noticeId);

    NoticeHearingDto scheduleHearing(UUID noticeId, ScheduleHearingRequest request);

    NoticeHearingDto recordHearingOutcome(UUID noticeId, UUID hearingId, RecordHearingOutcomeRequest request);

    TaxNoticeDto updateNoticeHearing(UUID noticeId, UpdateNoticeHearingRequest request);

    NoticeHearingDto completeHearing(UUID noticeId, UUID hearingId, RecordHearingOutcomeRequest request);

    NoticeHearingDto adjournHearing(UUID noticeId, UUID hearingId, AdjournHearingRequest request);

    NoticeHearingDto cancelHearing(UUID noticeId, UUID hearingId, String reason);

    // Portal Filing & Closure
    TaxNoticeDto submitNotice(UUID noticeId, SubmitNoticeRequest request);

    TaxNoticeDto closeNotice(UUID noticeId, CloseNoticeRequest request);

    // Activities & Notes
    List<NoticeActivityDto> getActivities(UUID noticeId);

    void addInternalNote(UUID noticeId, String note);
}
