package com.joe.quizzy.graphql.auth

import de.mkammerer.argon2.Argon2Factory

private const val ITERATIONS = 100
private const val MEMORY = 1024
private const val PARALLELISM = 2

class Argon2Hasher : Hasher {
    private val argon = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

    override fun hash(password: String): String {
        return argon.hash(ITERATIONS, MEMORY, PARALLELISM, password.toCharArray())
    }

    override fun verify(
        hash: String,
        password: String,
    ): Boolean {
        return argon.verify(hash, password.toCharArray())
    }
}
