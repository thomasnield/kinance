package org.nield.kinance.entity

import com.github.thomasnield.rxkotlinfx.toBinding
import db
import discretizeWords
import io.reactivex.Single
import io.reactivex.rxkotlin.toObservable
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.nield.kinance.prediction.PredictorModel
import org.nield.rxkotlinjdbc.insert
import toCurrency
import tornadofx.*
import java.io.FileReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class StagedTransaction(
        val transactionDate: LocalDate,
        val amount: BigDecimal,
        val description: String,
        account: Account
) {

    val categoryProperty = SimpleObjectProperty<Category?>()
    var category by categoryProperty

    val accountProperty = SimpleObjectProperty(account)
    var account by accountProperty

    val predictedCategoryAndConfidence = PredictorModel.categoryPredictor.predictWithProbability(description.discretizeWords())

    val predictedCategory get() = predictedCategoryAndConfidence?.let { "${it.category}: ${BigDecimal.valueOf(it.probability).setScale(2, RoundingMode.HALF_UP)}" }

    val taxStatusProperty = SimpleObjectProperty<TaxStatus?>(TaxStatus.NONE)
    var taxStatus by taxStatusProperty

    fun commit() = if (category != null && taxStatus != null) {

            db.insert("INSERT INTO HARD_TRANSACTION (CATEGORY_ID, ACCOUNT_ID, TRANSACTION_DATE, AMOUNT, MEMO, TAX_STATUS) VALUES (?,?,?,?,?,?)")
                    .parameter(category?.id)
                    .parameter(account.id)
                    .parameter(transactionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .parameter(amount)
                    .parameter(description)
                    .parameter(taxStatus?.code)
                    .toSingle { it.getInt(1) }
                    .doOnError { println(it) }
        } else {
            Single.just(-1)
        }.doOnSuccess { println(it) }

    companion object {

        /*
        fun fromChaseFile(path: String) = Account.allAsMap.map { accounts ->
                FileReader(path).let { CSVParser(it, CSVFormat.DEFAULT.withHeader()) }.let {
                    val result = it.asSequence().filter { it["Amount"].isNotBlank() }.map {
                        StagedTransaction(
                                transactionDate = it["Trans Date"]!!.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("MM/dd/yyyy")) },
                                description = it["Description"],
                                amount = it["Amount"].toCurrency(),
                                account = accounts[2]!!
                        )
                    }.toList()

                    it.close()
                    result.toObservable()
                }
        }.flatMapObservable { it }

        fun fromSwacuFile(path: String) = Account.allAsMap.map { accounts ->
            FileReader(path).let { CSVParser(it, CSVFormat.DEFAULT.withHeader()) }.let {
                val result = it.asSequence().filter { it["Amount"].isNotBlank() }
                        .map {
                            StagedTransaction(
                                    transactionDate = it["Date"]!!.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("M/d/yyyy")) },
                                    description = it["Description"],
                                    amount = it["Amount"].toCurrency(),
                                    account = accounts[1]!!
                            )
                        }.toList()

                it.close()
                result.toObservable()
            }
        }.flatMapObservable { it }
        */
    }


    fun predict() = PredictorModel.predict(this)
}

