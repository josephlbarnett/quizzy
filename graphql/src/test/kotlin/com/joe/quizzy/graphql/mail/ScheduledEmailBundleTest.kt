package com.joe.quizzy.graphql.mail

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.model.Userinfo
import com.joe.quizzy.api.models.AnswerChoice
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.NotificationType
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.QuestionType
import com.joe.quizzy.api.models.User
import com.joe.quizzy.graphql.groupme.GroupMeService
import com.joe.quizzy.graphql.groupme.GroupMeServiceFactory
import com.joe.quizzy.persistence.api.EmailNotificationDAO
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.config.ConfigLoader
import com.trib3.server.config.TribeApplicationConfig
import com.trib3.testing.LeakyMock
import com.trib3.testing.mock
import com.trib3.testing.niceMock
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.easymock.EasyMock
import org.easymock.EasyMockSupport
import org.testng.annotations.Test
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.util.Properties
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import javax.mail.Session
import javax.mail.internet.MimeMessage

class ScheduledEmailBundleTest {
    @Test
    fun testPollingLoopEnabledByConfig() = runBlocking<Unit> {
        val support = EasyMockSupport()
        val mockQuestionDAO = support.mock<QuestionDAO>()
        val mockInstanceDAO = support.mock<InstanceDAO>()
        val mockUserDAO = support.mock<UserDAO>()
        val mockEmailNotificationDAO = support.mock<EmailNotificationDAO>()
        val mockGmailServiceFactory = support.mock<GmailServiceFactory>()
        val mockGroupMeServiceFactory = support.mock<GroupMeServiceFactory>()
        val threadPool = Executors.newSingleThreadExecutor()
        val latch = CountDownLatch(1)
        val client = HttpClient(MockEngine) { engine { addHandler { respond("pong") } } }
        val bundle = ScheduledEmailBundle(
            ConfigLoader("test_override_send_email"),
            TribeApplicationConfig(ConfigLoader()),
            mockQuestionDAO,
            mockUserDAO,
            mockInstanceDAO,
            mockEmailNotificationDAO,
            mockGmailServiceFactory,
            mockGroupMeServiceFactory,
            client,
            threadPool.asCoroutineDispatcher(),
            1,
        )
        EasyMock.expect(mockQuestionDAO.active(NotificationType.REMINDER)).andAnswer {
            latch.countDown()
            listOf()
        }.once()
        EasyMock.expect(mockQuestionDAO.closed(NotificationType.ANSWER)).andReturn(listOf()).once()
        support.replayAll()
        launch {
            assertThat(client.isActive).isTrue()
            latch.await()
            bundle.pollJob?.cancel()
        }
        bundle.run(null, null)
        bundle.pollJob?.join()
        assertThat(threadPool.isShutdown).isTrue()
        support.verifyAll()
    }

    @Test
    fun testInjectConstructorAndPollingLoopDisabledByConfig() {
        val support = EasyMockSupport()
        val mockQuestionDAO = support.mock<QuestionDAO>()
        val mockInstanceDAO = support.mock<InstanceDAO>()
        val mockEmailNotificationDAO = support.mock<EmailNotificationDAO>()
        val mockUserDAO = support.mock<UserDAO>()
        val mockGmailServiceFactory = support.mock<GmailServiceFactory>()
        val mockGroupMeServiceFactory = support.mock<GroupMeServiceFactory>()
        val client = HttpClient(MockEngine) { engine { addHandler { respond("pong") } } }
        val bundle = ScheduledEmailBundle(
            ConfigLoader(),
            TribeApplicationConfig(ConfigLoader()),
            mockQuestionDAO,
            mockUserDAO,
            mockInstanceDAO,
            mockEmailNotificationDAO,
            mockGmailServiceFactory,
            mockGroupMeServiceFactory,
            client,
        )
        support.replayAll()
        bundle.run(null, null)
        assertThat(bundle.pollJob).isNull()
        assertThat(bundle.configLoader).isNotNull()
        assertThat(bundle.appConfig).isNotNull()
        assertThat(bundle.questionDAO).isNotNull()
        assertThat(bundle.userDAO).isNotNull()
        assertThat(bundle.instanceDAO).isNotNull()
        assertThat(bundle.emailNotificationDAO).isNotNull()
        assertThat(bundle.gmailServiceFactory).isNotNull()
        assertThat(bundle.groupMeServiceFactory).isNotNull()
        assertThat(bundle.client).isEqualTo(client)
        assertThat(bundle.dispatcher).isInstanceOf(ExecutorCoroutineDispatcher::class)
        var ran = false
        var tname = ""
        bundle.dispatcher.executor.execute {
            tname = Thread.currentThread().name
            ran = true
        }
        assertThat(bundle.minuteMod).isEqualTo(5)
        support.verifyAll()
        bundle.dispatcher.close()
        while (!ran) {
            Thread.sleep(10)
        }
        assertThat(tname).isEqualTo("ScheduledEmailBundle")
    }

