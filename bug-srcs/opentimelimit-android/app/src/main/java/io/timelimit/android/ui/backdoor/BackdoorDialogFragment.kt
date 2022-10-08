/*
 * TimeLimit Copyright <C> 2019 Jonas Lochmann
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
package io.timelimit.android.ui.backdoor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.timelimit.android.R
import io.timelimit.android.crypto.HexString
import io.timelimit.android.databinding.BackdoorDialogBinding

class BackdoorDialogFragment: DialogFragment() {
    companion object {
        private const val DIALOG_TAG = "BackdoorDialogFragment"
        private const val CODE_LENGTH = 256
        private const val STATUS_INPUT = "input"
    }

    val input = MutableLiveData<String>().apply { value = "" }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = BackdoorDialogBinding.inflate(inflater, container, false)
        val model = ViewModelProviders.of(this).get(BackdoorModel::class.java)

        if (savedInstanceState != null) {
            input.value = savedInstanceState.getString(STATUS_INPUT)
        }

        binding.handlers = object: BackdoorDialogListener {
            override fun onButtonClicked(value: Int) {
                val char = "0123456789ABCDEF".substring(value, value + 1)

                input.value = input.value!! + char

                if (input.value!!.length == CODE_LENGTH) {
                    model.validateSignatureAndReset(HexString.fromHex(input.value!!))
                }
            }

            override fun removeLastChar() {
                input.value = input.value!!.let { old ->
                    if (old.isEmpty()) {
                        old
                    } else {
                        old.substring(0, old.length - 1)
                    }
                }
            }
        }

        input.observe(this, Observer {
            binding.input = it.chunked(4).joinToString(separator = "-")
            binding.progress.max = CODE_LENGTH
            binding.progress.progress = it.length
        })

        model.nonce.observe(this, Observer {
            binding.nonce = it
        })

        model.status.observe(this, Observer {
            when (it!!) {
                RecoveryStatus.WaitingForCode -> {
                    binding.enableButtons = true
                }
                RecoveryStatus.Verifying -> {
                    binding.enableButtons = false
                }
                RecoveryStatus.Done -> {
                    dismiss()
                }
                RecoveryStatus.Invalid -> {
                    input.value = ""
                    Toast.makeText(context!!, R.string.backdoor_toast_invalid_code, Toast.LENGTH_SHORT).show()

                    model.confirmInvalidCode()
                }
            }
        })

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(STATUS_INPUT, input.value!!)
    }

    fun show(fragmentManager: FragmentManager) = show(fragmentManager, DIALOG_TAG)
}

interface BackdoorDialogListener {
    fun onButtonClicked(value: Int)
    fun removeLastChar()
}