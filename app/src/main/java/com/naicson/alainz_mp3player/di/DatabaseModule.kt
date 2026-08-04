package com.naicson.alainz_mp3player.di

import android.content.Context
import androidx.room.Room
import com.naicson.alainz_mp3player.data.local.AppDatabase
import com.naicson.alainz_mp3player.data.local.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "alainz_mp3player.db")
            // No shipped users yet (still pre-release) — schema shape changes (like customCoverVariant:Int → customCoverUri:String) don't need a real migration.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideSongDao(database: AppDatabase): SongDao = database.songDao()
}