    @Test
    fun testNoEmail() = runBlocking {
        val support = EasyMockSupport()
        val mockQuestionDAO = support.mock<QuestionDAO>()
        val mockInstanceDAO = support.mock<InstanceDAO>()
        val mockUserDAO = support.mock<UserDAO>()
        val mockEmailNotificationDAO = support.mock<EmailNotificationDAO>()
        val mockGmailServiceFactory = support.mock<GmailServiceFactory>()
        val mockGroupMeServiceFactory = support.mock<GroupMeServiceFactory>()
        val threadPool = Executors.newSingleThreadExecutor()
        val client = HttpClient(MockEngine) { engine { addHandler { respond("pong") } } }
        val bundle = ScheduledEmailBundle(
            ConfigLoader(),
            TribeApplicationConfig(ConfigLoader()),
            mockQuestionDAO,
            mockUserDAO,
            mockInstanceDAO,
            mockEmailNotificationDAO,
            mockGmailServiceFactory,
            mockGroupMeServiceFactory,
            client,
            threadPool.asCoroutineDispatcher(),
            1,
        )
        val authorId = UUID.randomUUID()
        val instanceId = UUID.randomUUID()
        EasyMock.expect(mockGroupMeServiceFactory.create(EasyMock.anyObject() ?: UUID.randomUUID()))
            .andReturn(null).once()
        EasyMock.expect(mockQuestionDAO.closed(NotificationType.ANSWER)).andReturn(
            listOf(
                Question(
                    UUID.randomUUID(),
                    authorId,
                    "q2",
                    "a2",
                    "r2",
                    OffsetDateTime.now().minusDays(2),
                    OffsetDateTime.now().minusHours(1),
                ),
            ),
        ).once()
        EasyMock.expect(mockQuestionDAO.active(NotificationType.REMINDER)).andReturn(
            listOf(
                Question(
                    UUID.randomUUID(),
                    authorId,
                    "q4",
                    "a4",
                    "r4",
                    OffsetDateTime.now().minusDays(1),
                    OffsetDateTime.now().plusHours(1),
                ),
            ),
        ).once()
        EasyMock.expect(mockUserDAO.get(LeakyMock.anyObject<List<UUID>>())).andReturn(
            listOf(
                User(
                    authorId,
                    instanceId,
                    "Joe",
                    "joe@joe.com",
                    "",
                    true,
                    "",
                    false,
                ),
            ),
        ).once()
        EasyMock.expect(mockUserDAO.getByInstance(instanceId)).andReturn(
            listOf(
                User(
                    authorId,
                    instanceId,
                    "Joe",
                    "joe@joe.com",
                    "",
                    true,
                    "",
                    false,
                ),
                User(
                    UUID.randomUUID(),
                    instanceId,
                    "jim",
                    "jim@jim.com",
                    "",
                    false,
                    "",
                    true,
                ),
            ),
        ).once()
        EasyMock.expect(mockGmailServiceFactory.getService(instanceId)).andReturn(null).once()
        support.replayAll()
        bundle.sendEmails(OffsetDateTime.now())
        support.verifyAll()
        threadPool.shutdown()
    }

