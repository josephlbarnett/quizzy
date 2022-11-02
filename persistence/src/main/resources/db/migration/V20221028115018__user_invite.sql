create table user_invite(
  id UUID NOT NULL /* [jooq ignore start] */ DEFAULT uuid_generate_v4() /* [jooq ignore stop] */,
  instance_id UUID,
  status text,
  FOREIGN KEY (instance_id) references instances (id),
  PRIMARY KEY(id)
);