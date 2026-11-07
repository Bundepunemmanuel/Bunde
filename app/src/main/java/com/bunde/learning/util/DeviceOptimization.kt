package com.bunde.learning.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.bunde.learning.data.model.DevicePerformanceProfile
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

/**
 * Memory and hardware optimization helper designed for budget devices
 * (such as Infinix Smart 8 with 2GB RAM / Android Go edition).
 */
object DeviceOptimization {

    fun getDeviceProfile(context: Context): DevicePerformanceProfile {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalMb = memoryInfo.totalMem / (1024 * 1024)
        val availMb = memoryInfo.availMem / (1024 * 1024)
        val isLowRam = memoryInfo.lowMemory || totalMb <= 2500

        return DevicePerformanceProfile(
            totalMemoryMb = totalMb,
            availableMemoryMb = availMb,
            isLowRamDevice = isLowRam,
            // For 2GB RAM devices, constrain max image dimension to 1024px to prevent OutOfMemoryError
            maxImageUploadDim = if (isLowRam) 1024 else 1600,
            compressionQuality = if (isLowRam) 70 else 85
        )
    }

    /**
     * Efficiently downsamples and compresses an input image stream in-memory without spike allocation.
     */
    fun compressImageForGemini(
        inputStream: InputStream,
        maxDimension: Int = 1024,
        quality: Int = 75
    ): ByteArray {
        val rawBytes = inputStream.readBytes()

        // First decode bounds only
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight

        // Compute sample size
        var inSampleSize = 1
        val maxSide = max(origWidth, origHeight)
        if (maxSide > maxDimension) {
            inSampleSize = (maxSide / maxDimension).coerceAtLeast(1)
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.RGB_565 // Consumes 50% less memory than ARGB_8888
        }

        val decodedBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
            ?: return rawBytes

        val outputStream = ByteArrayOutputStream()
        decodedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        decodedBitmap.recycle()

        return outputStream.toByteArray()
    }

    /**
     * Check network connectivity state for low bandwidth / offline resilience
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
