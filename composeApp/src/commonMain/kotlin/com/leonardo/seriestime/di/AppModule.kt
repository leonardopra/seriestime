package com.leonardo.seriestime.di

import com.leonardo.seriestime.data.supabase.AuthRepository
import com.leonardo.seriestime.data.supabase.createSupabase
import com.leonardo.seriestime.ui.auth.AuthViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val appModule = module {
    single { createSupabase() }
    single { AuthRepository(get()) }

    viewModel { AuthViewModel(get()) }
}

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(appModule)
    }
}
