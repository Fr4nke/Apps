-- Run this once in Supabase SQL editor to enable DB stats in the admin panel
-- https://supabase.com/dashboard/project/jghtqgsnevtzxhscfirg/sql/new

CREATE OR REPLACE FUNCTION get_db_stats()
RETURNS jsonb
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT jsonb_build_object(
    'db_size_bytes', pg_database_size(current_database()),
    'active_connections', (
      SELECT count(*)
      FROM pg_stat_activity
      WHERE datname = current_database()
        AND pid <> pg_backend_pid()
    )
  );
$$;
