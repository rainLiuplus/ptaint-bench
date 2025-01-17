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
package io.timelimit.android.ui.diagnose


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.timelimit.android.R
import io.timelimit.android.databinding.FragmentDiagnoseConnectionBinding
import io.timelimit.android.integration.platform.NetworkId
import io.timelimit.android.livedata.liveDataFromFunction
import io.timelimit.android.livedata.liveDataFromValue
import io.timelimit.android.logic.DefaultAppLogic
import io.timelimit.android.ui.main.FragmentWithCustomTitle

class DiagnoseConnectionFragment : Fragment(), FragmentWithCustomTitle {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentDiagnoseConnectionBinding.inflate(inflater, container, false)
        val logic = DefaultAppLogic.with(context!!)

        liveDataFromFunction { logic.platformIntegration.getCurrentNetworkId() }.observe(viewLifecycleOwner, Observer {
            binding.networkId = when (it) {
                NetworkId.MissingPermission -> "missing permission"
                NetworkId.NoNetworkConnected -> "no network connected"
                is NetworkId.Network -> it.id
            }
        })

        return binding.root
    }

    override fun getCustomTitle(): LiveData<String?> = liveDataFromValue("${getString(R.string.diagnose_connection_title)} < ${getString(R.string.about_diagnose_title)} < ${getString(R.string.main_tab_overview)}")
}
