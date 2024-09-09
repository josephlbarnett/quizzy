package com.joe.quizzy.graphql

import com.expediagroup.graphql.generator.extensions.get
import com.github.mustachejava.DefaultMustacheFactory
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.Session
import com.joe.quizzy.api.models.User
import com.joe.quizzy.graphql.auth.UserAuthenticator
import com.joe.quizzy.graphql.auth.UserPrincipal
import com.joe.quizzy.graphql.mail.GmailServiceFactory
import com.joe.quizzy.graphql.mail.ScheduledEmailBundle
import com.joe.quizzy.graphql.mail.sendEmail
import com.joe.quizzy.graphql.models.ApiResponse
import com.joe.quizzy.persistence.api.GradeDAO
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.graphql.execution.GraphQLAuth
import com.trib3.server.config.TribeApplicationConfig
import graphql.schema.DataFetchingEnvironment
import io.dropwizard.auth.basic.BasicCredentials
import jakarta.inject.Inject
import jakarta.mail.Message
import jakarta.mail.internet.MimeMessage
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.NewCookie
import jakarta.ws.rs.core.Response.ResponseBuilder
import java.io.InputStreamReader
import java.io.StringWriter
import java.net.URLEncoder
import java.security.Principal
import java.time.OffsetDateTime
import java.util.Date
import java.util.Properties
import java.util.UUID

private const val COOKIE_NAME = "x-quizzy-session"
private const val MAX_COOKIE_AGE = 60 * 60 * 24 * 30 // expire in 30 days

/**
 * GraphQL entry point for mutations.  Maps the DAO interfaces to the GraphQL models.
 */
