package org.jellyfin.androidtv.data.eventhandling

/**
 * Name of the optional `DisplayContent` argument with which the server (or a plugin) asks the app to stop the
 * current playback and show the content anyway. Regular clients that control this device never send it, so
 * browsing on another device does not interrupt a running video. Apps that do not know it simply ignore it.
 */
const val INTERRUPT_PLAYBACK_ARGUMENT = "InterruptPlayback"

enum class DisplayContentAction {
	/** Nothing is playing: show the content. */
	LAUNCH,

	/** Something is playing and the sender did not ask to interrupt it: leave the playback alone. */
	IGNORE,

	/** Something is playing and the sender explicitly asked to interrupt it: stop it, then show the content. */
	INTERRUPT_AND_LAUNCH,
}

fun decideDisplayContentAction(playbackActive: Boolean, interruptPlayback: Boolean) = when {
	!playbackActive -> DisplayContentAction.LAUNCH
	interruptPlayback -> DisplayContentAction.INTERRUPT_AND_LAUNCH
	else -> DisplayContentAction.IGNORE
}

/** Whether the command arguments contain [INTERRUPT_PLAYBACK_ARGUMENT] set to `true` (name and value are case-insensitive). */
fun Map<String, *>.isInterruptPlaybackRequested(): Boolean = entries.any { (key, value) ->
	key.equals(INTERRUPT_PLAYBACK_ARGUMENT, ignoreCase = true) && value?.toString().equals("true", ignoreCase = true)
}
