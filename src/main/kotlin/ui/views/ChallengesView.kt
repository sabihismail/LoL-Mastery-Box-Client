package ui.views

import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.ScrollPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import league.api.LeagueCommunityDragonApi
import league.models.ChallengeFilter
import league.models.ChallengeUiRefreshData
import league.models.enums.ChallengeCategory
import league.models.enums.ChallengeLevel
import league.models.enums.GameMode
import league.models.json.ChallengeInfo
import league.models.json.ChallengeSummary
import tornadofx.*
import ui.views.fragments.ChallengeFragment
import util.constants.GenericConstants
import util.constants.ViewConstants.CHALLENGE_IMAGE_WIDTH
import util.constants.ViewConstants.DEFAULT_SPACING
import util.constants.ViewConstants.SCROLLBAR_HEIGHT

class ChallengesView : View("League Challenges") {
    val currentGameModeProperty = SimpleObjectProperty(GameMode.ANY)

    private val challengesSummaryProperty = SimpleObjectProperty<ChallengeSummary>()
    private val allCategoriesProperty = SimpleListProperty<ChallengeCategory>()
    private val categoriesProperty = SimpleListProperty<ChallengeCategory>()
    private val allChallengesProperty = SimpleMapProperty<ChallengeCategory, List<ChallengeInfo>>()
    private val filteredChallengesProperty = SimpleMapProperty<ChallengeCategory, List<ChallengeInfo>>()

    private val hideEarnPointChallengesProperty = SimpleBooleanProperty(true)
    private val hidePremadeChallengesProperty = SimpleBooleanProperty(true)
    private val hideCompletedChallengesProperty = SimpleBooleanProperty(true)
    private val hideNonTitleChallengesProperty = SimpleBooleanProperty(false)
    private val hideWinChallengesProperty = SimpleBooleanProperty(false)
    private val hideNonWinChallengesProperty = SimpleBooleanProperty(false)
    private val hideMultiTierChallengesProperty = SimpleBooleanProperty(false)
    private val hideCollectionProperty = SimpleBooleanProperty(false)
    private val hideLegacyProperty = SimpleBooleanProperty(false)
    private val hideNonSeasonChallengesProperty = SimpleBooleanProperty(false)
    private val currentSearchTextProperty = SimpleStringProperty("")

    private lateinit var verticalRow: ScrollPane
    private lateinit var grid: DataGrid<ChallengeCategory>

