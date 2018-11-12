package org.nield.kinance.ux

import com.github.thomasnield.rxkotlinfx.actionEvents
import com.github.thomasnield.rxkotlinfx.events
import com.github.thomasnield.rxkotlinfx.onChangedObservable
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.subjects.PublishSubject
import javafx.collections.FXCollections
import javafx.scene.control.SelectionMode
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.FileChooser
import mapNonNulls
import org.nield.kinance.entity.Category
import org.nield.kinance.entity.StagedTransaction
import org.nield.kinance.entity.TaxStatus
import refreshRequests
import tornadofx.*
import java.util.concurrent.TimeUnit

class TransactionImportView : View("Import Transaction") {

    private val backingList = FXCollections.observableArrayList<StagedTransaction>()
    private val importRequests = PublishSubject.create<Observable<StagedTransaction>>()

    init {
        importRequests.flatMapSingle {
            it.toList()
        }.subscribe { backingList.setAll(it) }
    }

    override val root = borderpane {
        top = menubar {
            menu("Import") {

                item("CHASE").actionEvents()
                        .mapNonNulls {
                            chooseFile("Choose File", filters = arrayOf(FileChooser.ExtensionFilter("Any", "*.*"))).firstOrNull()?.absolutePath
                        }.filter { it.isNotBlank() }
                        .map {
                            StagedTransaction.fromChaseFile(it)
                        }.subscribe(importRequests)


                item("SWACU").actionEvents().mapNonNulls {
                    chooseFile("Choose File", filters = arrayOf(FileChooser.ExtensionFilter("Any", "*.*"))).firstOrNull()?.absolutePath
                }.filter { it.isNotBlank() }
                .map {
                    StagedTransaction.fromSwacuFile(it)
                }.subscribe(importRequests)
            }

            menu("Predict") {
                item("Predict Meta").actionEvents()
                        .flatMapSingle {
                            backingList.toObservable().flatMapSingle { it.predict() }.toList()
                        }.subscribe()
            }
            menu("Commit") {
                item("All Staged").actionEvents()
                        .flatMapSingle {
                            backingList.toObservable().filter { it.category != null }
                                    .flatMapSingle { t -> t.commit().map { t } }
                                    .toList()
                                    .doOnSuccess { it.forEach { t -> backingList.remove(t) } }
                        }
                        .map { Unit }
                        .subscribe(refreshRequests)
            }
        }

        center = tableview(backingList) {
            isEditable = true
            selectionModel.selectionMode = SelectionMode.SINGLE

            val categories = FXCollections.observableArrayList<Category>().apply {
                Category.all.subscribe { add(it) }
            }

            val taxStatus = TaxStatus.values().toList().observable()

            readonlyColumn("TRANS DATE", StagedTransaction::transactionDate)
            readonlyColumn("AMOUNT", StagedTransaction::amount)
            readonlyColumn("DESCRIPTION", StagedTransaction::description)
            column("CATEGORY", StagedTransaction::categoryProperty).useComboBox(categories)
            readonlyColumn("PREDICTED CATEGORY", StagedTransaction::predictedCategory)

            column("TAX STATUS", StagedTransaction::taxStatusProperty).useComboBox(taxStatus)
            readonlyColumn("ACCOUNT", StagedTransaction::account)

            backingList.onChangedObservable().subscribe { resizeColumnsToFitContent() }


            events(KeyEvent.KEY_PRESSED)
                    .filter { it.code == KeyCode.DELETE }
                    .subscribe {
                        backingList.remove(selectionModel.selectedItem)
                    }

            val typedKeys = events(KeyEvent.KEY_TYPED)
                    .map { it.character }
                    .filter { it.matches(Regex("[A-Za-z]")) }
                    .publish().refCount()

            typedKeys.debounce(200, TimeUnit.MILLISECONDS).startWith("")
                    .switchMap {
                        typedKeys.scan { x,y -> x + y }
                                .switchMap { input ->
                                    Category.all
                                            .filter { it.toString().toUpperCase().startsWith(input.toUpperCase()) }
                                            .take(1)
                                }.distinctUntilChanged()
                    }.subscribe {
                        with (selectionModel) {
                            if (selectedItem != null) {
                                selectedItem.category = it
                            }
                        }
                    }

            typedKeys.debounce(300,TimeUnit.MILLISECONDS).subscribe {
                with (selectionModel) {
                    select(selectedIndex + 1)
                }
            }
        }
    }
}