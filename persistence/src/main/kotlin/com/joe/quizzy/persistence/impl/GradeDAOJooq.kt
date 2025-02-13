package com.joe.quizzy.persistence.impl

import com.codahale.metrics.annotation.Timed
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.persistence.api.GradeDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import com.joe.quizzy.persistence.impl.jooq.tables.records.GradesRecord
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import java.util.UUID
import java.util.stream.Stream

private val log = KotlinLogging.logger { }

/**
 * DAO implementation for Things
 */
open class GradeDAOJooq
    @Inject
    constructor(
        private val ctx: DSLContext,
    ) : GradeDAO {
        private fun getRecord(
            dsl: DSLContext,
            id: UUID,
        ): GradesRecord? = dsl.selectFrom(Tables.GRADES).where(Tables.GRADES.ID.eq(id)).fetchOne()

        @Timed
        override fun get(id: UUID): Grade? = getRecord(ctx, id)?.into(Grade::class.java)

        @Timed
        override fun save(thing: Grade): Grade =
            ctx.transactionResult { config ->
                save(thing, config)
            }

        fun save(
            thing: Grade,
            config: Configuration,
        ): Grade {
            val thingId = thing.id
            val record =
                if (thingId == null) {
                    config.dsl().newRecord(
                        Tables.GRADES,
                        thing,
                    )
                } else {
                    val existing = getRecord(config.dsl(), thingId)
                    if (existing != null) {
                        existing.from(thing)
                        existing
                    } else {
                        config.dsl().newRecord(
                            Tables.GRADES,
                            thing,
                        )
                    }
                }
            record.store()
            return record.into(Grade::class.java)
        }

        @Timed
        override fun forUser(userId: UUID): List<Grade> =
            ctx
                .select(Tables.GRADES.asterisk())
                .from(Tables.GRADES)
                .join(Tables.RESPONSES)
                .on(Tables.GRADES.RESPONSE_ID.eq(Tables.RESPONSES.ID))
                .where(Tables.RESPONSES.USER_ID.eq(userId))
                .fetchInto(Grade::class.java)

        @Timed
        override fun forUsers(
            userIds: List<UUID>,
            startTime: OffsetDateTime?,
            endTime: OffsetDateTime?,
        ): Map<UUID, List<Grade>> {
            val query =
                ctx
                    .select(Tables.RESPONSES.USER_ID, Tables.GRADES.asterisk())
                    .from(Tables.GRADES)
                    .join(Tables.RESPONSES)
                    .on(Tables.GRADES.RESPONSE_ID.eq(Tables.RESPONSES.ID))
                    .join(Tables.QUESTIONS)
                    .on(Tables.RESPONSES.QUESTION_ID.eq(Tables.QUESTIONS.ID))
                    .where(
                        DSL.and(
                            listOfNotNull(
                                Tables.RESPONSES.USER_ID.`in`(userIds),
                                startTime?.let {
                                    Tables.QUESTIONS.CLOSED_AT.ge(it)
                                },
                                endTime?.let {
                                    Tables.QUESTIONS.ACTIVE_AT.le(it)
                                },
                            ),
                        ),
                    )
            log.info { "get grades for users: $query" }
            return query
                .fetch()
                .intoGroups(Tables.RESPONSES.USER_ID, Grade::class.java)
        }

        @Timed
        override fun forResponse(responseId: UUID): Grade? {
            val query =
                ctx
                    .select(Tables.GRADES.asterisk())
                    .from(Tables.GRADES)
                    .where(Tables.GRADES.RESPONSE_ID.eq(responseId))
            log.info { "get grade for response: $query" }
            return query.fetchOneInto(Grade::class.java)
        }

        @Timed
        override fun forResponses(responseIds: List<UUID>): Map<UUID, Grade> {
            val query =
                ctx
                    .select(Tables.GRADES.asterisk())
                    .from(Tables.GRADES)
                    .where(Tables.GRADES.RESPONSE_ID.`in`(responseIds))
            log.info { "get grade for responses: $query" }
            return query
                .fetch()
                .intoMap(Tables.GRADES.RESPONSE_ID, Grade::class.java)
        }

        @Timed
        override fun all(): List<Grade> = ctx.select().from(Tables.GRADES).fetchInto(Grade::class.java)

        @Timed
        override fun stream(): Stream<Grade> = ctx.select().from(Tables.GRADES).fetchStreamInto(Grade::class.java)
    }
