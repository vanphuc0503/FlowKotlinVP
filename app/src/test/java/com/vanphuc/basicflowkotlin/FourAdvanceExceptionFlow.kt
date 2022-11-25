package com.vanphuc.basicflowkotlin

import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FourAdvanceExceptionFlow {
    /**
     * Everything is caught
     */
    private fun simple(): Flow<String> =
        flow {
            for (i in 1..3) {
                println("Emitting $i")
                emit(i) // emit next value
            }
        }
            .map { value ->
                check(value <= 1) { "Crashed on $value" }
                "string $value"
            }

    @Test
    fun testTryCatch() = runBlocking<Unit> {
        try {
            simple()
//                .catch { e -> emit("Caught $e") } // emit on exception
                .collect { value -> println(value) }
        } catch (e: Throwable) {
            println("Caught $e")
        }
    }

    /**
     * Exception transparency
     */
    fun simple2(): Flow<Int> = flow {
        for (i in 1..3) {
            println("Emitting $i")
            emit(i)
        }
    }

    @Test
    fun testExceptionTransparency() = runBlocking<Unit> {
        simple2()
            .catch { e ->
                println("Caught $e")
            } // does not catch downstream exceptions
            .collect { value ->
                check(value <= 1) { "Collected $value" }
                println(value)
            }
    }

    /**
     * Catching declaratively
     */
    @Test
    fun testCatchingDeclaratively() = runBlocking<Unit> {
        simple2()
            .onEach { value ->
                check(value <= 1) { "Collected $value" }
                println(value)
            }
            .catch { e -> println("Caught $e") }
            .collect()
    }

    /**
     * Flow completion
     */
    fun simple3(): Flow<Int> = flow {
        emit(1)
        throw RuntimeException()
    }

    @Test
    fun testDeclarativeHandling() = runBlocking<Unit> {
        simple3()
            .onCompletion { cause -> if (cause != null) println("Flow completed exceptionally") }
            .catch { cause -> println("Caught exception") }
            .collect { value -> println(value) }
    }

    /**
     * Successful completion
     */
    @Test
    fun testSuccessfulCompletion() = runBlocking<Unit> {
        val simple = (1..3).asFlow()
        simple
            .onCompletion { cause -> println("Flow completed with $cause") }
            .collect { value ->
                check(value <= 1) { "Collected $value" }
                println(value)
            }
    }

    /**
     * Launching Flow
     */
    @Test
    fun testLaunchingFlow() = runBlocking {
        val events = (1..3).asFlow().onEach { delay(100) }
        events.onEach { event ->
            println("Event: $event")
        }
            .launchIn(this)
        println("Done")
    }

    /**
     * Flow cancellation checks
     */
    fun foo(): Flow<Int> = flow {
        for (i in 1..5) {
            println("Emitting $i")
            emit(i)
        }
    }

    @Test
    fun testFlowCancellationChecks() = runBlocking {
        foo().collect { value ->
            if (value == 3) cancel()
            println(value)
        }
    }

    /**
     *  IntRange.asFlow not cancel
     */

    @Test
    fun testNotCancelException() = runBlocking<Unit> {
        (1..5).asFlow().collect { value ->
            if (value == 3) cancel()
            println(value)
        }
    }

    @Test
    fun testCancelException() = runBlocking<Unit> {
        (1..5).asFlow().cancellable().collect { value ->
            if (value == 3) cancel()
            println(value)
        }
    }
}