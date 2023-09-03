/**
 * Copyright (c) 2023 Natalia Molinero Mingorance
 * All rights reserved.
 */

package wamboo.example.videocompressor.repository


import androidx.lifecycle.LiveData
import wamboo.example.videocompressor.db.CompressionDao
import wamboo.example.videocompressor.models.CompressData

class CompressRepository(val dao: CompressionDao) {

    suspend fun insert(comp: CompressData) {
        dao.insert(comp)
    }

    suspend fun delete() {
        dao.deleteAll()
    }

    fun getAllUserData(startDate : Long,endDate : Long) = dao.getAllCompressionByDates(startDate,endDate)

    suspend fun deleteSpecific(start: Long, end: Long) {
        dao.deleteSpec(start,end)
    }

    fun getAllCompression() : LiveData<List<CompressData>> {
        return dao.getAllCompression()
    }
}