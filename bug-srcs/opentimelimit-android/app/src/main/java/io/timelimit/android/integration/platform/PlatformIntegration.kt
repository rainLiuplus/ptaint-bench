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
package io.timelimit.android.integration.platform

import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.room.TypeConverter
import io.timelimit.android.data.model.App
import io.timelimit.android.data.model.AppActivity
import kotlinx.android.parcel.Parcelize

abstract class PlatformIntegration(
        val maximumProtectionLevel: ProtectionLevel
) {
    abstract fun getLocalApps(): Collection<App>
    abstract fun getLocalAppPackageNames(): List<String>
    abstract fun getLocalAppActivities(deviceId: String): Collection<AppActivity>
    abstract fun getLocalAppTitle(packageName: String): String?
    abstract fun getAppIcon(packageName: String): Drawable?
    abstract fun isSystemImageApp(packageName: String): Boolean
    abstract fun getLauncherAppPackageName(): String?
    abstract fun getCurrentProtectionLevel(): ProtectionLevel
    abstract fun getForegroundAppPermissionStatus(): RuntimePermissionStatus
    abstract fun getDrawOverOtherAppsPermissionStatus(): RuntimePermissionStatus
    abstract fun getNotificationAccessPermissionStatus(): NewPermissionStatus
    abstract fun getOverlayPermissionStatus(): RuntimePermissionStatus
    abstract fun isAccessibilityServiceEnabled(): Boolean
    abstract fun disableDeviceAdmin()
    abstract fun trySetLockScreenPassword(password: String): Boolean
    // this must have a fallback if the permission is not granted
    abstract fun showOverlayMessage(text: String)

    abstract fun showAppLockScreen(currentPackageName: String, currentActivityName: String?)
    abstract fun showAnnoyScreen(annoyDuration: Long)
    abstract fun muteAudioIfPossible(packageName: String)
    abstract fun setShowBlockingOverlay(show: Boolean, blockedElement: String? = null)
    // this should throw an SecurityException if the permission is missing
    abstract suspend fun getForegroundApps(queryInterval: Long, enableMultiAppDetection: Boolean): Set<ForegroundApp>
    abstract fun getMusicPlaybackPackage(): String?
    abstract fun setAppStatusMessage(message: AppStatusMessage?)
    abstract fun isScreenOn(): Boolean
    abstract fun setShowNotificationToRevokeTemporarilyAllowedApps(show: Boolean)
    abstract fun showTimeWarningNotification(title: String, text: String)
    // returns package names for which it was set
    abstract fun setSuspendedApps(packageNames: List<String>, suspend: Boolean): List<String>
    abstract fun stopSuspendingForAllApps()

    // returns true on success
    abstract fun setEnableSystemLockdown(enableLockdown: Boolean): Boolean
    // returns true on success
    abstract fun setLockTaskPackages(packageNames: List<String>): Boolean

    abstract fun getBatteryStatus(): BatteryStatus
    abstract fun getBatteryStatusLive(): LiveData<BatteryStatus>

    abstract fun setEnableCustomHomescreen(enable: Boolean)

    // this function requires the device owner permission and a recent android version
    abstract fun setForceNetworkTime(enable: Boolean)

    abstract fun restartApp()

    abstract fun getCurrentNetworkId(): NetworkId

    var installedAppsChangeListener: Runnable? = null
    var systemClockChangeListener: Runnable? = null
}

data class ForegroundApp(val packageName: String, val activityName: String)

enum class ProtectionLevel {
    None, SimpleDeviceAdmin, PasswordDeviceAdmin, DeviceOwner
}

object ProtectionLevelUtil {
    private const val NONE = "none"
    private const val SIMPLE_DEVICE_ADMIN = "simple device admin"
    private const val PASSWORD_DEVICE_ADMIN = "password device admin"
    private const val DEVICE_OWNER = "device owner"

    fun serialize(level: ProtectionLevel) = when(level) {
        ProtectionLevel.None -> NONE
        ProtectionLevel.SimpleDeviceAdmin -> SIMPLE_DEVICE_ADMIN
        ProtectionLevel.PasswordDeviceAdmin -> PASSWORD_DEVICE_ADMIN
        ProtectionLevel.DeviceOwner -> DEVICE_OWNER
    }

