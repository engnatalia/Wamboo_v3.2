package wamboo.eco.videocompressor.workers

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import wamboo.eco.videocompressor.services.VideoCompressionService


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