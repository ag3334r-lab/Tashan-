package com.example

import android.app.Application
import android.content.Context
import com.example.data.FirestoreConfig

class TashanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        // Initialize Firestore with local disk persistence for offline accessibility
        FirestoreConfig.configureFirestoreDiskPersistence(this)
    }

    companion object {
        private lateinit var instance: TashanApplication
        fun getAppContext(): Context {
            return instance.applicationContext
        }
    }
}
