package com.joe.quizzy.persistence.api

import com.joe.quizzy.api.models.Session
import java.util.UUID
import java.util.stream.Stream

/**
 * DAO for managing Things
 */
interface SessionDAO {
    fun all(): List<Session>
    fun stream(): Stream<Session>
    fun get(id: UUID): Session?
    fun save(thing: Session): Session
    fun delete(thing: Session): Int
}
