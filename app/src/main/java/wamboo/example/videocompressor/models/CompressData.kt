package wamboo.example.videocompressor.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "compression")
class CompressData(
    val sizeReduction: Long,
    val co2: Int,
    val milliSeconds: Long,
    val date: String,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

data class CompressChartView(val date: String, val compressDataList: List<CompressData>)

data class FinalChartData(val value: Double, var date: String)