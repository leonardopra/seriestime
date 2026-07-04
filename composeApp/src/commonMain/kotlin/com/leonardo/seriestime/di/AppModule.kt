package com.leonardo.seriestime.di

import com.leonardo.seriestime.data.supabase.AuthRepository
import com.leonardo.seriestime.data.supabase.LibraryRepository
import com.leonardo.seriestime.data.supabase.createSupabase
import com.leonardo.seriestime.data.importer.TvTimeImporter
import com.leonardo.seriestime.data.tmdb.TmdbApi
import com.leonardo.seriestime.ui.auth.AuthViewModel
import com.leonardo.seriestime.ui.importer.ImportViewModel
import com.leonardo.seriestime.ui.home.MoviesTabViewModel
import com.leonardo.seriestime.ui.home.ShowsTabViewModel
import com.leonardo.seriestime.ui.moviedetail.MovieDetailViewModel
import com.leonardo.seriestime.ui.search.SearchViewModel
import com.leonardo.seriestime.ui.showdetail.ShowDetailViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val appModule = module {
    single { createSupabase() }
    single { AuthRepository(get()) }
    single { LibraryRepository(get()) }
    single { TmdbApi() }
    single { TvTimeImporter(get<TmdbApi>(), get()) }

    viewModel { AuthViewModel(get()) }
    viewModel { ImportViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { MoviesTabViewModel(get()) }
    viewModel { ShowsTabViewModel(get()) }
    viewModel { (tmdbId: Int) -> MovieDetailViewModel(tmdbId, get(), get()) }
    viewModel { (tmdbId: Int) -> ShowDetailViewModel(tmdbId, get(), get()) }
}

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(appModule)
    }
}
