package org.antidepressants.mp5.di

import org.antidepressants.mp5.data.provider.MusicProvider
import org.antidepressants.mp5.data.provider.PipedProvider
import org.antidepressants.mp5.data.provider.SoundCloudProvider
import org.antidepressants.mp5.data.provider.YouTubeProvider
import org.antidepressants.mp5.domain.repository.MusicRepository
import org.antidepressants.mp5.settings.AppSettings
import org.koin.dsl.module

/**
 * Common Koin module for shared dependencies.
 * Platform-specific modules (Android/Desktop) should extend this.
 */
val commonModule = module {
    // Settings
    single { AppSettings() }
    
    // Music Providers (in priority order for fallback)
    single<YouTubeProvider> { YouTubeProvider() }
    single<SoundCloudProvider> { SoundCloudProvider() }
    single<PipedProvider> { PipedProvider() }
    
    // Provider list for fallback logic
    single<List<MusicProvider>> {
        listOf(
            get<YouTubeProvider>(),
            get<SoundCloudProvider>(),
            get<PipedProvider>()
        )
    }
    
    // Music Repository with fallback
    single { MusicRepository(get()) }
}

/**
 * Network module for Ktor client configuration.
 */
val networkModule = module {
    // TODO: Configure Ktor HttpClient with content negotiation
    // single {
    //     HttpClient {
    //         install(ContentNegotiation) {
    //             json(Json { ignoreUnknownKeys = true })
    //         }
    //     }
    // }
}
