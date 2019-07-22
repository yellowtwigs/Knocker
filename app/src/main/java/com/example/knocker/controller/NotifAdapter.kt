package com.example.knocker.controller


import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.DrawableCompat
import com.example.knocker.R
import com.example.knocker.model.ContactList
import com.example.knocker.model.ContactsRoomDatabase
import com.example.knocker.model.DbWorkerThread
import com.example.knocker.model.StatusBarParcelable
import java.util.*


/**
 * La Classe qui permet d'afficher les notifications prioritaires au milieu de l'écran
 * @author Florian Striebel, Kenzy Suon, Ryan Granet
 */
class NotifAdapter(private val context: Context, private val notifications: ArrayList<StatusBarParcelable>, private val windowManager: WindowManager, private val view: View) : BaseAdapter() {

    private val TAG = NotificationListener::class.java.simpleName
    private lateinit var notification_adapter_mDbWorkerThread: DbWorkerThread
    private val FACEBOOK_PACKAGE = "com.facebook.katana"
    private val MESSENGER_PACKAGE = "com.facebook.orca"
    private val WHATSAPP_SERVICE = "com.whatsapp"
    private val GMAIL_PACKAGE = "com.google.android.gm"
    private val MESSAGE_PACKAGE = "com.google.android.apps.messaging"
    private val MESSAGE_SAMSUNG_PACKAGE = "com.samsung.android.messaging"

    override fun getCount(): Int {
        return notifications.size
    }

    override fun getItem(position: Int): StatusBarParcelable {
        return notifications[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        notification_adapter_mDbWorkerThread = DbWorkerThread("dbWorkerThread")
        notification_adapter_mDbWorkerThread.start()


        var view = convertView//valeur qui prendra les changement
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_notification_adapter, parent, false)
        }

        val sbp = getItem(position)

        val gestionnaireContacts = ContactList(this.context)
        val contact = gestionnaireContacts.getContact(sbp.statusBarNotificationInfo["android.title"].toString())


        val app = view!!.findViewById<View>(R.id.notification_adapter_platform) as TextView
        val layout = view.findViewById<View>(R.id.notification_adapter_layout) as ConstraintLayout
        val content = view.findViewById<View>(R.id.notification_adapter_content) as TextView
        val appImg = view.findViewById<View>(R.id.notification_adapter_plateforme_img) as ImageView
        val senderImg = view.findViewById<View>(R.id.notification_adapter_sender_img) as ImageView
        val buttonSend = view.findViewById<View>(R.id.notification_adapter_send) as AppCompatImageView
        val editText = view.findViewById<View>(R.id.notification_adapter_message_to_send) as EditText

