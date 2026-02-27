-- ============================================================
-- Bera Storage Migration: Assign existing storage to oldest character
-- Run this ONCE before deploying the character-specific storage update.
-- ============================================================

-- Step 1: Add characterid column and index
ALTER TABLE storages ADD COLUMN characterid INT NOT NULL DEFAULT 0;
ALTER TABLE storages ADD INDEX idx_char_world (characterid, world);

-- Step 2: Assign each Bera account's existing storage row to the oldest
--         character on that account (lowest character ID = first created)
UPDATE storages s
JOIN (
    SELECT c.accountid, MIN(c.id) AS first_char_id
    FROM characters c
    WHERE c.world = 1
    GROUP BY c.accountid
) fc ON s.accountid = fc.accountid
SET s.characterid = fc.first_char_id
WHERE s.world = 1
  AND s.characterid = 0;

-- Verification query (run after to confirm):
-- SELECT s.storageid, s.accountid, s.characterid, s.world
-- FROM storages s
-- WHERE s.world = 1;
