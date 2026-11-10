package com.example.parentapp

import android.app.Application
import com.example.parentapp.data.local.DatabaseSeeder
import com.example.parentapp.di.AppContainer
import com.example.parentapp.di.DefaultAppContainer
import com.example.parentapp.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ParentsApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        NotificationHelper.createReminderChannel(this)
        appScope.launch {
            DatabaseSeeder.seedIfEmpty((container as DefaultAppContainer).db)
        }
    }
}
