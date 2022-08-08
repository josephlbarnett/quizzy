package com.joe.quizzy.api.models

import java.time.OffsetDateTime
import java.util.UUID

enum class QuestionType {
    SHORT_ANSWER,
    MULTIPLE_CHOICE
}

data class Question(
    val id: UUID?,
    val authorId: UUID,
    val body: String,
    val answer: String,
    val ruleReferences: String,
    val activeAt: OffsetDateTime,
    val closedAt: OffsetDateTime,
    val type: QuestionType = QuestionType.SHORT_ANSWER,
    val answerChoices: List<AnswerChoice>? = null
)
