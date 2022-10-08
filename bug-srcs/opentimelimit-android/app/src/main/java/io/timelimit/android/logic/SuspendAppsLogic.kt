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
package io.timelimit.android.logic

import io.timelimit.android.data.invalidation.Observer
import io.timelimit.android.data.invalidation.Table
import io.timelimit.android.data.model.CategoryApp
import io.timelimit.android.data.model.ExperimentalFlags
import io.timelimit.android.data.model.UserType
import io.timelimit.android.data.model.derived.UserRelatedData
import io.timelimit.android.integration.platform.ProtectionLevel
import io.timelimit.android.integration.platform.android.AndroidIntegrationApps
import io.timelimit.android.logic.blockingreason.CategoryHandlingCache
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class SuspendAppsLogic(private val appLogic: AppLogic): Observer {
    private var lastDefaultCategory: String? = null
    private var lastAllowedCategoryList = emptySet<String>()
    private var lastCategoryApps = emptyList<CategoryApp>()
    private val installedAppsModified = AtomicBoolean(false)
    private val categoryHandlingCache = CategoryHandlingCache()
    private var batteryStatus = appLogic.platformIntegration.getBatteryStatus()
    private val pendingSync = AtomicBoolean(true)
    private val executor = Executors.newSingleThreadExecutor()
    private var lastSuspendedApps: List<String>? = null
    private val userAndDeviceRelatedDataLive = appLogic.database.derivedDataDao().getUserAndDeviceRelatedDataLive()
    private var didLoadUserAndDeviceRelatedData = false

    private val backgroundRunnable = Runnable {
        while (pendingSync.getAndSet(false)) {
            updateBlockingSync()

            Thread.sleep(500)
        }
    }

    private val triggerRunnable = Runnable {
        triggerUpdate()
    }

    private fun triggerUpdate() {
        pendingSync.set(true); executor.submit(backgroundRunnable)
    }

    private fun scheduleUpdate(delay: Long) {
        appLogic.timeApi.cancelScheduledAction(triggerRunnable)
        appLogic.timeApi.runDelayedByUptime(triggerRunnable, delay)
    }

    init {
        appLogic.database.registerWeakObserver(arrayOf(Table.App), WeakReference(this))
        appLogic.platformIntegration.getBatteryStatusLive().observeForever { batteryStatus = it; triggerUpdate() }
        appLogic.realTimeLogic.registerTimeModificationListener { triggerUpdate() }
        userAndDeviceRelatedDataLive.observeForever { didLoadUserAndDeviceRelatedData = true; triggerUpdate() }
    }

    override fun onInvalidated(tables: Set<Table>) {
        installedAppsModified.set(true); triggerUpdate()
    }

    private fun updateBlockingSync() {
        if (!didLoadUserAndDeviceRelatedData) return

        val userAndDeviceRelatedData = userAndDeviceRelatedDataLive.value

        val isRestrictedUser = userAndDeviceRelatedData?.userRelatedData?.user?.type == UserType.Child
        val enableBlockingAtSystemLevel = userAndDeviceRelatedData?.deviceRelatedData?.isExperimentalFlagSetSync(ExperimentalFlags.SYSTEM_LEVEL_BLOCKING) ?: false
        val hasPermission = appLogic.platformIntegration.getCurrentProtectionLevel() == ProtectionLevel.DeviceOwner
        val enableBlocking = isRestrictedUser && enableBlockingAtSystemLevel && hasPermission

        if (!enableBlocking) {
            appLogic.platformIntegration.stopSuspendingForAllApps()
            lastSuspendedApps = emptyList()

            lastAllowedCategoryList = emptySet()
            lastCategoryApps = emptyList()
            return
        }

        val userRelatedData = userAndDeviceRelatedData!!.userRelatedData!!
        val now = appLogic.timeApi.getCurrentTimeInMillis()

        categoryHandlingCache.reportStatus(
                user = userRelatedData,
                timeInMillis = now,
                batteryStatus = batteryStatus,
                currentNetworkId = null
        )

        val defaultCategory = userRelatedData.user.categoryForNotAssignedApps
        val blockingAtActivityLevel = userAndDeviceRelatedData.deviceRelatedData.deviceEntry.enableActivityLevelBlocking
        val categoryApps = userRelatedData.categoryApps
        val categoryHandlings = userRelatedData.categoryById.keys.map { categoryHandlingCache.get(it) }
        val categoryIdsToAllow = categoryHandlings.filterNot { it.shouldBlockAtSystemLevel }.map { it.createdWithCategoryRelatedData.category.id }.toMutableSet()

        var didModify: Boolean

        do {
            didModify = false

            val iterator = categoryIdsToAllow.iterator()

            for (categoryId in iterator) {
                val parentCategory = userRelatedData.categoryById[userRelatedData.categoryById[categoryId]?.category?.parentCategoryId]

                if (parentCategory != null && !categoryIdsToAllow.contains(parentCategory.category.id)) {
                    iterator.remove(); didModify = true
                }
            }
        } while (didModify)

        categoryHandlings.minBy { it.dependsOnMaxTime }?.let {
            scheduleUpdate((it.dependsOnMaxTime - now))
        }

        if (
                categoryIdsToAllow != lastAllowedCategoryList || categoryApps != lastCategoryApps ||
                installedAppsModified.getAndSet(false) || defaultCategory != lastDefaultCategory
        ) {
            lastAllowedCategoryList = categoryIdsToAllow
            lastCategoryApps = categoryApps
            lastDefaultCategory = defaultCategory

            val installedApps = appLogic.platformIntegration.getLocalAppPackageNames()
            val prepared = getAppsWithCategories(installedApps, userRelatedData, blockingAtActivityLevel)
            val appsToBlock = mutableListOf<String>()

            installedApps.forEach { packageName ->
                val appCategories = prepared[packageName] ?: emptySet()

                if (appCategories.find { categoryId -> categoryIdsToAllow.contains(categoryId) } == null) {
                    if (!AndroidIntegrationApps.appsToNotSuspend.contains(packageName)) {
                        appsToBlock.add(packageName)
                    }
                }
            }

            applySuspendedApps(appsToBlock)
        }
    }

    private fun getAppsWithCategories(packageNames: List<String>, data: UserRelatedData, blockingAtActivityLevel: Boolean): Map<String, Set<String>> {
        val categoryForUnassignedApps = data.categoryById[data.user.categoryForNotAssignedApps]
        val categoryForOtherSystemApps = data.findCategoryApp(DummyApps.NOT_ASSIGNED_SYSTEM_IMAGE_APP)?.categoryId?.let { data.categoryById[it] }

        if (blockingAtActivityLevel) {
            val categoriesByPackageName = data.categoryApps.groupBy { it.packageNameWithoutActivityName }

            val result = mutableMapOf<String, Set<String>>()

            packageNames.forEach { packageName ->
                val categoriesItems = categoriesByPackageName[packageName]
                val categories = (categoriesItems?.map { it.categoryId }?.toSet() ?: emptySet()).toMutableSet()
                val isMainAppIncluded = categoriesItems?.find { !it.specifiesActivity } != null

                if (!isMainAppIncluded) {
                    if (categoryForOtherSystemApps != null && appLogic.platformIntegration.isSystemImageApp(packageName)) {
                        categories.add(categoryForOtherSystemApps.category.id)
                    } else if (categoryForUnassignedApps != null) {
                        categories.add(categoryForUnassignedApps.category.id)
                    }
                }

                result[packageName] = categories
            }

            return result
        } else {
            val categoryByPackageName = data.categoryApps.associateBy { it.packageName }

            val result = mutableMapOf<String, Set<String>>()

            packageNames.forEach { packageName ->
                val category = categoryByPackageName[packageName]?.categoryId ?: run {
                    if (categoryForOtherSystemApps != null && appLogic.platformIntegration.isSystemImageApp(packageName))
                        categoryForOtherSystemApps.category.id else categoryForUnassignedApps?.category?.id
                }

                result[packageName] = if (category != null) setOf(category) else emptySet()
            }

            return result
        }
    }

    private fun applySuspendedApps(packageNames: List<String>) {
        if (packageNames == lastSuspendedApps) {
            // nothing to do
        } else if (packageNames.isEmpty()) {
            appLogic.platformIntegration.stopSuspendingForAllApps()
            lastSuspendedApps = emptyList()
        } else {
            val allApps = appLogic.platformIntegration.getLocalAppPackageNames()
            val appsToNotBlock = allApps.subtract(packageNames)

            appLogic.platformIntegration.setSuspendedApps(appsToNotBlock.toList(), false)
            appLogic.platformIntegration.setSuspendedApps(packageNames, true)
            lastSuspendedApps = packageNames
        }
    }
}