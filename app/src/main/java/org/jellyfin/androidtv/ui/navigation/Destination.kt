package org.jellyfin.androidtv.ui.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.util.createBundle
import org.jellyfin.androidtv.util.hasSameContentAs
import kotlin.reflect.KClass

sealed interface Destination {
	data class Fragment(
		val fragment: KClass<out androidx.fragment.app.Fragment>,
		val arguments: Bundle = createBundle(),
	) : Destination
}

/**
 * Whether this and [other] lead to the same screen: the same fragment with equal arguments.
 * The data class equality is not enough because [Bundle] compares by identity.
 */
fun Destination.Fragment.hasSameTarget(other: Destination.Fragment) =
	fragment == other.fragment && arguments.hasSameContentAs(other.arguments)

inline fun <reified T : Fragment> fragmentDestination(
	noinline arguments: (Bundle.() -> Unit)? = null,
) = Destination.Fragment(
	fragment = T::class,
	arguments = createBundle(arguments),
)
