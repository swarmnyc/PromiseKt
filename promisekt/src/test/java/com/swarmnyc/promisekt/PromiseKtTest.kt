package com.swarmnyc.fulton.android.promise

import com.swarmnyc.promisekt.Promise
import com.swarmnyc.promisekt.PromiseState
import com.swarmnyc.promisekt.util.await
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@RunWith(JUnit4::class)
class PromiseTest {
    companion object {
        val TAG = PromiseTest::class.java.simpleName!!
    }

    init {
//        Promise.defaultOptions.debugMode = true
    }

    @Before
    fun before() {
        Promise.uncaughtError = { throw it }
    }

    @Test
    fun promiseTest() {
        // test basic promise

        val latch = CountDownLatch(1)
        var result: String? = null

        val p = Promise<String> { resolve, _ ->
            resolve("Abc")
        }

        p.then {
            result = it
            latch.countDown()
        }

        latch.await()

        assertEquals("Abc", result)
    }

    @Test
    fun thenTest() {
        // test resolve and then

        val latch = CountDownLatch(1)
        var result: String? = null
        Promise.resolve("Abc").then {
            result = it
            latch.countDown()
        }

        latch.await()

        assertEquals("Abc", result)
    }

    @Test
    fun then2Test() {
        // test two then, p1 -> p2 -> p3

        val latch = CountDownLatch(1)
        var result = 0

        val p = Promise<String> { resolve, _ ->
            resolve("Abc")
        }

        p.then {
            100
        }.then {
            result = it
            latch.countDown()
        }

        latch.await()

        assertEquals(100, result)
    }

    @Test
    fun then3Test() {
        // test two then, p1 -> p2, p1 -> p3

        val latch = CountDownLatch(2)

        val p = Promise<String> { resolve, _ ->
            resolve("Abc")
        }

        p.then {
            latch.countDown()
        }

        p.then {
            latch.countDown()
        }

        latch.await()
    }

    @Test
    fun then4Test() {
        // test three then, p1->p2->p4, p1->p3

        val latch = CountDownLatch(3)

        val p = Promise<String> { resolve, _ ->
            resolve("Abc")
        }

        val p2 = p.then {
            latch.countDown()
            "Cba"
        }

        p.then {
            latch.countDown()
        }

        var result = ""

        p2.then {
            result = it
            latch.countDown()
        }

        latch.await()

        assertEquals("Cba", result)
    }


    @Test
    fun catchTest() {
        // test catch error

        val latch = CountDownLatch(1)
        val error = Throwable("Test")
        var result: Throwable? = null

        Promise<String> { _, reject ->
            reject(error)
        }.then {
            fail()
            latch.countDown()
        }.catch {
            result = it
            latch.countDown()
        }

        latch.await()

        assertEquals(error, result)
    }

    @Test
    fun catchFailOnExecTest() {
        // test catch fail on executor

        val latch = CountDownLatch(1)
        val error = Throwable("Test")
        var result: Throwable? = null

        Promise<String> { _, _ ->
            throw error
        }.then {
            fail()
        }.catch {
            result = it
            latch.countDown()
        }

        latch.await()

        assertEquals(error, result)
    }

    @Test
    fun catchFailOnThenTest() {
        // test catch fail on then

        val latch = CountDownLatch(2)
        val error = Throwable("Test")
        var result: Throwable? = null

        Promise<String> { resolve, _ ->
            resolve("Abc")
        }.then {
            latch.countDown()
            throw error
        }.catch {
            result = it
            latch.countDown()
        }

        latch.await()

        assertEquals(error, result)
    }

    @Test
    fun catchFailOnThen2Test() {
        // test catch fail by p1->p2->catch, p1->p3->then

        val latch = CountDownLatch(3)
        val error = Throwable("Test")
        var result: Throwable? = null

        val p = Promise<String> { resolve, _ ->
            resolve("Abc")
        }

        p.then {
            latch.countDown()
            throw error
        }.catch {
            result = it
            latch.countDown()
        }

        var result2 = ""
        p.then {
            result2 = it
            latch.countDown()
        }

        latch.await()

        assertEquals(error, result)
        assertEquals("Abc", result2)
    }

    @Test
    fun catchRejectTest() {
        // test two catch

        val latch = CountDownLatch(2)
        val error = Throwable("Test")
        var result1: Throwable? = null
        var result2: Throwable? = null

        Promise.reject(error).then {
            fail()
        }.catch {
            result1 = it
            latch.countDown()
        }.catch {
            result2 = it
            latch.countDown()
        }

        latch.await()

        assertEquals(error, result1)
        assertEquals(error, result2)
    }

