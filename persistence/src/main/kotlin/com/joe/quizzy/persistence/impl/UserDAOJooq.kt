package com.joe.quizzy.persistence.impl

import com.codahale.metrics.annotation.Timed
import com.joe.quizzy.api.models.User
import com.joe.quizzy.persistence.api.UserDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import com.joe.quizzy.persistence.impl.jooq.tables.records.UsersRecord
import java.util.UUID
import java.util.stream.Stream
import javax.inject.Inject
import mu.KotlinLogging
import org.jooq.DSLContext

private val log = KotlinLogging.logger { }

/**
 * DAO implementation for Things
 */
open class UserDAOJooq
@Inject constructor(
    private val ctx: DSLContext
) : UserDAO {
    private fun getRecord(dsl: DSLContext, id: UUID): UsersRecord? {
        return dsl.selectFrom(Tables.USERS).where(Tables.USERS.ID.eq(id)).fetchOne()
    }

    @Timed
    override fun get(id: UUID): User? {
        return getRecord(ctx, id)?.into(User::class.java)
    }

    @Timed
    override fun getByEmail(email: String): User? {
        return ctx.selectFrom(Tables.USERS).where(Tables.USERS.EMAIL.eq(email)).fetchOne()?.into(User::class.java)
    }

    @Timed
    override fun save(thing: User): User {
        return ctx.transactionResult { config ->
            val thingId = thing.id
            val record = if (thingId == null) {
                config.dsl().newRecord(
                    Tables.USERS,
                    thing
                )
            } else {
                val existing = getRecord(config.dsl(), thingId)
                if (existing != null) {
                    existing.from(thing)
                    existing
                } else {
                    config.dsl().newRecord(
                        Tables.USERS,
                        thing
                    )
                }
            }
            record.store()
            record.into(User::class.java)
        }
    }

    @Timed
    override fun all(): List<User> {
        return ctx.select().from(Tables.USERS).fetchInto(User::class.java)
    }

    @Timed
    override fun stream(): Stream<User> {
        return ctx.select().from(Tables.USERS).fetchSize(1000).fetchStreamInto(User::class.java)
    }
}
