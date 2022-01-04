create table grades(
    id UUID NOT NULL /* [jooq ignore start] */ DEFAULT uuid_generate_v4() /* [jooq ignore stop] */,
    response_id UUID,
    correct bool,
    bonus int,
    FOREIGN KEY (response_id) references responses (id),
    PRIMARY KEY(id)
);

insert into grades (response_id, correct, bonus) (select id, correct, bonus from responses where correct is not null);