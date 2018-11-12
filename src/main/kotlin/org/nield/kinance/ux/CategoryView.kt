package org.nield.kinance.ux

import com.github.thomasnield.rxkotlinfx.onChangedObservable
import io.reactivex.subjects.BehaviorSubject
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import org.nield.kinance.entity.Category
import org.nield.kinance.entity.HardTransaction
import tornadofx.*
import java.time.LocalDate

class CategoryView: View("Category") {

    private val backingList = FXCollections.observableArrayList<CategoryItem>()
    private val refreshRequests = BehaviorSubject.createDefault(Unit)

    private val effFrom = SimpleObjectProperty<LocalDate>()
    private val effTo = SimpleObjectProperty<LocalDate>()

    init {
        refreshRequests.switchMapSingle {
            Category.all.map(::CategoryItem).toList()
        }.subscribe {
            backingList.setAll(it)
        }
    }
    override val root = borderpane {

        left = form {
            fieldset {
                field("EFF FROM") {
                    datepicker(effFrom)
                }
                field("EFF TO") {
                    datepicker(effTo)
                }
            }
        }

        center = tableview(backingList) {
            readonlyColumn("ID", CategoryItem::id)
            readonlyColumn("NAME", CategoryItem::name)
            readonlyColumn("FLOW TYPE", CategoryItem::flowType)

            items.onChangedObservable().subscribe { resizeColumnsToFitContent() }
        }
    }
}

class CategoryItem(val category: Category) {
    val id = category.id
    val name = category.name
    val flowType = category.flowType

    val transactions = HardTransaction.of(categoryId = category.id)

}