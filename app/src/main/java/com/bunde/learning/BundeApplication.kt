package com.bunde.learning

import android.app.Application
import android.util.Log
import com.bunde.learning.data.local.LocalStorageManager
import com.bunde.learning.data.local.UserPreferencesRepository
import com.bunde.learning.domain.ai.GeminiService
import com.bunde.learning.domain.ai.GeminiServiceImpl
import com.bunde.learning.util.DeviceOptimization

class BundeApplication : Application() {

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var localStorage: LocalStorageManager
        private set

    lateinit var geminiService: GeminiService
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = UserPreferencesRepository(applicationContext)
        localStorage = LocalStorageManager(applicationContext)
        geminiService = GeminiServiceImpl()

        val devProfile = DeviceOptimization.getDeviceProfile(applicationContext)
        Log.i(
            "BundeApp",
            "Bunde initialized. Total RAM: ${devProfile.totalMemoryMb}MB, Low-RAM device: ${devProfile.isLowRamDevice}"
        )
    }
}