    @Test
    fun catchReject2Test() {
        // test two catch, and first catch throw new error

        val latch = CountDownLatch(1)
        val error1 = Throwable("Test1")
        val error2 = Throwable("Test2")
        var result: Throwable? = null

        Promise.reject(error1).catch {
            throw error2
        }.catch {
            result = it
            latch.countDown()
        }

        latch.await()

        assertEquals(error2, result)
    }

    @Test
    fun uncaughtTest() {
        // test to throw uncaught error on root promise

        val latch = CountDownLatch(1)
        val error = Throwable("Test")
        var result: Throwable? = null

        val old = Promise.uncaughtError
        Promise.uncaughtError = {
            result = it
            latch.countDown()
        }

        Promise<Nothing> { _, _ ->
            throw error
        }

        latch.await()

        assertEquals(error, result)

        Promise.uncaughtError = old
    }

    @Test
    fun uncaught2Test() {
        // test to throw uncaught error on child promise's catch

        val latch = CountDownLatch(1)
        val error1 = Throwable("Test1")
        val error2 = Throwable("Test2")
        var result: Throwable? = null

        val old = Promise.uncaughtError
        Promise.uncaughtError = {
            result = it
            latch.countDown()
        }

        Promise<Nothing> { _, _ ->
            throw error1
        }.then {
            fail()
        }.catch {
            throw error1
        }.catch {
            throw error1
        }.catch {
            throw error1
        }.then {
            fail()
        }.catch {
            throw error2
        }

        latch.await()

        assertEquals(error2, result)

        Promise.uncaughtError = old
    }

    @Test
    fun uncaught3Test() {
        // test to throw uncaught error on child promise

        val latch = CountDownLatch(1)
        val error = Throwable("Test1")
        var result: Throwable? = null

        Promise.uncaughtError = {
            result = it
            latch.countDown()
        }

        Promise.resolve("Abc").then {
            123
        }.then {
            throw error
        }.then {
            false
        }

        latch.await()

        assertEquals(error, result)
    }


    @Test
    fun chainTest() {
        // test promise chain

        val latch = CountDownLatch(1)
        var result = 0

        val p = Promise<String> { resolve, _ ->
            Thread.sleep(100)
            resolve("Abc")
        }

        p.thenChain {
            Promise.resolve(123)
        }.then {
            result = it
            latch.countDown()
        }

        latch.await()

        assertEquals(123, result)
    }

    @Test
    fun treadTest() {
        // test multi-threads, there will create 3 thread, 1. main thread, 2. promise body thread 3. result(success, fail, always) thread

        val mainThread = Thread.currentThread().id
        val executorThread = AtomicLong(0)
        val then1Thread = AtomicLong(0)
        val then2Thread = AtomicLong(0)

        //System.out.println("Main Thread Id : ${Thread.currentThread().id}")

        val promise = Promise<Unit> { resolve, _ ->
            Thread.sleep(100)
            //System.out.println("executor Thread Id : ${Thread.currentThread().id}")
            executorThread.set(Thread.currentThread().id)
            resolve(Unit)
        }.then {
            //System.out.println("Then 1 Thread Id : ${Thread.currentThread().id}")
            then1Thread.set(Thread.currentThread().id)
        }.thenUi {
            //System.out.println("Then 2 Thread Id : ${Thread.currentThread().id}")
            then2Thread.set(Thread.currentThread().id)
        }

        val executor2Thread = AtomicLong(0)
        val then3Thread = AtomicLong(0)
        val then4Thread = AtomicLong(0)

        //System.out.println("Main Thread Id : ${Thread.currentThread().id}")

        val promise2 = Promise<Unit> { resolve, _ ->
            //System.out.println("executor Thread Id : ${Thread.currentThread().id}")
            executor2Thread.set(Thread.currentThread().id)
            resolve(Unit)
        }.then {
            //System.out.println("Then 1 Thread Id : ${Thread.currentThread().id}")
            then3Thread.set(Thread.currentThread().id)
        }.thenUi {
            //System.out.println("Then 2 Thread Id : ${Thread.currentThread().id}")
            then4Thread.set(Thread.currentThread().id)
        }

        promise.await()
        promise2.await()

        assertNotEquals(mainThread, executorThread.get())
        assertEquals(executorThread.get(), then1Thread.get()) // then use the same thread
        assertNotEquals(then1Thread.get(), then2Thread.get()) // thenUi use ui thread

        assertNotEquals(mainThread, executor2Thread.get())
        // each promise use its own thread
        assertNotEquals(executorThread.get(), executor2Thread.get())
    }

