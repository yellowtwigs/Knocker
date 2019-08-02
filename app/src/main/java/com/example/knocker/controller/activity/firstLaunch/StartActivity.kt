package com.example.knocker.controller.activity.firstLaunch

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityCompat
import com.example.knocker.R
import com.example.knocker.controller.activity.MainActivity
import com.example.knocker.controller.activity.MultiSelectActivity
import com.example.knocker.model.ContactList
import com.example.knocker.model.DbWorkerThread
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.os.Build



class StartActivity : AppCompatActivity() {

    //region ========================================== Val or Var ==========================================

    private var start_activity_ImportContacts: MaterialButton? = null
    private var start_activity_ActivateNotifications: MaterialButton? = null
    private var start_activity_AuthorizeSuperposition: MaterialButton? = null
    private var start_activity_Permissions: MaterialButton? = null

    private var start_activity_Next: MaterialButton? = null
    private var start_activity_Skip:MaterialButton? =null
    private var start_activity_ImportContactsLoading: ProgressBar? = null
    private var start_activity_ActivateNotificationsLoading: ProgressBar? = null
    private var start_activity_AuthorizeSuperpositionLoading: ProgressBar? = null
    private var start_activity_PermissionsLoading: ProgressBar? = null

    private var start_activity_ImportContactsCheck: AppCompatImageView? = null
    private var start_activity_ActivateNotificationsCheck: AppCompatImageView? = null
    private var start_activity_AuthorizeSuperpositionCheck: AppCompatImageView? = null
    private var start_activity_PermissionsCheck: AppCompatImageView? = null

