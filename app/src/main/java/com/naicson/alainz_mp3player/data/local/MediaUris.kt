package com.naicson.alainz_mp3player.data.local

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import com.naicson.alainz_mp3player.data.model.Song

fun Song.contentUri(): Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
