package dev.steenbakker.mobile_scanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
 */
class YuvToRgbConverter(@Suppress("UNUSED_PARAMETER") context: Context) {

    fun yuvToRgb(image: Image, output: Bitmap) {
        val decoded = yuvToBitmap(image) ?: return
        try {
            Canvas(output).drawBitmap(decoded, 0f, 0f, null)
        } finally {
            decoded.recycle()
        }
    }

    private fun yuvToBitmap(image: Image): Bitmap? {
        val width = image.width
        val height = image.height
        val nv21 = yuvToNv21(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val stream = ByteArrayOutputStream()
        if (!yuvImage.compressToJpeg(Rect(0, 0, width, height), JPEG_QUALITY, stream)) return null
        val jpegBytes = stream.toByteArray()
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    // Packs a YUV_420_888 image into NV21 byte layout, respecting row and pixel strides.
    private fun yuvToNv21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val nv21 = ByteArray(ySize + ySize / 2)

        val yPlane = image.planes[0]
        val yBuf = yPlane.buffer.duplicate()
        val yRowStride = yPlane.rowStride
        if (yRowStride == width) {
            yBuf.get(nv21, 0, ySize)
        } else {
            for (row in 0 until height) {
                yBuf.position(row * yRowStride)
                yBuf.get(nv21, row * width, width)
            }
        }

        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val uBuf = uPlane.buffer.duplicate()
        val vBuf = vPlane.buffer.duplicate()
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
                nv21[idx] = vBuf.get(row * vRowStride + col * vPixelStride)
                nv21[idx + 1] = uBuf.get(row * uRowStride + col * uPixelStride)
            }
        }

        return nv21
    }

    /** No-op. Kept for API compatibility with the previous RenderScript-based implementation. */
    fun release() {}

    companion object {
        private const val JPEG_QUALITY = 90
    }
}
