package com.worldrunner.core.model

data class Team(
    val id: String,
    val name: String,
    val isMine: Boolean,
)

/** A teammate's Weekly total, visible to members of the same Team only. */
data class TeammateTotal(
    val displayName: String,
    val weeklyTotal: Distance,
    val isMe: Boolean,
)

data class TeamDetail(
    val team: Team,
    val leagueName: String,
    val standing: Standing,
    val roster: List<TeammateTotal>,
)

enum class Zone { Promotion, Safe, Relegation }

data class Standing(
    val rank: Int,
    val team: Team,
    val totalDistance: Distance,
    val activeRunners: Int,
    val zone: Zone,
)

data class League(
    val id: String,
    val name: String,
    val tier: Int,
    val standings: List<Standing>,
)

/** A Team's total distance shown as a position along a virtual world route. Not a score. */
data class RouteProgress(
    val travelled: Distance,
    val routeLength: Distance,
    val nextPlace: String,
) {
    val fraction: Float get() = (travelled.metres.toDouble() / routeLength.metres).coerceIn(0.0, 1.0).toFloat()
}
