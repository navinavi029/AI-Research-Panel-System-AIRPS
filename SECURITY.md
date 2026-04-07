# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

We take the security of AI Research Panel System seriously. If you discover a security vulnerability, please follow these steps:

### 1. Do Not Disclose Publicly

Please do not open a public GitHub issue for security vulnerabilities.

### 2. Report Privately

Send details to the project maintainers via:
- GitHub Security Advisories (preferred)
- Email to the project maintainers

### 3. Include Details

Your report should include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

### 4. Response Timeline

- **Initial Response**: Within 48 hours
- **Status Update**: Within 7 days
- **Fix Timeline**: Depends on severity
  - Critical: 1-7 days
  - High: 7-30 days
  - Medium: 30-90 days
  - Low: Best effort

## Security Best Practices

### For Users

1. **API Keys**: Never commit `.env` files or API keys to version control
2. **Updates**: Keep Docker images and dependencies up to date
3. **Network**: Use HTTPS in production with valid SSL certificates
4. **Access**: Implement authentication for production deployments
5. **Monitoring**: Enable logging and monitor for suspicious activity

### For Contributors

1. **Dependencies**: Run `mvn dependency:check` regularly
2. **Secrets**: Use environment variables for sensitive data
3. **Input Validation**: Validate all user inputs
4. **SQL Injection**: Use parameterized queries (JPA handles this)
5. **XSS**: Sanitize outputs in API responses

## Known Security Considerations

### Current Limitations

1. **No Authentication**: The current version does not include authentication
   - Recommended: Implement OAuth 2.0 or API key authentication for production
   
2. **Rate Limiting**: Basic rate limiting via NVIDIA API only
   - Recommended: Implement application-level rate limiting

3. **File Upload**: PDF validation is basic
   - Recommended: Implement virus scanning for production

### Planned Security Enhancements

- OAuth 2.0 / JWT authentication
- Role-based access control (RBAC)
- API rate limiting per client
- Audit logging
- Data encryption at rest
- Enhanced input validation

## Security Updates

Security updates will be released as patch versions (e.g., 1.0.1) and announced via:
- GitHub Security Advisories
- Release notes
- README.md updates

## Acknowledgments

We appreciate responsible disclosure and will acknowledge security researchers who report vulnerabilities (with permission).
