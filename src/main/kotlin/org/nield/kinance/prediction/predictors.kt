package org.nield.kinance.prediction

import discretizeWords
import org.nield.kinance.entity.HardTransaction
import org.nield.kinance.entity.StagedTransaction
import org.nield.kinance.entity.TaxStatus
import org.nield.kotlinstatistics.toNaiveBayesClassifier

object PredictorModel {

    val categoryPredictor get() =  HardTransaction.all.toNaiveBayesClassifier(
            categorySelector = { it.category },
            featuresSelector = { it.memo.discretizeWords() }
    )


    val taxStatusPredictor get() = HardTransaction.all.toNaiveBayesClassifier(
            categorySelector = { it.taxStatus },
            featuresSelector = { it.memo.discretizeWords() },
            observationLimit = 500
    )

    fun predict(stagedTransaction: StagedTransaction) = with(stagedTransaction) {
        val discreteWords = description.discretizeWords()

        if (category == null)
            category = categoryPredictor.predictWithProbability(discreteWords)?.takeIf { it.probability > .2 }?.category

        taxStatus = taxStatusPredictor.predictWithProbability(discreteWords)?.takeIf { it.probability > .2 }?.category?: TaxStatus.NONE
    }

}