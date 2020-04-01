package com.joe.quizzy.persistence.api

import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import java.util.UUID
import java.util.stream.Stream

/**
 * DAO for managing Things
 */
interface ResponseDAO {
    fun all(): List<Response>
    fun stream(): Stream<Response>
    fun get(id: UUID): Response?
    fun save(thing: Response): Response
    fun byUserQuestion(user: User, questionId: UUID): Response?
}
