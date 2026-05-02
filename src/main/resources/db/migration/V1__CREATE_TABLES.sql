create table if not exists event_store (
    id UUID primary key not null,
    aggregate_id UUID not null,
    aggregate_type varchar(255) not null,
    event_type varchar(255) not null,
    event_version int not null,
    payload JSONB not null,
    created_at timestamp with time zone default now() not null
);
