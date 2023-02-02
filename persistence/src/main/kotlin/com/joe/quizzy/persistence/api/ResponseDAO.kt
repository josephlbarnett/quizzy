package com.joe.quizzy.persistence.api

import com.joe.quizzy.api.models.Response
import java.time.OffsetDateTime
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
    fun byUserQuestion(userId: UUID, questionId: UUID): Response?
    fun byUserQuestions(userId: UUID, questionIds: List<UUID>): Map<UUID, Response>
    fun forInstance(
        instanceId: UUID,
        regrade: Boolean,
        startTime: OffsetDateTime? = null,
        endTime: OffsetDateTime? = null,
    ): List<Response>

    fun forUser(userId: UUID): List<Response>
}
