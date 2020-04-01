package com.joe.quizzy.persistence.impl

import com.codahale.metrics.annotation.Timed
import com.joe.quizzy.api.models.Response
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.ResponseDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import com.joe.quizzy.persistence.impl.jooq.tables.records.ResponsesRecord
import java.util.UUID
import java.util.stream.Stream
import javax.inject.Inject
import mu.KotlinLogging
import org.jooq.DSLContext

private val log = KotlinLogging.logger { }

/**
 * DAO implementation for Things
 */
open class ResponseDAOJooq
@Inject constructor(
    private val ctx: DSLContext
) : ResponseDAO {
    private fun getRecord(dsl: DSLContext, id: UUID): ResponsesRecord? {
        return dsl.selectFrom(Tables.RESPONSES).where(Tables.RESPONSES.ID.eq(id)).fetchOne()
    }

    @Timed
    override fun get(id: UUID): Response? {
        return getRecord(ctx, id)?.into(Response::class.java)
    }

    @Timed
    override fun save(thing: Response): Response {
        return ctx.transactionResult { config ->
            val thingId = thing.id
            val record = if (thingId == null) {
                config.dsl().newRecord(
                    Tables.RESPONSES,
                    thing
                )
            } else {
                val existing = getRecord(config.dsl(), thingId)
                if (existing != null) {
                    existing.from(thing)
                    existing
                } else {
                    config.dsl().newRecord(
                        Tables.RESPONSES,
                        thing
                    )
                }
            }
            record.store()
            record.into(Response::class.java)
        }
    }

    override fun byUserQuestion(user: User, questionId: UUID): Response? {
        val query = ctx.select(Tables.RESPONSES.asterisk()).from(Tables.RESPONSES)
            .where(Tables.RESPONSES.USER_ID.eq(user.id).and(Tables.RESPONSES.QUESTION_ID.eq(questionId)))
        log.info("responses query : $query")
        return query.fetchOneInto(Response::class.java)
    }

    @Timed
    override fun all(): List<Response> {
        return ctx.select().from(Tables.RESPONSES).fetchInto(Response::class.java)
    }

    @Timed
    override fun stream(): Stream<Response> {
        return ctx.select().from(Tables.RESPONSES).fetchSize(1000).fetchStreamInto(Response::class.java)
    }
}
