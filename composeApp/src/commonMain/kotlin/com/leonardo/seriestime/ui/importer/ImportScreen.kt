package com.leonardo.seriestime.ui.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leonardo.seriestime.data.importer.ImportPhase
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import seriestime.composeapp.generated.resources.Res
import seriestime.composeapp.generated.resources.import_dry_run
import seriestime.composeapp.generated.resources.import_pick_files
import seriestime.composeapp.generated.resources.import_start
import seriestime.composeapp.generated.resources.import_summary
import seriestime.composeapp.generated.resources.import_title
import seriestime.composeapp.generated.resources.import_unmatched

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    viewModel: ImportViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val picker = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("json")),
        mode = PickerMode.Multiple(),
    ) { files ->
        if (!files.isNullOrEmpty()) {
            scope.launch {
                val contents = files.map { it.readBytes().decodeToString() }
                viewModel.onFilesPicked(contents)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.import_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val s = state) {
                is ImportUiState.Idle -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(onClick = { picker.launch() }) {
                        Text(stringResource(Res.string.import_pick_files))
                    }
                }

                is ImportUiState.Parsed -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(
                            Res.string.import_summary,
                            s.export.movies.size,
                            s.export.series.size,
                            s.export.episodeCount,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { viewModel.startImport(dryRun = false) }) {
                        Text(stringResource(Res.string.import_start))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { viewModel.startImport(dryRun = true) }) {
                        Text(stringResource(Res.string.import_dry_run))
                    }
                }

                is ImportUiState.Running -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val progress = s.progress
                    if (progress == null || progress.total == 0) {
                        CircularProgressIndicator()
                    } else {
                        Text("${phaseLabel(progress.phase)} ${progress.done}/${progress.total}")
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress.done.toFloat() / progress.total },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                is ImportUiState.Finished -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item {
                        Text(
                            text = (if (s.report.dryRun) "[DRY RUN] " else "") +
                                "🎬 ${s.report.matchedMovies}  ·  📺 ${s.report.matchedSeries}  ·  " +
                                "✔ ${s.report.importedEpisodes} ep.",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (s.report.unmatchedMovies.isNotEmpty() || s.report.unmatchedSeries.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.import_unmatched),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        items(s.report.unmatchedMovies + s.report.unmatchedSeries) {
                            Text("• ${it.title}${it.year?.let { y -> " ($y)" } ?: ""}")
                        }
                    }
                    items(s.report.seriesSummaries.filter { it.unmatchedEpisodes > 0 }) {
                        Text(
                            text = "${it.title}: ${it.matchedEpisodes} ok, ${it.unmatchedEpisodes} non abbinati",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item {
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = viewModel::reset) { Text("↺") }
                            Button(onClick = onBack) { Text("OK") }
                        }
                    }
                }

                is ImportUiState.Failed -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = viewModel::reset) { Text("↺") }
                }
            }
        }
    }
}

private fun phaseLabel(phase: ImportPhase): String = when (phase) {
    ImportPhase.MatchingMovies -> "🎬"
    ImportPhase.MatchingSeries -> "📺"
    ImportPhase.Uploading -> "⬆"
    ImportPhase.Done -> "✔"
}
