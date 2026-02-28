package org.androidbible.ui.screens.bible

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import org.androidbible.domain.model.Verse

class BibleScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { BibleScreenModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.currentBookName.ifEmpty { "Bible" } +
                                if (state.currentChapter > 0) " ${state.currentChapter}" else "",
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        ) { padding ->
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.verses.isNotEmpty() -> {
                    VerseList(
                        verses = state.verses,
                        modifier = Modifier.padding(padding),
                        onVerseLongClick = { /* TODO: show marker options */ },
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        Text(
                            "Select a Bible version to start reading",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerseList(
    verses: List<Verse>,
    modifier: Modifier = Modifier,
    onVerseLongClick: (Verse) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        items(verses, key = { it.ari }) { verse ->
            VerseItem(
                verse = verse,
                onLongClick = { onVerseLongClick(verse) },
            )
        }
    }
}

@Composable
fun VerseItem(
    verse: Verse,
    onLongClick: () -> Unit = {},
) {
    val annotatedText = buildAnnotatedString {
        withStyle(
            SpanStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        ) {
            append("${verse.verse} ")
        }
        append(verse.text)
    }

    Text(
        text = annotatedText,
        style = MaterialTheme.typography.bodyLarge.copy(
            lineHeight = 28.sp,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLongClick() }
            .padding(vertical = 2.dp),
    )
}
