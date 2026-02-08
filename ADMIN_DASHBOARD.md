# Admin Dashboard Guide

This document explains how to use the admin dashboard (Micronaut Control Panel) in this project.

## Overview

The admin dashboard provides a web-based interface to monitor and manage your Micronaut application. It includes various management endpoints that allow you to:

- View application beans and their dependencies
- Monitor environment properties and configuration
- Check application health and metrics
- Manage loggers and log levels at runtime
- View application information
- Refresh configuration dynamically

## Accessing the Admin Dashboard

### Prerequisites

1. The application must be running
2. For development: Direct browser access is enabled
3. For production: Access should be restricted via firewall/VPN

### Access URL

The control panel is available at:

```
http://localhost:8080/control-panel
```

Simply open this URL in your browser - no authentication is required for the control panel UI itself.

For production environments, replace `localhost:8080` with your application's hostname and port.

## Authentication

### Control Panel (Web UI)

- **URL**: `/control-panel`
- **Access**: Open in browser without authentication
- **⚠️ Production Warning**: This is configured for development convenience. In production, restrict access using:
  - Network firewall rules
  - VPN access only
  - IP whitelisting
  - Or reconfigure to require authentication

## Management API Endpoints

Other management endpoints (except `/health`) require JWT authentication when accessed programmatically:

1. **Obtain a JWT Token**: First, authenticate through the `/api/v1/auth/login` endpoint to get a JWT token
2. **Include the Token**: Add the JWT token to your request headers:
   ```
   Authorization: Bearer <your-jwt-token>
   ```

### Example: Getting a JWT Token for API Endpoints

```bash
# Login to get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"your-email@example.com","password":"your-password"}'

# Access management endpoint with token (e.g., beans)
curl http://localhost:8080/beans \
  -H "Authorization: Bearer <your-jwt-token>"
```

Note: The control panel web UI at `/control-panel` does NOT require JWT authentication - just open it in your browser.

## Available Management Endpoints

### Control Panel
- **URL**: `/control-panel`
- **Description**: Web UI for managing the application
- **Authentication**: Not required (open in browser directly)
- **⚠️ Production**: Restrict via firewall/VPN

### Health Check
- **URL**: `/health`
- **Description**: Application health status (publicly accessible for load balancers)
- **Authentication**: Not required

### Application Info
- **URL**: `/info`
- **Description**: General application information
- **Authentication**: Required

### Beans
- **URL**: `/beans`
- **Description**: List all application beans and their dependencies
- **Authentication**: Required

### Environment
- **URL**: `/env`
- **Description**: View environment properties and configuration
- **Authentication**: Required

### Loggers
- **URL**: `/loggers`
- **Description**: View and modify logger levels at runtime
- **Authentication**: Required

### Refresh
- **URL**: `/refresh`
- **Description**: Refresh application configuration
- **Authentication**: Required

## Configuration

The admin dashboard is configured in `application.properties`:

```properties
# Enable management endpoints
endpoints.info.enabled=true
endpoints.beans.enabled=true
endpoints.env.enabled=true
endpoints.loggers.enabled=true
endpoints.refresh.enabled=true

# Enable Control Panel
endpoints.control-panel.enabled=true
endpoints.control-panel.sensitive=true
```

## Security Considerations

⚠️ **Important Security Notes**:

1. **Control Panel Access**: 
   - **Development**: The control panel (`/control-panel`) is accessible without authentication for convenience
   - **Production**: MUST restrict access using:
     - Network firewall rules (block port 8080 from public internet)
     - VPN-only access
     - IP whitelisting at load balancer/proxy level
     - Or reconfigure to require authentication (see below)

2. **Management API Endpoints**: Other management endpoints (`/beans`, `/env`, `/loggers`, etc.) require JWT authentication

3. **Sensitive Data**: The environment endpoint may expose sensitive configuration. Review what's exposed before enabling in production.

4. **To Require Authentication for Control Panel** (optional, for production):
   ```properties
   # Change isAnonymous() to isAuthenticated() in application.properties
   micronaut.security.intercept-url-map[1].access[0]=isAuthenticated()
   ```

## Development vs Production

### Development Environment

In development (using `application-dev.properties`):
- Control panel is directly accessible in browser at `/control-panel`
- All endpoints enabled for debugging
- No authentication needed for control panel UI

### Production Environment

In production (using `application-prod.properties`), consider:
- **CRITICAL**: Block `/control-panel` access from public internet using firewall/VPN
- Disabling non-essential endpoints
- Requiring authentication for control panel (see Security Considerations above)
- Using environment variables for sensitive configuration
- Regular monitoring of endpoint access

## Troubleshooting

### Cannot Access Control Panel

1. **Check if the application is running**: Verify the application started successfully
2. **Check URL**: Ensure you're accessing `http://localhost:8080/control-panel` (not `/control-panel/`)
3. **Check configuration**: Confirm `endpoints.control-panel.enabled=true` in your properties file
4. **Review logs**: Check application logs for any errors
5. **Clear browser cache**: Try in incognito/private mode

### 401 Unauthorized Error

**For Control Panel (`/control-panel`):**
- This should NOT happen - control panel is configured for anonymous access
- If you see 401, check that `micronaut.security.intercept-url-map[1].access[0]=isAnonymous()` is set
- Verify no proxy/firewall is adding authentication requirements

**For Other Management Endpoints (`/beans`, `/env`, etc.):**

### 403 Forbidden Error

- Your user account may not have sufficient permissions
- Check the security configuration in `ManagementSecurityConfiguration.java`

## Additional Resources

- [Micronaut Control Panel Documentation](https://micronaut-projects.github.io/micronaut-control-panel/latest/guide/)
- [Micronaut Management & Monitoring](https://docs.micronaut.io/latest/guide/#management)
- [Micronaut Security](https://micronaut-projects.github.io/micronaut-security/latest/guide/)

## Example Use Cases

### 1. Changing Log Levels at Runtime

```bash
# View current logger configuration
curl http://localhost:8080/loggers \
  -H "Authorization: Bearer <token>"

# Change a specific logger level to DEBUG
curl -X POST http://localhost:8080/loggers/com.anonymous.wall \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'
```

### 2. Viewing Application Beans

```bash
curl http://localhost:8080/beans \
  -H "Authorization: Bearer <token>"
```

### 3. Checking Application Health

```bash
# No authentication needed for health check
curl http://localhost:8080/health
```

### 4. Refreshing Configuration

```bash
curl -X POST http://localhost:8080/refresh \
  -H "Authorization: Bearer <token>"
```
