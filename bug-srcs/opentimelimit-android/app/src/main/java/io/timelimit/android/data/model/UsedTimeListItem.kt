/*
 * TimeLimit Copyright <C> 2019 - 2020 Jonas Lochmann
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

package io.timelimit.android.data.model

data class UsedTimeListItem(
        val categoryId: String,
        val categoryTitle: String,
        val startMinuteOfDay: Int,
        val endMinuteOfDay: Int,
        val duration: Long,
        // used time item
        val day: Long?,
        // session duration
        val lastUsage: Long?,
        val maxSessionDuration: Long?,
        val pauseDuration: Long?
)