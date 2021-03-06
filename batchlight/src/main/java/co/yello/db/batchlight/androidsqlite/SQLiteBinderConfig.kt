package co.yello.db.batchlight.androidsqlite

import android.database.sqlite.SQLiteDatabase
import co.yello.db.batchlight.Binder
import co.yello.db.batchlight.BinderConfig
import co.yello.db.batchlight.sqlAndroidPreparedStatementStartIndex
import co.yello.db.batchlight.sqlMaxBinds

/**
 * The [BinderConfig] for Android SQLite.
 *
 * @property db the SQLite database that is being written to.
 * @property batchStatement the statement that will be executed. For SQLite it will be either REPLACE INTO TABLE_NAME or
 * INSERT INTO TABLE_NAME
 *
 * @constructor
 *
 * @param fieldsPerItem the number of fields per row to be inserted.
 * @param maxFields the maximum number of binds that can happen per statement.
 */
class SQLiteBinderConfig(
    private val db: SQLiteDatabase,
    private val batchStatement: String,
    override val fieldsPerItem: Int,
    override val maxFields: Int
) : BinderConfig {


    /**
     * The maximum number of items that can be inserted per statement.
     */
    override val maxInsertSize= if (fieldsPerItem > 0) maxFields / fieldsPerItem else 1

    /**
     * For a SQLite insert statement each item has the format (?, ?, ?)
     */
    private val objectStatement = (1..fieldsPerItem).joinToString(
        prefix = "(",
        transform = { "?" },
        separator = ",",
        postfix= ")"
    )

    override val maxInsertBinder: Binder by lazy {
        buildBinder(maxInsertSize)
    }

    override val startIndex: Int = sqlAndroidPreparedStatementStartIndex

    override fun buildBinder(insertCount: Int): Binder {
        val allObjects = (1..insertCount).joinToString(",") { objectStatement }
        val compiledStatement =  db.compileStatement("$batchStatement $allObjects")
        return AndroidSQLiteBinder(compiledStatement)
    }

    companion object {

        /**
         * Creates a statement generator for batch replace statements.
         *
         * @param db the database to perform the replaces on.
         * @param tableName the name of the table to preform the replaces on.
         * @param columnCount the number of columns that will be replaced per row.
         * @param maxBinds the total number of values that can be bound to the statement.
         */
        fun getReplaceConfig(
            db: SQLiteDatabase,
            tableName: String,
            columnCount: Int,
            maxBinds: Int = sqlMaxBinds
        )  = SQLiteBinderConfig(
            db,
            "REPLACE INTO $tableName VALUES",
            columnCount,
            maxBinds
        )

        /**
         * Creates a statement generator for batch insert statements.
         *
         * @param db the database to perform the insert on.
         * @param tableName the name of the table to preform the insert on.
         * @param columnCount the number of columns that will be insert per row.
         * @param maxBinds the total number of values that can be bound to the statement.
         */
        fun getInsertConfig(
            db: SQLiteDatabase,
            tableName: String,
            columnCount: Int,
            maxBinds: Int = sqlMaxBinds
        ) = SQLiteBinderConfig(
            db,
            "INSERT INTO $tableName VALUES",
            columnCount,
            maxBinds
        )
    }
}