        val unwrappedDrawable = AppCompatResources.getDrawable(context, R.drawable.custom_shape_top_bar_notif_adapter)
        val wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable!!)

        app.text = convertPackageToString(sbp.appNotifier!!)
        content.text = sbp.statusBarNotificationInfo["android.title"].toString() + ":" + sbp.statusBarNotificationInfo["android.text"]
        //appImg.setImageResource(getApplicationNotifier(sbp));

        content.setOnClickListener {
            when (app.text) {
                "Facebook" -> {
                    val uri = Uri.parse("facebook:/newsfeed")
                    val likeIng = Intent(Intent.ACTION_VIEW, uri)
                    try {
                        context.startActivity(likeIng)
                    } catch (e: ActivityNotFoundException) {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://facebook.com/")))
                    }
                    closeNotification()
                }
                "Messenger" -> {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.messenger.com/t/"))
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.messenger.com/t/"))
                        context.startActivity(intent)
                    }
                    closeNotification()
                }
                "WhatsApp" -> {
                    openWhatsapp(contact!!.getFirstPhoneNumber())
                }
                "Gmail" -> {
                    val appIntent = Intent(Intent.ACTION_VIEW);
                    appIntent.setClassName("com.google.android.gm", "com.google.android.gm.ConversationListActivityGmail");
                    try {
                        context.startActivity(appIntent)
                    } catch (e: ActivityNotFoundException) {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://gmail.com/")))
                    }
                    closeNotification()
                }
                "Message" -> {
                    openSms(contact!!.getFirstPhoneNumber())
                    closeNotification()
                }
            }
        }

        when (convertPackageToString(sbp.appNotifier!!)) {
            "Facebook" -> {
                DrawableCompat.setTint(wrappedDrawable, context.resources.getColor(R.color.custom_shape_top_bar_notif_adapter_facebook))
            }
            "Messenger" -> {
                DrawableCompat.setTint(wrappedDrawable, context.resources.getColor(R.color.custom_shape_top_bar_notif_adapter_messenger))
            }
            "WhatsApp" -> {
                DrawableCompat.setTint(wrappedDrawable, context.resources.getColor(R.color.custom_shape_top_bar_notif_adapter_whatsapp))
            }
            "Gmail" -> {
                DrawableCompat.setTint(wrappedDrawable, context.resources.getColor(R.color.custom_shape_top_bar_notif_adapter_gmail))
            }
            "Message" -> {
                DrawableCompat.setTint(wrappedDrawable, context.resources.getColor(R.color.colorPrimary))
            }
        }

        buttonSend.setOnClickListener {
            if (editText.text.toString() == "") {
                Toast.makeText(context, "Votre message ne doit pas être vide", Toast.LENGTH_SHORT).show()
            } else {

                when (convertPackageToString(sbp.appNotifier!!)) {
                    "WhatsApp" -> {
                        sendMessageWithWhatsapp(contact!!.getFirstPhoneNumber(), editText.text.toString())
                        closeNotification()
                    }
                    "Gmail" -> {
                    }
                    "Message" -> {
                        sendMessageWithAndroidMessage(contact!!.getFirstPhoneNumber(), editText.text.toString())
                        closeNotification()
                    }
                }
            }
        }

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
            senderImg.setImageBitmap(bitmap)
        }

//        val listener = View.OnClickListener { v ->
//            println("click on constraint layout")
//            val appName = convertPackageToString(sbp.appNotifier)
//            when (appName) {
//                "Facebook" -> {
//                    ContactGesture.openMessenger("", context)//TODO modifier si modification pour accès au post fb
//                    closeNotification()
//                }
//                "Messenger" -> {
//                    ContactGesture.openMessenger("", context)
//                    closeNotification()
//                }
//                "WhatsApp" -> {
//                    notification_adapeter_ContactsDatabase = ContactsRoomDatabase.getDatabase(context)
//                    closeNotification()
//                }
//                "Gmail" -> {
//                    ContactGesture.openGmail(context)
//                    closeNotification()
//                }
//                "Message" -> {
//                    openSms(sbp)
//                    closeNotification()
//                }
//            }
//        }

//        content.setOnClickListener(listener)
//        app.setOnClickListener(listener)
//        app.setOnClickListener(listener)

