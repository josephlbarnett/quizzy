package com.joe.quizzy.graphql.auth

import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import io.dropwizard.auth.Authenticator
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import java.security.Principal
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

private val log = KotlinLogging.logger {}

class SessionAuthenticator
    @Inject
    constructor(
        private val sessionDAO: SessionDAO,
        private val userDAO: UserDAO,
    ) : Authenticator<String?, Principal> {
        override fun authenticate(credentials: String?): Optional<Principal> {
            val sessionId =
                try {
                    UUID.fromString(credentials)
                } catch (e: Exception) {
                    log.trace(e) { "Ignoring invalid session id $credentials" }
                    null
                }
            val session = sessionId?.let { sessionDAO.get(it) }
            val user = session?.let { userDAO.get(it.userId) }
            return user?.let {
                val now = OffsetDateTime.now()
                if (Duration.between(session.lastUsedAt, now).toHours() > 1) {
                    sessionDAO.save(session.copy(lastUsedAt = now))
                }
                Optional.of(UserPrincipal(it, session))
            } ?: Optional.empty()
        }
    }
