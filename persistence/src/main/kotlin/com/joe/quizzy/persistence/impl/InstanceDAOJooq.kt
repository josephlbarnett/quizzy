package com.joe.quizzy.persistence.impl

import com.codahale.metrics.annotation.Timed
import com.joe.quizzy.api.models.Instance
import com.joe.quizzy.persistence.api.InstanceDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import com.joe.quizzy.persistence.impl.jooq.tables.records.InstancesRecord
import java.util.UUID
import java.util.stream.Stream
import javax.inject.Inject
import mu.KotlinLogging
import org.jooq.DSLContext

private val log = KotlinLogging.logger { }

/**
 * DAO implementation for Things
 */
open class InstanceDAOJooq
@Inject constructor(
    private val ctx: DSLContext
) : InstanceDAO {
    private fun getRecord(dsl: DSLContext, id: UUID): InstancesRecord? {
        return dsl.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(id)).fetchOne()
    }

    @Timed
    override fun get(id: UUID): Instance? {
        return getRecord(ctx, id)?.into(Instance::class.java)
    }

    @Timed
    override fun save(thing: Instance): Instance {
        return ctx.transactionResult { config ->
            val thingId = thing.id
            val record = if (thingId == null) {
                config.dsl().newRecord(
                    Tables.INSTANCES,
                    thing
                )
            } else {
                val existing = getRecord(config.dsl(), thingId)
                if (existing != null) {
                    existing.from(thing)
                    existing
                } else {
                    config.dsl().newRecord(
                        Tables.INSTANCES,
                        thing
                    )
                }
            }
            record.store()
            record.into(Instance::class.java)
        }
    }

    @Timed
    override fun all(): List<Instance> {
        return ctx.select().from(Tables.INSTANCES).fetchInto(Instance::class.java)
    }

    @Timed
    override fun stream(): Stream<Instance> {
        return ctx.select().from(Tables.INSTANCES).fetchSize(1000).fetchStreamInto(Instance::class.java)
    }
}
