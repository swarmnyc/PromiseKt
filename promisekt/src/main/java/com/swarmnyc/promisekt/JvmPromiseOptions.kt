package com.swarmnyc.promisekt

import com.swarmnyc.fulton.android.util.readWriteLazy
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class JvmPromiseOptions : PromiseOptions() {
    //background executor
    override var executor: ExecutorService by readWriteLazy {
        Executors.newCachedThreadPool { command ->
            Thread(command).also { thread ->
                thread.priority = Thread.NORM_PRIORITY
                thread.isDaemon = true
            }
        }
    }

    override var uiExecutor: Executor by readWriteLazy {
        Executor { command ->  executor.submit(command) }
    }

    override var log: LogAction = {
        System.out.println(it)
    }

    override var logError: LogErrorAction = { msg, error ->
        System.out.println(msg)
        error.printStackTrace()
    }
}