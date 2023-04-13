package wamboo.example.videocompressor

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WambooApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}