package com.joe.quizzy.server.graphql

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.Question
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.GradeDAO
import com.joe.quizzy.persistence.api.InstanceDAO
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
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.DataLoader
import org.dataloader.DataLoaderOptions
import org.dataloader.DataLoaderRegistry
import org.dataloader.MappedBatchLoader
import org.dataloader.MappedBatchLoaderWithContext
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
    val closedAt: OffsetDateTime
) {
    constructor(
        question: Question
    ) : this(
        question.id,
        question.authorId,
        question.body,
        question.answer,
        question.ruleReferences,
        question.activeAt,
        question.closedAt
    )

    fun response(context: GraphQLResourceContext, dfe: DataFetchingEnvironment): CompletableFuture<ApiResponse?> {
        val principal = context.principal
        if (principal is UserPrincipal && id != null) {
            return dfe.getDataLoader<UUID, Response>("questionresponses")
                .load(id)
                .thenApply { it?.let(::ApiResponse) }
        }
        return CompletableFuture.completedFuture(null)
    }

    fun author(dfe: DataFetchingEnvironment): CompletableFuture<ApiUser?> {
        return dfe.getDataLoader<UUID, User>("batchusers").load(authorId).thenApply { it?.let(::ApiUser) }
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
    val ruleReferences: String
) {
    constructor(response: Response) :
        this(
            response.id,
            response.userId,
            response.questionId,
            response.response,
            response.ruleReferences
        )

    fun user(context: GraphQLResourceContext, dfe: DataFetchingEnvironment): CompletableFuture<ApiUser?> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return dfe.getDataLoader<UUID, User>("batchusers").load(userId).thenApply { ApiUser(it) }
        }
        return CompletableFuture.completedFuture(null)
    }

    fun question(context: GraphQLResourceContext, dfe: DataFetchingEnvironment): CompletableFuture<ApiQuestion?> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return dfe.getDataLoader<UUID, Question>("batchquestions").load(questionId).thenApply { ApiQuestion(it) }
        }
        return CompletableFuture.completedFuture(null)
    }

    fun grade(context: GraphQLResourceContext, dfe: DataFetchingEnvironment): CompletableFuture<Grade?> {
        val principal = context.principal
        if (principal is UserPrincipal && id != null) {
            return dfe.getDataLoader<UUID, Grade>("responsegrades").load(id)
        }
        return CompletableFuture.completedFuture(null)
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
    val timeZoneId: String
) {
    constructor(
        user: User
    ) : this(
        user.id,
        user.instanceId,
        user.name,
        user.email,
        user.authCrypt,
        user.admin,
        user.timeZoneId
    )

    fun score(dfe: DataFetchingEnvironment): CompletableFuture<Int> {
        return dfe.getDataLoader<UUID, List<Grade>>("usergrades").load(id).thenApply {
            it?.map(Grade::score)?.fold(0, Int::plus) ?: 0
        }
    }

    fun instance(dfe: DataFetchingEnvironment): CompletableFuture<Instance> {
        return dfe.getDataLoader<UUID, Instance>("batchinstances").load(instanceId)
    }
}

/**
 * Batch load User ID -> List<Grade>
 */
class UserGradeLoader(val gradeDAO: GradeDAO) : MappedBatchLoader<UUID, List<Grade>> {
    override fun load(userIds: Set<UUID>): CompletionStage<Map<UUID, List<Grade>>> {
        return CoroutineScope(Dispatchers.IO + MDCContext()).future {
            gradeDAO.forUsers(userIds.toList())
        }
    }
}

/**
 * Batch load Response ID -> Grade
 */
class ResponseGradeLoader(val gradeDAO: GradeDAO) : MappedBatchLoader<UUID, Grade> {
    override fun load(keys: Set<UUID>): CompletionStage<Map<UUID, Grade>> {
        return CoroutineScope(Dispatchers.IO + MDCContext()).future {
            gradeDAO.forResponses(keys.toList())
        }
    }

}

/**
 * Batch load Users by ID
 */
class BatchUserLoader(val userDAO: UserDAO) : MappedBatchLoader<UUID, User> {
    override fun load(keys: Set<UUID>): CompletionStage<Map<UUID, User>> {
        return CoroutineScope(Dispatchers.IO + MDCContext()).future {
            userDAO.get(keys.toList()).associateBy { it.id!! }
        }
    }
}

/**
 * Batch load Questions by ID
 */
