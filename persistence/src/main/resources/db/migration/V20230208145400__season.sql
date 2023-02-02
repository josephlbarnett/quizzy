create table seasons
(
    id          UUID NOT NULL /* [jooq ignore start] */ DEFAULT uuid_generate_v4() /* [jooq ignore stop] */,
    instance_id UUID,
    name        varchar(255),
    start_time  TIMESTAMP WITH TIME ZONE,
    end_time    TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (instance_id) references instances (id),
    UNIQUE (instance_id, name),
    PRIMARY KEY (id)
);