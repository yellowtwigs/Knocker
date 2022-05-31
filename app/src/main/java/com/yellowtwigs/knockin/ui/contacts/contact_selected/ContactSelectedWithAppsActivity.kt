package com.yellowtwigs.knockin.ui.contacts.contact_selected

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton
import com.yellowtwigs.knockin.R
import com.yellowtwigs.knockin.databinding.ActivityContactSelectedWithAppsBinding
import com.yellowtwigs.knockin.model.ContactManager
import com.yellowtwigs.knockin.model.data.ContactDB
import com.yellowtwigs.knockin.model.data.ContactWithAllInformation
import com.yellowtwigs.knockin.ui.contacts.ContactGridViewAdapter
import com.yellowtwigs.knockin.ui.edit_contact.EditContactDetailsActivity
import com.yellowtwigs.knockin.ui.in_app.PremiumActivity
import com.yellowtwigs.knockin.utils.ContactGesture
import com.yellowtwigs.knockin.utils.ContactGesture.goToSignal
import com.yellowtwigs.knockin.utils.ContactGesture.goToTelegram
import com.yellowtwigs.knockin.utils.ContactGesture.isWhatsappInstalled
import com.yellowtwigs.knockin.utils.Converter
import com.yellowtwigs.knockin.utils.EveryActivityUtils.getAppOnPhone
import com.yellowtwigs.knockin.utils.RandomDefaultImage

class ContactSelectedWithAppsActivity : AppCompatActivity() {

    private var appsSupportPref: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityContactSelectedWithAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appsSupportPref = getSharedPreferences("Apps_Support_Bought", Context.MODE_PRIVATE)

        val id = intent.getIntExtra("id", 0)
        val currentContact = ContactManager(this).getContactById(id)

        if (currentContact != null) {
            initCircularMenu(binding, id, currentContact)
        }

        currentContact?.contactDB?.let { initUserData(binding, it) }

        binding.backIcon.setOnClickListener {
            finish()
        }
    }

    private fun initUserData(binding: ActivityContactSelectedWithAppsBinding, contact: ContactDB) {
        binding.apply {
            firstName.text = "${contact?.firstName}"
            lastName.text = "${contact?.lastName}"

            if (contact.profilePicture64 != "") {
                val bitmap = Converter.base64ToBitmap(contact.profilePicture64)
                image.setImageBitmap(bitmap)
            } else {
                image.setImageResource(
                    RandomDefaultImage.randomDefaultImage(
                        contact.profilePicture,
                        this@ContactSelectedWithAppsActivity
                    )
                )
            }
        }
    }

    private fun initCircularMenu(
        binding: ActivityContactSelectedWithAppsBinding,
        id: Int,
        currentContact: ContactWithAllInformation
    ) {
        val listApp = getAppOnPhone(this)

        binding.apply {
            currentContact.apply {
                mailIcon.isVisible = getFirstMail() != ""
                smsIcon.isVisible = getFirstPhoneNumber() != ""
                messengerIcon.isVisible =
                    getMessengerID() != "" && listApp.contains("com.facebook.orca")
                whatsappIcon.isVisible =
                    isWhatsappInstalled(this@ContactSelectedWithAppsActivity) && currentContact.contactDB?.hasWhatsapp == 1
                signalIcon.isVisible =
                    listApp.contains("org.thoughtcrime.securesms") && currentContact.contactDB?.hasSignal == 1
                telegramIcon.isVisible =
                    listApp.contains("org.telegram.messenger") && currentContact.contactDB?.hasTelegram == 1
            }

            if (appsSupportPref?.getBoolean("Apps_Support_Bought", false) == false) {
                signalIcon.setImageResource(R.drawable.ic_signal_disable)
                telegramIcon.setImageResource(R.drawable.ic_telegram_disable)
                messengerIcon.setImageResource(R.drawable.ic_messenger_disable)
            }

            val buttonListener = View.OnClickListener { v: View ->
                when (v.id) {
                    whatsappIcon.id -> {
                        ContactGesture.openWhatsapp(
                            Converter.converter06To33(currentContact.getFirstPhoneNumber()),
                            this@ContactSelectedWithAppsActivity
                        )
                    }
                    editIcon.id -> {
                        val intent = Intent(
                            this@ContactSelectedWithAppsActivity,
                            EditContactDetailsActivity::class.java
                        )
                        intent.putExtra("ContactId", id)
                        startActivity(intent)
                    }
                    callIcon.id -> {
                        ContactGesture.callPhone(
                            currentContact.getFirstPhoneNumber(),
                            this@ContactSelectedWithAppsActivity
                        )
                    }
                    smsIcon.id -> {
                        val phone = currentContact.getFirstPhoneNumber()
                        val i = Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", phone, null))
                        startActivity(i)
                    }
                    mailIcon.id -> {
                        val mail = currentContact.getFirstMail()
                        val intent = Intent(Intent.ACTION_SENDTO)
                        intent.data = Uri.parse("mailto:")
                        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(mail))
                        intent.putExtra(Intent.EXTRA_SUBJECT, "")
                        intent.putExtra(Intent.EXTRA_TEXT, "")
                        startActivity(intent)
                    }
                    messengerIcon.id -> {
                        if (appsSupportPref?.getBoolean("Apps_Support_Bought", false) == false) {
                            showInAppAlertDialog()
                        } else {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.messenger.com/t/" + currentContact.getMessengerID())
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                    }
                    signalIcon.id -> {
                        if (appsSupportPref?.getBoolean("Apps_Support_Bought", false) == false) {
                            showInAppAlertDialog()
                        } else {
                            goToSignal(this@ContactSelectedWithAppsActivity)
                        }
                    }
                    telegramIcon.id -> {
                        if (appsSupportPref?.getBoolean("Apps_Support_Bought", false) == false) {
                            showInAppAlertDialog()
                        } else {
                            goToTelegram(
                                this@ContactSelectedWithAppsActivity,
                                currentContact.getFirstPhoneNumber()
                            )
                        }
                    }
                }
            }

            messengerIcon.setOnClickListener(buttonListener)
            whatsappIcon.setOnClickListener(buttonListener)
            callIcon.setOnClickListener(buttonListener)
            smsIcon.setOnClickListener(buttonListener)
            editIcon.setOnClickListener(buttonListener)
            mailIcon.setOnClickListener(buttonListener)
            signalIcon.setOnClickListener(buttonListener)
            telegramIcon.setOnClickListener(buttonListener)
        }
    }

    private fun showInAppAlertDialog() {
        MaterialAlertDialogBuilder(this, R.style.AlertDialog)
            .setTitle(getString(R.string.in_app_popup_apps_support_title)) //
            .setMessage(getString(R.string.in_app_popup_apps_support_message)) //
            .setPositiveButton(R.string.alert_dialog_yes) { _, _ ->
                startActivity(Intent(this, PremiumActivity::class.java))
                finish()
            }
            .setNegativeButton(R.string.alert_dialog_later) { dialog , _ ->
                dialog.dismiss()
                dialog.cancel()
            }
            .show()
    }
}