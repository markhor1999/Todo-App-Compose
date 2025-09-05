package com.codingwithsalman.apps.todo.app.compose.features.note.domain.use_case

import com.codingwithsalman.jotjive.core.domain.jot.Jot
import com.codingwithsalman.jotjive.core.domain.jot.JotDataSource

class DeleteNote(
    private val jotDataSource: JotDataSource
) {
    suspend operator fun invoke(jot: Jot) {
        jotDataSource.deleteJot(jot)
    }
}