package league.models

import generated.LolStatstonesStatstoneSet
import league.api.LeagueApi
import league.models.enums.ChampionOwnershipStatus
import league.models.enums.ChampionRole

data class ChampionInfo(val id: Int = -1, val name: String = "None", val ownershipStatus: ChampionOwnershipStatus = ChampionOwnershipStatus.NOT_OWNED,
                        val masteryPoints: Int = 1, val currentMasteryPoints: Int = 1, val nextLevelMasteryPoints: Int = 2, val level: Int = 0, val tokens: Int = 0,
                        var isSummonerSelectedChamp: Boolean = false, var eternal: LolStatstonesStatstoneSet? = null, var roles: Set<ChampionRole>? = null) {
    val percentageUntilNextLevel by lazy {
        if (level in 1..4)
            " (${"%.1f".format((currentMasteryPoints.toDouble()/(nextLevelMasteryPoints + currentMasteryPoints)) * 100)}%)"
        else
            ""
    }

    val differentChallenges by lazy {
        val map = listOf(
            LeagueApi.getChampionWinInSummonersRift(id) to "SR",
            LeagueApi.getChampionWinInBotGames(id) to "BOT",
            LeagueApi.getChampionWonWithoutDying(id) to "0D",
            LeagueApi.getChampionGotPentakill(id) to "P"
        )

        "[" + map.filter { !it.first }.joinToString("|") { it.second } + "]"
    }

    override fun toString(): String {
        return "ChampionInfo(id=$id, name='$name', ownershipStatus=$ownershipStatus, masteryPoints=$masteryPoints, currentMasteryPoints=$currentMasteryPoints, " +
                "nextLevelMasteryPoints=$nextLevelMasteryPoints, level=$level, tokens=$tokens, isSummonerSelectedChamp=$isSummonerSelectedChamp, eternal=$eternal, " +
                "roles=$roles, percentageUntilNextLevel='$percentageUntilNextLevel')"
    }
}