import React, { useState } from 'react';
import { Card, Form, Button, Alert, Container, Row, Col } from 'react-bootstrap';
import './Login.css';

/**
 * Login component with username/password authentication
 */
function Login({ onLoginSuccess }) {
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState('');

  // Handle input changes
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    // Clear field error when user types
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
    // Clear API error when user types
    if (apiError) {
      setApiError('');
    }
  };

  // Validate form
  const validateForm = () => {
    const newErrors = {};

    if (!formData.username.trim()) {
      newErrors.username = 'Username is required';
    }

    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else if (formData.password.length < 3) {
      newErrors.password = 'Password must be at least 3 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();
    setApiError('');

    // Validate form
    if (!validateForm()) {
      return;
    }

    setLoading(true);

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: formData.username,
          password: formData.password
        })
      });

      const data = await response.json();

      // Handle error response
      if (!response.ok) {
        setApiError(data.message || 'Login failed. Please try again.');
        setLoading(false);
        return;
      }

      // Store token in localStorage
      localStorage.setItem('token', data.token);
      localStorage.setItem('username', data.username);
      localStorage.setItem('tokenExpiration', Date.now() + data.expiresIn);

      // Call success callback
      if (onLoginSuccess) {
        onLoginSuccess(data);
      }

    } catch (error) {
      console.error('Login error:', error);
      setApiError('Network error. Please check your connection and try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container fluid className="login-container">
      <Row className="justify-content-center align-items-center min-vh-100">
        <Col xs={12} sm={10} md={8} lg={5} xl={4}>
          <Card className="login-card shadow-lg">
            <Card.Body className="p-5">
              <div className="text-center mb-4">
                <h2 className="text-primary fw-bold">HR RagWiser</h2>
                <p className="text-muted">Intelligent Candidate Management System</p>
              </div>

              {apiError && (
                <Alert variant="danger" dismissible onClose={() => setApiError('')}>
                  {apiError}
                </Alert>
              )}

              <Form onSubmit={handleSubmit}>
                <Form.Group className="mb-3" controlId="username">
                  <Form.Label>Username</Form.Label>
                  <Form.Control
                    type="text"
                    name="username"
                    placeholder="Enter username"
                    value={formData.username}
                    onChange={handleChange}
                    isInvalid={!!errors.username}
                    disabled={loading}
                    autoComplete="username"
                    autoFocus
                  />
                  <Form.Control.Feedback type="invalid">
                    {errors.username}
                  </Form.Control.Feedback>
                </Form.Group>

                <Form.Group className="mb-4" controlId="password">
                  <Form.Label>Password</Form.Label>
                  <Form.Control
                    type="password"
                    name="password"
                    placeholder="Enter password"
                    value={formData.password}
                    onChange={handleChange}
                    isInvalid={!!errors.password}
                    disabled={loading}
                    autoComplete="current-password"
                  />
                  <Form.Control.Feedback type="invalid">
                    {errors.password}
                  </Form.Control.Feedback>
                </Form.Group>

                <Button
                  variant="primary"
                  type="submit"
                  className="w-100 mb-3"
                  disabled={loading}
                  size="lg"
                >
                  {loading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                      Logging in...
                    </>
                  ) : (
                    'Login'
                  )}
                </Button>

                <div className="text-center text-muted small">
                  <p className="mb-0">Default credentials: admin / changeme</p>
                </div>
              </Form>
            </Card.Body>
          </Card>

          <div className="text-center mt-3 text-muted">
            <small>&copy; 2025 HR RagWiser. All rights reserved.</small>
          </div>
        </Col>
      </Row>
    </Container>
  );
}

export default Login;

