package com.joe.quizzy.server.graphql

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.model.Userinfo
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.Session
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.GradeDAO
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.joe.quizzy.server.auth.Hasher
import com.joe.quizzy.server.auth.UserAuthenticator
import com.joe.quizzy.server.auth.UserPrincipal
import com.joe.quizzy.server.mail.GmailService
import com.joe.quizzy.server.mail.GmailServiceFactory
import com.joe.quizzy.server.mail.persistedInstance
import com.trib3.config.ConfigLoader
import com.trib3.graphql.resources.GraphQLResourceContext
import com.trib3.server.config.TribeApplicationConfig
import com.trib3.testing.LeakyMock
import com.trib3.testing.mock
import org.easymock.EasyMock
import org.easymock.EasyMockSupport
import org.testng.annotations.Test
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.util.Properties
import java.util.UUID
import javax.mail.Message.RecipientType
import javax.mail.internet.MimeMessage

/**
 * Support class to set up a mock Mutation per test with access to various
 * user/session contexts.  Pass an [initBlock] to setup mock behavior via
 * [EasyMock.expect], and call [test] to run tests with assertions.
 * [EasyMock.replay] will be called on all mocks after [initBlock] executes,
 * and [EasyMock.verify] will be called on all mocks after [test] executes.
 */
class MockMutation(initBlock: MockMutation.() -> Unit) : EasyMockSupport() {
    val questionDAO: QuestionDAO = mock()
    val sessionDAO: SessionDAO = mock()
    val userDAO: UserDAO = mock()
    val responseDAO: ResponseDAO = mock()
    val gradeDAO: GradeDAO = mock()
    val hasher: Hasher = mock()
    val userAuthenticator = UserAuthenticator(userDAO, hasher)
    val instanceDAO: InstanceDAO = mock()
    val gmailServiceFactory: GmailServiceFactory = mock()
    val mutation = Mutation(
        questionDAO,
        sessionDAO,
        userDAO,
        responseDAO,
        gradeDAO,
        userAuthenticator,
        instanceDAO,
        gmailServiceFactory,
        TribeApplicationConfig(ConfigLoader())
    )
    val emptyContext = GraphQLResourceContext(null)
    val user = User(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "user",
        "user",
        "pass",
        false,
        "UTC"
    )
    val admin = User(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "admin",
        "admin@admin.com",
        "pass",
        true,
        "UTC"
    )
    val session = Session(
        UUID.randomUUID(),
        user.id!!,
        OffsetDateTime.now(),
        OffsetDateTime.now()
    )
    val userSessionContext = GraphQLResourceContext(UserPrincipal(user, session))
    val userNoSessionContext = GraphQLResourceContext(UserPrincipal(user, null))
    val adminContext = GraphQLResourceContext(UserPrincipal(admin, null))

    init {
        initBlock()
        replayAll()
    }

    fun test(block: MockMutation.() -> Unit) {
        block()
        verifyAll()
    }
}

class MutationTest {
    /**
     * Test that we can log in with the right user/pass and get a cookie set
     */
    @Test
    fun testLoginSuccess() {
        MockMutation {
            EasyMock.expect(userDAO.getByEmail("user")).andReturn(user)
            EasyMock.expect(hasher.verify("pass", "pass")).andReturn(true)
            EasyMock.expect(sessionDAO.save(EasyMock.anyObject() ?: session))
                .andReturn(session)
        }.test {
            assertThat(mutation.login(emptyContext, "user", "pass")).isTrue()
            assertThat(emptyContext.cookie?.name).isEqualTo("x-quizzy-session")
            assertThat(emptyContext.cookie?.value).isEqualTo(session.id.toString())
        }
    }

    /**
     * Test that logging in multiple times does nothing
     */
    @Test
    fun testLoginAlreadyLoggedIn() {
        MockMutation { }.test {
            assertThat(mutation.login(GraphQLResourceContext(UserPrincipal(user, null)), "notauser", "wrong")).isTrue()
            assertThat(
                mutation.login(
                    userNoSessionContext,
                    "alreadysetuser",
                    "whatever"
                )
            ).isTrue()
        }
    }

    /**
     * Test that we don't log in a user with the wrong password
     */
    @Test
    fun testLoginWrongPassword() {
        MockMutation {
            EasyMock.expect(userDAO.getByEmail("user")).andReturn(user)
            EasyMock.expect(hasher.verify("pass", "wrong")).andReturn(false)
        }.test {
            assertThat(mutation.login(emptyContext, "user", "wrong")).isFalse()
        }
    }

