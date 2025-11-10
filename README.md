# HR Rag Assistant

An intelligent HR management system that leverages Retrieval-Augmented Generation (RAG) and AI to streamline candidate management, CV analysis, and job matching processes.

## 🎯 Overview

HR Rag Assistant is a full-stack application that combines Spring Boot backend with React frontend to provide comprehensive HR management capabilities. The system uses OpenAI's GPT models and vector embeddings to intelligently analyze candidate CVs, match them with job requirements, and provide AI-powered insights.

## ✨ Key Features

### 📄 Document Processing & RAG
- **CV Upload & Processing**: Upload PDF, DOC, DOCX, and text files
- **Intelligent Text Extraction**: Automatic extraction of candidate information from CVs
- **Vector Storage**: PostgreSQL with pgvector for semantic search capabilities
- **AI-Powered Q&A**: Ask questions about uploaded documents using natural language

![Upload CV](docs/upload_CV.png)

![Uploaded CV](docs/uploaded_CV.png)

### 👥 Candidate Management
- **Complete Candidate Profiles**: Name, email, phone, skills, experience, education
- **Skills Extraction**: Automatic identification of technical and soft skills
- **Experience Analysis**: Years of experience and career history extraction
- **Education Parsing**: Educational background and qualifications

![Candidate Management](docs/candidate_management.png)

### 🔍 Job Analysis & Matching
- **Smart Job Analysis**: Define job requirements with skills, experience, and education criteria
- **Candidate Ranking**: AI-powered scoring and ranking system (0-100% match)
- **Multi-criteria Evaluation**: 
  - Skills matching (40% weight) - Required vs. Preferred skills
  - Experience level (30% weight) - Years and relevance
  - Education requirements (20% weight)
  - CV content relevance (10% weight)
- **AI Recommendations**: Generated insights for top candidates

![Job Analysis](docs/job_analyze.png)

### 📊 Dashboard & Analytics
- **Real-time Metrics**: Total candidates, weekly/monthly uploads, skill analytics
- **Visual Insights**: Top skills distribution, candidate statistics
- **Activity Tracking**: Recent uploads and system activities
- **Comprehensive Logging**: System monitoring with exportable logs

![Dashboard](docs/dashboard.png)


## 🏗️ Architecture

### Backend (Spring Boot 3.3.2)
```
src/main/java/ie/com/rag/
├── Application.java           # Main application entry point
├── Constants.java                     # Application constants and prompts
├── config/
│   └── WebConfig.java                 # CORS and web configuration
├── controller/
│   ├── HRController.java             # HR operations (candidates, analysis, metrics)
│   ├── RagController.java            # RAG Q&A functionality
│   └── RagUploaderController.java    # CV upload and processing
├── dto/
│   ├── CandidateDTO.java            # Candidate data transfer objects
│   ├── JobAnalysisRequestDTO.java   # Job analysis request structure
│   ├── JobAnalysisResponseDTO.java  # Job analysis response structure
│   ├── QAHistoryDTO.java            # Q&A history records
│   ├── RankedCandidateDTO.java      # Ranked candidate results
│   └── UploadedDocumentDTO.java     # Document metadata
└── service/
    ├── CandidateService.java        # Candidate CRUD operations
    ├── DashboardService.java        # Dashboard metrics and analytics
    ├── JobAnalysisService.java     # Job matching and ranking logic
    ├── RagDocumentService.java     # Document processing for RAG
    └── RagUploaderService.java     # CV upload and text extraction
```

### Frontend (React 18.2.0)
```
frontend/src/
├── App.js                           # Main application component
├── components/
│   ├── Dashboard.js                 # Analytics dashboard with comprehensive logging
│   ├── DocumentUpload.js            # CV upload interface
│   ├── CandidateList.js            # Candidate management interface
│   ├── JobAnalysisForm.js          # Job analysis and matching
└── App.css                         # Application styling
```

### Database Schema (PostgreSQL with pgvector)
- **candidates**: Complete candidate profiles with skills array
- **job_analyses**: Job requirements and analysis results
- **candidate_rankings**: Scoring and ranking data for each analysis
- **qa_history**: Q&A interaction history
- **uploaded_documents**: Document metadata and tracking
- **vector_store**: Vector embeddings for semantic search

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Node.js 16+
- Docker & Docker Compose
- OpenAI API Key

### 1. Clone and Setup
```bash

git clone https://github.com/RobertoDure/hr-rag-assistant
cd HR-RagWiser
```

### 2. Environment Configuration
Create `.env` file or set environment variable:
```bash

export OPENAI_API_KEY=your_openai_api_key_here
```

### 3. Start Database
```bash 

docker-compose up -d
```
This starts PostgreSQL with pgvector extension on port 5432.

### 4. Backend Setup
```bash

# Build and run Spring Boot application
./mvnw clean spring-boot:run

# Or using Maven wrapper on Windows
mvnw.cmd clean spring-boot:run
```
Backend runs on `http://localhost:8080`

### 5. Frontend Setup
```bash

cd frontend
npm install
npm start
```
Frontend runs on `http://localhost:3000`

## 🔧 Configuration

