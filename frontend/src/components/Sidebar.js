import React, { useState } from 'react';
import {
  FaChartBar, FaUpload, FaUsers, FaSearch, FaUsersCog,
  FaQuestionCircle, FaCog, FaSignOutAlt, FaChevronLeft, FaChevronRight
} from 'react-icons/fa';

const Sidebar = ({ activeTab, onTabChange, username, userRole, onLogout }) => {
  const [collapsed, setCollapsed] = useState(false);

  const initials = username
    ? username.slice(0, 2).toUpperCase()
    : 'U';

  const mainNavItems = [
    { key: 'dashboard', label: 'Dashboard', icon: <FaChartBar /> },
    { key: 'upload', label: 'Upload CV', icon: <FaUpload /> },
    { key: 'candidates', label: 'Candidates', icon: <FaUsers /> },
    { key: 'analysis', label: 'Job Analysis', icon: <FaSearch /> },
  ];

  const adminNavItems = userRole === 'ADMIN'
    ? [{ key: 'users', label: 'User Management', icon: <FaUsersCog /> }]
    : [];

  return (
    <nav className={`sidebar ${collapsed ? 'collapsed' : ''}`}>
      <div className="sidebar-brand">
        <div className="sidebar-brand-icon">HR</div>
        <span className="sidebar-brand-text">RagWiser</span>
      </div>

      <div className="sidebar-nav">
        <div className="sidebar-section-title">Main</div>
        {mainNavItems.map(item => (
          <button
            key={item.key}
            className={`sidebar-nav-item ${activeTab === item.key ? 'active' : ''}`}
            onClick={() => onTabChange(item.key)}
          >
            <span className="nav-icon">{item.icon}</span>
            <span className="sidebar-nav-label">{item.label}</span>
          </button>
        ))}

        {adminNavItems.length > 0 && (
          <>
            <div className="sidebar-section-title">Administration</div>
            {adminNavItems.map(item => (
              <button
                key={item.key}
                className={`sidebar-nav-item ${activeTab === item.key ? 'active' : ''}`}
                onClick={() => onTabChange(item.key)}
              >
                <span className="nav-icon">{item.icon}</span>
                <span className="sidebar-nav-label">{item.label}</span>
              </button>
            ))}
          </>
        )}
      </div>

      <div className="sidebar-footer">
        <button className="sidebar-footer-item">
          <FaQuestionCircle />
          <span className="sidebar-nav-label">Help and Support</span>
        </button>
        <button className="sidebar-footer-item" onClick={onLogout}>
          <FaSignOutAlt />
          <span className="sidebar-nav-label">Logout</span>
        </button>
      </div>

      <div className="sidebar-user">
        <div className="sidebar-user-avatar">{initials}</div>
        <div className="sidebar-user-info">
          <div className="sidebar-user-name">{username}</div>
          <div className="sidebar-user-role">{userRole || 'User'}</div>
        </div>
        <button
          className="sidebar-collapse-btn"
          onClick={() => setCollapsed(!collapsed)}
          title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? <FaChevronRight size={12} /> : <FaChevronLeft size={12} />}
        </button>
      </div>
    </nav>
  );
};

export default Sidebar;
