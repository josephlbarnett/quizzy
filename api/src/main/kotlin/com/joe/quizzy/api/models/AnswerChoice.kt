package com.joe.quizzy.api.models

import java.util.UUID

data class AnswerChoice(
    val id: UUID?,
    val questionId: UUID?,
    val letter: String,
    val answer: String,
)
