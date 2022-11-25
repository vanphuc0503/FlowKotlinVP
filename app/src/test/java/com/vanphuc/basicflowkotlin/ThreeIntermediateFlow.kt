package com.vanphuc.basicflowkotlin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import kotlin.system.measureTimeMillis

class ThreeIntermediateFlow {
    /**
     * Flow context
     */
    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

    fun simple(): Flow<Int> = flow {
        log("Started simple flow")
        for (i in 1..3) {
            emit(i)
        }
    }

    @Test
    fun testFlowContext() = runBlocking<Unit> {
        simple().collect { value ->
            log("Collected $value")
        }
    }

    /**
     * A common pitfall when using withContext
     */
    fun simple2(): Flow<Int> = flow {
        // The WRONG way to change context for CPU-consuming code in flow builder
        withContext(Dispatchers.Default) {
            for (i in 1..3) {
                Thread.sleep(100) // pretend we are computing it in CPU-consuming way
                emit(i) // emit next value
            }
        }
    }

    // Run fall thought context
    @Test
    fun testPitfallWithContext() = runBlocking<Unit> {
        simple2().collect { value ->
            println(value)
        }
    }

    /**
     * flowOn operator
     */
    fun simple3(): Flow<Int> = flow {
        for (i in 1..3) {
            Thread.sleep(100) // pretend we are computing it in CPU-consuming way
            log("Emitting $i")
            emit(i) // emit next value
        }
    }.flowOn(Dispatchers.Default) // RIGHT way to change context for CPU-consuming code in flow builder

    @Test
    fun testFlowOnOperator() = runBlocking<Unit> {
        simple3().collect { value ->
            log("Collected $value")
        }
    }

    /**
     * Not Buffering
     */
    fun simple4(): Flow<Int> = flow {
        for (i in 1..3) {
            delay(100) // pretend we are asynchronously waiting 100 ms
            emit(i) // emit next value
        }
    }

    @Test
    fun testNotBuffering() = runBlocking<Unit> {
        val time = measureTimeMillis {
            simple4().collect { value ->
                delay(300) // pretend we are processing it for 300 ms
                println(value)
            }
        }
        println("Collected in $time ms")
    }

    /**
     * Buffering
     */
    @Test
    fun testBuffering() = runBlocking<Unit> {
        val time = measureTimeMillis {
            simple4()
                .buffer() // buffer emissions, don't wait
                .collect { value ->
                    delay(300) // pretend we are processing it for 300 ms
                    println(value)
                }
        }
        println("Collected in $time ms")
    }

    /**
     * Conflation
     */
    @Test
    fun testConflation() = runBlocking<Unit> {
        val time = measureTimeMillis {
            simple4()
                .conflate() // conflate emissions, don't process each one
                .collect { value ->
                    delay(300) // pretend we are processing it for 300 ms
                    println(value)
                }
        }
        println("Collected in $time ms")
    }

    /**
     * Processing the latest value
     */
    @Test
    fun testLatestValue() = runBlocking<Unit> {
        val time = measureTimeMillis {
            simple4()
                .collectLatest { value -> // cancel & restart on the latest value
                    println("Collecting $value")
                    delay(300) // pretend we are processing it for 300 ms
                    println(value)
                }
        }
        println("Collected in $time ms")
    }

    /**
     * Composing multiple flows
     */

    //Zip
    @Test
    fun testZip() = runBlocking<Unit> {
        val nums = (1..3).asFlow()/*.onEach { delay(300) }*/ // numbers 1..3
        val strs = flowOf("one", "two", "three")/*.onEach { delay(400) }*/ // strings
        val time = measureTimeMillis {
            nums
                .zip(strs) { a, b ->
                    "$a -> $b"
                } // compose a single string
                .collect {
                    println(it)
                } // collect and print
        }
        println("Collected in $time ms")
    }

    //Combine
    @Test
    fun testCombine() = runBlocking<Unit> {
        val nums = (1..3).asFlow().onEach { delay(300) } // numbers 1..3
        val strs = flowOf("one", "two", "three").onEach { delay(400) } // strings
        val time = measureTimeMillis {
            nums
                .combine(strs) { a, b ->
                    "$a -> $b"
                } // compose a single string
                .collect {
                    println(it)
                } // collect and print
        }
        println("Collected in $time ms")
    }

    /**
     * Flattening flows
     */

    fun requestFlow(i: Int): Flow<String> = flow {
        emit("$i: First")
        delay(500) // wait 500 ms
        emit("$i: Second")
    }

    //flatMapConcat
    @Test
    fun testFlatMapConcat() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis() // remember the start time
        (1..3).asFlow().onEach { delay(100) } // emit a number every 100 ms
            .flatMapConcat {
                requestFlow(it)
            } //map -> Flow<String> # flatMapConcat -> String
            .collect { value -> // collect and print
                println("$value at ${System.currentTimeMillis() - startTime} ms from start")
            }
    }

    //flatMapMerge
    @Test
    fun testFlatMapMerge() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis() // remember the start time
        (1..3).asFlow().onEach {
            delay(100)
        } // a number every 100 ms
            .flatMapMerge {
                requestFlow(it)
            }
            .collect { value -> // collect and print
                println("$value at ${System.currentTimeMillis() - startTime} ms from start")
            }
    }

    //flatMapLatest
    @Test
    fun testFlatMapLatest() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis() // remember the start time
        (1..3).asFlow().onEach {
            delay(100)
        } // a number every 100 ms
            .flatMapLatest {
                requestFlow(it)
            }
            .collect { value -> // collect and print
                println("$value at ${System.currentTimeMillis() - startTime} ms from start")
            }
    }
}