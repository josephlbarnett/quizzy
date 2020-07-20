package com.joe.quizzy.server.mail

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.model.Userinfo
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.config.ConfigLoader
import com.trib3.server.config.TribeApplicationConfig
import com.trib3.testing.LeakyMock
import com.trib3.testing.mock
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.util.KtorExperimentalAPI
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
        val mockGmailServiceFactory = support.mock<GmailServiceFactory>()
        val threadPool = Executors.newSingleThreadExecutor()
        val latch = CountDownLatch(1)
        val client = HttpClient(MockEngine) { engine { addHandler { respond("pong") } } }
        val bundle = ScheduledEmailBundle(
            ConfigLoader("test_override_send_email"),
            TribeApplicationConfig(ConfigLoader()),
            mockQuestionDAO,
            mockUserDAO,
            mockInstanceDAO,
            mockGmailServiceFactory,
            client,
            threadPool.asCoroutineDispatcher(),
            1
        )
        EasyMock.expect(mockQuestionDAO.active()).andAnswer {
            latch.countDown()
            listOf()
        }.once()
        EasyMock.expect(mockQuestionDAO.closed()).andReturn(listOf()).once()
        support.replayAll()
        launch {
            assertThat(client.isActive).isTrue()
            latch.await()
            bundle.pollJob?.cancel()
        }
        bundle.run(null, null)
        bundle.pollJob?.join()
        assertThat(client.isActive).isFalse()
        assertThat(threadPool.isShutdown).isTrue()
        support.verifyAll()
    }

    @OptIn(KtorExperimentalAPI::class)
    @Test
    fun testInjectConstructorAndPollingLoopDisabledByConfig() {
        val support = EasyMockSupport()
        val mockQuestionDAO = support.mock<QuestionDAO>()
        val mockInstanceDAO = support.mock<InstanceDAO>()
        val mockUserDAO = support.mock<UserDAO>()
        val mockGmailServiceFactory = support.mock<GmailServiceFactory>()
        val bundle = ScheduledEmailBundle(
            ConfigLoader(),
            TribeApplicationConfig(ConfigLoader()),
            mockQuestionDAO,
            mockUserDAO,
            mockInstanceDAO,
            mockGmailServiceFactory
        )
        support.replayAll()
        bundle.run(null, null)
        assertThat(bundle.pollJob).isNull()
        assertThat(bundle.configLoader).isNotNull()
        assertThat(bundle.appConfig).isNotNull()
        assertThat(bundle.questionDAO).isNotNull()
        assertThat(bundle.userDAO).isNotNull()
        assertThat(bundle.instanceDAO).isNotNull()
        assertThat(bundle.gmailServiceFactory).isNotNull()
        assertThat(bundle.client.engineConfig).isInstanceOf(CIOEngineConfig::class)
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
    fun testNoEmail() {
        val support = EasyMockSupport()
        val mockQuestionDAO = support.mock<QuestionDAO>()
        val mockInstanceDAO = support.mock<InstanceDAO>()
        val mockUserDAO = support.mock<UserDAO>()
        val mockGmailServiceFactory = support.mock<GmailServiceFactory>()
        val threadPool = Executors.newSingleThreadExecutor()
        val client = HttpClient(MockEngine) { engine { addHandler { respond("pong") } } }
        val bundle = ScheduledEmailBundle(
            ConfigLoader(),
            TribeApplicationConfig(ConfigLoader()),
            mockQuestionDAO,
            mockUserDAO,
            mockInstanceDAO,
            mockGmailServiceFactory,
            client,
            threadPool.asCoroutineDispatcher(),
            1
        )
        val authorId = UUID.randomUUID()
        val instanceId = UUID.randomUUID()
        EasyMock.expect(mockQuestionDAO.closed()).andReturn(
            listOf(
                Question(
                    UUID.randomUUID(),
                    authorId,
                    "q1",
                    "a1",
                    "r1",
                    OffsetDateTime.now().minusDays(2),
                    OffsetDateTime.now().minusHours(1),
                    sentReminder = true,
                    sentAnswer = true
                ),
                Question(
                    UUID.randomUUID(),
                    authorId,
                    "q2",
                    "a2",
                    "r2",
                    OffsetDateTime.now().minusDays(2),
                    OffsetDateTime.now().minusHours(1),
                    sentReminder = true,
                    sentAnswer = false
                )
            )
        ).once()
        EasyMock.expect(mockQuestionDAO.active()).andReturn(
            listOf(
                Question(
                    UUID.randomUUID(),
                    authorId,
                    "q3",
                    "a3",
                    "r3",
                    OffsetDateTime.now().minusDays(1),
                    OffsetDateTime.now().plusHours(1),
                    sentReminder = true,
                    sentAnswer = false
                ),
                Question(
                    UUID.randomUUID(),
                    authorId,
                    "q4",
                    "a4",
                    "r4",
                    OffsetDateTime.now().minusDays(1),
                    OffsetDateTime.now().plusHours(1),
                    sentReminder = false,
                    sentAnswer = false
                )
            )
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
                    false
                )
            )
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
                    false
                ),
                User(
                    UUID.randomUUID(),
                    instanceId,
                    "jim",
                    "jim@jim.com",
                    "",
                    false,
                    "",
                    true
                )
            )
        ).once()
        EasyMock.expect(mockGmailServiceFactory.getService(instanceId)).andReturn(null).once()
        support.replayAll()
        bundle.sendEmails(OffsetDateTime.now())
        support.verifyAll()
        threadPool.shutdown()
    }

    @Test
    fun testFullEmail() {
        val support = EasyMockSupport()
        val mockQuestionDAO = support.mock<QuestionDAO>()
        val mockInstanceDAO = support.mock<InstanceDAO>()
        val mockUserDAO = support.mock<UserDAO>()
        val mockGmailServiceFactory = support.mock<GmailServiceFactory>()
        val threadPool = Executors.newSingleThreadExecutor()
        val client = HttpClient(MockEngine) { engine { addHandler { respond("pong") } } }
        val bundle = ScheduledEmailBundle(
            ConfigLoader(),
            TribeApplicationConfig(ConfigLoader()),
            mockQuestionDAO,
            mockUserDAO,
            mockInstanceDAO,
            mockGmailServiceFactory,
            client,
            threadPool.asCoroutineDispatcher(),
            1
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
                sentReminder = true,
                sentAnswer = false
            ),
            Question(
                UUID.randomUUID(),
                authorId,
                "q2",
                "a2",
                "r2",
                OffsetDateTime.now().minusDays(2),
                OffsetDateTime.now().minusHours(1),
                sentReminder = false,
                sentAnswer = false
            )
        )
        EasyMock.expect(mockQuestionDAO.closed()).andReturn(
            closedQuestions
        ).once()
        val activeQuestions = listOf(
            Question(
                UUID.randomUUID(),
                authorId,
                "q3",
                "a3",
                "r3",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusHours(1),
                sentReminder = false,
                sentAnswer = false
            ),
            Question(
                UUID.randomUUID(),
                authorId,
                "q4",
                "a4",
                "r4",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusHours(1),
                sentReminder = false,
                sentAnswer = false
            )
        )
        EasyMock.expect(mockQuestionDAO.active()).andReturn(
            activeQuestions
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
                    false
                )
            )
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
                    false
                ),
                User(
                    UUID.randomUUID(),
                    instanceId,
                    "jim",
                    "jim@jim.com",
                    "",
                    false,
                    "",
                    true
                )
            )
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
        closedQuestions.filter { !it.sentAnswer }.forEach {
            EasyMock.expect(mockQuestionDAO.save(it.copy(sentReminder = true, sentAnswer = true)))
                .andReturn(it.copy(sentReminder = true, sentAnswer = true))
        }
        activeQuestions.filter { !it.sentReminder }.forEach {
            EasyMock.expect(mockQuestionDAO.save(it.copy(sentReminder = true))).andReturn(it.copy(sentReminder = true))
        }
        support.replayAll()
        bundle.sendEmails(OffsetDateTime.now())
        support.verifyAll()
        threadPool.shutdown()

        val message = sentMessageCapture.value
        val mimeMessage = MimeMessage(
            Session.getDefaultInstance(Properties()),
            ByteArrayInputStream(message.decodeRaw())
        )
        assertThat(mimeMessage.getRecipients(javax.mail.Message.RecipientType.BCC).toList().map { it.toString() })
            .isEqualTo(listOf("jim <jim@jim.com>"))
        assertThat(mimeMessage.from.toList().map { it.toString() })
            .isEqualTo(listOf("Instance Name <admin@gmail.com>"))
        assertThat(mimeMessage.subject).isEqualTo("New Questions and Answers Available from Instance Name")
        assertThat(mimeMessage.content.toString()).contains("2 new questions")
        assertThat(mimeMessage.content.toString()).contains("1: q3")
        assertThat(mimeMessage.content.toString()).contains("2: q4")
        assertThat(mimeMessage.content.toString()).contains("2 new answers")
        assertThat(mimeMessage.content.toString()).contains("1: q1")
        assertThat(mimeMessage.content.toString()).contains("Answer: a1")
        assertThat(mimeMessage.content.toString()).contains("Rule References: r1")
        assertThat(mimeMessage.content.toString()).contains("2: q2")
        assertThat(mimeMessage.content.toString()).contains("Answer: a2")
        assertThat(mimeMessage.content.toString()).contains("Rule References: r2")
    }

    @Test
    fun testSingleQuestionEmail() {
        val support = EasyMockSupport()
        val mockQuestionDAO = support.mock<QuestionDAO>()
        val mockInstanceDAO = support.mock<InstanceDAO>()
        val mockUserDAO = support.mock<UserDAO>()
        val mockGmailServiceFactory = support.mock<GmailServiceFactory>()
        val threadPool = Executors.newSingleThreadExecutor()
        val client = HttpClient(MockEngine) { engine { addHandler { respond("pong") } } }
        val bundle = ScheduledEmailBundle(
            ConfigLoader(),
            TribeApplicationConfig(ConfigLoader()),
            mockQuestionDAO,
            mockUserDAO,
            mockInstanceDAO,
            mockGmailServiceFactory,
            client,
            threadPool.asCoroutineDispatcher(),
            1
        )
        val authorId = UUID.randomUUID()
        val instanceId = UUID.randomUUID()
        val closedQuestions = listOf<Question>()
        EasyMock.expect(mockQuestionDAO.closed()).andReturn(
            closedQuestions
        ).once()
        val activeQuestions = listOf(
            Question(
                UUID.randomUUID(),
                authorId,
                "q3",
                "a3",
                "r3",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusHours(1),
                sentReminder = true,
                sentAnswer = false
            ),
            Question(
                UUID.randomUUID(),
                authorId,
                "q4",
                "a4",
                "r4",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusHours(1),
                sentReminder = false,
                sentAnswer = false
            )
        )
        EasyMock.expect(mockQuestionDAO.active()).andReturn(
            activeQuestions
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
                    false
                )
            )
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
                    false
                ),
                User(
                    UUID.randomUUID(),
                    instanceId,
                    "jim",
                    "jim@jim.com",
                    "",
                    false,
                    "",
                    true
                )
            )
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
        closedQuestions.filter { !it.sentAnswer }.forEach {
            EasyMock.expect(mockQuestionDAO.save(it.copy(sentReminder = true, sentAnswer = true)))
                .andReturn(it.copy(sentReminder = true, sentAnswer = true))
        }
        activeQuestions.filter { !it.sentReminder }.forEach {
            EasyMock.expect(mockQuestionDAO.save(it.copy(sentReminder = true))).andReturn(it.copy(sentReminder = true))
        }
        support.replayAll()
        bundle.sendEmails(OffsetDateTime.now())
        support.verifyAll()
        threadPool.shutdown()

        val message = sentMessageCapture.value
        val mimeMessage = MimeMessage(
            Session.getDefaultInstance(Properties()),
            ByteArrayInputStream(message.decodeRaw())
        )
        assertThat(mimeMessage.getRecipients(javax.mail.Message.RecipientType.BCC).toList().map { it.toString() })
            .isEqualTo(listOf("jim <jim@jim.com>"))
        assertThat(mimeMessage.from.toList().map { it.toString() })
            .isEqualTo(listOf("Instance Name <admin@gmail.com>"))
        assertThat(mimeMessage.subject).isEqualTo("New Questions Available from Instance Name")
        assertThat(mimeMessage.content.toString()).contains("1 new question")
        assertThat(mimeMessage.content.toString()).contains("1: q4")
        assertThat(mimeMessage.content.toString()).doesNotContain("new answer")
        assertThat(mimeMessage.content.toString()).doesNotContain("q1")
        assertThat(mimeMessage.content.toString()).doesNotContain("q2")
        assertThat(mimeMessage.content.toString()).doesNotContain("q3")
    }

    @Test
    fun testSingleAnswerEmail() {
        val support = EasyMockSupport()
        val mockQuestionDAO = support.mock<QuestionDAO>()
        val mockInstanceDAO = support.mock<InstanceDAO>()
        val mockUserDAO = support.mock<UserDAO>()
        val mockGmailServiceFactory = support.mock<GmailServiceFactory>()
        val threadPool = Executors.newSingleThreadExecutor()
        val client = HttpClient(MockEngine) { engine { addHandler { respond("pong") } } }
        val bundle = ScheduledEmailBundle(
            ConfigLoader(),
            TribeApplicationConfig(ConfigLoader()),
            mockQuestionDAO,
            mockUserDAO,
            mockInstanceDAO,
            mockGmailServiceFactory,
            client,
            threadPool.asCoroutineDispatcher(),
            1
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
                sentReminder = true,
                sentAnswer = false
            ),
            Question(
                UUID.randomUUID(),
                authorId,
                "q2",
                "a2",
                "r2",
                OffsetDateTime.now().minusDays(2),
                OffsetDateTime.now().minusHours(1),
                sentReminder = false,
                sentAnswer = true
            )
        )
        EasyMock.expect(mockQuestionDAO.closed()).andReturn(
            closedQuestions
        ).once()
        val activeQuestions = listOf<Question>()
        EasyMock.expect(mockQuestionDAO.active()).andReturn(
            activeQuestions
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
                    false
                )
            )
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
                    false
                ),
                User(
                    UUID.randomUUID(),
                    instanceId,
                    "jim",
                    "jim@jim.com",
                    "",
                    false,
                    "",
                    true
                )
            )
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
        closedQuestions.filter { !it.sentAnswer }.forEach {
            EasyMock.expect(mockQuestionDAO.save(it.copy(sentReminder = true, sentAnswer = true)))
                .andReturn(it.copy(sentReminder = true, sentAnswer = true))
        }
        activeQuestions.filter { !it.sentReminder }.forEach {
            EasyMock.expect(mockQuestionDAO.save(it.copy(sentReminder = true))).andReturn(it.copy(sentReminder = true))
        }
        support.replayAll()
        bundle.sendEmails(OffsetDateTime.now())
        support.verifyAll()
        threadPool.shutdown()

        val message = sentMessageCapture.value
        val mimeMessage = MimeMessage(
            Session.getDefaultInstance(Properties()),
            ByteArrayInputStream(message.decodeRaw())
        )
        assertThat(mimeMessage.getRecipients(javax.mail.Message.RecipientType.BCC).toList().map { it.toString() })
            .isEqualTo(listOf("jim <jim@jim.com>"))
        assertThat(mimeMessage.from.toList().map { it.toString() })
            .isEqualTo(listOf("Instance Name <admin@gmail.com>"))
        assertThat(mimeMessage.subject).isEqualTo("New Answers Available from Instance Name")
        assertThat(mimeMessage.content.toString()).contains("1 new answer")
        assertThat(mimeMessage.content.toString()).contains("1: q1")
        assertThat(mimeMessage.content.toString()).contains("Answer: a1")
        assertThat(mimeMessage.content.toString()).contains("Rule References: r1")
        assertThat(mimeMessage.content.toString()).doesNotContain("new question")
        assertThat(mimeMessage.content.toString()).doesNotContain("q2")
        assertThat(mimeMessage.content.toString()).doesNotContain("q3")
        assertThat(mimeMessage.content.toString()).doesNotContain("q4")
    }
}
