package com.joe.quizzy.graphql.models

import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.api.models.QuestionType
import com.joe.quizzy.api.models.Season
import com.joe.quizzy.graphql.dataloaders.InstanceTimePeriod
import com.joe.quizzy.graphql.groupme.GroupMeService
import graphql.schema.DataFetchingEnvironment
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

data class ApiInstance(
    val id: UUID?,
    val name: String,
    val status: String,
    val defaultQuestionType: QuestionType = QuestionType.SHORT_ANSWER,
    val autoGrade: Boolean = false,
    val defaultScore: Int = 15,
) {
    constructor(instance: Instance) : this(
        instance.id,
        instance.name,
        instance.status,
        instance.defaultQuestionType,
        instance.autoGrade,
        instance.defaultScore,
    )

    fun seasons(
        dfe: DataFetchingEnvironment,
        startTime: OffsetDateTime? = null,
        endTime: OffsetDateTime? = null,
    ): CompletableFuture<List<Season>> {
        return if (id != null) {
            dfe.getDataLoader<InstanceTimePeriod, List<Season>>("instanceseasons")
                .load(InstanceTimePeriod(id, startTime, endTime))
                .thenApply { it.orEmpty().sortedBy { s -> s.startTime } }
        } else {
            CompletableFuture.completedFuture(emptyList())
        }
    }

    fun supportsGroupMe(dfe: DataFetchingEnvironment): CompletableFuture<Boolean> {
        return if (id != null) {
            dfe.getDataLoader<UUID, GroupMeService?>("groupmeservice").load(id).thenApply { it != null }
        } else {
            CompletableFuture.completedFuture(false)
        }
    }
}
