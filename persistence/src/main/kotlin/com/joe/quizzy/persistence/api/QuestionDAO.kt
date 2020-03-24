package com.joe.quizzy.persistence.api

import com.joe.quizzy.api.models.Question
import java.util.UUID
import java.util.stream.Stream

/**
 * DAO for managing Things
 */
interface QuestionDAO {
    fun all(): List<Question>
    fun stream(): Stream<Question>
    fun get(id: UUID): Question?
    fun save(thing: Question): Question
}
