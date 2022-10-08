/*
 * Open TimeLimit Copyright <C> 2019 - 2020 Jonas Lochmann
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
package io.timelimit.android.integration.platform.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import io.timelimit.android.R

object NotificationIds {
    const val APP_STATUS = 1
    const val NOTIFICATION_BLOCKED = 2
    const val REVOKE_TEMPORARILY_ALLOWED_APPS = 3
    const val TIME_WARNING = 4
}

object NotificationChannels {
    const val APP_STATUS = "app status"
    const val BLOCKED_NOTIFICATIONS_NOTIFICATION = "notification blocked notification"
    const val TIME_WARNING = "time warning"

    private fun createAppStatusChannel(notificationManager: NotificationManager, context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                    NotificationChannel(
                            APP_STATUS,
                            context.getString(R.string.notification_channel_app_status_title),
                            NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = context.getString(R.string.notification_channel_app_status_description)
                        enableLights(false)
                        setSound(null, null)
                        enableVibration(false)
                        setShowBadge(false)
                        lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
                    }
            )
        }
    }

    private fun createBlockedNotificationChannel(notificationManager: NotificationManager, context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                    NotificationChannel(
                            NotificationChannels.BLOCKED_NOTIFICATIONS_NOTIFICATION,
                            context.getString(R.string.notification_channel_blocked_notification_title),
                            NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = context.getString(R.string.notification_channel_blocked_notification_text)
                    }
            )
        }
    }

    private fun createTimeWarningsNotificationChannel(notificationManager: NotificationManager, context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                    NotificationChannel(
                            NotificationChannels.TIME_WARNING,
                            context.getString(R.string.notification_channel_time_warning_title),
                            NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = context.getString(R.string.notification_channel_time_warning_text)
                    }
            )
        }
    }

    fun createNotificationChannels(notificationManager: NotificationManager, context: Context) {
        createAppStatusChannel(notificationManager, context)
        createBlockedNotificationChannel(notificationManager, context)
        createTimeWarningsNotificationChannel(notificationManager, context)
    }
}

object PendingIntentIds {
    const val OPEN_MAIN_APP = 1
    const val REVOKE_TEMPORARILY_ALLOWED = 2
    const val SWITCH_TO_DEFAULT_USER = 3
    val DYNAMIC_NOTIFICATION_RANGE = 100..10000
}
