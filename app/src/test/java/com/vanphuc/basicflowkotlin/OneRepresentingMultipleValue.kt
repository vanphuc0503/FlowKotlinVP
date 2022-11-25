package com.vanphuc.basicflowkotlin

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

class OneRepresentingMultipleValue {

    /**
     * Sequences
     */
    fun simple(): Sequence<Int> = sequence { // sequence builder
        for (i in 1..3) {
            Thread.sleep(100) // pretend we are computing it
            yield(i) // yield next value
        }
    }

    @Test
    fun testSequences() {
        simple().forEach { value -> println(value) }
    }

    /**
     * Suspending functions﻿
     */
    suspend fun simple2(): List<Int> {
        delay(1000) // pretend we are doing something asynchronous here
        return listOf(1, 2, 3)
    }

    @Test
    fun testSuspendingFunctions() {
        runBlocking {
            simple2().forEach { value -> println(value) }
        }
    }

    /**
     * Flows
     */
    fun simple3(): Flow<Int> = flow { // flow builder
        for (i in 1..3) {
            delay(100) // pretend we are doing something useful here
            emit(i) // emit next value
        }
    }

    @Test
    fun testFlows() {
        runBlocking {
            // Launch a concurrent coroutine to check if the main thread is blocked
            launch {
                for (k in 1..3) {
                    println("I'm not blocked $k")
                    delay(100)
                }
            }
            // Collect the flow
            simple3().collect { value -> println(value) }
        }
    }
}