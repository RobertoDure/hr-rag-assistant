import React, { useState } from 'react';
import {
  Row, Col, Card, Badge, Button, Modal, ListGroup,
  Form, InputGroup, Alert, Spinner
} from 'react-bootstrap';
import {
  FaUsers, FaEye, FaTrash, FaSearch, FaFileAlt,
  FaEnvelope, FaPhone, FaGraduationCap, FaBriefcase, FaUser
} from 'react-icons/fa';
import axios from 'axios';

const CandidateList = ({ candidates, onCandidateDeleted }) => {
  const [selectedCandidate, setSelectedCandidate] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterSkill, setFilterSkill] = useState('');
  const [deleting, setDeleting] = useState(null);
  const [deleteMessage, setDeleteMessage] = useState('');

  // Filter candidates based on search and skill filter
  const filteredCandidates = candidates.filter(candidate => {
    const matchesSearch = candidate.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         candidate.email.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesSkill = !filterSkill ||
                        (candidate.skills && candidate.skills.some(skill =>
                          skill.toLowerCase().includes(filterSkill.toLowerCase())));

    return matchesSearch && matchesSkill;
  });

  const handleViewDetails = (candidate) => {
    setSelectedCandidate(candidate);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedCandidate(null);
  };

  const handleDeleteCandidate = async (candidateId) => {
    if (!window.confirm('Are you sure you want to delete this candidate?')) {
      return;
    }

    setDeleting(candidateId);
    setDeleteMessage('');

    try {
      await axios.delete(`/api/hr/candidates/${candidateId}`);
      onCandidateDeleted(candidateId);
      setDeleteMessage('Candidate deleted successfully');
    } catch (error) {
      setDeleteMessage('Error deleting candidate');
      console.error('Delete error:', error);
    } finally {
      setDeleting(null);
    }
  };

  // Get all unique skills from candidates for filter dropdown
  const allSkills = [...new Set(
    candidates.flatMap(candidate => candidate.skills || [])
  )].sort();

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>
          <FaUsers className="me-2" />
          Candidates ({filteredCandidates.length})
        </h2>
      </div>

      {deleteMessage && (
        <Alert variant={deleteMessage.includes('Error') ? 'danger' : 'success'}
               className="mb-3">
          {deleteMessage}
        </Alert>
      )}

      {/* Search and Filter Controls */}
      <Row className="mb-4">
        <Col md={6}>
          <InputGroup>
            <InputGroup.Text>
              <FaSearch />
            </InputGroup.Text>
            <Form.Control
              type="text"
              placeholder="Search by name or email..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </InputGroup>
        </Col>
        <Col md={6}>
          <Form.Select
            value={filterSkill}
            onChange={(e) => setFilterSkill(e.target.value)}
          >
            <option value="">Filter by skill...</option>
            {allSkills.map(skill => (
              <option key={skill} value={skill}>{skill}</option>
            ))}
          </Form.Select>
        </Col>
      </Row>

      {/* Candidates Grid */}
      {filteredCandidates.length === 0 ? (
        <Card className="text-center py-5">
          <Card.Body>
            <FaUsers size={50} className="text-muted mb-3" />
            <h4>No Candidates Found</h4>
            <p className="text-muted">
              {candidates.length === 0
                ? 'Upload some CVs to get started'
                : 'Try adjusting your search or filter criteria'}
            </p>
          </Card.Body>
        </Card>
      ) : (
        <Row>
          {filteredCandidates.map(candidate => (
            <Col md={6} lg={4} key={candidate.id} className="mb-4">
              <Card className="h-100">
                <Card.Body>
                  <div className="d-flex justify-content-between align-items-start mb-3">
                    <div>
                      <Card.Title className="mb-1">{candidate.name}</Card.Title>
                      <Card.Subtitle className="text-muted">
                        <FaEnvelope className="me-1" size={12} />
                        {candidate.email}
                      </Card.Subtitle>
                    </div>
                  </div>

                  {candidate.phone && (
                    <div className="mb-2">
                      <small className="text-muted">
                        <FaPhone className="me-1" />
                        {candidate.phone}
                      </small>
                    </div>
                  )}

                  {candidate.skills && candidate.skills.length > 0 && (
                    <div className="mb-3">
                      <div className="mb-2">
                        <small className="text-muted">Top Skills:</small>
                      </div>
                      <div>
                        {candidate.skills.slice(0, 3).map(skill => (
                          <Badge key={skill} bg="secondary" className="me-1 mb-1">
                            {skill}
                          </Badge>
                        ))}
                        {candidate.skills.length > 3 && (
                          <Badge bg="light" text="dark" className="me-1 mb-1">
                            +{candidate.skills.length - 3} more
                          </Badge>
                        )}
                      </div>
                    </div>
                  )}

                  <div className="d-flex justify-content-between align-items-center">
                    <Button
                      variant="outline-primary"
                      size="sm"
                      onClick={() => handleViewDetails(candidate)}
                    >
                      <FaEye className="me-1" />
                      View Details
                    </Button>

                    <Button
                      variant="outline-danger"
                      size="sm"
                      onClick={() => handleDeleteCandidate(candidate.id)}
                      disabled={deleting === candidate.id}
                    >
                      {deleting === candidate.id ? (
                        <Spinner animation="border" size="sm" />
                      ) : (
                        <FaTrash />
                      )}
                    </Button>
                  </div>
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      {/* Candidate Details Modal */}
      <Modal show={showModal} onHide={handleCloseModal} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>
            <FaUser className="me-2" />
            {selectedCandidate?.name}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {selectedCandidate && (
            <Row>
              <Col md={4}>
                {/* Contact Information */}
                <Card className="mb-3">
                  <Card.Header>
                    <h6 className="mb-0">Contact Information</h6>
                  </Card.Header>
                  <Card.Body>
                    <div className="mb-2">
                      <FaEnvelope className="me-2 text-muted" />
                      {selectedCandidate.email}
                    </div>
                    {selectedCandidate.phone && (
                      <div className="mb-2">
                        <FaPhone className="me-2 text-muted" />
                        {selectedCandidate.phone}
                      </div>
                    )}
                    {selectedCandidate.originalFileName && (
                      <div className="mb-2">
                        <FaFileAlt className="me-2 text-muted" />
                        <small>{selectedCandidate.originalFileName}</small>
                      </div>
                    )}
                  </Card.Body>
                </Card>

                {/* Experience */}
                {selectedCandidate.experience && (
                  <Card className="mb-3">
                    <Card.Header>
                      <h6 className="mb-0">
                        <FaBriefcase className="me-2" />
                        Experience
                      </h6>
                    </Card.Header>
                    <Card.Body>
                      <small>{selectedCandidate.experience}</small>
                    </Card.Body>
                  </Card>
                )}

                {/* Education */}
                {selectedCandidate.education && (
                  <Card className="mb-3">
                    <Card.Header>
                      <h6 className="mb-0">
                        <FaGraduationCap className="me-2" />
                        Education
                      </h6>
                    </Card.Header>
                    <Card.Body>
                      <small>{selectedCandidate.education}</small>
                    </Card.Body>
                  </Card>
                )}
              </Col>

              <Col md={8}>
                {/* Skills */}
                {selectedCandidate.skills && selectedCandidate.skills.length > 0 && (
                  <Card className="mb-3">
                    <Card.Header>
                      <h6 className="mb-0">Skills ({selectedCandidate.skills.length})</h6>
                    </Card.Header>
                    <Card.Body>
                      <div>
                        {selectedCandidate.skills.map(skill => (
                          <Badge key={skill} bg="primary" className="me-2 mb-2">
                            {skill}
                          </Badge>
                        ))}
                      </div>
                    </Card.Body>
                  </Card>
                )}

                {/* CV Content */}
                {selectedCandidate.cvContent && (
                  <Card>
                    <Card.Header>
                      <h6 className="mb-0">CV Content</h6>
                    </Card.Header>
                    <Card.Body>
                      <div
                        style={{
                          maxHeight: '400px',
                          overflowY: 'auto',
                          fontSize: '14px',
                          lineHeight: '1.5',
                          whiteSpace: 'pre-wrap'
                        }}
                      >
                        {selectedCandidate.cvContent}
                      </div>
                    </Card.Body>
                  </Card>
                )}
              </Col>
            </Row>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={handleCloseModal}>
            Close
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default CandidateList;
