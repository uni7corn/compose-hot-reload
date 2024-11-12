package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.LogMessage
import org.jetbrains.compose.reload.orchestration.asFlow

@Composable
fun DevToolingConsole(tag: String, modifier: Modifier) {
    val listState = rememberLazyListState()
    var outputLines by remember { mutableStateOf(listOf<String>()) }
    var isExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        ComposeHotReloadAgent.orchestration.asFlow().filterIsInstance<LogMessage>()
            .filter { message -> message.tag == tag }
            .collect { value ->
                outputLines = (outputLines + value.message).takeLast(2048)
                listState.scrollToItem(outputLines.lastIndex)
            }
    }

    Column(modifier = modifier) {
        ExpandButton(
            isExpanded = isExpanded,
            onClick = { expanded -> isExpanded = expanded },
            Modifier.padding(horizontal = 16.dp)
        ) {
            Text(tag, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }

        AnimatedVisibility (isExpanded) {
            Card(Modifier.padding(16.dp).fillMaxWidth()) {
                Box(Modifier.padding(16.dp)) {
                    LazyColumn(state = listState, modifier = Modifier.wrapContentHeight()) {
                        items(outputLines) { text ->
                            Row {
                                Text(text, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandButton(
    isExpanded: Boolean,
    onClick: (expanded: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(if(isExpanded) 0f else -90f)

    Row(modifier.clickable(onClick = {
        onClick(!isExpanded)
    }), verticalAlignment = CenterVertically) {
        Icon(
            Icons.Default.ArrowDropDown, contentDescription = null,
            modifier = Modifier.rotate(rotation),
        )
        children()
    }
}