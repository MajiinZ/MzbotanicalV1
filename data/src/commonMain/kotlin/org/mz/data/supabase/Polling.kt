package org.mz.data.supabase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mz.killrs.shared.util.RequestState

/** Writes call [poke] so open screens refresh immediately instead of waiting for the next poll. */
internal object DataRefresh {
    val events = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    fun poke() { events.tryEmit(Unit) }
}

/**
 * Re-reads [load] every [intervalMs] and after any local write, emitting only when the
 * result changes. Keeps the "live" feel the Firestore snapshot listeners used to give.
 */
internal fun <T> pollingFlow(
    intervalMs: Long = 5_000,
    errorPrefix: String,
    load: suspend () -> RequestState<T>,
): Flow<RequestState<T>> = channelFlow {
    val lock = Mutex()
    suspend fun refresh() = lock.withLock {
        val state = try { load() } catch (e: CancellationException) { throw e } catch (e: Throwable) {
            RequestState.Error("$errorPrefix: ${e.readableMessage("network error")}")
        }
        send(state)
    }
    launch { DataRefresh.events.collect { refresh() } }
    while (true) {
        refresh()
        delay(intervalMs)
    }
}.distinctUntilChanged()
