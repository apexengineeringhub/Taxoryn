package com.taxoryn.module.marketplace.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Geographic calculation utilities supporting Haversine distance computations
 * and bounding-box spatial projections for Taxoryn Marketplace.
 */
public final class GeoUtils {

    /** Earth mean radius in kilometers (WGS 84) */
    public static final double EARTH_RADIUS_KM = 6371.0;

    /** Approximate kilometers per degree of latitude */
    public static final double KM_PER_LATITUDE_DEGREE = 111.045;

    private GeoUtils() {
        // Utility class
    }

    /**
     * Bounding box rectangle defined by min/max latitude and longitude.
     */
    public record BoundingBox(
            BigDecimal minLat,
            BigDecimal maxLat,
            BigDecimal minLng,
            BigDecimal maxLng
    ) {}

    /**
     * Computes the great-circle distance between two points on the Earth's surface
     * using the exact Haversine formula.
     *
     * @param lat1 Latitude of first point in degrees
     * @param lon1 Longitude of first point in degrees
     * @param lat2 Latitude of second point in degrees
     * @param lon2 Longitude of second point in degrees
     * @return Distance in kilometers
     */
    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Computes distance from BigDecimal coordinates.
     */
    public static double calculateDistanceKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }
        return calculateDistanceKm(lat1.doubleValue(), lon1.doubleValue(), lat2.doubleValue(), lon2.doubleValue());
    }

    /**
     * Computes a rectangular bounding box around a center coordinate given a search radius.
     *
     * @param lat Center latitude
     * @param lon Center longitude
     * @param radiusKm Radius in kilometers
     * @return BoundingBox with clamped geographic bounds
     */
    public static BoundingBox calculateBoundingBox(double lat, double lon, double radiusKm) {
        double deltaLat = radiusKm / KM_PER_LATITUDE_DEGREE;
        double cosLat = Math.cos(Math.toRadians(lat));
        double deltaLon = (Math.abs(cosLat) > 1e-6)
                ? radiusKm / (KM_PER_LATITUDE_DEGREE * cosLat)
                : 180.0;

        double minLat = Math.max(-90.0, lat - deltaLat);
        double maxLat = Math.min(90.0, lat + deltaLat);
        double minLng = Math.max(-180.0, lon - deltaLon);
        double maxLng = Math.min(180.0, lon + deltaLon);

        return new BoundingBox(
                BigDecimal.valueOf(minLat).setScale(6, RoundingMode.HALF_UP),
                BigDecimal.valueOf(maxLat).setScale(6, RoundingMode.HALF_UP),
                BigDecimal.valueOf(minLng).setScale(6, RoundingMode.HALF_UP),
                BigDecimal.valueOf(maxLng).setScale(6, RoundingMode.HALF_UP)
        );
    }

    /**
     * Rounds a distance in kilometers to 1 decimal place (e.g. 2.4 km).
     */
    public static double roundDistance(double distanceKm) {
        return BigDecimal.valueOf(distanceKm)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
