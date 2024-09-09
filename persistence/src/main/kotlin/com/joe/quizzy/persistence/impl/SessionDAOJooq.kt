package com.joe.quizzy.persistence.impl

import com.codahale.metrics.annotation.Timed
import com.joe.quizzy.api.models.Session
import com.joe.quizzy.persistence.api.SessionDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import com.joe.quizzy.persistence.impl.jooq.tables.records.SessionsRecord
import jakarta.inject.Inject
import mu.KotlinLogging
import org.jooq.DSLContext
import java.util.UUID
import java.util.stream.Stream

private val log = KotlinLogging.logger { }

/**
 * DAO implementation for Things
 */
open class SessionDAOJooq
    @Inject
    constructor(
        private val ctx: DSLContext,
    ) : SessionDAO {
        private fun getRecord(
            dsl: DSLContext,
            id: UUID,
        ): SessionsRecord? = dsl.selectFrom(Tables.SESSIONS).where(Tables.SESSIONS.ID.eq(id)).fetchOne()

        @Timed
        override fun get(id: UUID): Session? = getRecord(ctx, id)?.into(Session::class.java)

        @Timed
        override fun save(thing: Session): Session =
            ctx.transactionResult { config ->
                val thingId = thing.id
                val record =
                    if (thingId == null) {
                        config.dsl().newRecord(
                            Tables.SESSIONS,
                            thing,
                        )
                    } else {
                        val existing = getRecord(config.dsl(), thingId)
                        if (existing != null) {
                            existing.from(thing)
                            existing
                        } else {
                            config.dsl().newRecord(
                                Tables.SESSIONS,
                                thing,
                            )
                        }
                    }
                record.store()
                record.into(Session::class.java)
            }

        @Timed
        override fun all(): List<Session> = ctx.select().from(Tables.SESSIONS).fetchInto(Session::class.java)

        @Timed
        override fun stream(): Stream<Session> = ctx.select().from(Tables.SESSIONS).fetchStreamInto(Session::class.java)

        @Timed
        override fun delete(thing: Session): Int =
            ctx.transactionResult { config ->
                val query = config.dsl().deleteFrom(Tables.SESSIONS).where(Tables.SESSIONS.ID.eq(thing.id))
                log.info("$query")
                query.execute()
            }
    }
