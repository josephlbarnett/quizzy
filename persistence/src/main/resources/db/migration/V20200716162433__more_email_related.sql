alter table users add column notify_via_email boolean NOT NULL default FALSE;
alter table users add column password_reset_token text;
