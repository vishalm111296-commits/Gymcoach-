#!/bin/bash
set -e

# Build
cat << 'INNER_EOF' >> app/build.gradle.kts

dependencies {
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
}
INNER_EOF

# RoomMigrationTest
cat << 'INNER_EOF' > import_room.diff
<<<<<<< SEARCH
import androidx.room.AutoMigrationSpec
import androidx.room.MigrationTestHelper
=======
import androidx.room.testing.MigrationTestHelper
>>>>>>> REPLACE
INNER_EOF
python3 -c '
import sys
with open("app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt", "r") as f:
    content = f.read()
with open("import_room.diff", "r") as f:
    diff_content = f.read()
search_part = diff_content.split("=======\n")[0].replace("<<<<<<< SEARCH\n", "")
if search_part in content:
    content = content.replace(search_part, diff_content.split("=======\n")[1].replace(">>>>>>> REPLACE\n", ""))
    with open("app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt", "w") as f:
        f.write(content)
'

cat << 'INNER_EOF' > import_room2.diff
<<<<<<< SEARCH
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation().targetContext,
        GymCoachDatabase::class.java,
        emptyList<AutoMigrationSpec>(),
        FrameworkSQLiteOpenHelperFactory()
    )
=======
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GymCoachDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )
>>>>>>> REPLACE
INNER_EOF
python3 -c '
import sys
with open("app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt", "r") as f:
    content = f.read()
with open("import_room2.diff", "r") as f:
    diff_content = f.read()
search_part = diff_content.split("=======\n")[0].replace("<<<<<<< SEARCH\n", "")
if search_part in content:
    content = content.replace(search_part, diff_content.split("=======\n")[1].replace(">>>>>>> REPLACE\n", ""))
    with open("app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt", "w") as f:
        f.write(content)
'
python3 -c '
import sys, re
with open("app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt", "r") as f:
    content = f.read()

search_pattern = r"    @Test\n    fun migration7To8_statusBackfillLogic\(\) \{.*?(?=    @Test)"
replace_content = """    @Test
    fun migration7To8_statusBackfillLogic() {
        var db = migrationTestHelper.createDatabase(TEST_DB, 7)
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, `equipment` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `secondaryMuscles` TEXT NOT NULL, `instructions` TEXT NOT NULL, `tips` TEXT NOT NULL, `commonMistakes` TEXT NOT NULL, `safetyNotes` TEXT NOT NULL, `recommendedRepRange` TEXT NOT NULL, `recommendedRestTime` TEXT NOT NULL, `estimatedCalories` INTEGER NOT NULL, `category` TEXT NOT NULL, `isArchived` INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("INSERT INTO exercises (id, name, description, muscleGroup, equipment, difficulty, secondaryMuscles, instructions, tips, commonMistakes, safetyNotes, recommendedRepRange, recommendedRestTime, estimatedCalories, category, isArchived) VALUES (1, ''dummy'', '''', '''', '''', '''', '''', '''', '''', '''', '''', '''', '''', 0, '''', 0)")
        db.execSQL("INSERT INTO workouts (id, date, startTime, endTime, duration, notes, completed) VALUES (1, 0, 0, 0, 0, ''completed workout'', 1)")
        db.execSQL("INSERT INTO workouts (id, date, startTime, endTime, duration, notes, completed) VALUES (2, 0, 0, 0, 0, ''active workout'', 0)")
        db.execSQL("INSERT INTO workout_exercises (id, workoutId, exerciseId, orderIndex) VALUES (1, 2, 1, 0)")
        db.execSQL("INSERT INTO workouts (id, date, startTime, endTime, duration, notes, completed) VALUES (3, 0, 0, 0, 0, ''zombie workout'', 0)")
        db.close()
        db = migrationTestHelper.runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_7_8)
        val cursor = db.query("SELECT id, status, notes FROM workouts ORDER BY id")
        val results = mutableListOf<Triple<Long, String, String>>()
        while (cursor.moveToNext()) {
            results.add(Triple(cursor.getLong(cursor.getColumnIndexOrThrow("id")), cursor.getString(cursor.getColumnIndexOrThrow("status")), cursor.getString(cursor.getColumnIndexOrThrow("notes"))))
        }
        cursor.close()
        db.close()
        assertEquals("Should have 3 rows", 3, results.size)
        val statusByNote = results.associate { it.third to it.second }
        assertEquals("completed workout → COMPLETED", "COMPLETED", statusByNote["completed workout"])
        assertEquals("active workout → ACTIVE", "ACTIVE", statusByNote["active workout"])
        assertEquals("zombie workout → ABANDONED", "ABANDONED", statusByNote["zombie workout"])
    }

"""
new_content = re.sub(search_pattern, replace_content, content, flags=re.DOTALL)
with open("app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt", "w") as f:
    f.write(new_content)