### Application Properties (`application.yaml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rag_hr_db
    username: postgres
    password: postgres
  
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
          temperature: 0.3
          max-tokens: 4000
      embedding:
        options:
          model: text-embedding-3-small
    
    vectorstore:
      table: vector_store
      dimension: 1536
      distance: cosine
```

### Docker Compose Services
- **pgvector**: PostgreSQL 16 with vector extension
- **Database**: `rag_hr_db` with automatic schema initialization

## 📋 API Endpoints

### HR Management
- `GET /api/hr/candidates` - Get all candidates
- `GET /api/hr/candidates/{id}` - Get candidate by ID
- `DELETE /api/hr/candidates/{id}` - Delete candidate
- `POST /api/hr/analyze` - Analyze job requirements
- `GET /api/hr/metrics` - Dashboard metrics
- `GET /api/hr/health` - Health check

### Document Upload & RAG
- `POST /api/rag/upload` - Upload CV with metadata
- `GET /api/rag` - Ask questions about documents
- `GET /api/rag/qa-history` - Get Q&A history
- `GET /api/rag/uploaded-documents` - Get document list

## 🎨 User Interface

### Dashboard Features
- **Real-time Statistics**: Candidate counts, recent activity
- **Visual Analytics**: Skills distribution, experience levels
- **System Monitoring**: Comprehensive logging with export functionality
- **Auto-refresh**: Updates every 5 minutes

### CV Upload Process
1. **File Selection**: Drag-and-drop or file picker
2. **Metadata Entry**: Candidate name, email, phone
3. **Processing**: Text extraction and AI analysis
4. **Storage**: Database persistence and vector indexing

### Job Analysis Workflow
1. **Job Definition**: Title, description, requirements
2. **Candidate Matching**: AI-powered scoring algorithm
3. **Results Display**: Ranked candidates with match percentages
4. **AI Recommendations**: Generated insights for top matches

## 🔍 Matching Algorithm

The system uses a sophisticated 4-factor scoring system:

### Skills Matching (40% weight)
- **Required Skills (70%)**: Must-have competencies
- **Preferred Skills (30%)**: Nice-to-have abilities
- Case-insensitive matching with exact keyword detection

### Experience Level (30% weight)
- **Years Validation**: Minimum/maximum experience requirements
- **Penalty System**: Graduated scoring for over/under-qualification
- **Missing Data Handling**: Neutral scoring for incomplete information

### Education Requirements (20% weight)
- **Level Matching**: Degree type and field alignment
- **Hierarchy Recognition**: Higher degrees satisfy lower requirements
- **Partial Credit**: Scoring for related educational backgrounds

### Content Relevance (10% weight)
- **Keyword Analysis**: Job description terminology in CV
- **Semantic Matching**: Context-aware content evaluation
- **Frequency Weighting**: Multiple mention scoring

## 🛠️ Technology Stack

### Backend Technologies
- **Spring Boot 3.3.2**: Application framework
- **Spring AI 1.0.0-M1**: AI integration and RAG capabilities
- **PostgreSQL**: Primary database with JSON support
- **pgvector**: Vector similarity search
- **Spring Data JDBC**: Database access layer
- **OpenAI Integration**: GPT-4 and embedding models

### Frontend Technologies
- **React 18.2.0**: UI framework
- **React Bootstrap**: Component library
- **Axios**: HTTP client
- **React Router**: Navigation
- **React Dropzone**: File upload
- **React Icons**: Icon library

### Development Tools
- **Maven**: Build management
- **Docker Compose**: Development environment
- **Lombok**: Boilerplate reduction
- **SLF4J**: Logging framework

## 📊 Monitoring & Logging

### Dashboard Logging System
- **Comprehensive Tracking**: All user actions and system events
- **Log Levels**: Info, Warning, Error, Debug
- **Persistent Storage**: LocalStorage with 50-entry limit
- **Export Functionality**: JSON format download
- **Performance Metrics**: API response times and duration tracking

### System Health
- **Auto-refresh**: 5-minute intervals
- **Error Handling**: Graceful degradation and user feedback
- **Health Endpoints**: Service status monitoring

## 🔐 Security Considerations

- **CORS Configuration**: Controlled cross-origin access
- **Input Validation**: Comprehensive request validation
- **File Upload Security**: Type and size restrictions (50MB limit)
- **SQL Injection Prevention**: Parameterized queries
- **Text Sanitization**: Null byte and control character removal

## 📈 Performance Optimizations

- **Database Indexing**: Optimized queries for frequent operations
- **Concurrent Processing**: Parallel API calls in frontend
- **Vector Search**: Efficient similarity matching with pgvector
- **Batch Operations**: Optimized bulk data processing
- **Connection Pooling**: Database connection management

## 🚀 Deployment

### Production Considerations
- Set production OpenAI API key
- Configure database connection pooling
- Enable SSL/TLS for secure connections
- Set up application monitoring
- Configure log aggregation
- Implement backup strategies

### Environment Variables
```bash

OPENAI_API_KEY=your_production_api_key
SPRING_PROFILES_ACTIVE=production
DATABASE_URL=your_production_database_url
```

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the application logs via the dashboard
- Review the comprehensive logging system for debugging

---

**HR Rag Assistant** - Transforming HR management with AI-powered intelligence.
