package com.codingwithsalman.apps.todo.app.compose

import android.app.Application
import com.codingwithsalman.apps.todo.app.compose.di.appModule
import com.codingwithsalman.jotjive.core.database.di.databaseModule
import com.codingwithsalman.jotjive.core.di.jotJiveModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NoteApp : Application() {
    val applicationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@NoteApp)
            modules(
                appModule,
                jotJiveModule,
                databaseModule
            )
        }

    }
}