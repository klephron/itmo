DO $$
BEGIN
    IF to_regclass('public.user_feed_preferences') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_feed_preferences TO video_app;
    END IF;
END$$;
