package org.nield.kinance.ux

import javafx.scene.control.TabPane
import tornadofx.*

class MainView: View() {
    override val root = tabpane {

        title = "Kinance"

        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        tab(HardTransactionView::class)
        tab(StagedTransactionView::class)
        tab(AccountView::class)
        tab(CategoryView::class)
    }
}