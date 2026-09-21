package org.jellyfin.androidtv.update

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private fun release(
	tag: String = "v1.2.3",
	extra: String = "",
	assets: String = """[{"name":"app.apk","size":1234,"browser_download_url":"https://example.com/app.apk","digest":"sha256:ABCDEF"}]""",
) = """{"tag_name":"$tag","body":"notes",$extra"assets":$assets,"unknown":1}"""

class ReleaseInfoTests : FunSpec({
	test("computes the version code like the build") {
		versionCodeOf("0.0.0") shouldBe 99
		versionCodeOf("1.1.1") shouldBe 1010199
		versionCodeOf("v0.7.0") shouldBe 70099
		versionCodeOf("2.0.0-rc.3") shouldBe 2000003
		versionCodeOf("99.99.99") shouldBe 99999999
	}

	test("rejects tags that are not a version") {
		versionCodeOf("latest") shouldBe null
		versionCodeOf("1.2") shouldBe null
		versionCodeOf("1.2.x") shouldBe null
		versionCodeOf("100.0.0") shouldBe null
		versionCodeOf("1.0.0-rc.abc") shouldBe null
	}

	test("reads the release with its APK and checksum") {
		val info = parseLatestRelease(release())!!
		info.tag shouldBe "v1.2.3"
		info.versionCode shouldBe 1020399
		info.apkUrl shouldBe "https://example.com/app.apk"
		info.apkSizeBytes shouldBe 1234L
		info.apkSha256 shouldBe "abcdef"
		info.notes shouldBe "notes"
	}

	test("has no checksum when the release does not publish one") {
		val assets = """[{"name":"app.apk","size":1,"browser_download_url":"https://example.com/app.apk"}]"""
		parseLatestRelease(release(assets = assets))!!.apkSha256 shouldBe null
	}

	test("ignores drafts and pre-releases") {
		parseLatestRelease(release(extra = """"draft":true,""")) shouldBe null
		parseLatestRelease(release(extra = """"prerelease":true,""")) shouldBe null
	}

	test("ignores a release without an APK or with an unknown tag") {
		parseLatestRelease(release(assets = """[{"name":"notes.txt","browser_download_url":"https://example.com/n"}]""")) shouldBe null
		parseLatestRelease(release(tag = "nightly")) shouldBe null
	}

	test("ignores an answer that is not a release") {
		parseLatestRelease("""{"message":"Not Found"}""") shouldBe null
		parseLatestRelease("not json") shouldBe null
	}

	test("compares against the installed version code") {
		val info = parseLatestRelease(release())!!
		info.isNewerThan(1020398) shouldBe true
		info.isNewerThan(1020399) shouldBe false
		info.isNewerThan(2000000) shouldBe false
	}
})
