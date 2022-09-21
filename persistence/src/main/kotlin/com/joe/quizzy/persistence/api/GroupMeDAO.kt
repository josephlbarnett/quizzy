package com.joe.quizzy.persistence.api

import java.util.UUID

data class GroupMeInfo(val instanceId: UUID, val groupId: String, val apiKey: String)

interface GroupMeInfoDAO {
    fun get(instanceId: UUID): GroupMeInfo?
}
