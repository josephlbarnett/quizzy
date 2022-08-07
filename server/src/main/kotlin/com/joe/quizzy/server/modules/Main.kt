package com.joe.quizzy.server.modules

import com.trib3.server.main as leakyCauldronMain

/**
 * Workaround for running pebbles in intelliJ under java11+:
 * We appear to hit https://youtrack.jetbrains.com/issue/IDEA-277313
 * even though a dup of it's marked as fixed when running via the LC main,
 * so wrap it with a main per the workaround commented in the bug.
 */
fun main() {
    leakyCauldronMain()
}
