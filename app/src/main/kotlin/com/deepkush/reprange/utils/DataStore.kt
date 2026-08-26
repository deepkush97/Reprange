package com.deepkush.reprange.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PrefCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var prefs: Preferences? = null

    fun start(context: Context) {
        scope.launch {
            context.dataStore.data.collect { prefs = it }
        }
    }

    fun <T> get(key: Preferences.Key<T>): T? = prefs?.get(key)
}

operator fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? =
    PrefCache.get(key) ?: runBlocking(Dispatchers.IO) {
        withTimeoutOrNull(1500) { data.first()[key] }
    }

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): MutableState<T> {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val flow = remember {
        context.dataStore.data.map { it[key] ?: defaultValue }.distinctUntilChanged()
    }.collectAsState(context.dataStore[key] ?: defaultValue)
    return remember {
        object : MutableState<T> {
            override var value: T
                get() = flow.value
                set(v) {
                    scope.launch { context.dataStore.edit { it[key] = v } }
                }

            override fun component1(): T = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
inline fun <reified T : Enum<T>> rememberEnumPreference(
    key: Preferences.Key<String>,
    defaultValue: T,
): MutableState<T> {
    val state = rememberPreference(key, defaultValue.name)
    return remember {
        object : MutableState<T> {
            override var value: T
                get() =
                    runCatching { enumValueOf<T>(state.value) }.getOrDefault(defaultValue)
                set(v) {
                    state.value = v.name
                }

            override fun component1(): T = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

fun <T> preference(
    context: Context,
    key: Preferences.Key<T>,
    default: T,
): ReadOnlyProperty<Any?, T> = object : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        context.dataStore[key] ?: default
}
