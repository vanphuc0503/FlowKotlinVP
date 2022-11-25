package com.vanphuc.basicflowkotlin

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test

class TwoBasicFlow {
    /**
     * Flows are cold
     */
    suspend fun simple(): Flow<Int> = flow {
        println("Flow started")
        for (i in 1..3) {
            delay(100)
            emit(i)
        }
    }

    @Test
    fun testFlowsCold() = runBlocking {
        println("Calling simple function...")
        val flow = simple()
        println("Calling collect...")
        flow.collect { value -> println(value) }
        println("Calling collect again...")
        flow.collect { value -> println(value) }
    }

    /**
     * Flow cancellation basics
     */

    fun simple2(): Flow<Int> = flow {
        for (i in 1..3) {
            delay(100)
            println("Emitting $i")
            emit(i)
        }
    }

    @Test
    fun testFlowsCancellationBasics() = runBlocking<Unit> {
        withTimeoutOrNull(250) { // Timeout after 250ms
            simple2().collect { value -> println(value) }
        }
        println("Done")
    }

    /**
     * Flow Builder
     */
    @Test
    fun testFlowBuilder() = runBlocking {
        (1..3).asFlow().collect { value -> println(value) }
    }

    /**
     * Intermediate flow operators
     */
    suspend fun performRequest(request: Int): String {
        delay(1000) // imitate long-running asynchronous work
        return "response $request"
    }

    @Test
    fun testFlowOperators() = runBlocking<Unit> {
        (1..3).asFlow() // a flow of requests
            .map { request ->
                performRequest(request)
            }
            .collect { response ->
                println(response)
            }
    }

    /**
     * Transform operator
     */
    @Test
    fun testTransformOperator() = runBlocking<Unit> {
        (1..3).asFlow() // a flow of requests
            .transform { request ->
                emit("Making request $request")
                emit(performRequest(request))
            }
            .collect { response ->
                println(response)
            }
    }

    /**
     * Size-limiting operators
     */
    fun numbers(): Flow<Int> = flow {
        try {
            emit(1)
            emit(2)
            println("This line will not execute")
            emit(3)
        } finally {
            println("Finally in numbers")
        }
    }

    @Test
    fun testSize_limitingOperator() = runBlocking<Unit> {
        numbers()
            .take(2) // take only the first two
            .collect { value ->
                println(value)
            }
    }

    /**
     * Terminal flow operators
     */
    @Test
    fun testTerminalFlowOperators() = runBlocking<Unit> {
        val sum = (1..5).asFlow()
            .map {
                it * it
            } // squares of numbers from 1 to 5
            .reduce { a, b ->
                a + b
            } // sum them (terminal operator)
        println(sum)
    }

    /**
     * Flow Sequential
     */

    @Test
    fun testSequential() = runBlocking {
        (1..5).asFlow()
            .filter {
                println("Filter $it")
                it % 2 == 0
            }
            .map {
                println("Map $it")
                "string $it"
            }.collect {
                println("Collect $it")
            }
    }
}