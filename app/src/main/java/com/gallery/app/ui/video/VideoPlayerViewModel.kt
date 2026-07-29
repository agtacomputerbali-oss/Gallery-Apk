package com.gallery.app.ui.video

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val encodedVideoUri: String = savedStateHandle.get<String>("videoUri") ?: ""
    val videoUri: Uri = Uri.parse(Uri.decode(encodedVideoUri))

    private var exoPlayer: ExoPlayer? = null

    private val _playerState = MutableStateFlow<ExoPlayer?>(null)
    val playerState: StateFlow<ExoPlayer?> = _playerState.asStateFlow()

    fun initializePlayer() {
        if (exoPlayer == null && videoUri != Uri.EMPTY) {
            val player = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(videoUri)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
            exoPlayer = player
            _playerState.value = player
        }
    }

    fun releasePlayer() {
        exoPlayer?.let { player ->
            player.stop()
            player.release()
        }
        exoPlayer = null
        _playerState.value = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
