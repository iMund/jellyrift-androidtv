package org.jellyfin.androidtv.data.eventhandling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DisplayContentActionTests : FunSpec({
	test("launches the content when nothing is playing") {
		decideDisplayContentAction(playbackActive = false, interruptPlayback = false) shouldBe DisplayContentAction.LAUNCH
		decideDisplayContentAction(playbackActive = false, interruptPlayback = true) shouldBe DisplayContentAction.LAUNCH
	}

	test("keeps ignoring the command during playback unless the sender asks to interrupt") {
		decideDisplayContentAction(playbackActive = true, interruptPlayback = false) shouldBe DisplayContentAction.IGNORE
	}

	test("interrupts the playback only when explicitly requested") {
		decideDisplayContentAction(playbackActive = true, interruptPlayback = true) shouldBe DisplayContentAction.INTERRUPT_AND_LAUNCH
	}

	test("recognizes the interrupt argument regardless of case") {
		mapOf("InterruptPlayback" to "true").isInterruptPlaybackRequested() shouldBe true
		mapOf("interruptPlayback" to "True").isInterruptPlaybackRequested() shouldBe true
		mapOf("INTERRUPTPLAYBACK" to "TRUE").isInterruptPlaybackRequested() shouldBe true
	}

	test("does not interrupt without the argument or with another value") {
		emptyMap<String, String?>().isInterruptPlaybackRequested() shouldBe false
		mapOf("ItemId" to "abc").isInterruptPlaybackRequested() shouldBe false
		mapOf("InterruptPlayback" to "false").isInterruptPlaybackRequested() shouldBe false
		mapOf("InterruptPlayback" to "1").isInterruptPlaybackRequested() shouldBe false
		mapOf("InterruptPlayback" to null).isInterruptPlaybackRequested() shouldBe false
	}
})
