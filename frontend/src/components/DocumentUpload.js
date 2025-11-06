import React, { useState } from 'react';
import { Card, Form, Button, Alert, Row, Col, ProgressBar } from 'react-bootstrap';
import { useDropzone } from 'react-dropzone';
import { FaUpload, FaFileAlt } from 'react-icons/fa';
import axios from 'axios';

const CandidateUpload = ({ onCandidateUploaded }) => {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    file: null
  });
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState('');
  const [messageType, setMessageType] = useState('');

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    accept: {
      'text/plain': ['.txt'],
      'application/pdf': ['.pdf'],
      'application/msword': ['.doc'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx']
    },
    onDrop: (acceptedFiles) => {
      if (acceptedFiles.length > 0) {
        setFormData({ ...formData, file: acceptedFiles[0] });
      }
    }
  });

  const handleInputChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.name || !formData.email || !formData.file) {
      setMessage('Please fill in all required fields and select a file');
      setMessageType('danger');
      return;
    }

    setUploading(true);
    setMessage('');

    const uploadData = new FormData();
    uploadData.append('file', formData.file);
    uploadData.append('name', formData.name);
    uploadData.append('email', formData.email);
    uploadData.append('phone', formData.phone);

    try {
      const response = await axios.post('/api/rag/upload', uploadData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      setMessage('Candidate uploaded successfully!');
      setMessageType('success');

      // Reset form
      setFormData({ name: '', email: '', phone: '', file: null });

      // Notify parent component
      if (onCandidateUploaded) {
        onCandidateUploaded(response.data);
      }

    } catch (error) {
      setMessage('Error uploading candidate. Please try again.');
      setMessageType('danger');
      console.error('Upload error:', error);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <h2 className="mb-4">Upload Candidate CV</h2>

      <Card>
        <Card.Body>
          {message && (
            <Alert variant={messageType} className="mb-3">
              {message}
            </Alert>
          )}

          <Form onSubmit={handleSubmit}>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Candidate Name *</Form.Label>
                  <Form.Control
                    type="text"
                    name="name"
                    value={formData.name}
                    onChange={handleInputChange}
                    placeholder="Enter candidate's full name"
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Email Address *</Form.Label>
                  <Form.Control
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleInputChange}
                    placeholder="Enter email address"
                    required
                  />
                </Form.Group>
              </Col>
            </Row>

            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Phone Number</Form.Label>
                  <Form.Control
                    type="tel"
                    name="phone"
                    value={formData.phone}
                    onChange={handleInputChange}
                    placeholder="Enter phone number"
                  />
                </Form.Group>
              </Col>
            </Row>

            <Form.Group className="mb-4">
              <Form.Label>CV File *</Form.Label>
              <div
                {...getRootProps()}
                className={`dropzone ${isDragActive ? 'active' : ''}`}
                style={{
                  border: '2px dashed #dee2e6',
                  borderRadius: '8px',
                  padding: '40px',
                  textAlign: 'center',
                  cursor: 'pointer',
                  backgroundColor: isDragActive ? '#f8f9fa' : 'white',
                  transition: 'all 0.3s ease'
                }}
              >
                <input {...getInputProps()} />
                <FaUpload size={40} className="text-muted mb-3" />
                {formData.file ? (
                  <div>
                    <FaFileAlt className="text-success me-2" />
                    <strong>{formData.file.name}</strong>
                    <p className="text-muted mt-2">Click to change file</p>
                  </div>
                ) : (
                  <div>
                    <p className="mb-0">
                      {isDragActive
                        ? 'Drop the CV file here...'
                        : 'Drag & drop a CV file here, or click to select'}
                    </p>
                    <small className="text-muted">
                      Supported formats: PDF, DOC, DOCX, TXT
                    </small>
                  </div>
                )}
              </div>
            </Form.Group>

            {uploading && (
              <div className="mb-3">
                <ProgressBar animated now={100} label="Uploading..." />
              </div>
            )}

            <Button
              type="submit"
              variant="primary"
              size="lg"
              disabled={uploading}
              className="w-100"
            >
              {uploading ? 'Uploading...' : 'Upload Candidate'}
            </Button>
          </Form>
        </Card.Body>
      </Card>
    </div>
  );
};

export default CandidateUpload;
