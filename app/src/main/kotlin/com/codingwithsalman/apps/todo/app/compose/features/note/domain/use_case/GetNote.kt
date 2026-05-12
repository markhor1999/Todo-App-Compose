package com.codingwithsalman.apps.todo.app.compose.features.note.domain.use_case

import com.codingwithsalman.jotjive.core.domain.jot.JotDataSource

class GetNote(
    private val jotDataSource: JotDataSource
) {
    suspend operator fun invoke(id: Int) = jotDataSource.getJot(id)
}