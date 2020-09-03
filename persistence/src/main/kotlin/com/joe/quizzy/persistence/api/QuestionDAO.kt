package com.joe.quizzy.persistence.api

import com.joe.quizzy.api.models.NotificationType
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.User
import java.util.UUID
import java.util.stream.Stream

/**
 * DAO for managing Questions
 */
interface QuestionDAO {
    fun all(): List<Question>
    fun stream(): Stream<Question>
    fun get(id: UUID): Question?
    fun get(ids: List<UUID>): List<Question>
    fun save(thing: Question): Question
    fun active(): List<Question>
    fun active(user: User): List<Question>
    fun active(notificationType: NotificationType): List<Question>
    fun closed(): List<Question>
    fun closed(user: User): List<Question>
    fun closed(notificationType: NotificationType): List<Question>
    fun future(user: User): List<Question>
}
