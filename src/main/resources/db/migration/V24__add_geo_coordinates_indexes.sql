-- ============================================================================
-- V24: Add Geo Coordinates & State Index for Marketplace Practice Locations
-- ============================================================================

-- Composite B-tree index on geographic coordinates for efficient bounding-box queries
CREATE INDEX IF NOT EXISTS idx_marketplace_locations_geo 
ON marketplace_practice_locations(latitude, longitude);

-- Index on state for administrative state-level discovery
CREATE INDEX IF NOT EXISTS idx_marketplace_locations_state 
ON marketplace_practice_locations(state);
