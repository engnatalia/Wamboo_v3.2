package wamboo.eco.videocompressor

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.text.DecimalFormat

// This function returns the file size in human readable format . Like it will take in size and return the size in kb or mb which can be
// displayed to the user .
fun fileSize(size2: Long): String {
    val size = size2.toLong()
    if (size <= 0) return "0"
    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(
        size / Math.pow(
            1024.0, digitGroups.toDouble()
        )
    ) + " " + units[digitGroups]
}

/* function that returns the length of a file specified by a URI.
The function first tries to get the length of the file by opening an asset file descriptor and calling the length method.
If the length can't be obtained this way, the function checks if the scheme of the URI is "content://",
and if so, it tries to get the length by querying the content resolver table.
If either of these methods fails, the function returns -1.  */
fun Uri.length(contentResolver: ContentResolver): Long {

    val assetFileDescriptor = try {
        contentResolver.openAssetFileDescriptor(this, "r")
    } catch (e: Exception) {
        null
    }
    // uses ParcelFileDescriptor#getStatSize underneath if failed
    val length = assetFileDescriptor?.use { it.length } ?: -1L
    if (length != -1L) {
        return length
    }

    // if "content://" uri scheme, try contentResolver table
    if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
        return contentResolver.query(this, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                // maybe shouldn't trust ContentResolver for size: https://stackoverflow.com/questions/48302972/content-resolver-returns-wrong-size
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex == -1) {
                    return@use -1L
                }
                cursor.moveToFirst()
                return try {
                    cursor.getLong(sizeIndex)
                } catch (_: Throwable) {
                    -1L
                }
            } ?: -1L
    } else {
        return -1L
    }
}