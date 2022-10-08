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

package io.timelimit.android.ui.manage.category.appsandrules

import io.timelimit.android.data.model.TimeLimitRule

sealed class AppAndRuleItem {
    data class AppEntry(val title: String, val packageName: String, val packageNameWithoutActivityName: String): AppAndRuleItem()
    object AddAppItem: AppAndRuleItem()
    object ExpandAppsItem: AppAndRuleItem()
    data class RuleEntry(val rule: TimeLimitRule): AppAndRuleItem()
    object ExpandRulesItem: AppAndRuleItem()
    object RulesIntro: AppAndRuleItem()
    object AddRuleItem: AppAndRuleItem()
    data class Headline(val stringRessource: Int): AppAndRuleItem()
}