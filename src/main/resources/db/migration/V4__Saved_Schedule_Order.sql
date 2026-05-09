ALTER TABLE saved_schedule ADD COLUMN IF NOT EXISTS sort_order INTEGER;

UPDATE saved_schedule
SET sort_order = ranked.position
FROM (
    SELECT id, ROW_NUMBER() OVER (ORDER BY updated_at DESC, id ASC) - 1 AS position
    FROM saved_schedule
) ranked
WHERE saved_schedule.id = ranked.id
  AND saved_schedule.sort_order IS NULL;

ALTER TABLE saved_schedule ALTER COLUMN sort_order SET DEFAULT 0;
ALTER TABLE saved_schedule ALTER COLUMN sort_order SET NOT NULL;
