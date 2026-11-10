package com.bunde.learning.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String = "",
    val latestTag: String = "",
    val downloadUrl: String = "",
    val releaseNotes: String = ""
)

object UpdateManager {

    private const val GITHUB_OWNER = "Bundepunemmanuel"
    private const val GITHUB_REPO = "Bunde"

    /**
     * Checks GitHub Releases API in the background for a newer version than current app version.
     */
    suspend fun checkForUpdate(context: Context): AppUpdateInfo = withContext(Dispatchers.IO) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersionName = packageInfo.versionName ?: "1.0.0"

            val url = URL("https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val tagName = root.optString("tag_name", "")
                val releaseName = root.optString("name", tagName)
                val body = root.optString("body", "")

                // Clean version strings, e.g. "v1.0.4" -> "1.0.4"
                val latestClean = tagName.removePrefix("v").trim()
                val currentClean = currentVersionName.removePrefix("v").trim()

                var apkDownloadUrl = ""
                val assets = root.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            // Prefer bunde.apk, but fall back to any .apk asset
                            val downloadUrl = asset.optString("browser_download_url", "")
                            if (apkDownloadUrl.isEmpty() || name.equals("bunde.apk", ignoreCase = true)) {
                                apkDownloadUrl = downloadUrl
                            }
                        }
                    }
                }

                // Fallback default release URL if assets array is still propagating
                if (apkDownloadUrl.isEmpty() && tagName.isNotEmpty()) {
                    apkDownloadUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/download/$tagName/bunde.apk"
                }

                val isNewer = isVersionNewer(latestClean, currentClean)

                AppUpdateInfo(
                    hasUpdate = isNewer,
                    latestVersionName = tagName.ifEmpty { releaseName },
                    latestTag = tagName,
                    downloadUrl = apkDownloadUrl,
                    releaseNotes = body
                )
            } else {
                AppUpdateInfo(hasUpdate = false)
            }
        } catch (_: Exception) {
            AppUpdateInfo(hasUpdate = false)
        }
    }

    /**
     * Downloads the APK file using Android's system DownloadManager and triggers installation on completion.
     */
    fun startOneTapDownloadAndInstall(context: Context, downloadUrl: String, versionName: String) {
        try {
            val fileName = "bunde-$versionName.apk"
            val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Bunde Update $versionName")
                setDescription("Downloading latest Bunde APK...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(destinationFile))
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                    if (id == downloadId) {
                        try {
                            recvContext?.unregisterReceiver(this)
                        } catch (_: Exception) {}

                        triggerApkInstall(context, destinationFile)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (_: Exception) {
            // If DownloadManager fails, open downloadUrl directly in browser
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Triggers Android's package installer intent using FileProvider.
     */
    fun triggerApkInstall(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (_: Exception) {
            // Fallback: open via ACTION_INSTALL_PACKAGE or generic VIEW
            try {
                val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
                val fallbackIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    data = apkUri
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        if (latest.isBlank() || current.isBlank()) return false
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
