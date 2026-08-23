package com.gymcoach.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymcoach.app.data.local.dao.BodyMeasurementDao
import com.gymcoach.app.data.local.dao.ExerciseAliasDao
import com.gymcoach.app.data.local.dao.EquipmentDao
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ExerciseMuscleDao
import com.gymcoach.app.data.local.dao.ExerciseSubstitutionDao
import com.gymcoach.app.data.local.dao.FavoriteExerciseDao
import com.gymcoach.app.data.local.dao.MuscleDao
import com.gymcoach.app.data.local.dao.PersonalRecordDao
import com.gymcoach.app.data.local.dao.ProgramDao
import com.gymcoach.app.data.local.dao.UserProfileDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import com.gymcoach.app.data.local.entity.EquipmentEntity
import com.gymcoach.app.data.local.entity.ExerciseAliasEntity
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.ExerciseEquipmentEntity
import com.gymcoach.app.data.local.entity.ExerciseMuscleEntity
import com.gymcoach.app.data.local.entity.ExerciseSubstitutionEntity
import com.gymcoach.app.data.local.entity.FavoriteExerciseEntity
import com.gymcoach.app.data.local.entity.MuscleEntity
import com.gymcoach.app.data.local.entity.PersonalRecordEntity
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        BodyMeasurementEntity::class,
        EquipmentEntity::class,
        ExerciseAliasEntity::class,
        ExerciseEquipmentEntity::class,
        ExerciseMuscleEntity::class,
        ExerciseSubstitutionEntity::class,
        FavoriteExerciseEntity::class,
        MuscleEntity::class,
        PersonalRecordEntity::class,
        ProgramDayEntity::class,
        ProgramEntity::class,
        ProgramExerciseEntity::class,
        UserProfileEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class GymCoachDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun exerciseAliasDao(): ExerciseAliasDao
    abstract fun exerciseEquipmentDao(): ExerciseEquipmentDao
    abstract fun exerciseMuscleDao(): ExerciseMuscleDao
    abstract fun exerciseSubstitutionDao(): ExerciseSubstitutionDao
    abstract fun favoriteExerciseDao(): FavoriteExerciseDao
    abstract fun muscleDao(): MuscleDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun programDao(): ProgramDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS personal_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        recordType TEXT NOT NULL,
                        value REAL NOT NULL,
                        reps INTEGER NOT NULL DEFAULT 0,
                        achievedAt INTEGER NOT NULL,
                        notes TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_personal_records_exerciseId ON personal_records(exerciseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_personal_records_achievedAt ON personal_records(achievedAt)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_profile (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL DEFAULT '',
                        height REAL NOT NULL DEFAULT 0,
                        weight REAL NOT NULL DEFAULT 0,
                        age INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorite_exercises (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_exercises_exerciseId ON favorite_exercises(exerciseId)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rebuild muscles table to match MuscleEntity schema exactly
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS muscles_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        scientificName TEXT NOT NULL DEFAULT '',
                        bodyRegion TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT ''
                    )
                """)
                db.execSQL("INSERT INTO muscles_new (id, name, scientificName, bodyRegion, description) SELECT id, COALESCE(name, ''), COALESCE('', ''), COALESCE(bodyRegion, ''), COALESCE(description, '') FROM muscles")
                db.execSQL("DROP TABLE muscles")
                db.execSQL("ALTER TABLE muscles_new RENAME TO muscles")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_muscles_name ON muscles(name)")

                // Rebuild equipment table to match EquipmentEntity schema exactly
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS equipment_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT ''
                    )
                """)
                db.execSQL("INSERT INTO equipment_new (id, name, category, description) SELECT id, COALESCE(name, ''), COALESCE(category, 'free_weight'), COALESCE('', '') FROM equipment")
                db.execSQL("DROP TABLE equipment")
                db.execSQL("ALTER TABLE equipment_new RENAME TO equipment")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_equipment_name ON equipment(name)")

                // Exercise-muscle junction table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS exercise_muscles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        muscleId INTEGER NOT NULL,
                        role TEXT NOT NULL,
                        activationLevel REAL NOT NULL DEFAULT 0.5,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE,
                        FOREIGN KEY(muscleId) REFERENCES muscles(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_muscles_exerciseId ON exercise_muscles(exerciseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_muscles_muscleId ON exercise_muscles(muscleId)")

                // Exercise-equipment junction table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS exercise_equipment (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        equipmentId INTEGER NOT NULL,
                        role TEXT NOT NULL DEFAULT 'required',
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE,
                        FOREIGN KEY(equipmentId) REFERENCES equipment(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_equipment_exerciseId ON exercise_equipment(exerciseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_equipment_equipmentId ON exercise_equipment(equipmentId)")

                // Substitutions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS exercise_substitutions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        originalExerciseId INTEGER NOT NULL,
                        substituteExerciseId INTEGER NOT NULL,
                        reason TEXT NOT NULL DEFAULT 'similar_movement',
                        FOREIGN KEY(originalExerciseId) REFERENCES exercises(id) ON DELETE CASCADE,
                        FOREIGN KEY(substituteExerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_substitutions_originalExerciseId ON exercise_substitutions(originalExerciseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_substitutions_substituteExerciseId ON exercise_substitutions(substituteExerciseId)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No-op: user_profile already exists since 2->3; version bump only
                // to align schema hash after entity annotations were normalized.
            }
        }

        /**
         * 6 -> 7: explicit workout lifecycle states.
         * Adds workouts.status; backfills honest states for legacy rows:
         *  - completed=1                     -> COMPLETED (analytics parity kept)
         *  - completed=0 AND has exercises   -> ACTIVE (genuinely resumable)
         *  - completed=0 AND empty           -> ABANDONED (zombie artifact of the old
         *                                       eager-create bug; never auto-resumed)
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workouts ADD COLUMN status TEXT NOT NULL DEFAULT 'NOT_STARTED'")
                db.execSQL("UPDATE workouts SET status = 'COMPLETED' WHERE completed = 1")
                db.execSQL(
                    "UPDATE workouts SET status = 'ACTIVE' WHERE completed = 0 " +
                        "AND id IN (SELECT workoutId FROM workout_exercises)"
                )
                db.execSQL(
                    "UPDATE workouts SET status = 'ABANDONED' WHERE completed = 0 AND status = 'NOT_STARTED'"
                )
            }
        }

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
        )
    }
}
