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
2. You must be authenticated with a valid JWT token

### Access URL

The control panel is available at:

```
http://localhost:8080/control-panel
```

For production environments, replace `localhost:8080` with your application's hostname and port.

## Authentication

All management endpoints (except `/health`) are secured and require authentication. To access the admin dashboard:

1. **Obtain a JWT Token**: First, authenticate through the `/api/v1/auth/login` endpoint to get a JWT token
2. **Include the Token**: Add the JWT token to your request headers:
   ```
   Authorization: Bearer <your-jwt-token>
   ```

### Example: Getting a JWT Token

```bash
# Login to get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"your-email@example.com","password":"your-password"}'

# Access control panel with token
curl http://localhost:8080/control-panel \
  -H "Authorization: Bearer <your-jwt-token>"
```

## Available Management Endpoints

### Control Panel
- **URL**: `/control-panel`
- **Description**: Web UI for managing the application
- **Authentication**: Required

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

1. **Production Use**: The control panel is primarily intended for development and debugging. If you use it in production:
   - Ensure all management endpoints are properly secured
   - Use strong authentication mechanisms
   - Consider restricting access by IP address or VPN
   - Monitor access logs for unauthorized attempts

2. **Authentication**: All management endpoints (except `/health`) require authentication via JWT token

3. **Sensitive Data**: The environment endpoint may expose sensitive configuration. Review what's exposed before enabling in production.

## Development vs Production

### Development Environment

In development (using `application-dev.properties`), you may want to:
- Keep all endpoints enabled for debugging
- Use the control panel freely for monitoring and testing

### Production Environment

In production (using `application-prod.properties`), consider:
- Disabling non-essential endpoints
- Restricting access to specific IP ranges
- Using environment variables for sensitive configuration
- Regular monitoring of endpoint access

## Troubleshooting

### Cannot Access Control Panel

1. **Check if the application is running**: Verify the application started successfully
2. **Verify authentication**: Ensure you have a valid JWT token
3. **Check configuration**: Confirm `endpoints.control-panel.enabled=true` in your properties file
4. **Review logs**: Check application logs for any errors

### 401 Unauthorized Error

- You need to authenticate first and include a valid JWT token in the Authorization header
- Ensure your JWT token hasn't expired

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
