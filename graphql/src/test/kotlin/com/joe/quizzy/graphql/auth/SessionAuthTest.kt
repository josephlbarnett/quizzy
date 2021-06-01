package com.joe.quizzy.graphql.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isTrue
import com.joe.quizzy.api.models.Session
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.testing.LeakyMock
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.time.OffsetDateTime
import java.util.UUID

class SessionAuthTest {
    val user = User(UUID.randomUUID(), UUID.randomUUID(), "bill", "user", "password", false, "UTC")
    val session = Session(UUID.randomUUID(), user.id!!, OffsetDateTime.now(), OffsetDateTime.now())
    val oldSession =
        Session(UUID.randomUUID(), user.id!!, OffsetDateTime.now().minusHours(2), OffsetDateTime.now().minusHours(2))

    @Test
    fun testSessionAuth() {
        val sessionDAO = LeakyMock.mock<SessionDAO>()
        val userDAO = LeakyMock.mock<UserDAO>()
        val authenticator = SessionAuthenticator(sessionDAO, userDAO)
        EasyMock.expect(sessionDAO.get(session.id!!)).andReturn(session)
        EasyMock.expect(userDAO.get(session.userId)).andReturn(user)
        EasyMock.replay(sessionDAO, userDAO)
        val principal = authenticator.authenticate(session.id?.toString()).map { it as UserPrincipal }
        assertThat(principal.isPresent).isTrue()
        assertThat(principal.get().session).isEqualTo(session)
        assertThat(principal.get().user).isEqualTo(user)
        EasyMock.verify(sessionDAO, userDAO)
    }

    @Test
    fun testOldSessionAuth() {
        val sessionDAO = LeakyMock.mock<SessionDAO>()
        val userDAO = LeakyMock.mock<UserDAO>()
        val authenticator = SessionAuthenticator(sessionDAO, userDAO)
        EasyMock.expect(sessionDAO.get(oldSession.id!!)).andReturn(oldSession)
        EasyMock.expect(userDAO.get(oldSession.userId)).andReturn(user)
        val updateCapture = EasyMock.newCapture<Session>()
        EasyMock.expect(sessionDAO.save(EasyMock.capture(updateCapture) ?: oldSession)).andReturn(oldSession.copy())
        EasyMock.replay(sessionDAO, userDAO)
        val principal = authenticator.authenticate(oldSession.id?.toString()).map { it as UserPrincipal }
        assertThat(principal.isPresent).isTrue()
        assertThat(principal.get().session).isEqualTo(oldSession)
        assertThat(principal.get().user).isEqualTo(user)
        assertThat(updateCapture.value.id).isEqualTo(oldSession.id)
        assertThat(updateCapture.value.createdAt).isEqualTo(oldSession.createdAt)
        assertThat(updateCapture.value.lastUsedAt).isGreaterThanOrEqualTo(oldSession.lastUsedAt.plusHours(2))
        EasyMock.verify(sessionDAO, userDAO)
    }

    @Test
    fun testSessionWrongAuth() {
        val sessionDAO = LeakyMock.mock<SessionDAO>()
        val userDAO = LeakyMock.mock<UserDAO>()
        val authenticator = SessionAuthenticator(sessionDAO, userDAO)
        EasyMock.expect(sessionDAO.get(session.id!!)).andReturn(null)
        EasyMock.replay(sessionDAO, userDAO)
        val principal = authenticator.authenticate(session.id?.toString())
        assertThat(principal.isPresent).isFalse()
        assertThat(principal.isEmpty).isTrue()
        EasyMock.verify(sessionDAO, userDAO)
    }

    @Test
    fun testSessionBadIdValue() {
        val sessionDAO = LeakyMock.mock<SessionDAO>()
        val userDAO = LeakyMock.mock<UserDAO>()
        val authenticator = SessionAuthenticator(sessionDAO, userDAO)
        EasyMock.replay(sessionDAO, userDAO)
        val principal = authenticator.authenticate("cookie")
        assertThat(principal.isPresent).isFalse()
        assertThat(principal.isEmpty).isTrue()
        EasyMock.verify(sessionDAO, userDAO)
    }

    @Test
    fun testSessionNoUser() {
        val sessionDAO = LeakyMock.mock<SessionDAO>()
        val userDAO = LeakyMock.mock<UserDAO>()
        val authenticator = SessionAuthenticator(sessionDAO, userDAO)
        EasyMock.expect(sessionDAO.get(session.id!!)).andReturn(session)
        EasyMock.expect(userDAO.get(session.userId)).andReturn(null)
        EasyMock.replay(sessionDAO, userDAO)
        val principal = authenticator.authenticate(session.id?.toString())
        assertThat(principal.isPresent).isFalse()
        assertThat(principal.isEmpty).isTrue()
        EasyMock.verify(sessionDAO, userDAO)
    }
}
