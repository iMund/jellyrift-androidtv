package org.jellyfin.androidtv.ui.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class NavigationRepositoryTests : FunSpec({
	/** A bundle with the given content: [Bundle] cannot be used for real in JVM unit tests. */
	@Suppress("DEPRECATION")
	fun bundleOf(vararg content: Pair<String, Any?>) = mockk<Bundle> {
		every { this@mockk.keySet() } returns content.map { it.first }.toSet()
		content.forEach { (key, value) -> every { this@mockk.get(key) } returns value }
	}

	fun destination(vararg content: Pair<String, Any?>) = Destination.Fragment(Fragment::class, bundleOf(*content))

	context("currentDestination") {
		val home = destination("name" to "home")
		val details = destination("ItemId" to "1")

		test("is null while the back stack is empty") {
			NavigationRepositoryImpl(home).currentDestination shouldBe null
		}

		test("is the last destination navigated to") {
			val repository = NavigationRepositoryImpl(home)

			repository.navigate(details)

			repository.currentDestination shouldBe details
		}

		test("is the replacement after a replace") {
			val repository = NavigationRepositoryImpl(home)
			val other = destination("ItemId" to "2")
			repository.navigate(details)

			repository.navigate(other, replace = true)

			repository.currentDestination shouldBe other
		}

		test("goes back to the previous destination and to null when the stack is empty") {
			val repository = NavigationRepositoryImpl(home)
			val other = destination("ItemId" to "2")
			repository.navigate(details)
			repository.navigate(other)

			repository.goBack()
			repository.currentDestination shouldBe details

			repository.goBack()
			repository.currentDestination shouldBe null
		}
	}

	context("hasSameTarget") {
		test("is true for the same fragment with equal arguments in different bundles") {
			destination("ItemId" to "1").hasSameTarget(destination("ItemId" to "1")) shouldBe true
		}

		test("is false for different argument values") {
			destination("ItemId" to "1").hasSameTarget(destination("ItemId" to "2")) shouldBe false
		}

		test("is false for different argument keys") {
			destination("ItemId" to "1").hasSameTarget(destination("Other" to "1")) shouldBe false
			destination("ItemId" to "1").hasSameTarget(destination("ItemId" to "1", "Extra" to "x")) shouldBe false
		}

		test("is false for a different fragment") {
			val bundle = bundleOf("ItemId" to "1")
			Destination.Fragment(Fragment::class, bundle)
				.hasSameTarget(Destination.Fragment(androidx.fragment.app.DialogFragment::class, bundle)) shouldBe false
		}

		test("is true for destinations without arguments") {
			destination().hasSameTarget(destination()) shouldBe true
		}
	}
})