    /**
     * Test that we don't log in a non-existent user
     */
    @Test
    fun testLoginNoSuchUser() {
        MockMutation {
            EasyMock.expect(userDAO.getByEmail("notauser")).andReturn(null)
        }.test {
            assertThat(mutation.login(emptyContext, "notauser", "wrong")).isFalse()
        }
    }

    /**
     * Test that if the dao somehow returns a user with no id we don't log in
     */
    @Test
    fun testLoginInvalidUser() {
        MockMutation {
            EasyMock.expect(userDAO.getByEmail("invaliduser")).andReturn(user.copy(id = null))
            EasyMock.expect(hasher.verify("pass", "pass")).andReturn(true)
        }.test {
            assertThat(mutation.login(emptyContext, "invaliduser", "pass")).isFalse()
        }
    }

    /**
     * Test that logging out with a session deletes the session and expires the cookie
     */
    @Test
    fun testLogoutWithSession() {
        MockMutation {
            EasyMock.expect(sessionDAO.delete(session)).andReturn(1).once()
        }.test {
            assertThat(mutation.logout(userSessionContext)).isTrue()
            assertThat(userSessionContext.cookie?.expiry?.time).isEqualTo(0)
        }
    }

    /**
     * Test that logging out with a user but no session still expires the cookie
     */
    @Test
    fun testLogoutWithoutSession() {
        MockMutation { }.test {
            assertThat(mutation.logout(userNoSessionContext)).isTrue()
            assertThat(userNoSessionContext.cookie?.expiry?.time).isEqualTo(0)
        }
    }

    /**
     * Test that logging out a logged out context does nothing
     */
    @Test
    fun testLogoutNotLoggedIn() {
        MockMutation { }.test {
            assertThat(mutation.logout(emptyContext)).isFalse()
        }
    }

    /**
     * Test that changing password with correct old password
     */
    @Test
    fun testChangePassSuccess() {
        MockMutation {
            EasyMock.expect(userDAO.getByEmail("user")).andReturn(user)
            EasyMock.expect(hasher.verify("pass", "pass")).andReturn(true)
            EasyMock.expect(hasher.hash("newpass")).andReturn("newpass")
            EasyMock.expect(userDAO.savePassword(user, "newpass")).andReturn(1)
        }.test {
            assertThat(mutation.changePassword(userSessionContext, "pass", "newpass")).isTrue()
        }
    }

    /**
     * Test that changing password with wrong old password fails
     */
    @Test
    fun testChangePassWrongPassword() {
        MockMutation {
            EasyMock.expect(userDAO.getByEmail("user")).andReturn(user)
            EasyMock.expect(hasher.verify("pass", "notpass")).andReturn(false)
        }.test {
            assertThat(mutation.changePassword(userSessionContext, "notpass", "newpass")).isFalse()
        }
    }

    /**
     * Test that changing password without being logged in fails
     */
    @Test
    fun testChangePassNotLoggedIn() {
        MockMutation { }.test {
            assertThat(mutation.changePassword(emptyContext, "pass", "newpass")).isFalse()
        }
    }

    /**
     * Test that changing password with a context user that doesn't match dao returned user fails
     */
    @Test
    fun testChangePassUserIdMismatch() {
        MockMutation {
            EasyMock.expect(userDAO.getByEmail("user")).andReturn(user)
            EasyMock.expect(hasher.verify("pass", "pass")).andReturn(true)
        }.test {
            assertThat(
                mutation.changePassword(
                    GraphQLResourceContext(UserPrincipal(user.copy(id = UUID.randomUUID()), null)),
                    "pass",
                    "newpass"
                )
            ).isFalse()
        }
    }

    /**
     * Test that changing password with a context user that returns from the dao with no id fails
     */
    @Test
    fun testChangePassUserIdNull() {
        MockMutation {
            EasyMock.expect(userDAO.getByEmail("user2")).andReturn(user.copy(id = null, email = "user2"))
            EasyMock.expect(hasher.verify("pass", "pass")).andReturn(true)
        }.test {
            assertThat(
                mutation.changePassword(
                    GraphQLResourceContext(UserPrincipal(user.copy(email = "user2"), null)),
                    "pass",
                    "newpass"
                )
            ).isFalse()
        }
    }

