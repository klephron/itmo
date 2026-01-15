-- Migrate embedding vectors to 1024 dims for BAAI/bge-m3
DO $$
BEGIN
    IF to_regclass('public.file_meta') IS NOT NULL THEN
        IF to_regclass('public.idx_file_meta_combined_embedding_cosine') IS NOT NULL THEN
            DROP INDEX public.idx_file_meta_combined_embedding_cosine;
        END IF;

        ALTER TABLE public.file_meta
            ALTER COLUMN title_embedding TYPE vector(1024) USING NULL,
            ALTER COLUMN description_embedding TYPE vector(1024) USING NULL,
            ALTER COLUMN combined_embedding TYPE vector(1024) USING NULL;

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
