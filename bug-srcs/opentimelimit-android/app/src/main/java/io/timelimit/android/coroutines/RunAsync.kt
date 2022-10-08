/*
 * Open TimeLimit Copyright <C> 2019 Jonas Lochmann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.timelimit.android.coroutines

import io.timelimit.android.async.Threads
import kotlinx.coroutines.*

fun <T> runAsync(block: suspend CoroutineScope.() -> T) {
    GlobalScope.launch (Dispatchers.Main) {
        block()
    }.invokeOnCompletion {
        if (it != null && (!(it is CancellationException))) {
            Threads.mainThreadHandler.post {
                throw it
            }
        }
    }
}

fun <T> runAsyncExpectForever(block: suspend CoroutineScope.() -> T) {
    runAsync {
        block()

        throw IllegalStateException()
    }
}
