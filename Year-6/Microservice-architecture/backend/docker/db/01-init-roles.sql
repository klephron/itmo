DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'gateway_auth') THEN
        CREATE ROLE gateway_auth LOGIN PASSWORD 'gateway_auth';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'profile_app') THEN
        CREATE ROLE profile_app LOGIN PASSWORD 'profile_app';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'video_app') THEN
        CREATE ROLE video_app LOGIN PASSWORD 'video_app';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'interactions_app') THEN
        CREATE ROLE interactions_app LOGIN PASSWORD 'interactions_app';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'admin_app') THEN
        CREATE ROLE admin_app LOGIN PASSWORD 'admin_app';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'search_app') THEN
        CREATE ROLE search_app LOGIN PASSWORD 'search_app';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'storage_app') THEN
        CREATE ROLE storage_app LOGIN PASSWORD 'storage_app';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'transcoding_app') THEN
        CREATE ROLE transcoding_app LOGIN PASSWORD 'transcoding_app';
    END IF;
END$$;

REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO gateway_auth, profile_app, video_app, interactions_app, admin_app, search_app, storage_app, transcoding_app;
