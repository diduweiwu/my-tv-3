package com.lizongying.mytv0

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Hashtable

object QrCodeUtil {

    private const val DEFAULT_SIZE = 256

    fun createQRCodeBitmap(
        content: String,
        characterSet: String = "UTF-8",
        errorCorrection: String = "L",
        margin: String = "1",
        @ColorInt colorBlack: Int = Color.BLACK,
        @ColorInt colorWhite: Int = Color.WHITE,
    ): Bitmap? {
        try {
            val hints: Hashtable<EncodeHintType, String> = Hashtable()
            if (characterSet.isNotEmpty()) {
                hints[EncodeHintType.CHARACTER_SET] = characterSet
            }
            if (errorCorrection.isNotEmpty()) {
                hints[EncodeHintType.ERROR_CORRECTION] = errorCorrection
            }
            if (margin.isNotEmpty()) {
                hints[EncodeHintType.MARGIN] = margin
            }

            // Generate a small matrix for better performance
            val bitMatrix =
                QRCodeWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    DEFAULT_SIZE,
                    DEFAULT_SIZE,
                    hints
                )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) colorBlack else colorWhite
                }
            }

            // Use RGB_565 to save memory and processing time as alpha is not needed for QR codes
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
        }
        return null
    }
}