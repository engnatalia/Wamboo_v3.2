package wamboo.eco.videocompressor.workers

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
        const val VideoURI = "videoURI"
        const val SELECTION_TYPE = "type"
    }
}