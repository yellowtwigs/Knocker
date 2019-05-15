package com.example.knocker.model

import android.os.Handler
import android.os.HandlerThread

class DbWorkerThread(threadName: String) : HandlerThread(threadName){

    private lateinit var mWorkerHandler: Handler

    override fun onLooperPrepared() {
        super.onLooperPrepared()
        mWorkerHandler = Handler(looper)
    }

    //lance la fonction task dans un thread
    fun postTask(task: Runnable) {
        if (this::mWorkerHandler.isInitialized) {
            mWorkerHandler.post(task)
        } else {
            postTask(task)
        }
    }
}