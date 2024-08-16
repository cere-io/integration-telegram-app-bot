create table proof_payloads
(
    payload text not null,
    primary key (payload)
);

create table subscriptions
(
    durationInDays integer not null,
    id             integer not null,
    price          float4  not null,
    description    text    not null,
    primary key (id)
);

create table user_subscriptions
(
    subscription_id integer      not null,
    expiresAt       timestamp(6) not null,
    address         text         not null,
    primary key (address)
);

create table videos
(
    duration     bigint not null,
    fileSize     bigint not null,
    height       bigint not null,
    width        bigint not null,
    description  text   not null,
    mimeType     text,
    name         text   not null,
    thumbnailUrl text,
    url          text   not null,
    primary key (url)
);

alter table if exists user_subscriptions
    add constraint FKcrkxok09b5ucoqbd9gpuqy2kb
    foreign key (subscription_id)
    references subscriptions;