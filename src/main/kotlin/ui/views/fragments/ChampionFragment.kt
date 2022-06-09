package ui.views.fragments

import javafx.geometry.Pos
import javafx.scene.text.TextAlignment
import league.api.LeagueCommunityDragonApi
import league.models.ChampionInfo
import league.models.enums.CacheType
import tornadofx.*
import ui.views.util.blackLabel
import util.constants.ViewConstants.IMAGE_WIDTH

class ChampionFragment : Fragment() {
    val champion: ChampionInfo by param(ChampionInfo())
    val showTokens: Boolean by param(true)
    val showYou: Boolean by param(false)

    override val root = stackpane {
        alignment = Pos.TOP_CENTER

        blackLabel("You", textAlignment = TextAlignment.LEFT, fontSize = 11.0) {
            isVisible = champion.isSummonerSelectedChamp && showYou
        }

        imageview(LeagueCommunityDragonApi.getImage(CacheType.CHAMPION, champion.id)) {
            fitWidth = IMAGE_WIDTH
            fitHeight = IMAGE_WIDTH

            effect = LeagueCommunityDragonApi.getChampionImageEffect(champion)
        }

        borderpane {
            left = stackpane {
                alignment = Pos.TOP_LEFT

                blackLabel("Lvl ${champion.level}${champion.percentageUntilNextLevel}", fontSize = 9.6)
            }

            right = stackpane {
                alignment = Pos.TOP_RIGHT

                if (showTokens) {
                    vbox {
                        alignment = Pos.TOP_RIGHT

                        blackLabel(
                            "Tokens: " + when (champion.level) {
                                6 -> "${champion.tokens}/3"
                                5 -> "${champion.tokens}/2"
                                else -> ""
                            },
                            fontSize = 9.6, textAlignment = TextAlignment.RIGHT
                        ) {
                            isVisible = listOf(5, 6).contains(champion.level)
                        }

                        blackLabel(champion.differentChallenges, fontSize = 9.0) {
                            isVisible = champion.differentChallenges != "[]"
                        }
                    }
                }
            }

            if (champion.eternal != null) {
                bottom = find<EternalsFragment>(mapOf(EternalsFragment::eternal to champion.eternal.toProperty(), EternalsFragment::fontSizeIn to 9.0)).root
            }
        }
    }
}