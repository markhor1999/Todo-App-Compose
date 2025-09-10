package com.codingwithsalman.apps.todo.app.compose.di

import com.codingwithsalman.apps.todo.app.compose.JotJiveApp
import com.codingwithsalman.apps.todo.app.compose.features.note.presentation.add_edit_note.AddEditNoteViewModel
import com.codingwithsalman.apps.todo.app.compose.features.note.presentation.notes.NotesViewModel
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.create_jive.CreateJiveViewModel
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.JotJivesViewModel
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single<CoroutineScope> {
        (androidApplication() as JotJiveApp).applicationScope
    }

    viewModelOf(::AddEditNoteViewModel)
    viewModelOf(::NotesViewModel)
    viewModelOf(::JotJivesViewModel)
    viewModelOf(::CreateJiveViewModel)
    viewModelOf(::SettingsViewModel)
}