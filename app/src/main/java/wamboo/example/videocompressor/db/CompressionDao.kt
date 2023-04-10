package wamboo.example.videocompressor.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import wamboo.example.videocompressor.models.CompressData

@Dao
interface CompressionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(compressionDetails: CompressData)

    @Query("SELECT * FROM compression WHERE milliseconds BETWEEN :startDate AND :endDate")
    fun getAllCompressionByDates(startDate: Long, endDate: Long): LiveData<List<CompressData>>

    @Query("SELECT * FROM compression")
    fun getAllCompression(): LiveData<List<CompressData>>

    @Query("DELETE FROM compression")
    suspend fun deleteAll()

    @Query("DELETE FROM compression WHERE milliseconds BETWEEN :startDate AND :endDate")
    suspend fun deleteSpec(startDate: Long, endDate: Long)

}