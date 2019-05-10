package co.yello.db.batchinsertsample.room

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface MediumSizedRoomDao {
    @Insert
    fun insertAll(mediumSizedRooms: List<MediumSizedRoom>)
}