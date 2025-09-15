package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.create_jot

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.MoodUi
import com.codingwithsalman.jotjive.core.domain.jive.Mood
import com.codingwithsalman.jotjive.core.domain.jot.Jot
import com.codingwithsalman.jotjive.core.domain.jot.JotDataSource
import com.codingwithsalman.jotjive.core.domain.settings.SettingsPreferences
import com.codingwithsalman.jotjive.core.presentation.designsystem.dropdowns.Selectable.Companion.asUnselectedItems
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

class CreateJotViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val settingsPreferences: SettingsPreferences,
    private val jotDataSource: JotDataSource
) : ViewModel() {
    private var hasLoadedInitialData = false

    private val eventChannel = Channel<CreateJotEvent>()
    val events = eventChannel.receiveAsFlow()

    private val restoredTopics = savedStateHandle.get<String>("topics")?.split(",")
    private val _state = MutableStateFlow(
        CreateJotState(
            titleText = savedStateHandle["titleText"] ?: "",
            noteText = savedStateHandle["noteText"] ?: "",
            topics = restoredTopics ?: emptyList(),
            mood = savedStateHandle.get<String>("mood")?.let {
                MoodUi.valueOf(it)
            },
            showMoodSelector = savedStateHandle.get<String>("mood") == null,
            canSaveJot = savedStateHandle.get<Boolean>("canSaveJot") == true
        )
    )
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                observeAddTopicText()
                fetchDefaultSettings()
                hasLoadedInitialData = true
            }
        }
        .onEach { state ->
            savedStateHandle["titleText"] = state.titleText
            savedStateHandle["noteText"] = state.noteText
            savedStateHandle["topics"] = state.topics.joinToString(",")
            savedStateHandle["mood"] = state.mood?.name
            savedStateHandle["canSaveJot"] = state.canSaveJot
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CreateJotState()
        )

    fun onAction(action: CreateJotAction) {
        when (action) {
            is CreateJotAction.OnAddTopicTextChange -> onAddTopicTextChange(action.text)
            CreateJotAction.OnConfirmMood -> onConfirmMood()
            CreateJotAction.OnDismissMoodSelector -> onDismissMoodSelector()
            CreateJotAction.OnDismissTopicSuggestions -> onDismissTopicSuggestions()
            is CreateJotAction.OnMoodClick -> onMoodClick(action.moodUi)
            is CreateJotAction.OnNoteTextChange -> onNoteTextChange(action.text)
            is CreateJotAction.OnRemoveTopicClick -> onRemoveTopicClick(action.topic)
            CreateJotAction.OnSaveClick -> onSaveClick()
            is CreateJotAction.OnTitleTextChange -> onTitleTextChange(action.text)
            is CreateJotAction.OnTopicClick -> onTopicClick(action.topic)
            CreateJotAction.OnSelectMoodClick -> onSelectMoodClick()
            CreateJotAction.OnDismissConfirmLeaveDialog -> onDismissConfirmLeaveDialog()
            CreateJotAction.OnCancelClick,
            CreateJotAction.OnNavigateBackClick,
            CreateJotAction.OnGoBack -> onShowConfirmLeaveDialog()

            is CreateJotAction.OnJotCategoryUpdated -> onJotCategoryUpdated(action.category)
        }
    }

    private fun fetchDefaultSettings() {
        settingsPreferences
            .observeDefaultMood()
            .take(1)
            .onEach { defaultMood ->
                val moodUi = MoodUi.valueOf(defaultMood.name)
                _state.update {
                    it.copy(
                        selectedMood = moodUi,
                        mood = moodUi,
                        showMoodSelector = false
                    )
                }
            }
            .launchIn(viewModelScope)

        settingsPreferences
            .observeDefaultTopics()
            .take(1)
            .onEach { defaultTopics ->
                _state.update {
                    it.copy(
                        topics = defaultTopics
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun onJotCategoryUpdated(jotCategory: JotCategory) {
        _state.update {
            it.copy(
                selectedJotCategory = jotCategory
            )
        }
    }

    private fun onNoteTextChange(text: String) {
        _state.update {
            it.copy(
                noteText = text
            )
        }
    }


    private fun onTitleTextChange(text: String) {
        _state.update {
            it.copy(
                titleText = text,
                canSaveJot = text.isNotBlank() && it.mood != null
            )
        }
    }

    private fun onSaveClick() {
        if (!state.value.canSaveJot) {
            return
        }

        viewModelScope.launch {
            val currentState = state.value
            val jot = Jot(
                mood = currentState.mood?.let {
                    Mood.valueOf(it.name)
                } ?: throw IllegalStateException("Mood must be set before saving."),
                title = currentState.titleText.trim(),
                note = currentState.noteText.ifBlank { null },
                topics = currentState.topics,
                addedAt = Instant.now(),
                isTodo = currentState.selectedJotCategory == JotCategory.TODO,
                isDone = false
            )

            jotDataSource.insertJot(jot)
            eventChannel.send(CreateJotEvent.JotSuccessfullySaved)
        }
    }

    private fun onShowConfirmLeaveDialog() {
        _state.update {
            it.copy(
                showConfirmLeaveDialog = true
            )
        }
    }

    private fun onDismissConfirmLeaveDialog() {
        _state.update {
            it.copy(
                showConfirmLeaveDialog = false
            )
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeAddTopicText() {
        combine(
            state.map { it.addTopicText },
            jotDataSource.observeTopics()
        ) { addTopicText, topics ->
            addTopicText to topics
        }
            .distinctUntilChanged()
            .debounce(300)
            .onEach { (query, existingTopics) ->
                _state.update {
                    it.copy(
                        showTopicSuggestions = query.isNotBlank() && query.trim() !in it.topics,
                        searchResults = existingTopics.asUnselectedItems()
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun onDismissTopicSuggestions() {
        _state.update {
            it.copy(
                showTopicSuggestions = false
            )
        }
    }

    private fun onRemoveTopicClick(topic: String) {
        _state.update {
            it.copy(
                topics = it.topics - topic
            )
        }
    }

    private fun onTopicClick(topic: String) {
        _state.update {
            it.copy(
                addTopicText = "",
                topics = (it.topics + topic).distinct()
            )
        }
    }

    private fun onAddTopicTextChange(text: String) {
        _state.update {
            it.copy(
                addTopicText = text.filter {
                    it.isLetterOrDigit()
                }
            )
        }
    }

    private fun onConfirmMood() {
        _state.update {
            it.copy(
                mood = it.selectedMood,
                canSaveJot = it.titleText.isNotBlank(),
                showMoodSelector = false
            )
        }
    }

    private fun onDismissMoodSelector() {
        _state.update {
            it.copy(
                showMoodSelector = false
            )
        }
    }

    private fun onSelectMoodClick() {
        _state.update {
            it.copy(
                showMoodSelector = true
            )
        }
    }

    private fun onMoodClick(mood: MoodUi) {
        _state.update {
            it.copy(
                selectedMood = mood
            )
        }
    }
}