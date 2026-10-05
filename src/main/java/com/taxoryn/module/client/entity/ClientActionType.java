package com.taxoryn.module.client.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Action type for recommended next actions on a client")
public enum ClientActionType {
    COMPLETE_PROFILE,
    ADD_PRIMARY_CONTACT,
    ADD_PRIMARY_BRANCH,
    CONFIGURE_SERVICE,
    RESUME_SERVICE,
    INVITE_PORTAL_USER,
    ACTIVATE_CLIENT,
    REVIEW_CLIENT_STATUS,
    VIEW_TIMELINE
}
