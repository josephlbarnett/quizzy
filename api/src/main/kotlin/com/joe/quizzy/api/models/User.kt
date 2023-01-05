package com.joe.quizzy.api.models

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.UUID

data class User(
    val id: UUID?,
    val instanceId: UUID,
    val name: String,
    val email: String,
    @get:JsonIgnore @GraphQLIgnore
    val authCrypt: String?,
    val admin: Boolean,
    val timeZoneId: String,
    val notifyViaEmail: Boolean = true,
    @get:JsonIgnore @GraphQLIgnore
    val passwordResetToken: String? = null,
)
