package com.joe.quizzy.api.models

import java.util.UUID

private const val CORRECT_POINTS = 15

data class Grade(
    val id: UUID?,
    val responseId: UUID,
    val correct: Boolean?,
    val bonus: Int?
) {
    fun score(): Int {
        return if (correct == true) {
            CORRECT_POINTS + (bonus ?: 0)
        } else {
            0
        }
    }
}
