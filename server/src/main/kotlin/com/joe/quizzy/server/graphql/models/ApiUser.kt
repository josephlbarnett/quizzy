package com.joe.quizzy.server.graphql.models

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
