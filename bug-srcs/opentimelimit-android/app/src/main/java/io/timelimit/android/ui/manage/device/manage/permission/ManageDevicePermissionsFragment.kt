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
package io.timelimit.android.ui.manage.device.manage.permission

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import io.timelimit.android.R
import io.timelimit.android.data.model.Device
import io.timelimit.android.data.model.UserType
import io.timelimit.android.databinding.ManageDevicePermissionsFragmentBinding
import io.timelimit.android.integration.platform.NewPermissionStatus
import io.timelimit.android.integration.platform.ProtectionLevel
import io.timelimit.android.integration.platform.RuntimePermissionStatus
import io.timelimit.android.integration.platform.android.AdminReceiver
import io.timelimit.android.livedata.liveDataFromValue
import io.timelimit.android.livedata.map
import io.timelimit.android.logic.AppLogic
import io.timelimit.android.logic.DefaultAppLogic
import io.timelimit.android.ui.help.HelpDialogFragment
import io.timelimit.android.ui.main.ActivityViewModel
import io.timelimit.android.ui.main.ActivityViewModelHolder
import io.timelimit.android.ui.main.AuthenticationFab
import io.timelimit.android.ui.main.FragmentWithCustomTitle

class ManageDevicePermissionsFragment : Fragment(), FragmentWithCustomTitle {
    companion object {
        fun getPreviewText(device: Device, context: Context): String {
            val permissionLabels = mutableListOf<String>()

            if (device.currentUsageStatsPermission == RuntimePermissionStatus.Granted) {
                permissionLabels.add(context.getString(R.string.manage_device_permissions_usagestats_title_short))
            }

            if (device.currentNotificationAccessPermission == NewPermissionStatus.Granted) {
                permissionLabels.add(context.getString(R.string.manage_device_permission_notification_access_title))
            }

            if (device.currentProtectionLevel != ProtectionLevel.None) {
                permissionLabels.add(context.getString(R.string.manage_device_permission_device_admin_title))
            }

            if (device.currentOverlayPermission == RuntimePermissionStatus.Granted) {
                permissionLabels.add(context.getString(R.string.manage_device_permissions_overlay_title))
            }

            if (device.accessibilityServiceEnabled) {
                permissionLabels.add(context.getString(R.string.manage_device_permission_accessibility_title))
            }

            return if (permissionLabels.isEmpty()) {
                context.getString(R.string.manage_device_permissions_summary_none)
            } else {
                permissionLabels.joinToString(", ")
            }
        }
    }

