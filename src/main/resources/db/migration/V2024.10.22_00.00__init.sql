CREATE SEQUENCE IF NOT EXISTS subscriptions_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS videos_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE bot_users
(
    id              BIGINT  NOT NULL,
    isBot           BOOLEAN NOT NULL,
    firstName       TEXT    NOT NULL,
    chatContextJson TEXT    NOT NULL,
    CONSTRAINT pk_bot_users PRIMARY KEY (id)
);

CREATE TABLE connected_channels
(
    id                      BIGINT NOT NULL,
    title                   TEXT   NOT NULL,
    botDdcAccessTokenBase58 TEXT,
    payoutAddress           TEXT,
    connectedApp            TEXT   NOT NULL,
    CONSTRAINT pk_connected_channels PRIMARY KEY (id)
);

INSERT INTO connected_channels
(id, title, connectedApp) VALUES (-1002433493900, 'test channel', 'cere');

CREATE TABLE proof_payloads
(
    payload TEXT NOT NULL,
    CONSTRAINT pk_proof_payloads PRIMARY KEY (payload)
);

CREATE TABLE subscriptions
(
    id             BIGINT  NOT NULL,
    channel_id     BIGINT,
    durationInDays INTEGER NOT NULL,
    description    TEXT    NOT NULL,
    price          FLOAT   NOT NULL,
    CONSTRAINT pk_subscriptions PRIMARY KEY (id)
);

CREATE TABLE user_subscriptions
(
    subscription_id BIGINT NOT NULL,
    expiresAt       TIMESTAMP WITHOUT TIME ZONE,
    address         TEXT   NOT NULL,
    channelId       BIGINT NOT NULL,
    CONSTRAINT pk_user_subscriptions PRIMARY KEY (address, channelId)
);

CREATE TABLE videos
(
    id           BIGINT NOT NULL,
    channel_id   BIGINT,
    url          TEXT,
    title        TEXT,
    description  TEXT,
    thumbnailUrl TEXT,
    CONSTRAINT pk_videos PRIMARY KEY (id)
);

ALTER TABLE subscriptions
    ADD CONSTRAINT FK_SUBSCRIPTIONS_ON_CHANNEL FOREIGN KEY (channel_id) REFERENCES connected_channels (id);

ALTER TABLE user_subscriptions
    ADD CONSTRAINT FK_USER_SUBSCRIPTIONS_ON_SUBSCRIPTION FOREIGN KEY (subscription_id) REFERENCES subscriptions (id);

ALTER TABLE videos
    ADD CONSTRAINT FK_VIDEOS_ON_CHANNEL FOREIGN KEY (channel_id) REFERENCES connected_channels (id);