    @Test
    fun testFullEmail() = runBlocking {
        val support = EasyMockSupport()
        val mockQuestionDAO = support.mock<QuestionDAO>()
        val mockInstanceDAO = support.mock<InstanceDAO>()
        val mockUserDAO = support.mock<UserDAO>()
        val mockEmailNotificationDAO = support.mock<EmailNotificationDAO>()
        val mockGmailServiceFactory = support.mock<GmailServiceFactory>()
        val mockGroupMeServiceFactory = support.mock<GroupMeServiceFactory>()
        val mockGroupMeService = support.niceMock<GroupMeService>()
        val threadPool = Executors.newSingleThreadExecutor()
        val client = HttpClient(MockEngine) { engine { addHandler { respond("pong") } } }
        val bundle = ScheduledEmailBundle(
            ConfigLoader(),
            TribeApplicationConfig(ConfigLoader()),
            mockQuestionDAO,
            mockUserDAO,
            mockInstanceDAO,
            mockEmailNotificationDAO,
            mockGmailServiceFactory,
            mockGroupMeServiceFactory,
            client,
            threadPool.asCoroutineDispatcher(),
            1,
        )
        val authorId = UUID.randomUUID()
        val instanceId = UUID.randomUUID()
        val closedQuestions = listOf(
            Question(
                UUID.randomUUID(),
                authorId,
                "q1",
                "a1",
                "r1",
                OffsetDateTime.now().minusDays(2),
                OffsetDateTime.now().minusHours(1),
            ),
            Question(
                UUID.randomUUID(),
                authorId,
                "q2",
                "A",
                "r2",
                OffsetDateTime.now().minusDays(2),
                OffsetDateTime.now().minusHours(1),
                QuestionType.MULTIPLE_CHOICE,
                listOf(
                    AnswerChoice(UUID.randomUUID(), UUID.randomUUID(), "A", "First Choice"),
                    AnswerChoice(UUID.randomUUID(), UUID.randomUUID(), "B", "Second Choice"),
                ),
            ),
        )
        EasyMock.expect(mockQuestionDAO.closed(NotificationType.ANSWER)).andReturn(
            closedQuestions,
        ).once()
        EasyMock.expect(mockGroupMeServiceFactory.create(EasyMock.anyObject() ?: UUID.randomUUID()))
            .andReturn(mockGroupMeService)
        val activeQuestions = listOf(
            Question(
                UUID.randomUUID(),
                authorId,
                "q3",
                "a3",
                "r3",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusHours(1),
            ),
            Question(
                UUID.randomUUID(),
                authorId,
                "q4",
                "B",
                "r4",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusHours(1),
                QuestionType.MULTIPLE_CHOICE,
                listOf(
                    AnswerChoice(UUID.randomUUID(), UUID.randomUUID(), "A", "Letter A"),
                    AnswerChoice(UUID.randomUUID(), UUID.randomUUID(), "B", "Letter B"),
                ),
            ),
        )
        EasyMock.expect(mockQuestionDAO.active(NotificationType.REMINDER)).andReturn(
            activeQuestions,
        ).once()
        EasyMock.expect(mockUserDAO.get(LeakyMock.anyObject<List<UUID>>())).andReturn(
            listOf(
                User(
                    authorId,
                    instanceId,
                    "Joe",
                    "joe@joe.com",
                    "",
                    true,
                    "",
                    false,
                ),
            ),
        ).once()
        EasyMock.expect(mockUserDAO.getByInstance(instanceId)).andReturn(
            listOf(
                User(
                    authorId,
                    instanceId,
                    "Joe",
                    "joe@joe.com",
                    "",
                    true,
                    "",
                    false,
                ),
                User(
                    UUID.randomUUID(),
                    instanceId,
                    "jim",
                    "jim@jim.com",
                    "",
                    false,
                    "",
                    true,
                ),
            ),
        ).once()
        val sentMessageCapture = EasyMock.newCapture<Message>()
        val gmsMock: GmailService = support.mock()
        val gmailMock: Gmail = support.mock()
        val usersMock: Gmail.Users = support.mock()
        val messagesMock: Gmail.Users.Messages = support.mock()
        val sendMock: Gmail.Users.Messages.Send = support.mock()

        val oauthMock: Oauth2 = support.mock()
        val userInfoMock: Oauth2.Userinfo = support.mock()
        val uiv2Mock: Oauth2.Userinfo.V2 = support.mock()
        val meMock: Oauth2.Userinfo.V2.Me = support.mock()
        val meGetMock: Oauth2.Userinfo.V2.Me.Get = support.mock()
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
        EasyMock.expect(meGetMock.execute()).andReturn(Userinfo().apply { email = "admin@gmail.com" })
        EasyMock.expect(mockInstanceDAO.get(instanceId))
            .andReturn(Instance(instanceId, "Instance Name", "ACTIVE"))
        EasyMock.expect(mockGmailServiceFactory.getService(instanceId)).andReturn(gmsMock).once()
        EasyMock.expect(
            mockEmailNotificationDAO.markNotified(
                NotificationType.REMINDER,
                (activeQuestions + closedQuestions).mapNotNull { it.id },
            ),
        )
        EasyMock.expect(
            mockEmailNotificationDAO.markNotified(
                NotificationType.ANSWER,
                closedQuestions.mapNotNull { it.id },
            ),
        )

        support.replayAll()
        bundle.sendEmails(OffsetDateTime.now())
        support.verifyAll()
        threadPool.shutdown()

        val message = sentMessageCapture.value
        val mimeMessage = MimeMessage(
            Session.getDefaultInstance(Properties()),
            ByteArrayInputStream(message.decodeRaw()),
        )
        assertThat(mimeMessage.getRecipients(javax.mail.Message.RecipientType.BCC).toList().map { it.toString() })
            .isEqualTo(listOf("jim <jim@jim.com>"))
        assertThat(mimeMessage.from.toList().map { it.toString() })
            .isEqualTo(listOf("Instance Name <admin@gmail.com>"))
        assertThat(mimeMessage.subject).isEqualTo("New Questions and Answers Available from Instance Name")
        val bodyContent = mimeMessage.content
        assertThat(bodyContent.toString()).contains("2 new questions")
        assertThat(bodyContent.toString()).contains("q3")
        assertThat(bodyContent.toString()).doesNotContain("a3")
        assertThat(bodyContent.toString()).doesNotContain("r3")
        assertThat(bodyContent.toString()).contains("q4")
        assertThat(bodyContent.toString()).doesNotContain("a4")
        assertThat(bodyContent.toString()).doesNotContain("r4")
        assertThat(bodyContent.toString()).contains("A: Letter A")
        assertThat(bodyContent.toString()).contains("B: Letter B")
        assertThat(bodyContent.toString()).contains("new answers")
        assertThat(bodyContent.toString()).contains("q1")
        assertThat(bodyContent.toString()).contains("a1")
        assertThat(bodyContent.toString()).contains("r1")
        assertThat(bodyContent.toString()).contains("q2")
        assertThat(bodyContent.toString()).contains("A: First Choice")
        assertThat(bodyContent.toString()).contains("r2")
    }

