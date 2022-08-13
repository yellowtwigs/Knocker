package com.yellowtwigs.knockin.utils

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yellowtwigs.knockin.R
import com.yellowtwigs.knockin.utils.Converter.converter06To33
import java.sql.DriverManager
import java.util.*

/**
 * L'objet qui permet d'ouvrir messenger, whatsapp et gmail
 * @author Florian Striebel, Kenzy Suon
 */
object ContactGesture {

    //region =========================================== WHATSAPP ===========================================

    fun isWhatsappInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return try {
            pm.getApplicationInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openWhatsapp(contact: CharSequence, context: Context) {
        val url = "https://api.whatsapp.com/send?phone=$contact"

        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        context.startActivity(i)
    }

    fun openWhatsapp(context: Context) {
        val i = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
        try {
            context.startActivity(i)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://whatsapp.com/")
                )
            )
        }
    }

    fun sendMessageWithWhatsapp(phoneNumber: String, msg: String, activity: Activity) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val message = "phone=" + converter06To33(phoneNumber)
        intent.data = Uri.parse("http://api.whatsapp.com/send?phone=$message&text=$msg")

        activity.startActivity(intent)
    }

    //endregion

    //region =========================================== TELEGRAM ===========================================

    fun isTelegramInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return try {
            pm.getApplicationInfo("org.telegram.messenger", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun goToTelegram(context: Context, phoneNumber: String) {
        val appIntent = if (phoneNumber == "") {
            Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve"))
        } else {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "https://t.me/${
                        converter06To33(phoneNumber).replace(
                            "\\s".toRegex(),
                            ""
                        )
                    }"
                )
            )
        }
        try {
            appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            appIntent.setPackage("org.telegram.messenger")
            context.startActivity(appIntent)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://web.telegram.org/")
                )
            )
        }
    }

    fun goToTelegram(context: Context) {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve"))
        try {
            appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            appIntent.putExtra("", "")
            appIntent.setPackage("org.telegram.messenger")
            context.startActivity(appIntent)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://web.telegram.org/")
                )
            )
        }
    }

    //endregion

    //region ============================================ SIGNAL ============================================

    fun isSignalInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return try {
            pm.getApplicationInfo("org.thoughtcrime.securesms", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun goToSignal(context: Context) {
        val appIntent =
            context.packageManager.getLaunchIntentForPackage("org.thoughtcrime.securesms")
        try {
            context.startActivity(appIntent)
        } catch (e: ActivityNotFoundException) {
            Log.i("resolveInfoList", "$e")
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://signal.org/")
                )
            )
        }
    }

    //endregion

    //region ============================================ PHONE =============================================

    fun openSms(phoneNumber: String, context: Activity) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", phoneNumber, null))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        context.startActivity(intent)
//        context.finish()
    }

    fun callPhone(phoneNumber: String, context: Context) {
        var numberForPermission = ""

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val PERMISSION_CALL_RESULT = 1
            ActivityCompat.requestPermissions(
                (context as Activity),
                arrayOf(Manifest.permission.CALL_PHONE),
                PERMISSION_CALL_RESULT
            )
        } else {
            val sharedPreferences = context.getSharedPreferences("Phone_call", Context.MODE_PRIVATE)
            val popup = sharedPreferences.getBoolean("popup", true)
            if (popup && numberForPermission.isEmpty()) {
                MaterialAlertDialogBuilder(context, R.style.AlertDialog)
                    .setTitle(R.string.main_contact_grid_title)
                    .setMessage(R.string.main_contact_grid_message)
                    .setPositiveButton(android.R.string.yes) { dialog: DialogInterface?, id: Int ->
                        context.startActivity(
                            Intent(Intent.ACTION_CALL, Uri.fromParts("tel", phoneNumber, null))
                        )
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()
            } else {
                context.startActivity(
                    Intent(
                        Intent.ACTION_CALL,
                        Uri.fromParts("tel", phoneNumber, null)
                    )
                )
            }
        }
    }

    //endregion

    //region ========================================== MESSENGER ===========================================

    fun isMessengerInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return try {
            pm.getApplicationInfo("com.facebook.orca", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openMessenger(id: String, context: Context) {
        try {
            val intent = if (id == "") {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.messenger.com/t/"))
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.messenger.com/t/$id"))
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.messenger.com/t/$id"))
            context.startActivity(intent)
        }
    }


    //endregion

    fun goToOutlook(context: Context) {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("ms-outlook://emails"))
        try {
            context.startActivity(appIntent)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://outlook.com/")
                )
            )
        }
    }

    fun openMailApp(mail: String, context: Context){
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(mail))
        intent.putExtra(Intent.EXTRA_SUBJECT, "")
        intent.putExtra(Intent.EXTRA_TEXT, "")
        DriverManager.println(
            "intent " + Objects.requireNonNull(intent.extras).toString()
        )
        context.startActivity(intent)
    }
}