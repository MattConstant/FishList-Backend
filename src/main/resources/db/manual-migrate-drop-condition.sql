-- Optional manual cleanup after removing the unused Condition JPA entity.
-- Hibernate + SpringPhysicalNamingStrategy typically maps entity "Condition" to table "condition"
-- on PostgreSQL. JPA ddl-auto=update does not drop unused tables automatically.
--
-- Run once on Postgres/Supabase (SQL editor) when you are ready to reclaim storage:

DROP TABLE IF EXISTS condition CASCADE;
