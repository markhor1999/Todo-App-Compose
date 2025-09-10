package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.components.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.codingwithsalman.apps.todo.app.compose.R
import com.codingwithsalman.jotjive.core.presentation.designsystem.chips.HashtagChip
import com.codingwithsalman.jotjive.core.presentation.designsystem.dropdowns.Selectable
import com.codingwithsalman.jotjive.core.presentation.designsystem.dropdowns.Selectable.Companion.asUnselectedItems
import com.codingwithsalman.jotjive.core.presentation.designsystem.dropdowns.SelectableDropDownOptionsMenu
import com.codingwithsalman.jotjive.core.presentation.designsystem.dropdowns.SelectableOptionExtras
import com.codingwithsalman.jotjive.core.presentation.designsystem.text_fields.TransparentHintTextField
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.JotJiveTheme

@Composable
fun CreateTopicsRow(
    topics: List<String>,
    addTopicText: String,
    showCreateTopicOption: Boolean,
    showTopicSuggestions: Boolean,
    searchResults: List<Selectable<String>>,
    onTopicClick: (String) -> Unit,
    onDismissTopicSuggestions: () -> Unit,
    onRemoveTopicClick: (String) -> Unit,
    onAddTopicTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    dropDownMaxHeight: Dp = (LocalConfiguration.current.screenHeightDp * 0.3).dp
) {
    var topicRowHeight by remember {
        mutableIntStateOf(0)
    }
    var chipHeight by remember {
        mutableIntStateOf(0)
    }
    Row(
        modifier = modifier
            .onSizeChanged {
                topicRowHeight = it.height
            }
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.hashtag),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .defaultMinSize(
                    minHeight = with(LocalDensity.current) {
                        chipHeight.toDp()
                    }
                )
        )
        FlowRow(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            topics.forEach { topic ->
                HashtagChip(
                    text = topic,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.remove_topic, topic),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    onRemoveTopicClick(topic)
                                }
                        )
                    },
                    modifier = Modifier
                        .onSizeChanged { chipHeight = it.height }
                )
            }
            TransparentHintTextField(
                text = addTopicText,
                onValueChange = onAddTopicTextChange,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .defaultMinSize(
                        minHeight = with(LocalDensity.current) {
                            chipHeight.toDp()
                        }
                    )
                    .fillMaxHeight(),
                maxLines = 1,
                hintText = if (topics.isEmpty()) {
                    stringResource(R.string.topic)
                } else null
            )
        }

        if (showTopicSuggestions) {
            SelectableDropDownOptionsMenu(
                items = searchResults,
                itemDisplayText = { it },
                onDismiss = onDismissTopicSuggestions,
                key = { it },
                onItemClick = { onTopicClick(it.item) },
                leadingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.hashtag),
                        contentDescription = null,
                        modifier = Modifier
                            .size(14.dp)
                    )
                },
                maxDropDownHeight = dropDownMaxHeight,
                dropDownOffset = IntOffset(
                    x = 0,
                    y = topicRowHeight
                ),
                dropDownExtras = if (showCreateTopicOption) {
                    SelectableOptionExtras(
                        text = addTopicText,
                        onClick = { onTopicClick(addTopicText) }
                    )
                } else null
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun JiveTopicsRowPreview() {
    JotJiveTheme {
        CreateTopicsRow(
            topics = listOf(),
            addTopicText = "",
            showCreateTopicOption = true,
            showTopicSuggestions = true,
            searchResults = listOf(
                "hello",
                "helloworld"
            ).asUnselectedItems(),
            onTopicClick = {},
            onDismissTopicSuggestions = {},
            onRemoveTopicClick = {},
            onAddTopicTextChange = {},
            modifier = Modifier
                .padding(top = 64.dp)
                .fillMaxSize()
        )
    }
}