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
package io.timelimit.android.livedata

import androidx.lifecycle.LiveData

fun LiveData<Boolean>.or(other: LiveData<Boolean>): LiveData<Boolean> {
    return this.switchMap { value1 ->
        other.map { value2 ->
            value1 || value2
        }
    }
}

fun LiveData<Boolean>.and(other: LiveData<Boolean>): LiveData<Boolean> {
    return this.switchMap { value1 ->
        other.map { value2 ->
            value1 && value2
        }
    }
}

fun LiveData<Boolean>.invert(): LiveData<Boolean> = this.map { !it }
