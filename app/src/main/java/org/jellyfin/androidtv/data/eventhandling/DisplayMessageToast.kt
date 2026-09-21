package org.jellyfin.androidtv.data.eventhandling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Name of the optional `DisplayMessage` argument with how long the message should stay on screen, in milliseconds. */
const val DISPLAY_MESSAGE_TIMEOUT_ARGUMENT = "TimeoutMs"

/** How long a single Android toast stays visible (`Toast.LENGTH_LONG`). */
const val TOAST_LONG_DURATION_MS = 3_500L

private const val MIN_TIMEOUT_MS = 1_000L
private const val MAX_TIMEOUT_MS = 30_000L

/**
 * Header on its own line above the text, so a long text does not push the header out of view. Blank parts are left
 * out; the result is empty when there is nothing to show.
 */
fun buildDisplayMessageText(header: String?, text: String?): String =
	listOfNotNull(header, text).filter { it.isNotBlank() }.joinToString("\n")

/** Reads the requested display time from the command arguments, limited to a sane range. Null when not requested. */
fun Map<String, *>.displayMessageTimeoutMs(): Long? = entries
	.firstOrNull { (key, _) -> key.equals(DISPLAY_MESSAGE_TIMEOUT_ARGUMENT, ignoreCase = true) }
	?.value?.toString()?.toLongOrNull()
	?.coerceIn(MIN_TIMEOUT_MS, MAX_TIMEOUT_MS)

/**
 * Lengths of the toasts to show one after another so the message stays for [timeoutMs]. A toast cannot last longer
 * than [sliceMs], so longer requests are covered by showing it again.
 */
fun toastSlices(timeoutMs: Long, sliceMs: Long = TOAST_LONG_DURATION_MS): List<Long> = buildList {
	var remaining = timeoutMs
	while (remaining > 0) {
		val slice = minOf(remaining, sliceMs)
		add(slice)
		remaining -= slice
	}
}

/** A toast that is on screen and can be taken down. */
fun interface ShownToast {
	fun cancel()
}

/**
 * Shows one message at a time: a new message takes the previous one off the screen first, whether or not the
 * previous one asked for a specific display time. Messages without a time stay for one toast duration.
 *
 * [show] puts the text on screen and returns the toast; it is called from [scope], which must run on the thread that
 * owns the toasts. [display] can be called from any thread.
 */
class DisplayMessageToaster(
	scope: CoroutineScope,
	private val sliceMs: Long = TOAST_LONG_DURATION_MS,
	private val show: (String) -> ShownToast,
) {
	private class Request(val message: String, val timeoutMs: Long?)

	// Only the newest request matters. collectLatest ends the previous one, and waits for its cleanup, before starting
	private val requests = Channel<Request>(Channel.CONFLATED)

	init {
		scope.launch {
			requests.receiveAsFlow().collectLatest { request ->
				var toast: ShownToast? = null
				try {
					for (slice in toastSlices(request.timeoutMs ?: sliceMs, sliceMs)) {
						toast?.cancel()
						toast = show(request.message)
						delay(slice)
					}
				} finally {
					toast?.cancel()
				}
			}
		}
	}

	fun display(message: String, timeoutMs: Long?) {
		if (message.isNotBlank()) requests.trySend(Request(message, timeoutMs))
	}
}
