import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Badge, Modal, Form, Spinner, Alert, Pagination, Row, Col } from 'react-bootstrap';
import axios from 'axios';
import UserRegistration from './UserRegistration';

function UserManagement() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Pagination & Filtering state
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  
  const [filterUsername, setFilterUsername] = useState('');
  const [filterEmail, setFilterEmail] = useState('');
  const [filterRole, setFilterRole] = useState('');
  
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  
  const [selectedUser, setSelectedUser] = useState(null);
  const [editFormData, setEditFormData] = useState({});
  const [editError, setEditError] = useState(null);
  const [editLoading, setEditLoading] = useState(false);

  useEffect(() => {
    fetchUsers();
  }, [page, size]); // Re-fetch when page or size changes

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page, size });
      if (filterUsername) params.append('username', filterUsername);
      if (filterEmail) params.append('email', filterEmail);
      if (filterRole) params.append('role', filterRole);
      
      const response = await axios.get(`/api/users/search?${params.toString()}`);
      setUsers(response.data.content || response.data);
      setTotalPages(response.data.totalPages || 0);
      setError(null);
    } catch (err) {
      setError('Failed to load users');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleFilterSubmit = (e) => {
    e.preventDefault();
    setPage(0); // Reset to first page
    fetchUsers();
  };

  const handleClearFilters = () => {
    setFilterUsername('');
    setFilterEmail('');
    setFilterRole('');
    setPage(0);
    // Setting state is async, so we fetch directly with empty filters to ensure immediate refresh
    setLoading(true);
    axios.get(`/api/users/search?page=0&size=${size}`)
      .then(res => {
        setUsers(res.data.content || res.data);
        setTotalPages(res.data.totalPages || 0);
        setError(null);
      })
      .catch(err => {
        setError('Failed to load users');
        console.error(err);
      })
      .finally(() => setLoading(false));
  };

  const handleEditClick = (user) => {
    setSelectedUser(user);
    setEditFormData({
      firstName: user.firstName || '',
      lastName: user.lastName || '',
      email: user.email || '',
      role: user.role || 'USER',
      enabled: user.enabled,
    });
    setEditError(null);
    setShowEditModal(true);
  };

  const handleDeleteClick = (user) => {
    setSelectedUser(user);
    setShowDeleteModal(true);
  };

  const handleEditChange = (e) => {
    const { name, value, type, checked } = e.target;
    setEditFormData({
      ...editFormData,
      [name]: type === 'checkbox' ? checked : value
    });
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    setEditLoading(true);
    setEditError(null);
    try {
      await axios.put(`/api/users/${selectedUser.id}`, editFormData);
      setShowEditModal(false);
      fetchUsers();
    } catch (err) {
      const responseData = err.response?.data;
      if (typeof responseData?.validationErrors === 'object') {
        const issues = Object.values(responseData.validationErrors).join(', ');
        setEditError(issues);
      } else {
        setEditError(responseData?.message || 'Failed to update user');
      }
    } finally {
      setEditLoading(false);
    }
  };

  const confirmDelete = async () => {
    setEditLoading(true);
    try {
      await axios.delete(`/api/users/${selectedUser.id}`);
      setShowDeleteModal(false);
      fetchUsers();
    } catch (err) {
      console.error(err);
      alert('Failed to delete user');
    } finally {
      setEditLoading(false);
    }
  };

  if (loading && users.length === 0) {
    return <div className="text-center p-5"><Spinner animation="border" /></div>;
  }

  return (
    <div className="user-management">
      <div className="page-header">
        <h2>User Management</h2>
        <Button variant="primary" onClick={() => setShowAddModal(true)}>
          + Add New User
        </Button>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      <Card className="mb-4">
        <Card.Body>
          <Form onSubmit={handleFilterSubmit}>
            <Row className="g-3">
              <Col md={3}>
                <Form.Group>
                  <Form.Label>Username</Form.Label>
                  <Form.Control 
                    type="text" 
                    placeholder="Filter by username" 
                    value={filterUsername}
                    onChange={(e) => setFilterUsername(e.target.value)}
                  />
                </Form.Group>
              </Col>
              <Col md={3}>
                <Form.Group>
                  <Form.Label>Email</Form.Label>
                  <Form.Control 
                    type="text" 
                    placeholder="Filter by email" 
                    value={filterEmail}
                    onChange={(e) => setFilterEmail(e.target.value)}
                  />
                </Form.Group>
              </Col>
              <Col md={3}>
                <Form.Group>
                  <Form.Label>Role</Form.Label>
                  <Form.Select 
                    value={filterRole}
                    onChange={(e) => setFilterRole(e.target.value)}
                  >
                    <option value="">All Roles</option>
                    <option value="USER">USER</option>
                    <option value="HR_MANAGER">HR_MANAGER</option>
                    <option value="ADMIN">ADMIN</option>
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={3} className="d-flex align-items-end">
                <div className="d-flex gap-2 w-100">
                  <Button variant="primary" type="submit" className="flex-grow-1">
                    Search
                  </Button>
                  <Button variant="outline-secondary" onClick={handleClearFilters}>
                    Clear
                  </Button>
                </div>
              </Col>
            </Row>
          </Form>
        </Card.Body>
      </Card>

      <Card>
        <Table responsive hover className="mb-0">
          <thead>
            <tr>
              <th>Username</th>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map(user => (
              <tr key={user.id}>
                <td className="align-middle fw-bold">{user.username}</td>
                <td className="align-middle">{user.firstName} {user.lastName}</td>
                <td className="align-middle">{user.email}</td>
                <td className="align-middle">
                  <Badge bg={user.role === 'ADMIN' ? 'danger' : user.role === 'HR_MANAGER' ? 'info' : 'secondary'}>
                    {user.role}
                  </Badge>
                </td>
                <td className="align-middle">
                  <Badge bg={user.enabled ? 'success' : 'warning'}>
                    {user.enabled ? 'Active' : 'Disabled'}
                  </Badge>
                </td>
                <td className="align-middle">
                  <Button variant="outline-primary" size="sm" className="me-2" onClick={() => handleEditClick(user)}>
                    Edit
                  </Button>
                  <Button variant="outline-danger" size="sm" onClick={() => handleDeleteClick(user)}>
                    Delete
                  </Button>
                </td>
              </tr>
            ))}
            {users.length === 0 && (
              <tr>
                <td colSpan="6" className="text-center py-4 text-muted">No users found.</td>
              </tr>
            )}
          </tbody>
        </Table>
        {totalPages > 1 && (
          <Card.Footer className="bg-white d-flex justify-content-center pt-3 pb-2">
            <Pagination className="mb-0">
              <Pagination.First onClick={() => setPage(0)} disabled={page === 0} />
              <Pagination.Prev onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} />
              {[...Array(totalPages)].map((_, idx) => (
                <Pagination.Item key={idx} active={idx === page} onClick={() => setPage(idx)}>
                  {idx + 1}
                </Pagination.Item>
              ))}
              <Pagination.Next onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page === totalPages - 1} />
              <Pagination.Last onClick={() => setPage(totalPages - 1)} disabled={page === totalPages - 1} />
            </Pagination>
          </Card.Footer>
        )}
      </Card>

      {/* Add User Modal */}
      <Modal show={showAddModal} onHide={() => setShowAddModal(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Add New User</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <UserRegistration 
            isEmbedded={true} 
            onSuccess={() => {
              setShowAddModal(false);
              fetchUsers();
            }} 
          />
        </Modal.Body>
      </Modal>

      {/* Edit User Modal */}
      <Modal show={showEditModal} onHide={() => setShowEditModal(false)}>
        <Form onSubmit={handleEditSubmit}>
          <Modal.Header closeButton>
            <Modal.Title>Edit User: {selectedUser?.username}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            {editError && <Alert variant="danger">{editError}</Alert>}
            
            <Form.Group className="mb-3">
              <Form.Label>First Name</Form.Label>
              <Form.Control
                type="text"
                name="firstName"
                value={editFormData.firstName}
                onChange={handleEditChange}
              />
            </Form.Group>
            
            <Form.Group className="mb-3">
              <Form.Label>Last Name</Form.Label>
              <Form.Control
                type="text"
                name="lastName"
                value={editFormData.lastName}
                onChange={handleEditChange}
              />
            </Form.Group>
            
            <Form.Group className="mb-3">
              <Form.Label>Email</Form.Label>
              <Form.Control
                type="email"
                name="email"
                value={editFormData.email}
                onChange={handleEditChange}
              />
            </Form.Group>
            
            <Form.Group className="mb-3">
              <Form.Label>Role</Form.Label>
              <Form.Select name="role" value={editFormData.role} onChange={handleEditChange}>
                <option value="USER">USER</option>
                <option value="HR_MANAGER">HR_MANAGER</option>
                <option value="ADMIN">ADMIN</option>
              </Form.Select>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Check 
                type="checkbox"
                id="enabled-check"
                label="Account Enabled"
                name="enabled"
                checked={editFormData.enabled || false}
                onChange={handleEditChange}
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowEditModal(false)}>Cancel</Button>
            <Button variant="primary" type="submit" disabled={editLoading}>
              {editLoading ? <Spinner size="sm" animation="border" /> : 'Save Changes'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Delete User Modal */}
      <Modal show={showDeleteModal} onHide={() => setShowDeleteModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Confirm Delete</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          Are you sure you want to delete the user <strong>{selectedUser?.username}</strong>? This action cannot be undone.
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowDeleteModal(false)}>Cancel</Button>
          <Button variant="danger" onClick={confirmDelete} disabled={editLoading}>
            {editLoading ? <Spinner size="sm" animation="border" /> : 'Delete User'}
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}

export default UserManagement;