    /**
     * Test that changing password with a context user that doesn't return from the dao
     */
    @Test
    fun testChangePassUserNull() {
        MockMutation {
            EasyMock.expect(userDAO.getByEmail("user3")).andReturn(null)
        }.test {
            assertThat(
                mutation.changePassword(
                    GraphQLResourceContext(UserPrincipal(user.copy(email = "user3"), null)),
                    "pass",
                    "newpass"
                )
            ).isFalse()
        }
    }

    @Test
    fun testRequestPasswordResetEmail() {
        val sentMessageCapture = EasyMock.newCapture<Message>()
        val generatedCodeCapture = EasyMock.newCapture<String>()
        MockMutation {
            EasyMock.expect(userDAO.getByEmail(user.email)).andReturn(user)
            EasyMock.expect(instanceDAO.get(user.instanceId)).andReturn(persistedInstance)
            EasyMock.expect(userAuthenticator.hasher.hash(EasyMock.capture(generatedCodeCapture) ?: ""))
                .andReturn("hashedCode")
            EasyMock.expect(userDAO.save(user.copy(passwordResetToken = "hashedCode")))
                .andReturn(user.copy(passwordResetToken = "hashedCode"))

            val gmsMock: GmailService = mock()
            val gmailMock: Gmail = mock()
            val usersMock: Gmail.Users = mock()
            val messagesMock: Gmail.Users.Messages = mock()
            val sendMock: Gmail.Users.Messages.Send = mock()

            val oauthMock: Oauth2 = mock()
            val userInfoMock: Oauth2.Userinfo = mock()
            val uiv2Mock: Oauth2.Userinfo.V2 = mock()
            val meMock: Oauth2.Userinfo.V2.Me = mock()
            val meGetMock: Oauth2.Userinfo.V2.Me.Get = mock()
            EasyMock.expect(gmailServiceFactory.getService(user.instanceId)).andReturn(gmsMock)
            EasyMock.expect(gmsMock.gmail).andReturn(gmailMock)
            EasyMock.expect(gmailMock.users()).andReturn(usersMock)
            EasyMock.expect(usersMock.messages()).andReturn(messagesMock)
            EasyMock.expect(messagesMock.send(EasyMock.anyString(), EasyMock.capture(sentMessageCapture)))
                .andReturn(sendMock)
            EasyMock.expect(sendMock.execute()).andReturn(Message())
            EasyMock.expect(gmsMock.oauth).andReturn(oauthMock)
            EasyMock.expect(oauthMock.userinfo()).andReturn(userInfoMock)
            EasyMock.expect(userInfoMock.v2()).andReturn(uiv2Mock)
            EasyMock.expect(uiv2Mock.me()).andReturn(meMock)
            EasyMock.expect(meMock.get()).andReturn(meGetMock)
            EasyMock.expect(meGetMock.execute()).andReturn(Userinfo().apply { email = admin.email })
        }.test {
            assertThat(
                mutation.requestPasswordReset(user.email)
            ).isTrue()
            val message = sentMessageCapture.value
            val mimeMessage = MimeMessage(
                javax.mail.Session.getDefaultInstance(Properties()),
                ByteArrayInputStream(message.decodeRaw())
            )
            assertThat(mimeMessage.getRecipients(RecipientType.TO).toList().map { it.toString() })
                .isEqualTo(listOf("user <user>"))
            assertThat(mimeMessage.from.toList().map { it.toString() })
                .isEqualTo(listOf("persisted <admin@admin.com>"))
            assertThat(mimeMessage.subject).isEqualTo("persisted Password Reset")
            assertThat(mimeMessage.content.toString()).contains("Hello user")
            assertThat(mimeMessage.content.toString())
                .contains("We received a request to reset your password for persisted.")
            assertThat(mimeMessage.content.toString())
                .contains(generatedCodeCapture.value)
            assertThat(mimeMessage.content.toString())
                .contains(
                    "https://localhost/app/assets#/passreset?" +
                        "code&#61;$generatedCodeCapture&amp;email&#61;user"
                )
        }
    }

    @Test
    fun testRequestPasswordResetNoUser() {
        MockMutation {
            EasyMock.expect(userDAO.getByEmail(user.email)).andReturn(null)
        }.test {
            assertThat(
                mutation.requestPasswordReset(user.email)
            ).isTrue()
        }
    }

    @Test
    fun testRequestPasswordResetNoGmailService() {
        MockMutation {
            EasyMock.expect(userDAO.getByEmail(user.email)).andReturn(user)
            EasyMock.expect(gmailServiceFactory.getService(user.instanceId)).andReturn(null)
        }.test {
            assertThat(
                mutation.requestPasswordReset(user.email)
            ).isTrue()
        }
    }

