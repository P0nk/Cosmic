-- ============================================================
-- Subscriber System Migration
-- Run once to set up the subscription table and add passive
-- speed/jump columns to the characters table.
-- ============================================================

-- 1. New subscription tracking table
CREATE TABLE IF NOT EXISTS cosmic.cosmic_subscriptions (
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    characterid           INT NOT NULL UNIQUE,
    tier                  ENUM('MONTHLY','ANNUAL') NOT NULL DEFAULT 'MONTHLY',
    started_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at            DATETIME NOT NULL,
    accumulated_stat_points INT NOT NULL DEFAULT 0,  -- total granted since ever
    unspent_stat_points   INT NOT NULL DEFAULT 0,    -- not yet allocated
    INDEX idx_sub_characterid (characterid)
);

-- 2. Add passive speed + jump to characters table
-- (passive_watk, passive_matk, passive_wdef, passive_mdef, passive_acc, passive_eva already exist)
ALTER TABLE cosmic.characters
    ADD COLUMN IF NOT EXISTS passive_speed INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS passive_jump  INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS passive_str   INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS passive_dex   INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS passive_int   INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS passive_luk   INT NOT NULL DEFAULT 0;
