CREATE TABLE IF NOT EXISTS `gambling_pots` (
  `game_key` VARCHAR(50) NOT NULL,
  `current_amount` BIGINT DEFAULT 0,
  PRIMARY KEY (`game_key`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT IGNORE INTO `gambling_pots` (`game_key`, `current_amount`) VALUES
('4D_MESO', 0),
('4D_NX', 0);
