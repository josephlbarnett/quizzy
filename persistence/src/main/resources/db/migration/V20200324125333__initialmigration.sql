create table instances(
    id UUID NOT NULL /* [jooq ignore start] */ DEFAULT uuid_generate_v4() /* [jooq ignore stop] */,
    name varchar(255),
    status text,
    UNIQUE (name),
    PRIMARY KEY(id)
);

create table users(
    id UUID NOT NULL /* [jooq ignore start] */ DEFAULT uuid_generate_v4() /* [jooq ignore stop] */,
    instance_id UUID,
    name text,
    email varchar(255),
    auth_crypt text,
    admin bool,
    time_zone_id varchar(255),
    UNIQUE (email),
    FOREIGN KEY (instance_id) references instances (id),
    PRIMARY KEY(id)
);

create table questions(
    id UUID NOT NULL /* [jooq ignore start] */ DEFAULT uuid_generate_v4() /* [jooq ignore stop] */,
    author_id UUID,
    body text,
    answer text,
    rule_references text,
    active_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (author_id) references users (id),
    PRIMARY KEY(id)
);

create table responses(
    id UUID NOT NULL /* [jooq ignore start] */ DEFAULT uuid_generate_v4() /* [jooq ignore stop] */,
    user_id UUID,
    question_id UUID,
    response text,
    rule_references text,
    correct bool,
    bonus int,
    FOREIGN KEY (user_id) references users (id),
    FOREIGN KEY (question_id) references questions (id),
    UNIQUE (user_id, question_id),
    PRIMARY KEY(id)
);

create table sessions(
    id UUID NOT NULL /* [jooq ignore start] */ DEFAULT uuid_generate_v4() /* [jooq ignore stop] */,
    user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (user_id) references users (id),
    PRIMARY KEY(id)
);