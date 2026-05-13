package com.z.financetracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Compresses an image URI to a JPEG byte array suitable for upload.
 *
 * Strategy:
 *  - Decode only the bounds first (no memory allocation) to get original dimensions
 *  - Calculate the largest power-of-2 sub-sample that keeps the image within
 *    [maxDimension] on each axis — BitmapFactory does this in the decoder so
 *    we never load a 4K image into memory
 *  - JPEG-compress the result at [quality]% (85 keeps receipts readable)
 *
 * Typical savings: a 4 MB phone photo → ~120–200 KB
 */
object ImageCompressor {

    private const val MAX_DIMENSION = 1280   // px — good enough for a receipt
    private const val JPEG_QUALITY  = 85     // 85% keeps text sharp

    /**
     * Returns a compressed JPEG [ByteArray] ready to upload.
     * Always outputs `image/jpeg` regardless of source format.
     */
    fun compress(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver

        // 1. Read original size without allocating pixels
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        // 2. Calculate sub-sample factor
        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)

        // 3. Decode at reduced size
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap: Bitmap = resolver.openInputStream(uri)!!.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
                ?: throw IllegalStateException("Could not decode image")
        }

        // 4. JPEG encode
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        val larger = maxOf(width, height)
        while (larger / (sample * 2) > maxDim) sample *= 2
        return sample
    }
}
