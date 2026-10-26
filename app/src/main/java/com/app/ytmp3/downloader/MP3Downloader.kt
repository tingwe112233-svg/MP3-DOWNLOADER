package com.app.ytmp3.downloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MP3Downloader(private val context: Context) {

    private val client = OkHttpClient()

    suspend fun downloadAudio(videoId: String, title: String, audioBitrate: Int) {
        withContext(Dispatchers.IO) {
            try {
                val apiUrl = "https://api.cobalt.tools/api/json"
                val jsonPayload = JSONObject().apply {
                    put("url", "https://www.youtube.com/watch?v=$videoId")
                    put("isAudioOnly", true)
                    put("aFormat", "mp3")
                    put("audioBitrate", audioBitrate.toString())
                }

                val body = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseData = response.body()?.string()

                if (responseData != null) {
                    val json = JSONObject(responseData)
                    val downloadUrl = json.optString("url")

                    if (downloadUrl.isNotEmpty()) {
                        startAndroidDownload(downloadUrl, "$title ($audioBitrate kbps).mp3")
                    } else {
                        showToast("Gagal mendapatkan link MP3 dari server.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Error Download: ${e.localizedMessage}")
            }
        }
    }

    private fun startAndroidDownload(url: String, fileName: String) {
        val cleanFileName = fileName.replace("[^a-zA-Z0-9().-]".toRegex(), "_")
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(cleanFileName)
            .setDescription("Mengunduh MP3 berkualitas tinggi...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, cleanFileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        
        showToast("Unduhan dimulai! Cek notifikasi HP.")
    }

    private suspend fun showToast(msg: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }
}
