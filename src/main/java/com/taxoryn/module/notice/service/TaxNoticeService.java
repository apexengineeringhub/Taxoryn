package com.taxoryn.module.notice.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.PagedResponse;
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

    List<NoticeResponseDto> getResponses(UUID noticeId);

    NoticeResponseDto getResponseById(UUID noticeId, UUID responseId);

    NoticeResponseDto createResponse(UUID noticeId, CreateNoticeResponseRequest request);

    NoticeResponseDto reviewResponse(UUID noticeId, UUID responseId, ReviewNoticeResponseRequest request);

    List<NoticeHearingDto> getHearings(UUID noticeId);

    NoticeHearingDto scheduleHearing(UUID noticeId, ScheduleHearingRequest request);

    NoticeHearingDto recordHearingOutcome(UUID noticeId, UUID hearingId, RecordHearingOutcomeRequest request);

    TaxNoticeDto submitNotice(UUID noticeId, SubmitNoticeRequest request);

    TaxNoticeDto closeNotice(UUID noticeId, CloseNoticeRequest request);

    List<NoticeActivityDto> getActivities(UUID noticeId);

    void addInternalNote(UUID noticeId, String note);
}