    @Test
    fun testCompletePasswordReset() {
        MockMutation {
            EasyMock.expect(hasher.hash("newpassword")).andReturn("newhash")
            EasyMock.expect(hasher.verify("secrethash", "secretcode")).andReturn(true)
            EasyMock.expect(userDAO.getByEmail(user.email))
                .andReturn(user.copy(passwordResetToken = "secrethash"))
            EasyMock.expect(userDAO.savePassword(user.copy(passwordResetToken = "secrethash"), "newhash"))
                .andReturn(1)
        }.test {
            assertThat(
                mutation.completePasswordReset(user.email, "secretcode", "newpassword")
            ).isTrue()
        }
    }

    @Test
    fun testCompletePasswordResetFailed() {
        MockMutation {
            EasyMock.expect(hasher.verify("secrethash", "wrongsecretcode")).andReturn(false)
            EasyMock.expect(userDAO.getByEmail(user.email))
                .andReturn(user.copy(passwordResetToken = "secrethash"))
        }.test {
            assertThat(
                mutation.completePasswordReset(user.email, "wrongsecretcode", "newpassword")
            ).isFalse()
        }
    }

    @Test
    fun testCompletePasswordResetNeverRequested() {
        MockMutation {
            EasyMock.expect(userDAO.getByEmail(user.email))
                .andReturn(user)
        }.test {
            assertThat(
                mutation.completePasswordReset(user.email, "norealcode", "newpassword")
            ).isFalse()
        }
    }

    @Test
    fun testCompletePasswordResetNoUser() {
        MockMutation {
            EasyMock.expect(userDAO.getByEmail(user.email))
                .andReturn(null)
        }.test {
            assertThat(
                mutation.completePasswordReset(user.email, "norealcode", "newpassword")
            ).isFalse()
        }
    }

    /**
     * Test that admins can add/edit questions
     */
    @Test
    fun testQuestionSaveByAdmin() {
        lateinit var question: Question
        MockMutation {
            question = Question(
                null,
                user.id!!,
                "a question",
                "an answer",
                "rule reference",
                OffsetDateTime.now(),
                OffsetDateTime.now()
            )
            EasyMock.expect(questionDAO.save(question)).andReturn(question.copy(id = UUID.randomUUID()))
        }.test {
            assertThat(
                mutation.question(
                    adminContext,
                    question
                )?.id
            ).isNotNull()
        }
    }

    /**
     * Test that non-admins cannot add/edit questions
     */
    @Test
    fun testQuestionSaveByNonAdmin() {
        MockMutation { }.test {
            assertThat(
                mutation.question(
                    userSessionContext,
                    Question(
                        null,
                        user.id!!,
                        "a question",
                        "an answer",
                        "rule reference",
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                    )
                )
            ).isNull()
        }
    }

    /**
     * Test that non-logged in cannot add/edit questions
     */
    @Test
    fun testQuestionSaveNotLoggedIn() {
        MockMutation { }.test {
            assertThat(
                mutation.question(
                    emptyContext,
                    Question(
                        null,
                        user.id!!,
                        "a question",
                        "an answer",
                        "rule reference",
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                    )
                )
            ).isNull()
        }
    }

    /**
     * Test that admins can add/edit grades
     */
    @Test
    fun testGradeSaveByAdmin() {
        lateinit var grade: Grade
        MockMutation {
            grade = Grade(
                null,
                UUID.randomUUID(),
                true,
                5
            )
            EasyMock.expect(gradeDAO.save(grade)).andReturn(grade.copy(id = UUID.randomUUID()))
        }.test {
            assertThat(
                mutation.grade(
                    adminContext,
                    grade
                )?.id
            ).isNotNull()
        }
    }

    /**
     * Test that non-admins cannot add/edit grades
     */
    @Test
    fun testGradeSaveByNonAdmin() {
        MockMutation { }.test {
            assertThat(
                mutation.grade(
                    userSessionContext,
                    Grade(
                        null,
                        UUID.randomUUID(),
                        true,
                        5
                    )
                )
            ).isNull()
        }
    }

    /**
     * Test that non-logged in cannot add/edit grades
     */
    @Test
    fun testGradeSaveNotLoggedIn() {
        MockMutation { }.test {
            assertThat(
                mutation.grade(
                    emptyContext,
                    Grade(
                        null,
                        UUID.randomUUID(),
                        true,
                        5
                    )
                )
            ).isNull()
        }
    }

