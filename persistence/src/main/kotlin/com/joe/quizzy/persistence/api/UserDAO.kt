package com.joe.quizzy.persistence.api

import com.joe.quizzy.api.models.User
import java.util.UUID
import java.util.stream.Stream

/**
 * DAO for managing Things
 */
interface UserDAO {
    fun all(): List<User>

    fun stream(): Stream<User>

    fun get(id: UUID): User?

    fun get(ids: List<UUID>): List<User>

    fun save(thing: User): User

    fun create(
        user: User,
        inviteCode: UUID,
        passwordHash: String,
    ): User?

    fun getByEmail(email: String): User?

    fun getByInstance(instanceId: UUID): List<User>

    fun savePassword(
        user: User,
        cryptedPass: String,
    ): Int
}