    @Test
    fun testSingleQuestionEmail() = runBlocking {
        val support = EasyMockSupport()
        val mockQuestionDAO = support.mock<QuestionDAO>()
        val mockInstanceDAO = support.mock<InstanceDAO>()
        val mockUserDAO = support.mock<UserDAO>()
        val mockEmailNotificationDAO = support.mock<EmailNotificationDAO>()
        val mockGmailServiceFactory = support.mock<GmailServiceFactory>()
        val mockGroupMeServiceFactory = support.mock<GroupMeServiceFactory>()
        val mockGroupMeService = support.niceMock<GroupMeService>()
        val threadPool = Executors.newSingleThreadExecutor()
        val client = HttpClient(MockEngine) { engine { addHandler { respond("pong") } } }
        val bundle = ScheduledEmailBundle(
            ConfigLoader(),
            TribeApplicationConfig(ConfigLoader()),
            mockQuestionDAO,
            mockUserDAO,
            mockInstanceDAO,
            mockEmailNotificationDAO,
            mockGmailServiceFactory,
            mockGroupMeServiceFactory,
            client,
            threadPool.asCoroutineDispatcher(),
            1,
        )
        val authorId = UUID.randomUUID()
        val instanceId = UUID.randomUUID()
        val closedQuestions = listOf<Question>()
        EasyMock.expect(mockQuestionDAO.closed(NotificationType.ANSWER)).andReturn(
            closedQuestions,
        ).once()
        EasyMock.expect(mockGroupMeServiceFactory.create(EasyMock.anyObject() ?: UUID.randomUUID()))
            .andReturn(mockGroupMeService)
        val activeQuestions = listOf(
            Question(
                UUID.randomUUID(),
                authorId,
                "q4",
                "a4",
                "r4",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusHours(1),
            ),
        )
        EasyMock.expect(mockQuestionDAO.active(NotificationType.REMINDER)).andReturn(
            activeQuestions,
        ).once()
        EasyMock.expect(mockUserDAO.get(LeakyMock.anyObject<List<UUID>>())).andReturn(
            listOf(
                User(
                    authorId,
                    instanceId,
                    "Joe",
                    "joe@joe.com",
                    "",
                    true,
                    "",
                    false,
                ),
            ),
        ).once()
        EasyMock.expect(mockUserDAO.getByInstance(instanceId)).andReturn(
            listOf(
                User(
                    authorId,
                    instanceId,
                    "Joe",
                    "joe@joe.com",
                    "",
                    true,
                    "",
                    false,
                ),
                User(
                    UUID.randomUUID(),
                    instanceId,
                    "jim",
                    "jim@jim.com",
                    "",
                    false,
                    "",
                    true,
                ),
            ),
        ).once()
        val sentMessageCapture = EasyMock.newCapture<Message>()
        val gmsMock: GmailService = support.mock()
        val gmailMock: Gmail = support.mock()
        val usersMock: Gmail.Users = support.mock()
        val messagesMock: Gmail.Users.Messages = support.mock()
        val sendMock: Gmail.Users.Messages.Send = support.mock()

        val oauthMock: Oauth2 = support.mock()
        val userInfoMock: Oauth2.Userinfo = support.mock()
        val uiv2Mock: Oauth2.Userinfo.V2 = support.mock()
        val meMock: Oauth2.Userinfo.V2.Me = support.mock()
        val meGetMock: Oauth2.Userinfo.V2.Me.Get = support.mock()
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
        EasyMock.expect(meGetMock.execute()).andReturn(Userinfo().apply { email = "admin@gmail.com" })
        EasyMock.expect(mockInstanceDAO.get(instanceId))
            .andReturn(Instance(instanceId, "Instance Name", "ACTIVE"))
        EasyMock.expect(mockGmailServiceFactory.getService(instanceId)).andReturn(gmsMock).once()
        EasyMock.expect(
            mockEmailNotificationDAO.markNotified(
                NotificationType.REMINDER,
                (activeQuestions + closedQuestions).mapNotNull { it.id },
            ),
        )
        EasyMock.expect(
            mockEmailNotificationDAO.markNotified(
                NotificationType.ANSWER,
                closedQuestions.mapNotNull { it.id },
            ),
        )
        support.replayAll()
        bundle.sendEmails(OffsetDateTime.now())
        support.verifyAll()
        threadPool.shutdown()

        val message = sentMessageCapture.value
        val mimeMessage = MimeMessage(
            Session.getDefaultInstance(Properties()),
            ByteArrayInputStream(message.decodeRaw()),
        )
        assertThat(mimeMessage.getRecipients(javax.mail.Message.RecipientType.BCC).toList().map { it.toString() })
            .isEqualTo(listOf("jim <jim@jim.com>"))
        assertThat(mimeMessage.from.toList().map { it.toString() })
            .isEqualTo(listOf("Instance Name <admin@gmail.com>"))
        assertThat(mimeMessage.subject).isEqualTo("New Question Available from Instance Name")
        val bodyContent = mimeMessage.content
        assertThat(bodyContent.toString()).contains("1 new question")
        assertThat(bodyContent.toString()).contains("q4")
        assertThat(bodyContent.toString()).doesNotContain("a4")
        assertThat(bodyContent.toString()).doesNotContain("r4")
        assertThat(bodyContent.toString()).doesNotContain("new answer")
        assertThat(bodyContent.toString()).doesNotContain("q1")
        assertThat(bodyContent.toString()).doesNotContain("q2")
        assertThat(bodyContent.toString()).doesNotContain("q3")
    }

