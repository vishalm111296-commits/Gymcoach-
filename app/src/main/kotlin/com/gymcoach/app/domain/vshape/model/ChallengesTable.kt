package com.gymcoach.app.domain.vshape.model

import java.time.Instant

data class ChallengesTable(
    val id: String,
    val name: String,
    val description: String,
    val category: ChallengeCategory,
    val challengeType: ChallengeType,
    val userId: String,
    val startDate: Instant,
    val endDate: Instant,
    val status: ChallengeStatus,
    val targetDays: Int,
    val completedDays: Int = 0,
    val createdAt: Instant = Instant.now(),
    val tags: List<String> = emptyList(),
    val difficulty: ChallengeDifficulty = ChallengeDifficulty.BEGINNER,
    val rewards: List<ChallengeReward> = emptyList(),
    val dailyRequirements: List<ChallengeDay> = emptyList(),
    val rules: List<ChallengeRule> = emptyList(),
    val exemptions: List<ChallengeExemption> = emptyList()
)