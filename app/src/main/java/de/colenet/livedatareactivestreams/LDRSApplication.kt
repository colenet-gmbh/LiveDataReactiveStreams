package de.colenet.livedatareactivestreams

import android.app.Application
import org.koin.android.ext.android.startKoin
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val appModule = module {
    viewModel { MainViewModel() }
    single { LDRSRepository() }
    single { GithubRepositoriesService() }
}

class LDRSApplication: Application(){
    override fun onCreate() {
        super.onCreate()
        // Start Koin
        startKoin(this, listOf(appModule))
    }
}