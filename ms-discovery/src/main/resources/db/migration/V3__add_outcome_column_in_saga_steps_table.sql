alter table orchestrator.saga_steps
    add column if not exists outcome text;