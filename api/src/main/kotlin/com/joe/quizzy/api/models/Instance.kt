package com.joe.quizzy.api.models

import java.util.UUID

data class Instance(
    val id: UUID?,
    val name: String,
    val status: String
)
