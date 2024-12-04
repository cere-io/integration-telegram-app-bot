CREATE SEQUENCE IF NOT EXISTS campaigns_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE campaigns
(
    id           BIGINT NOT NULL,
    title        TEXT,
    description  TEXT,
    channel_id   BIGINT,
    CONSTRAINT pk_campaigns PRIMARY KEY (id)
);

ALTER TABLE campaigns
    ADD CONSTRAINT FK_CAMPAIGNS_ON_CHANNEL FOREIGN KEY (channel_id) REFERENCES connected_channels (id);

CREATE TABLE campaigns_quests
(
    campaign_id           BIGINT,
    quest_id           BIGINT,
    CONSTRAINT pk_campaigns_quests PRIMARY KEY (campaign_id, quest_id)
);
