package com.joe.quizzy.graphql.mail

import com.github.mustachejava.DefaultMustacheFactory
import com.joe.quizzy.api.models.NotificationType
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.persistence.api.EmailNotificationDAO
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.config.ConfigLoader
import com.trib3.config.extract
import com.trib3.server.config.TribeApplicationConfig
import io.dropwizard.Configuration
import io.dropwizard.ConfiguredBundle
import io.dropwizard.setup.Environment
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import mu.KotlinLogging
import java.io.InputStreamReader
import java.io.StringWriter
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Properties
import java.util.UUID
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.ws.rs.core.MediaType

private val log = KotlinLogging.logger {}
private const val POLL_MINUTES = 5

@Suppress("LongParameterList")
class ScheduledEmailBundle(
    val configLoader: ConfigLoader,
    val appConfig: TribeApplicationConfig,
    val questionDAO: QuestionDAO,
    val userDAO: UserDAO,
    val instanceDAO: InstanceDAO,
    val emailNotificationDAO: EmailNotificationDAO,
    val gmailServiceFactory: GmailServiceFactory,
    val client: HttpClient,
    val dispatcher: ExecutorCoroutineDispatcher,
    val minuteMod: Int
) : ConfiguredBundle<Configuration>, CoroutineScope by CoroutineScope(dispatcher) {
    @Inject
    constructor(
        configLoader: ConfigLoader,
        appConfig: TribeApplicationConfig,
        questionDAO: QuestionDAO,
        userDAO: UserDAO,
        instanceDAO: InstanceDAO,
        emailNotificationDAO: EmailNotificationDAO,
        gmailServiceFactory: GmailServiceFactory
    ) : this(
        configLoader,
        appConfig,
        questionDAO,
        userDAO,
        instanceDAO,
        emailNotificationDAO,
        gmailServiceFactory,
        HttpClient(CIO),
        Executors.newSingleThreadExecutor {
            Thread(it, "ScheduledEmailBundle").apply { isDaemon = true }
        }.asCoroutineDispatcher(),
        POLL_MINUTES
    )

    internal var pollJob: Job? = null
    private val htmlTemplate =
        ScheduledEmailBundle::class.java.getResourceAsStream("/assets/emails/question.html").let {
            InputStreamReader(it).use { reader ->
                DefaultMustacheFactory().compile(reader, "question")
            }
        }

    private fun countObject(size: Int): Map<String, Any>? {
        return when (size) {
            0 -> null
            1 -> mapOf("num" to size, "plural" to " is")
            else -> mapOf("num" to size, "plural" to "s are")
        }
    }

    /**
     * Sends an email to all users with notifications enabled containing
     * the newly available questions and any newly published answers
     */
    internal fun sendEmail(instanceId: UUID, questions: List<Question>, answers: List<Question>) {
        val usersToNotify =
            userDAO.getByInstance(instanceId).filter { it.notifyViaEmail }
        gmailServiceFactory.getService(instanceId)?.let { gmail ->
            val instanceAddress = gmail.oauth.userinfo().v2().me().get().execute().email
            val instanceName = instanceDAO.get(instanceId)?.name ?: "Quizzy"
            val message = MimeMessage(Session.getDefaultInstance(Properties(), null))
            message.setFrom("$instanceName <$instanceAddress>")
            usersToNotify.forEach { user ->
                message.addRecipients(
                    Message.RecipientType.BCC,
                    "${user.name} <${user.email}>"
                )
            }
            val questionAnswerString = if (questions.isNotEmpty() && answers.isNotEmpty()) {
                "Questions and Answers"
            } else if (answers.isNotEmpty()) {
                "Answers"
            } else {
                "Questions"
            }
            message.subject = "New $questionAnswerString Available from $instanceName"
            message.setContent(
                htmlTemplate.execute(
                    StringWriter(),
                    mapOf(
                        "instanceName" to instanceName,
                        "domainLink" to "https://${appConfig.corsDomains[0]}",
                        "questionCount" to countObject(questions.size),
                        "answerCount" to countObject(answers.size),
                        "question" to questions.mapIndexed { index, question ->
                            mapOf("index" to index + 1, "body" to question.body)
                        },
                        "answer" to answers.mapIndexed { index, question ->
                            mapOf(
                                "index" to index + 1,
                                "body" to question.body,
                                "answer" to question.answer,
                                "ruleReferences" to question.ruleReferences
                            )
                        }
                    )
                ).toString(),
                MediaType.TEXT_HTML
            )
            emailNotificationDAO.markNotified(NotificationType.REMINDER, (questions + answers).mapNotNull { it.id })
            emailNotificationDAO.markNotified(NotificationType.ANSWER, answers.mapNotNull { it.id })
            log.info(
                "Sending email for ${questions.size} questions and ${answers.size} answers " +
                    "to ${usersToNotify.size} users for instance $instanceName"
            )
            gmail.gmail.sendEmail("me", message).execute()
        }
    }

    /**
     * Collects questions that are newly published or have newly published answers
     * by instanceId, and sends an email for each instanceId grouping.
     */
    internal fun sendEmails(now: OffsetDateTime) {
        val activeNotifications = questionDAO.active(NotificationType.REMINDER)
        val closedNotifications = questionDAO.closed(NotificationType.ANSWER)
        val authors = if (activeNotifications.isEmpty() && closedNotifications.isEmpty()) {
            mapOf()
        } else {
            userDAO.get(activeNotifications.map { it.authorId } + closedNotifications.map { it.authorId })
                .associateBy { it.id }
        }
        (activeNotifications + closedNotifications).distinct().groupBy {
            authors.getValue(it.authorId).instanceId
        }.forEach { entry ->
            val (questions, answers) = entry.value.partition { it.closedAt.isAfter(now) }
            val instanceId = entry.key
            if (questions.isNotEmpty() || answers.isNotEmpty()) {
                sendEmail(instanceId, questions, answers)
            }
        }
    }

    override fun run(configuration: Configuration?, environment: Environment?) {
        // just use a configloader directly because too lazy to create a config object
        if (configLoader.load().extract("pollForEmails")) {
            val newJob = launch {
                while (isActive) {
                    val now = OffsetDateTime.now()
                    if (now.minute % minuteMod == 0) {
                        runCatching {
                            log.trace(
                                client.get { url("https://${appConfig.corsDomains[0]}/app/ping") }.body<String>()
                            )
                        }
                        sendEmails(now)
                    }
                    delay(Duration.ofMinutes(1))
                }
            }
            newJob.invokeOnCompletion {
                client.close()
                dispatcher.close()
            }
            pollJob = newJob
        }
    }
}
