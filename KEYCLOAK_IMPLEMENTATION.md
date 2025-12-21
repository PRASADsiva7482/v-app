# Keycloak Authentication & Menu Management Implementation

## Overview
This implementation provides Keycloak-based authentication with dynamic, role-based menu management. It extracts resources and scopes from Keycloak tokens and builds a hierarchical menu structure based on user permissions.

## Architecture

### Key Components

1. **LoginController** (`web/rest/controller/LoginController.java`)
   - REST endpoint for login validation
   - Endpoint: `GET /checkLogin`
   - Returns user details with role-based menus

2. **CheckLoginService** (`service/CheckLoginServiceImpl.java`)
   - Validates Keycloak tokens
   - Extracts user details and permissions
   - Builds hierarchical menu structure

3. **KeycloakRequestMaker** (`service/KeycloakRequestMaker.java`)
   - Handles Keycloak authentication requests
   - Token validation and introspection

4. **MenuAndRoleIteratorService** (`service/MenuAndRoleIteratorService.java`)
   - Iterates through menus and functions
   - Filters based on Keycloak resources and scopes

5. **MDCFilter** (`config/MDCFilter.java`)
   - Populates MDC with request headers
   - Enables context propagation throughout request lifecycle

## Database Schema

### menu_details_master
Stores menu hierarchy with Keycloak resource mapping:
- `menu_id`: Primary key
- `menu_name`: Display name
- `menu_path`: Route path
- `menu_icon`: Icon identifier
- `parent_id`: Parent menu ID (0 for root)
- `menu_order`: Display order
- `menu_level`: Hierarchy level (0 = root)
- `resource_name`: **Keycloak resource name** for authorization

### menu_to_function_mapping
Maps menus to functions/scopes:
- `seq_id`: Primary key
- `menu_id`: Foreign key to menu_details_master
- `function_name`: Function display name
- `scope_name`: **Keycloak scope name** for authorization
- `function_description`: Description

## Configuration

### application-keycloak.properties
```properties
keycloak.enabled=true
keycloak.auth-server-url=http://localhost:8180/auth
keycloak.realm=your-realm
keycloak.resource=your-client-id
keycloak.credentials.secret=your-client-secret
keycloak.grant-type=urn:ietf:params:oauth:grant-type:uma-ticket
keycloak.audience=your-audience
```

### Required Keycloak Setup

1. **Create Realm** in Keycloak admin console

2. **Create Client**
   - Client ID: Match `keycloak.resource`
   - Access Type: `confidential`
   - Authorization Enabled: `ON`
   - Service Accounts Enabled: `ON`

3. **Configure Resources**
   Create resources matching `menu_details_master.resource_name`:
   - `dashboard-resource`
   - `user-management-resource`
   - `user-list-resource`
   - `user-create-resource`
   - etc.

4. **Configure Scopes**
   Create scopes matching `menu_to_function_mapping.scope_name`:
   - `dashboard:view`
   - `dashboard:export`
   - `user:view`
   - `user:edit`
   - `user:delete`
   - `user:create`
   - etc.

5. **Create Policies**
   - Role-based policies
   - Group-based policies
   - Custom policies as needed

6. **Create Permissions**
   Link resources and scopes to policies

## API Usage

### Check Login
```bash
curl -X GET "http://localhost:8080/v-app/checkLogin" \
  -H "Authorization: Bearer YOUR_KEYCLOAK_TOKEN"
```

### Response Structure
```json
{
  "userId": "user-uuid",
  "userName": "john.doe",
  "fullName": "John Doe",
  "emailId": "john.doe@example.com",
  "groupNames": "admin,users",
  "roleDetails": {
    "menuIds": [
      {
        "menuId": 1,
        "menuName": "Dashboard",
        "menuPath": "/dashboard",
        "menuIcon": "dashboard",
        "parentId": 0,
        "menuOrder": 1,
        "menuLevel": 0,
        "resourceName": "dashboard-resource",
        "functionIds": [
          {
            "functionId": 1,
            "menuId": 1,
            "functionName": "View Dashboard",
            "scopeName": "dashboard:view",
            "functionDescription": "View dashboard data"
          }
        ],
        "childs": []
      },
      {
        "menuId": 2,
        "menuName": "User Management",
        "menuPath": "/users",
        "menuIcon": "people",
        "parentId": 0,
        "menuOrder": 2,
        "menuLevel": 0,
        "resourceName": "user-management-resource",
        "functionIds": [],
        "childs": [
          {
            "menuId": 5,
            "menuName": "User List",
            "menuPath": "/users/list",
            "menuIcon": "list",
            "parentId": 2,
            "menuOrder": 1,
            "menuLevel": 1,
            "resourceName": "user-list-resource",
            "functionIds": [
              {
                "functionId": 3,
                "menuId": 5,
                "functionName": "View Users",
                "scopeName": "user:view"
              },
              {
                "functionId": 4,
                "menuId": 5,
                "functionName": "Edit User",
                "scopeName": "user:edit"
              }
            ],
            "childs": null
          }
        ]
      }
    ]
  }
}
```

