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
package io.timelimit.android.logic

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import io.timelimit.android.BuildConfig
import io.timelimit.android.R
import io.timelimit.android.async.Threads
import io.timelimit.android.coroutines.executeAndWait
import io.timelimit.android.coroutines.runAsyncExpectForever
import io.timelimit.android.data.model.AppActivity
import io.timelimit.android.livedata.*
import io.timelimit.android.sync.actions.*
import io.timelimit.android.sync.actions.apply.ApplyActionUtil
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncInstalledAppsLogic(val appLogic: AppLogic) {
    companion object {
        private const val LOG_TAG = "SyncInstalledAppsLogic"
    }

    private val doSyncLock = Mutex()
    private var requestSync = MutableLiveData<Boolean>().apply { value = false }

    private fun requestSync() {
        requestSync.value = true
    }

    init {
        appLogic.platformIntegration.installedAppsChangeListener = Runnable { requestSync() }
        appLogic.deviceEntry.map { it?.id + it?.enableActivityLevelBlocking }.ignoreUnchanged().observeForever { requestSync() }
        runAsyncExpectForever { syncLoop() }
    }

    private suspend fun syncLoop() {
        requestSync.postValue(true)

        // wait a moment before the first sync
        appLogic.timeApi.sleep(15 * 1000)

        while (true) {
            requestSync.waitUntilValueMatches { it == true }
            requestSync.value = false

            try {
                doSyncNow()

                // maximal 1 time per 5 seconds
                appLogic.timeApi.sleep(5 * 1000)
            } catch (ex: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.w(LOG_TAG, "could not sync installed app list", ex)
                }

                Toast.makeText(appLogic.context, R.string.background_logic_toast_sync_apps, Toast.LENGTH_SHORT).show()

                appLogic.timeApi.sleep(45 * 1000)
                requestSync.value = true
            }
        }
    }

    private suspend fun doSyncNow() {
        doSyncLock.withLock {
            val deviceEntry = appLogic.deviceEntry.waitForNullableValue() ?: return@withLock
            val deviceId = deviceEntry.id

            run {
                val currentlyInstalled = Threads.backgroundOSInteraction.executeAndWait {
                    appLogic.platformIntegration.getLocalApps().associateBy { app -> app.packageName }
                }
                val currentlySaved = appLogic.database.app().getApps().waitForNonNullValue().associateBy { app -> app.packageName }

                // skip all items for removal which are still saved locally
                val itemsToRemove = HashMap(currentlySaved)
                currentlyInstalled.forEach { (packageName, _) -> itemsToRemove.remove(packageName) }

                // only add items which are not the same locally
                val itemsToAdd = currentlyInstalled.filter { (packageName, app) -> currentlySaved[packageName] != app }

                // save the changes
                if (itemsToRemove.isNotEmpty()) {
                    ApplyActionUtil.applyAppLogicAction(
                            action = RemoveInstalledAppsAction(packageNames = itemsToRemove.keys.toList()),
                            appLogic = appLogic,
                            ignoreIfDeviceIsNotConfigured = true
                    )
                }

                if (itemsToAdd.isNotEmpty()) {
                    ApplyActionUtil.applyAppLogicAction(
                            action = AddInstalledAppsAction(
                                    apps = itemsToAdd.map { (_, app) ->

                                        InstalledApp(
                                                packageName = app.packageName,
                                                title = app.title,
                                                recommendation = app.recommendation,
                                                isLaunchable = app.isLaunchable
                                        )
                                    }
                            ),
                            appLogic = appLogic,
                            ignoreIfDeviceIsNotConfigured = true
                    )
                }
            }

            run {
                fun buildKey(activity: AppActivity) = "${activity.appPackageName}:${activity.activityClassName}"

                val currentlyInstalled = if (deviceEntry.enableActivityLevelBlocking)
                    Threads.backgroundOSInteraction.executeAndWait {
                        appLogic.platformIntegration.getLocalAppActivities(deviceId = deviceId).associateBy { buildKey(it) }
                    }
                else
                    emptyMap()

                val currentlySaved = appLogic.database.appActivity().getAppActivitiesByDeviceIds(deviceIds = listOf(deviceId)).waitForNonNullValue().associateBy { buildKey(it) }

                // skip all items for removal which are still saved locally
                val itemsToRemove = HashMap(currentlySaved)
                currentlyInstalled.forEach { (packageName, _) -> itemsToRemove.remove(packageName) }

                // only add items which are not the same locally
                val itemsToAdd = currentlyInstalled.filter { (packageName, app) -> currentlySaved[packageName] != app }

                // save the changes
                if (itemsToRemove.isNotEmpty() or itemsToAdd.isNotEmpty()) {
                    ApplyActionUtil.applyAppLogicAction(
                            action = UpdateAppActivitiesAction(
                                    removedActivities = itemsToRemove.map { it.value.appPackageName to it.value.activityClassName },
                                    updatedOrAddedActivities = itemsToAdd.map { item ->
                                        AppActivityItem(
                                                packageName = item.value.appPackageName,
                                                className = item.value.activityClassName,
                                                title = item.value.title
                                        )
                                    }
                            ),
                            appLogic = appLogic,
                            ignoreIfDeviceIsNotConfigured = true
                    )
                }
            }
        }
    }
}
