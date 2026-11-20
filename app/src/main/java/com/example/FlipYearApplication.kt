package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.data.FlipYearImageLoaders

class FlipYearApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return FlipYearImageLoaders.getMediaImageLoader(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        FlipYearImageLoaders.onTrimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        FlipYearImageLoaders.clearMemoryCaches()
    }
}
