package com.codingwithsalman.apps.todo.app.compose.di

import com.codingwithsalman.apps.todo.app.compose.NoteApp
import com.codingwithsalman.apps.todo.app.compose.features.note.presentation.add_edit_note.AddEditNoteViewModel
import com.codingwithsalman.apps.todo.app.compose.features.note.presentation.notes.NotesViewModel
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single<CoroutineScope> {
        (androidApplication() as NoteApp).applicationScope
    }

    viewModelOf(::AddEditNoteViewModel)
    viewModelOf(::NotesViewModel)
}
