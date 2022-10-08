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
package io.timelimit.android.data.model

import android.util.JsonReader
import android.util.JsonWriter
import androidx.room.*
import io.timelimit.android.data.IdGenerator
import io.timelimit.android.data.JsonSerializable
import io.timelimit.android.data.customtypes.ImmutableBitmaskAdapter

@Entity(tableName = "user")
@TypeConverters(
        UserTypeConverter::class,
        ImmutableBitmaskAdapter::class
)
data class User(
        @PrimaryKey
        @ColumnInfo(name = "id")
        val id: String,
        @ColumnInfo(name = "name")
        val name: String,
        @ColumnInfo(name = "password")
        val password: String,   // protected using bcrypt, can be empty if not configured
        @ColumnInfo(name = "type")
        val type: UserType,
        @ColumnInfo(name = "timezone")
        val timeZone: String,
        // 0 = time limits enabled
        @ColumnInfo(name = "disable_limits_until")
        val disableLimitsUntil: Long,
        @ColumnInfo(name = "category_for_not_assigned_apps")
        // empty or invalid = no category
        val categoryForNotAssignedApps: String,
        @ColumnInfo(name = "blocked_times")
        @Deprecated(message = "this feature was removed; the limit login category is a replacement")
        val obsoleteBlockedTimes: String = "",
        @ColumnInfo(name = "flags")
        val flags: Long
): JsonSerializable {
    companion object {
        private const val ID = "id"
        private const val NAME = "name"
        private const val PASSWORD = "password"
        private const val TYPE = "type"
        private const val TIMEZONE = "timeZone"
        private const val DISABLE_LIMITS_UNTIL = "disableLimitsUntil"
        private const val CATEGORY_FOR_NOT_ASSIGNED_APPS = "categoryForNotAssignedApps"
        private const val OBSOLETE_BLOCKED_TIMES = "blockedTimes"
        private const val FLAGS = "flags"

        fun parse(reader: JsonReader): User {
            var id: String? = null
            var name: String? = null
            var password: String? = null
            var type: UserType? = null
            var timeZone: String? = null
            var disableLimitsUntil: Long? = null
            var categoryForNotAssignedApps = ""
            var flags = 0L

            reader.beginObject()
            while (reader.hasNext()) {
                when(reader.nextName()) {
                    ID -> id = reader.nextString()
                    NAME -> name = reader.nextString()
                    PASSWORD -> password = reader.nextString()
                    TYPE -> type = UserTypeJson.parse(reader.nextString())
                    TIMEZONE -> timeZone = reader.nextString()
                    DISABLE_LIMITS_UNTIL -> disableLimitsUntil = reader.nextLong()
                    CATEGORY_FOR_NOT_ASSIGNED_APPS -> categoryForNotAssignedApps = reader.nextString()
                    FLAGS -> flags = reader.nextLong()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            return User(
                    id = id!!,
                    name = name!!,
                    password = password!!,
                    type = type!!,
                    timeZone = timeZone!!,
                    disableLimitsUntil = disableLimitsUntil!!,
                    categoryForNotAssignedApps = categoryForNotAssignedApps,
                    flags = flags
            )
        }
    }

    init {
        IdGenerator.assertIdValid(id)

        if (disableLimitsUntil < 0) {
            throw IllegalArgumentException()
        }

        if (name.isEmpty()) {
            throw IllegalArgumentException()
        }

        if (timeZone.isEmpty()) {
            throw IllegalArgumentException()
        }

        if (categoryForNotAssignedApps.isNotEmpty()) {
            IdGenerator.assertIdValid(categoryForNotAssignedApps)
        }
    }

    val restrictViewingToParents: Boolean
        get() = flags and UserFlags.RESTRICT_VIEWING_TO_PARENTS == UserFlags.RESTRICT_VIEWING_TO_PARENTS

    val allowSelfLimitAdding: Boolean
        get() = flags and UserFlags.ALLOW_SELF_LIMIT_ADD == UserFlags.ALLOW_SELF_LIMIT_ADD

    override fun serialize(writer: JsonWriter) {
        writer.beginObject()

        writer.name(ID).value(id)
        writer.name(NAME).value(name)
        writer.name(PASSWORD).value(password)
        writer.name(TYPE).value(UserTypeJson.serialize(type))
        writer.name(TIMEZONE).value(timeZone)
        writer.name(DISABLE_LIMITS_UNTIL).value(disableLimitsUntil)
        writer.name(CATEGORY_FOR_NOT_ASSIGNED_APPS).value(categoryForNotAssignedApps)
        writer.name(OBSOLETE_BLOCKED_TIMES).value("")
        writer.name(FLAGS).value(flags)

        writer.endObject()
    }
}

enum class UserType {
    Parent, Child
}

object UserTypeJson {
    private const val PARENT = "parent"
    private const val CHILD = "child"

    fun parse(value: String) = when(value) {
        PARENT -> UserType.Parent
        CHILD -> UserType.Child
        else -> throw IllegalArgumentException()
    }

    fun serialize(value: UserType) = when(value) {
        UserType.Parent -> PARENT
        UserType.Child -> CHILD
    }
}

class UserTypeConverter {
    @TypeConverter
    fun toUserType(value: String) = UserTypeJson.parse(value)

    @TypeConverter
    fun toString(value: UserType) = UserTypeJson.serialize(value)
}

object UserFlags {
    const val RESTRICT_VIEWING_TO_PARENTS = 1L
    const val ALLOW_SELF_LIMIT_ADD = 2L
    const val ALL_FLAGS = RESTRICT_VIEWING_TO_PARENTS or ALLOW_SELF_LIMIT_ADD
}