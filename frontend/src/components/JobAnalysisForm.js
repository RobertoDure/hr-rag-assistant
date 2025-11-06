import React, { useState } from 'react';
import {
  Container, Row, Col, Card, Form, Button, Alert, Spinner,
  Badge, ListGroup, ProgressBar, Table
} from 'react-bootstrap';
import {
  FaBriefcase, FaSearch, FaUsers, FaTrophy, FaStar,
  FaClipboardList, FaGraduationCap, FaClock, FaUser
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

  return (
    <Container className="mt-4">
      <Row>
        <Col lg={6}>
          <Card className="h-100 border-0 shadow-sm">
            <Card.Header className="bg-primary text-white">
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
            <Card className="h-100 border-0 shadow-sm">
              <Card.Header className="bg-success text-white">
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
            <Card className="h-100 border-0 shadow-sm">
              <Card.Body className="d-flex align-items-center justify-content-center text-muted">
                <div className="text-center">
                  <FaClipboardList size={48} className="mb-3 opacity-50" />
                  <p>Fill out the job requirements form to analyze candidates</p>
                </div>
              </Card.Body>
            </Card>
          )}
        </Col>
      </Row>
    </Container>
  );
};

export default JobAnalysisForm;
