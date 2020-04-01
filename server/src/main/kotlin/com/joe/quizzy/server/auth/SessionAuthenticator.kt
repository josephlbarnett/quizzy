package com.joe.quizzy.server.auth

import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import io.dropwizard.auth.Authenticator
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID
import javax.inject.Inject

class SessionAuthenticator
@Inject constructor(
    private val sessionDAO: SessionDAO,
    private val userDAO: UserDAO
) : Authenticator<String?, UserPrincipal> {
    override fun authenticate(credentials: String?): Optional<UserPrincipal> {
        val sessionId = try {
            UUID.fromString(credentials)
        } catch (e: Exception) {
            null
        }
        val session = sessionId?.let { sessionDAO.get(it) }
        val user = session?.let { userDAO.get(it.userId) }
        return user?.let {
            val now = OffsetDateTime.now()
            if (Duration.between(session.lastUsedAt, now).toMinutes() > 60) {
                sessionDAO.save(session.copy(lastUsedAt = now))
            }
            Optional.of(UserPrincipal(it))
        } ?: Optional.empty()
    }
}
