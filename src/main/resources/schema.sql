CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Table for storing uploaded document metadata
CREATE TABLE IF NOT EXISTS uploaded_documents (
    id VARCHAR(255) PRIMARY KEY,
    file_name VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    upload_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for storing candidate information
CREATE TABLE IF NOT EXISTS candidates (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    cv_content TEXT,
    original_file_name VARCHAR(500),
    skills TEXT[],
    experience TEXT,
    education TEXT,
    years_of_experience INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for QA history
CREATE TABLE IF NOT EXISTS qa_history (
    id VARCHAR(255) PRIMARY KEY,
    candidate_id VARCHAR(255) REFERENCES candidates(id),
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for storing job analysis results
CREATE TABLE IF NOT EXISTS job_analyses (
    id VARCHAR(255) PRIMARY KEY,
    job_title VARCHAR(255) NOT NULL,
    job_description TEXT,
    required_skills TEXT[],
    preferred_skills TEXT[],
    experience_level VARCHAR(100),
    education_requirement VARCHAR(255),
    min_years_experience INTEGER,
    max_years_experience INTEGER,
    total_candidates_analyzed INTEGER NOT NULL,
    top_candidate_recommendation TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for storing candidate rankings for each job analysis
CREATE TABLE IF NOT EXISTS candidate_rankings (
    id SERIAL PRIMARY KEY,
    job_analysis_id VARCHAR(255) NOT NULL REFERENCES job_analyses(id) ON DELETE CASCADE,
    candidate_id VARCHAR(255) NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    match_score DECIMAL(5,2) NOT NULL,
    ranking_position INTEGER NOT NULL,
    key_highlights TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(job_analysis_id, candidate_id)
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_candidates_email ON candidates(email);
CREATE INDEX IF NOT EXISTS idx_qa_history_candidate_id ON qa_history(candidate_id);
CREATE INDEX IF NOT EXISTS idx_uploaded_documents_timestamp ON uploaded_documents(upload_timestamp);
CREATE INDEX IF NOT EXISTS idx_job_analyses_created_at ON job_analyses(created_at);
CREATE INDEX IF NOT EXISTS idx_candidate_rankings_job_analysis_id ON candidate_rankings(job_analysis_id);
CREATE INDEX IF NOT EXISTS idx_candidate_rankings_candidate_id ON candidate_rankings(candidate_id);
CREATE INDEX IF NOT EXISTS idx_candidate_rankings_match_score ON candidate_rankings(match_score DESC);
