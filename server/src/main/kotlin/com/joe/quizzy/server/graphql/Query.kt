package com.joe.quizzy.server.graphql

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.GradeDAO
import com.joe.quizzy.persistence.api.QuestionDAO
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.api.UserDAO
import com.joe.quizzy.server.auth.UserPrincipal
import com.trib3.graphql.modules.DataLoaderRegistryFactory
import com.trib3.graphql.resources.GraphQLResourceContext
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import org.dataloader.BatchLoader
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import javax.inject.Inject
import javax.inject.Provider

private val log = KotlinLogging.logger {}

/**
 * like a [Question] but can fetch current [User]'s [Response]s
 */
data class ApiQuestion(
    val id: UUID?,
    val authorId: UUID,
    val body: String,
    val answer: String,
    val ruleReferences: String,
    val activeAt: OffsetDateTime,
    val closedAt: OffsetDateTime,

    @GraphQLIgnore private val responseDAO: ResponseDAO,
    @GraphQLIgnore private val userDAO: UserDAO,
    @GraphQLIgnore private val questionDAO: QuestionDAO,
    @GraphQLIgnore private val gradeDAO: GradeDAO
) {
    constructor(
        question: Question,
        responseDAO: ResponseDAO,
        userDAO: UserDAO,
        questionDAO: QuestionDAO,
        gradeDAO: GradeDAO
    ) : this(
        question.id,
        question.authorId,
        question.body,
        question.answer,
        question.ruleReferences,
        question.activeAt,
        question.closedAt,
        responseDAO,
        userDAO,
        questionDAO,
        gradeDAO
    )

    suspend fun response(context: GraphQLResourceContext): ApiResponse? {
        val principal = context.principal
        if (principal is UserPrincipal && id != null) {
            return responseDAO.byUserQuestion(principal.user, id)?.let {
                ApiResponse(it, userDAO, questionDAO, gradeDAO)
            }
        }
        return null
    }
}

/**
 * Like a [Response] but can dynamically fetch the associated [User]/[Question]/[Grade]
 */
data class ApiResponse(
    val id: UUID?,
    val userId: UUID,
    val questionId: UUID,
    val response: String,
    val ruleReferences: String,

    @GraphQLIgnore private val userDAO: UserDAO,
    @GraphQLIgnore private val questionDAO: QuestionDAO,
    @GraphQLIgnore private val gradeDAO: GradeDAO
) {
    constructor(response: Response, userDAO: UserDAO, questionDAO: QuestionDAO, gradeDAO: GradeDAO) :
        this(
            response.id,
            response.userId,
            response.questionId,
            response.response,
            response.ruleReferences,
            userDAO,
            questionDAO,
            gradeDAO
        )

    suspend fun user(context: GraphQLResourceContext): User? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return userDAO.get(userId)
        }
        return null
    }

    suspend fun question(context: GraphQLResourceContext): Question? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return questionDAO.get(questionId)
        }
        return null
    }

    suspend fun grade(context: GraphQLResourceContext): Grade? {
        val principal = context.principal
        if (principal is UserPrincipal && id != null) {
            return gradeDAO.forResponse(id)
        }
        return null
    }
}

/**
 * Like a [User] but can dynamically calculate the overall score of [Response]s
 */
data class ApiUser(
    val id: UUID?,
    val instanceId: UUID,
    val name: String,
    val email: String,
    val authCrypt: String?,
    val admin: Boolean,
    val timeZoneId: String,
    val score: Int
) {
    constructor(
        user: User,
        score: Int
    ) : this(
        user.id,
        user.instanceId,
        user.name,
        user.email,
        user.authCrypt,
        user.admin,
        user.timeZoneId,
        score
    )
}

class ApiUserLoader(val gradeDAO: GradeDAO) : BatchLoader<User, ApiUser> {
    override fun load(users: List<User>): CompletionStage<List<ApiUser>> {
        return CoroutineScope(Dispatchers.IO + MDCContext()).future {
            val lookup = gradeDAO.forUsers(users.map { it.id!! })
            users.map { user ->
                ApiUser(user, lookup[user.id]?.fold(0) { acc, grade ->
                    acc + grade.score()
                } ?: 0)
            }
        }
    }

}

class DataLoaderRegistryFactoryProvider @Inject constructor(
    val gradeDAO: GradeDAO
) : Provider<DataLoaderRegistryFactory> {
    override fun get(): DataLoaderRegistryFactory {
        return { _, _ ->
            val registry = DataLoaderRegistry()
            registry.register("usergrades", DataLoader.newDataLoader(ApiUserLoader(gradeDAO)))
            registry
        }
    }
}

/**
 * GraphQL entry point for queries.  Maps the DAO interfaces to the GraphQL models.
 */
class Query @Inject constructor(
    private val questionDAO: QuestionDAO,
    private val userDAO: UserDAO,
    private val responseDAO: ResponseDAO,
    private val gradeDAO: GradeDAO
) : GraphQLQueryResolver {

    fun user(context: GraphQLResourceContext): User? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return principal.user
        }
        return null
    }

    fun users(
        context: GraphQLResourceContext,
        dfe: DataFetchingEnvironment
    ): CompletableFuture<List<ApiUser>> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            val users = userDAO.getByInstance(principal.user.instanceId)
            return if (dfe.selectionSet.arguments.containsKey("score")) {
                dfe.getDataLoader<User, ApiUser>("usergrades").loadMany(users)
            } else {
                CompletableFuture.completedFuture(users.map { ApiUser(it, 0) })
            }
        }
        return CompletableFuture.completedFuture(emptyList<ApiUser>())
    }

    fun activeQuestions(context: GraphQLResourceContext): List<ApiQuestion> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return questionDAO.active(principal.user).map {
                ApiQuestion(
                    id = it.id,
                    authorId = it.authorId,
                    body = it.body,
                    answer = "",
                    ruleReferences = "",
                    activeAt = it.activeAt,
                    closedAt = it.closedAt,
                    responseDAO = responseDAO,
                    userDAO = userDAO,
                    questionDAO = questionDAO,
                    gradeDAO = gradeDAO
                )
            }
        }
        return emptyList()
    }

    fun closedQuestions(context: GraphQLResourceContext): List<ApiQuestion> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return questionDAO.closed(principal.user).map {
                ApiQuestion(it, responseDAO, userDAO, questionDAO, gradeDAO)
            }
        }
        return emptyList()
    }

    fun futureQuestions(context: GraphQLResourceContext): List<Question> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            if (principal.user.admin) {
                return questionDAO.future(principal.user)
            }
        }
        return emptyList()
    }

    fun responses(context: GraphQLResourceContext, includeGraded: Boolean): List<ApiResponse> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            if (principal.user.admin) {
                return responseDAO.forInstance(principal.user, includeGraded)
                    .map { ApiResponse(it, userDAO, questionDAO, gradeDAO) }
            }
        }
        return emptyList()
    }
}
