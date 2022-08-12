alter table instances add column default_score int;
update instances set default_score=15;