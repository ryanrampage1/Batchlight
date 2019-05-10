package co.yello.db.batchinsertsample.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MediumSizedRoom::class], version = 1, exportSchema = false)
abstract class RoomDemoDatabase : RoomDatabase() {

    abstract fun getMediumSizedDao(): MediumSizedRoomDao

    companion object {
        const val databaseName = "room_demo.db"

        @Volatile private var instance: RoomDemoDatabase? = null

        fun getInstance(context: Context): RoomDemoDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): RoomDemoDatabase {
            return Room.databaseBuilder(context, RoomDemoDatabase::class.java, databaseName).build()
        }
    }
}
