alter table instances
    add column default_question_type text,
    add column auto_grade boolean;
update instances
    set default_question_type = 'SHORT_ANSWER',
    auto_grade = FALSE;
alter table questions add column type text;
update questions set type = 'SHORT_ANSWER';

create table answer_choices (
    id UUID NOT NULL /* [jooq ignore start] */ DEFAULT uuid_generate_v4() /* [jooq ignore stop] */,
    question_id UUID,
    letter varchar(5),
    answer text,

    FOREIGN KEY (question_id) references questions (id),
    UNIQUE (question_id, letter),
    PRIMARY KEY (id)
);