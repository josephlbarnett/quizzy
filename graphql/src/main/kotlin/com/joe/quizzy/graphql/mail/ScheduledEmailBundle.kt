package com.joe.quizzy.graphql.mail

import com.github.mustachejava.DefaultMustacheFactory
import com.joe.quizzy.api.models.AnswerChoice
import com.joe.quizzy.api.models.NotificationType
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.QuestionType
import com.joe.quizzy.graphql.groupme.GroupMeServiceFactory
import com.joe.quizzy.persistence.api.EmailNotificationDAO
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.config.ConfigLoader
import com.trib3.config.extract
import com.trib3.server.config.TribeApplicationConfig
import io.dropwizard.core.Configuration
import io.dropwizard.core.ConfiguredBundle
import io.dropwizard.core.setup.Environment
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.url
import jakarta.inject.Inject
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import jakarta.ws.rs.core.MediaType
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
    val groupMeServiceFactory: GroupMeServiceFactory,
    val client: HttpClient,
    val dispatcher: ExecutorCoroutineDispatcher,
    val minuteMod: Int,
) : ConfiguredBundle<Configuration>, CoroutineScope by CoroutineScope(dispatcher) {
    @Inject
    constructor(
        configLoader: ConfigLoader,
        appConfig: TribeApplicationConfig,
        questionDAO: QuestionDAO,
        userDAO: UserDAO,
        instanceDAO: InstanceDAO,
        emailNotificationDAO: EmailNotificationDAO,
        gmailServiceFactory: GmailServiceFactory,
        groupMeServiceFactory: GroupMeServiceFactory,
        ktorClient: HttpClient,
    ) : this(
        configLoader,
        appConfig,
        questionDAO,
        userDAO,
        instanceDAO,
        emailNotificationDAO,
        gmailServiceFactory,
        groupMeServiceFactory,
        ktorClient,
        Executors.newSingleThreadExecutor {
            Thread(it, "ScheduledEmailBundle").apply { isDaemon = true }
        }.asCoroutineDispatcher(),
        POLL_MINUTES,
    )

    internal var pollJob: Job? = null
    private val htmlTemplate =
        ScheduledEmailBundle::class.java.getResourceAsStream("/assets/emails/question.html")?.let {
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

    internal suspend fun sendText(
        instanceId: UUID,
        questionAnswerString: String,
    ) {
        try {
            val groupMe = groupMeServiceFactory.create(instanceId)
            groupMe?.postMessage("New $questionAnswerString Available: https://${appConfig.corsDomains[0]}/app/assets")
        } catch (e: Exception) {
            log.error("Error sending text: ${e.message}", e)
        }
    }

    /**
     * Sends an email to all users with notifications enabled containing
     * the newly available questions and any newly published answers
     */
    internal fun sendEmail(
        instanceId: UUID,
        questions: List<Question>,
        answers: List<Question>,
        questionAnswerString: String,
    ) {
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
                    "${user.name} <${user.email}>",
                )
            }
            message.subject = "New $questionAnswerString Available from $instanceName"
            message.setContent(
                htmlTemplate?.execute(
                    StringWriter(),
                    mapOf(
                        "instanceName" to instanceName,
                        "domainLink" to "https://${appConfig.corsDomains[0]}",
                        "questionCount" to countObject(questions.size),
                        "answerCount" to countObject(answers.size),
                        "question" to questions.mapIndexed { index, question ->
                            mapOf(
                                "index" to index + 1,
                                "body" to question.body,
                                "answerChoices" to question.answerChoices?.map { choiceMap(it) },
                            )
                        },
                        "answer" to answers.mapIndexed { index, question ->
                            mapOf(
                                "index" to index + 1,
                                "body" to question.body,
                                "answer" to question.answer + multiChoiceAnswer(question),
                                "ruleReferences" to question.ruleReferences,
                            )
                        },
                    ),
                ).toString(),
                MediaType.TEXT_HTML,
            )
            emailNotificationDAO.markNotified(NotificationType.REMINDER, (questions + answers).mapNotNull { it.id })
            emailNotificationDAO.markNotified(NotificationType.ANSWER, answers.mapNotNull { it.id })
            log.info(
                "Sending email for ${questions.size} questions and ${answers.size} answers " +
                    "to ${usersToNotify.size} users for instance $instanceName",
            )
            gmail.gmail.sendEmail("me", message).execute()
        }
    }

    private fun choiceMap(choice: AnswerChoice): Map<String, String> {
        return mapOf(
            "letter" to choice.letter,
            "answerChoiceAnswer" to choice.answer,
        )
    }

    private fun multiChoiceAnswer(question: Question): String {
        if (question.type == QuestionType.MULTIPLE_CHOICE) {
            val correctAnswer = question.answerChoices?.firstOrNull { it.letter == question.answer }
            if (correctAnswer != null) {
                return ": ${correctAnswer.answer}"
            }
        }
        return ""
    }

    /**
     * Collects questions that are newly published or have newly published answers
     * by instanceId, and sends an email for each instanceId grouping.
     */
    internal suspend fun sendEmails(now: OffsetDateTime) {
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
                val qString = if (questions.size == 1) {
                    "Question"
                } else {
                    "Questions"
                }
                val aString = if (answers.size == 1) {
                    "Answer"
                } else {
                    "Answers"
                }
                val questionAnswerString = if (questions.isNotEmpty() && answers.isNotEmpty()) {
                    "$qString and $aString"
                } else if (answers.isNotEmpty()) {
                    aString
                } else {
                    qString
                }
                sendEmail(instanceId, questions, answers, questionAnswerString)
                sendText(instanceId, questionAnswerString)
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
                                client.get { url("https://${appConfig.corsDomains[0]}/app/ping") }.body<String>(),
                            )
                        }
                        sendEmails(now)
                    }
                    val newNow = OffsetDateTime.now()
                    delay(Duration.ofSeconds(Duration.ofMinutes(1).seconds - newNow.second))
                }
            }
            newJob.invokeOnCompletion {
                dispatcher.close()
            }
            pollJob = newJob
        }
    }
}
