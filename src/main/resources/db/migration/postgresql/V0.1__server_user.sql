DO $$
BEGIN
    CREATE USER ${server-username} WITH PASSWORD '${server-password}';
    EXCEPTION WHEN duplicate_object THEN RAISE NOTICE '%, skipping', SQLERRM USING ERRCODE = SQLSTATE;
END
$$;
GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${server-username};