    @Test
    fun allThenTest() {
        // test promise.all success

        val latch = CountDownLatch(1)
        val promise1 = Promise<String> { resolve, _ ->
            Thread.sleep(200)
            resolve("Abc")
        }

        val promise2 = Promise<String> { resolve, _ ->
            Thread.sleep(100)
            resolve("Efg")
        }

        var result: Array<Any>? = null

        Promise.all(promise1, promise2).then {
            result = it
            latch.countDown()
        }

        latch.await()

        assertArrayEquals(arrayOf("Abc", "Efg"), result)
    }

    @Test
    fun allFailTest() {
        // test promise.all with error

        val latch = CountDownLatch(1)
        val error = Throwable("Test")
        var result: Throwable? = null

        val promise1 = Promise<String> { _, _ ->
            Thread.sleep(200)
            throw error
        }

        val promise2 = Promise<String> { resolve, _ ->
            Thread.sleep(100)
            resolve("Efg")
        }

        Promise.all(promise1, promise2).then {
            fail()
        }.catch {
            result = it
            latch.countDown()
        }

        latch.await()

        assertEquals(error, result)
    }

    @Test
    fun raceTest() {
        // test promise.race

        val latch = CountDownLatch(1)
        var result = ""

        val promise1 = Promise<String> { resolve, _ ->
            Thread.sleep(500)
            resolve("Abc")
        }

        val promise2 = Promise<String> { resolve, _ ->
            Thread.sleep(100)
            resolve("Efg")
        }

        Promise.race(promise1, promise2).then {
            result = it as String
            latch.countDown()
        }

        latch.await()

        assertEquals("Efg", result)
    }

    @Test
    fun raceFailTest() {
        // test promise.race with error

        val latch = CountDownLatch(1)
        var result = ""

        val promise1 = Promise<String> { resolve, _ ->
            Thread.sleep(200)
            throw Throwable("Test")
        }

        val promise2 = Promise<String> { resolve, _ ->
            Thread.sleep(100)
            resolve("Efg")
        }

        Promise.race(promise1, promise2).then {
            result = it as String
            latch.countDown()
        }

        latch.await()

        assertEquals("Efg", result)
    }

    @Test
    fun cancelTest() {
        // test cancel promise without error

        val latch = CountDownLatch(1)

        val p = Promise<String> { resolve, _ ->
            Thread.sleep(100000)
            resolve("abc")
        }

        p.then {
            fail()
        }.catch {
            fail()
        }

        Promise.defaultOptions.executor.submit {
            p.cancel()

            latch.countDown()
        }

        latch.await()

        assertEquals(PromiseState.Canceled.ordinal, p.mState.get())
    }

    @Test
    fun cancel2Test() {
        // test cancel on root promise and cause error and catch it.

        val latch = CountDownLatch(1)
        var result: Throwable? = null

        val p = Promise<String> { resolve, _ ->
            Thread.sleep(100000)
            resolve("abc")
        }

        p.then {
            fail()
        }.catch {
            result = it
            latch.countDown()
        }

        Promise.defaultOptions.executor.submit {
            p.cancel(true)
        }

        latch.await()

        assertTrue(result is InterruptedException)
    }

    @Test
    fun cancel3Test() {
        // test cancel on child promise and cause error and catch it.

        val latch = CountDownLatch(1)
        var result: Throwable? = null

        val p = Promise<String> { resolve, _ ->
            Thread.sleep(100000)
            resolve("abc")
        }.then {
            fail()
        }.catch {
            result = it
            latch.countDown()
        }

        Promise.defaultOptions.executor.submit {
            p.cancel(true)
        }

        latch.await()

        assertTrue(result is InterruptedException)
    }

    @Test
    fun timeoutTest() {
        // test timeout on root promise and cause error and catch it.

        val latch = CountDownLatch(1)
        var result: Throwable? = null

        Promise<String> { resolve, _ ->
            Thread.sleep(100000)
            resolve("abc")
        }.timeout(1000, true).then {
            fail()
        }.catch {
            result = it
            latch.countDown()
        }

        latch.await()

        assertTrue(result is InterruptedException)
    }

