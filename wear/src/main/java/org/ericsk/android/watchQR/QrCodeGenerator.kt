package org.ericsk.android.watchQR

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QrCodeGenerator {
    /**
     * Generates a code Bitmap (QR or Barcode) from the given text with the specified size,
     * stripping all padding from 1D Barcodes.
     */
    fun generateCode(text: String, format: BarcodeFormat, width: Int, height: Int): Bitmap? {
        if (text.isEmpty()) return null
        return try {
            val hints = mapOf(EncodeHintType.MARGIN to 0)
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                format,
                width,
                height,
                hints
            )
            val w = bitMatrix.width
            val h = bitMatrix.height

            // Find horizontal boundaries of 1D barcode to strip all margins
            var firstBlack = -1
            var lastBlack = -1
            if (format != BarcodeFormat.QR_CODE) {
                for (x in 0 until w) {
                    var hasBlack = false
                    for (y in 0 until h) {
                        if (bitMatrix.get(x, y)) {
                            hasBlack = true
                            break
                        }
                    }
                    if (hasBlack) {
                        if (firstBlack == -1) firstBlack = x
                        lastBlack = x
                    }
                }
            }

            val startX = if (firstBlack != -1) firstBlack else 0
            val croppedWidth = if (firstBlack != -1 && lastBlack != -1) lastBlack - firstBlack + 1 else w

            val pixels = IntArray(croppedWidth * h)
            for (y in 0 until h) {
                val offset = y * croppedWidth
                for (x in 0 until croppedWidth) {
                    pixels[offset + x] = if (bitMatrix.get(startX + x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(croppedWidth, h, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, croppedWidth, 0, 0, croppedWidth, h)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
