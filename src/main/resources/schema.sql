-- Drop tables if they exist (in correct order to handle foreign key constraints)

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DROP TABLE IF EXISTS candidate_rankings;
DROP TABLE IF EXISTS qa_history;
DROP TABLE IF EXISTS uploaded_documents;
DROP TABLE IF EXISTS job_analyses;
DROP TABLE IF EXISTS candidates;

CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1536)
    );

CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);

-- Create candidates table
CREATE TABLE candidates (
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

-- Create job_analyses table
CREATE TABLE job_analyses (
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

-- Create candidate_rankings table
CREATE TABLE candidate_rankings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_analysis_id VARCHAR(36) NOT NULL,
    candidate_id VARCHAR(36) NOT NULL,
    match_score DOUBLE PRECISION NOT NULL,
    ranking_position INTEGER NOT NULL,
    key_highlights TEXT[],
    FOREIGN KEY (job_analysis_id) REFERENCES job_analyses(id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);

-- Create qa_history table
CREATE TABLE qa_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    context_type VARCHAR(50),
    confidence_score DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create uploaded_documents table
CREATE TABLE uploaded_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT,
    file_path VARCHAR(500),
    status VARCHAR(50) DEFAULT 'UPLOADED',
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);


-- Create indexes for better performance
CREATE INDEX idx_candidates_email ON candidates(email);
CREATE INDEX idx_candidate_rankings_job_analysis ON candidate_rankings(job_analysis_id);
CREATE INDEX idx_candidate_rankings_candidate ON candidate_rankings(candidate_id);
CREATE INDEX idx_candidate_rankings_match_score ON candidate_rankings(match_score DESC);
CREATE INDEX idx_qa_history_created_at ON qa_history(created_at DESC);
CREATE INDEX idx_uploaded_documents_status ON uploaded_documents(status);