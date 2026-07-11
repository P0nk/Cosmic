/*  Manually run this script in MySQL Workbench or some other database client
    to migrate your old (pre Jan 19th 2022) monsterbook table to the new version */
ALTER TABLE cosmic.`monsterbook`
CHANGE COLUMN `charid` `charid` INT(11) NOT NULL,
ADD PRIMARY KEY (`charid`, `cardid`),
ADD CONSTRAINT `FK_monsterbook_1` FOREIGN KEY (`charid`) REFERENCES `characters` (`id`) ON UPDATE CASCADE ON DELETE CASCADE;