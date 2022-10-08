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
package io.timelimit.android.ui.user.create

import android.content.Context
import io.timelimit.android.R
import io.timelimit.android.data.IdGenerator
import io.timelimit.android.data.model.TimeLimitRule
import io.timelimit.android.extensions.MinuteOfDay
import java.util.*

class DefaultCategories private constructor(private val context: Context) {
    companion object {
        private var instance: DefaultCategories? = null

        fun with(context: Context): DefaultCategories {
            if (instance == null) {
                instance = DefaultCategories(context.applicationContext)
            }

            return instance!!
        }
    }

    val allowedAppsTitle get() = context.getString(R.string.setup_category_allowed)
    val allowedGamesTitle get() = context.getString(R.string.setup_category_games)

    fun generateGamesTimeLimitRules(categoryId: String): List<TimeLimitRule> {
        val rules = mutableListOf<TimeLimitRule>()

        // maximum time for each workday
        for (day in 0..4) {
            rules.add(
                    TimeLimitRule(
                            id = IdGenerator.generateId(),
                            categoryId = categoryId,
                            applyToExtraTimeUsage = false,
                            dayMask = (1 shl day).toByte(),
                            maximumTimeInMillis = 1000 * 60 * 30,    // 30 minutes
                            startMinuteOfDay = TimeLimitRule.MIN_START_MINUTE,
                            endMinuteOfDay = TimeLimitRule.MAX_END_MINUTE,
                            sessionPauseMilliseconds = 0,
                            sessionDurationMilliseconds = 0
                    )
            )
        }

        // maximum time for each weekend day
        for (day in 5..6) {
            rules.add(
                    TimeLimitRule(
                            id = IdGenerator.generateId(),
                            categoryId = categoryId,
                            applyToExtraTimeUsage = false,
                            dayMask = (1 shl day).toByte(),
                            maximumTimeInMillis = 1000 * 60 * 60 * 3,    // 3 hours
                            startMinuteOfDay = TimeLimitRule.MIN_START_MINUTE,
                            endMinuteOfDay = TimeLimitRule.MAX_END_MINUTE,
                            sessionPauseMilliseconds = 0,
                            sessionDurationMilliseconds = 0
                    )
            )
        }

        // maximum time per total week
        val dayMask = BitSet()

        dayMask.set(0, 7)

        rules.add(
                TimeLimitRule(
                        id = IdGenerator.generateId(),
                        categoryId = categoryId,
                        applyToExtraTimeUsage = false,
                        dayMask = 1 + 2 + 4 + 8 + 16 + 32 + 64,
                        maximumTimeInMillis = 1000 * 60 * 60 * 6,    // 6 hours
                        startMinuteOfDay = TimeLimitRule.MIN_START_MINUTE,
                        endMinuteOfDay = TimeLimitRule.MAX_END_MINUTE,
                        sessionPauseMilliseconds = 0,
                        sessionDurationMilliseconds = 0
                )
        )

        // blocked time areas for weekdays
        rules.add(
                TimeLimitRule(
                        id = IdGenerator.generateId(),
                        categoryId = categoryId,
                        applyToExtraTimeUsage = true,
                        dayMask = 1 + 2 + 4 + 8 + 16,
                        maximumTimeInMillis = 0,
                        startMinuteOfDay = 0,
                        endMinuteOfDay = 6 * 60 - 1,
                        sessionPauseMilliseconds = 0,
                        sessionDurationMilliseconds = 0
                )
        )

        rules.add(
                TimeLimitRule(
                        id = IdGenerator.generateId(),
                        categoryId = categoryId,
                        applyToExtraTimeUsage = true,
                        dayMask = 1 + 2 + 4 + 8 + 16,
                        maximumTimeInMillis = 0,
                        startMinuteOfDay = 18 * 60,
                        endMinuteOfDay = MinuteOfDay.MAX,
                        sessionPauseMilliseconds = 0,
                        sessionDurationMilliseconds = 0
                )
        )

        // blocked time areas for the weekend
        rules.add(
                TimeLimitRule(
                        id = IdGenerator.generateId(),
                        categoryId = categoryId,
                        applyToExtraTimeUsage = true,
                        dayMask = 32 + 64,
                        maximumTimeInMillis = 0,
                        startMinuteOfDay = 0,
                        endMinuteOfDay = 9 * 60 - 1,
                        sessionPauseMilliseconds = 0,
                        sessionDurationMilliseconds = 0
                )
        )

        rules.add(
                TimeLimitRule(
                        id = IdGenerator.generateId(),
                        categoryId = categoryId,
                        applyToExtraTimeUsage = true,
                        dayMask = 32 + 64,
                        maximumTimeInMillis = 0,
                        startMinuteOfDay = 20 * 60,
                        endMinuteOfDay = MinuteOfDay.MAX,
                        sessionPauseMilliseconds = 0,
                        sessionDurationMilliseconds = 0
                )
        )

        return rules.toList()
    }
}
