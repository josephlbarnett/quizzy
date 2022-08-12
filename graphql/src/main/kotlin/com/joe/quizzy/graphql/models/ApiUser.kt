package com.joe.quizzy.graphql.models

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.User
import graphql.schema.DataFetchingEnvironment
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Like a [User] but can dynamically calculate the overall score of [Response]s
 */
data class ApiUser(
    val id: UUID?,
    val instanceId: UUID,
    val name: String,
    val email: String,
    val admin: Boolean,
    val timeZoneId: String,
    val notifyViaEmail: Boolean,
    @GraphQLIgnore
    private val defaultScore: Int
) {
    constructor(
        user: User,
        defaultScore: Int
    ) : this(
        user.id,
        user.instanceId,
        user.name,
        user.email,
        user.admin,
        user.timeZoneId,
        user.notifyViaEmail,
        defaultScore
    )

    fun score(dfe: DataFetchingEnvironment): CompletableFuture<Int> {
        return dfe.getDataLoader<UUID, List<Grade>>("usergrades").load(id).thenApply {
            it?.map { g -> ApiGrade(g, defaultScore).score() }?.fold(0, Int::plus) ?: 0
        }
    }

    fun instance(dfe: DataFetchingEnvironment): CompletableFuture<Instance> {
        return dfe.getDataLoader<UUID, Instance>("batchinstances").load(instanceId)
    }
}
