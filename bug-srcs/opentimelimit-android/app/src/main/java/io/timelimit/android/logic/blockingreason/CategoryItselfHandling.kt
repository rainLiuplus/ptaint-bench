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
package io.timelimit.android.logic.blockingreason

import io.timelimit.android.data.model.CategoryNetworkId
import io.timelimit.android.data.model.derived.CategoryRelatedData
import io.timelimit.android.data.model.derived.UserRelatedData
import io.timelimit.android.date.CalendarCache
import io.timelimit.android.date.DateInTimezone
import io.timelimit.android.date.getMinuteOfWeek
import io.timelimit.android.extensions.MinuteOfDay
import io.timelimit.android.integration.platform.BatteryStatus
import io.timelimit.android.logic.BlockingReason
import io.timelimit.android.logic.RemainingSessionDuration
import io.timelimit.android.logic.RemainingTime
import io.timelimit.android.sync.actions.AddUsedTimeActionItemAdditionalCountingSlot
import io.timelimit.android.sync.actions.AddUsedTimeActionItemSessionDurationLimitSlot
import org.threeten.bp.ZoneId
import java.util.*

data class CategoryItselfHandling (
        val shouldCountTime: Boolean,
        val shouldCountExtraTime: Boolean,
        val maxTimeToAdd: Long,
        val sessionDurationSlotsToCount: Set<AddUsedTimeActionItemSessionDurationLimitSlot>,
        val additionalTimeCountingSlots: Set<AddUsedTimeActionItemAdditionalCountingSlot>,
        val areLimitsTemporarilyDisabled: Boolean,
        val okByBattery: Boolean,
        val okByTempBlocking: Boolean,
        val okByNetworkId: Boolean,
        val okByBlockedTimeAreas: Boolean,
        val okByTimeLimitRules: Boolean,
        val okBySessionDurationLimits: Boolean,
        val blockAllNotifications: Boolean,
        val remainingTime: RemainingTime?,
        val remainingSessionDuration: Long?,
        val dependsOnMinTime: Long,
        val dependsOnMaxTime: Long,
        val dependsOnBatteryCharging: Boolean,
        val dependsOnMinBatteryLevel: Int,
        val dependsOnMaxBatteryLevel: Int,
        val dependsOnNetworkId: Boolean,
        val createdWithCategoryRelatedData: CategoryRelatedData,
        val createdWithUserRelatedData: UserRelatedData,
        val createdWithBatteryStatus: BatteryStatus,
        val createdWithNetworkId: String?
) {
    companion object {
        fun calculate(
                categoryRelatedData: CategoryRelatedData,
                user: UserRelatedData,
                batteryStatus: BatteryStatus,
                timeInMillis: Long,
                currentNetworkId: String?
        ): CategoryItselfHandling {
            val dependsOnMinTime = timeInMillis
            val dateInTimezone = DateInTimezone.newInstance(timeInMillis, user.timeZone)
            val minuteInWeek = getMinuteOfWeek(timeInMillis, user.timeZone)
            val dayOfWeek = dateInTimezone.dayOfWeek
            val dayOfEpoch = dateInTimezone.dayOfEpoch
            val firstDayOfWeekAsEpochDay = dayOfEpoch - dayOfWeek
            val localDate = dateInTimezone.localDate

            val minRequiredBatteryLevel = if (batteryStatus.charging) categoryRelatedData.category.minBatteryLevelWhileCharging else categoryRelatedData.category.minBatteryLevelMobile
            val okByBattery = batteryStatus.level >= minRequiredBatteryLevel
            val dependsOnBatteryCharging = categoryRelatedData.category.minBatteryLevelWhileCharging != categoryRelatedData.category.minBatteryLevelMobile
            val dependsOnMinBatteryLevel = if (okByBattery) minRequiredBatteryLevel else Int.MIN_VALUE
            val dependsOnMaxBatteryLevel = if (okByBattery) Int.MAX_VALUE else minRequiredBatteryLevel - 1

            val okByTempBlocking = !categoryRelatedData.category.temporarilyBlocked || (
                    categoryRelatedData.category.temporarilyBlockedEndTime != 0L && categoryRelatedData.category.temporarilyBlockedEndTime < timeInMillis )
            val dependsOnMaxTimeByTempBlocking = if (okByTempBlocking || categoryRelatedData.category.temporarilyBlockedEndTime == 0L) Long.MAX_VALUE else categoryRelatedData.category.temporarilyBlockedEndTime

            val disableLimitsUntil = user.user.disableLimitsUntil.coerceAtLeast(categoryRelatedData.category.disableLimitsUntil)
            val areLimitsTemporarilyDisabled = timeInMillis < disableLimitsUntil
            val dependsOnMaxTimeByTemporarilyDisabledLimits = if (areLimitsTemporarilyDisabled) disableLimitsUntil else Long.MAX_VALUE

            val dependsOnNetworkId = categoryRelatedData.networks.isNotEmpty()
            val okByNetworkId = if (categoryRelatedData.networks.isEmpty() || areLimitsTemporarilyDisabled)
                true
            else if (currentNetworkId == null)
                false
            else
                categoryRelatedData.networks.find { CategoryNetworkId.anonymizeNetworkId(itemId = it.networkItemId, networkId = currentNetworkId) == it.hashedNetworkId } != null

            val allRelatedRules = if (areLimitsTemporarilyDisabled)
                emptyList()
            else
                RemainingTime.getRulesRelatedToDay(
                        dayOfWeek = dayOfWeek,
                        minuteOfDay = minuteInWeek % MinuteOfDay.LENGTH,
                        rules = categoryRelatedData.rules
                )

            val regularRelatedRules = allRelatedRules.filter { it.maximumTimeInMillis != 0 }
            val hasBlockedTimeAreaRelatedRule = allRelatedRules.find { it.maximumTimeInMillis == 0 } != null

            val okByBlockedTimeAreas = areLimitsTemporarilyDisabled || (
                    (!categoryRelatedData.category.blockedMinutesInWeek.read(minuteInWeek)) && (!hasBlockedTimeAreaRelatedRule))
            val dependsOnMaxMinuteOfWeekByBlockedTimeAreas = categoryRelatedData.category.blockedMinutesInWeek.let { blockedTimeAreas ->
                if (blockedTimeAreas.dataNotToModify[minuteInWeek]) {
                    blockedTimeAreas.dataNotToModify.nextClearBit(minuteInWeek)
                } else {
                    val next = blockedTimeAreas.dataNotToModify.nextSetBit(minuteInWeek)

                    if (next == -1) Int.MAX_VALUE else next
                }
            }
            val dependsOnMaxMinuteOfDayByBlockedTimeAreas = if (dependsOnMaxMinuteOfWeekByBlockedTimeAreas / MinuteOfDay.LENGTH != minuteInWeek / MinuteOfDay.LENGTH)
                Int.MAX_VALUE
            else
                dependsOnMaxMinuteOfWeekByBlockedTimeAreas % MinuteOfDay.LENGTH

            val remainingTime = RemainingTime.getRemainingTime(
                    usedTimes = categoryRelatedData.usedTimes,
                    // dependsOnMaxTimeByRules always depends on the day so that this is invalidated correctly
                    extraTime = categoryRelatedData.category.getExtraTime(dayOfEpoch = dayOfEpoch),
                    rules = regularRelatedRules,
                    dayOfWeek = dayOfWeek,
                    minuteOfDay = minuteInWeek % MinuteOfDay.LENGTH,
                    firstDayOfWeekAsEpochDay = firstDayOfWeekAsEpochDay
            )

            val remainingSessionDuration = RemainingSessionDuration.getRemainingSessionDuration(
                    rules = regularRelatedRules,
                    minuteOfDay = minuteInWeek % MinuteOfDay.LENGTH,
                    dayOfWeek = dayOfWeek,
                    timestamp = timeInMillis,
                    durationsOfCategory = categoryRelatedData.durations
            )

            val okByTimeLimitRules = regularRelatedRules.isEmpty() || (remainingTime != null && remainingTime.hasRemainingTime)
            val dependsOnMaxTimeByMinuteOfDay = (allRelatedRules.minBy { it.endMinuteOfDay }?.endMinuteOfDay ?: Int.MAX_VALUE).coerceAtMost(
                    categoryRelatedData.rules
                            .filter {
                                // related to today
                                it.dayMask.toInt() and (1 shl dayOfWeek) != 0 &&
                                        // will be applied later at this day
                                        it.startMinuteOfDay > minuteInWeek % MinuteOfDay.LENGTH
                            }
                            .minBy { it.startMinuteOfDay }?.startMinuteOfDay ?: Int.MAX_VALUE
            ).coerceAtMost(dependsOnMaxMinuteOfDayByBlockedTimeAreas)
            val dependsOnMaxTimeByRules = if (dependsOnMaxTimeByMinuteOfDay <= MinuteOfDay.MAX) {
                val calendar = CalendarCache.getCalendar()

                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.timeZone = user.timeZone

                calendar.timeInMillis = timeInMillis
                calendar[Calendar.HOUR_OF_DAY] = dependsOnMaxTimeByMinuteOfDay / 60
                calendar[Calendar.MINUTE] = dependsOnMaxTimeByMinuteOfDay % 60
                calendar[Calendar.SECOND] = 0
                calendar[Calendar.MILLISECOND] = 0

                calendar.timeInMillis
            } else {
                // always depend on the current day
                localDate.plusDays(1).atStartOfDay(ZoneId.of(user.user.timeZone)).toEpochSecond() * 1000
            }
            val dependsOnMaxTimeBySessionDurationLimitItems = (
                    categoryRelatedData.durations.map { it.lastUsage + it.sessionPauseDuration } +
                            categoryRelatedData.durations.map { it.lastUsage + it.maxSessionDuration - it.lastSessionDuration }
                    )
                    .filter { it > timeInMillis }
                    .min() ?: Long.MAX_VALUE

            val okBySessionDurationLimits = remainingSessionDuration == null || remainingSessionDuration > 0

            val dependsOnMaxTime = dependsOnMaxTimeByTempBlocking
                    .coerceAtMost(dependsOnMaxTimeByTemporarilyDisabledLimits)
                    .coerceAtMost(dependsOnMaxTimeByRules)
                    .coerceAtMost(dependsOnMaxTimeBySessionDurationLimitItems)
                    .coerceAtLeast(timeInMillis + 100)  // prevent loops in case of calculation bugs

            val shouldCountTime = regularRelatedRules.isNotEmpty()
            val shouldCountExtraTime = remainingTime?.usingExtraTime == true
            val sessionDurationSlotsToCount = if (remainingSessionDuration != null && remainingSessionDuration <= 0)
                emptySet()
            else
                regularRelatedRules.filter { it.sessionDurationLimitEnabled }.map {
                    AddUsedTimeActionItemSessionDurationLimitSlot(
                            startMinuteOfDay = it.startMinuteOfDay,
                            endMinuteOfDay = it.endMinuteOfDay,
                            maxSessionDuration = it.sessionDurationMilliseconds,
                            sessionPauseDuration = it.sessionPauseMilliseconds
                    )
                }.toSet()

            val maxTimeToAddByRegularTime = if (!shouldCountTime || remainingTime == null)
                Long.MAX_VALUE
            else if (shouldCountExtraTime)
                remainingTime.includingExtraTime
            else
                remainingTime.default
            val maxTimeToAddBySessionDuration = remainingSessionDuration ?: Long.MAX_VALUE
            val maxTimeToAdd = maxTimeToAddByRegularTime.coerceAtMost(maxTimeToAddBySessionDuration)

            val additionalTimeCountingSlots = if (shouldCountTime)
                regularRelatedRules
                        .filterNot { it.appliesToWholeDay }
                        .map { AddUsedTimeActionItemAdditionalCountingSlot(it.startMinuteOfDay, it.endMinuteOfDay) }
                        .toSet()
            else
                emptySet()

            val blockAllNotifications = categoryRelatedData.category.blockAllNotifications

            return CategoryItselfHandling(
                    shouldCountTime = shouldCountTime,
                    shouldCountExtraTime = shouldCountExtraTime,
                    maxTimeToAdd = maxTimeToAdd,
                    sessionDurationSlotsToCount = sessionDurationSlotsToCount,
                    areLimitsTemporarilyDisabled = areLimitsTemporarilyDisabled,
                    okByBattery = okByBattery,
                    okByTempBlocking = okByTempBlocking,
                    okByNetworkId = okByNetworkId,
                    okByBlockedTimeAreas = okByBlockedTimeAreas,
                    okByTimeLimitRules = okByTimeLimitRules,
                    okBySessionDurationLimits = okBySessionDurationLimits,
                    blockAllNotifications = blockAllNotifications,
                    remainingTime = remainingTime,
                    remainingSessionDuration = remainingSessionDuration,
                    additionalTimeCountingSlots = additionalTimeCountingSlots,
                    dependsOnMinTime = dependsOnMinTime,
                    dependsOnMaxTime = dependsOnMaxTime,
                    dependsOnBatteryCharging = dependsOnBatteryCharging,
                    dependsOnMinBatteryLevel = dependsOnMinBatteryLevel,
                    dependsOnMaxBatteryLevel = dependsOnMaxBatteryLevel,
                    dependsOnNetworkId = dependsOnNetworkId,
                    createdWithCategoryRelatedData = categoryRelatedData,
                    createdWithBatteryStatus = batteryStatus,
                    createdWithUserRelatedData = user,
                    createdWithNetworkId = currentNetworkId
            )
        }
    }

    val okBasic = okByBattery && okByTempBlocking && okByBlockedTimeAreas && okByTimeLimitRules && okBySessionDurationLimits// in the regular TimeLimit: && !missingNetworkTime
    val okAll = okBasic && okByNetworkId
    val shouldBlockActivities = !okAll
    val activityBlockingReason: BlockingReason = if (!okByBattery)
        BlockingReason.BatteryLimit
    else if (!okByTempBlocking)
        BlockingReason.TemporarilyBlocked
    else if (!okByNetworkId)
        BlockingReason.MissingRequiredNetwork
    else if (!okByBlockedTimeAreas)
        BlockingReason.BlockedAtThisTime
    else if (!okByTimeLimitRules)
        if (remainingTime?.hasRemainingTime == true)
            BlockingReason.TimeOverExtraTimeCanBeUsedLater
        else
            BlockingReason.TimeOver
    else if (!okBySessionDurationLimits)
        BlockingReason.SessionDurationLimit
    else
        BlockingReason.None

    // blockAllNotifications is only relevant if premium or local mode
    // val shouldBlockNotifications = !okAll || blockAllNotifications
    val shouldBlockAtSystemLevel = !okBasic
    val systemLevelBlockingReason: BlockingReason = if (!okByBattery)
        BlockingReason.BatteryLimit
    else if (!okByTempBlocking)
        BlockingReason.TemporarilyBlocked
    else if (!okByBlockedTimeAreas)
        BlockingReason.BlockedAtThisTime
    else if (!okByTimeLimitRules)
        if (remainingTime?.hasRemainingTime == true)
            BlockingReason.TimeOverExtraTimeCanBeUsedLater
        else
            BlockingReason.TimeOver
    else if (!okBySessionDurationLimits)
        BlockingReason.SessionDurationLimit
    else
        BlockingReason.None

    fun isValid(
            categoryRelatedData: CategoryRelatedData,
            user: UserRelatedData,
            batteryStatus: BatteryStatus,
            timeInMillis: Long,
            currentNetworkId: String?
    ): Boolean {
        if (
                categoryRelatedData != createdWithCategoryRelatedData || user != createdWithUserRelatedData
        ) {
            return false
        }

        if (timeInMillis < dependsOnMinTime || timeInMillis > dependsOnMaxTime) {
            return false
        }

        if (batteryStatus.charging != this.createdWithBatteryStatus.charging && this.dependsOnBatteryCharging) {
            return false
        }

        if (batteryStatus.level < dependsOnMinBatteryLevel || batteryStatus.level > dependsOnMaxBatteryLevel) {
            return false
        }

        if (dependsOnNetworkId && currentNetworkId != createdWithNetworkId) {
            return false
        }

        return true
    }
}
