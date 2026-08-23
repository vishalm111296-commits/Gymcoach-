# Stabilization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve three compile blockers to make Android CI pass on branch phase3/p0-stabilization.

**Architecture:** Correct schema queries in ExerciseSubstitutionDao, rebase VolumeCalculator on LoggedSet DTO and add repository assembler, deduplicate ProgressViewModel.

**Tech Stack:** Kotlin, Room, Coroutines, Compose, Hilt

**Spec:** N/A (Stabilization directive)

## Global Constraints

- No structural migration changes (version 7 database remains).
- Do not redesign UI or add unrelated test files unless compilation demands.
- Always check the latest file contents on the remote branch phase3/p0-stabilization before modifying them.

---

### Task 1: Fix ExerciseSubstitutionDao queries

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/data/local/dao/ExerciseSubstitutionDao.kt`

- [ ] **Step 1: Fetch ExerciseSubstitutionDao.kt**
- [ ] **Step 2: Update queries to match database schema columns**
```kotlin
package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymcoach.app.data.local.entity.ExerciseSubstitutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSubstitutionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(substitution: ExerciseSubstitutionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(substitutions: List<ExerciseSubstitutionEntity>)

    @Query("SELECT * FROM exercise_substitutions WHERE original_exercise_id = :exerciseId")
    fun getByExerciseId(exerciseId: Long): Flow<List<ExerciseSubstitutionEntity>>

    @Query("SELECT * FROM exercise_substitutions WHERE substitute_exercise_id = :substituteId")
    fun getBySubstituteId(substituteId: Long): Flow<List<ExerciseSubstitutionEntity>>

    @Query("DELETE FROM exercise_substitutions WHERE original_exercise_id = :exerciseId")
    suspend fun deleteByExerciseId(exerciseId: Long): Int

    @Query("DELETE FROM exercise_substitutions WHERE original_exercise_id = :exerciseId AND substitute_exercise_id = :substituteId")
    suspend fun deleteByExerciseAndSubstitute(exerciseId: Long, substituteId: Long): Int
}
```
- [ ] **Step 3: Push changes to remote branch**
Commit: `fix(data): correct ExerciseSubstitutionDao queries to actual substitution table schema`

---

### Task 2: Rebase VolumeCalculator on LoggedSet DTO

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/core/program/VolumeCalculator.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/data/local/dao/WorkoutDao.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/domain/repository/WorkoutRepository.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/data/repository/WorkoutRepositoryImpl.kt`

- [ ] **Step 1: Fetch VolumeCalculator.kt**
- [ ] **Step 2: Define LoggedSet DTO and refactor VolumeCalculator**
- [ ] **Step 3: Fetch WorkoutDao.kt, add LoggedSetRaw and query**
- [ ] **Step 4: Fetch WorkoutRepository.kt, add getLoggedSets signature**
- [ ] **Step 5: Fetch WorkoutRepositoryImpl.kt, implement getLoggedSets mapping**
- [ ] **Step 6: Push changes to remote branch**
Commit: `fix(domain): rebase VolumeCalculator on LoggedSet DTO + correct set counting`

---

### Task 3: Deduplicate ProgressViewModel definitions

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/progress/ProgressDashboardScreen.kt`

- [ ] **Step 1: Fetch ProgressDashboardScreen.kt**
- [ ] **Step 2: Delete duplicate ProgressUiState and ProgressViewModel definitions**
- [ ] **Step 3: Push changes to remote branch**
Commit: `fix(ui): deduplicate ProgressViewModel definitions`
