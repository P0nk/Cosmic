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

-- ============================================================
-- PHASE 2: Cleanup bogus empty storage rows created by the routing bug.
-- Run this AFTER deploying the code fix.
-- These are rows that were incorrectly created as new empty storages
-- because the routing never fired (items are in the real migrated rows).
-- ============================================================

-- Preview first (check that these are the empty bad rows):
SELECT s.storageid, s.accountid, s.characterid, s.slots, s.meso,
       COUNT(ii.inventoryitemid) as item_count
FROM storages s
LEFT JOIN inventoryitems ii ON ii.accountid = s.storageid AND ii.type = 2
WHERE s.world = 1
  AND s.characterid != 0
GROUP BY s.storageid
HAVING item_count = 0 AND s.meso = 0;

-- Once confirmed safe, delete them:
-- DELETE s FROM storages s
-- LEFT JOIN inventoryitems ii ON ii.accountid = s.storageid AND ii.type = 2
-- WHERE s.world = 1
--   AND s.characterid != 0
--   AND ii.inventoryitemid IS NULL
--   AND s.meso = 0;
