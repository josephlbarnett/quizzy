package com.joe.quizzy.server.auth

interface Hasher {
    fun hash(password: String): String

    fun verify(hash: String, password: String): Boolean
}
