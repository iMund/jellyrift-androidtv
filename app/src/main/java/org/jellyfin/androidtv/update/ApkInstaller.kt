package org.jellyfin.androidtv.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.AndroidVersion
import timber.log.Timber
import java.io.File
import java.io.IOException

/** Installs an APK over the running app with the system package installer. */
class ApkInstaller(private val context: Context) {
	/** Whether the user allowed this app to install packages. Always true before Android 8, where it is global. */
	val allowed get() = !AndroidVersion.isAtLeastO || context.packageManager.canRequestPackageInstalls()

	/** Opens the system screen where the user allows this app to install packages. False if the device has none. */
	fun openPermissionSettings(): Boolean = try {
		context.startActivity(
			Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		)
		true
	} catch (err: Exception) {
		Timber.w(err, "Unable to open the install permission settings")
		false
	}

	/**
	 * Hands the APK to the installer and deletes it: the installer keeps its own copy. The system asks the user to
	 * confirm and restarts the app when done.
	 */
	fun install(apk: File): Boolean {
		val installer = context.packageManager.packageInstaller
		var sessionId = -1

		return try {
			val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
				setSize(apk.length())
			}
			sessionId = installer.createSession(params)

			installer.openSession(sessionId).use { session ->
				apk.inputStream().use { input ->
					session.openWrite("update.apk", 0, apk.length()).use { output ->
						input.copyTo(output)
						session.fsync(output)
					}
				}

				// The installer adds its status to the intent, so the PendingIntent has to be mutable
				val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (AndroidVersion.isAtLeastS) PendingIntent.FLAG_MUTABLE else 0
				val callback = PendingIntent.getBroadcast(
					context,
					sessionId,
					Intent(context, InstallStatusReceiver::class.java).setPackage(context.packageName),
					flags,
				)
				session.commit(callback.intentSender)
			}
			true
		} catch (err: Exception) {
			if (err !is IOException && err !is SecurityException) throw err

			Timber.e(err, "Unable to install the update")
			// A session that was never committed would keep holding disk space
			if (sessionId >= 0) runCatching { installer.abandonSession(sessionId) }
			false
		} finally {
			apk.delete()
		}
	}
}

/**
 * The system asks for confirmation through an activity that Android 10+ only lets an app in the background start when
 * it has no visible window. When that happens the confirmation is kept and shown as soon as the app is back.
 */
private object PendingInstallConfirmation {
	private var pending: Intent? = null
	private var observing = false

	fun show(context: Context, confirmation: Intent) {
		val lifecycle = ProcessLifecycleOwner.get().lifecycle
		if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
			context.startActivity(confirmation)
			return
		}

		Timber.w("The app is in the background, showing the install confirmation when it returns")
		pending = confirmation
		if (!observing) {
			observing = true
			lifecycle.addObserver(object : DefaultLifecycleObserver {
				override fun onStart(owner: LifecycleOwner) {
					pending?.let(context.applicationContext::startActivity)
					pending = null
				}
			})
		}
	}
}

/** Receives the result of the installation: forwards the confirmation prompt and reports failures. */
class InstallStatusReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
			PackageInstaller.STATUS_PENDING_USER_ACTION -> {
				@Suppress("DEPRECATION")
				val confirmation = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
				confirmation?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { PendingInstallConfirmation.show(context, it) }
			}

			PackageInstaller.STATUS_SUCCESS -> Timber.i("Update installed")

			// The user declined the confirmation: nothing failed
			PackageInstaller.STATUS_FAILURE_ABORTED -> Timber.i("Update installation cancelled")

			else -> {
				Timber.e("Update installation failed: status=$status message=${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
				Toast.makeText(context, R.string.update_install_failed, Toast.LENGTH_LONG).show()
			}
		}
	}
}
