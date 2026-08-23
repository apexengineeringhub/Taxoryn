package com.taxoryn.module.marketplace.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilsTest {

    @Test
    @DisplayName("Haversine: distance between Bangalore City Center and Whitefield is approx 15-18 km")
    void testCalculateDistance_BangaloreToWhitefield() {
        // Bengaluru MG Road: 12.9716, 77.5946
        // Whitefield: 12.9698, 77.7499
        double distance = GeoUtils.calculateDistanceKm(12.9716, 77.5946, 12.9698, 77.7499);
        assertTrue(distance > 15.0 && distance < 18.0, "Expected distance between 15 and 18 km, got: " + distance);
        assertEquals(16.9, GeoUtils.roundDistance(distance), 0.5);
    }

    @Test
    @DisplayName("Haversine: distance between Bengaluru and Mumbai is approx 840 km")
    void testCalculateDistance_BangaloreToMumbai() {
        // Bengaluru: 12.9716, 77.5946
        // Mumbai: 19.0760, 72.8777
        double distance = GeoUtils.calculateDistanceKm(12.9716, 77.5946, 19.0760, 72.8777);
        assertTrue(distance > 830.0 && distance < 860.0, "Expected distance between 830 and 860 km, got: " + distance);
    }

    @Test
    @DisplayName("Haversine: same point distance is zero")
    void testCalculateDistance_SamePointIsZero() {
        double distance = GeoUtils.calculateDistanceKm(12.9716, 77.5946, 12.9716, 77.5946);
        assertEquals(0.0, distance, 0.0001);
    }

    @Test
    @DisplayName("Haversine: handles BigDecimal coordinates and null safety")
    void testCalculateDistance_BigDecimalNullSafety() {
        assertEquals(Double.MAX_VALUE, GeoUtils.calculateDistanceKm(null, BigDecimal.valueOf(77.59), BigDecimal.valueOf(12.97), BigDecimal.valueOf(77.59)));
        double distance = GeoUtils.calculateDistanceKm(
                BigDecimal.valueOf(12.9716),
                BigDecimal.valueOf(77.5946),
                BigDecimal.valueOf(12.9716),
                BigDecimal.valueOf(77.5946)
        );
        assertEquals(0.0, distance, 0.0001);
    }

    @Test
    @DisplayName("BoundingBox: radius 10 km creates appropriate latitude and longitude delta")
    void testCalculateBoundingBox_Radius10Km() {
        double lat = 12.9716;
        double lon = 77.5946;
        double radiusKm = 10.0;

        GeoUtils.BoundingBox bbox = GeoUtils.calculateBoundingBox(lat, lon, radiusKm);

        assertNotNull(bbox);
        assertTrue(bbox.minLat().doubleValue() < lat);
        assertTrue(bbox.maxLat().doubleValue() > lat);
        assertTrue(bbox.minLng().doubleValue() < lon);
        assertTrue(bbox.maxLng().doubleValue() > lon);

        // Center point should be enclosed
        assertTrue(bbox.minLat().doubleValue() <= lat && lat <= bbox.maxLat().doubleValue());
        assertTrue(bbox.minLng().doubleValue() <= lon && lon <= bbox.maxLng().doubleValue());
    }

    @Test
    @DisplayName("BoundingBox: coordinates clamped to [-90, 90] and [-180, 180]")
    void testCalculateBoundingBox_ClampingAtExtremes() {
        GeoUtils.BoundingBox bbox = GeoUtils.calculateBoundingBox(89.9, 179.9, 50.0);
        assertTrue(bbox.maxLat().doubleValue() <= 90.0);
        assertTrue(bbox.maxLng().doubleValue() <= 180.0);

        GeoUtils.BoundingBox southBbox = GeoUtils.calculateBoundingBox(-89.9, -179.9, 50.0);
        assertTrue(southBbox.minLat().doubleValue() >= -90.0);
        assertTrue(southBbox.minLng().doubleValue() >= -180.0);
    }

    @Test
    @DisplayName("RoundDistance: rounds to 1 decimal place")
    void testRoundDistance() {
        assertEquals(2.4, GeoUtils.roundDistance(2.4123));
        assertEquals(2.5, GeoUtils.roundDistance(2.4678));
        assertEquals(10.0, GeoUtils.roundDistance(9.99));
    }
}
