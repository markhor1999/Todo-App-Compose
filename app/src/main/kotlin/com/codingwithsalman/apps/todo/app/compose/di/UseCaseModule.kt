package com.codingwithsalman.apps.todo.app.compose.di

import com.codingwithsalman.apps.todo.app.compose.features.note.domain.use_case.AddNote
import com.codingwithsalman.apps.todo.app.compose.features.note.domain.use_case.DeleteNote
import com.codingwithsalman.apps.todo.app.compose.features.note.domain.use_case.GetNote
import com.codingwithsalman.apps.todo.app.compose.features.note.domain.use_case.GetNotes
import com.codingwithsalman.apps.todo.app.compose.features.note.domain.use_case.NoteUseCases
import org.koin.dsl.module

val useCaseModule = module {
    factory { AddNote(get()) }
    factory { DeleteNote(get()) }
    factory { GetNotes(get()) }
    factory { GetNote(get()) }
    factory {
        NoteUseCases(
            get(),
            get(),
            get(),
            get()
        )
    }
}