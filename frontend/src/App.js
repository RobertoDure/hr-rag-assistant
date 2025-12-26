import React, { useState, useEffect } from 'react';
import { Container, Navbar, Nav, Tab, Tabs, Alert, Button } from 'react-bootstrap';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';
import axios from 'axios';

import Dashboard from './components/Dashboard';
import DocumentUpload from './components/DocumentUpload';
import CandidateList from './components/CandidateList';
import JobAnalysisForm from './components/JobAnalysisForm';
import Login from './components/Login';
import authService from './authService';

function App() {
  const [candidates, setCandidates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('dashboard');
  const [username, setUsername] = useState('User');
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [authLoading, setAuthLoading] = useState(true);

  useEffect(() => {
    // Check if user is authenticated
    const checkAuth = async () => {
      if (authService.isAuthenticated()) {
        const storedUsername = authService.getUsername();
        if (storedUsername) {
          setUsername(storedUsername);
        }
        setIsAuthenticated(true);
        fetchCandidates();
      } else {
        setLoading(false);
      }
      setAuthLoading(false);
    };

    checkAuth();

    // Add axios interceptor to handle 401 errors
    const interceptor = axios.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response && error.response.status === 401) {
          // Token expired or invalid - log user out
          handleLogout();
        }
        return Promise.reject(error);
      }
    );

    // Cleanup interceptor on unmount
    return () => {
      axios.interceptors.response.eject(interceptor);
    };
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

  const handleLogout = () => {
    // Clear authentication data
    authService.logout();
    setIsAuthenticated(false);
    setUsername('User');
    setCandidates([]);
  };

  const handleLoginSuccess = (data) => {
    setUsername(data.username || 'User');
    setIsAuthenticated(true);
    fetchCandidates();
  };

  // Show loading spinner while checking authentication
  if (authLoading) {
    return (
      <Container className="d-flex justify-content-center align-items-center" style={{ minHeight: '100vh' }}>
        <div className="text-center">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-3">Loading...</p>
        </div>
      </Container>
    );
  }

  // Show login screen if not authenticated
  if (!isAuthenticated) {
    return <Login onLoginSuccess={handleLoginSuccess} />;
  }

  return (
    <div className="App">
      <Navbar bg="primary" variant="dark" expand="lg" className="mb-4">
        <Container>
          <Navbar.Brand href="#dashboard">
            <strong>HR RagWiser</strong>
          </Navbar.Brand>
          <Navbar.Text className="text-light me-auto">
            Intelligent Candidate Management System
          </Navbar.Text>
          <Navbar.Text className="text-light me-3">
            Welcome, <strong>{username}</strong>
          </Navbar.Text>
          <Button
            variant="outline-light"
            size="sm"
            onClick={handleLogout}
          >
            Logout
          </Button>
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