    private lateinit var start_activity_mDbWorkerThread: DbWorkerThread
    private var activityVisible= false
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_activity)

        //region ========================================== FindViewById ==========================================
        start_activity_ImportContacts = findViewById(R.id.start_activity_import_contacts_button)
        start_activity_ActivateNotifications = findViewById(R.id.start_activity_activate_notifications_button)
        start_activity_AuthorizeSuperposition = findViewById(R.id.start_activity_superposition_button)
        start_activity_Permissions = findViewById(R.id.start_activity_permissions_button)

        start_activity_Next = findViewById(R.id.start_activity_next)
        start_activity_Skip = findViewById(R.id.start_activity_skip)
        start_activity_ImportContactsLoading = findViewById(R.id.start_activity_import_contacts_loading)
        start_activity_ActivateNotificationsLoading = findViewById(R.id.start_activity_activate_notifications_loading)
        start_activity_AuthorizeSuperpositionLoading = findViewById(R.id.start_activity_superposition_loading)
        start_activity_PermissionsLoading = findViewById(R.id.start_activity_permissions_loading)

        start_activity_ImportContactsCheck = findViewById(R.id.start_activity_import_contacts_check)
        start_activity_ActivateNotificationsCheck = findViewById(R.id.start_activity_activate_notifications_check)
        start_activity_AuthorizeSuperpositionCheck = findViewById(R.id.start_activity_superposition_check)
        start_activity_PermissionsCheck = findViewById(R.id.start_activity_permissions_check)

        //endregion

        //region ========================================== WorkerThread ==========================================

        start_activity_mDbWorkerThread = DbWorkerThread("dbWorkerThread")
        start_activity_mDbWorkerThread.start()

        //endregion

        //region ========================================== Listeners ==========================================

        start_activity_ImportContacts!!.setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), ImportContactsActivity.REQUEST_CODE_READ_CONTACT)
            start_activity_ImportContacts!!.visibility = View.GONE
            start_activity_ImportContactsLoading!!.visibility = View.VISIBLE

            val SPLASH_DISPLAY_LENGHT = 3000
            /*
            Handler().postDelayed({
                start_activity_ImportContactsLoading!!.visibility = View.GONE
                start_activity_ImportContactsCheck!!.visibility = View.VISIBLE

            }, SPLASH_DISPLAY_LENGHT.toLong())*/
            val displayLoading = Runnable {
                start_activity_ImportContactsLoading!!.visibility = View.VISIBLE
            }
            runOnUiThread(displayLoading)
        }

        start_activity_ActivateNotifications!!.setOnClickListener {
            activateNotificationsClick()
            start_activity_ActivateNotifications!!.visibility = View.GONE
            start_activity_ActivateNotificationsLoading!!.visibility = View.VISIBLE

            val SPLASH_DISPLAY_LENGHT = 2000

           /* Handler().postDelayed({
                start_activity_ActivateNotificationsLoading!!.visibility = View.GONE
                start_activity_ActivateNotificationsCheck!!.visibility = View.VISIBLE

            }, SPLASH_DISPLAY_LENGHT.toLong())*/
            val displayLoading = Runnable {
                start_activity_ActivateNotificationsLoading!!.visibility = View.VISIBLE
            }
            runOnUiThread(displayLoading)
            val verifiedNotification= Thread{
                activityVisible=false
                while(!activityVisible){
                    println("while")
                }
                println("NotificationService"+isNotificationServiceEnabled()+" activity visible"+activityVisible)
                if(isNotificationServiceEnabled()) {
                    println("into before delayed")
               //     Handler().postDelayed({

                    val displayLoading = Runnable {
                        //start_activity_ActivateNotificationsLoading!!.visibility = View.GONE
                        //start_activity_ActivateNotificationsCheck!!.visibility = View.VISIBLE
                        Handler().postDelayed({
                            start_activity_ActivateNotificationsLoading!!.visibility = View.GONE
                            start_activity_ActivateNotificationsCheck!!.visibility = View.VISIBLE
                            val sharedPreferences: SharedPreferences = getSharedPreferences("Knocker_preferences", Context.MODE_PRIVATE)
                            val edit: SharedPreferences.Editor = sharedPreferences.edit()
                            edit.putBoolean("serviceNotif", true)
                            edit.apply()
                            allIsChecked()
                        }, SPLASH_DISPLAY_LENGHT.toLong())
                    }
                    runOnUiThread(displayLoading)

                  //  }, SPLASH_DISPLAY_LENGHT.toLong())
                }else{
                    val displayLoading = Runnable {
                        start_activity_ActivateNotificationsLoading!!.visibility = View.GONE
                        start_activity_ActivateNotifications!!.visibility = View.VISIBLE
                    }
                    runOnUiThread(displayLoading)
                }
            }
            verifiedNotification.start()

        }

        start_activity_AuthorizeSuperposition!!.setOnClickListener {
            start_activity_AuthorizeSuperposition!!.visibility = View.GONE
            start_activity_AuthorizeSuperpositionLoading!!.visibility = View.VISIBLE

            val SPLASH_DISPLAY_LENGHT = 3000

           /* Handler().postDelayed({
                start_activity_AuthorizeSuperpositionLoading!!.visibility = View.GONE
                start_activity_AuthorizeSuperpositionCheck!!.visibility = View.VISIBLE

            }, SPLASH_DISPLAY_LENGHT.toLong())*/

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName"))
                    startActivity(intent)

            }
            val displayLoading = Runnable {
                start_activity_AuthorizeSuperpositionLoading!!.visibility = View.VISIBLE
            }
            runOnUiThread(displayLoading)
            val verifiedSuperposition= Thread{
                activityVisible=false
                while(!activityVisible){
                    println("while")
                }
                println("NotificationService"+isNotificationServiceEnabled()+" activity visible"+activityVisible)
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    println("into before delayed")
                    //     Handler().postDelayed({


                    val displayLoading = Runnable {
                        //start_activity_ActivateNotificationsLoading!!.visibility = View.GONE
                        //start_activity_ActivateNotificationsCheck!!.visibility = View.VISIBLE
                        Handler().postDelayed({
                            start_activity_AuthorizeSuperpositionLoading!!.visibility = View.GONE
                            start_activity_AuthorizeSuperpositionCheck!!.visibility = View.VISIBLE
                            val sharedPreferences: SharedPreferences = getSharedPreferences("Knocker_preferences", Context.MODE_PRIVATE)
                            val edit: SharedPreferences.Editor = sharedPreferences.edit()
                            edit.putBoolean("popupNotif", true)
                            edit.apply()
                            allIsChecked()

                        }, SPLASH_DISPLAY_LENGHT.toLong())
                    }
                    runOnUiThread(displayLoading)

                    //  }, SPLASH_DISPLAY_LENGHT.toLong())
                }else{
                    val displayLoading = Runnable {
                        start_activity_AuthorizeSuperpositionLoading!!.visibility = View.GONE
                        start_activity_AuthorizeSuperposition!!.visibility = View.VISIBLE
                    }
                    runOnUiThread(displayLoading)
                }
            }
            verifiedSuperposition.start()


        }

        start_activity_Permissions!!.setOnClickListener {
            val arraylistPermission = ArrayList<String>()
            arraylistPermission.add(Manifest.permission.SEND_SMS)
            arraylistPermission.add(Manifest.permission.CALL_PHONE)
            ActivityCompat.requestPermissions(this, arraylistPermission.toArray(arrayOfNulls<String>(arraylistPermission.size)), REQUEST_CODE_SMS_AND_CALL)
            start_activity_Permissions!!.visibility = View.GONE
            start_activity_PermissionsLoading!!.visibility = View.VISIBLE


        }

        start_activity_Next!!.setOnClickListener {
            buildMultiSelectAlertDialog()
        }
            //startActivity(Intent(this@StartActivity, MultiSelectActivity::class.java))
        start_activity_Skip!!.setOnClickListener{
            buildLeaveAlertDialog()
        }



        //endregion
    }

    //region ========================================== Functions ==========================================

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_CONTACT) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                start_activity_ImportContactsLoading!!.visibility = View.GONE
                start_activity_ImportContactsCheck!!.visibility = View.VISIBLE
                Toast.makeText(this, R.string.import_contacts_toast, Toast.LENGTH_LONG).show()
                val sync = Runnable {
                    ContactList(this).getAllContacsInfoSync(contentResolver)
                }
                start_activity_mDbWorkerThread.postTask(sync)
            }else{
                start_activity_ImportContactsLoading!!.visibility = View.GONE
                start_activity_ImportContacts!!.visibility = View.VISIBLE
            }
        }
        if(REQUEST_CODE_SMS_AND_CALL==requestCode){

            start_activity_PermissionsLoading!!.visibility = View.GONE
            start_activity_PermissionsCheck!!.visibility = View.VISIBLE
        }
        allIsChecked()

    }

    private fun activateNotificationsClick() {
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        val intentFilter = IntentFilter()
        intentFilter.addAction("com.example.knocker.notificationExemple")
    }

    companion object {
        const val REQUEST_CODE_READ_CONTACT = 2
        const val REQUEST_CODE_SMS_AND_CALL=5
    }
    private fun buildMultiSelectAlertDialog(): androidx.appcompat.app.AlertDialog {
        val alertDialog = MaterialAlertDialogBuilder(this)
                .setBackground(getDrawable(R.color.backgroundColor))
                .setTitle(getString(R.string.notification_alert_dialog_title))
                .setMessage(getString(R.string.notification_alert_dialog_message))
                .setPositiveButton(R.string.alert_dialog_yes) { _, _ ->
                    startActivity(Intent(this@StartActivity, MultiSelectActivity::class.java))
                    val sharedPreferences: SharedPreferences = getSharedPreferences("Knocker_preferences", Context.MODE_PRIVATE)
                    val edit: SharedPreferences.Editor = sharedPreferences.edit()
                    edit.putBoolean("view", true)
                    edit.apply()
                    closeContextMenu()
                }
                .setNegativeButton(R.string.alert_dialog_no)
                { _, _ ->
                    closeContextMenu()
                    startActivity(Intent(this@StartActivity, MainActivity::class.java))
                }
                .show()


        return alertDialog
    }
    private fun buildLeaveAlertDialog(): androidx.appcompat.app.AlertDialog {
        val alertDialog = MaterialAlertDialogBuilder(this)
                .setBackground(getDrawable(R.color.backgroundColor))
                .setTitle("Passer les autorisations")
                .setMessage("Si vous sautez cette étape les autorisations vous serons demandé au moment ou vous en aurait besoin")
                .setPositiveButton(R.string.alert_dialog_yes) { _, _ ->
                    startActivity(Intent(this@StartActivity, MainActivity::class.java))
                    val sharedPreferences: SharedPreferences = getSharedPreferences("Knocker_preferences", Context.MODE_PRIVATE)
                    val edit: SharedPreferences.Editor = sharedPreferences.edit()
                    edit.putBoolean("view", true)
                    edit.apply()
                    closeContextMenu()
                }
                .setNegativeButton(R.string.alert_dialog_cancel)
                { _, _ ->
                    closeContextMenu()
                }
                .show()


        return alertDialog
    }
    //endregion

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val str = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(str)) {
            val names = str.split(":")
            for (i in names.indices) {
                val cn = ComponentName.unflattenFromString(names[i])
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun onStart() {
        super.onStart()
        activityVisible=true
    }

    override fun onResume() {
        super.onResume()
        activityVisible=true
    }

    override fun onPause() {
        super.onPause()
        activityVisible=false
    }
    fun allIsChecked() {
        if (start_activity_PermissionsCheck!!.visibility == View.VISIBLE &&
                start_activity_ActivateNotificationsCheck!!.visibility == View.VISIBLE &&
                start_activity_AuthorizeSuperpositionCheck!!.visibility == View.VISIBLE &&
                start_activity_ImportContactsCheck!!.visibility == View.VISIBLE) {
            start_activity_Next!!.visibility= View.VISIBLE
        }
    }

}