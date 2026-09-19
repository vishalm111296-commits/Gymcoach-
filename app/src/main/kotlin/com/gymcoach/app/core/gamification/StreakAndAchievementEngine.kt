package com.gymcoach.app.core.gamification

import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.WorkoutWithStats
import java.time.Instant
import java.time.ZoneId
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import javax.inject.Inject
import javax.inject.Singleton

enum class BadgeTier { BRONZE, SILVER, GOLD, OBSIDIAN }
enum class BadgeCategory { ALL, MILESTONES, VOLUME, CONSISTENCY, SPECIAL }

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val tier: BadgeTier,
    val category: BadgeCategory,
    val currentProgress: Int,
    val maxProgress: Int,
    val isUnlocked: Boolean,
    val unlockedDateEpochMilli: Long? = null
)

data class StreakReport(
    val currentWeeklyStreak: Int,
    val longestWeeklyStreak: Int,
    val currentWeekWorkoutsCompleted: Int,
    val weeklyTarget: Int = 4,
    val totalWorkouts: Int,
    val totalVolumeKg: Double,
    val totalSets: Int,
    val athleteLevel: Int,
    val currentXp: Int,
    val xpForNextLevel: Int = 1000,
    val badges: List<AchievementBadge>,
    val heatMapData: Map<String, Int>
)

