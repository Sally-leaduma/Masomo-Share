package com.example.masomoshare254

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

object NetworkUtils {
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/** Local storage: app-private INTERNAL storage, one sub-folder per user. No permission needed. */
object FileStorage {
    fun fileFor(context: Context, userId: String, resourceId: String): File {
        val dir = File(context.filesDir, "downloads/$userId").apply { mkdirs() }
        return File(dir, "$resourceId.pdf")
    }
    fun isValid(file: File) = file.exists() && file.length() > 0
}

sealed class FileResult {
    data class Success(val file: File) : FileResult()
    data class Failure(val message: String) : FileResult()
}

class FileDownloader {

    /**
     * Downloads [url] into [target].
     * - gs:// and firebasestorage.googleapis.com links -> Firebase Storage SDK
     * - any other https link -> plain HTTP
     * Both write to a .part file first, so a failed/cancelled download never leaves a broken PDF.
     * Cancelling the calling coroutine cancels the download.
     */
    suspend fun download(url: String, target: File, onProgress: (Int) -> Unit = {}): FileResult =
        if (url.startsWith("gs://") || url.contains("firebasestorage.googleapis.com")) {
            downloadFromFirebase(url, target, onProgress)
        } else {
            downloadOverHttp(url, target, onProgress)
        }

    // ---------- Firebase Storage ----------
    private suspend fun downloadFromFirebase(url: String, target: File, onProgress: (Int) -> Unit): FileResult {
        val temp = File(target.parentFile, target.name + ".part")
        target.parentFile?.mkdirs()
        val ref = try {
            FirebaseStorage.getInstance().getReferenceFromUrl(url)
        } catch (e: Exception) {
            return FileResult.Failure("Invalid file link.")
        }
        return suspendCancellableCoroutine { cont ->
            val task = ref.getFile(temp)
            task.addOnProgressListener { snap ->
                val total = snap.totalByteCount
                if (total > 0) onProgress((snap.bytesTransferred * 100 / total).toInt())
            }.addOnSuccessListener {
                if (target.exists()) target.delete()
                if (temp.renameTo(target)) cont.resume(FileResult.Success(target))
                else { temp.delete(); cont.resume(FileResult.Failure("Could not save file")) }
            }.addOnFailureListener { e ->
                temp.delete()
                cont.resume(FileResult.Failure(e.message ?: "Download failed. Check your connection."))
            }
            cont.invokeOnCancellation { task.cancel(); temp.delete() }
        }
    }

    // ---------- Plain HTTP ----------
    private suspend fun downloadOverHttp(url: String, target: File, onProgress: (Int) -> Unit): FileResult =
        withContext(Dispatchers.IO) {
            val temp = File(target.parentFile, target.name + ".part")
            var conn: HttpURLConnection? = null
            try {
                target.parentFile?.mkdirs()
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 15_000
                }
                conn.connect()
                if (conn.responseCode !in 200..299) {
                    return@withContext FileResult.Failure("Server error (${conn.responseCode})")
                }
                val total = conn.contentLengthLong
                var downloaded = 0L
                var lastPercent = -1
                conn.inputStream.use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()      // cancellation point
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                val percent = (downloaded * 100 / total).toInt()
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    withContext(Dispatchers.Main) { onProgress(percent) }
                                }
                            }
                        }
                    }
                }
                if (target.exists()) target.delete()
                if (!temp.renameTo(target)) return@withContext FileResult.Failure("Could not save file")
                FileResult.Success(target)
            } catch (e: CancellationException) {
                temp.delete(); throw e
            } catch (e: Exception) {
                temp.delete()
                FileResult.Failure(e.message ?: "Download failed. Check your connection.")
            } finally {
                conn?.disconnect()
            }
        }
}
