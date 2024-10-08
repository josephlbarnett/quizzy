package com.joe.quizzy.persistence.impl

import com.codahale.metrics.annotation.Timed
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import com.joe.quizzy.persistence.impl.jooq.tables.records.InstancesRecord
import jakarta.inject.Inject
import mu.KotlinLogging
import org.jooq.DSLContext
import java.util.UUID
import java.util.stream.Stream

private val log = KotlinLogging.logger { }

/**
 * DAO implementation for Things
 */
open class InstanceDAOJooq
    @Inject
    constructor(
        private val ctx: DSLContext,
    ) : InstanceDAO {
        private fun getRecord(
            dsl: DSLContext,
            id: UUID,
        ): InstancesRecord? = dsl.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(id)).fetchOne()

        @Timed
        override fun get(id: UUID): Instance? = getRecord(ctx, id)?.into(Instance::class.java)

        @Timed
        override fun get(ids: List<UUID>): List<Instance> {
            val query = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.`in`(ids))
            log.info("batch get instances: $query")
            return query.fetchInto(Instance::class.java)
        }

        @Timed
        override fun save(thing: Instance): Instance =
            ctx.transactionResult { config ->
                val thingId = thing.id
                val record =
                    if (thingId == null) {
                        config.dsl().newRecord(
                            Tables.INSTANCES,
                            thing,
                        )
                    } else {
                        val existing = getRecord(config.dsl(), thingId)
                        if (existing != null) {
                            existing.from(thing)
                            existing
                        } else {
                            config.dsl().newRecord(
                                Tables.INSTANCES,
                                thing,
                            )
                        }
                    }
                record.store()
                record.into(Instance::class.java)
            }

        @Timed
        override fun all(): List<Instance> = ctx.select().from(Tables.INSTANCES).fetchInto(Instance::class.java)

        @Timed
        override fun stream(): Stream<Instance> =
            ctx.select().from(Tables.INSTANCES).fetchStreamInto(Instance::class.java)
    }
