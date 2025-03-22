CREATE TABLE global_monster_drop
(
    id           serial  NOT NULL,
    item_id      integer NOT NULL,
    continent    integer NOT NULL,
    min_quantity integer NOT NULL,
    max_quantity integer NOT NULL,
    quest_id     integer,
    chance       integer NOT NULL,
    PRIMARY KEY (id)
);
GRANT SELECT ON global_monster_drop TO ${server-username};

INSERT INTO global_monster_drop (continent, item_id, min_quantity, max_quantity, quest_id, chance)
VALUES (-1, 4031865, 1, 1, NULL, 35000),
       (-1, 4031866, 1, 1, NULL, 20000),
       (-1, 4001126, 1, 2, NULL, 8000),
       (-1, 2049100, 1, 1, NULL, 1200),
       (-1, 2340000, 1, 1, NULL, 1200),
       (-1, 4001006, 1, 1, NULL, 10000);
