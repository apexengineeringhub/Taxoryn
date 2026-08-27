package com.taxoryn.module.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentScheduleRunner {

    private final ContentService contentService;

    /**
     * Executes every 60 seconds to find and publish scheduled content.
     */
    @Scheduled(fixedDelay = 60000)
    public void processScheduledPublications() {
        try {
            int count = contentService.publishScheduledContent();
            if (count > 0) {
                log.info("ContentScheduleRunner: Successfully published {} scheduled content items.", count);
            }
        } catch (Exception e) {
            log.error("ContentScheduleRunner: Error processing scheduled publications", e);
        }
    }
}