//        buttonSend.setOnClickListener {
//            val msg = editText.text.toString()
//            val phoneNumb = compose_message_PhoneNumberEditText!!.text.toString()
//
//            if (!TextUtils.isEmpty(msg) && !TextUtils.isEmpty(phoneNumb)) {
//                if (checkPermission(Manifest.permission.SEND_SMS)) {
//                    val smsManager = SmsManager.getDefault()
//                    smsManager.sendTextMessage(phoneNumb, null, msg, null, null)
//
//                    val message = Message(msg, true, "", 0, currentDate, currentHour)
//
//                    compose_message_listOfMessage.add(message)
//
//                    compose_message_ListViewMessage!!.adapter = MessageListAdapter(this, compose_message_listOfMessage)
//
//                    compose_message_MessageEditText!!.text.clear()
//                    Toast.makeText(this, "Message envoyé", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(this@ComposeMessageActivity, "Permission denied", Toast.LENGTH_SHORT).show()
//                }
//            } else {
//                Toast.makeText(this@ComposeMessageActivity, "Enter a message and a phone number", Toast.LENGTH_SHORT).show()
//            }
//        }

        return view
    }

    private fun openSms(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", phoneNumber, null))
        intent.flags = FLAG_ACTIVITY_NEW_TASK

        context.startActivity(intent)
    }

    private fun sendMessageWithWhatsapp(phoneNumber: String, msg: String) {

        val intent = Intent(Intent.ACTION_VIEW)
        val message = "phone=" + converter06To33(phoneNumber)
        intent.data = Uri.parse("http://api.whatsapp.com/send?phone=$message&text=$msg")

        context.startActivity(intent)
    }

    private fun openWhatsapp(phoneNumber: String) {

        val intent = Intent(Intent.ACTION_VIEW)
        val message = "phone=" + converter06To33(phoneNumber)
        intent.data = Uri.parse("http://api.whatsapp.com/send?phone=$message")

        context.startActivity(intent)
    }

    private fun sendMessageWithAndroidMessage(phoneNumber: String, msg: String) {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(phoneNumber, null, msg, null, null)

        Toast.makeText(context, "Message Sent",
                Toast.LENGTH_LONG).show()
    }

    private fun converter06To33(phoneNumber: String): String {
        return if (phoneNumber[0].toString() == "0") {
            val phoneNumberConvert = "+33" + phoneNumber.substring(0)
            phoneNumberConvert
        } else {
            phoneNumber
        }
    }

    private fun closeNotification() {
        windowManager.removeView(view)
        val sharedPreferences = context.getSharedPreferences("Knocker_preferences", Context.MODE_PRIVATE)
        val edit = sharedPreferences.edit()
        edit.putBoolean("view", false)
        edit.apply()
    }

    private fun convertPackageToString(packageName: String): String {
        if (packageName == FACEBOOK_PACKAGE) {
            return "Facebook"
        } else if (packageName == MESSENGER_PACKAGE) {
            return "Messenger"
        } else if (packageName == WHATSAPP_SERVICE) {
            return "WhatsApp"
        } else if (packageName == GMAIL_PACKAGE) {
            return "Gmail"
        } else if (packageName == MESSAGE_PACKAGE || packageName ==


                MESSAGE_SAMSUNG_PACKAGE) {
            return "Message"
        }
        return ""
    }

//    private fun canResponse(packageName: String): Boolean {
//        if ((checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) && (packageName == MESSAGE_PACKAGE || packageName == WHATSAPP_SERVICE || packageName == MESSAGE_SAMSUNG_PACKAGE)) {
//            return true
//        }
//        return false
//    }

    private fun getApplicationNotifier(sbp: StatusBarParcelable): Int {

        if ((sbp.appNotifier == FACEBOOK_PACKAGE || sbp.appNotifier == MESSENGER_PACKAGE)) {
            return R.drawable.ic_facebook
        } else if (sbp.appNotifier == GMAIL_PACKAGE) {
            return R.drawable.ic_gmail
        } else if (sbp.appNotifier == WHATSAPP_SERVICE) {
            return R.drawable.ic_whatsapp_circle_menu
        }
        return R.drawable.ic_sms_selector
    }


    /////****** code dupliqué faire attention trouvé un moyen de ne plus en avoir *******//////

    fun addNotification(sbp: StatusBarParcelable) {
        notifications.add(0, sbp)
        this.notifyDataSetChanged()
    }

//    private fun getContactNameFromString(NameFromSbp: String): String {
//        val pregMatchString: String = ".*\\([0-9]*\\)"
//        if (NameFromSbp.matches(pregMatchString.toRegex())) {
//            return NameFromSbp.substring(0, TextUtils.lastIndexOf(NameFromSbp, '(')).dropLast(1)
//        } else {
//            println("pregmatch fail$NameFromSbp")
//            return NameFromSbp
//        }
//    }

//    fun getContact(name: String, listContact: List<ContactDB>?): ContactDB? {
//
//        if (name.contains(" ")) {
//            listContact!!.forEach { dbContact ->
//
//                //                println("contact "+dbContact+ "différent de name"+name)
//                if (dbContact.firstName + " " + dbContact.lastName == name) {
//                    return dbContact
//                }
//            }
//        } else {
//            listContact!!.forEach { dbContact ->
//                if (dbContact.firstName == name && dbContact.lastName == "" || dbContact.firstName == "" && dbContact.lastName == name) {
//                    return dbContact
//                }
//            }
//        }
//        return null
//    }//TODO : trouver une place pour toutes les méthodes des contacts

}