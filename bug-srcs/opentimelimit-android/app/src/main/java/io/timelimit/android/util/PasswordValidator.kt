/*
 * Open TimeLimit Copyright <C> 2019 Jonas Lochmann
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
package io.timelimit.android.util

import android.content.Context
import io.timelimit.android.R

object PasswordValidator {
    private const val MINIMAL_CHAR_AMOUNT = 5
    private const val MINIMAL_LETTER_AMOUNT = 2

    /**
     * Use this function to check if the password is possible
     *
     * @param password a password to check
     * @param context  a context
     * @return a string which says what is missing or null if it is ok
     */
    fun validate(password: String, context: Context): String? {
        return if (length(password) < MINIMAL_CHAR_AMOUNT) {
            context.getString(R.string.password_validator_too_short, MINIMAL_CHAR_AMOUNT)
        } else if (countAlphabeticChars(password) < MINIMAL_LETTER_AMOUNT) {
            context.getString(R.string.password_validator_min_letters, MINIMAL_LETTER_AMOUNT)
        } else {
            null
        }
    }

    private fun countAlphabeticChars(string: String): Int {
        var alphabetic = 0
        var i = 0
        val length = length(string)
        while (i < length) {
            if (Character.isLetter(string[i])) {
                alphabetic++
            }
            i++
        }

        return alphabetic
    }

    private fun length(string: String?): Int {
        return string?.length ?: 0
    }
}
