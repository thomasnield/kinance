package org.nield.kinance.prediction

import discretizeWords
import io.reactivex.rxkotlin.Singles
import org.nield.kinance.entity.HardTransaction
import org.nield.kinance.entity.StagedTransaction
import org.nield.kinance.entity.TaxStatus
import org.nield.kotlinstatistics.toNaiveBayesClassifier
import switchReplaySingle

object PredictorModel {

    val categoryPredictor =  HardTransaction.all.toList()
            .map {
                it.toNaiveBayesClassifier(
                        categorySelector = { it.category },
                        featuresSelector = { it.memo.discretizeWords() }
                )
            }.switchReplaySingle()


    val taxStatusPredictor = HardTransaction.all.toList()
            .map {
                it.toNaiveBayesClassifier(
                        categorySelector = { it.taxStatus },
                        featuresSelector = { it.memo.discretizeWords() },
                        observationLimit = 500
                )
            }.switchReplaySingle()

    fun predict(stagedTransaction: StagedTransaction) = Singles.zip(categoryPredictor, taxStatusPredictor) { categoryModel, taxModel ->
        with(stagedTransaction) {
            val discreteWords = description.discretizeWords()

            if (category == null)
                category = categoryModel.predictWithProbability(discreteWords)?.takeIf { it.probability > .2 }?.category


            taxStatus = taxModel.predictWithProbability(discreteWords)?.takeIf { it.probability > .2 }?.category?: TaxStatus.NONE
        }
    }

}