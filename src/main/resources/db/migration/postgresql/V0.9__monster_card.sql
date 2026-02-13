CREATE TABLE monster_card
(
    chr_id  integer  NOT NULL,
    card_id integer  NOT NULL,
    level   smallint NOT NULL,
    PRIMARY KEY(chr_id, card_id)
    /* TODO once chr is moved to postgres:
      CONSTRAINT fk_monster_card_chr FOREIGN KEY (chr_id) REFERENCES chr(id);
    */
);
GRANT SELECT, INSERT, UPDATE ON TABLE monster_card TO ${server-username};
