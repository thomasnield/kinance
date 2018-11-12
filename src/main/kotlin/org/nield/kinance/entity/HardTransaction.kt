package org.nield.kinance.entity

import db
import getLocalDate
import io.reactivex.rxkotlin.Singles
import org.nield.rxkotlinjdbc.select
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.LocalDate

data class HardTransaction(
        val id: Int,
        val categoryId: Int,
        val accountId: Int,
        val transactionDate: LocalDate,
        val amount: BigDecimal,
        val memo: String,
        val taxStatus: TaxStatus,
        val category: Category,
        val account: Account
) {

    constructor(rs: ResultSet, category: Category, account: Account): this(
            rs.getInt("ID"),
            rs.getInt("CATEGORY_ID"),
            rs.getInt("ACCOUNT_ID"),
            rs.getLocalDate("TRANSACTION_DATE"),
            rs.getBigDecimal("AMOUNT"),
            rs.getString("MEMO"),
            rs.getInt("TAX_STATUS").let { TaxStatus.forCode(it) },
            category,
            account
    )

    companion object {

        val all = Singles.zip(Category.allAsMap, Account.allAsMap) { categories, accounts ->
            db.select("SELECT * FROM HARD_TRANSACTION")
                    .toObservable {
                        val categoryId = it.getInt("CATEGORY_ID")
                        val accountId = it.getInt("ACCOUNT_ID")
                        HardTransaction(it, categories[categoryId]!!, accounts[accountId]!!)
                    }
        }.flatMapObservable { it }

        fun of(categoryId: Int? = null,
               effFrom: LocalDate? = null,
               effTo: LocalDate? = null
        ) = StringBuilder().apply {
            if (sequenceOf(categoryId, effFrom, effTo).any { it != null }) append(" WHERE 1 == 1 ")
            if (categoryId != null) append("AND ID = ? ")
            if (effFrom != null) append("AND TRANSACTION_DATE >= ? ")
            if (effTo != null) append("AND TRANSACTION_DATE <= ?")
        }.toString()
        .let { where ->
            db.select("SELECT * FROM HARD_TRANSACTION $where")
                    .apply {
                        sequenceOf(categoryId, effFrom, effTo).filterNotNull().forEach { parameter(it) }
            }
        }.let {
            Singles.zip(Category.allAsMap, Account.allAsMap) { categories, accounts ->
                it.toObservable {
                    HardTransaction(it, categories[categoryId]!!, accounts[it.getInt("ACCOUNT_ID")]!!)
                }
            }
        }
    }
}

enum class TaxStatus(val code: Int) {
    NONE(0),
    DEDUCTIBLE(1),
    TAXABLE(2);

    companion object {
        fun forCode(code: Int) = values().first { it.code == code }
    }
}