    /**
     * Test that users can save responses and it will always be assigned to the context user
     */
    @Test
    fun testResponseSave() {
        lateinit var response: Response
        MockMutation {
            response = Response(
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "an answer",
                "with references"
            )
            EasyMock.expect(responseDAO.save(response.copy(userId = user.id!!)))
                .andReturn(response.copy(id = UUID.randomUUID(), userId = user.id!!))
        }.test {
            val savedResponse = mutation.response(
                userSessionContext,
                response
            )
            assertThat(savedResponse?.id).isNotNull()
            assertThat(savedResponse?.userId).isEqualTo(user.id)
        }
    }

    /**
     * Test that non-logged in cannot add/edit responses
     */
    @Test
    fun testResponseSaveNotLoggedIn() {
        MockMutation { }.test {
            assertThat(
                mutation.response(
                    emptyContext,
                    Response(
                        null,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "an answer",
                        "with references"
                    )
                )
            ).isNull()
        }
    }

    /**
     * Test that an admin can edit other users
     */
    @Test
    fun testUserEditOtherAsAdmin() {
        lateinit var userToEdit: User
        MockMutation {
            userToEdit = User(user.id, UUID.randomUUID(), "bill", "bill@gmail.com", "pass", false, "UTC")
            EasyMock.expect(userDAO.save(userToEdit)).andReturn(userToEdit).times(2)
        }.test {
            assertThat(
                mutation.user(
                    adminContext,
                    userToEdit
                )
            ).isEqualTo(userToEdit)
            assertThat(
                mutation.users(
                    adminContext,
                    listOf(userToEdit)
                )
            ).isEqualTo(listOf(userToEdit))
        }
    }

    /**
     * Test that an admin can create other users
     */
    @Test
    fun testUserCreateOtherAsAdminWithNoEmail() {
        lateinit var userToEdit: User
        MockMutation {
            userToEdit = User(null, user.instanceId, "bill", "bill@gmail.com", "pass", false, "UTC")
            EasyMock.expect(hasher.hash(LeakyMock.anyString())).andReturn("hashedsecret")
            EasyMock.expect(userDAO.save(userToEdit.copy(authCrypt = "hashedsecret")))
                .andReturn(
                    userToEdit.copy(
                        id = UUID.fromString("9eed42c1-4469-4b36-8417-e7e35fe45bd5"),
                        authCrypt = "hashedsecret"
                    )
                )
            EasyMock.expect(gmailServiceFactory.getService(admin.instanceId)).andReturn(null)
        }.test {
            assertThat(
                mutation.user(
                    adminContext,
                    userToEdit
                )
            ).isEqualTo(
                userToEdit.copy(
                    id = UUID.fromString("9eed42c1-4469-4b36-8417-e7e35fe45bd5"),
                    authCrypt = "hashedsecret"
                )
            )
        }
    }

