import React, { useState } from 'react';
import { Alert, Button, Card, Col, Container, Form, Row } from 'react-bootstrap';
import axios from 'axios';
import './Login.css';

function UserRegistration({ onNavigateToLogin, isEmbedded, onSuccess }) {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    role: 'USER'
  });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((previous) => ({
      ...previous,
      [name]: value
    }));

    if (errors[name]) {
      setErrors((previous) => ({
        ...previous,
        [name]: ''
      }));
    }

    if (apiError) {
      setApiError('');
    }

    if (successMessage) {
      setSuccessMessage('');
    }
  };

  const validateForm = () => {
    const validationErrors = {};

    if (!formData.username.trim()) {
      validationErrors.username = 'Username is required';
    } else if (formData.username.trim().length < 3) {
      validationErrors.username = 'Username must be at least 3 characters';
    }

    if (!formData.email.trim()) {
      validationErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      validationErrors.email = 'Please enter a valid email address';
    }

    if (!formData.password) {
      validationErrors.password = 'Password is required';
    } else if (formData.password.length < 8) {
      validationErrors.password = 'Password must be at least 8 characters';
    } else if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(formData.password)) {
      validationErrors.password = 'Password must contain at least one uppercase letter, one lowercase letter, and one digit';
    }

    setErrors(validationErrors);
    return Object.keys(validationErrors).length === 0;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setApiError('');
    setSuccessMessage('');

    if (!validateForm()) {
      return;
    }

    setLoading(true);

    try {
      await axios.post('/api/users/register', {
        username: formData.username,
        email: formData.email,
        password: formData.password,
        firstName: formData.firstName || null,
        lastName: formData.lastName || null,
        role: isEmbedded ? formData.role : 'USER'
      });

      setSuccessMessage(`User ${formData.username} created successfully!`);
      setFormData({
        username: '',
        email: '',
        password: '',
        firstName: '',
        lastName: '',
        role: 'USER'
      });
      setErrors({});
      if (onSuccess) {
        onSuccess(formData);
      }
    } catch (error) {
      const responseData = error.response?.data;
      const validationErrors = responseData?.validationErrors;

      if (validationErrors && typeof validationErrors === 'object') {
        setErrors(validationErrors);
      }

      setApiError(responseData?.message || 'Failed to create user. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const formContent = (
    <Card className={isEmbedded ? 'shadow-sm' : 'login-card shadow-lg'}>
      <Card.Body className={isEmbedded ? '' : 'p-5'}>
        <div className={isEmbedded ? 'd-flex justify-content-between align-items-center mb-3' : 'text-center mb-4'}>
          {isEmbedded ? (
            <Card.Title className="mb-0">Add User</Card.Title>
          ) : (
            <>
              <h2 className="text-primary fw-bold">HR RagWiser</h2>
              <p className="text-muted">Create your account</p>
            </>
          )}
          {isEmbedded && onNavigateToLogin && (
            <Button
              variant="link"
              type="button"
              className="p-0 text-decoration-none"
              onClick={onNavigateToLogin}
            >
              Back to Login
            </Button>
          )}
        </div>

        {apiError && (
          <Alert variant="danger" dismissible onClose={() => setApiError('')}>
            {apiError}
          </Alert>
        )}

        {successMessage && (
          <Alert variant="success" dismissible onClose={() => setSuccessMessage('')}>
            {successMessage}
          </Alert>
        )}

        <Form onSubmit={handleSubmit}>
          <Row>
            <Col md={6}>
              <Form.Group className="mb-3" controlId="registerFirstName">
                <Form.Label>First Name</Form.Label>
                <Form.Control
                  type="text"
                  name="firstName"
                  placeholder="Enter first name"
                  value={formData.firstName}
                  onChange={handleChange}
                  isInvalid={!!errors.firstName}
                  disabled={loading}
                  autoComplete="given-name"
                />
                <Form.Control.Feedback type="invalid">{errors.firstName}</Form.Control.Feedback>
              </Form.Group>
            </Col>

            <Col md={6}>
              <Form.Group className="mb-3" controlId="registerLastName">
                <Form.Label>Last Name</Form.Label>
                <Form.Control
                  type="text"
                  name="lastName"
                  placeholder="Enter last name"
                  value={formData.lastName}
                  onChange={handleChange}
                  isInvalid={!!errors.lastName}
                  disabled={loading}
                  autoComplete="family-name"
                />
                <Form.Control.Feedback type="invalid">{errors.lastName}</Form.Control.Feedback>
              </Form.Group>
            </Col>
          </Row>

          <Form.Group className="mb-3" controlId="registerUsername">
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
              autoFocus={!isEmbedded}
            />
            <Form.Control.Feedback type="invalid">{errors.username}</Form.Control.Feedback>
          </Form.Group>

          <Form.Group className="mb-3" controlId="registerEmail">
            <Form.Label>Email</Form.Label>
            <Form.Control
              type="email"
              name="email"
              placeholder="Enter email address"
              value={formData.email}
              onChange={handleChange}
              isInvalid={!!errors.email}
              disabled={loading}
              autoComplete="email"
            />
            <Form.Control.Feedback type="invalid">{errors.email}</Form.Control.Feedback>
          </Form.Group>

          <Form.Group className={isEmbedded ? 'mb-3' : 'mb-4'} controlId="registerPassword">
            <Form.Label>Password</Form.Label>
            <Form.Control
              type="password"
              name="password"
              placeholder="Enter password"
              value={formData.password}
              onChange={handleChange}
              isInvalid={!!errors.password}
              disabled={loading}
              autoComplete="new-password"
            />
            <Form.Control.Feedback type="invalid">{errors.password}</Form.Control.Feedback>
            {!isEmbedded && !errors.password && (
              <Form.Text className="text-muted">
                Min 8 characters with uppercase, lowercase, and a digit.
              </Form.Text>
            )}
          </Form.Group>

          {isEmbedded && (
            <Form.Group className="mb-3" controlId="registerRole">
              <Form.Label>Role</Form.Label>
              <Form.Select
                name="role"
                value={formData.role}
                onChange={handleChange}
                isInvalid={!!errors.role}
                disabled={loading}
              >
                <option value="USER">USER</option>
                <option value="HR_MANAGER">HR_MANAGER</option>
                <option value="ADMIN">ADMIN</option>
              </Form.Select>
              <Form.Control.Feedback type="invalid">{errors.role}</Form.Control.Feedback>
            </Form.Group>
          )}

          <Button
            variant="primary"
            type="submit"
            className={isEmbedded ? '' : 'w-100 mb-3'}
            disabled={loading}
            size={isEmbedded ? undefined : 'lg'}
          >
            {loading ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                Creating account...
              </>
            ) : (
              isEmbedded ? 'Create user' : 'Create Account'
            )}
          </Button>

          {!isEmbedded && onNavigateToLogin && (
            <Button
              variant="link"
              type="button"
              className="w-100 mb-3 text-decoration-none"
              disabled={loading}
              onClick={onNavigateToLogin}
            >
              Back to Login
            </Button>
          )}
        </Form>
      </Card.Body>
    </Card>
  );

  if (isEmbedded) {
    return (
      <Row className="justify-content-center">
        <Col lg={8} xl={7}>
          {formContent}
        </Col>
      </Row>
    );
  }

  return (
    <Container fluid className="login-container">
      <Row className="justify-content-center align-items-center min-vh-100">
        <Col xs={12} sm={10} md={8} lg={6} xl={5}>
          {formContent}

          <div className="text-center mt-3 text-muted">
            <small>&copy; 2025 HR RagWiser. All rights reserved.</small>
          </div>
        </Col>
      </Row>
    </Container>
  );
}

export default UserRegistration;
