package org.jellyfin.androidtv.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A published release of the app that can be installed over the running one. */
data class ReleaseInfo(
	val tag: String,
	val versionCode: Int,
	val notes: String,
	val apkUrl: String,
	val apkSizeBytes: Long,
	/** Lowercase hex SHA-256 of the APK when the release publishes it. */
	val apkSha256: String?,
)

@Serializable
private data class GitHubRelease(
	@SerialName("tag_name") val tagName: String,
	val draft: Boolean = false,
	val prerelease: Boolean = false,
	val body: String? = null,
	val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
private data class GitHubAsset(
	val name: String,
	val size: Long = 0,
	val digest: String? = null,
	@SerialName("browser_download_url") val downloadUrl: String,
)

private val json = Json { ignoreUnknownKeys = true }

/**
 * Reads the answer of GitHub's "latest release" endpoint. Returns null when the release is a draft or pre-release,
 * has no APK, or its tag is not a version this app understands.
 */
fun parseLatestRelease(body: String): ReleaseInfo? {
	val release = try {
		json.decodeFromString<GitHubRelease>(body)
	} catch (_: Exception) {
		return null
	}
	if (release.draft || release.prerelease) return null

	val versionCode = versionCodeOf(release.tagName) ?: return null
	val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) } ?: return null

	return ReleaseInfo(
		tag = release.tagName,
		versionCode = versionCode,
		notes = release.body.orEmpty(),
		apkUrl = apk.downloadUrl,
		apkSizeBytes = apk.size,
		apkSha256 = apk.digest?.removePrefix("sha256:")?.takeIf { it != apk.digest }?.lowercase(),
	)
}

/**
 * Version code of a release tag, computed the way the build does it (`MA.MI.PA` or `MA.MI.PA-pre.N`, optional "v"
 * prefix), so a tag can be compared with the installed app. Null when the tag has no valid version.
 */
fun versionCodeOf(tag: String): Int? {
	val name = tag.removePrefix("v")
	val core = name.substringBefore('-').split('.')
	if (core.size < 3) return null
	val (major, minor, patch) = core.take(3).map { it.toIntOrNull()?.takeIf { n -> n in 0..99 } ?: return null }

	val preRelease = if ('-' in name) {
		name.substringAfter('-').substringAfter('.', "").toIntOrNull()?.takeIf { it in 0..99 } ?: return null
	} else {
		99
	}

	return major * 1_000_000 + minor * 10_000 + patch * 100 + preRelease
}

fun ReleaseInfo.isNewerThan(installedVersionCode: Int) = versionCode > installedVersionCode
