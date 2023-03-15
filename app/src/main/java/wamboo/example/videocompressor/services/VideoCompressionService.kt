package wamboo.example.videocompressor.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import wamboo.example.videocompressor.*
import wamboo.example.videocompressor.workers.ForegroundWorker
import java.io.File
import java.math.RoundingMode
import kotlin.math.abs


class VideoCompressionService : Service() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var builder2: NotificationCompat.Builder
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "VideoCompressionChannel"
        const val CHANNEL_NAME = "Video Compression"
    }

    lateinit var pref: SharedPreferences

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoUri = intent?.getStringExtra(ForegroundWorker.VideoURI)
        val selectedtype = intent?.getStringExtra(ForegroundWorker.SELECTION_TYPE)
        val videoResolution =intent?.getStringExtra(ForegroundWorker.VIDEO_RESOLUTION)
        val videoCodec =intent?.getStringExtra(ForegroundWorker.VIDEO_CODEC)
        val compressSpeed =intent?.getStringExtra(ForegroundWorker.COMPRESS_SPEED)
        compressVideo(Uri.parse(videoUri), selectedtype.toString(),videoResolution, videoCodec, compressSpeed)

        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VideoCompress::lock").apply {
                    acquire()
                }
            }

        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            channel.setShowBadge(false)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }

        pref = getSharedPreferences(packageName, Context.MODE_PRIVATE)


        builder2 = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.icono)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        //intent which opens app's launcher activity when user clicks on the notification
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 12, intent, PendingIntent.FLAG_IMMUTABLE
        )
        builder2.setContentIntent(pendingIntent)



        startForeground(NOTIFICATION_ID, builder2.build())
    }


    /* This function compresses a video file. It uses the FFmpeg library to perform the compression.
     The compression format is determined by the selectedtype variable, which can be "H.264", "H.265", or "VP9".
     A progress dialog is displayed while the compression is in progress.
     Before compressing the video, the function creates a file to store the compressed video.
     If the device is running Android Q or higher, the file is created using the Android's content resolver.
     If the device is running an older version of Android, the file is created in the app's root directory.
      The uri of the output file is stored in outPutSafeUri.
     After creating the output file, the FFmpeg command is constructed based on the selectedtype.
     The command is then executed by the FFmpeg library.
     If the compression is successful, the statistics of the compression, such as the initial size,
      conversion time, and compressed size, are displayed in a text view. */

    private fun compressVideo(
        videoUri: Uri,
        selectedtype: String,
        videoResolution: String?,
        videoCodec: String?,
        compressSpeed: String?

    ) {
        val root: String = Environment.getExternalStorageDirectory().toString()
        val appFolder = "$root/GFG/"
        var outPutSafeUri = ""
        var command = ""
        var uriPath: Uri? = null
        val filePrefix = "Compressed"
        val fileExtn = ".mp4"
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val initcapacity: Int = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // With introduction of scoped storage in Android Q the primitive method gives error
            // So, it is recommended to use the below method to create a video file in storage.
            val valuesVideos = ContentValues()
            valuesVideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Wamboo")
            valuesVideos.put(MediaStore.Video.Media.TITLE, filePrefix + System.currentTimeMillis())
            valuesVideos.put(
                MediaStore.Video.Media.DISPLAY_NAME,
                filePrefix + System.currentTimeMillis() + fileExtn
            )

            if (fileExtn == ".webm") {
                valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/webm")
            } else {
                valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            }

            valuesVideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            valuesVideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            val uri = contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                valuesVideos
            )

            if (uri != null) {
                uriPath = uri
            }

            outPutSafeUri = FFmpegKitConfig.getSafParameterForWrite(this, uri)
        } else {
            // This else statement will work for devices with Android version lower than 10
            // Here, "app_folder" is the path to your app's root directory in device storage
            var dest = File(File(appFolder), filePrefix + fileExtn)
            var fileNo = 0
            // check if the file name previously exist. Since we don't want
            // to overwrite the video files
            while (dest.exists()) {
                fileNo++
                dest = File(File(appFolder), filePrefix + fileNo + fileExtn)
            }

            outPutSafeUri =
                FFmpegKitConfig.getSafParameterForWrite(this, dest.toUri())

            // Get the filePath once the file is successfully created.
            uriPath = dest.toUri()
        }
        //get the input video duration
        val mediaInformation = FFprobeKit.getMediaInformation(
            FFmpegKitConfig.getSafParameterForRead(
                this,
                videoUri
            )
        )
        var duration = mediaInformation.mediaInformation.formatProperties.getString("duration")


        when (selectedtype) {
            getString(R.string.ultrafast) -> {
                command = "-y -i ${
                    FFmpegKitConfig.getSafParameterForRead(
                        applicationContext,
                        videoUri
                    )
                } -movflags faststart -c:v libx264 -crf 40 -c:a copy -preset ultrafast $outPutSafeUri"
            }
            getString(R.string.good) -> {
                command = "-y -i ${
                    FFmpegKitConfig.getSafParameterForRead(
                        applicationContext,
                        videoUri
                    )
                } -movflags faststart -c:v libx264 -crf 30 -c:a copy -preset ultrafast $outPutSafeUri"
            }
            getString(R.string.best) -> {
                command = "-y -i ${
                    FFmpegKitConfig.getSafParameterForRead(
                        applicationContext,
                        videoUri
                    )
                } -movflags faststart -c:v libx265 -crf 30 -c:a copy -preset ultrafast $outPutSafeUri"
            }
            getString(R.string.custom_h) -> {
                command = "-y -i ${
                    FFmpegKitConfig.getSafParameterForRead(
                        applicationContext,
                        videoUri
                    )
                } -movflags faststart -c:v $videoCodec -crf 23 -c:a copy -s $videoResolution -preset $compressSpeed $outPutSafeUri"
            }
            else -> {
                command = "-y -i ${
                    FFmpegKitConfig.getSafParameterForRead(
                        applicationContext,
                        videoUri
                    )
                } -movflags faststart -c:v $videoCodec -crf 40 -c:a copy -s $videoResolution -preset $compressSpeed $outPutSafeUri"
            }
        }

        Log.d("MyFFMPEG", command)

        FFmpegKit.executeAsync(command,
            { session ->

                val returnCode = session.returnCode

                //val initialSize = fileSize(videoUri.length(contentResolver))
                val compressedSize = uriPath?.length(contentResolver)
                    ?.let { fileSize(it) }

                if (wakeLock?.isHeld == true)
                    wakeLock?.release()

                stopSelf()

                Log.d("Service", "Service stopped. Compression completed.")
                val current: Int = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                val finalcapacity: Int = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val processingtime = (session.duration / 1000).toDouble()
                val dif=(initcapacity-finalcapacity).toDouble()
                val power = abs(5*(dif/100)*current/1000).toDouble()
                val kwh= power*(processingtime/3600)/1000
                //val pollution = round(kwh*0.519)
                val pollution = kwh*519
                val co2 = pollution.toBigDecimal().setScale(5,RoundingMode.UP).toDouble()
                //send broadcast to HomeFragment that the compression is completed															  
                val intent = Intent(Constants.WORK_COMPLETED_ACTION)
                intent.putExtra(HomeFragment.RETURN_CODE, returnCode.toString())
                intent.putExtra(HomeFragment.URI_PATH, uriPath.toString())
                /*intent.putExtra(HomeFragment.INITIAL_SIZE, initialSize)
                intent.putExtra(HomeFragment.COMPRESS_SZE, compressedSize)
                intent.putExtra(HomeFragment.CONVERSION_TIME, getTime(session.duration / 1000))
                intent.putExtra(HomeFragment.BAT, "$finalcapacity")*/

                pref.edit().apply {
                    putString(HomeFragment.RETURN_CODE, returnCode.toString()).commit()
                    //putString(HomeFragment.INITIAL_SIZE, initialSize).commit()
                    putString(HomeFragment.COMPRESS_SZE, compressedSize).commit()
                    putString(
                        HomeFragment.CONVERSION_TIME,
                        getTime(session.duration / 1000)
                    ).commit()
                    putString(HomeFragment.INITIAL_BATTERY, "$initcapacity%").commit()
                    putString(HomeFragment.REMAINING_BATTERY, "$finalcapacity%").commit()
                    putString(HomeFragment.CO2, co2.toString()).commit()
                }

                Log.d("Service", "Service stopped data .")

                sendBroadcast(intent)
                updateNotificationMessage(returnCode)

            }, {

                //handle FFmpegKit logs
                //Log.d("Logs", it.toString())
                // CALLED WHEN SESSION PRINTS LOGS
            }, {

                var progress = ((it.time.toDouble() / duration.toDouble()) / 10)
                var percentage = progress.toBigDecimal().setScale(0, RoundingMode.CEILING).toInt()
                var msg3 = "$percentage%"

                Log.d("service", "$msg3")

                //builder2.setContentText("$msg3 completed")

                //Update notification information:
                //builder2.setProgress(100, percentage, false);

                builder2.setContentText("$msg3 compressed")

                //updateNotificationMessage()
                notificationManager.notify(1, builder2.build())

                val returnCode = 0
                val intent2 = Intent(Constants.WORK_PROGRESS_ACTION)
                intent2.putExtra(HomeFragment.RETURN_CODE, returnCode.toString())
                intent2.putExtra("percentage", msg3)
                sendBroadcast(intent2)
            }
        )

    }

    private fun updateNotificationMessage(returnCode: ReturnCode) {
        if (ReturnCode.isSuccess(returnCode)) {
            builder2.setContentText(getText(R.string.notification_message_success))
        } else {
            builder2.setContentText(getText(R.string.notification_message_failure))
        }

        builder2.setOngoing(false)
        builder2.setAutoCancel(true)
        notificationManager.notify(1, builder2.build())

    }

    private fun getTime(seconds: Long): String {
        val hr = seconds / 3600
        val rem = seconds % 3600
        val mn = rem / 60
        val sec = rem % 60
        return String.format("%02d", hr) + ":" + String.format(
            "%02d",
            mn
        ) + ":" + String.format("%02d", sec)
    }

}