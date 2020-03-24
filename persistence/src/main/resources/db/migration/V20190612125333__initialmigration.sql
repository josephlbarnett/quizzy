create table users(
    id UUID primary key,
    name text,
    email text,
    auth_crypt text,
    admin bool
);

create table questions(
    id UUID primary key,
    author UUID,
    body text,
    FOREIGN KEY author references (users.id)
);