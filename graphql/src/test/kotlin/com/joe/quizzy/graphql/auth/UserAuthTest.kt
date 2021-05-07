package com.joe.quizzy.graphql.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.testing.LeakyMock
import io.dropwizard.auth.basic.BasicCredentials
import org.easymock.EasyMock
import org.testng.annotations.Test
import java.util.UUID

class UserAuthTest {
    val user = User(UUID.randomUUID(), UUID.randomUUID(), "bill", "user", "password", false, "UTC")

    @Test
    fun testAuthentication() {
        val mockDAO = LeakyMock.mock<UserDAO>()
        val mockHasher = LeakyMock.mock<Hasher>()
        EasyMock.expect(mockDAO.getByEmail("user")).andReturn(user)
        EasyMock.expect(mockHasher.verify("password", "password")).andReturn(true)
        EasyMock.replay(mockDAO, mockHasher)
        val userAuther = UserAuthenticator(mockDAO, mockHasher)
        val optUser = userAuther.authenticate(BasicCredentials("user", "password"))
        assertThat(optUser.isPresent).isTrue()
        assertThat(optUser.get().name).isEqualTo("bill")
        EasyMock.verify(mockDAO, mockHasher)
    }

    @Test
    fun testBadUser() {
        val mockDAO = LeakyMock.mock<UserDAO>()
        val mockHasher = LeakyMock.mock<Hasher>()
        EasyMock.expect(mockDAO.getByEmail(LeakyMock.anyString())).andReturn(null)
        EasyMock.replay(mockDAO, mockHasher)
        val userAuther = UserAuthenticator(mockDAO, mockHasher)
        val optUser = userAuther.authenticate(BasicCredentials("user", "password"))
        assertThat(optUser.isPresent).isFalse()
        assertThat(optUser.isEmpty).isTrue()
        EasyMock.verify(mockDAO, mockHasher)
    }

    @Test
    fun testBadPassword() {
        val mockDAO = LeakyMock.mock<UserDAO>()
        val mockHasher = LeakyMock.mock<Hasher>()
        EasyMock.expect(mockDAO.getByEmail("user")).andReturn(user)
        EasyMock.expect(mockHasher.verify(LeakyMock.anyString(), LeakyMock.anyString())).andReturn(false)
        EasyMock.replay(mockDAO, mockHasher)
        val userAuther = UserAuthenticator(mockDAO, mockHasher)
        val optUser = userAuther.authenticate(BasicCredentials("user", "password"))
        assertThat(optUser.isPresent).isFalse()
        assertThat(optUser.isEmpty).isTrue()
        EasyMock.verify(mockDAO, mockHasher)
    }

    @Test
    fun testAuthorizer() {
        val authorizer = UserAuthorizer()
        val principal = UserPrincipal(user, null)
        assertThat(authorizer.authorize(principal, "ADMIN", null)).isFalse()
        assertThat(authorizer.authorize(principal, "USER", null)).isTrue()
        val adminPrincipal = UserPrincipal(user.copy(admin = true), null)
        assertThat(authorizer.authorize(adminPrincipal, "ADMIN", null)).isTrue()
        assertThat(authorizer.authorize(adminPrincipal, "USER", null)).isTrue()
    }
}
