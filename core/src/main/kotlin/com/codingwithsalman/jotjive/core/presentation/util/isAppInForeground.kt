package com.codingwithsalman.jotjive.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner

@Composable
fun isAppInForeground(): State<Boolean> {
    return produceState(initialValue = true) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> value = true
                Lifecycle.Event.ON_STOP -> value = false
                else -> Unit
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)

        awaitDispose {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }
}