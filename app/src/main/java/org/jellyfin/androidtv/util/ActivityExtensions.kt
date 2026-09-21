package org.jellyfin.androidtv.util

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.Process

private const val EXIT_PROCESS_DELAY_MS = 300L

/**
 * Closes the app for real. Leaving with the back key only sends the app to the background: the process stays alive
 * and so does its connection to the server, which keeps the session open until the server notices it is gone and
 * makes a quick return look like a session that never ended. Ending the process closes the connection right away.
 */
fun Activity.exitApp() {
	finishAndRemoveTask()

	// Let the activity finish before the process goes away
	Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, EXIT_PROCESS_DELAY_MS)
}
