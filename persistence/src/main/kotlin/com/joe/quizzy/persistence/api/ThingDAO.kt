package com.joe.quizzy.persistence.api

import com.joe.quizzy.api.models.Thing
import java.util.stream.Stream

/**
 * DAO for managing Things
 */
interface ThingDAO {
    fun all(): List<Thing>
    fun stream(): Stream<Thing>
    fun get(id: Int): Thing?
    fun save(thing: Thing): Thing
}
