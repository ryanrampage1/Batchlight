package co.yello.db.batchinsertsample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlin.system.measureTimeMillis
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import co.yello.db.batchinsertsample.room.MediumSizedRoom
import co.yello.db.batchinsertsample.room.RoomDemoDatabase
import co.yello.db.batchinsertsample.room.createMediumRooms
import co.yello.db.batchinsertsample.sql.MediumSizeObject
import co.yello.db.batchinsertsample.sql.SQLiteHelper
import co.yello.db.batchinsertsample.sql.createMediumObjects
import co.yello.db.batchlight.BatchStatement
import co.yello.db.batchlight.androidsupportsqlite.SupportSQLiteBinderConfig


class MainActivity : AppCompatActivity() {

    // A job that just tracks the database inserts. When the mainScopeJob is cancelled it will make the scope unusable
    // so we need this independent job to be able to start and stop the inserts without stopping all future inserts.
    private var dbInsertJob = Job()
    // This job is a parent job for all jobs ran within the mainScope. When this is cancelled all children are also
    // cancelled
    private val mainScopeJob: Job = Job()
    private val mainScope = CoroutineScope(mainScopeJob + Dispatchers.Main)

    private val databaseHelper: SQLiteHelper by lazy {
        SQLiteHelper(this)
    }
    private val roomDb by lazy {
        RoomDemoDatabase.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        doInsertButton.setOnClickListener {
            executeInserts()
        }

        rowCount.setOnEditorActionListener(object : OnEditorActionListener {
            override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                if (event?.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                    executeInserts()
                }
                return false
            }
        })
    }

    private fun executeInserts() {
        generateAndRun(rowCount.text.toString().ifBlank { "0" }.toInt())
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScopeJob.cancel()
    }

    private suspend fun insertItems(items: List<MediumSizeObject>) = withContext(Dispatchers.Main) {
        Log.i(tag, "Starting individual insertion execution.")
        val time = measureTimeSeconds {
            val channel = Channel<Int>(Channel.CONFLATED)

            launch {
                databaseHelper.loopInsert(channel, items)
                channel.close()
            }

            val progressPipeline = calcProgress(channel, insertsPerOnePercent(items.size))

            for (progress in progressPipeline) {
                single_time.text = getString(R.string.progress_percentage, progress)
            }
        }

        Log.i(tag, "Finished individual insertion execution in $time seconds.")
        return@withContext time
    }

    private suspend fun batchInsertItems(items: List<MediumSizeObject>) {
        Log.i(tag, "Starting batch insertion execution.")
        batch_time.text = getString(R.string.inserting)
        val time = measureTimeSeconds {
            databaseHelper.batchInsert(items)
        }

        batch_time.text = getString(R.string.batch_insert_time, time)
        Log.i(tag, "Finished batch insertion execution in $time seconds.")
    }

    private fun generateAndRun(size: Int) {
        insertionCountText.text = resources.getQuantityString(R.plurals.inserting_medium_objects, size, size)

        batch_time.text = getString(R.string.waiting_to_start)
        single_time.text = getString(R.string.waiting_to_start)
        support_batch.text = getString(R.string.waiting_to_start)
        room_insert.text = getString(R.string.waiting_to_start)

        // clears out potential existing transactions to restart the SQLite batch insertion job
        Log.i(tag, "Cancel $dbInsertJob")
        dbInsertJob.cancel()

        Log.i(tag, "Clearing Database")
        databaseHelper.clearDb()

        dbInsertJob = mainScope.launch {
            val list = createMediumObjects(size)

            val singleTime = insertItems(list)
            single_time.text = getString(R.string.single_insert_Time, singleTime)

            batchInsertItems(list)

            val roomList = createMediumRooms(size)
            clearRoomTables()
            val roomInsertTime = roomInsert(roomList)
            room_insert.text = getString(R.string.room_insert_time, roomInsertTime)

            clearRoomTables()
            val batchTime = batchRoom(roomList)
            support_batch.text = getString(R.string.batch_support_insert_time, batchTime)
        }
    }

    private suspend fun batchRoom(items: List<MediumSizedRoom>) = withContext(Dispatchers.IO){
        Log.i(tag, "Starting batch sql support insertion execution.")
        val writableDatabase = roomDb.openHelper.writableDatabase

        val binderConfig = SupportSQLiteBinderConfig.getInsertConfig(
            db = writableDatabase,
            tableName = "mediumSizedRooms",
            columnCount = 3
        )
        val statement = BatchStatement<MediumSizedRoom>(binderConfig)

        val time = measureTimeSeconds {
                writableDatabase.transaction {
                    statement.execute(items) { mediumSizeObject ->
                        bindLong(mediumSizeObject.id.toLong())
                        bindString(mediumSizeObject.text)
                        bindString(mediumSizeObject.moreText)
                    }
                }

        }

        Log.i(tag, "Finished batch insertion execution in $time seconds.")
        return@withContext time
    }


    private suspend fun roomInsert(items: List<MediumSizedRoom>) = withContext(Dispatchers.IO) {
        Log.i(tag, "Starting individual insertion execution.")
        val dao = roomDb.getMediumSizedDao()
        val time = measureTimeSeconds {
            dao.insertAll(items)
        }

        Log.i(tag, "Finished individual insertion execution in $time seconds.")
        return@withContext time
    }

    private suspend fun clearRoomTables() = withContext(Dispatchers.IO) {
        roomDb.clearAllTables()
    }

    private fun insertsPerOnePercent(totalCount: Int) = if (totalCount <= 100) {
        1
    } else {
        totalCount / 100
    }

    companion object {
        const val tag = "Batch Insert Demo"
    }
}

@ExperimentalCoroutinesApi
fun CoroutineScope.calcProgress(
    insertCountChannel: ReceiveChannel<Int>,
    moduloValue: Int
): ReceiveChannel<Int> = produce {
    for (count in insertCountChannel) {
        if (count.rem(moduloValue) == 0) {
            send(count / moduloValue)
        }
    }
}

inline fun measureTimeSeconds(block: () -> Unit) = measureTimeMillis(block) / 1000.0