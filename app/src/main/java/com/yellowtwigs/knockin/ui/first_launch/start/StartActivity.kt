package com.yellowtwigs.knockin.ui.first_launch.start

import android.Manifest
import android.app.ActivityManager
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.android.billingclient.api.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yellowtwigs.knockin.R
import com.yellowtwigs.knockin.databinding.ActivityStartActivityBinding
import com.yellowtwigs.knockin.model.ContactManager
import com.yellowtwigs.knockin.model.DbWorkerThread
import com.yellowtwigs.knockin.model.data.ContactDB
import com.yellowtwigs.knockin.model.data.ContactDetailDB
import com.yellowtwigs.knockin.ui.contacts.MainActivity
import com.yellowtwigs.knockin.ui.first_launch.ImportContactsActivity
import com.yellowtwigs.knockin.ui.first_launch.MultiSelectActivity
import com.yellowtwigs.knockin.utils.EveryActivityUtils.getAppOnPhone
import kotlin.math.abs

/**
 * Activité qui nous permet d'importer nos contacts et accepter toutes les autorisations liées aux notifications appel et message
 * @author Florian Striebel, Kenzy Suon
 */
class StartActivity : AppCompatActivity(), PurchasesUpdatedListener {

    //region ========================================== Val or Var ==========================================

    private lateinit var workerThread: DbWorkerThread
    private var currentPosition = 0

    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityStartActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

        setupBillingClient()
        setSliderContainer(binding)

        //region ======================================= WorkerThread =======================================

        workerThread = DbWorkerThread("dbWorkerThread")
        workerThread.start()

        //endregion

        if (checkIfGoEdition()) {
            MaterialAlertDialogBuilder(this, R.style.AlertDialog)
                .setBackground(getDrawable(R.color.backgroundColor))
                .setMessage(getString(R.string.start_activity_go_edition_message))
                .setPositiveButton(R.string.start_activity_go_edition_positive_button) { _, _ ->
                }
                .show()
        }

        val importContactPreferences = getSharedPreferences("Import_Contact", Context.MODE_PRIVATE)

        val arraylistPermission = ArrayList<String>()
        arraylistPermission.add(Manifest.permission.READ_CONTACTS)
        arraylistPermission.add(Manifest.permission.SEND_SMS)
        arraylistPermission.add(Manifest.permission.CALL_PHONE)

        if (!importContactPreferences.getBoolean("Import_Contact", false)) {
            ActivityCompat.requestPermissions(
                this,
                arraylistPermission.toArray(arrayOfNulls<String>(arraylistPermission.size)),
                ALL_PERMISSIONS
            )
        }