@Singleton
class StreakAndAchievementEngine @Inject constructor(
    private val clock: Clock
) {

    fun calculateReport(
        workouts: List<WorkoutWithStats>,
        setsWithContext: List<VolumeCalculator.SetWithContext>,
        weeklyTarget: Int = 4,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StreakReport {
        val sortedWorkouts = workouts.filter { it.completed }.sortedBy { it.date }
        val totalWorkouts = sortedWorkouts.size

        // Calculate Streak
        val weekBuckets = sortedWorkouts.groupBy {
            val date = it.date.atZone(zoneId).toLocalDate()
            val year = date.get(IsoFields.WEEK_BASED_YEAR)
            val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            year * 100 + week
        }

        var currentWeeklyStreak = 0
        var longestWeeklyStreak = 0
        var tempStreak = 0

        val now = Instant.now(clock).atZone(zoneId).toLocalDate()
        val currentYear = now.get(IsoFields.WEEK_BASED_YEAR)
        val currentWeek = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val currentWeekKey = currentYear * 100 + currentWeek

        val sortedWorkoutsDates = sortedWorkouts.map { it.date.atZone(zoneId).toLocalDate() }

        if (sortedWorkoutsDates.isNotEmpty()) {
            val firstDate = sortedWorkoutsDates.first()
            var currentIterDate = firstDate

            // Back up to the start of the week for the first date
            val firstDateDayOfWeek = currentIterDate.dayOfWeek.value
            currentIterDate = currentIterDate.minusDays((firstDateDayOfWeek - 1).toLong())

            val endIterDate = now.plusDays((7 - now.dayOfWeek.value).toLong())

            while (!currentIterDate.isAfter(endIterDate)) {
                val iterYear = currentIterDate.get(IsoFields.WEEK_BASED_YEAR)
                val iterWeek = currentIterDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                val weekKey = iterYear * 100 + iterWeek

                val count = weekBuckets[weekKey]?.size ?: 0

                if (count >= weeklyTarget) {
                    tempStreak++
                    longestWeeklyStreak = maxOf(longestWeeklyStreak, tempStreak)
                } else if (weekKey == currentWeekKey) {
                    // Current week is in progress, preserve streak if previous week met target
                    // No-op to preserve tempStreak
                } else {
                    tempStreak = 0
                }

                if (weekKey == currentWeekKey) {
                    currentWeeklyStreak = tempStreak
                    break
                }

                currentIterDate = currentIterDate.plusWeeks(1)
            }
        }

        val currentWeekWorkoutsCompleted = weekBuckets[currentWeekKey]?.size ?: 0

        // Calculate XP and Levels
        val completedNormalSets = setsWithContext.count { it.set.completed && it.set.setType == 0 } // 0 is NORMAL
        val totalVolumeKg = setsWithContext.filter { it.set.completed }.sumOf { it.set.weight * it.set.reps }
        val totalSets = completedNormalSets // using normal sets for the metric as requested, or all completed sets

        val totalXp = (totalWorkouts * 100) + (completedNormalSets * 10)
        val athleteLevel = (totalXp / 1000) + 1
        val currentXp = totalXp % 1000

        // Calculate Badges
        val firstRepUnlocked = totalWorkouts >= 1
        val centurionUnlocked = totalSets >= 100
        val volume100kUnlocked = totalVolumeKg >= 100000.0
        val ironConsistencyUnlocked = longestWeeklyStreak >= 4
        val titaniumHabitUnlocked = longestWeeklyStreak >= 12
        // Simplified deload logic for badge: a workout with "deload" in notes or very low volume
        val deloadUnlocked = sortedWorkouts.any { it.notes.contains("deload", ignoreCase = true) }

        val badges = listOf(
            AchievementBadge(
                id = "first_rep",
                title = "First Rep",
                description = "Complete your first workout",
                tier = BadgeTier.BRONZE,
                category = BadgeCategory.MILESTONES,
                currentProgress = minOf(totalWorkouts, 1),
                maxProgress = 1,
                isUnlocked = firstRepUnlocked,
                unlockedDateEpochMilli = if (firstRepUnlocked) sortedWorkouts.firstOrNull()?.date?.toEpochMilli() else null
            ),
            AchievementBadge(
                id = "centurion",
                title = "Centurion",
                description = "Complete 100 normal sets",
                tier = BadgeTier.SILVER,
                category = BadgeCategory.MILESTONES,
                currentProgress = minOf(totalSets, 100),
                maxProgress = 100,
                isUnlocked = centurionUnlocked,
                unlockedDateEpochMilli = null // Simplified
            ),
            AchievementBadge(
                id = "volume_100k",
                title = "100k Volume Monster",
                description = "Lift a total of 100,000 kg",
                tier = BadgeTier.GOLD,
                category = BadgeCategory.VOLUME,
                currentProgress = minOf(totalVolumeKg.toInt(), 100000),
                maxProgress = 100000,
                isUnlocked = volume100kUnlocked,
                unlockedDateEpochMilli = null
            ),
            AchievementBadge(
                id = "streak_4w",
                title = "Iron Consistency",
                description = "Achieve a 4-week streak",
                tier = BadgeTier.SILVER,
                category = BadgeCategory.CONSISTENCY,
                currentProgress = minOf(longestWeeklyStreak, 4),
                maxProgress = 4,
                isUnlocked = ironConsistencyUnlocked,
                unlockedDateEpochMilli = null
            ),
            AchievementBadge(
                id = "streak_12w",
                title = "Titanium Habit",
                description = "Achieve a 12-week streak",
                tier = BadgeTier.OBSIDIAN,
                category = BadgeCategory.CONSISTENCY,
                currentProgress = minOf(longestWeeklyStreak, 12),
                maxProgress = 12,
                isUnlocked = titaniumHabitUnlocked,
                unlockedDateEpochMilli = null
            ),
            AchievementBadge(
                id = "deload_disciple",
                title = "Deload Champion",
                description = "Complete a deload session",
                tier = BadgeTier.OBSIDIAN,
                category = BadgeCategory.SPECIAL,
                currentProgress = if (deloadUnlocked) 1 else 0,
                maxProgress = 1,
                isUnlocked = deloadUnlocked,
                unlockedDateEpochMilli = null
            )
        )

        // Generate Heatmap Data
        val heatMapData = mutableMapOf<String, Int>()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = Instant.now(clock).atZone(zoneId).toLocalDate()
        val oneYearAgo = today.minusDays(365)

        // Initialize all dates in past 365 days to 0
        var currentDate = oneYearAgo
        while (!currentDate.isAfter(today)) {
            heatMapData[currentDate.format(formatter)] = 0
            currentDate = currentDate.plusDays(1)
        }

        // Populate with workout counts
        sortedWorkouts.forEach { workout ->
            val localDate = workout.date.atZone(zoneId).toLocalDate()
            if (!localDate.isBefore(oneYearAgo) && !localDate.isAfter(today)) {
                val dateString = localDate.format(formatter)
                heatMapData[dateString] = (heatMapData[dateString] ?: 0) + 1
            }
        }

        return StreakReport(
            currentWeeklyStreak = currentWeeklyStreak,
            longestWeeklyStreak = longestWeeklyStreak,
            currentWeekWorkoutsCompleted = currentWeekWorkoutsCompleted,
            weeklyTarget = weeklyTarget,
            totalWorkouts = totalWorkouts,
            totalVolumeKg = totalVolumeKg,
            totalSets = totalSets,
            athleteLevel = athleteLevel,
            currentXp = currentXp,
            badges = badges,
            heatMapData = heatMapData
        )
    }
}
