package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldStartWith

class ClientNameTests : FunSpec({
	test("the client name is JellyRift, not the name of the official app") {
		appClientName shouldStartWith "JellyRift"
	}
})
