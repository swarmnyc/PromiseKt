package com.swarmnyc.promisekt.util

import com.swarmnyc.promisekt.Promise
import java.util.concurrent.TimeUnit

fun <T> Promise<T>.await(throwError: Boolean = true, timeoutMs: Long? = null): T? {
    val latch = java.util.concurrent.CountDownLatch(1)

    var t: T? = null
    var e: Throwable? = null

    this.then {
        t = it
        latch.countDown()
    }.catch {
        e = it
        latch.countDown()
    }

    if (timeoutMs == null){
        latch.await()
    }else{
        latch.await(timeoutMs, TimeUnit.MILLISECONDS)
    }

    if (e != null && throwError) {
        throw e!!
    }

    return t
}