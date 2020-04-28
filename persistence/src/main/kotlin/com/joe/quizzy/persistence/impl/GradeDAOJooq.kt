package com.joe.quizzy.persistence.impl

import com.codahale.metrics.annotation.Timed
import com.joe.quizzy.api.models.Grade
import com.joe.quizzy.persistence.api.GradeDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import com.joe.quizzy.persistence.impl.jooq.tables.records.GradesRecord
import java.util.UUID
import java.util.stream.Stream
import javax.inject.Inject
import mu.KotlinLogging
import org.jooq.DSLContext

private val log = KotlinLogging.logger { }

/**
 * DAO implementation for Things
 */
open class GradeDAOJooq
@Inject constructor(
    private val ctx: DSLContext
) : GradeDAO {
    private fun getRecord(dsl: DSLContext, id: UUID): GradesRecord? {
        return dsl.selectFrom(Tables.GRADES).where(Tables.GRADES.ID.eq(id)).fetchOne()
    }

    @Timed
    override fun get(id: UUID): Grade? {
        return getRecord(ctx, id)?.into(Grade::class.java)
    }

    @Timed
    override fun save(thing: Grade): Grade {
        return ctx.transactionResult { config ->
            val thingId = thing.id
            val record = if (thingId == null) {
                config.dsl().newRecord(
                    Tables.GRADES,
                    thing
                )
            } else {
                val existing = getRecord(config.dsl(), thingId)
                if (existing != null) {
                    existing.from(thing)
                    existing
                } else {
                    config.dsl().newRecord(
                        Tables.GRADES,
                        thing
                    )
                }
            }
            record.store()
            record.into(Grade::class.java)
        }
    }

    override fun forUser(userId: UUID): List<Grade> {
        return ctx.select(Tables.GRADES.asterisk())
            .from(Tables.GRADES)
            .join(Tables.RESPONSES).on(Tables.GRADES.RESPONSE_ID.eq(Tables.RESPONSES.ID))
            .where(Tables.RESPONSES.USER_ID.eq(userId)).fetchInto(Grade::class.java)
    }

    override fun forUsers(userIds: List<UUID>): Map<UUID, List<Grade>> {
        val query = ctx.select(Tables.RESPONSES.USER_ID, Tables.GRADES.asterisk())
            .from(Tables.GRADES)
            .join(Tables.RESPONSES).on(Tables.GRADES.RESPONSE_ID.eq(Tables.RESPONSES.ID))
            .where(Tables.RESPONSES.USER_ID.`in`(userIds))
        log.info("get grades for users: $query")
        return query.fetch()
            .intoGroups(Tables.RESPONSES.USER_ID, Grade::class.java)
    }

    override fun forResponse(responseId: UUID): Grade? {
        return ctx.select(Tables.GRADES.asterisk())
            .from(Tables.GRADES)
            .where(Tables.GRADES.RESPONSE_ID.eq(responseId)).fetchOneInto(Grade::class.java)
    }

    @Timed
    override fun all(): List<Grade> {
        return ctx.select().from(Tables.GRADES).fetchInto(Grade::class.java)
    }

    @Timed
    override fun stream(): Stream<Grade> {
        return ctx.select().from(Tables.GRADES).fetchSize(1000).fetchStreamInto(Grade::class.java)
    }
}
