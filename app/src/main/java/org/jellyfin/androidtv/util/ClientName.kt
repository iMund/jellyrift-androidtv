package org.jellyfin.androidtv.util

import org.jellyfin.androidtv.BuildConfig

/**
 * Name the app reports to the server as its client. Servers and plugins tell apps apart by it, so JellyRift uses its
 * own name instead of the one of the official app. Debug builds get a suffix, so they can be told apart too.
 */
val appClientName: String
	get() = buildString {
		append("JellyRift")
		if (BuildConfig.DEBUG) append(" (debug)")
	}
