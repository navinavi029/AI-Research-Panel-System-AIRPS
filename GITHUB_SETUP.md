# GitHub Repository Setup Guide

This guide will help you publish the AI Research Panel System to GitHub.

## Prerequisites

- Git installed on your system
- GitHub account
- Repository already initialized locally (✓ Done)

## Step 1: Create GitHub Repository

1. Go to [GitHub](https://github.com) and sign in
2. Click the **+** icon in the top right corner
3. Select **New repository**
4. Configure your repository:
   - **Repository name**: `ai-rps` (or your preferred name)
   - **Description**: "Multi-agent AI system for research document analysis with 6 specialized agents"
   - **Visibility**: Public
   - **DO NOT** initialize with README, .gitignore, or license (we already have these)
5. Click **Create repository**

## Step 2: Add Remote and Push

After creating the repository, GitHub will show you commands. Use these:

```bash
# Add GitHub as remote origin
git remote add origin https://github.com/YOUR_USERNAME/ai-rps.git

# Verify remote was added
git remote -v

# Push to GitHub
git push -u origin main
```

If you're on `master` branch instead of `main`:
```bash
# Rename branch to main
git branch -M main

# Push to GitHub
git push -u origin main
```

## Step 3: Configure Repository Settings

### Enable GitHub Actions

1. Go to your repository on GitHub
2. Click **Settings** tab
3. Click **Actions** → **General** in the left sidebar
4. Under "Actions permissions", select **Allow all actions and reusable workflows**
5. Click **Save**

### Add Secrets

1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Add the following secret:
   - **Name**: `NVIDIA_API_KEY`
   - **Value**: Your NVIDIA API key
4. Click **Add secret**

### Enable Issues and Discussions

1. Go to **Settings** → **General**
2. Under "Features":
   - ✓ Check **Issues**
   - ✓ Check **Discussions** (optional but recommended)
3. Scroll down and click **Save changes**

### Configure Branch Protection (Optional but Recommended)

1. Go to **Settings** → **Branches**
2. Click **Add branch protection rule**
3. Configure:
   - **Branch name pattern**: `main`
   - ✓ **Require a pull request before merging**
   - ✓ **Require status checks to pass before merging**
     - Add status check: `test`
     - Add status check: `build`
   - ✓ **Require branches to be up to date before merging**
4. Click **Create**

## Step 4: Update README Badges

Replace `YOUR_USERNAME` in README.md with your actual GitHub username:

```bash
# Open README.md and replace
YOUR_USERNAME → your-actual-username
```

Or use this command:
```bash
# Windows PowerShell
(Get-Content README.md) -replace 'YOUR_USERNAME', 'your-actual-username' | Set-Content README.md

# Linux/Mac
sed -i 's/YOUR_USERNAME/your-actual-username/g' README.md
```

Then commit and push:
```bash
git add README.md
git commit -m "docs: update GitHub username in badges"
git push
```

## Step 5: Create Initial Release (Optional)

1. Go to your repository on GitHub
2. Click **Releases** in the right sidebar
3. Click **Create a new release**
4. Configure:
   - **Tag version**: `v1.0.0`
   - **Release title**: `AI-RPS v1.0.0 - Initial Release`
   - **Description**: Copy from below
5. Click **Publish release**

### Release Description Template

```markdown
# AI Research Panel System v1.0.0

First public release of AI-RPS - a multi-agent AI system for research document analysis.

## Features

✨ **6 Specialized AI Agents**
- Lead Analyst
- General Analyst  
- Methodology Reviewer
- Literature Reviewer
- Quick Screener
- Fact Extractor

🚀 **Core Capabilities**
- PDF document upload (up to 50MB)
- Intelligent chunking for large documents
- Parallel agent processing
- Consensus generation
- Real-time status tracking

🐳 **Docker-First**
- Complete containerization
- No local dependencies required
- Production-ready deployment

📊 **Quality**
- 152 tests with 87% coverage
- Comprehensive error handling
- Circuit breaker pattern
- Retry logic with exponential backoff

## Quick Start

```bash
# Clone repository
git clone https://github.com/YOUR_USERNAME/ai-rps.git
cd ai-rps

# Configure
cp .env.example .env
# Edit .env and add NVIDIA_API_KEY

# Start
docker-compose up -d

# Access
curl http://localhost:8080/actuator/health
```

## Documentation

- [README.md](README.md) - Quick start guide
- [COMPREHENSIVE-DOCUMENTATION.md](COMPREHENSIVE-DOCUMENTATION.md) - Full documentation
- [README-DOCKER.md](README-DOCKER.md) - Docker deployment guide
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines

## Requirements

- Docker Desktop
- NVIDIA API Key (free at https://build.nvidia.com/)

## What's Next

See [Future Enhancements](COMPREHENSIVE-DOCUMENTATION.md#future-enhancements) for planned features.
```

## Step 6: Add Topics (Tags)

1. Go to your repository on GitHub
2. Click the ⚙️ icon next to "About" in the right sidebar
3. Add topics:
   - `ai`
   - `machine-learning`
   - `research`
   - `document-analysis`
   - `multi-agent`
   - `spring-boot`
   - `docker`
   - `java`
   - `nvidia`
   - `llm`
4. Click **Save changes**

## Step 7: Enable GitHub Pages (Optional)

If you want to host documentation:

1. Go to **Settings** → **Pages**
2. Under "Source", select **Deploy from a branch**
3. Select branch: `main`
4. Select folder: `/docs` (you'll need to create this)
5. Click **Save**

## Step 8: Star Your Own Repository

Don't forget to star your own repository! ⭐

## Verification Checklist

- [ ] Repository created on GitHub
- [ ] Code pushed successfully
- [ ] README displays correctly
- [ ] GitHub Actions workflow runs
- [ ] Secrets configured
- [ ] Issues enabled
- [ ] License file visible
- [ ] Topics/tags added
- [ ] Branch protection configured (optional)
- [ ] Initial release created (optional)

## Troubleshooting

### Authentication Issues

If you get authentication errors when pushing:

**Option 1: HTTPS with Personal Access Token**
```bash
# Generate token at: https://github.com/settings/tokens
# Use token as password when prompted
git push -u origin main
```

**Option 2: SSH**
```bash
# Generate SSH key
ssh-keygen -t ed25519 -C "your_email@example.com"

# Add to GitHub: https://github.com/settings/keys
# Change remote to SSH
git remote set-url origin git@github.com:YOUR_USERNAME/ai-rps.git
git push -u origin main
```

### Large File Issues

If you get errors about large files:
```bash
# Check file sizes
git ls-files -z | xargs -0 du -h | sort -h | tail -20

# Remove large files from git history if needed
git rm --cached large-file.pdf
echo "large-file.pdf" >> .gitignore
git commit -m "Remove large file from tracking"
```

## Next Steps

1. Share your repository with the community
2. Add a demo video or screenshots
3. Write a blog post about the project
4. Submit to awesome lists
5. Engage with issues and pull requests

## Support

If you encounter issues:
- Check [GitHub Docs](https://docs.github.com)
- Open an issue in your repository
- Ask in [GitHub Community](https://github.community)

---

**Congratulations!** Your AI Research Panel System is now on GitHub! 🎉
