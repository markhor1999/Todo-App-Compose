package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.create_jive

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.PlaybackState
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.TrackSizeInfo
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.MoodUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.navigation.NavigationRoute
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.util.AmplitudeNormalizer
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.util.toRecordingDetails
import com.codingwithsalman.jotjive.core.domain.audio.AudioPlayer
import com.codingwithsalman.jotjive.core.domain.jive.Jive
import com.codingwithsalman.jotjive.core.domain.jive.JiveDataSource
import com.codingwithsalman.jotjive.core.domain.jive.Mood
import com.codingwithsalman.jotjive.core.domain.recording.RecordingStorage
import com.codingwithsalman.jotjive.core.domain.settings.SettingsPreferences
import com.codingwithsalman.jotjive.core.presentation.designsystem.dropdowns.Selectable.Companion.asUnselectedItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
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
import kotlin.time.Duration

class CreateJiveViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val recordingStorage: RecordingStorage,
    private val audioPlayer: AudioPlayer,
    private val jiveDataSource: JiveDataSource,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {
    private var hasLoadedInitialData = false

    private val route = savedStateHandle.toRoute<NavigationRoute.CreateJive>()
    private val recordingDetails = route.toRecordingDetails()

    private val eventChannel = Channel<CreateJiveEvent>()
    val events = eventChannel.receiveAsFlow()

    private val restoredTopics = savedStateHandle.get<String>("topics")?.split(",")
    private val _state = MutableStateFlow(
        CreateJiveState(
            playbackTotalDuration = recordingDetails.duration,
            titleText = savedStateHandle["titleText"] ?: "",
            noteText = savedStateHandle["noteText"] ?: "",
            topics = restoredTopics ?: emptyList(),
            mood = savedStateHandle.get<String>("mood")?.let {
                MoodUi.valueOf(it)
            },
            showMoodSelector = savedStateHandle.get<String>("mood") == null,
            canSaveJive = savedStateHandle.get<Boolean>("canSaveJive") == true
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
            savedStateHandle["canSaveJive"] = state.canSaveJive
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CreateJiveState()
        )

    private var durationJob: Job? = null

    fun onAction(action: CreateJiveAction) {
        when (action) {
            is CreateJiveAction.OnAddTopicTextChange -> onAddTopicTextChange(action.text)
            CreateJiveAction.OnConfirmMood -> onConfirmMood()
            CreateJiveAction.OnDismissMoodSelector -> onDismissMoodSelector()
            CreateJiveAction.OnDismissTopicSuggestions -> onDismissTopicSuggestions()
            is CreateJiveAction.OnMoodClick -> onMoodClick(action.moodUi)
            is CreateJiveAction.OnNoteTextChange -> onNoteTextChange(action.text)
            CreateJiveAction.OnPauseAudioClick -> audioPlayer.pause()
            CreateJiveAction.OnPlayAudioClick -> onPlayAudioClick()
            is CreateJiveAction.OnRemoveTopicClick -> onRemoveTopicClick(action.topic)
            CreateJiveAction.OnSaveClick -> onSaveClick()
            is CreateJiveAction.OnTitleTextChange -> onTitleTextChange(action.text)
            is CreateJiveAction.OnTopicClick -> onTopicClick(action.topic)
            is CreateJiveAction.OnTrackSizeAvailable -> onTrackSizeAvailable(action.trackSizeInfo)
            CreateJiveAction.OnSelectMoodClick -> onSelectMoodClick()
            CreateJiveAction.OnDismissConfirmLeaveDialog -> onDismissConfirmLeaveDialog()
            CreateJiveAction.OnCancelClick,
            CreateJiveAction.OnNavigateBackClick,
            CreateJiveAction.OnGoBack -> onShowConfirmLeaveDialog()
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

    private fun onNoteTextChange(text: String) {
        _state.update {
            it.copy(
                noteText = text
            )
        }
    }

    private fun onPlayAudioClick() {
        if (state.value.playbackState == PlaybackState.PAUSED) {
            audioPlayer.resume()
        } else {
            audioPlayer.play(
                filePath = recordingDetails.filePath ?: throw IllegalArgumentException(
                    "File path can't be null"
                ),
                onComplete = {
                    _state.update {
                        it.copy(
                            playbackState = PlaybackState.STOPPED,
                            durationPlayed = Duration.ZERO
                        )
                    }
                }
            )

            durationJob = audioPlayer
                .activeTrack
                .filterNotNull()
                .onEach { track ->
                    _state.update {
                        it.copy(
                            playbackState = if (track.isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED,
                            durationPlayed = track.durationPlayed
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun onTrackSizeAvailable(trackSizeInfo: TrackSizeInfo) {
        viewModelScope.launch(Dispatchers.Default) {
            val finalAmplitudes = AmplitudeNormalizer.normalize(
                sourceAmplitudes = recordingDetails.amplitudes,
                trackWidth = trackSizeInfo.trackWidth,
                barWidth = trackSizeInfo.barWidth,
                spacing = trackSizeInfo.spacing
            )

            _state.update {
                it.copy(
                    playbackAmplitudes = finalAmplitudes
                )
            }
        }
    }

    private fun onTitleTextChange(text: String) {
        _state.update {
            it.copy(
                titleText = text,
                canSaveJive = text.isNotBlank() && it.mood != null
            )
        }
    }

    private fun onSaveClick() {
        if (recordingDetails.filePath == null || !state.value.canSaveJive) {
            return
        }

        viewModelScope.launch {
            val savedFilePath = recordingStorage.savePersistently(
                tempFilePath = recordingDetails.filePath!!
            )
            if (savedFilePath == null) {
                eventChannel.send(CreateJiveEvent.FailedToSaveFile)
                return@launch
            }

            val currentState = state.value
            val jive = Jive(
                mood = currentState.mood?.let {
                    Mood.valueOf(it.name)
                } ?: throw IllegalStateException("Mood must be set before saving."),
                title = currentState.titleText.trim(),
                note = currentState.noteText.ifBlank { null },
                topics = currentState.topics,
                audioFilePath = savedFilePath,
                audioPlaybackLength = currentState.playbackTotalDuration,
                audioAmplitudes = recordingDetails.amplitudes,
                recordedAt = Instant.now()
            )

            jiveDataSource.insertJive(jive)
            eventChannel.send(CreateJiveEvent.JiveSuccessfullySaved)
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
            jiveDataSource.observeTopics()
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
                canSaveJive = it.titleText.isNotBlank(),
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