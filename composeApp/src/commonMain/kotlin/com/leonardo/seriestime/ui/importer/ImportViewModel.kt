package com.leonardo.seriestime.ui.importer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leonardo.seriestime.data.importer.ImportProgress
import com.leonardo.seriestime.data.importer.ImportReport
import com.leonardo.seriestime.data.importer.TvTimeExport
import com.leonardo.seriestime.data.importer.TvTimeImporter
import com.leonardo.seriestime.data.importer.TvTimeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data class Parsed(val export: TvTimeExport) : ImportUiState
    data class Running(val progress: ImportProgress?) : ImportUiState
    data class Finished(val report: ImportReport) : ImportUiState
    data class Failed(val message: String) : ImportUiState
}

class ImportViewModel(private val importer: TvTimeImporter) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState

    private var export: TvTimeExport? = null

    fun onFilesPicked(contents: List<String>) {
        viewModelScope.launch {
            try {
                val parsed = withContext(Dispatchers.Default) {
                    TvTimeParser.parseAll(contents)
                }
                export = parsed
                _uiState.value = ImportUiState.Parsed(parsed)
            } catch (e: Exception) {
                _uiState.value = ImportUiState.Failed(e.message ?: "Parse error")
            }
        }
    }

    fun startImport(dryRun: Boolean = false) {
        val data = export ?: return
        viewModelScope.launch {
            _uiState.value = ImportUiState.Running(null)
            try {
                val report = importer.import(data, dryRun = dryRun) { progress ->
                    _uiState.update {
                        if (it is ImportUiState.Running) ImportUiState.Running(progress) else it
                    }
                }
                _uiState.value = ImportUiState.Finished(report)
            } catch (e: Exception) {
                _uiState.value = ImportUiState.Failed(e.message ?: "Import error")
            }
        }
    }

    fun reset() {
        export = null
        _uiState.value = ImportUiState.Idle
    }
}