    fun setChallenges(summary: ChallengeSummary = challengesSummaryProperty.value,
                      challengeInfo: Map<ChallengeCategory, List<ChallengeInfo>> = allChallengesProperty.value,
                      allCategories: List<ChallengeCategory> = allCategoriesProperty.value) {
        runAsync {
            val filters = listOf(
                ChallengeFilter(hideEarnPointChallengesProperty.get()) { challengeInfo ->
                    !CRINGE_MISSIONS.any { x -> challengeInfo.description!!.contains(x) }
                },

                ChallengeFilter(hidePremadeChallengesProperty.get()) { challengeInfo ->
                    !challengeInfo.description!!.lowercase().contains("premade")
                },

                ChallengeFilter(hideCompletedChallengesProperty.get()) { challengeInfo -> !challengeInfo.isComplete },

                ChallengeFilter(hideNonTitleChallengesProperty.get()) { challengeInfo -> challengeInfo.hasRewardTitle && !challengeInfo.rewardObtained },

                ChallengeFilter(hideWinChallengesProperty.get()) { challengeInfo -> !challengeInfo.description!!.lowercase().contains("win") },

                ChallengeFilter(hideNonWinChallengesProperty.get()) { challengeInfo -> challengeInfo.description!!.lowercase().contains("win") },

                ChallengeFilter(hideMultiTierChallengesProperty.get()) { challengeInfo -> challengeInfo.thresholds!!.count() == 1 },

                ChallengeFilter(hideNonSeasonChallengesProperty.get()) { challengeInfo -> challengeInfo.name?.contains(GenericConstants.YEAR) == true },

                ChallengeFilter(true) { challengeInfo ->
                    if (challengeInfo.category == ChallengeCategory.COLLECTION) return@ChallengeFilter true

                    when(currentGameModeProperty.value) {
                        GameMode.ANY -> true
                        GameMode.ARAM -> challengeInfo.gameModeSet.contains(GameMode.ARAM) || challengeInfo.description!!.contains("ARAM")
                        else -> challengeInfo.gameModeSet.contains(currentGameModeProperty.value) && !challengeInfo.description!!.contains("ARAM")
                    }
                },

                ChallengeFilter(currentSearchTextProperty.value.isNotEmpty()) { challengeInfo ->
                    challengeInfo.descriptiveDescription.lowercase().contains(currentSearchTextProperty.value.lowercase())
                },
            )

            val sortedMap = challengeInfo.toList().associate { (k, v) -> k to v.filter { challengeInfo -> filters.filter { it.isSet }.all { it.action(challengeInfo) } } }

            var categories = if (hideCollectionProperty.value) allCategories.filter { it != ChallengeCategory.COLLECTION } else allCategories
            categories = if (hideLegacyProperty.value) categories.filter { it != ChallengeCategory.LEGACY } else categories

            ChallengeUiRefreshData(summary, FXCollections.observableMap(challengeInfo), FXCollections.observableMap(sortedMap),
                FXCollections.observableList(allCategories), FXCollections.observableList(categories))
        } ui {
            challengesSummaryProperty.value = it.challengesSummary
            allCategoriesProperty.value = it.allCategories
            categoriesProperty.value = it.categories
            allChallengesProperty.value = it.allChallenges
            filteredChallengesProperty.value = it.filteredChallenges

            ROW_COUNT = categoriesProperty.size

            verticalRow.minHeight = getOuterGridPaneHeight()
            verticalRow.maxHeight = getOuterGridPaneHeight()

            grid.minHeight = getOuterGridPaneHeight()
            grid.cellWidth = (CHALLENGE_IMAGE_WIDTH + DEFAULT_SPACING * 2) * (categoriesProperty.maxOfOrNull { key -> filteredChallengesProperty[key]!!.size } ?: 1)

            currentWindow!!.sizeToScene()
            currentWindow!!.centerOnScreen()
        }
    }

    init {
        setOf(
            hideEarnPointChallengesProperty,
            hideCompletedChallengesProperty,
            hideNonTitleChallengesProperty,
            hideWinChallengesProperty,
            hideNonWinChallengesProperty,
            hideMultiTierChallengesProperty,
            hideCollectionProperty,
            hideLegacyProperty,
            hideNonSeasonChallengesProperty,
            currentGameModeProperty,
            currentSearchTextProperty,
        ).forEach {
            it.onChange {
                if (challengesSummaryProperty.value == null || allChallengesProperty.value == null || allCategoriesProperty.value == null) return@onChange
                setChallenges()
            }
        }
    }

    private fun getWorldPercentage(percentage: Double): String {
        return "Top " + "%.2f".format(percentage) + "% World"
    }

    private fun getChallengeString(level: ChallengeLevel, key: String, current: Long, s: String = " - ", includeTotal: Boolean = false): String {
        val maxPoints = LeagueCommunityDragonApi.getChallenge(key, ChallengeLevel.entries[level.ordinal + 1])
        val minPoints = LeagueCommunityDragonApi.getChallenge(key, ChallengeLevel.entries[level.ordinal])

        val currentPoints = current - minPoints
        val maxPointsCurrentLevel = maxPoints - minPoints

        val currentPercentage = "%.2f".format(currentPoints.toDouble().div(maxPointsCurrentLevel) * 100) + "%"

        return "$level$s$currentPercentage ($currentPoints/$maxPointsCurrentLevel)" + (if (includeTotal) " (${minPoints + currentPoints}/$maxPoints)" else "")
    }

