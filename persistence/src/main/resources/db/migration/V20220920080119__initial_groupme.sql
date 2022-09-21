alter table questions add column image_url text default null;

create table groupme_info (
  id UUID NOT NULL /* [jooq ignore start] */ DEFAULT uuid_generate_v4() /* [jooq ignore stop] */,
  instance_id UUID,
  group_id text,
  api_key text,
  UNIQUE (instance_id),
  FOREIGN KEY (instance_id) references instances (id),
  PRIMARY KEY(id)
);