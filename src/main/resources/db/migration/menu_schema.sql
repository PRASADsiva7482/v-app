-- Menu Details Master Table
CREATE TABLE IF NOT EXISTS menu_details_master (
    menu_id INT AUTO_INCREMENT PRIMARY KEY,
    menu_name VARCHAR(100) NOT NULL,
    menu_path VARCHAR(255),
    menu_icon VARCHAR(100),
    parent_id INT DEFAULT 0,
    menu_order INT DEFAULT 0,
    menu_level INT DEFAULT 0,
    resource_name VARCHAR(100) NOT NULL COMMENT 'Keycloak resource name',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resource_name (resource_name),
    INDEX idx_parent_id (parent_id),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Menu master data with Keycloak resource mapping';

-- Menu to Function Mapping Table
CREATE TABLE IF NOT EXISTS menu_to_function_mapping (
    seq_id INT AUTO_INCREMENT PRIMARY KEY,
    menu_id INT NOT NULL,
    function_name VARCHAR(100) NOT NULL,
    scope_name VARCHAR(100) NOT NULL COMMENT 'Keycloak scope name',
    function_description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (menu_id) REFERENCES menu_details_master(menu_id) ON DELETE CASCADE,
    INDEX idx_menu_id (menu_id),
    INDEX idx_scope_name (scope_name),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Mapping of menu items to functions/scopes';

-- Sample Data for Testing
-- Root Menus (Level 0)
INSERT INTO menu_details_master (menu_name, menu_path, menu_icon, parent_id, menu_order, menu_level, resource_name, is_active) VALUES
('Dashboard', '/dashboard', 'dashboard', 0, 1, 0, 'dashboard-resource', TRUE),
('User Management', '/users', 'people', 0, 2, 0, 'user-management-resource', TRUE),
('Reports', '/reports', 'assessment', 0, 3, 0, 'reports-resource', TRUE),
('Settings', '/settings', 'settings', 0, 4, 0, 'settings-resource', TRUE);

-- Child Menus (Level 1) - User Management submenu
INSERT INTO menu_details_master (menu_name, menu_path, menu_icon, parent_id, menu_order, menu_level, resource_name, is_active) VALUES
('User List', '/users/list', 'list', (SELECT menu_id FROM (SELECT menu_id FROM menu_details_master WHERE menu_name = 'User Management') AS temp), 1, 1, 'user-list-resource', TRUE),
('Create User', '/users/create', 'person_add', (SELECT menu_id FROM (SELECT menu_id FROM menu_details_master WHERE menu_name = 'User Management') AS temp), 2, 1, 'user-create-resource', TRUE),
('Roles', '/users/roles', 'admin_panel_settings', (SELECT menu_id FROM (SELECT menu_id FROM menu_details_master WHERE menu_name = 'User Management') AS temp), 3, 1, 'user-roles-resource', TRUE);

-- Sample Functions/Scopes
INSERT INTO menu_to_function_mapping (menu_id, function_name, scope_name, function_description, is_active) VALUES
-- Dashboard functions
((SELECT menu_id FROM menu_details_master WHERE menu_name = 'Dashboard' LIMIT 1), 'View Dashboard', 'dashboard:view', 'View dashboard data', TRUE),
((SELECT menu_id FROM menu_details_master WHERE menu_name = 'Dashboard' LIMIT 1), 'Export Dashboard', 'dashboard:export', 'Export dashboard data', TRUE),

-- User List functions
((SELECT menu_id FROM menu_details_master WHERE menu_name = 'User List' LIMIT 1), 'View Users', 'user:view', 'View user list', TRUE),
((SELECT menu_id FROM menu_details_master WHERE menu_name = 'User List' LIMIT 1), 'Edit User', 'user:edit', 'Edit user details', TRUE),
((SELECT menu_id FROM menu_details_master WHERE menu_name = 'User List' LIMIT 1), 'Delete User', 'user:delete', 'Delete user', TRUE),

-- Create User functions
((SELECT menu_id FROM menu_details_master WHERE menu_name = 'Create User' LIMIT 1), 'Create User', 'user:create', 'Create new user', TRUE),

-- Roles functions
((SELECT menu_id FROM menu_details_master WHERE menu_name = 'Roles' LIMIT 1), 'View Roles', 'role:view', 'View roles', TRUE),
((SELECT menu_id FROM menu_details_master WHERE menu_name = 'Roles' LIMIT 1), 'Manage Roles', 'role:manage', 'Manage roles', TRUE),

-- Reports functions
((SELECT menu_id FROM menu_details_master WHERE menu_name = 'Reports' LIMIT 1), 'View Reports', 'report:view', 'View reports', TRUE),
((SELECT menu_id FROM menu_details_master WHERE menu_name = 'Reports' LIMIT 1), 'Generate Reports', 'report:generate', 'Generate new reports', TRUE),

-- Settings functions
((SELECT menu_id FROM menu_details_master WHERE menu_name = 'Settings' LIMIT 1), 'View Settings', 'settings:view', 'View settings', TRUE),
((SELECT menu_id FROM menu_details_master WHERE menu_name = 'Settings' LIMIT 1), 'Update Settings', 'settings:update', 'Update settings', TRUE);
