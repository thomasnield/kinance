package org.nield.kinance.entity

import db
import org.nield.rxkotlinjdbc.select

enum class Account(
        val id: Int,
        val accountName: String,
        val number: String,
        val accountType: AccountType
) {

    SWACU_CHECKING_7(1),
    SWACU_CHECKING_8(4),
    SWACU_SAVINGS_0(3),
    CHASE_RR(2);

    constructor(record: Record): this(record.id, record.name, record.number, record.accountType)

    constructor(id: Int): this(
            db.select("SELECT * FROM ACCOUNT WHERE ID = ?").parameter(id).blockingFirst {
                Record(it.getInt("ID"), it.getString("NAME"), it.getString("NUMBER"), it.getString("ACCOUNT_TYPE").let(AccountType::valueOf))
            }
    )
    private class Record(val id: Int, val name: String, val number: String, val accountType: AccountType)

}

enum class AccountType {
    DEBIT,
    CREDIT,
    LOAN;
}