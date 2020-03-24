package com.joe.quizzy.persistence.impl

import com.codahale.metrics.annotation.Timed
import com.joe.quizzy.api.models.Thing
import com.joe.quizzy.persistence.api.ThingDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import com.joe.quizzy.persistence.impl.jooq.tables.records.ThingsRecord
import java.util.stream.Stream
import javax.inject.Inject
import mu.KotlinLogging
import org.jooq.DSLContext

private val log = KotlinLogging.logger { }

/**
 * DAO implementation for Things
 */
open class ThingDAOJooq
@Inject constructor(
    private val ctx: DSLContext
) : ThingDAO {
    private fun getRecord(dsl: DSLContext, id: Int): ThingsRecord {
        return dsl.selectFrom(Tables.THINGS).where(Tables.THINGS.ID.eq(id)).fetchOne()
    }

    @Timed
    override fun get(id: Int): Thing? {
        return getRecord(ctx, id).into(Thing::class.java)
    }

    @Timed
    override fun save(thing: Thing): Thing {
        return ctx.transactionResult { config ->
            val thingId = thing.id
            val record = if (thingId == null) {
                config.dsl().newRecord(
                    Tables.THINGS,
                    thing
                )
            } else {
                val existing = getRecord(config.dsl(), thingId)
                existing.from(thing)
                existing
            }
            record.store()
            record.into(Thing::class.java)
        }
    }

    @Timed
    override fun all(): List<Thing> {
        return ctx.select().from(Tables.THINGS).fetchInto(Thing::class.java)
    }

    @Timed
    override fun stream(): Stream<Thing> {
        return ctx.select().from(Tables.THINGS).fetchSize(1000).fetchStreamInto(Thing::class.java)
    }
}
