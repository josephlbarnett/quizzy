package com.joe.quizzy.graphql

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
import com.joe.quizzy.persistence.api.GradeDAO
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.trib3.graphql.execution.GraphQLAuth
import com.trib3.graphql.resources.GraphQLResourceContext
import com.trib3.server.config.TribeApplicationConfig
import io.dropwizard.auth.basic.BasicCredentials
import java.io.InputStreamReader
import java.io.StringWriter
import java.net.URLEncoder
import java.security.Principal
import java.time.OffsetDateTime
import java.util.Date
import java.util.Properties
import java.util.UUID
import javax.inject.Inject
import javax.mail.Message
import javax.mail.internet.MimeMessage
import javax.ws.rs.core.Cookie
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.NewCookie

private const val COOKIE_NAME = "x-quizzy-session"
private const val MAX_COOKIE_AGE = 60 * 60 * 24 * 30 // expire in 30 days

/**
 * GraphQL entry point for [Thing] mutations.   Maps the DAO interfaces to the GraphQL models.
 */
class Mutation @Inject constructor(
    private val questionDAO: QuestionDAO,
    private val sessionDAO: SessionDAO,
    private val userDAO: UserDAO,
    private val responseDAO: ResponseDAO,
    private val gradeDAO: GradeDAO,
    private val userAuthenticator: UserAuthenticator,
    private val instanceDAO: InstanceDAO,
    private val gmailServiceFactory: GmailServiceFactory,
    private val appConfig: TribeApplicationConfig
) : com.expediagroup.graphql.server.operations.Mutation {

    private val newUserHtmlTemplate =
        ScheduledEmailBundle::class.java.getResourceAsStream("/assets/emails/newuser.html").let {
            InputStreamReader(it).use { reader ->
                DefaultMustacheFactory().compile(reader, "newuser")
            }
        }

    private val passwordResetHtmlTemplate =
        ScheduledEmailBundle::class.java.getResourceAsStream("/assets/emails/passwordreset.html").let {
            InputStreamReader(it).use { reader ->
                DefaultMustacheFactory().compile(reader, "passwordreset")
            }
        }

    private fun loginNewUser(principal: Principal?, context: GraphQLResourceContext): Boolean {
        if (principal is UserPrincipal) {
            val userId = principal.user.id
            if (userId != null) {
                val newSession = sessionDAO.save(Session(null, userId, OffsetDateTime.now(), OffsetDateTime.now()))
                context.cookie = NewCookie(
                    COOKIE_NAME,
                    newSession.id.toString(),
                    null,
                    null,
                    1,
                    null,
                    MAX_COOKIE_AGE,
                    null,
                    true,
                    true
                )
                return true
            }
        }
        return false
    }

    fun login(context: GraphQLResourceContext, email: String, pass: String): Boolean {
        if (context.principal != null) {
            return true // if already logged in, return
        }
        val principal = userAuthenticator.authenticate(BasicCredentials(email, pass)).orElse(null)
        return loginNewUser(principal, context)
    }

    fun changePassword(context: GraphQLResourceContext, oldPass: String, newPass: String): Boolean {
        val principal = context.principal
        if (principal is UserPrincipal) {
            val passCheck = userAuthenticator.authenticate(
                BasicCredentials(principal.user.email, oldPass)
            ).map { it as? UserPrincipal }.orElse(null)
            if (passCheck != null && passCheck.user.id != null && passCheck.user.id == principal.user.id) {
                userDAO.savePassword(passCheck.user, userAuthenticator.hasher.hash(newPass))
                return true
            }
        }
        return false
    }

    fun logout(context: GraphQLResourceContext): Boolean {
        val principal = context.principal
        if (principal is UserPrincipal) {
            context.cookie =
                NewCookie(
                    Cookie(COOKIE_NAME, ""),
                    null,
                    -1,
                    Date(0), // expire 1970
                    false,
                    true
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
                val instanceAddress = gmail.oauth.userinfo().v2().me().get().execute().email
                val instanceName = instanceDAO.get(user.instanceId)?.name ?: "Quizzy"
                val message = MimeMessage(javax.mail.Session.getDefaultInstance(Properties(), null))
                val code = UUID.randomUUID().toString()
                message.setFrom("$instanceName <$instanceAddress>")
                message.addRecipients(
                    Message.RecipientType.TO,
                    "${user.name} <${user.email}>"
                )
                message.subject = "$instanceName Password Reset"
                message.setContent(
                    passwordResetHtmlTemplate.execute(
                        StringWriter(),
                        mapOf(
                            "instanceName" to instanceName,
                            "user" to user,
                            "code" to code,
                            "link" to "https://${appConfig.corsDomains[0]}/app/assets#/passreset" +
                                "?code=$code&email=${URLEncoder.encode(user.email, "UTF-8")}"
                        )
                    ).toString(),
                    MediaType.TEXT_HTML
                )
                userDAO.save(user.copy(passwordResetToken = userAuthenticator.hasher.hash(code)))
                gmail.gmail.sendEmail("me", message).execute()
            }
        }
        return true
    }

    fun completePasswordReset(email: String, code: String, newPass: String): Boolean {
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

    fun users(context: GraphQLResourceContext, users: List<User>): List<User?> {
        return users.map { user(context, it) }
    }

    private fun sendNewUserEmail(principal: UserPrincipal, savedUser: User, password: String?) {
        gmailServiceFactory.getService(principal.user.instanceId)?.let { gmail ->
            val instanceAddress = gmail.oauth.userinfo().v2().me().get().execute().email
            val instanceName = instanceDAO.get(principal.user.instanceId)?.name ?: "Quizzy"
            val message = MimeMessage(javax.mail.Session.getDefaultInstance(Properties(), null))
            message.setFrom("$instanceName <$instanceAddress>")
            message.addRecipients(
                Message.RecipientType.TO,
                "${savedUser.name} <${savedUser.email}>"
            )
            message.subject = "Welcome to $instanceName"
            message.setContent(
                newUserHtmlTemplate.execute(
                    StringWriter(),
                    mapOf(
                        "instanceName" to instanceName,
                        "user" to savedUser,
                        "admin" to principal.user,
                        "password" to password,
                        "link" to "https://${appConfig.corsDomains[0]}"
                    )
                ).toString(),
                MediaType.TEXT_HTML
            )
            gmail.gmail.sendEmail("me", message).execute()
        }
    }

    fun user(context: GraphQLResourceContext, user: User): User? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            if (principal.user.admin || (principal.user.id != null && principal.user.id == user.id)) {
                val password = if (user.id == null) {
                    // generate new password
                    UUID.randomUUID().toString()
                } else {
                    null
                }
                val savedUser = if (password != null) {
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

    fun response(context: GraphQLResourceContext, response: Response): Response? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            val id = principal.user.id
            require(id != null)
            return responseDAO.save(
                response.copy(
                    userId = id
                )
            )
        }
        return null
    }

    @GraphQLAuth(["ADMIN"])
    fun grade(grade: Grade): Grade? {
        return gradeDAO.save(grade)
    }

    @GraphQLAuth(["ADMIN"])
    fun question(question: Question): Question? {
        return questionDAO.save(question)
    }
}
