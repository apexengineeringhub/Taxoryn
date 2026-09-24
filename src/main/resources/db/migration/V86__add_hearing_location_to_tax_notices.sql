-- Taxoryn Platform Migration V86
-- Add hearing location to tax notices.

ALTER TABLE tax_notices
    ADD COLUMN IF NOT EXISTS hearing_location VARCHAR(255);



ALTER TABLE tax_notices
    ADD COLUMN IF NOT EXISTS hearing_mode VARCHAR(50);


ALTER TABLE tax_notices
    ADD COLUMN IF NOT EXISTS hearing_notes TEXT;

ALTER TABLE tax_notices
    ADD COLUMN IF NOT EXISTS hearing_outcome TEXT;

ALTER TABLE tax_notices
    ADD COLUMN IF NOT EXISTS hearing_reference VARCHAR(255);

 ALTER TABLE tax_notices
     ADD COLUMN IF NOT EXISTS hearing_required BOOLEAN;

 ALTER TABLE tax_notices
     ADD COLUMN IF NOT EXISTS hearing_status VARCHAR(50);

 ALTER TABLE tax_notices
     ADD COLUMN IF NOT EXISTS response_draft TEXT;

 ALTER TABLE tax_notices
     ADD COLUMN IF NOT EXISTS response_required BOOLEAN;

 ALTER TABLE tax_notices
     ADD COLUMN IF NOT EXISTS response_status VARCHAR(50);

 ALTER TABLE tax_notices
     ADD COLUMN IF NOT EXISTS response_submitted_at TIMESTAMPTZ;

ALTER TABLE tax_notices
    ADD COLUMN IF NOT EXISTS response_submitted_by UUID;

ALTER TABLE tax_notices
    ADD COLUMN IF NOT EXISTS submission_notes TEXT;

 ALTER TABLE tax_notices
     ADD COLUMN IF NOT EXISTS submission_reference VARCHAR(255);