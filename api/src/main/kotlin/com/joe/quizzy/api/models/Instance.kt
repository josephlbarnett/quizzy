package com.joe.quizzy.api.models

import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.UUID

data class Instance(
    val id: UUID?,
    val name: String,
    val status: String,
    @get:JsonIgnore @GraphQLIgnore val gmailRefreshToken: String? = null
)
