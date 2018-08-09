package com.swarmnyc.promisekt

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger

typealias LogAction = (msg: String) -> Unit

typealias LogErrorAction = (msg: String, e: Throwable) -> Unit

abstract class PromiseOptions {
    internal var debugMode = false
    internal val idCounter: AtomicInteger by lazy {
        AtomicInteger()
    }

    //background executor
    abstract var executor: ExecutorService

    abstract var uiExecutor: Executor

    abstract var log: LogAction

    abstract var logError: LogErrorAction
}

