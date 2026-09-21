package com.sparklab.radio

import android.app.Application
import com.sparklab.radio.di.AppContainer

/**
 * Application entry point. Owns the DI container so it lives for the whole
 * process (shared by the phone UI and the Android Auto media service).
 */
class RadioApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

