package com.joe.quizzy.persistence.api

import com.joe.quizzy.api.models.Season
import java.time.OffsetDateTime
import java.util.UUID

interface SeasonDAO {
    fun getSeasons(
        instanceIds: List<UUID>,
        startTime: OffsetDateTime? = null,
        endTime: OffsetDateTime? = null,
    ): Map<UUID, List<Season>>

    fun getSeasons(
        instanceId: UUID,
        startTime: OffsetDateTime? = null,
        endTime: OffsetDateTime? = null,
    ): List<Season> = getSeasons(listOf(instanceId), startTime, endTime)[instanceId].orEmpty()
}
