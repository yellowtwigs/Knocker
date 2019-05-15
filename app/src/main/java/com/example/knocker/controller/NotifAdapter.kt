package com.example.knocker.controller


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap

import android.view.inputmethod.InputMethodManager
import java.util.ArrayList

import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.telephony.SmsManager
import android.view.*
import android.widget.*
import com.example.knocker.*
import com.example.knocker.model.*

class NotifAdapter(private val context: Context, private val notifications: ArrayList<StatusBarParcelable>, private val windowManager: WindowManager, private val view: View) : BaseAdapter() {
    private val TAG = NotificationListener::class.java.simpleName
    private val notification_listener_ContactsDatabase: ContactsRoomDatabase? = null
    private val notification_listener_mDbWorkerThread: DbWorkerThread? = null
    private val FACEBOOK_PACKAGE = "com.facebook.katana"
    private val MESSENGER_PACKAGE = "com.facebook.orca"
    private val WATHSAPP_SERVICE = "com.whatsapp"
    private val GMAIL_PACKAGE = "com.google.android.gm"
    private val MESSAGE_PACKAGE = "com.google.android.apps.messaging"
    val MESSAGE_SAMSUNG_PACKAGE= "com.samsung.android.messaging"
    override fun getCount(): Int {
        return notifications.size
    }

    override fun getItem(position: Int): StatusBarParcelable {
        return notifications[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView//valeur qui prendra les changement
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.activity_notification_adapter, parent, false)
        }
        System.out.println("notifications taile"+notifications.size)
        val sbp = getItem(position)
        val app = convertView!!.findViewById<View>(R.id.notification_plateformeTv) as TextView
        val contenue = convertView.findViewById<View>(R.id.notification_contenu) as TextView
        val appImg = convertView.findViewById<View>(R.id.notification_plateformeImg) as ImageView
        val expImg = convertView.findViewById<View>(R.id.notification_expediteurImg) as ImageView
        val buttonResponse = convertView.findViewById<View>(R.id.notification_adapter_button_response) as Button
        val buttonSend = convertView.findViewById<View>(R.id.notification_adapter_button_send) as Button
        val editText = convertView.findViewById<View>(R.id.notification_adapter_editText) as EditText

        app.text = convertPackageToString(sbp.appNotifier)
        contenue.text = sbp.statusBarNotificationInfo["android.title"].toString() + ":" + sbp.statusBarNotificationInfo["android.text"]
        //appImg.setImageResource(getApplicationNotifier(sbp));

