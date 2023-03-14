package wamboo.example.videocompressor.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ForegroundWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {


    override suspend fun doWork(): Result {
        return Result.success()
    }


    companion object {

        //const val VIDEO_BITRATE = "videoBitrate"
        const val VIDEO_RESOLUTION = "videoResolution"
        const val VideoURI = "videoURI"
        const val SELECTION_TYPE = "type"
    }
}