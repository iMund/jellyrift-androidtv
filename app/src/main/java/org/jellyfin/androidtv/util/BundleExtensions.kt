package org.jellyfin.androidtv.util

import android.os.Bundle

@Suppress("DEPRECATION")
inline fun <reified T> Bundle.getValue(key: String): T? = when {
	AndroidVersion.isAtLeastT -> getParcelable(key, T::class.java)
	else -> get(key) as T?
}

fun createBundle(init: (Bundle.() -> Unit)? = null) = Bundle().also { bundle ->
	if (init != null) bundle.init()
}

/** Whether both bundles have the same keys and equal values. [Bundle] does not implement `equals` itself. */
@Suppress("DEPRECATION")
fun Bundle.hasSameContentAs(other: Bundle): Boolean {
	val keys = keySet()
	return keys == other.keySet() && keys.all { key -> get(key) == other.get(key) }
}
