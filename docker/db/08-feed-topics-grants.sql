DO $$
BEGIN
    IF to_regclass('public.user_feed_topics') IS NOT NULL THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_feed_topics TO video_app;
    END IF;
END$$;

DO $$
BEGIN
    IF to_regclass('public.user_feed_topics_id_seq') IS NOT NULL THEN
        GRANT USAGE, SELECT ON SEQUENCE public.user_feed_topics_id_seq TO video_app;
    END IF;
END$$;
