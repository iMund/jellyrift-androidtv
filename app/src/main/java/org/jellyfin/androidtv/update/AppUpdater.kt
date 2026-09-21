package org.jellyfin.androidtv.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.security.MessageDigest

sealed interface UpdateCheck {
	data object UpToDate : UpdateCheck
	data class Available(val release: ReleaseInfo) : UpdateCheck
	data object Failed : UpdateCheck
}

/**
 * Looks for a newer release of the app in the GitHub repository [repository] ("owner/name", served by [apiUrl]) and downloads it. The
 * download is checked against the checksum of the release; installing it is up to [ApkInstaller], and Android itself
 * only accepts an APK signed with the same key as the installed app.
 */
class AppUpdater(
	private val context: Context,
	private val client: OkHttpClient,
	private val repository: String,
	private val installedVersionCode: Int,
	private val apiUrl: String = "https://api.github.com",
) {
	val enabled get() = repository.isNotBlank()

	suspend fun check(): UpdateCheck = withContext(Dispatchers.IO) {
		if (!enabled) return@withContext UpdateCheck.Failed

		val request = Request.Builder()
			.url("${apiUrl.trimEnd('/')}/repos/$repository/releases/latest")
			.header("Accept", "application/vnd.github+json")
			.build()

		try {
			client.newCall(request).execute().use { response ->
				when {
					// No release published yet
					response.code == 404 -> UpdateCheck.UpToDate
					!response.isSuccessful -> UpdateCheck.Failed
					else -> {
						val release = response.body?.string()?.let(::parseLatestRelease)
						if (release != null && release.isNewerThan(installedVersionCode)) UpdateCheck.Available(release)
						else UpdateCheck.UpToDate
					}
				}
			}
		} catch (err: IOException) {
			Timber.w(err, "Unable to check for updates")
			UpdateCheck.Failed
		}
	}

	/** Downloads the APK of [release]. Returns null when it fails or does not match the published checksum. */
	suspend fun download(release: ReleaseInfo, onProgress: (Float) -> Unit): File? = withContext(Dispatchers.IO) {
		val directory = File(context.cacheDir, "updates").apply { mkdirs() }
		val target = File(directory, "update.apk")
		target.delete()

		try {
			client.newCall(Request.Builder().url(release.apkUrl).build()).execute().use { response ->
				val body = response.body
				if (!response.isSuccessful || body == null) return@withContext null

				val total = body.contentLength().takeIf { it > 0 } ?: release.apkSizeBytes
				val digest = MessageDigest.getInstance("SHA-256")
				var received = 0L

				body.byteStream().use { input ->
					target.outputStream().use { output ->
						val buffer = ByteArray(64 * 1024)
						while (true) {
							currentCoroutineContext().ensureActive()
							val read = input.read(buffer)
							if (read < 0) break
							output.write(buffer, 0, read)
							digest.update(buffer, 0, read)
							received += read
							if (total > 0) onProgress((received.toFloat() / total).coerceAtMost(1f))
						}
					}
				}

				val actual = digest.digest().joinToString("") { "%02x".format(it) }
				if (!isDownloadValid(received, release.apkSizeBytes, actual, release.apkSha256)) {
					Timber.e("Update download rejected: size $received of ${release.apkSizeBytes}, checksum $actual, expected ${release.apkSha256}")
					target.delete()
					return@withContext null
				}
				if (release.apkSha256 == null) Timber.w("The release publishes no checksum, only its size was checked")
			}
			target
		} catch (err: IOException) {
			Timber.w(err, "Unable to download the update")
			target.delete()
			null
		}
	}
}

/**
 * Whether a finished download is the file the release published: it has the published size (when known) and, when the
 * release publishes a checksum, that checksum.
 */
fun isDownloadValid(receivedBytes: Long, expectedBytes: Long, actualSha256: String, expectedSha256: String?): Boolean {
	if (expectedBytes > 0 && receivedBytes != expectedBytes) return false
	return expectedSha256 == null || actualSha256.equals(expectedSha256, ignoreCase = true)
}