        binding.apply {
            activateButton.setOnClickListener {
                when (currentPosition) {
                    0 -> {
                        activateNotificationsClick()
                    }
                    1 -> {
                        openOverlaySettings()
                    }
                }
            }
            next.setOnClickListener {
                if (!checkIfGoEdition()) {
                    startActivity(Intent(this@StartActivity, MultiSelectActivity::class.java))
                    val sharedPreferences: SharedPreferences =
                        getSharedPreferences("Knockin_preferences", Context.MODE_PRIVATE)
                    val edit: SharedPreferences.Editor = sharedPreferences.edit()
                    edit.putBoolean("view", true)
                    edit.apply()
                    finish()
                } else {
                    val intent = Intent(this@StartActivity, MainActivity::class.java)
                    intent.putExtra("fromStartActivity", true)
                    startActivity(intent)
                    finish()
                }
            }
            skip.setOnClickListener {
                val sharedPreferences: SharedPreferences =
                    getSharedPreferences("Knockin_preferences", Context.MODE_PRIVATE)
                val edit: SharedPreferences.Editor = sharedPreferences.edit()
                edit.putBoolean("view", true)
                edit.apply()
                val intent = Intent(this@StartActivity, MainActivity::class.java)
                intent.putExtra("fromStartActivity", true)
                startActivity(intent)
                finish()
            }
        }
    }

    //region ========================================== Functions ==========================================

    private fun setSliderContainer(binding: ActivityStartActivityBinding) {
        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val sliderItems = arrayListOf<SliderItem>()
        sliderItems.add(SliderItem(R.drawable.notification_reception))
        sliderItems.add(SliderItem(R.drawable.vip_message_reception))
        sliderItems.add(SliderItem(R.drawable.carrousel_1))

        val sliderAdapter = SliderAdapter(sliderItems)

        viewPager.apply {
            adapter = sliderAdapter
            clipToPadding = false
            clipChildren = false
            offscreenPageLimit = 3
            getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

            val compositePageTransformer = CompositePageTransformer()
            compositePageTransformer.addTransformer(MarginPageTransformer(30))
            compositePageTransformer.addTransformer { page, position ->
                val r = 1 - abs(position)
                page.scaleY = 0.85f + r * 0.25f
            }

            setPageTransformer(compositePageTransformer)

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    currentPosition = position

                    if (checkIfGoEdition()) {
                        currentPosition = 2
                    }

                    binding.apply {
                        when (currentPosition) {
                            0 -> {
                                title.text = getString(R.string.start_activity_notification_title)
                                subtitle.text =
                                    getString(R.string.start_activity_notification_subtitle)

                                radioButton1.isChecked = true
                                radioButton2.isChecked = false
                                radioButton3.isChecked = false

                                activateButton.visibility = View.VISIBLE
                                next.visibility = View.GONE
                                skip.visibility = View.GONE
                            }
                            1 -> {
                                title.text = getString(R.string.superposition_title)
                                subtitle.text = getString(R.string.superposition_subtitle)

                                radioButton1.isChecked = false
                                radioButton2.isChecked = true
                                radioButton3.isChecked = false

                                activateButton.visibility = View.VISIBLE
                                next.visibility = View.GONE
                                skip.visibility = View.GONE
                            }
                            2 -> {
                                title.text = getString(R.string.notification_alert_dialog_title)
                                subtitle.text =
                                    getString(R.string.notification_alert_dialog_message)

                                radioButton1.isChecked = false
                                radioButton2.isChecked = false
                                radioButton3.isChecked = true

                                activateButton.visibility = View.GONE
                                next.visibility = View.VISIBLE
                                skip.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            })
        }
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    //region =========================================== BILLING ============================================

    private fun setupBillingClient() {
        val billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        connectToGooglePlayBilling(billingClient)
    }

    private fun connectToGooglePlayBilling(billingClient: BillingClient) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySkuDetails(billingClient)
                } else {
                    Log.i("playBilling", "Fail")
                }
            }

            override fun onBillingServiceDisconnected() {
                connectToGooglePlayBilling(billingClient)
            }
        })
    }

    private fun querySkuDetails(billingClient: BillingClient) {
        val skuList = ArrayList<String>()
        skuList.add("contacts_vip_unlimited")
        skuList.add("notifications_vip_funk_theme")
        skuList.add("notifications_vip_jazz_theme")
        skuList.add("notifications_vip_relaxation_theme")
        skuList.add("additional_applications_support")

        val params = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(BillingClient.SkuType.INAPP)

        billingClient.querySkuDetailsAsync(
            params.build()
        ) { _, _ ->
            billingClient.queryPurchasesAsync(
                BillingClient.SkuType.INAPP
            ) { _, listOfPurchases ->
                if (listOfPurchases.isNotEmpty()) {
                    fillSharedPreferencesWithListPurchases(listOfPurchases)
                }
            }
        }
    }

    private fun fillSharedPreferencesWithListPurchases(listOfPurchases: MutableList<Purchase>) {
        for (purchase in listOfPurchases) {
            purchase.originalJson.apply {
                when {
                    contains("notifications_vip_funk_theme") -> {
                        sharedPreferencesConfiguration("Funky_Sound_Bought")
                    }
                    contains("notifications_vip_jazz_theme") -> {
                        sharedPreferencesConfiguration("Jazzy_Sound_Bought")
                    }
                    contains("notifications_vip_relaxation_theme") -> {
                        sharedPreferencesConfiguration("Relax_Sound_Bought")
                    }
                    contains("contacts_vip_unlimited") -> {
                        sharedPreferencesConfiguration("Contacts_Unlimited_Bought")
                    }
                    contains("additional_applications_support") -> {
                        sharedPreferencesConfiguration("Apps_Support_Bought")
                    }
                }
            }
        }
    }

    private fun sharedPreferencesConfiguration(sharedPreferencesName: String) {
        val private = Context.MODE_PRIVATE

        val sharedPreferences = getSharedPreferences(sharedPreferencesName, private)
        val edit = sharedPreferences.edit()
        edit.putBoolean(sharedPreferencesName, true)
        edit.apply()
    }

    //endregion

    private fun checkIfGoEdition(): Boolean {
        val am = baseContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return am.isLowRamDevice
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ALL_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (getAppOnPhone(this).contains("com.whatsapp")) {
                    val importWhatsappSharedPreferences: SharedPreferences =
                        getSharedPreferences("importWhatsappPreferences", Context.MODE_PRIVATE)
                    val edit: SharedPreferences.Editor = importWhatsappSharedPreferences.edit()
                    edit.putBoolean("importWhatsappPreferences", true)
                    edit.apply()
                }
                val sync = Runnable {
                    ContactManager(this).getAllContactsInfoSync(contentResolver)

                    val sharedPreferencesSync =
                        getSharedPreferences("save_last_sync", Context.MODE_PRIVATE)
                    var index = 1
                    var stringSet = listOf<String>()
                    if (sharedPreferencesSync.getStringSet(index.toString(), null) != null)
                        stringSet =
                            sharedPreferencesSync.getStringSet(index.toString(), null)!!.sorted()
                    arrayListOf<Pair<ContactDB, List<ContactDetailDB>>>()
                    while (sharedPreferencesSync.getStringSet(
                            index.toString(),
                            null
                        ) != null && stringSet.isNotEmpty()
                    ) {
                        stringSet =
                            sharedPreferencesSync.getStringSet(index.toString(), null)!!.sorted()
                        index++
                    }
                }
                workerThread.postTask(sync)
            }
        }
    }

    override fun onBackPressed() {
    }

    private fun activateNotificationsClick() {
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        val intentFilter = IntentFilter()
        intentFilter.addAction("com.yellowtwigs.Knockin.notificationExemple")
    }

    companion object {
        const val ALL_PERMISSIONS = 1
    }

    private fun buildMultiSelectAlertDialog(): AlertDialog {
        return MaterialAlertDialogBuilder(this, R.style.AlertDialog)
            .setBackground(getDrawable(R.color.backgroundColor))
            .setTitle(getString(R.string.notification_alert_dialog_title))
            .setMessage(getString(R.string.notification_alert_dialog_message))
            .setPositiveButton(R.string.alert_dialog_yes) { _, _ ->
                startActivity(Intent(this@StartActivity, MultiSelectActivity::class.java))
                val sharedPreferences: SharedPreferences =
                    getSharedPreferences("Knockin_preferences", Context.MODE_PRIVATE)
                val edit: SharedPreferences.Editor = sharedPreferences.edit()
                edit.putBoolean("view", true)
                edit.apply()
                closeContextMenu()
                finish()
            }
            .setNegativeButton(R.string.alert_dialog_later)
            { _, _ ->
                closeContextMenu()
                val intent = Intent(this@StartActivity, MainActivity::class.java)
                intent.putExtra("fromStartActivity", true)
                startActivity(intent)
                finish()
            }
            .show()
    }

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

    override fun onPurchasesUpdated(p0: BillingResult, p1: MutableList<Purchase>?) {
    }

    //endregion

    //region =========================================== Lifecycle ==========================================

    override fun onRestart() {
        super.onRestart()

        val sharedPreferences = getSharedPreferences("Knockin_preferences", Context.MODE_PRIVATE)
        if (isNotificationServiceEnabled()) {
            val edit: SharedPreferences.Editor = sharedPreferences.edit()
            edit.putBoolean("serviceNotif", true)
            edit.apply()
        }
        if (Settings.canDrawOverlays(this)) {
            val edit: SharedPreferences.Editor = sharedPreferences.edit()
            edit.putBoolean("popupNotif", true)
            edit.apply()
        }
    }

    //endregion
}