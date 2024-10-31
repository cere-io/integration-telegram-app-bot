ALTER TABLE connected_channels
    ADD COLUMN username TEXT;
ALTER TABLE connected_channels
    ADD COLUMN connectedAt TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW();
CREATE INDEX "connected_channels_by_username" ON connected_channels (username);
CREATE INDEX "connected_channels_by_connectedAt" ON connected_channels (connectedAt);

ALTER TABLE user_subscriptions
    ADD COLUMN subscribedAt TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW();
CREATE INDEX "user_subscriptions_by_subscribedAt" ON user_subscriptions (subscribedAt);

CREATE TABLE connected_wallets
(
    channelId BIGINT NOT NULL ,
    address TEXT NOT NULL,
    connectedAt TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_connected_wallets PRIMARY KEY (channelId, address)
);

CREATE INDEX "connected_wallets_by_connectedAt" ON connected_wallets (connectedAt);
