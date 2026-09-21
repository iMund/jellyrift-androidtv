package org.jellyfin.androidtv.data.eventhandling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import java.util.Collections

class DisplayMessageToastTests : FunSpec({
	test("puts the header on its own line above the text") {
		buildDisplayMessageText("Title", "Body") shouldBe "Title\nBody"
	}

	test("shows only the text when there is no header") {
		buildDisplayMessageText(null, "Body") shouldBe "Body"
		buildDisplayMessageText("  ", "Body") shouldBe "Body"
	}

	test("leaves no trailing line when there is no text") {
		buildDisplayMessageText("Title", null) shouldBe "Title"
		buildDisplayMessageText("Title", "") shouldBe "Title"
	}

	test("is empty when there is nothing to show") {
		buildDisplayMessageText(null, null) shouldBe ""
		buildDisplayMessageText(" ", "") shouldBe ""
	}

	test("reads the timeout regardless of the argument name case") {
		mapOf("TimeoutMs" to "5000").displayMessageTimeoutMs() shouldBe 5000L
		mapOf("timeoutms" to "5000").displayMessageTimeoutMs() shouldBe 5000L
	}

	test("has no timeout when it is missing or invalid") {
		emptyMap<String, String>().displayMessageTimeoutMs() shouldBe null
		mapOf("TimeoutMs" to "abc").displayMessageTimeoutMs() shouldBe null
	}

	test("limits the timeout to a sane range") {
		mapOf("TimeoutMs" to "10").displayMessageTimeoutMs() shouldBe 1_000L
		mapOf("TimeoutMs" to "999999").displayMessageTimeoutMs() shouldBe 30_000L
	}

	test("covers the timeout with toasts that last at most one toast duration") {
		toastSlices(2_000) shouldBe listOf(2_000L)
		toastSlices(5_000) shouldBe listOf(3_500L, 1_500L)
		toastSlices(7_000) shouldBe listOf(3_500L, 3_500L)
	}

	context("DisplayMessageToaster") {
		// Real time with short slices: the coroutines test library is not a dependency of the app
		val sliceMs = 100L
		val events = Collections.synchronizedList(mutableListOf<String>())
		val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
		fun toaster() = DisplayMessageToaster(scope, sliceMs) { text ->
			events += "show:$text"
			ShownToast { events += "cancel:$text" }
		}

		beforeEach { events.clear() }
		afterSpec { scope.cancel() }

		test("shows a message without a timeout for one slice and then takes it down") {
			toaster().display("A", null)
			delay(sliceMs * 3)
			events.toList() shouldBe listOf("show:A", "cancel:A")
		}

		test("shows a long message again until the timeout is covered") {
			toaster().display("A", sliceMs * 2)
			delay(sliceMs * 4)
			events.toList() shouldBe listOf("show:A", "cancel:A", "show:A", "cancel:A")
		}

		test("a new message replaces one that had no timeout") {
			val toaster = toaster()
			toaster.display("A", null)
			delay(sliceMs / 4)
			toaster.display("B", sliceMs)
			delay(sliceMs * 3)
			events.toList() shouldBe listOf("show:A", "cancel:A", "show:B", "cancel:B")
		}

		test("a new message replaces one that is still repeating") {
			val toaster = toaster()
			toaster.display("A", sliceMs * 5)
			delay(sliceMs * 3 / 2)
			toaster.display("B", sliceMs)
			delay(sliceMs * 3)
			events.toList() shouldBe listOf("show:A", "cancel:A", "show:A", "cancel:A", "show:B", "cancel:B")
		}

		test("ignores a message with nothing to show") {
			toaster().display("  ", 1000)
			delay(sliceMs)
			events.toList() shouldBe emptyList()
		}
	}
})
