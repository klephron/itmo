DO $$
BEGIN
    IF to_regclass('public.file_meta') IS NOT NULL THEN
        ALTER TABLE public.file_meta
            ADD COLUMN IF NOT EXISTS llm_tags text,
            ADD COLUMN IF NOT EXISTS llm_flagged boolean DEFAULT false,
            ADD COLUMN IF NOT EXISTS llm_flags text;
    END IF;
END $$;
