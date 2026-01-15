-- Feed performance indexes (pgvector + chronological sorting)
DO $$
BEGIN
    IF to_regclass('public.file_meta') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_file_meta_uploaded_at
            ON public.file_meta (uploaded_at DESC, id DESC);

        -- Approximate nearest neighbor index for cosine distance (<=>)
        IF EXISTS (SELECT 1 FROM pg_am WHERE amname = 'ivfflat')
           AND EXISTS (SELECT 1 FROM pg_opclass WHERE opcname = 'vector_cosine_ops') THEN
            CREATE INDEX IF NOT EXISTS idx_file_meta_combined_embedding_cosine
                ON public.file_meta
                USING ivfflat (combined_embedding vector_cosine_ops)
                WITH (lists = 100);
        END IF;

        ANALYZE public.file_meta;
    END IF;
END $$;
