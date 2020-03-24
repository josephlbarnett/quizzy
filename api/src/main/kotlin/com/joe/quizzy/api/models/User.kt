package com.joe.quizzy.api.models

import java.util.UUID

data class User(
    val id: UUID?,
    val instanceId: UUID,
    val name: String,
    val email: String,
    val authCrypt: String?,
    val admin: Boolean,
    val timeZoneId: String
)
