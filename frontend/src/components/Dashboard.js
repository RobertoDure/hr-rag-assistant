import React, { useState, useEffect, useCallback } from 'react';
import {
  Container, Row, Col, Card, Alert, Spinner, Badge, Button,
  ListGroup, ProgressBar, Table, Dropdown, Modal
} from 'react-bootstrap';
import {
  FaUsers, FaUpload, FaClipboardList,
  FaChartLine, FaClock, FaExclamationTriangle, FaInfoCircle,
  FaTimesCircle, FaRedo, FaBug, FaDownload
} from 'react-icons/fa';
import axios from 'axios';

const Dashboard = () => {
  // State management with comprehensive logging
  const [dashboardData, setDashboardData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastRefresh, setLastRefresh] = useState(null);
  const [refreshing, setRefreshing] = useState(false);
  const [logs, setLogs] = useState([]);
  const [showLogsModal, setShowLogsModal] = useState(false);

  // Comprehensive logging function
  const addLog = useCallback((level, message, data = null, error = null) => {
    const timestamp = new Date().toISOString();
    const logEntry = {
      id: Date.now() + Math.random(),
      timestamp,
      level, // 'info', 'warn', 'error', 'debug'
      message,
      data: data ? JSON.stringify(data, null, 2) : null,
      error: error ? {
        message: error.message,
        stack: error.stack,
        name: error.name
      } : null
    };

    console.log(`[${level.toUpperCase()}] ${timestamp}: ${message}`, data || '', error || '');

    setLogs(prevLogs => {
      const newLogs = [logEntry, ...prevLogs.slice(0, 49)]; // Keep last 50 logs

      // Store logs in localStorage for persistence
      try {
        localStorage.setItem('hr-ragwiser-logs', JSON.stringify(newLogs));
      } catch (e) {
        console.warn('Failed to store logs in localStorage:', e);
      }

      return newLogs;
    });
  }, []);

  // Load logs from localStorage on component mount
  useEffect(() => {
    try {
      const storedLogs = localStorage.getItem('hr-ragwiser-logs');
      if (storedLogs) {
        const parsedLogs = JSON.parse(storedLogs);
        setLogs(parsedLogs);
        addLog('info', 'Dashboard initialized with stored logs', { logCount: parsedLogs.length });
      } else {
        addLog('info', 'Dashboard initialized - no stored logs found');
      }
    } catch (e) {
      addLog('error', 'Failed to load stored logs', null, e);
    }
  }, [addLog]);

  // Fetch dashboard data with comprehensive error handling
  const fetchDashboardData = useCallback(async (isRefresh = false) => {
    const startTime = performance.now();

    if (isRefresh) {
      setRefreshing(true);
      addLog('info', 'Manual dashboard refresh initiated');
    } else {
      setLoading(true);
      addLog('info', 'Initial dashboard data fetch started');
    }

    setError(null);

    try {
      addLog('debug', 'Fetching dashboard statistics');

      // Fetch multiple endpoints concurrently
      const requests = [
        axios.get('/api/hr/metrics').catch(err => {
          addLog('warn', 'Dashboard metrics endpoint failed, using fallback', null, err);
          return { data: null };
        }),
        axios.get('/api/hr/candidates').catch(err => {
          addLog('error', 'Candidates endpoint failed', null, err);
          throw err;
        })
      ];

      addLog('debug', 'Executing concurrent API requests', { requestCount: requests.length });

      const [metricsResponse, candidatesResponse] =
        await Promise.all(requests);

      const endTime = performance.now();
      const fetchDuration = Math.round(endTime - startTime);

      // Process the data
      const candidates = candidatesResponse.data || [];
      const metrics = metricsResponse.data || {};

      // Calculate derived statistics
      const derivedStats = {
        totalCandidates: candidates.length,
        candidatesThisWeek: candidates.filter(c =>
          new Date(c.createdAt) > new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
        ).length,
        candidatesThisMonth: candidates.filter(c =>
          new Date(c.createdAt) > new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)
        ).length,
        avgSkillsPerCandidate: candidates.length > 0
          ? Math.round(candidates.reduce((acc, c) => acc + (c.skills?.length || 0), 0) / candidates.length)
          : 0,
        topSkills: getTopSkills(candidates),
        recentUploads: candidates.filter(c =>
          new Date(c.createdAt) > new Date(Date.now() - 24 * 60 * 60 * 1000)
        )
      };

      const dashboardPayload = {
        stats: { ...metrics, ...derivedStats },
        candidates: candidates.slice(0, 10), // Latest 10 candidates
        recentActivities: [], // No recent activities endpoint available
        systemInfo: {},
        lastUpdated: new Date().toISOString(),
        fetchDuration
      };

      setDashboardData(dashboardPayload);
      setLastRefresh(new Date());

      addLog('info', 'Dashboard data fetch completed successfully', {
        fetchDuration: `${fetchDuration}ms`,
        candidateCount: candidates.length,
        metricsKeys: Object.keys(metrics)
      });

    } catch (error) {
      const endTime = performance.now();
      const fetchDuration = Math.round(endTime - startTime);

      setError('Failed to load dashboard data. Please check your connection and try again.');
      addLog('error', 'Dashboard data fetch failed', {
        fetchDuration: `${fetchDuration}ms`,
        isRefresh,
        errorCode: error.response?.status,
        errorMessage: error.response?.data?.message
      }, error);

      // Set partial data if available
      if (error.response?.status !== 500) {
        setDashboardData({
          stats: { totalCandidates: 0, candidatesThisWeek: 0, candidatesThisMonth: 0 },
          candidates: [],
          recentActivities: [],
          systemInfo: {},
          lastUpdated: new Date().toISOString(),
          fetchDuration,
          hasError: true
        });
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [addLog]);

  // Helper function to get top skills
  const getTopSkills = (candidates) => {
    addLog('debug', 'Calculating top skills from candidates', { candidateCount: candidates.length });

    const skillCount = {};
    candidates.forEach(candidate => {
      if (candidate.skills) {
        candidate.skills.forEach(skill => {
          skillCount[skill] = (skillCount[skill] || 0) + 1;
        });
      }
    });

    const topSkills = Object.entries(skillCount)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 5)
      .map(([skill, count]) => ({ skill, count }));

    addLog('debug', 'Top skills calculated', { topSkillsCount: topSkills.length, totalUniqueSkills: Object.keys(skillCount).length });

    return topSkills;
  };

  // Manual refresh function
  const handleRefresh = () => {
    addLog('info', 'Manual refresh triggered by user');
    fetchDashboardData(true);
  };

  // Clear logs function
  const clearLogs = () => {
    addLog('info', 'Logs cleared by user', { previousLogCount: logs.length });
    setLogs([]);
    localStorage.removeItem('hr-ragwiser-logs');
  };

  // Export logs function
  const exportLogs = () => {
    try {
      const logsData = JSON.stringify(logs, null, 2);
      const blob = new Blob([logsData], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `hr-ragwiser-logs-${new Date().toISOString().split('T')[0]}.json`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      addLog('info', 'Logs exported successfully', { logCount: logs.length });
    } catch (error) {
      addLog('error', 'Failed to export logs', null, error);
    }
  };

  // Initial data fetch
  useEffect(() => {
    addLog('info', 'Dashboard component mounted, starting initial data fetch');
    fetchDashboardData();
  }, [fetchDashboardData]);

  // Auto-refresh every 5 minutes
  useEffect(() => {
    addLog('debug', 'Setting up auto-refresh interval (5 minutes)');

    const interval = setInterval(() => {
      addLog('info', 'Auto-refresh triggered');
      fetchDashboardData(true);
    }, 5 * 60 * 1000);

    return () => {
      addLog('debug', 'Clearing auto-refresh interval');
      clearInterval(interval);
    };
  }, [fetchDashboardData]);

  // Render log level badge
  const renderLogLevelBadge = (level) => {
    const variants = {
      info: 'info',
      warn: 'warning',
      error: 'danger',
      debug: 'secondary'
    };

    const icons = {
      info: <FaInfoCircle />,
      warn: <FaExclamationTriangle />,
      error: <FaTimesCircle />,
      debug: <FaBug />
    };

    return (
      <Badge bg={variants[level]} className="d-flex align-items-center">
        {icons[level]}
        <span className="ms-1">{level}</span>
      </Badge>
    );
  };

  if (loading && !dashboardData) {
    return (
      <Container>
        <div className="text-center py-5">
          <Spinner animation="border" variant="primary" className="mb-3" />
          <h4>Loading Dashboard...</h4>
          <p className="text-muted">Fetching the latest data</p>
        </div>
      </Container>
    );
  }

  return (
    <Container fluid>
      {/* Header with refresh and logs */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2>
            <FaChartLine className="me-2 text-primary" />
            Dashboard
          </h2>
          {lastRefresh && (
            <small className="text-muted">
              <FaClock className="me-1" />
              Last updated: {lastRefresh.toLocaleTimeString()}
            </small>
          )}
        </div>

        <div className="d-flex gap-2">
          <Button
            variant="outline-secondary"
            size="sm"
            onClick={() => setShowLogsModal(true)}
            title="View system logs"
          >
            <FaBug className="me-1" />
            Logs ({logs.length})
          </Button>

          <Button
            variant="outline-primary"
            size="sm"
            onClick={handleRefresh}
            disabled={refreshing}
            title="Refresh dashboard data"
          >
            <FaRedo className={`me-1 ${refreshing ? 'fa-spin' : ''}`} />
            {refreshing ? 'Refreshing...' : 'Refresh'}
          </Button>
        </div>
      </div>

      {error && (
        <Alert variant="danger" className="mb-4" dismissible onClose={() => setError(null)}>
          <FaExclamationTriangle className="me-2" />
          {error}
        </Alert>
      )}

      {dashboardData && (
        <>
          {/* Statistics Cards */}
          <Row className="mb-4">
            <Col md={3}>
              <Card className="text-center border-0 shadow-sm">
                <Card.Body>
                  <FaUsers size={40} className="text-primary mb-2" />
                  <h3 className="mb-0">{dashboardData.stats.totalCandidates || 0}</h3>
                  <small className="text-muted">Total Candidates</small>
                </Card.Body>
              </Card>
            </Col>

            <Col md={3}>
              <Card className="text-center border-0 shadow-sm">
                <Card.Body>
                  <FaUpload size={40} className="text-success mb-2" />
                  <h3 className="mb-0">{dashboardData.stats.candidatesThisWeek || 0}</h3>
                  <small className="text-muted">This Week</small>
                </Card.Body>
              </Card>
            </Col>

            <Col md={3}>
              <Card className="text-center border-0 shadow-sm">
                <Card.Body>
                  <FaClipboardList size={40} className="text-warning mb-2" />
                  <h3 className="mb-0">{dashboardData.stats.candidatesThisMonth || 0}</h3>
                  <small className="text-muted">This Month</small>
                </Card.Body>
              </Card>
            </Col>

            <Col md={3}>
              <Card className="text-center border-0 shadow-sm">
                <Card.Body>
                  <FaChartLine size={40} className="text-info mb-2" />
                  <h3 className="mb-0">{dashboardData.stats.avgSkillsPerCandidate || 0}</h3>
                  <small className="text-muted">Avg Skills/Candidate</small>
                </Card.Body>
              </Card>
            </Col>
          </Row>

          <Row>
            {/* Recent Candidates */}
            <Col lg={6}>
              <Card className="mb-4">
                <Card.Header>
                  <FaUsers className="me-2" />
                  Recent Candidates
                </Card.Header>
                <Card.Body>
                  {dashboardData.candidates.length > 0 ? (
                    <ListGroup variant="flush">
                      {dashboardData.candidates.map(candidate => (
                        <ListGroup.Item key={candidate.id} className="d-flex justify-content-between align-items-center px-0">
                          <div>
                            <strong>{candidate.name}</strong>
                            <br />
                            <small className="text-muted">{candidate.email}</small>
                          </div>
                          <div className="text-end">
                            <Badge bg="primary">
                              {candidate.skills?.length || 0} skills
                            </Badge>
                            <br />
                            <small className="text-muted">
                              {new Date(candidate.createdAt).toLocaleDateString()}
                            </small>
                          </div>
                        </ListGroup.Item>
                      ))}
                    </ListGroup>
                  ) : (
                    <div className="text-center py-3">
                      <FaUsers size={40} className="text-muted mb-2" />
                      <p className="text-muted">No candidates uploaded yet</p>
                    </div>
                  )}
                </Card.Body>
              </Card>
            </Col>

            {/* Top Skills & System Info */}
            <Col lg={6}>
              <Card className="mb-4">
                <Card.Header>
                  <FaChartLine className="me-2" />
                  Top Skills
                </Card.Header>
                <Card.Body>
                  {dashboardData.stats.topSkills?.length > 0 ? (
                    <div>
                      {dashboardData.stats.topSkills.map((skillData, index) => (
                        <div key={skillData.skill} className="mb-3">
                          <div className="d-flex justify-content-between align-items-center mb-1">
                            <span>{skillData.skill}</span>
                            <Badge bg="secondary">{skillData.count}</Badge>
                          </div>
                          <ProgressBar
                            now={(skillData.count / dashboardData.stats.totalCandidates) * 100}
                            variant={index === 0 ? 'primary' : 'secondary'}
                            style={{ height: '6px' }}
                          />
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="text-center py-3">
                      <FaChartLine size={40} className="text-muted mb-2" />
                      <p className="text-muted">No skills data available</p>
                    </div>
                  )}
                </Card.Body>
              </Card>
            </Col>
          </Row>
        </>
      )}

      {/* Logs Modal */}
      <Modal show={showLogsModal} onHide={() => setShowLogsModal(false)} size="xl">
        <Modal.Header closeButton>
          <Modal.Title>
            <FaBug className="me-2" />
            System Logs ({logs.length})
          </Modal.Title>
        </Modal.Header>
        <Modal.Body style={{ maxHeight: '500px', overflowY: 'auto' }}>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <div>
              <small className="text-muted">
                Logs are automatically stored and limited to the most recent 50 entries
              </small>
            </div>
            <div>
              <Button variant="outline-secondary" size="sm" onClick={exportLogs} className="me-2">
                <FaDownload className="me-1" />
                Export
              </Button>
              <Button variant="outline-danger" size="sm" onClick={clearLogs}>
                Clear All
              </Button>
            </div>
          </div>

          {logs.length > 0 ? (
            <Table responsive striped size="sm">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Level</th>
                  <th>Message</th>
                  <th>Details</th>
                </tr>
              </thead>
              <tbody>
                {logs.map(log => (
                  <tr key={log.id}>
                    <td style={{ minWidth: '120px' }}>
                      <small>{new Date(log.timestamp).toLocaleTimeString()}</small>
                    </td>
                    <td>
                      {renderLogLevelBadge(log.level)}
                    </td>
                    <td>{log.message}</td>
                    <td>
                      {(log.data || log.error) && (
                        <Dropdown>
                          <Dropdown.Toggle variant="outline-secondary" size="sm">
                            Details
                          </Dropdown.Toggle>
                          <Dropdown.Menu>
                            <div className="p-2" style={{ maxWidth: '300px' }}>
                              {log.data && (
                                <div className="mb-2">
                                  <strong>Data:</strong>
                                  <pre className="small mt-1" style={{ fontSize: '11px' }}>
                                    {log.data}
                                  </pre>
                                </div>
                              )}
                              {log.error && (
                                <div>
                                  <strong>Error:</strong>
                                  <pre className="small mt-1 text-danger" style={{ fontSize: '11px' }}>
                                    {JSON.stringify(log.error, null, 2)}
                                  </pre>
                                </div>
                              )}
                            </div>
                          </Dropdown.Menu>
                        </Dropdown>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          ) : (
            <div className="text-center py-4">
              <FaBug size={40} className="text-muted mb-2" />
              <p className="text-muted">No logs available</p>
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowLogsModal(false)}>
            Close
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
};

export default Dashboard;
