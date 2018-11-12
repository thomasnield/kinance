package org.nield.kinance.entity

import db
import io.reactivex.rxkotlin.toMap
import io.reactivex.rxkotlin.toObservable
import org.nield.rxkotlinjdbc.select
import switchReplaySingle
import java.sql.ResultSet

class Account(
        val id: Int,
        val name: String,
        val accountType: AccountType
) {

    constructor(rs: ResultSet): this(
            rs.getInt("ID"),
            rs.getString("NAME"),
            rs.getString("ACCOUNT_TYPE").let(AccountType::valueOf)
    )

    companion object {
        val all = db.select("SELECT * FROM ACCOUNT")
                .toObservable(::Account)
                .toList()
                .switchReplaySingle()
                .flatMapObservable { it.toObservable() }

        val allAsMap get() = all.map { it.id to it }
                .toMap()
                .switchReplaySingle()
    }

    override fun toString() = name
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Account

        if (id != other.id) return false

        return true
    }
    override fun hashCode(): Int {
        return id
    }
}

enum class AccountType {
    DEBIT,
    CREDIT,
    LOAN;
}