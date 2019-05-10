package co.yello.db.batchinsertsample.sql

data class MediumSizeObject(
    val id: Int,
    val text: String,
    val moreText: String
)

/**
 * Helper function to generate a list of [MediumSizeObject].
 *
 * @param count the number of objects to put in the list.
 */
fun createMediumObjects(count: Int): List<MediumSizeObject> {
    val text = "Here is some static text"
    val moreText = "Here is some more text to show how fast insert can be"

    return (1..count).map { MediumSizeObject(it, text, moreText) }
}