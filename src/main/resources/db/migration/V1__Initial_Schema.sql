-- Baseline schema migration for HR RagWiser
-- This migration creates the initial database schema

-- Enable required PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Vector store table for pgvector embeddings
CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1536)
);

CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store USING HNSW (embedding vector_cosine_ops);

-- Candidates table
CREATE TABLE IF NOT EXISTS candidates (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(255),
    cv_content TEXT,
    original_file_name VARCHAR(255),
    skills TEXT[],
    experience TEXT,
    education TEXT,
    years_of_experience INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Job analyses table
CREATE TABLE IF NOT EXISTS job_analyses (
    id VARCHAR(36) PRIMARY KEY,
    job_title VARCHAR(255) NOT NULL,
    job_description TEXT,
    required_skills TEXT[],
    preferred_skills TEXT[],
    experience_level VARCHAR(255),
    education_requirement VARCHAR(255),
    min_years_experience INTEGER,
    max_years_experience INTEGER,
    total_candidates_analyzed INTEGER NOT NULL DEFAULT 0,
    top_candidate_recommendation TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Candidate rankings table - Fixed array column definition
CREATE TABLE IF NOT EXISTS candidate_rankings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_analysis_id VARCHAR(36) NOT NULL,
    candidate_id VARCHAR(36) NOT NULL,
    match_score DOUBLE PRECISION NOT NULL,
    ranking_position INTEGER NOT NULL,
    key_highlights text[],
    FOREIGN KEY (job_analysis_id) REFERENCES job_analyses(id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);

-- Q&A history table
CREATE TABLE IF NOT EXISTS qa_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    context_type VARCHAR(50),
    confidence_score DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Uploaded documents table
CREATE TABLE IF NOT EXISTS uploaded_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT,
    file_path VARCHAR(500),
    status VARCHAR(50) DEFAULT 'UPLOADED',
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

-- Create indexes for better query performance
/*CREATE INDEX IF NOT EXISTS idx_candidates_email ON candidates(email);
CREATE INDEX IF NOT EXISTS idx_candidates_created_at ON candidates(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_candidate_rankings_job_analysis ON candidate_rankings(job_analysis_id);
CREATE INDEX IF NOT EXISTS idx_candidate_rankings_candidate ON candidate_rankings(candidate_id);
CREATE INDEX IF NOT EXISTS idx_candidate_rankings_match_score ON candidate_rankings(match_score DESC);
CREATE INDEX IF NOT EXISTS idx_qa_history_created_at ON qa_history(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_uploaded_documents_status ON uploaded_documents(status);
CREATE INDEX IF NOT EXISTS idx_job_analyses_created_at ON job_analyses(created_at DESC);

-- Add composite index for common queries
CREATE INDEX IF NOT EXISTS idx_candidate_rankings_job_score ON candidate_rankings(job_analysis_id, match_score DESC);*/