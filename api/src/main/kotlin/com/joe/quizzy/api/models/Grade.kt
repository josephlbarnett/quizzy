package com.joe.quizzy.api.models

import java.util.UUID

data class Grade(
    val id: UUID?,
    val responseId: UUID,
    val correct: Boolean?,
    val bonus: Int?
) {
    fun score(): Int {
        return if (correct == true) {
            15 + (bonus ?: 0)
        } else {
            0
        }
    }
}
