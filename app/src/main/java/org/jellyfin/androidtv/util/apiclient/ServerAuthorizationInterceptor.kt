package org.jellyfin.androidtv.util.apiclient

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder

/**
 * Adds the Jellyfin `Authorization` header to requests made to the server the user is signed in to.
 *
 * ExoPlayer downloads media with its own HTTP stack, so URLs built for direct play (`/Videos/{id}/stream`) are
 * requested without any credentials. Servers, plugins and reverse proxies that require authentication for
 * streaming then answer 403. The token is only sent to the configured server (same scheme, host and port) and
 * never to other hosts, such as remote media paths or external subtitles.
 */
class ServerAuthorizationInterceptor(
	private val api: ApiClient,
) : Interceptor {
	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()

		val accessToken = api.accessToken
		if (accessToken.isNullOrBlank() || request.header(AUTHORIZATION_HEADER) != null || !isServerUrl(api.baseUrl, request.url)) {
			return chain.proceed(request)
		}

		val header = AuthorizationHeaderBuilder.buildHeader(
			clientName = api.clientInfo.name,
			clientVersion = api.clientInfo.version,
			deviceId = api.deviceInfo.id,
			deviceName = api.deviceInfo.name,
			accessToken = accessToken,
		)

		return chain.proceed(request.newBuilder().header(AUTHORIZATION_HEADER, header).build())
	}

	companion object {
		const val AUTHORIZATION_HEADER = "Authorization"

		/** Whether [url] points to the same origin (scheme, host and port) as the server at [baseUrl]. */
		fun isServerUrl(baseUrl: String?, url: HttpUrl): Boolean {
			val server = baseUrl?.toHttpUrlOrNull() ?: return false
			return server.scheme == url.scheme && server.host.equals(url.host, ignoreCase = true) && server.port == url.port
		}
	}
}