    fun parse(level: String) = when(level) {
        NONE -> ProtectionLevel.None
        SIMPLE_DEVICE_ADMIN -> ProtectionLevel.SimpleDeviceAdmin
        PASSWORD_DEVICE_ADMIN -> ProtectionLevel.PasswordDeviceAdmin
        DEVICE_OWNER -> ProtectionLevel.DeviceOwner
        else -> throw IllegalArgumentException()
    }

    fun toInt(level: ProtectionLevel) = when(level) {
        ProtectionLevel.None -> 0
        ProtectionLevel.SimpleDeviceAdmin -> 1
        ProtectionLevel.PasswordDeviceAdmin -> 2
        ProtectionLevel.DeviceOwner -> 3
    }
}

class ProtectionLevelConverter {
    @TypeConverter
    fun fromString(value: String) = ProtectionLevelUtil.parse(value)

    @TypeConverter
    fun toString(value: ProtectionLevel) = ProtectionLevelUtil.serialize(value)
}

enum class RuntimePermissionStatus {
    NotRequired, Granted, NotGranted
}

object RuntimePermissionStatusUtil {
    private const val NOT_REQUIRED = "not required"
    private const val GRANTED = "granted"
    private const val NOT_GRANTED = "not granted"

    fun serialize(value: RuntimePermissionStatus) = when(value) {
        RuntimePermissionStatus.NotRequired -> NOT_REQUIRED
        RuntimePermissionStatus.Granted -> GRANTED
        RuntimePermissionStatus.NotGranted -> NOT_GRANTED
    }

    fun parse(value: String) = when(value) {
        NOT_REQUIRED -> RuntimePermissionStatus.NotRequired
        GRANTED -> RuntimePermissionStatus.Granted
        NOT_GRANTED -> RuntimePermissionStatus.NotGranted
        else -> throw IllegalArgumentException()
    }

    fun toInt(value: RuntimePermissionStatus) = when(value) {
        RuntimePermissionStatus.NotGranted -> 0
        RuntimePermissionStatus.NotRequired -> 1
        RuntimePermissionStatus.Granted -> 2
    }
}

class RuntimePermissionStatusConverter {
    @TypeConverter
    fun fromString(value: String) = RuntimePermissionStatusUtil.parse(value)

    @TypeConverter
    fun toString(value: RuntimePermissionStatus) = RuntimePermissionStatusUtil.serialize(value)
}

enum class NewPermissionStatus {
    NotSupported, Granted, NotGranted
}

object NewPermissionStatusUtil {
    private const val NOT_SUPPORTED = "not supported"
    private const val GRANTED = "granted"
    private const val NOT_GRANTED = "not granted"

    fun serialize(value: NewPermissionStatus) = when(value) {
        NewPermissionStatus.NotSupported -> NOT_SUPPORTED
        NewPermissionStatus.Granted -> GRANTED
        NewPermissionStatus.NotGranted -> NOT_GRANTED
    }

    fun parse(value: String) = when(value) {
        NOT_SUPPORTED -> NewPermissionStatus.NotSupported
        GRANTED -> NewPermissionStatus.Granted
        NOT_GRANTED -> NewPermissionStatus.NotGranted
        else -> throw IllegalArgumentException()
    }

    fun toInt(value: NewPermissionStatus) = when(value) {
        NewPermissionStatus.NotGranted -> 0
        NewPermissionStatus.NotSupported -> 1
        NewPermissionStatus.Granted -> 2
    }
}

class NewPermissionStatusConverter {
    @TypeConverter
    fun fromString(value: String) = NewPermissionStatusUtil.parse(value)

    @TypeConverter
    fun toString(value: NewPermissionStatus) = NewPermissionStatusUtil.serialize(value)
}

@Parcelize
data class AppStatusMessage(
        val title: String,
        val text: String,
        val subtext: String? = null,
        val showSwitchToDefaultUserOption: Boolean = false
): Parcelable

data class BatteryStatus(
        val charging: Boolean,
        val level: Int
) {
    companion object {
        val dummy = BatteryStatus(false, 0)
    }

    init {
        if (level < 0 || level > 100) {
            throw IllegalArgumentException()
        }
    }
}

sealed class NetworkId {
    object NoNetworkConnected : NetworkId()
    object MissingPermission: NetworkId()
    data class Network(val id: String): NetworkId()
}

fun NetworkId.getNetworkIdOrNull(): String? = if (this is NetworkId.Network) this.id else null