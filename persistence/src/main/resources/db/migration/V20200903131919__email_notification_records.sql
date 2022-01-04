create table email_notifications(
    id UUID NOT NULL /* [jooq ignore start] */ DEFAULT uuid_generate_v4() /* [jooq ignore stop] */,
    notification_type varchar(100),
    question_id UUID,

    FOREIGN KEY (question_id) references questions (id),
    UNIQUE (question_id, notification_type),
    PRIMARY KEY (id)
);

insert into email_notifications (
    notification_type, question_id
) (select 'REMINDER', id
    from questions where sent_reminder
);

insert into email_notifications (
    notification_type, question_id
) (select 'ANSWER', id
    from questions where sent_answer
);
