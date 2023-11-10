package com.yellowtwigs.knockin.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.Telephony
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.text.Normalizer

object Converter {

    fun converter06To33(phoneNumber: String): String {
        return if (phoneNumber[0] == '0') {
            "+33$phoneNumber"
        } else phoneNumber
    }

    fun converter33To06(phoneNumber: String): String {
        val cleanPhoneNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        if (cleanPhoneNumber.startsWith("33")) {
            return "0" + cleanPhoneNumber.substring(2)
        }
        return cleanPhoneNumber
    }

    fun CharSequence.unAccent(): String {
        val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(temp, "")
    }

    //region ============================================ BITMAP ============================================

    fun base64ToBitmap(base64: String): Bitmap {
        val decodedString = Base64.decode(base64, Base64.DEFAULT)
        val options = BitmapFactory.Options()
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size, options)
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()

        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    //endregion

    //region ========================================= CONVERT TIME =========================================

    fun convertTimeToHour(time: String): String {
        return if (time.contains("h")) {
            val parts = time.split("h").toTypedArray()
            parts[0]
        } else {
            "0"
        }
    }

    fun convertTimeToMinutes(time: String): String {
        val array = arrayListOf<Char>()
        return if (time.contains("h")) {
            val parts = time.split("h").toTypedArray()
            parts[1].forEach {
                if (!it.isWhitespace()) {
                    array.add(it)
                }
            }
            String(array.toCharArray())
        } else {
            "0"
        }
    }

    fun convertTimeToStartTime(time: String): String {
        return if (time.contains("to")) {
            val parts = time.split(" to").toTypedArray()
            parts[0]
        } else {
            val customTime = "0h0 to 23h59"
            val parts = customTime.split(" to").toTypedArray()
            parts[0]
        }
    }

    fun convertTimeToEndTime(time: String): String {
        return if (time.contains("to")) {
            val parts = time.split("to ").toTypedArray()
            parts[1]
        } else {
            val customTime = "0h0 to 23h59"
            val parts = customTime.split("to ").toTypedArray()
            parts[1]
        }
    }

    //endregion
}