    /**
     * Test that an admin can create other users and send email
     */
    @Test
    fun testUserCreateOtherAsAdminWithWelcomeEmail() {
        lateinit var userToEdit: User
        val sentMessageCapture = EasyMock.newCapture<Message>()
        MockMutation {
            userToEdit = User(null, user.instanceId, "bill", "bill@gmail.com", "pass", false, "UTC")
            EasyMock.expect(hasher.hash(LeakyMock.anyString())).andReturn("hashedsecret")
            EasyMock.expect(userDAO.save(userToEdit.copy(authCrypt = "hashedsecret")))
                .andReturn(
                    userToEdit.copy(
                        id = UUID.fromString("9eed42c1-4469-4b36-8417-e7e35fe45bd5"),
                        authCrypt = "hashedsecret"
                    )
                )
            val gmsMock: GmailService = mock()
            val gmailMock: Gmail = mock()
            val usersMock: Gmail.Users = mock()
            val messagesMock: Gmail.Users.Messages = mock()
            val sendMock: Gmail.Users.Messages.Send = mock()

            val oauthMock: Oauth2 = mock()
            val userInfoMock: Oauth2.Userinfo = mock()
            val uiv2Mock: Oauth2.Userinfo.V2 = mock()
            val meMock: Oauth2.Userinfo.V2.Me = mock()
            val meGetMock: Oauth2.Userinfo.V2.Me.Get = mock()
            EasyMock.expect(gmailServiceFactory.getService(admin.instanceId)).andReturn(gmsMock)
            EasyMock.expect(gmsMock.gmail).andReturn(gmailMock)
            EasyMock.expect(gmailMock.users()).andReturn(usersMock)
            EasyMock.expect(usersMock.messages()).andReturn(messagesMock)
            EasyMock.expect(messagesMock.send(EasyMock.anyString(), EasyMock.capture(sentMessageCapture)))
                .andReturn(sendMock)
            EasyMock.expect(sendMock.execute()).andReturn(Message())
            EasyMock.expect(gmsMock.oauth).andReturn(oauthMock)
            EasyMock.expect(oauthMock.userinfo()).andReturn(userInfoMock)
            EasyMock.expect(userInfoMock.v2()).andReturn(uiv2Mock)
            EasyMock.expect(uiv2Mock.me()).andReturn(meMock)
            EasyMock.expect(meMock.get()).andReturn(meGetMock)
            EasyMock.expect(meGetMock.execute()).andReturn(Userinfo().apply { email = admin.email })
            EasyMock.expect(instanceDAO.get(admin.instanceId))
                .andReturn(Instance(admin.instanceId, "Instance Name", "ACTIVE"))
        }.test {
            assertThat(
                mutation.user(
                    adminContext,
                    userToEdit
                )
            ).isEqualTo(
                userToEdit.copy(
                    id = UUID.fromString("9eed42c1-4469-4b36-8417-e7e35fe45bd5"),
                    authCrypt = "hashedsecret"
                )
            )
            val message = sentMessageCapture.value
            val mimeMessage = MimeMessage(
                javax.mail.Session.getDefaultInstance(Properties()),
                ByteArrayInputStream(message.decodeRaw())
            )
            assertThat(mimeMessage.getRecipients(RecipientType.TO).toList().map { it.toString() })
                .isEqualTo(listOf("bill <bill@gmail.com>"))
            assertThat(mimeMessage.from.toList().map { it.toString() })
                .isEqualTo(listOf("Instance Name <admin@admin.com>"))
            assertThat(mimeMessage.subject).isEqualTo("Welcome to Instance Name")
            assertThat(mimeMessage.content.toString()).contains("Welcome bill")
            assertThat(mimeMessage.content.toString())
                .contains("admin has invited you to participate in Instance Name")
            assertThat(mimeMessage.content.toString()).contains("bill@gmail.com")
            assertThat(mimeMessage.content.toString()).contains("<b>Password</b>: ")
        }
    }

    /**
     * Test that a non-admin can edit themselves
     */
    @Test
    fun testUserEditSelfNotAdmin() {
        lateinit var userToEdit: User
        MockMutation {
            userToEdit = User(user.id, UUID.randomUUID(), "bill", "bill@gmail.com", "pass", false, "UTC")
            EasyMock.expect(userDAO.save(userToEdit)).andReturn(userToEdit).times(2)
        }.test {
            assertThat(
                mutation.user(
                    userSessionContext,
                    userToEdit
                )
            ).isEqualTo(userToEdit)
            assertThat(
                mutation.users(
                    userSessionContext,
                    listOf(userToEdit)
                )
            ).isEqualTo(listOf(userToEdit))
        }
    }

    /**
     * Test that a non-admin cannot edit other users
     */
    @Test
    fun testUserSaveNotAdmin() {
        MockMutation {}.test {
            assertThat(
                mutation.user(
                    userSessionContext,
                    User(null, UUID.randomUUID(), "bill", "bill@gmail.com", "pass", false, "UTC")
                )
            ).isNull()
            assertThat(
                mutation.users(
                    userSessionContext,
                    listOf(User(null, UUID.randomUUID(), "bill", "bill@gmail.com", "pass", false, "UTC"))
                )
            ).isEqualTo(listOf(null))
        }
    }

    /**
     * Test that a non-logged-in cannot edit users
     */
    @Test
    fun testUserSaveNotLoggedIn() {
        MockMutation {}.test {
            assertThat(
                mutation.user(
                    emptyContext,
                    User(null, UUID.randomUUID(), "bill", "bill@gmail.com", "pass", false, "UTC")
                )
            ).isNull()
            assertThat(
                mutation.users(
                    emptyContext,
                    listOf(User(null, UUID.randomUUID(), "bill", "bill@gmail.com", "pass", false, "UTC"))
                )
            ).isEqualTo(listOf(null))
        }
    }
}
