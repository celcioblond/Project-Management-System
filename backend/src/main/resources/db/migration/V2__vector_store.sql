-- Table used by Spring AI's PgVectorStore (same layout it creates itself).
-- IF NOT EXISTS keeps this safe on databases where Spring AI already created it.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store
(
    id        UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding VECTOR(1536)
);

CREATE INDEX IF NOT EXISTS spring_ai_vector_index ON vector_store USING HNSW (embedding vector_cosine_ops);
