package com.joe.quizzy.persistence.impl

import com.joe.quizzy.api.models.Season
import com.joe.quizzy.persistence.api.SeasonDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import jakarta.inject.Inject
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import java.util.UUID

open class SeasonDAOJooq
@Inject constructor(
    private val ctx: DSLContext,
) : SeasonDAO {
    override fun getSeasons(
        instanceIds: List<UUID>,
        startTime: OffsetDateTime?,
        endTime: OffsetDateTime?,
    ): Map<UUID, List<Season>> {
        return ctx.select().from(Tables.SEASONS)
            .where(
                DSL.and(
                    listOfNotNull(
                        Tables.SEASONS.INSTANCE_ID.`in`(instanceIds),
                        startTime?.let { Tables.SEASONS.END_TIME.ge(it) },
                        endTime?.let { Tables.SEASONS.START_TIME.le(it) },
                    ),
                ),
            )
            .fetch()
            .intoGroups(Tables.SEASONS.INSTANCE_ID, Season::class.java)
    }
}
