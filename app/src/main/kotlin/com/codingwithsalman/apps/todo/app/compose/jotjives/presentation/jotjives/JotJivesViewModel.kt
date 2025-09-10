package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithsalman.apps.todo.app.compose.R
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.AudioCaptureMethod
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.JotJiveFilterChip
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.MoodChipContent
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.PlaybackState
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.RecordingState
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.TrackSizeInfo
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.JiveUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.MoodUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.util.AmplitudeNormalizer
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.util.toJiveUi
import com.codingwithsalman.jotjive.core.domain.audio.AudioPlayer
import com.codingwithsalman.jotjive.core.domain.jive.Jive
import com.codingwithsalman.jotjive.core.domain.jive.JiveDataSource
import com.codingwithsalman.jotjive.core.domain.jot.JotDataSource
import com.codingwithsalman.jotjive.core.domain.recording.VoiceRecorder
import com.codingwithsalman.jotjive.core.presentation.designsystem.dropdowns.Selectable
import com.codingwithsalman.jotjive.core.presentation.util.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class JotJivesViewModel(
    private val voiceRecorder: VoiceRecorder,
    private val audioPlayer: AudioPlayer,
    private val jotDataSource: JotDataSource,
    private val jiveDataSource: JiveDataSource,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        private val MIN_RECORD_DURATION = 1.5.seconds
    }

    private var hasLoadedInitialData = false

    private val playingJiveId = MutableStateFlow<Int?>(null)
    private val selectedMoodFilters = MutableStateFlow<List<MoodUi>>(emptyList())
    private val selectedTopicFilters = MutableStateFlow<List<String>>(emptyList())
    private val audioTrackSizeInfo = MutableStateFlow<TrackSizeInfo?>(null)

    private val eventChannel = Channel<JotJivesEvent>()
    val events = eventChannel.receiveAsFlow()

    private val _state = MutableStateFlow(JotJivesState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                observeFilters()
                observeJives()
                fetchNavigationArgs()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = JotJivesState()
        )

    private val filteredJives = jiveDataSource
        .observeJives()
        .filterByMoodAndTopics()
        .onEach { jives ->
            _state.update {
                it.copy(
                    hasJotJivesRecorded = jives.isNotEmpty(),
                    isLoadingData = false
                )
            }
        }
        .combine(audioTrackSizeInfo) { jives, trackSizeInfo ->
            if (trackSizeInfo != null) {
                jives.map { jive ->
                    jive.copy(
                        audioAmplitudes = AmplitudeNormalizer.normalize(
                            sourceAmplitudes = jive.audioAmplitudes,
                            trackWidth = trackSizeInfo.trackWidth,
                            barWidth = trackSizeInfo.barWidth,
                            spacing = trackSizeInfo.spacing
                        )
                    )
                }
            } else jives
        }
        .flowOn(Dispatchers.Default)


    fun onAction(action: JotJivesAction) {
        when (action) {
            JotJivesAction.OnRecordFabClick -> {
                requestAudioPermission()
                _state.update {
                    it.copy(
                        currentCaptureMethod = AudioCaptureMethod.STANDARD
                    )
                }
            }

            JotJivesAction.OnRequestPermissionQuickRecording -> {
                requestAudioPermission()
                _state.update {
                    it.copy(
                        currentCaptureMethod = AudioCaptureMethod.QUICK
                    )
                }
            }

            JotJivesAction.OnRecordButtonLongClick -> {
                startRecording(captureMethod = AudioCaptureMethod.QUICK)
            }

            JotJivesAction.OnSettingsClick -> {}
            JotJivesAction.OnAddJotClick -> {
                viewModelScope.launch {
                    eventChannel.send(JotJivesEvent.AddJot)
                }
            }

            is JotJivesAction.OnRemoveFilters -> {
                when (action.filterType) {
                    JotJiveFilterChip.MOODS -> selectedMoodFilters.update { emptyList() }
                    JotJiveFilterChip.TOPICS -> selectedTopicFilters.update { emptyList() }
                }
            }

            JotJivesAction.OnTopicChipClick -> {
                _state.update {
                    it.copy(
                        selectedJotJiveFilterChip = JotJiveFilterChip.TOPICS
                    )
                }
            }

            JotJivesAction.OnMoodChipClick -> {
                _state.update {
                    it.copy(
                        selectedJotJiveFilterChip = JotJiveFilterChip.MOODS
                    )
                }
            }

            JotJivesAction.OnDismissTopicDropDown,
            JotJivesAction.OnDismissMoodDropDown -> {
                _state.update {
                    it.copy(
                        selectedJotJiveFilterChip = null
                    )
                }
            }

            is JotJivesAction.OnFilterByMoodClick -> {
                toggleMoodFilter(action.moodUi)
            }

            is JotJivesAction.OnFilterByTopicClick -> {
                toggleTopicFilter(action.topic)
            }

            is JotJivesAction.OnPlayJiveClick -> onPlayJiveClick(action.jiveId)
            is JotJivesAction.OnTrackSizeAvailable -> {
                audioTrackSizeInfo.update { action.trackSizeInfo }
            }

            is JotJivesAction.OnAudioPermissionGranted -> {
                startRecording(captureMethod = AudioCaptureMethod.STANDARD)
            }

            JotJivesAction.OnPauseAudioClick -> audioPlayer.pause()

            JotJivesAction.OnPauseRecordingClick -> pauseRecording()

            JotJivesAction.OnCancelRecording -> cancelRecording()

            JotJivesAction.OnCompleteRecording -> stopRecording()

            JotJivesAction.OnResumeRecordingClick -> resumeRecording()
        }
    }

    private fun fetchNavigationArgs() {
        val startRecording = savedStateHandle["startRecording"] ?: false
        if (startRecording) {
            _state.update {
                it.copy(
                    currentCaptureMethod = AudioCaptureMethod.STANDARD
                )
            }
            requestAudioPermission()
        }
    }

    private fun observeJives() {
        combine(
            filteredJives,
            playingJiveId,
            audioPlayer.activeTrack
        ) { jives, playingJiveId, activeTrack ->
            if (playingJiveId == null || activeTrack == null) {
                return@combine jives.map { it.toJiveUi() }
            }

            jives.map { jive ->
                if (jive.id == playingJiveId) {
                    jive.toJiveUi(
                        currentPlaybackDuration = activeTrack.durationPlayed,
                        playbackState = if (activeTrack.isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
                    )
                } else jive.toJiveUi()
            }
        }
            .groupByRelativeDate()
            .onEach { groupedJives ->
                _state.update {
                    it.copy(
                        jives = groupedJives
                    )
                }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    private fun onPlayJiveClick(jiveId: Int) {
        val selectedJive = state.value.jives.values.flatten().first { it.id == jiveId }
        val activeTrack = audioPlayer.activeTrack.value
        val isNewJive = playingJiveId.value != jiveId
        val isSameJiveIsPlayingFromBeginning = jiveId == playingJiveId.value && activeTrack != null
                && activeTrack.durationPlayed == Duration.ZERO

        when {
            isNewJive || isSameJiveIsPlayingFromBeginning -> {
                playingJiveId.update { jiveId }
                audioPlayer.stop()
                audioPlayer.play(
                    filePath = selectedJive.audioFilePath,
                    onComplete = ::completePlayback
                )
            }

            else -> audioPlayer.resume()
        }
    }

    private fun completePlayback() {
        _state.update {
            it.copy(
                jives = it.jives.mapValues { (_, jives) ->
                    jives.map { jive ->
                        jive.copy(
                            playbackCurrentDuration = Duration.ZERO
                        )
                    }
                }
            )
        }
        playingJiveId.update { null }
    }

    private fun requestAudioPermission() = viewModelScope.launch {
        eventChannel.send(JotJivesEvent.RequestAudioPermission)
    }

    private fun pauseRecording() {
        voiceRecorder.pause()
        _state.update {
            it.copy(
                recordingState = RecordingState.PAUSED
            )
        }
    }

    private fun resumeRecording() {
        voiceRecorder.resume()
        _state.update {
            it.copy(
                recordingState = RecordingState.NORMAL_CAPTURE
            )
        }
    }

    private fun cancelRecording() {
        _state.update {
            it.copy(
                recordingState = RecordingState.NOT_RECORDING,
                currentCaptureMethod = null
            )
        }
        voiceRecorder.cancel()
    }

    private fun stopRecording() {
        voiceRecorder.stop()
        _state.update {
            it.copy(
                recordingState = RecordingState.NOT_RECORDING
            )
        }

        val recordingDetails = voiceRecorder.recordingDetails.value
        viewModelScope.launch {
            if (recordingDetails.duration < MIN_RECORD_DURATION) {
                eventChannel.send(JotJivesEvent.RecordingTooShort)
            } else {
                eventChannel.send(
                    JotJivesEvent.OnDoneRecording(
                        details = recordingDetails.copy(
                            // Arbitrary track dimensions to not make the app crash
                            // when navigating and passing the amplitudes as an argument.
                            amplitudes = AmplitudeNormalizer.normalize(
                                sourceAmplitudes = recordingDetails.amplitudes,
                                trackWidth = 10_000f,
                                barWidth = 20f,
                                spacing = 15f
                            )
                        )
                    )
                )
            }
        }
    }

    private fun startRecording(captureMethod: AudioCaptureMethod) {
        _state.update {
            it.copy(
                recordingState = when (captureMethod) {
                    AudioCaptureMethod.STANDARD -> RecordingState.NORMAL_CAPTURE
                    AudioCaptureMethod.QUICK -> RecordingState.QUICK_CAPTURE
                }
            )
        }
        voiceRecorder.start()

        if (captureMethod == AudioCaptureMethod.STANDARD) {
            voiceRecorder
                .recordingDetails
                .distinctUntilChangedBy { it.duration }
                .map { it.duration }
                .onEach { duration ->
                    _state.update {
                        it.copy(
                            recordingElapsedDuration = duration
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun toggleMoodFilter(moodUi: MoodUi) {
        selectedMoodFilters.update { selectedMoods ->
            if (moodUi in selectedMoods) {
                selectedMoods - moodUi
            } else {
                selectedMoods + moodUi
            }
        }
    }

    private fun toggleTopicFilter(topic: String) {
        selectedTopicFilters.update { selectedTopics ->
            if (topic in selectedTopics) {
                selectedTopics - topic
            } else {
                selectedTopics + topic
            }
        }
    }

    private fun observeFilters() {
        combine(
            jiveDataSource.observeTopics(),
            selectedTopicFilters,
            selectedMoodFilters
        ) { allTopics, selectedTopics, selectedMoods ->
            _state.update {
                it.copy(
                    topics = allTopics.map { topic ->
                        Selectable(
                            item = topic,
                            selected = selectedTopics.contains(topic)
                        )
                    },
                    moods = MoodUi.entries.map { moodUi ->
                        Selectable(
                            item = moodUi,
                            selected = selectedMoods.contains(moodUi)
                        )
                    },
                    hasActiveMoodFilters = selectedMoods.isNotEmpty(),
                    hasActiveTopicFilters = selectedTopics.isNotEmpty(),
                    topicChipTitle = selectedTopics.deriveTopicsToText(),
                    moodChipContent = selectedMoods.asMoodChipContent()
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun List<String>.deriveTopicsToText(): UiText {
        return when (size) {
            0 -> UiText.StringResource(R.string.all_topics)
            1 -> UiText.Dynamic(this.first())
            2 -> UiText.Dynamic("${this.first()}, ${this.last()}")
            else -> {
                val extraElementCount = size - 2
                UiText.Dynamic("${this.first()}, ${this[1]} +$extraElementCount")
            }
        }
    }

    private fun List<MoodUi>.asMoodChipContent(): MoodChipContent {
        if (this.isEmpty()) {
            return MoodChipContent()
        }

        val icons = this.map { it.iconSet.fill }
        val moodNames = this.map { it.title }

        return when (size) {
            1 -> MoodChipContent(
                iconsRes = icons,
                title = moodNames.first()
            )

            2 -> MoodChipContent(
                iconsRes = icons,
                title = UiText.Combined(
                    format = "%s, %s",
                    uiTexts = moodNames.toTypedArray()
                )
            )

            else -> {
                val extraElementCount = size - 2
                MoodChipContent(
                    iconsRes = icons,
                    title = UiText.Combined(
                        format = "%s, %s +$extraElementCount",
                        uiTexts = moodNames.take(2).toTypedArray()
                    )
                )
            }
        }
    }

    private fun Flow<List<Jive>>.filterByMoodAndTopics(): Flow<List<Jive>> {
        return combine(
            this,
            selectedMoodFilters,
            selectedTopicFilters
        ) { jives, moodFilters, topicFilters ->
            jives.filter { jive ->
                val matchesMoodFilter = moodFilters
                    .takeIf { it.isNotEmpty() }
                    ?.any { it.name == jive.mood.name }
                    ?: true
                val matchesTopicFilter = topicFilters
                    .takeIf { it.isNotEmpty() }
                    ?.any { it in jive.topics }
                    ?: true

                matchesMoodFilter && matchesTopicFilter
            }
        }
    }

    private fun Flow<List<JiveUi>>.groupByRelativeDate(): Flow<Map<UiText, List<JiveUi>>> {
        val formatter = DateTimeFormatter.ofPattern("dd MMM")
        val today = LocalDate.now()
        return map { jives ->
            jives
                .groupBy { jive ->
                    LocalDate.ofInstant(
                        jive.recordedAt,
                        ZoneId.systemDefault()
                    )
                }
                .mapValues { (_, jives) ->
                    jives.sortedByDescending { it.recordedAt }
                }
                .toSortedMap(compareByDescending { it })
                .mapKeys { (date, _) ->
                    when (date) {
                        today -> UiText.StringResource(R.string.today)
                        today.minusDays(1) -> UiText.StringResource(R.string.yesterday)
                        else -> UiText.Dynamic(date.format(formatter))
                    }
                }
        }
    }
}