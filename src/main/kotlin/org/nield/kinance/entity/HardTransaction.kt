package org.nield.kinance.entity

import db
import getLocalDate
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
        val taxStatus: TaxStatus
) {

    constructor(rs: ResultSet): this(
            rs.getInt("ID"),
            rs.getInt("CATEGORY_ID"),
            rs.getInt("ACCOUNT_ID"),
            rs.getLocalDate("TRANSACTION_DATE"),
            rs.getBigDecimal("AMOUNT"),
            rs.getString("MEMO"),
            rs.getInt("TAX_STATUS").let { TaxStatus.forCode(it) }
    )

    val category = Category.forId(categoryId)

    val account = Account.forId(accountId)

    companion object {

        val all
            get() = db.select("SELECT * FROM HARD_TRANSACTION")
                    .toSequence(::HardTransaction)
                    .toList()

        fun of(categoryId: Int? = null,
               effFrom: LocalDate? = null,
               effTo: LocalDate? = null
        ) = db.select("SELECT * FROM HARD_TRANSACTION")
                .whereIfProvided("CATEGORY_ID", categoryId)
                .whereIfProvided("EFF_FROM", effFrom)
                .whereIfProvided("EFF_TO", effTo)
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