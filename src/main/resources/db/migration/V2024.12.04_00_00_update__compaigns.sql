ALTER TABLE campaigns ADD COLUMN start_date DATETIME NULL;
ALTER TABLE campaigns ADD COLUMN end_date DATETIME NULL;

ALTER TABLE campaigns ALTER COLUMN start_date SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE campaigns ALTER COLUMN end_date SET DEFAULT CURRENT_TIMESTAMP;
