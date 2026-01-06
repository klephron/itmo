DO $$
BEGIN
    IF to_regclass('public.users') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.users TO gateway_auth;
    END IF;
    IF to_regclass('public.user_roles') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_roles TO gateway_auth;
    END IF;
END$$;

DO $$
BEGIN
    IF to_regclass('public.users_id_seq') IS NOT NULL THEN
        GRANT USAGE, SELECT ON SEQUENCE public.users_id_seq TO gateway_auth;
    END IF;
END$$;

DO $$
BEGIN
    IF to_regclass('public.user_follow') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_follow TO profile_app;
    END IF;
END$$;

DO $$
BEGIN
    IF to_regclass('public.user_follow_id_seq') IS NOT NULL THEN
        GRANT USAGE, SELECT ON SEQUENCE public.user_follow_id_seq TO profile_app;
    END IF;
END$$;

DO $$
BEGIN
    IF to_regclass('public.file_meta') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.file_meta TO video_app;
    END IF;
    IF to_regclass('public.file_rendition') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.file_rendition TO video_app;
    END IF;
    IF to_regclass('public.upload_jobs') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.upload_jobs TO video_app;
    END IF;
END$$;

DO $$
BEGIN
    IF to_regclass('public.file_meta_id_seq') IS NOT NULL THEN
        GRANT USAGE, SELECT ON SEQUENCE public.file_meta_id_seq TO video_app;
    END IF;
    IF to_regclass('public.file_rendition_id_seq') IS NOT NULL THEN
        GRANT USAGE, SELECT ON SEQUENCE public.file_rendition_id_seq TO video_app;
    END IF;
END$$;

DO $$
BEGIN
    IF to_regclass('public.video_likes') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.video_likes TO interactions_app;
    END IF;
    IF to_regclass('public.video_views') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.video_views TO interactions_app;
    END IF;
    IF to_regclass('public.video_watch') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.video_watch TO interactions_app;
    END IF;
    IF to_regclass('public.video_comments') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.video_comments TO interactions_app;
    END IF;
    IF to_regclass('public.complaints') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.complaints TO interactions_app;
    END IF;
END$$;

DO $$
BEGIN
    IF to_regclass('public.video_likes_id_seq') IS NOT NULL THEN
        GRANT USAGE, SELECT ON SEQUENCE public.video_likes_id_seq TO interactions_app;
    END IF;
    IF to_regclass('public.video_views_id_seq') IS NOT NULL THEN
        GRANT USAGE, SELECT ON SEQUENCE public.video_views_id_seq TO interactions_app;
    END IF;
    IF to_regclass('public.video_watch_id_seq') IS NOT NULL THEN
        GRANT USAGE, SELECT ON SEQUENCE public.video_watch_id_seq TO interactions_app;
    END IF;
    IF to_regclass('public.video_comments_id_seq') IS NOT NULL THEN
        GRANT USAGE, SELECT ON SEQUENCE public.video_comments_id_seq TO interactions_app;
    END IF;
END$$;
