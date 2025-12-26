import axios from 'axios';

/**
 * Authentication service for managing user authentication and tokens
 */
const authService = {
  /**
   * Login user with credentials
   * @param {string} username
   * @param {string} password
   * @returns {Promise<Object>} Login response with token
   */
  login: async (username, password) => {
    try {
      const response = await axios.post('/api/auth/login', {
        username,
        password
      });

      if (response.data.token) {
        // Store authentication data
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('username', response.data.username);
        localStorage.setItem('tokenExpiration', Date.now() + response.data.expiresIn);
      }

      return response.data;
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  },

  /**
   * Logout user and clear authentication data
   */
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('tokenExpiration');
  },

  /**
   * Get stored JWT token
   * @returns {string|null} JWT token or null if not found
   */
  getToken: () => {
    return localStorage.getItem('token');
  },

  /**
   * Get stored username
   * @returns {string|null} Username or null if not found
   */
  getUsername: () => {
    return localStorage.getItem('username');
  },

  /**
   * Check if user is authenticated with a valid token
   * @returns {boolean} True if authenticated and token not expired
   */
  isAuthenticated: () => {
    const token = authService.getToken();
    const expiration = localStorage.getItem('tokenExpiration');

    if (!token || !expiration) {
      return false;
    }

    // Check if token has expired
    if (Date.now() > parseInt(expiration)) {
      authService.logout();
      return false;
    }

    return true;
  },

  /**
   * Verify token with backend
   * @returns {Promise<boolean>} True if token is valid
   */
  verifyToken: async () => {
    try {
      const token = authService.getToken();
      if (!token) {
        return false;
      }

      const response = await axios.get('/api/auth/verify', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      return response.data.valid === true;
    } catch (error) {
      console.error('Token verification error:', error);
      return false;
    }
  },

  /**
   * Get time remaining until token expires
   * @returns {number} Milliseconds until expiration, or 0 if expired/not authenticated
   */
  getTimeToExpiration: () => {
    const expiration = localStorage.getItem('tokenExpiration');
    if (!expiration) {
      return 0;
    }

    const timeRemaining = parseInt(expiration) - Date.now();
    return Math.max(0, timeRemaining);
  }
};

/**
 * Axios interceptor to add Authorization header to all requests
 */
axios.interceptors.request.use(
  (config) => {
    const token = authService.getToken();
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Axios interceptor to handle 401 responses (token expiration)
 */
axios.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      // Token expired or invalid
      console.warn('Authentication failed, token may be expired');
      authService.logout();

      // Note: The App component will handle showing the login screen
      // when it detects the user is no longer authenticated
    }
    return Promise.reject(error);
  }
);

export default authService;

