CREATE TABLE account
(
    id              serial                  NOT NULL,
    name            varchar(30)             NOT NULL,
    password        varchar(200)            NOT NULL,
    pin             varchar(4),
    pic             varchar(26),
    created_at      timestamp DEFAULT now() NOT NULL,
    birthdate       date                    NOT NULL,
    tos_accepted    boolean   DEFAULT false NOT NULL,
    gender          smallint,
    chr_slots       smallint                NOT NULL,
    nx_credit       integer   DEFAULT 0     NOT NULL,
    maple_point     integer   DEFAULT 0     NOT NULL,
    nx_prepaid      integer   DEFAULT 0     NOT NULL,
    login_state     smallint                NOT NULL,
    last_login      timestamp,
    banned          boolean   DEFAULT false NOT NULL,
    banned_until    timestamp,
    ban_reason      smallint,
    ban_description text,
    ip              text,
    hwid            text,
    macs            text,
    PRIMARY KEY (id),
    UNIQUE (name)
);
CREATE UNIQUE INDEX lower_account_name_idx ON "account" (lower(name));
GRANT SELECT, INSERT, UPDATE ON TABLE account TO ${server-username};
GRANT USAGE ON SEQUENCE account_id_seq TO ${server-username};
ALTER SEQUENCE account_id_seq RESTART WITH 1000;

INSERT INTO account (id, name, password, pin, pic, birthdate, tos_accepted, chr_slots, login_state)
VALUES (nextval('account_id_seq'), 'admin', '$2y$12$aFD9BDeUocDMY1X4tDYDyeJw/HhkQwCQWs3KAY7gCaRG0cpqJcaL.', '0000', '000000', '2005-05-11', true, 9, 0);
