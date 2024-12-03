CREATE SEQUENCE IF NOT EXISTS quests_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE quests
(
    id           BIGINT NOT NULL,
    title        TEXT,
    description  TEXT,
    type TEXT,
    videoId TEXT,
    rewardPoints BIGINT,
    channel_id   BIGINT,
    CONSTRAINT pk_quests PRIMARY KEY (id)
);

ALTER TABLE quests
    ADD CONSTRAINT FK_QUESTS_ON_CHANNEL FOREIGN KEY (channel_id) REFERENCES connected_channels (id);

/*
     var id: Long? = null,
    var title: String,
    var description: String,
    var type: String,
    var videoId: String = "",
    var rewardPoints: Long,
 */