        val pckg = sbp.appNotifier
        if (sbp.statusBarNotificationInfo["android.icon"] != null) {
            val iconID = Integer.parseInt(sbp.statusBarNotificationInfo["android.icon"]!!.toString())
        }
        try {
            val pckManager = context.packageManager
            val icon = pckManager.getApplicationIcon(sbp.appNotifier)
            appImg.setImageDrawable(icon)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        if (sbp.statusBarNotificationInfo["android.largeIcon"] != "") {//image de l'expediteur provenant l'application source
            println("bitmap :" + sbp.statusBarNotificationInfo["android.largeIcon"]!!)
            val bitmap = sbp.statusBarNotificationInfo["android.largeIcon"] as Bitmap?
            expImg.setImageBitmap(bitmap)
        }
        if(canResponse(sbp.appNotifier)){
            buttonResponse.visibility= View.VISIBLE
        }
        val listener = View.OnClickListener { v ->
            println("click on constraint layout")
            val app = convertPackageToString(sbp.appNotifier)
            if (v.id == R.id.notification_adapter_button_response) {
                println("click on button response")
                buttonResponse.visibility = View.GONE
                buttonSend.visibility = View.VISIBLE
                editText.visibility = View.VISIBLE
                editText.requestFocus()
                val inputMM = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMM.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            } else if (v.id == R.id.notification_adapter_button_send) {
                println("click on button send")
                val message = editText.text.toString()
                // String number= ContactInfo.
                val number= ContactInfo.getInfoWithName(sbp.statusBarNotificationInfo["android.title"].toString(), app)
                if(sbp.appNotifier.equals(MESSAGE_PACKAGE )|| sbp.appNotifier.equals(MESSAGE_SAMSUNG_PACKAGE)){
                    val smsManager= SmsManager.getDefault()
                    smsManager.sendTextMessage(number,null, message,null,null)
                }else if(sbp.appNotifier.equals(WATHSAPP_SERVICE)){
                    /*context.startActivity( Intent(Intent.ACTION_VIEW,
                            Uri.parse(
                                    "https://api.whatsapp.com/send?phone="+number+"&text="+message)));
                    */
                    //println("whatsapp message")
                }
                notifications.remove(sbp)
                val sharedPreferences: SharedPreferences = context.getSharedPreferences("Knocker_preferences", Context.MODE_PRIVATE)
                //val edit = sharedPreferences.edit()
                //val gson=GsonBuilder().serializeNulls().create()
                //var notificationJson = gson.toJson()
                //println("JSON " +notificationJson.toString())
                this.notifyDataSetChanged()
                println("message :"+message + " sendto : "+ number)
                buttonResponse.visibility = View.VISIBLE
                buttonSend.visibility = View.GONE
                editText.visibility = View.GONE
                editText.text=null
                if(this.notifications.size==0){
                    closeNotification()
                }
                //closeNotification()

            } else {

                if (app == "Facebook") {
                    ContactGesture.openMessenger("", context)//TODO modifier si modification pour accès au post fb
                    closeNotification()
                } else if (app == "Messenger") {
                    ContactGesture.openMessenger("", context)
                    closeNotification()
                } else if (app == "WhatsApp") {
                    ContactGesture.openWhatsapp(ContactInfo.getInfoWithName(sbp.statusBarNotificationInfo["android.title"].toString(), app), context)
                    closeNotification()
                } else if (app == "gmail") {
                    ContactGesture.openGmail(context)
                    closeNotification()
                } else if (app == "message") {
                    openSms(sbp)
                    closeNotification()
                }
            }
        }

        contenue.setOnClickListener(listener)
        app.setOnClickListener(listener)
        app.setOnClickListener(listener)
        buttonResponse.setOnClickListener(listener)
        buttonSend.setOnClickListener(listener)
        return convertView
    }

    private fun closeNotification() {
        windowManager.removeView(view)
        val sharedPreferences = context.getSharedPreferences("Knocker_preferences", Context.MODE_PRIVATE)
        val edit = sharedPreferences.edit()
        edit.putBoolean("view", false)
        edit.commit()
    }

    private fun convertPackageToString(packageName: String): String {
        if (packageName == FACEBOOK_PACKAGE) {
            return "Facebook"
        } else if (packageName == MESSENGER_PACKAGE) {
            return "Messenger"
        } else if (packageName == WATHSAPP_SERVICE) {
            return "WhatsApp"
        } else if (packageName == GMAIL_PACKAGE) {
            return "gmail"
        } else if (packageName == MESSAGE_PACKAGE || packageName==


                  MESSAGE_SAMSUNG_PACKAGE) {
            return "message"
        }
        return ""
    }
    private fun canResponse(packageName: String):Boolean{
        if((checkSelfPermission(context,Manifest.permission.SEND_SMS)==PackageManager.PERMISSION_GRANTED)&&(packageName== MESSAGE_PACKAGE || packageName==WATHSAPP_SERVICE|| packageName==MESSAGE_SAMSUNG_PACKAGE )){
            return true
        }
        return false
    }
    private fun getApplicationNotifier(sbp: StatusBarParcelable): Int {

        if ((sbp.appNotifier == FACEBOOK_PACKAGE || sbp.appNotifier == MESSENGER_PACKAGE)) {
            return R.drawable.ic_facebook
        } else if (sbp.appNotifier == GMAIL_PACKAGE) {
            return R.drawable.ic_gmail
        } else if (sbp.appNotifier == WATHSAPP_SERVICE) {
            return R.drawable.ic_whatsapp_circle_menu
        }
        return R.drawable.ic_sms
    }


    /////****** code dupliqué faire attention trouvé un moyen de ne plus en avoir *******//////





    private fun openSms(sbp: StatusBarParcelable) {
        val i: Intent
        if (sbp.appNotifier.equals(MESSAGE_PACKAGE)){
            i = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.messaging")
        }else {
            i = context.packageManager.getLaunchIntentForPackage("com.samsung.android.messaging")
        }
        i.flags = FLAG_ACTIVITY_NEW_TASK
        context.startActivity(i)
    }
    public fun addNotification(sbp: StatusBarParcelable){
        notifications.add(0,sbp)
        this.notifyDataSetChanged()
    }

}