    private val activity: ActivityViewModelHolder by lazy { getActivity() as ActivityViewModelHolder }
    private val logic: AppLogic by lazy { DefaultAppLogic.with(context!!) }
    private val auth: ActivityViewModel by lazy { activity.getActivityViewModel() }
    private val args: ManageDevicePermissionsFragmentArgs by lazy { ManageDevicePermissionsFragmentArgs.fromBundle(arguments!!) }
    private val deviceEntry: LiveData<Device?> by lazy {
        logic.database.device().getDeviceById(args.deviceId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val navigation = Navigation.findNavController(container!!)
        val binding = ManageDevicePermissionsFragmentBinding.inflate(inflater, container, false)

        // auth
        AuthenticationFab.manageAuthenticationFab(
                fab = binding.fab,
                shouldHighlight = auth.shouldHighlightAuthenticationButton,
                authenticatedUser = auth.authenticatedUser,
                fragment = this,
                doesSupportAuth = liveDataFromValue(true)
        )

        auth.authenticatedUser.map { it?.type == UserType.Parent }.observe(this, Observer {
            binding.isUserSignedIn = it
        })

        // handlers
        binding.handlers = object: ManageDevicePermissionsFragmentHandlers {
            override fun openUsageStatsSettings() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // According to user reports, some devices open the wrong screen
                    // with the Settings.ACTION_USAGE_ACCESS_SETTINGS
                    // but using an activity launcher to open this intent works for them.
                    // This intent works at regular android too, so try this first
                    // and use the "correct" one as fallback.

                    try {
                        startActivity(
                                Intent()
                                        .setClassName("com.android.settings", "com.android.settings.Settings\$UsageAccessSettingsActivity")
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (ex: Exception) {
                        try {
                            startActivity(
                                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (ex: Exception) {
                            AdbUsageStatsDialogFragment().show(parentFragmentManager)
                        }
                    }
                }
            }

            override fun openNotificationAccessSettings() {
                try {
                    startActivity(
                            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (ex: Exception) {
                    Toast.makeText(context, R.string.error_general, Toast.LENGTH_SHORT).show()
                }
            }

            override fun openDrawOverOtherAppsScreen() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context!!.packageName))
                        )
                    } catch (ex: Exception) {
                        Toast.makeText(context, R.string.error_general, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun openAccessibilitySettings() {
                try {
                    startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (ex: Exception) {
                    Toast.makeText(context, R.string.error_general, Toast.LENGTH_SHORT).show()
                }
            }

            override fun manageDeviceAdmin() {
                val protectionLevel = logic.platformIntegration.getCurrentProtectionLevel()

                if (protectionLevel == ProtectionLevel.None) {
                    if (InformAboutDeviceOwnerDialogFragment.shouldShow) {
                        InformAboutDeviceOwnerDialogFragment().show(fragmentManager!!)
                    } else {
                        try {
                            startActivity(
                                    Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                                            .putExtra(
                                                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                                    ComponentName(context!!, AdminReceiver::class.java)
                                            )
                            )
                        } catch (ex: Exception) {
                            AdbDeviceAdminDialogFragment().show(parentFragmentManager)
                        }
                    }
                } else {
                    try {
                        startActivity(
                                Intent(Settings.ACTION_SECURITY_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (ex: Exception) {
                        AdbDeviceAdminDialogFragment().show(parentFragmentManager)
                    }
                }
            }

            override fun showAuthenticationScreen() {
                activity.showAuthenticationScreen()
            }

            override fun helpUsageStatsAccess() {
                HelpDialogFragment.newInstance(
                        title = R.string.manage_device_permissions_usagestats_title,
                        text = R.string.manage_device_permissions_usagestats_text
                ).show(fragmentManager!!)
            }

            override fun helpNotificationAccess() {
                HelpDialogFragment.newInstance(
                        title = R.string.manage_device_permission_notification_access_title,
                        text = R.string.manage_device_permission_notification_access_text
                ).show(fragmentManager!!)
            }

            override fun helpDrawOverOtherApps() {
                HelpDialogFragment.newInstance(
                        title = R.string.manage_device_permissions_overlay_title,
                        text = R.string.manage_device_permissions_overlay_text
                ).show(fragmentManager!!)
            }

            override fun helpAccesibility() {
                HelpDialogFragment.newInstance(
                        title = R.string.manage_device_permission_accessibility_title,
                        text = R.string.manage_device_permission_accessibility_text
                ).show(fragmentManager!!)
            }
        }

        // permissions
        deviceEntry.observe(this, Observer {
            device ->

            if (device == null) {
                navigation.popBackStack(R.id.overviewFragment, false)
            } else {
                binding.usageStatsAccess = device.currentUsageStatsPermission
                binding.notificationAccessPermission = device.currentNotificationAccessPermission
                binding.protectionLevel = device.currentProtectionLevel
                binding.overlayPermission = device.currentOverlayPermission
                binding.accessibilityServiceEnabled = device.accessibilityServiceEnabled
            }
        })


        return binding.root
    }

    override fun onResume() {
        super.onResume()

        logic.backgroundTaskLogic.syncDeviceStatusAsync()
    }

    override fun getCustomTitle(): LiveData<String?> = deviceEntry.map { "${getString(R.string.manage_device_card_permission_title)} < ${it?.name} < ${getString(R.string.main_tab_overview)}" }
}

interface ManageDevicePermissionsFragmentHandlers {
    fun openUsageStatsSettings()
    fun openNotificationAccessSettings()
    fun openDrawOverOtherAppsScreen()
    fun openAccessibilitySettings()
    fun manageDeviceAdmin()
    fun showAuthenticationScreen()
    fun helpUsageStatsAccess()
    fun helpNotificationAccess()
    fun helpDrawOverOtherApps()
    fun helpAccesibility()
}
