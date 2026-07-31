# TESTING_GUIDE.md

## Overview

This document outlines the testing strategy for the GymCoach project.

## Test Structure

- **Unit Tests:** Located in `app/src/test/`
  - `ExerciseRepositoryTest.kt` tests the ExerciseRepository mapping.

- **UI Tests:** Located in `app/src/androidTest/`
  - No UI tests are currently implemented.

## Running Tests

### Unit Tests
```bash
./gradlew test
```

### UI Tests
```bash
./gradlew connectedAndroidTest
```
UI tests require a connected device or emulator.

## Manual Testing Checklist
- [ ] Launch app on a physical device.
- [ ] Verify the Exercise Library loads.
- [ ] Verify Exercise Detail screen displays correctly.
- [ ] Verify Workout Session can be started and exercises/logged.
- [ ] Verify Rest Timer starts and counts down correctly.
- [ ] Verify Camera Preview launches and displays correctly.
- [ ] Verify Form Analyzer displays feedback.
- [ ] Verify Progress Dashboard displays data.
