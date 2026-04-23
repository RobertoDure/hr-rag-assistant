import React, { useState, useEffect, useCallback } from 'react';
import {
  Container, Row, Col, Card, Form, Button, Alert, Spinner,
  Badge, ListGroup, ProgressBar, Table, Modal, Pagination
} from 'react-bootstrap';
import {
  FaBriefcase, FaSearch, FaUsers, FaTrophy, FaStar,
  FaClipboardList, FaGraduationCap, FaClock, FaUser,
  FaEye, FaTrash, FaHistory
} from 'react-icons/fa';
import axios from 'axios';

const JobAnalysisForm = () => {
  const [formData, setFormData] = useState({
    jobTitle: '',
    jobDescription: '',
    requiredSkills: [],
    preferredSkills: [],
    experienceLevel: '',
    educationRequirement: '',
    minYearsExperience: '',
    maxYearsExperience: ''
  });

  const [skillInput, setSkillInput] = useState('');
  const [preferredSkillInput, setPreferredSkillInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState(null);
  const [error, setError] = useState(null);

  const [analyses, setAnalyses] = useState([]);
  const [analysesLoading, setAnalysesLoading] = useState(false);
  const [analysesError, setAnalysesError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const PAGE_SIZE = 10;

  const [detailModal, setDetailModal] = useState({ open: false, analysis: null, loading: false });
  const [deleteModal, setDeleteModal] = useState({ open: false, id: null, title: '' });
  const [deleteLoading, setDeleteLoading] = useState(false);

  const fetchAnalyses = useCallback(async (page = 0) => {
    setAnalysesLoading(true);
    setAnalysesError(null);
    try {
      const response = await axios.get('/api/hr/analyses', { params: { page, size: PAGE_SIZE } });
      setAnalyses(response.data.content);
      setTotalPages(response.data.totalPages);
      setTotalElements(response.data.totalElements);
      setCurrentPage(response.data.number);
    } catch (err) {
      setAnalysesError('Failed to load job analyses history.');
    } finally {
      setAnalysesLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAnalyses();
  }, [fetchAnalyses]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const addSkill = (skillType) => {
    const input = skillType === 'required' ? skillInput : preferredSkillInput;
    if (input.trim()) {
      setFormData(prev => ({
        ...prev,
        [skillType === 'required' ? 'requiredSkills' : 'preferredSkills']: [
          ...prev[skillType === 'required' ? 'requiredSkills' : 'preferredSkills'],
          input.trim()
        ]
      }));
      if (skillType === 'required') {
        setSkillInput('');
      } else {
        setPreferredSkillInput('');
      }
    }
  };

  const removeSkill = (skillType, index) => {
    setFormData(prev => ({
      ...prev,
      [skillType]: prev[skillType].filter((_, i) => i !== index)
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.jobTitle.trim() || !formData.jobDescription.trim()) {
      setError('Job title and description are required');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await axios.post('/api/hr/analyze', {
        ...formData,
        minYearsExperience: formData.minYearsExperience ? parseInt(formData.minYearsExperience) : null,
        maxYearsExperience: formData.maxYearsExperience ? parseInt(formData.maxYearsExperience) : null
      });

      setResults(response.data);
      fetchAnalyses(currentPage);
    } catch (err) {
      console.error('Error analyzing job:', err);
      setError('Failed to analyze job. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const getMatchScoreColor = (score) => {
    if (score >= 80) return 'success';
    if (score >= 60) return 'warning';
    return 'danger';
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const openDetailModal = async (id) => {
    setDetailModal({ open: true, analysis: null, loading: true });
    try {
      const response = await axios.get(`/api/hr/analyses/${id}`);
      setDetailModal({ open: true, analysis: response.data, loading: false });
    } catch (err) {
      setDetailModal({ open: false, analysis: null, loading: false });
    }
  };

  const closeDetailModal = () => setDetailModal({ open: false, analysis: null, loading: false });

  const openDeleteModal = (id, title) => setDeleteModal({ open: true, id, title });

  const closeDeleteModal = () => setDeleteModal({ open: false, id: null, title: '' });

  const confirmDelete = async () => {
    setDeleteLoading(true);
    try {
      await axios.delete(`/api/hr/analyses/${deleteModal.id}`);
      if (results && results.id === deleteModal.id) {
        setResults(null);
      }
      const isLastItemOnPage = analyses.length === 1 && currentPage > 0;
      const nextPage = isLastItemOnPage ? currentPage - 1 : currentPage;
      fetchAnalyses(nextPage);
      closeDeleteModal();
    } catch (err) {
      closeDeleteModal();
    } finally {
      setDeleteLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <h2>Job Analysis</h2>
      </div>
      <Row>
        <Col lg={6}>
          <Card className="h-100">
            <Card.Header>
              <FaBriefcase className="me-2" />
              Job Requirements Analysis
            </Card.Header>
            <Card.Body>
              {error && (
                <Alert variant="danger" className="mb-3">
                  {error}
                </Alert>
              )}

              <Form onSubmit={handleSubmit}>
                <Form.Group className="mb-3">
                  <Form.Label>Job Title *</Form.Label>
                  <Form.Control
                    type="text"
                    name="jobTitle"
                    value={formData.jobTitle}
                    onChange={handleInputChange}
                    placeholder="e.g., Senior Software Engineer"
                    required
                  />
                </Form.Group>

                <Form.Group className="mb-3">
                  <Form.Label>Job Description *</Form.Label>
                  <Form.Control
                    as="textarea"
                    rows={4}
                    name="jobDescription"
                    value={formData.jobDescription}
                    onChange={handleInputChange}
                    placeholder="Describe the role, responsibilities, and requirements..."
                    required
                  />
                </Form.Group>

                <Row>
                  <Col md={6}>
                    <Form.Group className="mb-3">
                      <Form.Label>Experience Level</Form.Label>
                      <Form.Select
                        name="experienceLevel"
                        value={formData.experienceLevel}
                        onChange={handleInputChange}
                      >
                        <option value="">Select level</option>
                        <option value="Entry">Entry Level</option>
                        <option value="Junior">Junior</option>
                        <option value="Mid">Mid Level</option>
                        <option value="Senior">Senior</option>
                        <option value="Lead">Lead</option>
                        <option value="Manager">Manager</option>
                      </Form.Select>
                    </Form.Group>
                  </Col>
                  <Col md={6}>
                    <Form.Group className="mb-3">
                      <Form.Label>Education Requirement</Form.Label>
                      <Form.Control
                        type="text"
                        name="educationRequirement"
                        value={formData.educationRequirement}
                        onChange={handleInputChange}
                        placeholder="e.g., Bachelor's in Computer Science"
                      />
                    </Form.Group>
                  </Col>
                </Row>

                <Row>
                  <Col md={6}>
                    <Form.Group className="mb-3">
                      <Form.Label>Min Years Experience</Form.Label>
                      <Form.Control
                        type="number"
                        name="minYearsExperience"
                        value={formData.minYearsExperience}
                        onChange={handleInputChange}
                        min="0"
                        max="30"
                      />
                    </Form.Group>
                  </Col>
                  <Col md={6}>
                    <Form.Group className="mb-3">
                      <Form.Label>Max Years Experience</Form.Label>
                      <Form.Control
                        type="number"
                        name="maxYearsExperience"
                        value={formData.maxYearsExperience}
                        onChange={handleInputChange}
                        min="0"
                        max="30"
                      />
                    </Form.Group>
                  </Col>
                </Row>

                {/* Required Skills */}
                <Form.Group className="mb-3">
                  <Form.Label>Required Skills</Form.Label>
                  <div className="d-flex mb-2">
                    <Form.Control
                      type="text"
                      value={skillInput}
                      onChange={(e) => setSkillInput(e.target.value)}
                      placeholder="Add required skill..."
                      onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), addSkill('required'))}
                    />
                    <Button
                      variant="outline-primary"
                      className="ms-2"
                      onClick={() => addSkill('required')}
                    >
                      Add
                    </Button>
                  </div>
                  <div>
                    {formData.requiredSkills.map((skill, index) => (
                      <Badge
                        key={index}
                        bg="primary"
                        className="me-2 mb-2"
                        style={{ cursor: 'pointer' }}
                        onClick={() => removeSkill('requiredSkills', index)}
                      >
                        {skill} ×
                      </Badge>
                    ))}
                  </div>
                </Form.Group>

                {/* Preferred Skills */}
                <Form.Group className="mb-3">
                  <Form.Label>Preferred Skills</Form.Label>
                  <div className="d-flex mb-2">
                    <Form.Control
                      type="text"
                      value={preferredSkillInput}
                      onChange={(e) => setPreferredSkillInput(e.target.value)}
                      placeholder="Add preferred skill..."
                      onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), addSkill('preferred'))}
                    />
                    <Button
                      variant="outline-secondary"
                      className="ms-2"
                      onClick={() => addSkill('preferred')}
                    >
                      Add
                    </Button>
                  </div>
                  <div>
                    {formData.preferredSkills.map((skill, index) => (
                      <Badge
                        key={index}
                        bg="secondary"
                        className="me-2 mb-2"
                        style={{ cursor: 'pointer' }}
                        onClick={() => removeSkill('preferredSkills', index)}
                      >
                        {skill} ×
                      </Badge>
                    ))}
                  </div>
                </Form.Group>

                <Button
                  type="submit"
                  variant="primary"
                  size="lg"
                  disabled={loading}
                  className="w-100"
                >
                  {loading ? (
                    <>
                      <Spinner animation="border" size="sm" className="me-2" />
                      Analyzing Candidates...
                    </>
                  ) : (
                    <>
                      <FaSearch className="me-2" />
                      Analyze Job Requirements
                    </>
                  )}
                </Button>
              </Form>
            </Card.Body>
          </Card>
        </Col>

        <Col lg={6}>
          {results ? (
            <Card className="h-100">
              <Card.Header>
                <FaTrophy className="me-2" />
                Analysis Results for "{results.jobTitle}"
              </Card.Header>
              <Card.Body>
                <div className="mb-4">
                  <div className="d-flex justify-content-between align-items-center mb-2">
                    <span className="fw-bold">Candidates Analyzed:</span>
                    <Badge bg="success" className="fs-6">{results.totalCandidatesAnalyzed}</Badge>
                  </div>
                  <div className="mb-3">
                    <small className="text-muted">
                      <FaClock className="me-1" />
                      Analyzed on {formatDate(results.createdAt)}
                    </small>
                  </div>
                </div>

                {results.topCandidateRecommendation && (
                  <Alert variant="info" className="mb-4">
                    <strong>AI Recommendation:</strong>
                    <p className="mb-0 mt-2">{results.topCandidateRecommendation}</p>
                  </Alert>
                )}

                <h6 className="mb-3">
                  <FaUsers className="me-2" />
                  Top Candidates
                </h6>

                {results.rankedCandidates && results.rankedCandidates.length > 0 ? (
                  <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
                    {results.rankedCandidates.slice(0, 10).map((candidate, index) => (
                      <Card key={candidate.id} className="mb-2 border-0 bg-light">
                        <Card.Body className="p-3">
                          <div className="d-flex justify-content-between align-items-start mb-2">
                            <div>
                              <strong className="text-primary">#{candidate.rankingPosition}</strong>
                              <span className="ms-2 fw-bold">{candidate.name}</span>
                            </div>
                            <Badge bg={getMatchScoreColor(candidate.matchScore)} className="fs-6">
                              {candidate.matchScore.toFixed(1)}%
                            </Badge>
                          </div>

                          <div className="mb-2">
                            <ProgressBar
                              now={candidate.matchScore}
                              variant={getMatchScoreColor(candidate.matchScore)}
                              style={{ height: '8px' }}
                            />
                          </div>

                          <div className="small text-muted mb-2">
                            <FaUser className="me-1" />
                            {candidate.email}
                          </div>

                          {candidate.keyHighlights && candidate.keyHighlights.length > 0 && (
                            <div>
                              <div className="small fw-bold text-dark mb-1">Key Highlights:</div>
                              <ul className="small mb-0 ps-3">
                                {candidate.keyHighlights.map((highlight, idx) => (
                                  <li key={idx} className="text-muted">{highlight}</li>
                                ))}
                              </ul>
                            </div>
                          )}
                        </Card.Body>
                      </Card>
                    ))}
                  </div>
                ) : (
                  <Alert variant="warning">
                    No candidates found matching the job requirements.
                  </Alert>
                )}
              </Card.Body>
            </Card>
          ) : (
            <Card className="h-100">
              <Card.Body className="d-flex align-items-center justify-content-center" style={{ color: 'var(--text-muted)' }}>
                <div className="text-center">
                  <FaClipboardList size={48} className="mb-3" style={{ opacity: 0.4 }} />
                  <p>Fill out the job requirements form to analyze candidates</p>
                </div>
              </Card.Body>
            </Card>
          )}
        </Col>
      </Row>

      {/* Job Analysis History Table */}
      <Row className="mt-4">
        <Col>
          <Card>
            <Card.Header className="d-flex justify-content-between align-items-center">
              <span>
                <FaHistory className="me-2" />
                Job Analysis History
              </span>
              <Button variant="outline-secondary" size="sm" onClick={() => fetchAnalyses(currentPage)} disabled={analysesLoading}>
                {analysesLoading ? <Spinner animation="border" size="sm" /> : 'Refresh'}
              </Button>
            </Card.Header>
            <Card.Body className="p-0">
              {analysesError && (
                <Alert variant="danger" className="m-3">{analysesError}</Alert>
              )}
              {analysesLoading && analyses.length === 0 ? (
                <div className="text-center p-4">
                  <Spinner animation="border" size="sm" className="me-2" />
                  Loading analyses...
                </div>
              ) : analyses.length === 0 ? (
                <div className="text-center p-4" style={{ color: 'var(--text-muted)' }}>
                  <FaClipboardList size={32} className="mb-2" style={{ opacity: 0.4 }} />
                  <p className="mb-0">No job analyses yet. Run your first analysis above.</p>
                </div>
              ) : (
                <div className="table-responsive">
                  <Table hover className="mb-0">
                    <thead className="table-light">
                      <tr>
                        <th>Job Title</th>
                        <th>Experience Level</th>
                        <th>Candidates Analyzed</th>
                        <th>Required Skills</th>
                        <th>Date</th>
                        <th className="text-center">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {analyses.map(analysis => (
                        <tr key={analysis.id}>
                          <td className="fw-semibold">{analysis.jobTitle}</td>
                          <td>
                            {analysis.experienceLevel
                              ? <Badge bg="info" text="dark">{analysis.experienceLevel}</Badge>
                              : <span className="text-muted">—</span>}
                          </td>
                          <td>
                            <Badge bg="success">{analysis.totalCandidatesAnalyzed}</Badge>
                          </td>
                          <td>
                            <div style={{ maxWidth: '220px' }}>
                              {analysis.requiredSkills && analysis.requiredSkills.slice(0, 3).map((skill, idx) => (
                                <Badge key={idx} bg="primary" className="me-1 mb-1" style={{ fontSize: '0.7rem' }}>
                                  {skill}
                                </Badge>
                              ))}
                              {analysis.requiredSkills && analysis.requiredSkills.length > 3 && (
                                <Badge bg="secondary" style={{ fontSize: '0.7rem' }}>
                                  +{analysis.requiredSkills.length - 3}
                                </Badge>
                              )}
                              {(!analysis.requiredSkills || analysis.requiredSkills.length === 0) && (
                                <span className="text-muted">—</span>
                              )}
                            </div>
                          </td>
                          <td className="text-muted small">{formatDate(analysis.createdAt)}</td>
                          <td className="text-center">
                            <Button
                              variant="outline-primary"
                              size="sm"
                              className="me-2"
                              onClick={() => openDetailModal(analysis.id)}
                            >
                              <FaEye className="me-1" />
                              Details
                            </Button>
                            <Button
                              variant="outline-danger"
                              size="sm"
                              onClick={() => openDeleteModal(analysis.id, analysis.jobTitle)}
                            >
                              <FaTrash />
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </div>
              )}
              {totalPages > 1 && (
                <div className="d-flex justify-content-between align-items-center px-3 py-2 border-top">
                  <small className="text-muted">
                    Showing page {currentPage + 1} of {totalPages} ({totalElements} total)
                  </small>
                  <Pagination className="mb-0" size="sm">
                    <Pagination.First onClick={() => fetchAnalyses(0)} disabled={currentPage === 0 || analysesLoading} />
                    <Pagination.Prev onClick={() => fetchAnalyses(currentPage - 1)} disabled={currentPage === 0 || analysesLoading} />
                    {Array.from({ length: totalPages }, (_, i) => i)
                      .filter(i => i === 0 || i === totalPages - 1 || Math.abs(i - currentPage) <= 2)
                      .reduce((acc, i, idx, arr) => {
                        if (idx > 0 && i - arr[idx - 1] > 1) acc.push('ellipsis-' + i);
                        acc.push(i);
                        return acc;
                      }, [])
                      .map(item =>
                        typeof item === 'string'
                          ? <Pagination.Ellipsis key={item} disabled />
                          : <Pagination.Item
                              key={item}
                              active={item === currentPage}
                              onClick={() => item !== currentPage && fetchAnalyses(item)}
                              disabled={analysesLoading}
                            >
                              {item + 1}
                            </Pagination.Item>
                      )}
                    <Pagination.Next onClick={() => fetchAnalyses(currentPage + 1)} disabled={currentPage === totalPages - 1 || analysesLoading} />
                    <Pagination.Last onClick={() => fetchAnalyses(totalPages - 1)} disabled={currentPage === totalPages - 1 || analysesLoading} />
                  </Pagination>
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Detail Modal */}
      <Modal show={detailModal.open} onHide={closeDetailModal} size="xl" scrollable>
        <Modal.Header closeButton>
          <Modal.Title>
            <FaBriefcase className="me-2" />
            {detailModal.analysis ? detailModal.analysis.jobTitle : 'Job Analysis Details'}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {detailModal.loading && (
            <div className="text-center p-5">
              <Spinner animation="border" />
              <p className="mt-3">Loading analysis...</p>
            </div>
          )}
          {detailModal.analysis && (
            <>
              {/* Job Requirements Section */}
              <h5 className="mb-3">
                <FaClipboardList className="me-2 text-primary" />
                Job Requirements
              </h5>
              <Row className="mb-4">
                <Col md={6}>
                  <div className="mb-2">
                    <span className="fw-bold">Job Title:</span>{' '}
                    <span>{detailModal.analysis.jobTitle}</span>
                  </div>
                  {detailModal.analysis.experienceLevel && (
                    <div className="mb-2">
                      <span className="fw-bold">Experience Level:</span>{' '}
                      <Badge bg="info" text="dark">{detailModal.analysis.experienceLevel}</Badge>
                    </div>
                  )}
                  {detailModal.analysis.educationRequirement && (
                    <div className="mb-2">
                      <span className="fw-bold">Education:</span>{' '}
                      <span>{detailModal.analysis.educationRequirement}</span>
                    </div>
                  )}
                  {(detailModal.analysis.minYearsExperience != null || detailModal.analysis.maxYearsExperience != null) && (
                    <div className="mb-2">
                      <span className="fw-bold">Years of Experience:</span>{' '}
                      <span>
                        {detailModal.analysis.minYearsExperience ?? 0} – {detailModal.analysis.maxYearsExperience ?? '∞'} years
                      </span>
                    </div>
                  )}
                  <div className="mb-2">
                    <span className="fw-bold">Analyzed on:</span>{' '}
                    <span className="text-muted">{formatDate(detailModal.analysis.createdAt)}</span>
                  </div>
                </Col>
                <Col md={6}>
                  {detailModal.analysis.requiredSkills && detailModal.analysis.requiredSkills.length > 0 && (
                    <div className="mb-2">
                      <div className="fw-bold mb-1">Required Skills:</div>
                      <div>
                        {detailModal.analysis.requiredSkills.map((skill, idx) => (
                          <Badge key={idx} bg="primary" className="me-1 mb-1">{skill}</Badge>
                        ))}
                      </div>
                    </div>
                  )}
                  {detailModal.analysis.preferredSkills && detailModal.analysis.preferredSkills.length > 0 && (
                    <div className="mb-2">
                      <div className="fw-bold mb-1">Preferred Skills:</div>
                      <div>
                        {detailModal.analysis.preferredSkills.map((skill, idx) => (
                          <Badge key={idx} bg="secondary" className="me-1 mb-1">{skill}</Badge>
                        ))}
                      </div>
                    </div>
                  )}
                </Col>
              </Row>

              {detailModal.analysis.jobDescription && (
                <div className="mb-4">
                  <div className="fw-bold mb-1">Job Description:</div>
                  <div className="bg-light p-3 rounded" style={{ whiteSpace: 'pre-wrap', fontSize: '0.9rem' }}>
                    {detailModal.analysis.jobDescription}
                  </div>
                </div>
              )}

              <hr />

              {/* AI Recommendation */}
              {detailModal.analysis.topCandidateRecommendation && (
                <div className="mb-4">
                  <h5 className="mb-2">
                    <FaTrophy className="me-2 text-warning" />
                    AI Recommendation
                  </h5>
                  <Alert variant="info" className="mb-0">
                    {detailModal.analysis.topCandidateRecommendation}
                  </Alert>
                </div>
              )}

              {/* Candidates Ranking */}
              <h5 className="mb-3">
                <FaUsers className="me-2 text-success" />
                Candidates Ranking
                <Badge bg="success" className="ms-2">{detailModal.analysis.totalCandidatesAnalyzed} analyzed</Badge>
              </h5>

              {detailModal.analysis.rankedCandidates && detailModal.analysis.rankedCandidates.length > 0 ? (
                <div className="table-responsive">
                  <Table hover bordered size="sm">
                    <thead className="table-light">
                      <tr>
                        <th style={{ width: '60px' }}>Rank</th>
                        <th>Candidate</th>
                        <th>Contact</th>
                        <th style={{ width: '160px' }}>Match Score</th>
                        <th>Key Highlights</th>
                      </tr>
                    </thead>
                    <tbody>
                      {detailModal.analysis.rankedCandidates.map(candidate => (
                        <tr key={candidate.id}>
                          <td className="text-center">
                            <span className="fw-bold text-primary">#{candidate.rankingPosition}</span>
                          </td>
                          <td>
                            <div className="fw-semibold">{candidate.name}</div>
                          </td>
                          <td className="small text-muted">
                            <div>{candidate.email}</div>
                            {candidate.phone && <div>{candidate.phone}</div>}
                          </td>
                          <td>
                            <div className="d-flex align-items-center gap-2">
                              <ProgressBar
                                now={candidate.matchScore}
                                variant={getMatchScoreColor(candidate.matchScore)}
                                style={{ height: '8px', flex: 1 }}
                              />
                              <Badge bg={getMatchScoreColor(candidate.matchScore)} style={{ minWidth: '52px' }}>
                                {candidate.matchScore.toFixed(1)}%
                              </Badge>
                            </div>
                          </td>
                          <td>
                            {candidate.keyHighlights && candidate.keyHighlights.length > 0 ? (
                              <ul className="mb-0 ps-3 small">
                                {candidate.keyHighlights.map((h, idx) => (
                                  <li key={idx} className="text-muted">{h}</li>
                                ))}
                              </ul>
                            ) : (
                              <span className="text-muted">—</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </div>
              ) : (
                <Alert variant="warning">No candidates found for this analysis.</Alert>
              )}
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={closeDetailModal}>Close</Button>
        </Modal.Footer>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal show={deleteModal.open} onHide={closeDeleteModal} centered>
        <Modal.Header closeButton>
          <Modal.Title>Delete Job Analysis</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          Are you sure you want to delete the analysis for <strong>"{deleteModal.title}"</strong>?
          This will also remove all associated candidate rankings and cannot be undone.
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={closeDeleteModal} disabled={deleteLoading}>
            Cancel
          </Button>
          <Button variant="danger" onClick={confirmDelete} disabled={deleteLoading}>
            {deleteLoading ? <><Spinner animation="border" size="sm" className="me-2" />Deleting...</> : 'Delete'}
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default JobAnalysisForm;