    override val root = vbox {
        alignment = Pos.CENTER
        minWidth = 1580.0
        maxWidth = 1580.0

        hbox {
            spacing = 0.0

            label(challengesSummaryProperty.select {
                getChallengeString(it.overallChallengeLevel!!, TOTAL_CHALLENGE_POINTS_KEY, it.totalChallengeScore!!, includeTotal = true).toProperty()
            }) {
                textFill = Color.WHITE
                font = Font.font(Font.getDefault().family, FontWeight.BOLD, HEADER_FONT_SIZE + 2.0)
                paddingHorizontal = 16.0
                paddingVertical = 0.0

                fitToParentWidth()
                style {
                    backgroundColor += Color.BLACK
                }
            }

            label(challengesSummaryProperty.select { getWorldPercentage(it.positionPercentile!!).toProperty() }) {
                textFill = Color.WHITE
                alignment = Pos.TOP_RIGHT
                font = Font.font(Font.getDefault().family, FontWeight.BOLD, HEADER_FONT_SIZE + 2.0)
                paddingHorizontal = 8.0

                fitToParentWidth()
                style {
                    backgroundColor += Color.BLACK
                }
            }
        }

        verticalRow = scrollpane(fitToWidth = true) {
            vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            minHeight = getOuterGridPaneHeight()
            maxHeight = getOuterGridPaneHeight()

            grid = datagrid(categoriesProperty) {
                maxCellsInRow = 1
                verticalCellSpacing = SPACING_BETWEEN_ROW
                cellHeight = INNER_CELL_HEIGHT
                minHeight = getOuterGridPaneHeight()

                cellFormat {
                    graphic = vbox {
                        alignment = Pos.CENTER_LEFT
                        maxHeight = INNER_CELL_HEIGHT
                        minHeight = INNER_CELL_HEIGHT

                        label(
                            challengesSummaryProperty.select { summary ->
                                val category = summary.categoryProgress!!.firstOrNull { category -> category.category == it }
                                if (category == null)
                                    it.name.toProperty()
                                else
                                    (it.name + " (" + getChallengeString(category.level!!, category.category!!.name, category.current!!.toLong(), s=") - ") + " --- " +
                                            getWorldPercentage(category.positionPercentile!!)).toProperty()
                            }
                        ) {
                            textFill = Color.WHITE
                            font = Font.font(HEADER_FONT_SIZE)
                            paddingHorizontal = 8.0

                            fitToParentWidth()
                            style {
                                backgroundColor += Color.BLACK
                            }
                        }

                        datagrid(filteredChallengesProperty[it]) {
                            alignment = Pos.CENTER
                            maxRows = 1
                            cellWidth = CHALLENGE_IMAGE_WIDTH
                            cellHeight = CHALLENGE_IMAGE_WIDTH

                            cellFormat {
                                graphic = find<ChallengeFragment>(
                                    mapOf(ChallengeFragment::challenge to it, ChallengeFragment::bracketText to it.pointsDifference.toString())
                                ).root
                            }
                        }
                    }
                }
            }
        }

        vbox {
            textfield {
                textProperty().addListener { _, _, newValue ->
                    currentSearchTextProperty.set(newValue)
                }
            }

            hbox {
                paddingHorizontal = 10.0
                spacing = 10.0

                checkbox("Hide Grind/Time", hideEarnPointChallengesProperty)
                checkbox("Hide Premade", hidePremadeChallengesProperty)
                checkbox("Hide Completed", hideCompletedChallengesProperty)
                checkbox("Hide Non-Title", hideNonTitleChallengesProperty)
                checkbox("Hide Win", hideWinChallengesProperty)
                checkbox("Hide Non-Win", hideNonWinChallengesProperty)
                checkbox("Hide Non-Season", hideNonSeasonChallengesProperty)
                checkbox("Hide Multi-tier", hideMultiTierChallengesProperty)
                checkbox("Hide Collection", hideCollectionProperty)
                checkbox("Hide Legacy", hideLegacyProperty)
                combobox(currentGameModeProperty, listOf(GameMode.ANY, GameMode.ARAM, GameMode.CLASSIC))
            }
        }
    }

    companion object {
        private const val TOTAL_CHALLENGE_POINTS_KEY = "CRYSTAL"

        private const val HEADER_FONT_SIZE = 14.0
        private const val SPACING_BETWEEN_ROW = 4.0

        // image_height + 2 * verticalCellSpacing + font size of label
        private const val INNER_CELL_HEIGHT = CHALLENGE_IMAGE_WIDTH + (DEFAULT_SPACING * 2) + HEADER_FONT_SIZE

        private var ROW_COUNT = 6
        // cell + row_spacing for 6 rows + vert spacing
        private var getOuterGridPaneHeight = {
            (INNER_CELL_HEIGHT + SPACING_BETWEEN_ROW * 2) * ROW_COUNT + DEFAULT_SPACING * 2 + SCROLLBAR_HEIGHT
        }

        val CRINGE_MISSIONS = setOf(
            "Earn points from challenges in the ",
        )
    }
}