    @Test
    fun timeout2Test() {
        // test timeout on child promise and cause error and catch it.

        val latch = CountDownLatch(1)
        var result: Throwable? = null

        Promise<String> { resolve, _ ->
            Thread.sleep(100000)
            resolve("abc")
        }.then {
            fail()
        }.catch {
            result = it
            latch.countDown()
        }.timeout(1000, true)

        latch.await()

        assertTrue(result is InterruptedException)
    }

    @Test
    fun timeout3Test() {
        // test timeout, but the promise is finished.

        val latch = CountDownLatch(1)
        var result: String? = null

        Promise<String> { resolve, _ ->
            resolve("abc")
        }.then {
            result = it
            latch.countDown()
        }.catch {
            fail()

        }.timeout(1000, true)

        latch.await()

        assertEquals("abc", result)
    }

    @Test
    fun timeout4Test() {
        // test timeout, but the promise is finished by error.

        val latch = CountDownLatch(1)
        val error = Throwable("Test")
        var result: Throwable? = null

        Promise<String> { resolve, _ ->
            throw error
        }.then {
            fail()
        }.catch {
            result = it
            latch.countDown()
        }.timeout(1000, true)

        latch.await()

        assertEquals(error, result)
    }

    @Test
    fun timeout5Test() {
        // test timeout with promise.thenChain

        val latch = CountDownLatch(1)
        var result: Throwable? = null

        Promise.resolve("abc")
                .thenChain {
                    Promise<String> { resolve, _ ->
                        Thread.sleep(100000)
                        resolve("efg")
                    }
                }
                .then {
                    fail()
                }
                .timeout(1000, true)
                .catch {
                    result = it
                    latch.countDown()
                }

        latch.await()

        assertTrue(result is InterruptedException)
    }


    @Test
    fun timeout6Test() {
        // test timeout, but time in
        val latch = CountDownLatch(1)
        var result: String? = null

        Promise.resolve("abc")
                .thenChain {
                    Promise<String> { resolve, _ ->
                        Thread.sleep(100)
                        resolve("efg")
                    }
                }.then {
                    result = it
                    latch.countDown()
                }
                .timeout(1000, true)
                .catch {
                    fail()
                }

        latch.await()

        assertEquals("efg", result)
    }

    @Test
    fun catchThenTest() {
        // then after catch
        val latch = CountDownLatch(1)
        var result: String? = null

        Promise.resolve("abc")
                .catch {
                    fail()
                }.then {
                    result = it
                    latch.countDown()
                }

        latch.await()

        assertEquals("abc", result)
    }

    @Test
    fun catchForkTest() {
        // one parent (error), two children (one then, one catch)
        val error = Throwable("Test")
        val count = AtomicInteger()
        Promise.uncaughtError = {
            count.incrementAndGet()
        }

        val promise = Promise<Int> { promise ->
            Thread.sleep(10)
            promise.reject(error)
        }

        promise.catch {
            count.incrementAndGet()
        }

        promise.then {
            count.incrementAndGet()
        }

        Thread.sleep(100)
        assertEquals(1, count.get())
    }

    @Test
    fun catchFork2Test() {
        // one parent (fulfill), two children (one error no catch, one then)
        val error = Throwable("Test")
        val count = AtomicInteger()
        var result: Throwable? = null
        Promise.uncaughtError = {
            result = it
        }

        val promise = Promise<Int> { promise ->
            Thread.sleep(10)
            promise.resolve(1)
        }

        promise.then {
            throw error
        }

        promise.then {
        }

        Thread.sleep(100)
        assertEquals(error, result)
    }

    @Test
    fun done1Test() {
        // for then
        val latch = CountDownLatch(3)

        Promise<Int> { promise ->
            promise.resolve(1)
        }.then {
            latch.countDown()
        }.then {
            latch.countDown()
        }.catch {
            latch.countDown()
        }.done {
            latch.countDown()
        }

        latch.await(1000, TimeUnit.MILLISECONDS)

        assertEquals(0, latch.count)
    }

    @Test
    fun done2Test() {
        // for catch
        val latch = CountDownLatch(2)

        Promise<Int> { promise ->
            promise.reject(Exception("Test"))
        }.catch {
            latch.countDown()
        }.done {
            latch.countDown()
        }

        latch.await(1000, TimeUnit.MILLISECONDS)

        assertEquals(0, latch.count)
    }
}