## Flow Diagram

```
1. Client sends request with Bearer token
   ↓
2. MDCFilter extracts headers to MDC
   ↓
3. LoginController.checkLogin()
   ↓
4. CheckLoginServiceImpl.validateLogin()
   ↓
5. KeycloakRequestMaker.initiateSSOReq()
   - Calls Keycloak token endpoint
   - Gets RPT (Requesting Party Token) with permissions
   ↓
6. Parse AccessToken
   - Extract user details (username, email, etc.)
   - Extract permissions (resources + scopes)
   ↓
7. MenuAndRoleIteratorService.iterateMenuAndRoleDetails()
   - Query menu_details_master WHERE resource_name IN (extracted resources)
   - Query menu_to_function_mapping WHERE scope_name IN (extracted scopes)
   - Build hierarchical menu structure
   ↓
8. Return UserDetailsBean with menus
   ↓
9. Client receives user details + authorized menus
```

## Menu Exclusion Logic

The menu exclusion is handled automatically through Keycloak permissions:
- If a user doesn't have access to a resource, that menu won't appear
- If a user doesn't have a specific scope, that function won't be available
- Hierarchical menus are filtered - parent menus without children are excluded

## Integration with Frontend

### React/Angular Example
```javascript
// After login, call checkLogin API
const response = await fetch('/v-app/checkLogin', {
  headers: {
    'Authorization': `Bearer ${keycloakToken}`
  }
});

const userData = await response.json();

// Build menu from roleDetails.menuIds
const buildMenu = (menuItems) => {
  return menuItems.map(menu => ({
    id: menu.menuId,
    label: menu.menuName,
    path: menu.menuPath,
    icon: menu.menuIcon,
    children: menu.childs ? buildMenu(menu.childs) : [],
    functions: menu.functionIds.map(f => f.scopeName)
  }));
};

const navigationMenu = buildMenu(userData.roleDetails.menuIds);
```

## Security Considerations

1. **Token Validation**: Always validate tokens with Keycloak
2. **HTTPS**: Use HTTPS in production
3. **Token Expiry**: Handle token refresh
4. **CORS**: Configure appropriate CORS settings
5. **Rate Limiting**: Implement rate limiting on login endpoint

## Troubleshooting

### Common Issues

1. **"Token is not available to validate"**
   - Ensure Authorization header is sent
   - Check MDCFilter is registered

2. **Empty menu list**
   - Verify Keycloak resources match database resource_name
   - Check user has permissions assigned
   - Verify menu_details_master has is_active=true

3. **Missing functions**
   - Verify Keycloak scopes match database scope_name
   - Check menu_to_function_mapping has is_active=true

4. **401 Unauthorized**
   - Token expired - refresh token
   - Invalid client credentials
   - Keycloak server unreachable

## Testing

### Manual Testing with Keycloak

1. Get token from Keycloak:
```bash
curl -X POST "http://localhost:8180/auth/realms/your-realm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=your-client-id" \
  -d "client_secret=your-client-secret" \
  -d "grant_type=password" \
  -d "username=testuser" \
  -d "password=testpass"
```

2. Use token to call checkLogin:
```bash
curl -X GET "http://localhost:8080/v-app/checkLogin" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Dependencies Added

```xml
<!-- Keycloak -->
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-core</artifactId>
    <version>26.0.7</version>
</dependency>
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-admin-client</artifactId>
    <version>26.0.7</version>
</dependency>
```

## Next Steps

1. **Configure Keycloak** with your realm, client, resources, and scopes
2. **Run SQL scripts** to create menu tables
3. **Update application-keycloak.properties** with your Keycloak details
4. **Test the checkLogin endpoint** with a valid token
5. **Integrate with frontend** to build dynamic navigation

## Support

For issues or questions:
1. Check Keycloak admin console for proper configuration
2. Verify database has menu data
3. Check application logs for detailed error messages
4. Ensure all dependencies are properly resolved
