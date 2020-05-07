package com.joe.quizzy.persistence.api

import com.joe.quizzy.api.models.Grade
import java.util.UUID
import java.util.stream.Stream

/**
 * DAO for managing Things
 */
interface GradeDAO {
    fun all(): List<Grade>
    fun stream(): Stream<Grade>
    fun get(id: UUID): Grade?
    fun save(thing: Grade): Grade
    fun forUser(userId: UUID): List<Grade>
    fun forUsers(userIds: List<UUID>): Map<UUID, List<Grade>>
    fun forResponse(responseId: UUID): Grade?
    fun forResponses(responseIds: List<UUID>): Map<UUID, Grade>
}
