import React, { useState, useEffect } from 'react';
import { Container, Navbar, Nav, Tab, Tabs, Alert } from 'react-bootstrap';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';
import axios from 'axios';

import Dashboard from './components/Dashboard';
import DocumentUpload from './components/DocumentUpload';
import CandidateList from './components/CandidateList';
import JobAnalysisForm from './components/JobAnalysisForm';

function App() {
  const [candidates, setCandidates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('dashboard');

  useEffect(() => {
    fetchCandidates();
  }, []);

  const fetchCandidates = async () => {
    try {
      const response = await axios.get('/api/hr/candidates');
      setCandidates(response.data);
      setError(null);
    } catch (err) {
      console.error('Error fetching candidates:', err);
      setError('Failed to load candidates');
    } finally {
      setLoading(false);
    }
  };

  const handleCandidateAdded = (newCandidate) => {
    setCandidates(prevCandidates => [newCandidate, ...prevCandidates]);
  };

  const handleCandidateDeleted = (deletedCandidateId) => {
    setCandidates(prevCandidates =>
      prevCandidates.filter(candidate => candidate.id !== deletedCandidateId)
    );
  };

  return (
    <div className="App">
      <Navbar bg="primary" variant="dark" expand="lg" className="mb-4">
        <Container>
          <Navbar.Brand href="#dashboard">
            <strong>HR RagWiser</strong>
          </Navbar.Brand>
          <Navbar.Text className="text-light">
            Intelligent Candidate Management System
          </Navbar.Text>
        </Container>
      </Navbar>

      <Container fluid>
        {error && (
          <Alert variant="danger" dismissible onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        <Tabs
          activeKey={activeTab}
          onSelect={(k) => setActiveTab(k)}
          className="mb-4"
          variant="pills"
        >
          <Tab eventKey="dashboard" title="📊 Dashboard">
            <Dashboard />
          </Tab>

          <Tab eventKey="upload" title="📄 Upload CV">
            <DocumentUpload onCandidateAdded={handleCandidateAdded} />
          </Tab>

          <Tab eventKey="candidates" title="👥 Candidates">
            <CandidateList
              candidates={candidates}
              onCandidateDeleted={handleCandidateDeleted}
              loading={loading}
            />
          </Tab>

          <Tab eventKey="analysis" title="🔍 Job Analysis">
            <JobAnalysisForm />
          </Tab>
        </Tabs>
      </Container>
    </div>
  );
}

export default App;
