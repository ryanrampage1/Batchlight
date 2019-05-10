package co.yello.db.batchinsertsample.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mediumSizedRooms")
data class MediumSizedRoom(
    @PrimaryKey val id: Int,
    val text: String,
    val moreText: String
)

/**
 * Helper function to generate a list of [MediumSizedRoom].
 *
 * @param count the number of objects to put in the list.
 */
fun createMediumRooms(count: Int): List<MediumSizedRoom> {
    val text = "Here is some static text"
    val moreText = "Here is some more text to show how fast insert can be"

    return (1..count).map { MediumSizedRoom(it, text, moreText) }
}