package com.taxoryn.module.gmail.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

public class GmailThreadModels {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ThreadListResponse {
        @Builder.Default
        private List<ThreadSummary> threads = new ArrayList<>();
        private String nextPageToken;
        private Long resultSizeEstimate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ThreadSummary {
        private String id;
        private String snippet;
        private String historyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ThreadDetail {
        private String id;
        private String historyId;
        @Builder.Default
        private List<MessageMetadata> messages = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageMetadata {
        private String id;
        private String threadId;
        @Builder.Default
        private List<String> labelIds = new ArrayList<>();
        private String snippet;
        private Long internalDate;
        private MessagePayload payload;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessagePayload {
        @Builder.Default
        private List<HeaderEntry> headers = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HeaderEntry {
        private String name;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HistoryResponse {
        @Builder.Default
        private List<HistoryRecord> history = new ArrayList<>();
        private String historyId;
        private String nextPageToken;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HistoryRecord {
        private String id;
        @Builder.Default
        private List<MessageAdded> messagesAdded = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageAdded {
        private MessageMetadata message;
    }
}
