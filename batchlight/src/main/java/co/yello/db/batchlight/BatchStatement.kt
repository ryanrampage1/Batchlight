package co.yello.db.batchlight

import android.database.sqlite.SQLiteDatabase
import co.yello.db.batchlight.androidsqlite.AndroidSQLiteBinder
import co.yello.db.batchlight.androidsqlite.SQLiteBinderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.sql.Statement

/**
 * Drives the inserts into the database.
 *
 * @param T The type of the collection that will be inserted. This is used to pass into the bind function that is
 * passed into the execute function.
 * @property binderConfig A [BinderConfig] used to generate all of the [Binder]s that are required to perform
 * the inserts. This flexibility lets the library work with a variety of databases.
 */
class BatchStatement<T>(
    private val binderConfig: BinderConfig
) {

    /**
     * Chunks the list into the maximum sized list that can be inserted at a time, then executes the insert
     * with the [bindItemFunction] passed into the class.
     *
     * @param itemsToInsert The collection of items of type [T] to batch insert into the database.
     * @param bindItemFunction A function that performs the binds for each field in the [T] passed to the function.
     * The function is an extension on [BatchBinder] so all bind functions can be accessed from within.
     *
     * When binding the order of the bind calls should match the structure of the table inserting.
     *
     * Example usage:
     *
     * A simple class would look something like
     *
     * ```
     * data class BatchClass(val id: Int, val text: String, val num: Int)
     *```
     *
     * The SQL Create statement for the corresponding table to that object would be:
     *
     * ```
     * CREATE TABLE BatchClass (
     *      id INTEGER,
     *      text TEXT,
     *      num INTEGER
     * )
     *```
     *
     * The [Binder] generated for this table will will have the params (?,?,?). To correctly bind the three values
     * to their correct ? in the prepared statement the function would be:
     *
     * ```
     * { batchObject - >
     *      bindLong(batchObject.id)
     *      bindString(batchObject.text)
     *      bindLong(batchObject.num)
     * }
     * ```
     */
    suspend fun execute(itemsToInsert: Collection<T>, bindItemFunction: BatchBinder.(T) -> Unit) {
        itemsToInsert
            .chunked(binderConfig.maxInsertSize)
//            .map { helper(getBinder(it.size), it) }
            .toList()
            .pmap { prepareBinder(getBinder(it.size), it, bindItemFunction) }
//            .forEach { binder ->
//                (binderConfig as SQLiteBinderConfig).db.transaction {
//                    binder.execute()
//                }
//            }
//            .forEach { maxSizeList -> performInsert(maxSizeList, bindItemFunction) }
    }

    fun getBinder(size: Int): Binder {
      return if (size == binderConfig.maxInsertSize) {
            binderConfig.maxInsertBinder
        } else {
            binderConfig.buildBinder(size)
        }
    }

    /**
     * Binds all values to the correct [Binder] for the given collection then executes the statement.
     *
     * @param collection The collection of items to be bound and executed.
     * @param bindItem The bind statement to execute on each item of the collection.
     */
    private fun prepareBinder(statement: Binder, collection: Collection<T>, bindItem: BatchBinder.(T) -> Unit) {

        val binder = BatchBinder(statement, binderConfig.startIndex)

        collection.forEach { item ->
            binder.bindItem(item)
        }

        check(binder.currentBindIndex - 1 == collection.size * binderConfig.fieldsPerItem) {
            "Expected to bind ${binderConfig.fieldsPerItem} columns per record, " +
                    "found ${binder.currentBindIndex / collection.size} binds per record."
        }

//        (binderConfig as SQLiteBinderConfig).db.transaction {
            statement.execute()
//        }
    }

    suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = withContext(Dispatchers.Default) {
        map { async { f(it) } }.map { it.await() }
    }
}


/**
 * Run [body] in a transaction marking it as successful if it completes without exception.
 *
 * @param exclusive Run in `EXCLUSIVE` mode when true, `IMMEDIATE` mode otherwise.
 */
inline fun <T> SQLiteDatabase.transaction(
    exclusive: Boolean = true,
    body: SQLiteDatabase.() -> T
): T {
    if (exclusive) {
        beginTransaction()
    } else {
        beginTransactionNonExclusive()
    }
    try {
        val result = body()
        setTransactionSuccessful()
        return result
    } finally {
        endTransaction()
    }
}
