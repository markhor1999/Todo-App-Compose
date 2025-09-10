package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.create_jive

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithsalman.apps.todo.app.compose.R
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.components.JiveMoodPlayer
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.create_jive.components.JiveTopicsRow
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.create_jive.components.SelectMoodSheet
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.MoodUi
import com.codingwithsalman.jotjive.core.presentation.designsystem.buttons.PrimaryButton
import com.codingwithsalman.jotjive.core.presentation.designsystem.buttons.SecondaryButton
import com.codingwithsalman.jotjive.core.presentation.designsystem.text_fields.TransparentHintTextField
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.JotJiveTheme
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.secondary70
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.secondary95
import com.codingwithsalman.jotjive.core.presentation.util.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

@Composable
fun CreateJiveRoot(
    onConfirmLeave: () -> Unit,
    viewModel: CreateJiveViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            CreateJiveEvent.FailedToSaveFile -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.error_couldnt_save_file),
                    Toast.LENGTH_LONG
                ).show()
                onConfirmLeave()
            }

            CreateJiveEvent.JiveSuccessfullySaved -> {
                onConfirmLeave()
            }
        }
    }

    CreateJiveScreen(
        state = state,
        onAction = viewModel::onAction,
        onConfirmLeave = onConfirmLeave
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateJiveScreen(
    state: CreateJiveState,
    onConfirmLeave: () -> Unit,
    onAction: (CreateJiveAction) -> Unit,
) {
    BackHandler(
        enabled = !state.showConfirmLeaveDialog
    ) {
        onAction(CreateJiveAction.OnGoBack)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.new_entry),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onAction(CreateJiveAction.OnNavigateBackClick)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val descriptionFocusRequester = remember {
            FocusRequester()
        }
        val focusManager = LocalFocusManager.current
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.mood == null) {
                    FilledIconButton(
                        onClick = {
                            onAction(CreateJiveAction.OnSelectMoodClick)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary95,
                            contentColor = MaterialTheme.colorScheme.secondary70
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_mood)
                        )
                    }
                } else {
                    Image(
                        imageVector = ImageVector.vectorResource(state.mood.iconSet.fill),
                        contentDescription = state.mood.title.asString(),
                        modifier = Modifier
                            .height(32.dp)
                            .clickable {
                                onAction(CreateJiveAction.OnSelectMoodClick)
                            },
                        contentScale = ContentScale.FillHeight
                    )
                }

                TransparentHintTextField(
                    text = state.titleText,
                    onValueChange = {
                        onAction(CreateJiveAction.OnTitleTextChange(it))
                    },
                    modifier = Modifier
                        .weight(1f),
                    hintText = stringResource(R.string.add_title),
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            descriptionFocusRequester.requestFocus()
                        }
                    )
                )
            }

            JiveMoodPlayer(
                moodUi = state.mood,
                playbackState = state.playbackState,
                playerProgress = { state.durationPlayedRatio },
                durationPlayed = state.durationPlayed,
                totalPlaybackDuration = state.playbackTotalDuration,
                powerRatios = state.playbackAmplitudes,
                onPlayClick = {
                    onAction(CreateJiveAction.OnPlayAudioClick)
                },
                onPauseClick = {
                    onAction(CreateJiveAction.OnPauseAudioClick)
                },
                onTrackSizeAvailable = {
                    onAction(CreateJiveAction.OnTrackSizeAvailable(it))
                }
            )

            JiveTopicsRow(
                topics = state.topics,
                addTopicText = state.addTopicText,
                showCreateTopicOption = state.showCreateTopicOption,
                showTopicSuggestions = state.showTopicSuggestions,
                searchResults = state.searchResults,
                onTopicClick = {
                    onAction(CreateJiveAction.OnTopicClick(it))
                },
                onDismissTopicSuggestions = {
                    onAction(CreateJiveAction.OnDismissTopicSuggestions)
                },
                onRemoveTopicClick = {
                    onAction(CreateJiveAction.OnRemoveTopicClick(it))
                },
                onAddTopicTextChange = {
                    onAction(CreateJiveAction.OnAddTopicTextChange(it))
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Create,
                    contentDescription = stringResource(R.string.add_description),
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(16.dp)
                )
                TransparentHintTextField(
                    text = state.noteText,
                    onValueChange = {
                        onAction(CreateJiveAction.OnNoteTextChange(it))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(descriptionFocusRequester),
                    hintText = stringResource(R.string.add_description),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecondaryButton(
                    text = stringResource(R.string.cancel),
                    onClick = {
                        onAction(CreateJiveAction.OnCancelClick)
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                )
                PrimaryButton(
                    text = stringResource(R.string.save),
                    onClick = {
                        onAction(CreateJiveAction.OnSaveClick)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = state.canSaveJive,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.save),
                            modifier = Modifier
                                .size(16.dp)
                        )
                    }
                )
            }
        }

        if (state.showMoodSelector) {
            SelectMoodSheet(
                selectedMood = state.selectedMood,
                onMoodClick = {
                    onAction(CreateJiveAction.OnMoodClick(it))
                },
                onDismiss = {
                    onAction(CreateJiveAction.OnDismissMoodSelector)
                },
                onConfirmClick = {
                    onAction(CreateJiveAction.OnConfirmMood)
                }
            )
        }

        if (state.showConfirmLeaveDialog) {
            AlertDialog(
                onDismissRequest = {
                    onAction(CreateJiveAction.OnDismissConfirmLeaveDialog)
                },
                confirmButton = {
                    TextButton(
                        onClick = onConfirmLeave,
                    ) {
                        Text(
                            text = stringResource(R.string.discard),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onAction(CreateJiveAction.OnDismissConfirmLeaveDialog)
                        },
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.discard_recording)
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.this_cannot_be_undone)
                    )
                }
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    JotJiveTheme {
        CreateJiveScreen(
            state = CreateJiveState(
                mood = MoodUi.EXCITED,
                canSaveJive = true
            ),
            onAction = {},
            onConfirmLeave = {}
        )
    }
}