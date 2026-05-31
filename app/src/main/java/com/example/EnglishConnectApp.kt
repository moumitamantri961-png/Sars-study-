package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EnglishConnectApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppRepository(database) }

    override fun onCreate() {
        super.onCreate()
        
        // Seed database asynchronously on launch
        applicationScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }
}
