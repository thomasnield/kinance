package org.nield.kinance.ux

import io.reactivex.subjects.BehaviorSubject
import javafx.collections.FXCollections
import org.nield.kinance.entity.Account
import org.nield.kinance.entity.Category
import org.nield.kinance.entity.HardTransaction
import tornadofx.*

class HardTransactionView: View("Hard Transaction") {

    private val backingList = FXCollections.observableArrayList<HardTransactionItem>()
    private val refreshRequests = BehaviorSubject.createDefault(Unit)

    init {
        refreshRequests.switchMapSingle {
            HardTransaction.all
                    .map { HardTransactionItem(it) }
                    .toList()
        }.subscribe {
            backingList.setAll(it)
        }
    }

    override val root = borderpane  {

        center = tableview(backingList) {
            readonlyColumn("ID", HardTransactionItem::id)
            readonlyColumn("ACCOUNT", HardTransactionItem::account)
            readonlyColumn("TRANS DATE", HardTransactionItem::transactionDate)
            readonlyColumn("AMOUNT", HardTransactionItem::amount)
            readonlyColumn("CATEGORY", HardTransactionItem::category)
            readonlyColumn("MEMO", HardTransactionItem::memo)
            readonlyColumn("TAX STATUS", HardTransactionItem::taxStatus)
        }
    }
}

class HardTransactionItem(val ht: HardTransaction) {
    val id get() = ht.id
    val categoryId get() = ht.categoryId
    val category get() = ht.category
    val account get() = ht.account
    val transactionDate get() = ht.transactionDate
    val amount get() = ht.amount
    val memo get() = ht.memo
    val taxStatus get() = ht.taxStatus
}