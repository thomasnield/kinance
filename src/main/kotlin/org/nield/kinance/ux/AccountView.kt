package org.nield.kinance.ux

import io.reactivex.subjects.BehaviorSubject
import javafx.collections.FXCollections
import org.nield.kinance.entity.Account
import tornadofx.*

class AccountView: View("Account") {

    private val backingList = FXCollections.observableArrayList<AccountItem>()
    private val refreshRequests = BehaviorSubject.createDefault(Unit)

    init {
        refreshRequests.flatMapSingle {
            Account.all
                    .map(::AccountItem)
                    .toList()
        }.subscribe {
            backingList.setAll(it)
        }
    }
    override val root = borderpane {
        center = tableview(backingList) {
            readonlyColumn("ID", AccountItem::id)
            readonlyColumn("NAME", AccountItem::name)
            readonlyColumn("ACCOUNT TYPE", AccountItem::accountType)
        }
    }
}

class AccountItem(val account: Account) {
    val id = account.id
    val name = account.name
    val accountType = account.accountType
}