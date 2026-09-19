CREATE TYPE board_type AS ENUM ('NOTICE', 'GENERAL', 'COMMITTEE');
CREATE TYPE review_status AS ENUM ('PENDING', 'APPROVED');
CREATE TYPE draft_type AS ENUM ('SIGNUP', 'POST');
COMMENT ON TYPE board_type IS 'Board categories.';

CREATE TABLE committee
(
    id         uuid         NOT NULL PRIMARY KEY,
    name       varchar(120) NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now(),
    budget     numeric(14, 0) NULL,
    share      numeric(5, 2)  NULL
);
COMMENT ON TABLE committee IS 'Standing committees.';

CREATE TABLE board
(
    id            uuid        NOT NULL PRIMARY KEY,
    name          varchar(160) NOT NULL,
    board_type    board_type  NOT NULL,
    cohort_number integer     NULL,
    committee_id  uuid        NULL REFERENCES committee (id),
    active        boolean     NOT NULL DEFAULT true,
    display_order integer     NOT NULL DEFAULT 0,
    created_at    timestamptz NOT NULL DEFAULT now()
);
COMMENT ON COLUMN board.board_type IS 'NOTICE allows guest reading; "quoted" words stay.';

CREATE TABLE "user"
(
    id            uuid        NOT NULL PRIMARY KEY,
    login_id      varchar(64) NOT NULL,
    password_hash text        NOT NULL,
    tags          text[]      NULL,
    profile       jsonb       NULL,
    view_count    bigint      NOT NULL DEFAULT 0,
    birth_date    date        NULL,
    referrer_id   uuid        NULL REFERENCES "user" (id)
);

CREATE TABLE event_rsvp
(
    event_id uuid          NOT NULL REFERENCES committee (id),
    user_id  uuid          NOT NULL REFERENCES "user" (id),
    status   review_status NOT NULL,
    PRIMARY KEY (event_id, user_id)
);

CREATE TABLE feed_mention
(
    feed_id uuid NOT NULL REFERENCES board (id),
    user_id uuid NOT NULL REFERENCES "user" (id)
);

CREATE TABLE flyway_schema_history
(
    installed_rank integer NOT NULL PRIMARY KEY,
    version        varchar(50)
);