@Suppress("TooManyFunctions")
class Mutation
    @Inject
    constructor(
        private val questionDAO: QuestionDAO,
        private val sessionDAO: SessionDAO,
        private val userDAO: UserDAO,
        private val responseDAO: ResponseDAO,
        private val gradeDAO: GradeDAO,
        private val userAuthenticator: UserAuthenticator,
        private val instanceDAO: InstanceDAO,
        private val gmailServiceFactory: GmailServiceFactory,
        private val appConfig: TribeApplicationConfig,
    ) : com.expediagroup.graphql.server.operations.Mutation {
        private val newUserHtmlTemplate =
            ScheduledEmailBundle::class.java.getResourceAsStream("/assets/emails/newuser.html")?.let {
                InputStreamReader(it).use { reader ->
                    DefaultMustacheFactory().compile(reader, "newuser")
                }
            }

        private val passwordResetHtmlTemplate =
            ScheduledEmailBundle::class.java.getResourceAsStream("/assets/emails/passwordreset.html")?.let {
                InputStreamReader(it).use { reader ->
                    DefaultMustacheFactory().compile(reader, "passwordreset")
                }
            }

        private fun loginNewUser(
            principal: Principal?,
            dfe: DataFetchingEnvironment,
        ): Boolean {
            if (principal is UserPrincipal) {
                val userId = principal.user.id
                if (userId != null) {
                    val newSession = sessionDAO.save(Session(null, userId, OffsetDateTime.now(), OffsetDateTime.now()))
                    dfe.graphQlContext.get<ResponseBuilder>()?.cookie(
                        NewCookie
                            .Builder(COOKIE_NAME)
                            .value(newSession.id.toString())
                            .maxAge(MAX_COOKIE_AGE)
                            .secure(true)
                            .httpOnly(true)
                            .build(),
                    )
                    return true
                }
            }
            return false
        }

        fun login(
            dfe: DataFetchingEnvironment,
            email: String,
            pass: String,
        ): Boolean {
            if (dfe.graphQlContext.get<Principal>() != null) {
                return true // if already logged in, return
            }
            val principal = userAuthenticator.authenticate(BasicCredentials(email, pass)).orElse(null)
            return loginNewUser(principal, dfe)
        }

        fun changePassword(
            dfe: DataFetchingEnvironment,
            oldPass: String,
            newPass: String,
        ): Boolean {
            val principal = dfe.graphQlContext.get<Principal>()
            if (principal is UserPrincipal) {
                val passCheck =
                    userAuthenticator
                        .authenticate(
                            BasicCredentials(principal.user.email, oldPass),
                        ).map { it as? UserPrincipal }
                        .orElse(null)
                if (passCheck != null && passCheck.user.id != null && passCheck.user.id == principal.user.id) {
                    userDAO.savePassword(passCheck.user, userAuthenticator.hasher.hash(newPass))
                    return true
                }
            }
            return false
        }

        fun logout(dfe: DataFetchingEnvironment): Boolean {
            val principal = dfe.graphQlContext.get<Principal>()
            if (principal is UserPrincipal) {
                dfe.graphQlContext.get<ResponseBuilder>()?.cookie(
                    NewCookie
                        .Builder(COOKIE_NAME)
                        .value("")
                        .maxAge(-1)
                        .expiry(Date(0)) // expire 1970
                        .httpOnly(true)
                        .secure(true)
                        .build(),
                )
                val session = principal.session
                if (session != null) {
                    sessionDAO.delete(session)
                }
                return true
            }
            return false
        }

        fun requestPasswordReset(email: String): Boolean {
            val user = userDAO.getByEmail(email)
            if (user != null) {
                gmailServiceFactory.getService(user.instanceId)?.let { gmail ->
                    val instanceAddress =
                        gmail.oauth
                            .userinfo()
                            .v2()
                            .me()
                            .get()
                            .execute()
                            .email
                    val instanceName = instanceDAO.get(user.instanceId)?.name ?: "Quizzy"
                    val message = MimeMessage(jakarta.mail.Session.getDefaultInstance(Properties(), null))
                    val code = UUID.randomUUID().toString()
                    message.setFrom("$instanceName <$instanceAddress>")
                    message.addRecipients(
                        Message.RecipientType.TO,
                        "${user.name} <${user.email}>",
                    )
                    message.subject = "$instanceName Password Reset"
                    message.setContent(
                        passwordResetHtmlTemplate
                            ?.execute(
                                StringWriter(),
                                mapOf(
                                    "instanceName" to instanceName,
                                    "user" to user,
                                    "code" to code,
                                    "link" to "https://${appConfig.corsDomains[0]}/app/assets#/passreset" +
                                        "?code=$code&email=${URLEncoder.encode(user.email, "UTF-8")}",
                                ),
                            ).toString(),
                        MediaType.TEXT_HTML,
                    )
                    userDAO.save(user.copy(passwordResetToken = userAuthenticator.hasher.hash(code)))
                    gmail.gmail.sendEmail("me", message).execute()
                }
            }
            return true
        }

        fun completePasswordReset(
            email: String,
            code: String,
            newPass: String,
        ): Boolean {
            val user = userDAO.getByEmail(email)
            val existingCode = user?.passwordResetToken
            if (existingCode != null) {
                if (userAuthenticator.hasher.verify(existingCode, code)) {
                    userDAO.savePassword(user, userAuthenticator.hasher.hash(newPass))
                    return true
                }
            }
            return false
        }

        fun users(
            dfe: DataFetchingEnvironment,
            users: List<User>,
        ): List<User?> = users.map { user(dfe, it) }

        private fun sendNewUserEmail(
            principal: UserPrincipal,
            savedUser: User,
            password: String?,
        ) {
            gmailServiceFactory.getService(principal.user.instanceId)?.let { gmail ->
                val instanceAddress =
                    gmail.oauth
                        .userinfo()
                        .v2()
                        .me()
                        .get()
                        .execute()
                        .email
                val instanceName = instanceDAO.get(principal.user.instanceId)?.name ?: "Quizzy"
                val message = MimeMessage(jakarta.mail.Session.getDefaultInstance(Properties(), null))
                message.setFrom("$instanceName <$instanceAddress>")
                message.addRecipients(
                    Message.RecipientType.TO,
                    "${savedUser.name} <${savedUser.email}>",
                )
                message.subject = "Welcome to $instanceName"
                message.setContent(
                    newUserHtmlTemplate
                        ?.execute(
                            StringWriter(),
                            mapOf(
                                "instanceName" to instanceName,
                                "user" to savedUser,
                                "admin" to principal.user,
                                "password" to password,
                                "link" to "https://${appConfig.corsDomains[0]}/app/assets#/me",
                            ),
                        ).toString(),
                    MediaType.TEXT_HTML,
                )
                gmail.gmail.sendEmail("me", message).execute()
            }
        }

        fun user(
            dfe: DataFetchingEnvironment,
            user: User,
        ): User? {
            val principal = dfe.graphQlContext.get<Principal>()
            if (principal is UserPrincipal) {
                if (principal.user.admin || (principal.user.id != null && principal.user.id == user.id)) {
                    val password =
                        if (user.id == null) {
                            // generate new password
                            UUID.randomUUID().toString()
                        } else {
                            null
                        }
                    val savedUser =
                        if (password != null) {
                            user.copy(authCrypt = userAuthenticator.hasher.hash(password))
                        } else {
                            user
                        }.let {
                            userDAO.save(it)
                        }
                    if (savedUser.id != user.id) {
                        // new user!
                        sendNewUserEmail(principal, savedUser, password)
                    }
                    return savedUser
                }
            }
            return null
        }

        fun createUser(
            user: User,
            inviteCode: UUID,
            password: String,
        ): User? = userDAO.create(user, inviteCode, userAuthenticator.hasher.hash(password))

        fun response(
            dfe: DataFetchingEnvironment,
            response: Response,
        ): ApiResponse? {
            val principal = dfe.graphQlContext.get<Principal>()
            if (principal is UserPrincipal) {
                val id = principal.user.id
                requireNotNull(id)
                val instance = instanceDAO.get(principal.user.instanceId)
                requireNotNull(instance)
                return ApiResponse(
                    responseDAO.save(
                        response.copy(
                            userId = id,
                        ),
                    ),
                    instance.defaultScore,
                )
            }
            return null
        }

        @GraphQLAuth(["ADMIN"])
        fun grade(grade: Grade): Grade? = gradeDAO.save(grade)

        @GraphQLAuth(["ADMIN"])
        fun question(question: Question): Question? = questionDAO.save(question)
    }
