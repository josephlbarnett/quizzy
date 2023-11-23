package com.joe.quizzy.persistence.impl

import com.joe.quizzy.persistence.api.GroupMeInfo
import com.joe.quizzy.persistence.api.GroupMeInfoDAO
import com.joe.quizzy.persistence.impl.jooq.Tables
import jakarta.inject.Inject
import org.jooq.DSLContext
import java.util.UUID

open class GroupMeInfoDAOJooq
@Inject constructor(
    private val ctx: DSLContext,
) : GroupMeInfoDAO {
    override fun get(instanceId: UUID): GroupMeInfo? {
        return ctx.select().from(Tables.GROUPME_INFO).where(Tables.GROUPME_INFO.INSTANCE_ID.eq(instanceId))
            .fetchOneInto(GroupMeInfo::class.java)
    }
}
