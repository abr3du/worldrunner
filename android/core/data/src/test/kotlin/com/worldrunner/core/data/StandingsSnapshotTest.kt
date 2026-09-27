package com.worldrunner.core.data

import com.worldrunner.core.data.snapshot.StandingsSnapshot
import com.worldrunner.core.data.snapshot.parseGermanKilometres
import com.worldrunner.core.model.Distance
import com.worldrunner.core.model.WorldRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class StandingsSnapshotTest {
    @Test
    fun germanNumbersGroupThousandsWithDotsAndUseACommaForDecimals() {
        assertEquals(Distance.kilometres(4_138.0), parseGermanKilometres("4.138"))
        assertEquals(Distance.kilometres(916.0), parseGermanKilometres("916"))
        assertEquals(Distance(1_234_500), parseGermanKilometres("1.234,5"))
        assertEquals(Distance(12_345_678_000), parseGermanKilometres("12.345.678"))
        assertEquals(Distance(1_250), parseGermanKilometres("1,25"))
        assertEquals(Distance.Zero, parseGermanKilometres("0"))
    }

    @Test
    fun ambiguousOrEnglishNumbersAreRejected() {
        listOf("4,138.5", "4.13", "41.38.0", "", "-5", "4 138", "1.234,").forEach { text ->
            assertThrows("\"$text\"", IllegalArgumentException::class.java) { parseGermanKilometres(text) }
        }
    }

    /** The bundled snapshot, checked against the kmspiel page it was read from (see the header of liga-4.tsv). */
    @Test
    fun bundledSnapshotIsTheImportedKmspielLeague() {
        val view = StandingsSnapshot.readBundled()
        val standings = view.league.standings

        assertEquals("kmspiel 4. Liga", view.league.name)
        assertEquals(35, standings.size)
        assertEquals((1..35).toList(), standings.map { it.rank })
        assertEquals("Österreich", standings.first().team.name)
        assertEquals(Distance.kilometres(4_138.0), standings.first().totalDistance)
        assertEquals("Musiker", standings.last().team.name)
        assertEquals(Distance.kilometres(395.0), standings.last().totalDistance)
        assertEquals("„Lebenshilfe - Wir bewegen uns!\"", standings[5].team.name)
        assertEquals("TuS Oedt", standings[32].team.name)
        assertEquals(standings.sortedByDescending { it.totalDistance }, standings)
        assertEquals(35, standings.map { it.team.id }.toSet().size)

        val mine = standings.single { it.team.isMine }
        assertEquals(20, mine.rank)
        assertEquals("LG Albatros Kiel", mine.team.name)
        assertEquals(Distance.kilometres(2_314.0), mine.totalDistance)
        assertEquals(10, mine.activeRunners)
        assertEquals(mine.totalDistance, view.routeProgress.travelled)

        val source = view.source!!
        assertEquals("2026-2", source.season)
        assertEquals(LocalDate.of(2026, 9, 27), source.asOf)
        assertEquals("https://www.kmspiel.de/2018/km_liga.php?liga=4", source.url)
        assertEquals(Instant.parse("2026-09-27T19:43:56Z"), source.retrievedAt)
    }

    @Test
    fun noImportedTeamHasCompletedACircuitYet() {
        StandingsSnapshot.readBundled().league.standings.forEach {
            assertEquals(it.team.name, 0, WorldRoute.positionAt(it.totalDistance).lap)
        }
    }

    @Test
    fun brokenSnapshotsFailLoudlyInsteadOfShowingPartialData() {
        val good = """
            # competition	kmspiel 4. Liga
            # season	2026-2
            # as_of	27.09.2026
            # source_url	https://example.invalid
            # retrieved_at	2026-09-27T19:43:56Z
            # unit	km
            # my_team	B
            rank	team	distance	runners
            1	A	1.000	3
            2	B	999	2
        """.trimIndent()
        assertEquals(2, StandingsSnapshot.parse(good).league.standings.size)

        val broken = mapOf(
            "skipped rank" to good.replace("2\tB", "3\tB"),
            "missing my team" to good.replace("my_team\tB", "my_team\tC"),
            "miles" to good.replace("unit\tkm", "unit\tmi"),
            "English number" to good.replace("1.000", "1,000.0"),
            "missing column" to good.replace("\t3\n", "\n"),
            "missing header" to good.lines().filterNot { it.startsWith("# season") }.joinToString("\n"),
        )
        broken.forEach { (what, text) ->
            val error = runCatching { StandingsSnapshot.parse(text) }.exceptionOrNull()
            assertTrue(what, error is IllegalArgumentException || error is java.time.DateTimeException)
        }
    }
}
