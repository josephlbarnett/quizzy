package com.joe.quizzy.graphql.models

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.joe.quizzy.api.models.Grade
import java.util.UUID

data class ApiGrade(
    val id: UUID?,
    val responseId: UUID,
    val correct: Boolean?,
    val bonus: Int?,
    @GraphQLIgnore
    private val defaultScore: Int,
) {
    constructor(grade: Grade, defaultScore: Int) : this(
        grade.id,
        grade.responseId,
        grade.correct,
        grade.bonus,
        defaultScore,
    )

    fun score(): Int {
        return if (correct == true) {
            defaultScore + (bonus ?: 0)
        } else {
            0
        }
    }
}
