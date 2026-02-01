CREATE TABLE IF NOT EXISTS public.user_feed_topics (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    term TEXT NOT NULL,
    weight DOUBLE PRECISION NOT NULL DEFAULT 1,
    embedding vector(1024),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_feed_topics_user
    ON public.user_feed_topics (user_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_feed_topics_user_term
    ON public.user_feed_topics (user_id, term);
