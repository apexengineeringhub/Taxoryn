-- V63: Correct existing document scan status to LEGACY_UNSCANNED and update default constraint
-- Historical documents created before enforced malware scanning must not be falsely trusted as CLEAN.

-- 1. Ensure default for un-scanned rows is PENDING_SCAN rather than CLEAN
ALTER TABLE documents
    ALTER COLUMN scan_status SET DEFAULT 'PENDING_SCAN';

-- 2. Transition all historical documents that were defaulted to CLEAN without genuine scanner attribution
UPDATE documents
SET scan_status = 'LEGACY_UNSCANNED',
    scan_result_details = COALESCE(scan_result_details, 'Legacy document created prior to mandatory malware scanning enforcement')
WHERE scan_status = 'CLEAN' AND (scanned_at IS NULL OR scanner_name IS NULL);
