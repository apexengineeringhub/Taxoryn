package com.taxoryn.module.content.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("YouTubeUtils Unit Tests")
class YouTubeUtilsTest {

    @ParameterizedTest(name = "Valid YouTube URL: {0}")
    @ValueSource(strings = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "http://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtube.com/watch?v=dQw4w9WgXcQ&feature=share",
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "http://youtu.be/dQw4w9WgXcQ?t=10s",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://www.youtube.com/v/dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            "dQw4w9WgXcQ"
    })
    void shouldExtractCanonicalVideoId(String input) {
        String videoId = YouTubeUtils.extractVideoId(input);
        assertEquals("dQw4w9WgXcQ", videoId);
        assertTrue(YouTubeUtils.isValidYouTubeReference(input));
    }

    @ParameterizedTest(name = "Invalid YouTube input: {0}")
    @ValueSource(strings = {
            "https://vimeo.com/12345678",
            "https://dailymotion.com/video/x7tgad0",
            "https://example.com/video.mp4",
            "not-a-video-id",
            "shortId",
            "",
            "   "
    })
    void shouldRejectInvalidInputs(String input) {
        assertNull(YouTubeUtils.extractVideoId(input));
        assertFalse(YouTubeUtils.isValidYouTubeReference(input));
    }

    @Test
    void shouldBuildThumbnailUrl() {
        String thumb = YouTubeUtils.buildThumbnailUrl("dQw4w9WgXcQ");
        assertEquals("https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg", thumb);
        assertNull(YouTubeUtils.buildThumbnailUrl(null));
        assertNull(YouTubeUtils.buildThumbnailUrl(""));
    }

    @Test
    void shouldBuildEmbedUrl() {
        String embed = YouTubeUtils.buildEmbedUrl("dQw4w9WgXcQ");
        assertEquals("https://www.youtube.com/embed/dQw4w9WgXcQ", embed);
        assertNull(YouTubeUtils.buildEmbedUrl(null));
    }

    @Test
    void shouldBuildWatchUrl() {
        String watch = YouTubeUtils.buildWatchUrl("dQw4w9WgXcQ");
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", watch);
        assertNull(YouTubeUtils.buildWatchUrl(null));
    }

    @Test
    void shouldFormatDuration() {
        assertEquals("5 min", YouTubeUtils.formatDuration(300));
        assertEquals("1 min 15 sec", YouTubeUtils.formatDuration(75));
        assertEquals("45 sec", YouTubeUtils.formatDuration(45));
        assertNull(YouTubeUtils.formatDuration(null));
        assertNull(YouTubeUtils.formatDuration(0));
        assertNull(YouTubeUtils.formatDuration(-10));
    }
}