    @Test
    fun testSingleAnswerEmail() = runBlocking {
        val support = EasyMockSupport()
        val mockQuestionDAO = support.mock<QuestionDAO>()
        val mockInstanceDAO = support.mock<InstanceDAO>()
        val mockEmailNotificationDAO = support.mock<EmailNotificationDAO>()
        val mockUserDAO = support.mock<UserDAO>()
        val mockGmailServiceFactory = support.mock<GmailServiceFactory>()
        val mockGroupMeServiceFactory = support.mock<GroupMeServiceFactory>()
        val mockGroupMeService = support.niceMock<GroupMeService>()
        val threadPool = Executors.newSingleThreadExecutor()
        val client = HttpClient(MockEngine) { engine { addHandler { respond("pong") } } }
        val bundle = ScheduledEmailBundle(
            ConfigLoader(),
            TribeApplicationConfig(ConfigLoader()),
            mockQuestionDAO,
            mockUserDAO,
            mockInstanceDAO,
            mockEmailNotificationDAO,
            mockGmailServiceFactory,
            mockGroupMeServiceFactory,
            client,
            threadPool.asCoroutineDispatcher(),
            1,
        )
        val authorId = UUID.randomUUID()
        val instanceId = UUID.randomUUID()
        val closedQuestions = listOf(
            Question(
                UUID.randomUUID(),
                authorId,
                "q1",
                "a1",
                "r1",
                OffsetDateTime.now().minusDays(2),
                OffsetDateTime.now().minusHours(1),
            ),
        )
        EasyMock.expect(mockQuestionDAO.closed(NotificationType.ANSWER)).andReturn(
            closedQuestions,
        ).once()
        EasyMock.expect(mockGroupMeServiceFactory.create(EasyMock.anyObject() ?: UUID.randomUUID()))
            .andReturn(mockGroupMeService)
        val activeQuestions = listOf<Question>()
        EasyMock.expect(mockQuestionDAO.active(NotificationType.REMINDER)).andReturn(
            activeQuestions,
        ).once()
        EasyMock.expect(mockUserDAO.get(LeakyMock.anyObject<List<UUID>>())).andReturn(
            listOf(
                User(
                    authorId,
                    instanceId,
                    "Joe",
                    "joe@joe.com",
                    "",
                    true,
                    "",
                    false,
                ),
            ),
        ).once()
        EasyMock.expect(mockUserDAO.getByInstance(instanceId)).andReturn(
            listOf(
                User(
                    authorId,
                    instanceId,
                    "Joe",
                    "joe@joe.com",
                    "",
                    true,
                    "",
                    false,
                ),
                User(
                    UUID.randomUUID(),
                    instanceId,
                    "jim",
                    "jim@jim.com",
                    "",
                    false,
                    "",
                    true,
                ),
            ),
        ).once()
        val sentMessageCapture = EasyMock.newCapture<Message>()
        val gmsMock: GmailService = support.mock()
        val gmailMock: Gmail = support.mock()
        val usersMock: Gmail.Users = support.mock()
        val messagesMock: Gmail.Users.Messages = support.mock()
        val sendMock: Gmail.Users.Messages.Send = support.mock()

        val oauthMock: Oauth2 = support.mock()
        val userInfoMock: Oauth2.Userinfo = support.mock()
        val uiv2Mock: Oauth2.Userinfo.V2 = support.mock()
        val meMock: Oauth2.Userinfo.V2.Me = support.mock()
        val meGetMock: Oauth2.Userinfo.V2.Me.Get = support.mock()
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
        EasyMock.expect(meGetMock.execute()).andReturn(Userinfo().apply { email = "admin@gmail.com" })
        EasyMock.expect(mockInstanceDAO.get(instanceId))
            .andReturn(Instance(instanceId, "Instance Name", "ACTIVE"))
        EasyMock.expect(mockGmailServiceFactory.getService(instanceId)).andReturn(gmsMock).once()
        EasyMock.expect(
            mockEmailNotificationDAO.markNotified(
                NotificationType.REMINDER,
                (activeQuestions + closedQuestions).mapNotNull { it.id },
            ),
        )
        EasyMock.expect(
            mockEmailNotificationDAO.markNotified(
                NotificationType.ANSWER,
                closedQuestions.mapNotNull { it.id },
            ),
        )
        support.replayAll()
        bundle.sendEmails(OffsetDateTime.now())
        support.verifyAll()
        threadPool.shutdown()

        val message = sentMessageCapture.value
        val mimeMessage = MimeMessage(
            Session.getDefaultInstance(Properties()),
            ByteArrayInputStream(message.decodeRaw()),
        )
        assertThat(mimeMessage.getRecipients(javax.mail.Message.RecipientType.BCC).toList().map { it.toString() })
            .isEqualTo(listOf("jim <jim@jim.com>"))
        assertThat(mimeMessage.from.toList().map { it.toString() })
            .isEqualTo(listOf("Instance Name <admin@gmail.com>"))
        assertThat(mimeMessage.subject).isEqualTo("New Answer Available from Instance Name")
        val bodyContent = mimeMessage.content
        assertThat(bodyContent.toString()).contains("1 new answer")
        assertThat(bodyContent.toString()).contains("q1")
        assertThat(bodyContent.toString()).contains("a1")
        assertThat(bodyContent.toString()).contains("r1")
        assertThat(bodyContent.toString()).doesNotContain("new question")
        assertThat(bodyContent.toString()).doesNotContain("q2")
        assertThat(bodyContent.toString()).doesNotContain("q3")
        assertThat(bodyContent.toString()).doesNotContain("q4")
    }
}
