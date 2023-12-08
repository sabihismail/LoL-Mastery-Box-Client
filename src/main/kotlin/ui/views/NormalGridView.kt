package ui.views

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import league.models.ChampionInfo
import league.models.enums.ChampionRole
import league.models.enums.GameMode
import league.models.enums.Role
import league.models.json.Challenge
import tornadofx.*
import ui.views.fragments.ChampionFragment
import ui.views.util.boldLabel
import util.constants.ViewConstants.IMAGE_HORIZONTAL_COUNT
import util.constants.ViewConstants.IMAGE_WIDTH


class NormalGridView: View() {
    val currentLaneProperty = SimpleObjectProperty(Role.ANY)
    val currentChampionRoleProperty = SimpleObjectProperty(ChampionRole.ANY)
    val currentChallengeProperty = SimpleObjectProperty<Challenge>(null)

    private val allChampionsProperty = SimpleListProperty<ChampionInfo>()
    private val championListProperty = SimpleListProperty<ChampionInfo>()
    private val eternalsOnlyProperty = SimpleBooleanProperty(false)
    private val loadEternalsProperty = SimpleBooleanProperty(false)
    private val completableChallengesProperty = SimpleListProperty<Challenge>()
    private val championSearchProperty = SimpleStringProperty("")

    fun setChampions(lst: List<ChampionInfo>) {
        allChampionsProperty.value = FXCollections.observableList(lst)

        setActiveChampions()
    }

    fun setCompletableChallenges(completableChallenges: List<Challenge>) {
        runAsync {
            FXCollections.observableList(
                completableChallenges.sortedBy { it.description }.filter { it.gameModeSet != setOf(GameMode.ARAM) }
            )
        } ui {
            completableChallengesProperty.value = it
        }
    }

    private fun setActiveChampions() {
        runAsync {
            FXCollections.observableList(
                allChampionsProperty.value.filter { !eternalsOnlyProperty.value || it.eternalInfo.any { eternal -> eternal.value } }
                    .filter { it.nameLower.contains(championSearchProperty.value.lowercase()) }
                    .filter { currentChampionRoleProperty.value == ChampionRole.ANY || it.roles?.contains(currentChampionRoleProperty.value) == true }
                    .filter {
                        currentChallengeProperty.value == null ||
                                (currentChallengeProperty.value.availableIdsInt?.isEmpty() == true && !it.completedChallenges.contains(currentChallengeProperty.value.id?.toInt())) ||
                                (currentChallengeProperty.value.availableIdsInt?.isEmpty() == false && it.availableChallenges.contains(currentChallengeProperty.value.id?.toInt()) && !it.completedChallenges.contains(currentChallengeProperty.value.id?.toInt()))
                    }
            )
        } ui {
            championListProperty.value = it
        }
    }

    override val root = borderpane {
        prefHeight = 1000.0

        center = vbox {
            alignment = Pos.CENTER

            stackpane {
                alignment = Pos.CENTER_LEFT
                paddingHorizontal = 16.0

                boldLabel("Available Champions:")
            }

            datagrid(championListProperty) {
                alignment = Pos.CENTER
                prefHeight = 600.0
                paddingBottom = 8.0

                maxRows = 32
                maxCellsInRow = IMAGE_HORIZONTAL_COUNT
                cellWidth = IMAGE_WIDTH
                cellHeight = IMAGE_WIDTH

                cellFormat {
                    graphic = find<ChampionFragment>(mapOf(ChampionFragment::champion to it, ChampionFragment::showEternals to loadEternalsProperty.value)).root
                }
            }

            textfield(championSearchProperty) {
                paddingRight = 16
                paddingBottom = 4

                textProperty().addListener { _, _, _ ->
                    setActiveChampions()
                }
            }
        }

        bottom = borderpane {
            right = vbox {
                alignment = Pos.BOTTOM_RIGHT
                paddingBottom = 24.0
                paddingRight = 24.0

                vbox {
                    alignment = Pos.BOTTOM_RIGHT
                    spacing = 6.0

                    hbox {
                        alignment = Pos.CENTER_RIGHT

                        label("Lane: ")
                        combobox(currentLaneProperty, Role.entries)
                    }

                    hbox {
                        alignment = Pos.CENTER_RIGHT

                        label("Character Role: ")
                        combobox(currentChampionRoleProperty, ChampionRole.entries)
                    }

                    hbox {
                        alignment = Pos.CENTER_RIGHT

                        label("Challenge (skips completed champs): ")
                        combobox(currentChallengeProperty, completableChallengesProperty)
                    }

                    hbox {
                        alignment = Pos.CENTER_RIGHT
                        spacing = 10.0

                        checkbox("Load Eternals", loadEternalsProperty).apply {
                            loadEternalsProperty.onChange { setActiveChampions() }
                        }

                        checkbox("Champions with Eternals Only", eternalsOnlyProperty).apply {
                            eternalsOnlyProperty.onChange { setActiveChampions() }
                        }
                    }
                }
            }
        }
    }
}
