package com.ysajid.names99

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

/**
 * Step five of the plan: my voice on the names.
 *
 * Drop clips into app/src/main/assets/audio as 01.mp3 .. 99.mp3, numbered by
 * the traditional order. The speaker button only appears for names that
 * actually have a clip, so recording ten of them is a perfectly good state to
 * leave this in.
 */
class Voice(private val context: Context) {

    private val recorded: Set<String> by lazy {
        runCatching { context.assets.list(FOLDER)?.toSet() }.getOrNull().orEmpty()
    }

    private var player: MediaPlayer? = null

    fun has(n: Int): Boolean = fileFor(n) in recorded

    fun play(n: Int, onFinished: () -> Unit) {
        stop()
        if (!has(n)) return onFinished()
        try {
            context.assets.openFd("$FOLDER/${fileFor(n)}").use { fd ->
                player = MediaPlayer().apply {
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    setOnCompletionListener { stop(); onFinished() }
                    setOnErrorListener { _, _, _ -> stop(); onFinished(); true }
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "could not play clip $n", e)
            stop()
            onFinished()
        }
    }

    fun stop() {
        player?.runCatching { if (isPlaying) stop(); release() }
        player = null
    }

    private fun fileFor(n: Int) = "%02d.mp3".format(n)

    private companion object {
        const val FOLDER = "audio"
        const val TAG = "Voice"
    }
}
