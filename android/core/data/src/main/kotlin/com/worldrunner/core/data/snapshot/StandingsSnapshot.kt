package com.worldrunner.core.data.snapshot

import com.worldrunner.core.data.LeagueView
import com.worldrunner.core.model.Distance
import com.worldrunner.core.model.League
import com.worldrunner.core.model.RouteProgress
import com.worldrunner.core.model.Standing
import com.worldrunner.core.model.StandingsSource
import com.worldrunner.core.model.Team
import com.worldrunner.core.model.WorldRoute
import com.worldrunner.core.model.Zone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Reads a kmspiel Standings snapshot written by `tools/kmspiel_standings.py`: `# key<TAB>value` header lines, then
 * a `rank team distance runners` table. See `docs/adr/0003-kmspiel-standings-prototype.md`.
 */
object StandingsSnapshot {
    /** The snapshot bundled with the test app, as a JVM resource of `:core:data`. */
    const val BUNDLED = "/kmspiel/liga-4.tsv"

    fun parse(text: String): LeagueView {
        val lines = text.lines().filter { it.isNotBlank() }
        val header = lines.filter { it.startsWith("# ") && '\t' in it }
            .associate { it.removePrefix("# ").split('\t', limit = 2).let { (k, v) -> k to v } }
        fun field(key: String) = requireNotNull(header[key]) { "Snapshot has no \"$key\" line" }
        require(field("unit") == "km") { "Unsupported unit: ${field("unit")}" }

        val table = lines.filterNot { it.startsWith("#") }
        require(table.firstOrNull() == "rank\tteam\tdistance\trunners") { "Unexpected table header: ${table.firstOrNull()}" }
        val myTeam = field("my_team")
        val standings = table.drop(1).mapIndexed { i, line ->
            val cells = line.split('\t')
            require(cells.size == 4) { "Expected 4 columns: $line" }
            val (rank, name, distance, runners) = cells
            require(rank.toInt() == i + 1) { "Ranks must run 1..n, found $rank at row ${i + 1}" }
            // kmspiel has promotion and relegation rules of its own; they are not in the snapshot, so no zones.
            Standing(rank.toInt(), Team("kmspiel:$name", name, isMine = name == myTeam), parseGermanKilometres(distance), runners.toInt(), Zone.Safe)
        }
        val mine = requireNotNull(standings.firstOrNull { it.team.isMine }) { "my_team \"$myTeam\" is not in the table" }

        val source = StandingsSource(
            competition = field("competition"),
            season = field("season"),
            asOf = LocalDate.parse(field("as_of"), GERMAN_DATE),
            url = field("source_url"),
            retrievedAt = Instant.parse(field("retrieved_at")),
        )
        val league = League(id = "kmspiel-${field("competition")}", name = field("competition"), tier = 0, standings = standings)
        val progress = RouteProgress(mine.totalDistance, WorldRoute.length, WorldRoute.positionAt(mine.totalDistance).next.name)
        return LeagueView(league, progress, source)
    }

    fun readBundled(): LeagueView {
        val stream = requireNotNull(StandingsSnapshot::class.java.getResourceAsStream(BUNDLED)) { "$BUNDLED is missing" }
        return parse(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
    }

    private val GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy")
}

private val GERMAN_NUMBER = Regex("""\d{1,3}(\.\d{3})*(,\d+)?|\d+(,\d+)?""")

/**
 * Parses a kilometre value in German notation, where "." groups thousands and "," is the decimal mark:
 * "4.138" is 4,138 km and "1.234,5" is 1,234.5 km. Rejects anything else rather than guessing.
 */
fun parseGermanKilometres(text: String): Distance {
    require(GERMAN_NUMBER.matches(text)) { "Not a German-formatted number: \"$text\"" }
    val km = BigDecimal(text.replace(".", "").replace(',', '.'))
    return Distance(km.movePointRight(3).setScale(0, RoundingMode.HALF_UP).longValueExact())
}
