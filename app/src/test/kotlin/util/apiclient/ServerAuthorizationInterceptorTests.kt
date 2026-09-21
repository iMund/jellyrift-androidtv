package org.jellyfin.androidtv.util.apiclient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo

class ServerAuthorizationInterceptorTests : FunSpec({
	fun apiClient(baseUrl: String? = "https://jellyfin.example.com", accessToken: String? = "secret-token") = mockk<ApiClient> {
		every { this@mockk.baseUrl } returns baseUrl
		every { this@mockk.accessToken } returns accessToken
		every { clientInfo } returns ClientInfo("Jellyfin for Android TV", "1.2.3")
		every { deviceInfo } returns DeviceInfo("device-id", "Living room")
	}

	/** Runs [url] through the interceptor and returns the request that would have reached the network. */
	fun send(api: ApiClient, url: String, headers: Map<String, String> = emptyMap()): Request {
		lateinit var sent: Request
		val client = OkHttpClient.Builder()
			.addInterceptor(ServerAuthorizationInterceptor(api))
			.addInterceptor(Interceptor { chain ->
				sent = chain.request()
				Response.Builder().request(sent).protocol(Protocol.HTTP_1_1).code(200).message("OK").build()
			})
			.build()

		val request = Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }.build()
		client.newCall(request).execute().close()
		return sent
	}

	test("adds the authorization header for requests to the server") {
		val request = send(apiClient(), "https://jellyfin.example.com/Videos/123/stream?static=true")

		request.header("Authorization") shouldBe
			"""MediaBrowser Client="Jellyfin+for+Android+TV", Version="1.2.3", DeviceId="device-id", Device="Living+room", Token="secret-token""""
	}

	test("does not send the token to other hosts") {
		send(apiClient(), "https://cdn.other.com/video.mp4").header("Authorization") shouldBe null
		send(apiClient(), "https://jellyfin.example.com.evil.com/Videos/1/stream").header("Authorization") shouldBe null
	}

	test("does not send the token over a different scheme or port") {
		send(apiClient(), "http://jellyfin.example.com/Videos/1/stream").header("Authorization") shouldBe null
		send(apiClient(), "https://jellyfin.example.com:8920/Videos/1/stream").header("Authorization") shouldBe null
	}

	test("supports servers with a custom port and base path") {
		val api = apiClient(baseUrl = "http://192.168.1.10:8096/jellyfin")

		(send(api, "http://192.168.1.10:8096/jellyfin/Videos/1/stream").header("Authorization") != null) shouldBe true
		send(api, "http://192.168.1.10:8097/jellyfin/Videos/1/stream").header("Authorization") shouldBe null
	}

	test("compares the host case-insensitively") {
		(send(apiClient(), "https://JELLYFIN.example.com/Videos/1/stream").header("Authorization") != null) shouldBe true
	}

	test("keeps an authorization header that is already set") {
		val request = send(apiClient(), "https://jellyfin.example.com/Videos/1/stream", mapOf("Authorization" to "Bearer custom"))

		request.header("Authorization") shouldBe "Bearer custom"
	}

	test("does nothing without an access token") {
		send(apiClient(accessToken = null), "https://jellyfin.example.com/Videos/1/stream").header("Authorization") shouldBe null
		send(apiClient(accessToken = ""), "https://jellyfin.example.com/Videos/1/stream").header("Authorization") shouldBe null
	}

	test("does nothing without a server address") {
		send(apiClient(baseUrl = null), "https://jellyfin.example.com/Videos/1/stream").header("Authorization") shouldBe null
	}

	test("isServerUrl() rejects unparsable server addresses") {
		ServerAuthorizationInterceptor.isServerUrl("not a url", "https://jellyfin.example.com/".toHttpUrl()) shouldBe false
	}
})
