package com.taxoryn.module.gst.service;

import com.taxoryn.module.gst.dto.GstAuthContinueRequest;
import com.taxoryn.module.gst.dto.GstAuthSessionDto;
import com.taxoryn.module.gst.dto.GstAuthSessionRequest;
import com.taxoryn.module.gst.model.GstAuthenticationPurpose;

import java.util.Map;
import java.util.UUID;

/**
 * High-level GST domain authentication boundary.
 * Orchestrates purpose-scoped authentication (GST, E-Way Bill, E-Invoice) on top of the
 * provider-neutral Government Authentication subsystem.
 */
public interface GstAuthenticationService {

    GstAuthSessionDto authenticate(GstAuthSessionRequest request);

    GstAuthSessionDto getAuthenticationStatus(UUID sessionId, GstAuthenticationPurpose purpose, Map<String, Object> options);

    GstAuthSessionDto continueAuthorization(UUID sessionId, GstAuthContinueRequest request, GstAuthenticationPurpose purpose);

    GstAuthSessionDto refreshToken(UUID sessionId, GstAuthenticationPurpose purpose, Map<String, Object> options);

    GstAuthSessionDto revoke(UUID sessionId, GstAuthenticationPurpose purpose);

    GstAuthSessionDto getActiveToken(UUID connectionId, GstAuthenticationPurpose purpose);
}
