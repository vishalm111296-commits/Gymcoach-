package com.gymcoach.app.core.data.seed

import androidx.room.withTransaction
import com.gymcoach.app.core.data.seed.SeedDataArmsCore.ARMS
import com.gymcoach.app.core.data.seed.SeedDataArmsCore.CORE
import com.gymcoach.app.core.data.seed.SeedDataBack.BACK
import com.gymcoach.app.core.data.seed.SeedDataChestShoulders.CHEST
import com.gymcoach.app.core.data.seed.SeedDataChestShoulders.SHOULDERS
import com.gymcoach.app.core.data.seed.SeedDataLegs.LEGS
import com.gymcoach.app.data.local.dao.EquipmentDao
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ExerciseMuscleDao
import com.gymcoach.app.data.local.dao.MuscleDao
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.ExerciseMuscleEntity
import com.gymcoach.app.data.local.entity.MuscleEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Idempotent, transactional seeding of the exercise library.
 *
 * Idempotency guard: skips when any exercises already exist. This replaces the
 * audit-recommended seed_meta table — since the app is unreleased and the old
 * hardcoded onCreate seeds are removed, a count-guard inside a single
 * transaction provides equivalent safety with zero migration surface.
 */
@Singleton
class ExerciseSeeder @Inject constructor(
    private val database: GymCoachDatabase,
    private val muscleDao: MuscleDao,
    private val equipmentDao: EquipmentDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseMuscleDao: ExerciseMuscleDao
) {

    val ALL_EXERCISES: List<SeedExercise> =
        CHEST + SHOULDERS + BACK + LEGS + ARMS + CORE

    suspend fun seedIfEmpty(): SeedResult {
        val existing = muscleDao.count() + equipmentDao.count() + exerciseDao.count()
        if (existing == 0) {
            seed()
            return SeedResult.SEEDED
        }
        return SeedResult.ALREADY_PRESENT
    }

    private suspend fun seed() {
        database.withTransaction {
            // 1. Reference data: muscles then equipment; resolve name -> id.
            val muscleIdByName = HashMap<String, Long>(SeedReference.MUSCLES.size)
            for (m in SeedReference.MUSCLES) {
                val id = muscleDao.insert(
                    MuscleEntity(name = m.name, displayName = m.displayName, bodyRegion = m.bodyRegion)
                )
                muscleIdByName[m.name] = id
            }

            val equipmentIdByName = HashMap<String, Long>(SeedReference.EQUIPMENT.size)
            for (e in SeedReference.EQUIPMENT) {
                val id = equipmentDao.insert(
                    com.gymcoach.app.data.local.entity.EquipmentEntity(
                        name = e.name, displayName = e.displayName, category = e.category
                    )
                )
                equipmentIdByName[e.name] = id
            }

            // 2. Exercises + join rows, resolved by canonical muscle names.
            for (seed in ALL_EXERCISES) {
                val exerciseId = exerciseDao.insert(
                    ExerciseEntity(
                        name = seed.name,
                        description = seed.description,
                        muscleGroup = seed.muscleGroup,
                        equipment = seed.equipment,
                        difficulty = seed.difficulty,
                        secondaryMuscles = seed.secondaryMuscles,
                        instructions = seed.instructions,
                        tips = seed.tips,
                        recommendedRepRange = seed.recommendedRepRange,
                        recommendedRestTime = seed.recommendedRestTime,
                        category = seed.category,
                        vtaperLat = seed.vtaperLat,
                        vtaperLateralDelt = seed.vtaperLateralDelt,
                        vtaperUpperChest = seed.vtaperUpperChest,
                        vtaperRearDelt = seed.vtaperRearDelt,
                        movementPattern = seed.movementPattern,
                        setupInstructions = seed.setupInstructions,
                        executionInstructions = seed.executionInstructions
                    )
                )

                val relations = seed.muscles.mapNotNull { ref ->
                    muscleIdByName[ref.name]?.let { muscleId ->
                        ExerciseMuscleEntity(
                            exerciseId = exerciseId,
                            muscleId = muscleId,
                            role = ref.role
                        )
                    }
                }
                if (relations.isNotEmpty()) {
                    exerciseMuscleDao.insertAll(relations)
                }
            }
        }
    }
}

enum class SeedResult { SEEDED, ALREADY_PRESENT }
