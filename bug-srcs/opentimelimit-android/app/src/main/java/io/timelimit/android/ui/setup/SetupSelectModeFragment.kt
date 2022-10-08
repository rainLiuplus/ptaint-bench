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
package io.timelimit.android.ui.setup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import io.timelimit.android.R
import io.timelimit.android.databinding.FragmentSetupSelectModeBinding
import io.timelimit.android.extensions.safeNavigate
import io.timelimit.android.logic.DefaultAppLogic
import io.timelimit.android.ui.setup.parentmode.SetupParentmodeDialogFragment
import kotlinx.android.synthetic.main.fragment_setup_select_mode.*

class SetupSelectModeFragment : Fragment() {
    companion object {
        private const val REQUEST_SETUP_PARENT_MODE = 1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentSetupSelectModeBinding.inflate(inflater, container, false).root
    }

    private lateinit var navigation: NavController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navigation = Navigation.findNavController(view)

        btn_child_mode.setOnClickListener {
            navigation.safeNavigate(
                    SetupSelectModeFragmentDirections.actionSetupSelectModeFragmentToSetupDevicePermissionsFragment(),
                    R.id.setupSelectModeFragment
            )
        }

        btn_parent_mode.setOnClickListener {
            SetupParentmodeDialogFragment().apply {
                setTargetFragment(this@SetupSelectModeFragment, REQUEST_SETUP_PARENT_MODE)
            }.show(parentFragmentManager)
        }

        btn_uninstall.setOnClickListener {
            DefaultAppLogic.with(context!!).platformIntegration.disableDeviceAdmin()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context!!.packageName}"))
                                .addCategory(Intent.CATEGORY_DEFAULT)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } else {
                startActivity(
                        Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:${context!!.packageName}"))
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SETUP_PARENT_MODE && resultCode == Activity.RESULT_OK) {
            navigation.popBackStack(R.id.overviewFragment, false)
        }
    }
}