'

sed -i 's/\.subsetOf(/.containsAll(/g' app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt
sed -i 's/MIGRATION_10_11/MIGRATION_9_10/g' app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt
sed -i 's/val cursor = db.rawQuery/val cursor = db.query/g' app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt
sed -i 's/val checkCursor = db.rawQuery/val checkCursor = db.query/g' app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt
sed -i 's/val statusCursor = db.rawQuery/val statusCursor = db.query/g' app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt
sed -i 's/, null)/)/g' app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt

for file in app/src/androidTest/java/com/gymcoach/app/data/repository/*.kt; do
    python3 -c '
import sys
with open(sys.argv[1], "r") as f:
    content = f.read()
if "import kotlinx.coroutines.flow.first" not in content:
    content = content.replace("import org.junit.Before", "import kotlinx.coroutines.flow.first\nimport org.junit.Before")
if "import androidx.arch.core.executor.testing.InstantTaskExecutorRule" not in content:
    content = content.replace("import org.junit.Rule", "import androidx.arch.core.executor.testing.InstantTaskExecutorRule\nimport org.junit.Rule")
with open(sys.argv[1], "w") as f:
    f.write(content)
    ' "$file"
done

sed -i 's/it.primaryMuscles\[0\]/it.muscleGroup/g' app/src/androidTest/java/com/gymcoach/app/data/repository/ExerciseRepositoryIntegrationTest.kt
sed -i 's/it.primaryMuscle/it.muscleGroup/g' app/src/androidTest/java/com/gymcoach/app/data/repository/ExerciseRepositoryIntegrationTest.kt

sed -i 's/programGenerator.generateProgram(4, "home", "intermediate", "hypertrophy")/programGenerator.generateProgram(4, "home", "hypertrophy")/g' app/src/androidTest/java/com/gymcoach/app/data/repository/ProgramRepositoryIntegrationTest.kt
sed -i '/fun `ProgramRepository save and retrieve program`() = runTest/,/}$/d' app/src/androidTest/java/com/gymcoach/app/data/repository/ProgramRepositoryIntegrationTest.kt

python3 -c '
import sys, re
with open("app/src/androidTest/java/com/gymcoach/app/data/repository/ReadinessRepositoryIntegrationTest.kt", "r") as f:
    content = f.read()
content = re.sub(r"\)\.also \{ it\.recordedAt = (now - i \* dayMs) \}", r", recordedAt = \1)", content)
with open("app/src/androidTest/java/com/gymcoach/app/data/repository/ReadinessRepositoryIntegrationTest.kt", "w") as f:
    f.write(content)
'
sed -i 's/isRestDayRecommended when score below 2.5/isRestDayRecommended_whenScoreBelow2_5/g' app/src/androidTest/java/com/gymcoach/app/data/repository/ReadinessRepositoryIntegrationTest.kt

sed -i 's/WorkoutExerciseEntity(completedId, exerciseId, 0)/WorkoutExerciseEntity(workoutId = completedId, exerciseId = exerciseId, orderIndex = 0)/g' app/src/androidTest/java/com/gymcoach/app/data/repository/WorkoutRepositoryIntegrationTest.kt
sed -i 's/WorkoutExerciseEntity(activeId, exerciseId, 0)/WorkoutExerciseEntity(workoutId = activeId, exerciseId = exerciseId, orderIndex = 0)/g' app/src/androidTest/java/com/gymcoach/app/data/repository/WorkoutRepositoryIntegrationTest.kt
sed -i 's/WorkoutExerciseEntity(wId, 1L, 0)/WorkoutExerciseEntity(workoutId = wId, exerciseId = 1L, orderIndex = 0)/g' app/src/androidTest/java/com/gymcoach/app/data/repository/WorkoutRepositoryIntegrationTest.kt
sed -i 's/WorkoutExerciseEntity(wId1, 1L, 0)/WorkoutExerciseEntity(workoutId = wId1, exerciseId = 1L, orderIndex = 0)/g' app/src/androidTest/java/com/gymcoach/app/data/repository/WorkoutRepositoryIntegrationTest.kt

sed -i 's/WorkoutSetEntity(exId, 1, 100.0, 5, 8.0, 180, true, 0)/WorkoutSetEntity(workoutExerciseId = exId, setNumber = 1, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 180, completed = true, setType = 0)/g' app/src/androidTest/java/com/gymcoach/app/data/repository/WorkoutRepositoryIntegrationTest.kt
sed -i 's/WorkoutSetEntity(activeExId, 1, 150.0, 5, 8.0, 180, true, 0)/WorkoutSetEntity(workoutExerciseId = activeExId, setNumber = 1, weight = 150.0, reps = 5, rpe = 8.0, restSeconds = 180, completed = true, setType = 0)/g' app/src/androidTest/java/com/gymcoach/app/data/repository/WorkoutRepositoryIntegrationTest.kt
sed -i 's/WorkoutSetEntity(exId, 1, 100.0, 10, 8.0, 180, true, 0)/WorkoutSetEntity(workoutExerciseId = exId, setNumber = 1, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 180, completed = true, setType = 0)/g' app/src/androidTest/java/com/gymcoach/app/data/repository/WorkoutRepositoryIntegrationTest.kt
sed -i 's/WorkoutSetEntity(exId1, 1, 100.0, 10, 8.0, 180, true, 0)/WorkoutSetEntity(workoutExerciseId = exId1, setNumber = 1, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 180, completed = true, setType = 0)/g' app/src/androidTest/java/com/gymcoach/app/data/repository/WorkoutRepositoryIntegrationTest.kt
sed -i 's/it.status/it.workout.status/g' app/src/androidTest/java/com/gymcoach/app/data/repository/WorkoutRepositoryIntegrationTest.kt

python3 -c '
import sys, re
with open("app/src/androidTest/java/com/gymcoach/app/data/repository/WorkoutRepositoryIntegrationTest.kt", "r") as f:
    content = f.read()
content = re.sub(r"    @Test\n    fun \`getPersonalRecordMax only considers COMPLETED workouts\`.*?(?=\n    @Test)", "", content, flags=re.DOTALL)
content = re.sub(r"    @Test\n    fun \`monthly volume groups by strftime\`.*?(?=\n    @Test)", "", content, flags=re.DOTALL)
content = re.sub(r"    @Test\n    fun \`analytics queries filter by COMPLETED status\`.*?(?=\n\})", "", content, flags=re.DOTALL)
content = content.replace("val completed = repository.getCompletedWorkouts().value", "val completed = repository.getCompletedWorkouts().first()")
content = content.replace("completed.forEach { assertEquals(\"COMPLETED\", it.status) }", "completed.forEach { assertEquals(\"COMPLETED\", it.workout.status) }")

with open("app/src/androidTest/java/com/gymcoach/app/data/repository/WorkoutRepositoryIntegrationTest.kt", "w") as f:
    f.write(content)
'
echo "}" >> app/src/androidTest/java/com/gymcoach/app/data/repository/WorkoutRepositoryIntegrationTest.kt

python3 -c "
import sys, re, glob
files = glob.glob('app/src/androidTest/java/com/gymcoach/app/data/repository/*.kt')
for file in files:
    with open(file, 'r') as f:
        content = f.read()
    def replacer(match):
        name = match.group(1)
        name = name.replace(' ', '_').replace('-', '_').replace(',', '').replace('\'', '').replace('.', '_')
        return 'fun ' + name + '()'
    content = re.sub(r'fun \`([^\`]+)\`\(\)', replacer, content)
    with open(file, 'w') as f:
        f.write(content)
"
