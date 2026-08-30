package com.taxoryn.module.content.util;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing, validating, and formatting YouTube video references.
 */
public final class YouTubeUtils {

    private YouTubeUtils() {}

    // Regex matching standard watch, short URL, embed, shorts, live, and raw 11-char ID
    private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
            "^(?:https?:\\/\\/)?(?:www\\.|m\\.)?(?:youtube\\.com\\/(?:watch\\?(?:.*&)?v=|embed\\/|v\\/|shorts\\/|live\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})(?:[?&].*)?$"
    );

    private static final Pattern RAW_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{11}$");

    /**
     * Extracts canonical 11-character YouTube video ID from a URL or raw ID.
     *
     * @param input Raw YouTube URL or video ID string
     * @return Canonical 11-character YouTube video ID, or null if invalid
     */
    public static String extractVideoId(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        String trimmed = input.trim();

        if (RAW_ID_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }

        Matcher matcher = YOUTUBE_URL_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Validates whether an input is a valid YouTube URL or video ID.
     */
    public static boolean isValidYouTubeReference(String input) {
        return extractVideoId(input) != null;
    }

    /**
     * Constructs the standard high-quality thumbnail URL for a video ID.
     */
    public static String buildThumbnailUrl(String videoId) {
        if (!StringUtils.hasText(videoId)) {
            return null;
        }
        return "https://img.youtube.com/vi/" + videoId.trim() + "/hqdefault.jpg";
    }

    /**
     * Constructs standard embed URL for iframe player.
     */
    public static String buildEmbedUrl(String videoId) {
        if (!StringUtils.hasText(videoId)) {
            return null;
        }
        return "https://www.youtube.com/embed/" + videoId.trim();
    }

    /**
     * Constructs standard watch URL on YouTube.
     */
    public static String buildWatchUrl(String videoId) {
        if (!StringUtils.hasText(videoId)) {
            return null;
        }
        return "https://www.youtube.com/watch?v=" + videoId.trim();
    }

    /**
     * Formats duration in seconds to human-readable string (e.g., 300s -> "5 min").
     */
    public static String formatDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return null;
        }
        int mins = seconds / 60;
        int remSecs = seconds % 60;
        if (mins == 0) {
            return remSecs + " sec";
        }
        if (remSecs == 0) {
            return mins + " min";
        }
        return mins + " min " + remSecs + " sec";
    }
}
