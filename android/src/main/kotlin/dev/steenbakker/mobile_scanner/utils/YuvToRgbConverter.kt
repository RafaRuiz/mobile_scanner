package dev.steenbakker.mobile_scanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream

/**
 * Converts a YUV_420_888 [Image] into an RGB [Bitmap].
 *
 * Uses [YuvImage] + JPEG + [BitmapFactory] — all stable `android.graphics` APIs.
 * Replaces the previous `android.renderscript` implementation, which was
 * deprecated in Android 12 and produced uniform-colour output on recent
 * arm64 emulators.
 *
 * Instances hold pooled buffers across calls (NV21 byte array, JPEG
 * [ByteArrayOutputStream]) and decode JPEG directly into the caller's output
 * [Bitmap] via `BitmapFactory.Options.inBitmap` — no intermediate bitmap. This
 * makes the class cheap to call repeatedly but NOT thread-safe; share one
 * instance per analyzer thread.
 */
class YuvToRgbConverter(@Suppress("UNUSED_PARAMETER") context: Context) {

    private var nv21: ByteArray = ByteArray(0)
    private val jpegStream = ByteArrayOutputStream()
    private val decodeOptions = BitmapFactory.Options().apply {
        inMutable = true
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    fun yuvToRgb(image: Image, output: Bitmap) {
        val width = image.width
        val height = image.height

        val required = width * height * 3 / 2
        if (nv21.size < required) nv21 = ByteArray(required)
        packNv21(image, nv21)

        jpegStream.reset()
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        if (!yuvImage.compressToJpeg(Rect(0, 0, width, height), JPEG_QUALITY, jpegStream)) return
        val jpegBytes = jpegStream.toByteArray()

        // Ask BitmapFactory to decode directly into `output`. Matching dims +
        // ARGB_8888 + mutable is enough for API 19+ to reuse the allocation.
        decodeOptions.inBitmap = output
        val decoded = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, decodeOptions)
        decodeOptions.inBitmap = null

        if (decoded != null && decoded !== output) {
            // Reuse wasn't honoured (shouldn't happen with matching dims, but
            // handle defensively): copy pixels in and drop the stray bitmap.
            val w = decoded.width
            val h = decoded.height
            val pixels = IntArray(w * h)
            decoded.getPixels(pixels, 0, w, 0, 0, w, h)
            output.setPixels(pixels, 0, output.width, 0, 0, w, h)
            decoded.recycle()
        }
    }

    private fun packNv21(image: Image, dst: ByteArray) {
        val width = image.width
        val height = image.height
        val ySize = width * height

        val yPlane = image.planes[0]
        val yBuf = yPlane.buffer.duplicate().apply { rewind() }
        val yRowStride = yPlane.rowStride
        if (yRowStride == width) {
            yBuf.get(dst, 0, ySize)
        } else {
            for (row in 0 until height) {
                yBuf.position(row * yRowStride)
                yBuf.get(dst, row * width, width)
            }
        }

        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val uBuf = uPlane.buffer.duplicate().apply { rewind() }
        val vBuf = vPlane.buffer.duplicate().apply { rewind() }
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride
        val halfW = width / 2
        val halfH = height / 2

        for (row in 0 until halfH) {
            val dstRow = ySize + row * width
            for (col in 0 until halfW) {
                val idx = dstRow + col * 2
                dst[idx] = vBuf.get(row * vRowStride + col * vPixelStride)
                dst[idx + 1] = uBuf.get(row * uRowStride + col * uPixelStride)
            }
        }
    }

    /** No-op. Kept for API compatibility with the previous RenderScript-based implementation. */
    fun release() {}

    companion object {
        private const val JPEG_QUALITY = 90
    }
}
