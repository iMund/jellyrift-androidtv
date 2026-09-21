package org.jellyfin.androidtv.update

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DownloadValidationTests : FunSpec({
	test("accepts a download with the published size and checksum") {
		isDownloadValid(100, 100, "abc", "ABC") shouldBe true
	}

	test("rejects a truncated download even without a published checksum") {
		isDownloadValid(90, 100, "abc", null) shouldBe false
	}

	test("rejects a different checksum") {
		isDownloadValid(100, 100, "abc", "def") shouldBe false
	}

	test("accepts any size when the release does not publish one") {
		isDownloadValid(90, 0, "abc", "abc") shouldBe true
		isDownloadValid(90, 0, "abc", null) shouldBe true
	}
})
