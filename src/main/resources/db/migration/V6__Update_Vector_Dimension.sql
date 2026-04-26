-- Update vector_store embedding column from 1536 to 3072 dimensions
-- Required for google/gemini-embedding-exp-03-07 which outputs 3072-dimensional vectors
-- Note: existing vectors must be re-generated after this migration

DROP INDEX IF EXISTS vector_store_embedding_idx;

ALTER TABLE vector_store DROP COLUMN IF EXISTS embedding;
ALTER TABLE vector_store ADD COLUMN embedding vector(3072);

-- HNSW index on vector type is limited to 2000 dimensions in pgvector.
-- For 3072 dimensions, skip index creation to keep migration compatible.
DO $$
BEGIN
	IF 3072 <= 2000 THEN
		EXECUTE 'CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store USING HNSW (embedding vector_cosine_ops)';
	ELSE
		RAISE NOTICE 'Skipping HNSW index creation for 3072-d vectors (pgvector HNSW supports up to 2000 dimensions).';
	END IF;
END
$$;
