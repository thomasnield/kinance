package org.nield.kinance.entity

import db
import getLocalDate
import org.nield.rxkotlinjdbc.select
import java.sql.ResultSet
import java.time.LocalDate

class Category(
        val id: Int,
        val name: String,
        val flowType: FlowType,
        val effFrom: LocalDate,
        val effTo: LocalDate
) {
    constructor(rs: ResultSet): this(
            rs.getInt("ID"),
            rs.getString("NAME"),
            rs.getString("FLOW_TYPE").let(FlowType::valueOf),
            rs.getLocalDate("EFF_FROM"),
            rs.getLocalDate("EFF_TO")
    )

    companion object {

        val all = db.select("SELECT * FROM CATEGORY")
                .toSequence(::Category)
                .toList()

        val allAsMap = all.asSequence().map { it.id to it }.toMap()

        fun forId(id: Int) = allAsMap[id]!!
    }

    override fun toString() = name
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Category

        if (id != other.id) return false

        return true
    }
    override fun hashCode(): Int {
        return id
    }

}

enum class FlowType {
    IN,
    OUT,
    TRANSFER;
}
