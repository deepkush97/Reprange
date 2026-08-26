package com.deepkush.reprange

import android.app.Application
import coil3.ImageLoader
import coil3.request.crossfade
import com.deepkush.reprange.utils.AppHaptics
import com.deepkush.reprange.utils.PrefCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ReprangeApp : Application(), coil3.SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        PrefCache.start(this)
        AppHaptics.init(this)
    }

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(true)
            .build()
}
