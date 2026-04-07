# Contributing to AI Research Panel System

Thank you for your interest in contributing to AI-RPS! This document provides guidelines for contributing to the project.

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment for all contributors.

## How to Contribute

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates. When creating a bug report, include:

- **Clear title and description**
- **Steps to reproduce** the issue
- **Expected behavior** vs actual behavior
- **Environment details** (OS, Docker version, Java version)
- **Logs and error messages**
- **Screenshots** if applicable

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, include:

- **Clear title and description**
- **Use case** and motivation
- **Proposed solution** or implementation approach
- **Alternative solutions** considered

### Pull Requests

1. **Fork the repository** and create your branch from `main`
2. **Follow coding standards** (see below)
3. **Write tests** for new functionality
4. **Update documentation** as needed
5. **Ensure tests pass** (`mvn test`)
6. **Create a pull request** with a clear description

## Development Setup

### Prerequisites

- Docker Desktop
- Git
- (Optional) Java 21 and Maven for local development

### Getting Started

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/ai-rps.git
cd ai-rps

# Create .env file
cp .env.example .env
# Edit .env and add your NVIDIA_API_KEY

# Start development environment
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# View logs
docker-compose logs -f app
```

### Running Tests

```bash
# Run all tests
docker-compose exec app mvn test

# Run specific test
docker-compose exec app mvn test -Dtest=DocumentProcessorTest

# Run with coverage
docker-compose exec app mvn test jacoco:report
```

## Coding Standards

### Java Style

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use meaningful variable and method names
- Maximum method length: 50 lines
- Maximum class length: 500 lines
- Prefer composition over inheritance

### Documentation

- Add Javadoc for all public classes and methods
- Include inline comments for complex logic
- Update README.md for user-facing changes
- Update COMPREHENSIVE-DOCUMENTATION.md for architectural changes

### Testing

- Minimum 80% code coverage for new code
- Write unit tests for business logic
- Write integration tests for API endpoints
- Use property-based tests for invariants (jqwik)

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**
```
feat(agents): add statistical analyst agent

Implements a new specialized agent focused on statistical analysis
of research methodologies.

Closes #123
```

```
fix(chunking): correct overlap calculation for multi-byte characters

The previous implementation incorrectly calculated overlap for
documents with multi-byte UTF-8 characters.

Fixes #456
```

## Project Structure

```
ai-rps/
├── src/main/java/com/aipanelist/  # Application code
├── src/test/java/com/aipanelist/  # Tests
├── docker-compose.yml              # Docker configuration
├── Dockerfile                      # Production image
├── pom.xml                         # Maven configuration
└── README.md                       # User documentation
```

## Adding New Features

### Adding a New Agent Type

1. Create agent class extending `AIAgent`
2. Implement `getSystemPrompt()` method
3. Add enum value to `AgentType`
4. Register in `PanelOrchestrator.createPanel()`
5. Write unit tests
6. Update documentation

### Adding a New API Endpoint

1. Define DTO classes
2. Add controller method with proper annotations
3. Implement service layer logic
4. Write unit and integration tests
5. Update API documentation

## Review Process

1. **Automated checks** must pass (tests, linting)
2. **Code review** by at least one maintainer
3. **Documentation review** for user-facing changes
4. **Testing verification** in development environment

## Questions?

- Open an issue for questions
- Join discussions in existing issues
- Check documentation in `/docs` folder

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
