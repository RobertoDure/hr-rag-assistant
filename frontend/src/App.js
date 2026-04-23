import React, { useState, useEffect } from 'react';
import { Alert } from 'react-bootstrap';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';
import axios from 'axios';

import Sidebar from './components/Sidebar';
import Dashboard from './components/Dashboard';
import DocumentUpload from './components/DocumentUpload';
import CandidateList from './components/CandidateList';
import JobAnalysisForm from './components/JobAnalysisForm';
import Login from './components/Login';
import UserRegistration from './components/UserRegistration';
import UserManagement from './components/UserManagement';
import authService from './authService';

function App() {
  const [candidates, setCandidates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('dashboard');
  const [username, setUsername] = useState('User');
  const [userRole, setUserRole] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [authLoading, setAuthLoading] = useState(true);

  useEffect(() => {
    const checkAuth = async () => {
      if (authService.isAuthenticated()) {
        const storedUsername = authService.getUsername();
        const storedRole = authService.getRole();
        if (storedUsername) {
          setUsername(storedUsername);
        }
        if (storedRole) {
          setUserRole(storedRole);
        }
        setIsAuthenticated(true);
        fetchCandidates();
      } else {
        setLoading(false);
      }
      setAuthLoading(false);
    };

    checkAuth();

    const interceptor = axios.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response && error.response.status === 401) {
          handleLogout();
        }
        return Promise.reject(error);
      }
    );

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
    authService.logout();
    setIsAuthenticated(false);
    setUsername('User');
    setUserRole(null);
    setCandidates([]);
  };

  const handleLoginSuccess = (data) => {
    setUsername(data.username || 'User');
    setUserRole(data.role || 'USER');
    setIsAuthenticated(true);
    fetchCandidates();
  };

  if (authLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: 'var(--content-bg)' }}>
        <div style={{ textAlign: 'center' }}>
          <div className="spinner-border" role="status" style={{ width: '2.5rem', height: '2.5rem' }}>
            <span className="visually-hidden">Loading...</span>
          </div>
          <p style={{ marginTop: '16px', color: 'var(--text-secondary)' }}>Loading...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Login onLoginSuccess={handleLoginSuccess} />;
  }

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard':
        return <Dashboard />;
      case 'upload':
        return <DocumentUpload onCandidateAdded={handleCandidateAdded} />;
      case 'candidates':
        return (
          <CandidateList
            candidates={candidates}
            onCandidateDeleted={handleCandidateDeleted}
            loading={loading}
          />
        );
      case 'analysis':
        return <JobAnalysisForm />;
      case 'users':
        return userRole === 'ADMIN' ? <UserManagement /> : <Dashboard />;
      default:
        return <Dashboard />;
    }
  };

  return (
    <div className="App">
      <Sidebar
        activeTab={activeTab}
        onTabChange={setActiveTab}
        username={username}
        userRole={userRole}
        onLogout={handleLogout}
      />

      <main className="app-main">
        <div className="app-content">
          {error && (
            <Alert variant="danger" dismissible onClose={() => setError(null)} className="mb-4">
              {error}
            </Alert>
          )}
          {renderContent()}
        </div>
      </main>
    </div>
  );
}

export default App;
