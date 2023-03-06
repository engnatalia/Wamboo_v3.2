package wamboo.example.videocompressor.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import wamboo.example.videocompressor.Constants
import wamboo.example.videocompressor.FileUtils
import wamboo.example.videocompressor.R
import wamboo.example.videocompressor.services.VideoCompressionService
import java.io.File
import java.io.FileNotFoundException
import java.text.DecimalFormat


class VideoCompressionWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val videoUrl = Uri.parse(inputData.getString(ForegroundWorker.VideoURI))
        val selectedMethod = Uri.parse(inputData.getString(ForegroundWorker.SELECTION_TYPE))
        val serviceIntent = Intent(context, VideoCompressionService::class.java)
        serviceIntent.putExtra(ForegroundWorker.VideoURI, videoUrl.toString())
        serviceIntent.putExtra(ForegroundWorker.SELECTION_TYPE, selectedMethod.toString())
        context.startForegroundService(serviceIntent)
        return Result.success()
    }

}