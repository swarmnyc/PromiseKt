package com.swarmnyc.promisekt.android

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.swarmnyc.fulton.android.util.readWriteLazy
import com.swarmnyc.promisekt.LogAction
import com.swarmnyc.promisekt.LogErrorAction
import com.swarmnyc.promisekt.PromiseOptions
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class AndroidPromiseOptions : PromiseOptions() {
    companion object {
        private const val TAG = "PromiseKt"
    }

    private val handler = Handler(Looper.getMainLooper())

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
        Executor { command -> handler.post(command) }
    }

    override var log: LogAction = {
        Log.d(TAG, it)
    }

    override var logError: LogErrorAction = { msg, error ->
        Log.e(TAG, msg, error)
    }
}