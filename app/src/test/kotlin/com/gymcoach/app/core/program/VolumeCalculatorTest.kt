package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class VolumeCalculatorTest {

    private lateinit var volumeCalculator: VolumeCalculator

    @Before
    fun setUp() {
        volumeCalculator = VolumeCalculator()
    }

    @Test
    fun `calculateWeeklyVolume returns zero volume for empty set list`() {
        val balance = volumeCalculator.calculateWeeklyVolume(
            completedSets = emptyList(),
            exerciseMuscleMap = emptyMap()
        )

        assertNotNull(balance)
        val allVolumes = balance.asList()
        assertEquals(12, allVolumes.size)
        allVolumes.forEach { volume ->
            assertEquals(0, volume.weeklySets)
            assertEquals(0, volume.directSets)
            assertEquals(0, volume.indirectSets)
            assertEquals(VolumeCalculator.VolumeStatus.INSUFFICIENT, volume.status)
        }
    }

    @Test
    fun `calculateWeeklyVolume ignores uncompleted sets`() {
        val uncompletedSet = WorkoutSetEntity(
            id = 1,
            workoutExerciseId = 1,
            setNumber = 1,
            weight = 100.0,
            reps = 10,
            rpe = 8.0,
            restSeconds = 90,
            completed = false,
            setType = 0
        )
        val setWithContext = VolumeCalculator.SetWithContext(
            set = uncompletedSet,
            exerciseId = 101L,
            workoutDate = System.currentTimeMillis()
        )
        val muscleMap = mapOf(
            101L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)
            )
        )

        val balance = volumeCalculator.calculateWeeklyVolume(
            completedSets = listOf(setWithContext),
            exerciseMuscleMap = muscleMap
        )

        assertEquals(0, balance.latVolume.weeklySets)
        assertEquals(0, balance.latVolume.directSets)
        assertEquals(0, balance.latVolume.indirectSets)
        assertEquals(VolumeCalculator.VolumeStatus.INSUFFICIENT, balance.latVolume.status)
    }

    @Test
    fun `calculateWeeklyVolume ignores non-work sets such as warmups`() {
        val warmupSet = WorkoutSetEntity(
            id = 1,
            workoutExerciseId = 1,
            setNumber = 1,
            weight = 50.0,
            reps = 10,
            rpe = 5.0,
            restSeconds = 60,
            completed = true,
            setType = 1 // Warmup set
        )
        val setWithContext = VolumeCalculator.SetWithContext(
            set = warmupSet,
            exerciseId = 101L,
            workoutDate = System.currentTimeMillis()
        )
        val muscleMap = mapOf(
            101L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)
            )
        )

        val balance = volumeCalculator.calculateWeeklyVolume(
            completedSets = listOf(setWithContext),
            exerciseMuscleMap = muscleMap
        )

        assertEquals(0, balance.latVolume.weeklySets)
        assertEquals(0, balance.latVolume.directSets)
        assertEquals(VolumeCalculator.VolumeStatus.INSUFFICIENT, balance.latVolume.status)
    }

    @Test
    fun `calculateWeeklyVolume correctly counts direct sets for primary muscle assignments`() {
        val workSet = WorkoutSetEntity(
            id = 1,
            workoutExerciseId = 1,
            setNumber = 1,
            weight = 100.0,
            reps = 10,
            rpe = 8.0,
            restSeconds = 90,
            completed = true,
            setType = 0
        )
        val setWithContext = VolumeCalculator.SetWithContext(
            set = workSet,
            exerciseId = 101L,
            workoutDate = System.currentTimeMillis()
        )
        val muscleMap = mapOf(
            101L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)
            )
        )

        val balance = volumeCalculator.calculateWeeklyVolume(
            completedSets = listOf(setWithContext),
            exerciseMuscleMap = muscleMap
        )

        assertEquals(1, balance.latVolume.directSets)
        assertEquals(0, balance.latVolume.indirectSets)
        assertEquals(1, balance.latVolume.weeklySets)
        assertEquals("Lats", balance.latVolume.muscleName)
    }

    @Test
    fun `calculateWeeklyVolume correctly counts indirect sets for secondary and stabilizer assignments`() {
        val workSet = WorkoutSetEntity(
            id = 1,
            workoutExerciseId = 1,
            setNumber = 1,
            weight = 80.0,
            reps = 10,
            rpe = 8.0,
            restSeconds = 90,
            completed = true,
            setType = 0
        )
        val setWithContext = VolumeCalculator.SetWithContext(
            set = workSet,
            exerciseId = 102L,
            workoutDate = System.currentTimeMillis()
        )
        val muscleMap = mapOf(
            102L to listOf(
                VolumeCalculator.MuscleAssignment("Biceps", VolumeCalculator.MuscleRole.SECONDARY),
                VolumeCalculator.MuscleAssignment("Core", VolumeCalculator.MuscleRole.STABILIZER)
            )
        )

        val balance = volumeCalculator.calculateWeeklyVolume(
            completedSets = listOf(setWithContext),
            exerciseMuscleMap = muscleMap
        )

        assertEquals(0, balance.bicepsVolume.directSets)
        assertEquals(1, balance.bicepsVolume.indirectSets)
        assertEquals(1, balance.bicepsVolume.weeklySets)

        assertEquals(0, balance.coreVolume.directSets)
        assertEquals(1, balance.coreVolume.indirectSets)
        assertEquals(1, balance.coreVolume.weeklySets)
    }

    @Test
    fun `calculateWeeklyVolume classifies volume status thresholds correctly`() {
        fun makeSets(count: Int, exerciseId: Long, muscle: String): Pair<List<VolumeCalculator.SetWithContext>, Map<Long, List<VolumeCalculator.MuscleAssignment>>> {
            val sets = (1..count).map { i ->
                VolumeCalculator.SetWithContext(
                    set = WorkoutSetEntity(
                        id = i.toLong(),
                        workoutExerciseId = i.toLong(),
                        setNumber = 1,
                        weight = 100.0,
                        reps = 10,
                        rpe = 8.0,
                        restSeconds = 90,
                        completed = true,
                        setType = 0
                    ),
                    exerciseId = exerciseId + i,
                    workoutDate = System.currentTimeMillis()
                )
            }
            val map = (1..count).associate { i ->
                (exerciseId + i) to listOf(VolumeCalculator.MuscleAssignment(muscle, VolumeCalculator.MuscleRole.PRIMARY))
            }
            return Pair(sets, map)
        }

        // INSUFFICIENT (< 10 sets)
        val (sets9, map9) = makeSets(9, 1000L, "Quadriceps")
        val balance9 = volumeCalculator.calculateWeeklyVolume(sets9, map9)
        assertEquals(VolumeCalculator.VolumeStatus.INSUFFICIENT, balance9.quadricepsVolume.status)

        // MODERATE (10-13 sets)
        val (sets12, map12) = makeSets(12, 2000L, "Quadriceps")
        val balance12 = volumeCalculator.calculateWeeklyVolume(sets12, map12)
        assertEquals(VolumeCalculator.VolumeStatus.MODERATE, balance12.quadricepsVolume.status)

        // OPTIMAL (14-17 sets)
        val (sets15, map15) = makeSets(15, 3000L, "Quadriceps")
        val balance15 = volumeCalculator.calculateWeeklyVolume(sets15, map15)
        assertEquals(VolumeCalculator.VolumeStatus.OPTIMAL, balance15.quadricepsVolume.status)

        // HIGH (18-21 sets)
        val (sets20, map20) = makeSets(20, 4000L, "Quadriceps")
        val balance20 = volumeCalculator.calculateWeeklyVolume(sets20, map20)
        assertEquals(VolumeCalculator.VolumeStatus.HIGH, balance20.quadricepsVolume.status)

        // EXCESSIVE (22+ sets)
        val (sets25, map25) = makeSets(25, 5000L, "Quadriceps")
        val balance25 = volumeCalculator.calculateWeeklyVolume(sets25, map25)
        assertEquals(VolumeCalculator.VolumeStatus.EXCESSIVE, balance25.quadricepsVolume.status)
    }

    @Test
    fun `calculateVtaperBalance evaluates scores and overall balance categories correctly`() {
        val insufficientVolume = VolumeCalculator.MuscleVolume(
            muscleName = "Test",
            weeklySets = 5,
            directSets = 5,
            indirectSets = 0,
            status = VolumeCalculator.VolumeStatus.INSUFFICIENT // ordinal 0
        )
        val optimalVolume = VolumeCalculator.MuscleVolume(
            muscleName = "Test",
            weeklySets = 15,
            directSets = 15,
            indirectSets = 0,
            status = VolumeCalculator.VolumeStatus.OPTIMAL // ordinal 3
        )
        val moderateVolume = VolumeCalculator.MuscleVolume(
            muscleName = "Test",
            weeklySets = 12,
            directSets = 12,
            indirectSets = 0,
            status = VolumeCalculator.VolumeStatus.MODERATE // ordinal 1
        )
        val highVolume = VolumeCalculator.MuscleVolume(
            muscleName = "Test",
            weeklySets = 20,
            directSets = 20,
            indirectSets = 0,
            status = VolumeCalculator.VolumeStatus.HIGH // ordinal 2
        )

        // Low V-taper balance
        val lowBalance = VolumeCalculator.TrainingBalance(
            latVolume = insufficientVolume,
            lateralDeltVolume = insufficientVolume,
            rearDeltVolume = insufficientVolume,
            upperChestVolume = insufficientVolume,
            upperBackVolume = insufficientVolume,
            bicepsVolume = insufficientVolume,
            tricepsVolume = insufficientVolume,
            quadricepsVolume = insufficientVolume,
            hamstringsVolume = insufficientVolume,
            glutesVolume = insufficientVolume,
            calvesVolume = insufficientVolume,
            coreVolume = insufficientVolume
        )
        val lowVtaper = volumeCalculator.calculateVtaperBalance(lowBalance)
        assertEquals(0.0, lowVtaper.primaryScore, 0.01)
        assertEquals(0.0, lowVtaper.secondaryScore, 0.01)
        assertEquals("Low V-taper volume", lowVtaper.overallBalance)

        // Moderate V-taper balance (primaryScore >= 2.0, secondaryScore < 2.0)
        val moderateBalance = VolumeCalculator.TrainingBalance(
            latVolume = optimalVolume, // ordinal 3
            lateralDeltVolume = highVolume, // ordinal 2 -> primary avg = (3+2)/2 = 2.5
            rearDeltVolume = moderateVolume, // ordinal 1
            upperChestVolume = moderateVolume, // ordinal 1
            upperBackVolume = moderateVolume, // ordinal 1 -> secondary avg = (1+1+1)/3 = 1.0
            bicepsVolume = insufficientVolume,
            tricepsVolume = insufficientVolume,
            quadricepsVolume = insufficientVolume,
            hamstringsVolume = insufficientVolume,
            glutesVolume = insufficientVolume,
            calvesVolume = insufficientVolume,
            coreVolume = insufficientVolume
        )
        val moderateVtaper = volumeCalculator.calculateVtaperBalance(moderateBalance)
        assertEquals(2.5, moderateVtaper.primaryScore, 0.01)
        assertEquals(1.0, moderateVtaper.secondaryScore, 0.01)
        assertEquals("Moderate V-taper focus", moderateVtaper.overallBalance)

        // Good V-taper balance (primaryScore >= 3.0, secondaryScore >= 2.0)
        val goodBalance = VolumeCalculator.TrainingBalance(
            latVolume = optimalVolume, // ordinal 3
            lateralDeltVolume = optimalVolume, // ordinal 3 -> primary avg = 3.0
            rearDeltVolume = highVolume, // ordinal 2
            upperChestVolume = highVolume, // ordinal 2
            upperBackVolume = highVolume, // ordinal 2 -> secondary avg = 2.0
            bicepsVolume = insufficientVolume,
            tricepsVolume = insufficientVolume,
            quadricepsVolume = insufficientVolume,
            hamstringsVolume = insufficientVolume,
            glutesVolume = insufficientVolume,
            calvesVolume = insufficientVolume,
            coreVolume = insufficientVolume
        )
        val goodVtaper = volumeCalculator.calculateVtaperBalance(goodBalance)
        assertEquals(3.0, goodVtaper.primaryScore, 0.01)
        assertEquals(2.0, goodVtaper.secondaryScore, 0.01)
        assertEquals("Good V-taper volume distribution", goodVtaper.overallBalance)
    }

    @Test
    fun `TrainingBalance asList returns all 12 muscle volumes in correct order`() {
        val emptyVol = VolumeCalculator.MuscleVolume("Empty", 0, 0, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT)
        val balance = VolumeCalculator.TrainingBalance(
            latVolume = emptyVol.copy(muscleName = "Lats"),
            lateralDeltVolume = emptyVol.copy(muscleName = "Lateral Deltoid"),
            rearDeltVolume = emptyVol.copy(muscleName = "Rear Deltoid"),
            upperChestVolume = emptyVol.copy(muscleName = "Upper Chest"),
            upperBackVolume = emptyVol.copy(muscleName = "Upper Back"),
            bicepsVolume = emptyVol.copy(muscleName = "Biceps"),
            tricepsVolume = emptyVol.copy(muscleName = "Triceps"),
            quadricepsVolume = emptyVol.copy(muscleName = "Quadriceps"),
            hamstringsVolume = emptyVol.copy(muscleName = "Hamstrings"),
            glutesVolume = emptyVol.copy(muscleName = "Glutes"),
            calvesVolume = emptyVol.copy(muscleName = "Calves"),
            coreVolume = emptyVol.copy(muscleName = "Core")
        )

        val list = balance.asList()
        assertEquals(12, list.size)
        assertEquals("Lats", list[0].muscleName)
        assertEquals("Lateral Deltoid", list[1].muscleName)
        assertEquals("Rear Deltoid", list[2].muscleName)
        assertEquals("Upper Chest", list[3].muscleName)
        assertEquals("Upper Back", list[4].muscleName)
        assertEquals("Biceps", list[5].muscleName)
        assertEquals("Triceps", list[6].muscleName)
        assertEquals("Quadriceps", list[7].muscleName)
        assertEquals("Hamstrings", list[8].muscleName)
        assertEquals("Glutes", list[9].muscleName)
        assertEquals("Calves", list[10].muscleName)
        assertEquals("Core", list[11].muscleName)
    }

    @Test
    fun `calculateWeeklyVolume handles unknown exercise IDs gracefully`() {
        val set = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(
                id = 1,
                workoutExerciseId = 1,
                setNumber = 1,
                weight = 100.0,
                reps = 10,
                rpe = 8.0,
                restSeconds = 90,
                completed = true,
                setType = 0
            ),
            exerciseId = 99999L, // Not in exerciseMuscleMap
            workoutDate = System.currentTimeMillis()
        )

        val balance = volumeCalculator.calculateWeeklyVolume(
            completedSets = listOf(set),
            exerciseMuscleMap = emptyMap()
        )

        balance.asList().forEach { volume ->
            assertEquals(0, volume.weeklySets)
            assertEquals(0, volume.directSets)
            assertEquals(0, volume.indirectSets)
            assertEquals(VolumeCalculator.VolumeStatus.INSUFFICIENT, volume.status)
        }
    }
}
