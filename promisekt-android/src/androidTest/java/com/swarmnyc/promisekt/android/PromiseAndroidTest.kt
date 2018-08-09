package com.swarmnyc.promisekt.android

import androidx.test.runner.AndroidJUnit4
import com.swarmnyc.promisekt.Promise
import com.swarmnyc.promisekt.util.await
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

@RunWith(AndroidJUnit4::class)
class PromiseAndroidTest {
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
        assertTrue(Promise.defaultOptions is AndroidPromiseOptions)
    }

    @Test
    fun treadTest() {
        // test multi-threads, there will create 3 thread, 1. main thread, 2. promise body thread 3. result(success, fail, always) thread

        val mainThread = Thread.currentThread().id
        val executorThread = AtomicLong(0)
        val then1Thread = AtomicLong(0)
        val then2Thread = AtomicLong(0)

        val promise = Promise<Unit> { resolve, _ ->
                Thread.sleep(100)
                executorThread.set(Thread.currentThread().id)
                resolve(Unit)
        }.then {
            then1Thread.set(Thread.currentThread().id)
        }.thenUi {
            then2Thread.set(Thread.currentThread().id)
        }

        val executor2Thread = AtomicLong(0)
        val then3Thread = AtomicLong(0)
        val then4Thread = AtomicLong(0)

        val promise2 = Promise<Unit> { resolve, _ ->
                executor2Thread.set(Thread.currentThread().id)
                resolve(Unit)
        }.then {
            then3Thread.set(Thread.currentThread().id)
        }.thenUi {
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
}