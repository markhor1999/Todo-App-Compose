package com.codingwithsalman.jotjive.core.presentation.designsystem.dropdowns

data class Selectable<T>(
    val item: T,
    val selected: Boolean
) {
    companion object {
        fun <T> List<T>.asUnselectedItems(): List<Selectable<T>> {
            return map { Selectable(
                item = it,
                selected = false
            ) }
        }
    }
}
