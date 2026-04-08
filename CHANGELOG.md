# Changelog

All notable changes to the AI Panelist System will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive API documentation guide (API-GUIDE.md)
- Docker deployment guide (DOCKER-GUIDE.md)
- Quick start guide for new users (GETTING-STARTED.md)
- Centralized documentation summary (DOCUMENTATION-SUMMARY.md)
- GitHub documentation structure (.github/DOCUMENTATION.md)
- Enhanced Makefile with additional utility commands

### Changed
- Restructured documentation into focused, purpose-specific guides
- Streamlined README.md for better first-time user experience
- Updated docker-start.bat with improved error handling
- Consolidated project documentation for better maintainability

### Removed
- Legacy comprehensive documentation file (COMPREHENSIVE-DOCUMENTATION.md)
- Redundant Docker documentation (README-DOCKER.md)
- Outdated setup guides (GITHUB_SETUP.md, SWAGGER-TESTING-GUIDE.md)
- Test suite files (to be reimplemented with updated architecture)
- Legacy batch scripts (install-dependencies.bat, setup-dependencies.bat, quick-start.bat, verify-docker-only.bat)
- Production Docker Compose configuration (docker-compose.prod.yml)
- Nginx configuration files (nginx/nginx.conf)
- Test artifacts and sample PDFs
- Task completion documentation (TASK_1_COMPLETION.md, PDF-EXPORT-FEATURE.md)
- JQwik database file (.jqwik-database)

## [1.0.0] - Initial Release

### Added
- Multi-agent AI panel system for document analysis
- Six specialized AI agents (Quick Screener, Fact Extractor, Literature Reviewer, Methodology Reviewer, General Analyst, Lead Analyst)
- NVIDIA NIM API integration for LLM capabilities
- Consensus engine for synthesizing agent analyses
- PDF export functionality with styled reports
- RESTful API with OpenAPI/Swagger documentation
- PostgreSQL database for persistent storage
- Docker containerization for easy deployment
- Asynchronous processing pipeline
- Comprehensive error handling and logging
- Health check endpoints
- Document chunking and processing system
