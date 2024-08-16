create table proof_payloads
(
    address varchar(255) not null,
    payload varchar(255),
    primary key (address)
);

create table subscriptions
(
    durationInDays integer not null,
    id             integer not null,
    price          float4  not null,
    description    varchar(255),
    primary key (id)
);

create table user_subscriptions
(
    subscription_id integer      not null unique,
    expiresAt       timestamp(6),
    address         varchar(255) not null,
    primary key (address)
);

create table videos
(
    duration     bigint       not null,
    fileSize     bigint       not null,
    height       bigint       not null,
    width        bigint       not null,
    description  varchar(255),
    mimeType     varchar(255),
    name         varchar(255),
    thumbnailUrl varchar(255),
    url          varchar(255) not null,
    primary key (url)
);

alter table if exists user_subscriptions
    add constraint FKcrkxok09b5ucoqbd9gpuqy2kb
    foreign key (subscription_id)
    references subscriptions;