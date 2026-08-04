package com.naicson.alainz_mp3player.di

import com.naicson.alainz_mp3player.data.repository.MediaStoreMusicRepository
import com.naicson.alainz_mp3player.data.repository.MusicRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindMusicRepository(impl: MediaStoreMusicRepository): MusicRepository
}
