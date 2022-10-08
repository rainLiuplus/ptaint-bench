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
package io.timelimit.android.data.model

import android.util.JsonReader
import android.util.JsonWriter
import androidx.room.ColumnInfo
import androidx.room.Entity
import io.timelimit.android.data.IdGenerator
import io.timelimit.android.data.JsonSerializable

@Entity(primaryKeys = ["category_id", "package_name"], tableName = "category_app")
data class CategoryApp(
        @ColumnInfo(index = true, name = "category_id")
        val categoryId: String,
        @ColumnInfo(index = true, name = "package_name")
        val packageName: String
): JsonSerializable {
    companion object {
        private const val CATEGORY_ID = "categoryId"
        private const val PACKAGE_NAME = "packageName"

        fun parse(reader: JsonReader): CategoryApp {
            var categoryId: String? = null
            var packageName: String? = null

            reader.beginObject()

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    CATEGORY_ID -> categoryId = reader.nextString()
                    PACKAGE_NAME -> packageName = reader.nextString()
                    else -> reader.skipValue()
                }
            }

            reader.endObject()

            return CategoryApp(
                    categoryId = categoryId!!,
                    packageName = packageName!!
            )
        }
    }

    @delegate:Transient
    val packageNameWithoutActivityName: String by lazy {
        if (specifiesActivity)
            packageName.substring(0, packageName.indexOf(":"))
        else
            packageName
    }

    @Transient
    val specifiesActivity = packageName.contains(":")

    init {
        IdGenerator.assertIdValid(categoryId)

        if (packageName.isEmpty()) {
            throw IllegalArgumentException()
        }
    }

    override fun serialize(writer: JsonWriter) {
        writer.beginObject()

        writer.name(CATEGORY_ID).value(categoryId)
        writer.name(PACKAGE_NAME).value(packageName)

        writer.endObject()
    }
}
