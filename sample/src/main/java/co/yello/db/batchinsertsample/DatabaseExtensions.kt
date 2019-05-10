package co.yello.db.batchinsertsample

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Run [body] in a transaction marking it as successful if it completes without exception.
 *
 * @param exclusive Run in `EXCLUSIVE` mode when true, `IMMEDIATE` mode otherwise.
 */
inline fun <T> SupportSQLiteDatabase.transaction(
    exclusive: Boolean = true,
    body: SupportSQLiteDatabase.() -> T
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