class BatchQuestionLoader(val questionDAO: QuestionDAO) : MappedBatchLoader<UUID, Question> {
    override fun load(keys: Set<UUID>): CompletionStage<Map<UUID, Question>> {
        return CoroutineScope(Dispatchers.IO + MDCContext()).future {
            questionDAO.get(keys.toList()).associateBy { it.id!! }
        }
    }
}

/**
 * Batch load Question ID -> Response for context User
 */
class QuestionResponseLoader(val responseDAO: ResponseDAO) : MappedBatchLoaderWithContext<UUID, Response> {
    override fun load(
        keys: Set<UUID>,
        environment: BatchLoaderEnvironment
    ): CompletionStage<Map<UUID, Response>> {
        return CoroutineScope(Dispatchers.IO + MDCContext()).future {
            val principal = environment.getContext<GraphQLResourceContext>().principal
            if (principal is UserPrincipal) {
                responseDAO.byUserQuestions(principal.user.id!!, keys.toList())
            } else {
                emptyMap()
            }
        }
    }
}

/**
 * Batch load Instance by Id
 */
class BulkInstanceLoader(val instanceDAO: InstanceDAO) : MappedBatchLoader<UUID, Instance> {
    override fun load(keys: Set<UUID>): CompletionStage<Map<UUID, Instance>> {
        return CoroutineScope(Dispatchers.IO + MDCContext()).future {
            instanceDAO.get(keys.toList()).associateBy { it.id!! }
        }
    }
}

/**
 * Provide a DataLoaderRegistry per request that forwards
 * the graphQL context object to each dataloader registered.
 * DataLoaders that fetch child objects are MappedDataLoaders,
 * DataLoaders that fetch parents are regular DataLoaders
 */
class DataLoaderRegistryFactoryProvider @Inject constructor(
    private val gradeDAO: GradeDAO,
    private val userDAO: UserDAO,
    private val questionDAO: QuestionDAO,
    private val responseDAO: ResponseDAO,
    private val instanceDAO: InstanceDAO
) : Provider<DataLoaderRegistryFactory> {
    override fun get(): DataLoaderRegistryFactory {
        return { _, context ->
            val registry = DataLoaderRegistry()
            val dataLoaderOptions = DataLoaderOptions.newOptions().setBatchLoaderContextProvider {
                context
            }
            registry.register(
                "responsegrades",
                DataLoader.newMappedDataLoader(ResponseGradeLoader(gradeDAO), dataLoaderOptions)
            )
            registry.register(
                "usergrades",
                DataLoader.newMappedDataLoader(UserGradeLoader(gradeDAO), dataLoaderOptions)
            )
            registry.register(
                "batchusers",
                DataLoader.newMappedDataLoader(BatchUserLoader(userDAO), dataLoaderOptions)
            )
            registry.register(
                "batchquestions",
                DataLoader.newMappedDataLoader(BatchQuestionLoader(questionDAO), dataLoaderOptions)
            )
            registry.register(
                "questionresponses",
                DataLoader.newMappedDataLoader(QuestionResponseLoader(responseDAO), dataLoaderOptions)
            )
            registry.register(
                "batchinstances",
                DataLoader.newMappedDataLoader(BulkInstanceLoader(instanceDAO), dataLoaderOptions)
            )
        }
    }
}

/**
 * GraphQL entry point for queries.  Maps the DAO interfaces to the GraphQL models.
 */
class Query @Inject constructor(
    private val questionDAO: QuestionDAO,
    private val userDAO: UserDAO,
    private val responseDAO: ResponseDAO
) : GraphQLQueryResolver {

    fun user(context: GraphQLResourceContext): ApiUser? {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return ApiUser(principal.user)
        }
        return null
    }

    fun users(
        context: GraphQLResourceContext
    ): List<ApiUser> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return userDAO.getByInstance(principal.user.instanceId).map { ApiUser(it) }
        }
        return emptyList()
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
                    closedAt = it.closedAt
                )
            }
        }
        return emptyList()
    }

    fun closedQuestions(context: GraphQLResourceContext): List<ApiQuestion> {
        val principal = context.principal
        if (principal is UserPrincipal) {
            return questionDAO.closed(principal.user).map { ApiQuestion(it) }
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
                return responseDAO.forInstance(principal.user.instanceId, includeGraded).map { ApiResponse(it) }
            }
        }
        return emptyList()
    }
}
