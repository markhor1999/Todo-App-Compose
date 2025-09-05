package com.codingwithsalman.jotjive.core.database.di

import androidx.room.Room
import com.codingwithsalman.jotjive.core.database.JotJiveDatabase
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val databaseModule = module {
    single<JotJiveDatabase> {
        Room.databaseBuilder(
            androidApplication(),
            JotJiveDatabase::class.java,
            "jotjive.db",
        ).build()
    }
    single {
        get<JotJiveDatabase>().jiveDao
    }
}