# GymCoach Cloud Build Documentation

This document provides comprehensive information about the GitHub Actions CI/CD pipeline for building GymCoach Android APKs.

## Overview

This workflow automatically builds debug APKs for every push to the `main` and `develop` branches, and on pull requests. It can also be manually triggered via the Actions tab.

## Prerequisites

- **GitHub Account**: Free tier is sufficient
- **Repository with GymCoach structure**: The project must have `app/build.gradle.kts` and standard Android structure
- **No local development environment required**: All builds happen in GitHub's cloud infrastructure

## Workflow Triggers

### Automatic Triggers
1. **Push to `main` branch**: Builds production-ready APKs
2. **Push to `develop` branch**: Builds development APKs
3. **Pull Requests**: Builds APKs for testing changes

### Manual Trigger
- Navigate to the **Actions** tab in your GitHub repository
- Click **"Run workflow"** on the "GymCoach CI/CD Pipeline" workflow
- Configure any required parameters

## Build Output

### What Gets Built
- **Debug APK**: `./app/build/outputs/apk/debug/app-debug.apk`
- Build artifacts are uploaded as GitHub Actions artifacts
- Test reports and build logs are available in GitHub Actions logs

### Available APKs in GitHub Actions
```bash
# Debug APK (for testing)
./app/build/outputs/apk/debug/app-debug.apk

# If signing is configured (see secrets below)
./app/build/outputs/apk/release/app-release.apk
```

## Download APK from GitHub Actions

1. Go to your repository on GitHub
2. Click the **"Actions"** tab
3. Find the "GymCoach CI/CD Pipeline" workflow
4. Click on a completed workflow run (green checkmark ✅)
5. In the "Artifacts" section:
   - Find "gymcoach-debug-apk"
   - Click **"Download"** to download the APK
6. The downloaded APK can be installed directly on Android devices for testing

## Common Failures and Troubleshooting

### 1. GitHub Actions Not Triggering

**Problem**: Workflow not running on commits

**Solution**:
- Ensure the workflow file is in the correct path: `.github/workflows/android-build.yml`
- Check that the workflow name matches the filename exactly
- Commit the workflow file to a branch that triggers the workflow

### 2. Build Fails with Gradle Error

**Problem**: Gradle build fails during compilation

**Solution**:
- Check the Actions log for specific Kotlin or Android build errors
- Ensure dependencies in `libs.versions.toml` are correctly referenced
- Verify Kotlin and Java versions match

### 3. Artifact Not Found

**Problem**: No APK artifact to download

**Solution**:
- Check if the `assembleDebug` step succeeded
- Verify the artifact upload path `./app/build/outputs/apk/debug/` matches the actual output
