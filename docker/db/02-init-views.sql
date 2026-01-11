DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'users'
    ) THEN
        EXECUTE 'CREATE OR REPLACE VIEW public.v_users_public AS '
            || 'SELECT id, username, email, display_name, avatar_key, enabled, created_at '
            || 'FROM public.users';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'user_roles'
    ) THEN
        EXECUTE 'CREATE OR REPLACE VIEW public.v_user_roles AS '
            || 'SELECT user_id, role FROM public.user_roles';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'file_meta'
    ) THEN
        EXECUTE 'CREATE OR REPLACE VIEW public.v_file_meta_public AS '
            || 'SELECT id, uuid, name, content_type, content_size, uploader_id, uploader_name, '
            || 'uploaded_at, likes_count, views_count, thumbnail_key, default_height, description '
            || 'FROM public.file_meta';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'file_rendition'
    ) THEN
        EXECUTE 'CREATE OR REPLACE VIEW public.v_file_rendition_public AS '
            || 'SELECT id, file_meta_id, height, content_type, content_size, storage_key '
            || 'FROM public.file_rendition';
    END IF;
END$$;

DO $$
BEGIN
    IF to_regclass('public.v_users_public') IS NOT NULL THEN
        EXECUTE 'GRANT SELECT ON public.v_users_public TO profile_app, admin_app';
    END IF;
    IF to_regclass('public.v_user_roles') IS NOT NULL THEN
        EXECUTE 'GRANT SELECT ON public.v_user_roles TO profile_app, admin_app';
    END IF;
    IF to_regclass('public.v_file_meta_public') IS NOT NULL THEN
        EXECUTE 'GRANT SELECT ON public.v_file_meta_public TO interactions_app, search_app, admin_app';
    END IF;
    IF to_regclass('public.v_file_rendition_public') IS NOT NULL THEN
        EXECUTE 'GRANT SELECT ON public.v_file_rendition_public TO interactions_app, search_app, admin_app';
    END IF;
END$$;
