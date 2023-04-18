package com.yellowtwigs.knockin.model.service

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yellowtwigs.knockin.R
import com.yellowtwigs.knockin.databinding.ItemPopupNotificationBinding
import com.yellowtwigs.knockin.utils.ContactGesture
import com.yellowtwigs.knockin.utils.ContactGesture.goToSignal
import com.yellowtwigs.knockin.utils.ContactGesture.goToTelegram
import com.yellowtwigs.knockin.utils.ContactGesture.openMessenger
import com.yellowtwigs.knockin.utils.ContactGesture.openWhatsapp
import com.yellowtwigs.knockin.utils.ContactGesture.sendMail
import com.yellowtwigs.knockin.utils.ContactGesture.sendMessageWithAndroidMessage
import com.yellowtwigs.knockin.utils.ContactGesture.sendMessageWithWhatsapp
import com.yellowtwigs.knockin.utils.Converter

class PopupNotificationsListAdapter(
    private val cxt: Context, private val windowManager: WindowManager, private val popupView: View
) : ListAdapter<PopupNotificationViewState, PopupNotificationsListAdapter.ViewHolder>(PopupNotificationViewStateComparator()) {

    private lateinit var thisParent: ViewGroup
    var newMessage = false

    private var numberForPermission = ""

    var isClose = false
    private var lastChanged: Long = 0
    private var lastChangedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        thisParent = parent
        val binding = ItemPopupNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    fun deleteItem(position: Int) {
        NotificationsListenerService.deleteItem(position)
        notifyItemRemoved(position)

        if (NotificationsListenerService.popupNotificationViewStates.size == 0) {
            closeNotificationPopup()
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPopupNotificationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun onBind(popup: PopupNotificationViewState) {
            binding.apply {
                messageToSend.isEnabled = true

                val unwrappedDrawable = AppCompatResources.getDrawable(cxt, R.drawable.item_notif_adapter_top_bar)
                val wrappedDrawable = unwrappedDrawable?.let { DrawableCompat.wrap(it) }

                platform.text = popup.platform

                when (popup.platform) {
                    "Gmail" -> {
                        callButton.visibility = View.INVISIBLE

                        if (wrappedDrawable != null) {
                            setupIconAndColor(
                                platformImage, wrappedDrawable, R.drawable.ic_gmail, R.color.custom_shape_top_bar_gmail, cxt
                            )
                        }
                    }
                    "Messenger" -> {
                        callButton.visibility = View.INVISIBLE
                        buttonSend.visibility = View.INVISIBLE
                        messageToSend.visibility = View.INVISIBLE

                        if (wrappedDrawable != null) {
                            setupIconAndColor(
                                platformImage, wrappedDrawable, R.drawable.ic_messenger, R.color.custom_shape_top_bar_messenger, cxt
                            )
                        }
                    }
                    "Telegram" -> {
                        callButton.visibility = View.INVISIBLE
                        buttonSend.visibility = View.INVISIBLE
                        messageToSend.visibility = View.INVISIBLE

                        if (wrappedDrawable != null) {
                            setupIconAndColor(
                                platformImage, wrappedDrawable, R.drawable.ic_telegram, R.color.custom_shape_top_bar_telegram, cxt
                            )
                        }
                    }
                    "Signal" -> {
                        callButton.visibility = View.INVISIBLE
                        buttonSend.visibility = View.INVISIBLE
                        messageToSend.visibility = View.INVISIBLE

                        if (wrappedDrawable != null) {
                            setupIconAndColor(
                                platformImage, wrappedDrawable, R.drawable.ic_signal, R.color.custom_shape_top_bar_signal, cxt
                            )
                        }
                    }
                    "Facebook" -> {
                        callButton.visibility = View.INVISIBLE
                        buttonSend.visibility = View.INVISIBLE
                        messageToSend.visibility = View.INVISIBLE

                        if (wrappedDrawable != null) {
                            setupIconAndColor(
                                platformImage, wrappedDrawable, R.drawable.ic_facebook, R.color.custom_shape_top_bar_facebook, cxt
                            )
                        }
                    }
                    "Message" -> {
                        callButton.visibility = View.VISIBLE
                        buttonSend.visibility = View.VISIBLE
                        messageToSend.visibility = View.VISIBLE

                        if (wrappedDrawable != null) {
                            setupIconAndColor(
                                platformImage, wrappedDrawable, R.drawable.ic_micon, R.color.custom_shape_top_bar_sms, cxt
                            )
                        }
                    }
                    "WhatsApp" -> {
                        callButton.visibility = View.VISIBLE
                        buttonSend.visibility = View.VISIBLE
                        messageToSend.visibility = View.VISIBLE

                        if (wrappedDrawable != null) {
                            setupIconAndColor(
                                platformImage, wrappedDrawable, R.drawable.ic_circular_whatsapp, R.color.custom_shape_top_bar_whatsapp, cxt
                            )
                        }
                    }
                }

                if (newMessage && System.currentTimeMillis() - getLastChangeMillis() <= 10000) {
                    (thisParent as RecyclerView).post {
                        thisParent.requestFocusFromTouch()
                        (thisParent as RecyclerView).scrollToPosition(lastChangedPosition + 1)
                        thisParent.requestFocus()
                    }
                    messageToSend.isFocusable = true
                    newMessage = false
                } else if (newMessage && System.currentTimeMillis() - getLastChangeMillis() > 10000) {
                    (thisParent as RecyclerView).post {
                        thisParent.requestFocusFromTouch()
                        (thisParent as RecyclerView).scrollToPosition(0)
                        thisParent.requestFocus()
                    }
                    newMessage = false
                }

                if (platform.text == "WhatsApp" || platform.text == "Message") {
                    callButton.visibility = View.VISIBLE
                }

                content.text = "${popup.title} : ${popup.description}"

                showMessage.setOnClickListener {
                    NotificationsListenerService.alarmSound?.stop()
                    closeNotificationPopup()
                    when (platform.text) {
                        "Facebook" -> {
                            val uri = Uri.parse("facebook:/newsfeed")
                            val likeIng = Intent(Intent.ACTION_VIEW, uri)
                            try {
                                cxt.startActivity(likeIng)
                            } catch (e: ActivityNotFoundException) {
                                cxt.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW, Uri.parse("http://facebook.com/")
                                    )
                                )
                            }
                            closeNotificationPopup()
                        }
                        "Messenger" -> {
                            if (popup.messengerId != "") {
                                openMessenger(popup.messengerId, cxt)
                            } else {
                                val intent = Intent(
                                    Intent.ACTION_VIEW, Uri.parse("https://www.messenger.com/t/")
                                )
                                intent.flags = FLAG_ACTIVITY_NEW_TASK
                                cxt.startActivity(intent)
                            }
                            closeNotificationPopup()
                        }
                        "WhatsApp" -> {
                            ContactGesture.handleContactWithMultiplePhoneNumbers(
                                cxt = cxt,
                                phoneNumbers = popup.listOfPhoneNumbersWithSpinner,
                                action = "whatsapp",
                                onClickedMultipleNumbers = { action, number1, number2 ->
                                    when (action) {
                                        "call" -> {
                                            MaterialAlertDialogBuilder(
                                                cxt, R.style.AlertDialog
                                            ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle(R.string.notif_adapter_call)
                                                .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                    ContactGesture.callPhone(number1.phoneNumber, cxt)
                                                }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                    ContactGesture.callPhone(number2.phoneNumber, cxt)
                                                }.show()
                                        }
                                        "sms" -> {
                                            MaterialAlertDialogBuilder(
                                                cxt, R.style.AlertDialog
                                            ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle(R.string.list_contact_item_sms)
                                                .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                    ContactGesture.openSms(number1.phoneNumber, cxt)
                                                }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                    ContactGesture.openSms(number2.phoneNumber, cxt)
                                                }.show()
                                        }
                                        "whatsapp" -> {
                                            MaterialAlertDialogBuilder(
                                                cxt, R.style.AlertDialog
                                            ).setBackground(cxt.getDrawable(R.color.backgroundColor))
                                                .setTitle(R.string.list_contact_item_whatsapp)
                                                .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                    openWhatsapp(Converter.converter06To33(number1.phoneNumber), cxt)
                                                }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                    openWhatsapp(Converter.converter06To33(number2.phoneNumber), cxt)
                                                }.show()
                                        }
                                        "telegram" -> {
                                            MaterialAlertDialogBuilder(
                                                cxt, R.style.AlertDialog
                                            ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle("Telegram")
                                                .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                    goToTelegram(cxt, number1.phoneNumber)
                                                }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                    goToTelegram(cxt, number2.phoneNumber)
                                                }.show()
                                        }
                                    }
                                },
                                onNotMobileFlagClicked = { action, phoneNumber, message ->
                                    MaterialAlertDialogBuilder(
                                        cxt, R.style.AlertDialog
                                    ).setBackground(cxt.getDrawable(R.color.backgroundColor))
                                        .setTitle(cxt.getString(R.string.not_mobile_flag_title))
                                        .setMessage(cxt.getString(R.string.not_mobile_flag_msg))
                                        .setPositiveButton(phoneNumber) { _, _ ->
                                            when (action) {
                                                "send_whatsapp" -> {
                                                    sendMessageWithWhatsapp(phoneNumber, message, cxt)
                                                }
                                                "send_message" -> {
                                                    sendMessageWithAndroidMessage(phoneNumber, message, cxt)
                                                }
                                                "sms" -> {
                                                    ContactGesture.openSms(phoneNumber, cxt)
                                                }
                                                "whatsapp" -> {
                                                    openWhatsapp(phoneNumber, cxt)
                                                }
                                                "telegram" -> {
                                                    goToTelegram(cxt, phoneNumber)
                                                }
                                            }
                                        }.setNegativeButton(cxt.getString(R.string.alert_dialog_no)) { _, _ ->
                                        }.show()
                                },
                                ""
                            )
                            closeNotificationPopup()
                        }
                        "Gmail" -> {
                            val appIntent = Intent(Intent.ACTION_VIEW)
                            appIntent.flags = FLAG_ACTIVITY_NEW_TASK
                            appIntent.setClassName(
                                "com.google.android.gm", "com.google.android.gm.ConversationListActivityGmail"
                            )
                            try {
                                cxt.startActivity(appIntent)
                            } catch (e: ActivityNotFoundException) {
                                cxt.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW, Uri.parse("https://gmail.com/")
                                    )
                                )
                            }
                            closeNotificationPopup()
                        }
                        "Message" -> {
                            ContactGesture.handleContactWithMultiplePhoneNumbers(
                                cxt = cxt,
                                phoneNumbers = popup.listOfPhoneNumbersWithSpinner,
                                action = "sms",
                                onClickedMultipleNumbers = { action, number1, number2 ->
                                    when (action) {
                                        "call" -> {
                                            MaterialAlertDialogBuilder(
                                                cxt, R.style.AlertDialog
                                            ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle(R.string.notif_adapter_call)
                                                .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                    ContactGesture.callPhone(number1.phoneNumber, cxt)
                                                }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                    ContactGesture.callPhone(number2.phoneNumber, cxt)
                                                }.show()
                                        }
                                        "sms" -> {
                                            MaterialAlertDialogBuilder(
                                                cxt, R.style.AlertDialog
                                            ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle(R.string.list_contact_item_sms)
                                                .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                    ContactGesture.openSms(number1.phoneNumber, cxt)
                                                }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                    ContactGesture.openSms(number2.phoneNumber, cxt)
                                                }.show()
                                        }
                                        "whatsapp" -> {
                                            MaterialAlertDialogBuilder(
                                                cxt, R.style.AlertDialog
                                            ).setBackground(cxt.getDrawable(R.color.backgroundColor))
                                                .setTitle(R.string.list_contact_item_whatsapp)
                                                .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                    openWhatsapp(Converter.converter06To33(number1.phoneNumber), cxt)
                                                }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                    openWhatsapp(Converter.converter06To33(number2.phoneNumber), cxt)
                                                }.show()
                                        }
                                        "telegram" -> {
                                            MaterialAlertDialogBuilder(
                                                cxt, R.style.AlertDialog
                                            ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle("Telegram")
                                                .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                    goToTelegram(cxt, number1.phoneNumber)
                                                }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                    goToTelegram(cxt, number2.phoneNumber)
                                                }.show()
                                        }
                                    }
                                },
                                onNotMobileFlagClicked = { action, phoneNumber, message ->
                                    MaterialAlertDialogBuilder(
                                        cxt, R.style.AlertDialog
                                    ).setBackground(cxt.getDrawable(R.color.backgroundColor))
                                        .setTitle(cxt.getString(R.string.not_mobile_flag_title))
                                        .setMessage(cxt.getString(R.string.not_mobile_flag_msg))
                                        .setPositiveButton(phoneNumber) { _, _ ->
                                            when (action) {
                                                "send_whatsapp" -> {
                                                    sendMessageWithWhatsapp(phoneNumber, message, cxt)
                                                }
                                                "send_message" -> {
                                                    sendMessageWithAndroidMessage(phoneNumber, message, cxt)
                                                }
                                                "sms" -> {
                                                    ContactGesture.openSms(phoneNumber, cxt)
                                                }
                                                "whatsapp" -> {
                                                    openWhatsapp(phoneNumber, cxt)
                                                }
                                                "telegram" -> {
                                                    goToTelegram(cxt, phoneNumber)
                                                }
                                            }
                                        }.setNegativeButton(cxt.getString(R.string.alert_dialog_no)) { _, _ ->
                                        }.show()
                                },
                                ""
                            )
                            closeNotificationPopup()
                        }
                        "Signal" -> {
                            goToSignal(cxt)
                            closeNotificationPopup()
                        }
                        "Telegram" -> {
                            ContactGesture.handleContactWithMultiplePhoneNumbers(
                                cxt = cxt,
                                phoneNumbers = popup.listOfPhoneNumbersWithSpinner,
                                action = "telegram",
                                onClickedMultipleNumbers = { action, number1, number2 ->
                                    when (action) {
                                        "call" -> {
                                            MaterialAlertDialogBuilder(
                                                cxt, R.style.AlertDialog
                                            ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle(R.string.notif_adapter_call)
                                                .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                    ContactGesture.callPhone(number1.phoneNumber, cxt)
                                                }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                    ContactGesture.callPhone(number2.phoneNumber, cxt)
                                                }.show()
                                        }
                                        "sms" -> {
                                            MaterialAlertDialogBuilder(
                                                cxt, R.style.AlertDialog
                                            ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle(R.string.list_contact_item_sms)
                                                .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                    ContactGesture.openSms(number1.phoneNumber, cxt)
                                                }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                    ContactGesture.openSms(number2.phoneNumber, cxt)
                                                }.show()
                                        }
                                        "whatsapp" -> {
                                            MaterialAlertDialogBuilder(
                                                cxt, R.style.AlertDialog
                                            ).setBackground(cxt.getDrawable(R.color.backgroundColor))
                                                .setTitle(R.string.list_contact_item_whatsapp)
                                                .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                    openWhatsapp(Converter.converter06To33(number1.phoneNumber), cxt)
                                                }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                    openWhatsapp(Converter.converter06To33(number2.phoneNumber), cxt)
                                                }.show()
                                        }
                                        "telegram" -> {
                                            MaterialAlertDialogBuilder(
                                                cxt, R.style.AlertDialog
                                            ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle("Telegram")
                                                .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                    goToTelegram(cxt, number1.phoneNumber)
                                                }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                    goToTelegram(cxt, number2.phoneNumber)
                                                }.show()
                                        }
                                    }
                                },
                                onNotMobileFlagClicked = { action, phoneNumber, message ->
                                    MaterialAlertDialogBuilder(
                                        cxt, R.style.AlertDialog
                                    ).setBackground(cxt.getDrawable(R.color.backgroundColor))
                                        .setTitle(cxt.getString(R.string.not_mobile_flag_title))
                                        .setMessage(cxt.getString(R.string.not_mobile_flag_msg))
                                        .setPositiveButton(phoneNumber) { _, _ ->
                                            when (action) {
                                                "send_whatsapp" -> {
                                                    sendMessageWithWhatsapp(phoneNumber, message, cxt)
                                                }
                                                "send_message" -> {
                                                    sendMessageWithAndroidMessage(phoneNumber, message, cxt)
                                                }
                                                "sms" -> {
                                                    ContactGesture.openSms(phoneNumber, cxt)
                                                }
                                                "whatsapp" -> {
                                                    openWhatsapp(phoneNumber, cxt)
                                                }
                                                "telegram" -> {
                                                    goToTelegram(cxt, phoneNumber)
                                                }
                                            }
                                        }.setNegativeButton(cxt.getString(R.string.alert_dialog_no)) { _, _ ->
                                        }.show()
                                },
                                ""
                            )
                            closeNotificationPopup()
                        }
                    }
                }

                callButton.setOnClickListener {
                    NotificationsListenerService.alarmSound?.stop()

                    ContactGesture.handleContactWithMultiplePhoneNumbers(
                        cxt = cxt,
                        phoneNumbers = popup.listOfPhoneNumbersWithSpinner,
                        action = "call",
                        onClickedMultipleNumbers = { action, number1, number2 ->
                            when (action) {
                                "call" -> {
                                    MaterialAlertDialogBuilder(
                                        cxt, R.style.AlertDialog
                                    ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle(R.string.notif_adapter_call)
                                        .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                        .setPositiveButton(number1.phoneNumber) { _, _ ->
                                            ContactGesture.callPhone(number1.phoneNumber, cxt)
                                        }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                            ContactGesture.callPhone(number2.phoneNumber, cxt)
                                        }.show()
                                }
                                "sms" -> {
                                    MaterialAlertDialogBuilder(
                                        cxt, R.style.AlertDialog
                                    ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle(R.string.list_contact_item_sms)
                                        .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                        .setPositiveButton(number1.phoneNumber) { _, _ ->
                                            ContactGesture.openSms(number1.phoneNumber, cxt)
                                        }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                            ContactGesture.openSms(number2.phoneNumber, cxt)
                                        }.show()
                                }
                                "whatsapp" -> {
                                    MaterialAlertDialogBuilder(
                                        cxt, R.style.AlertDialog
                                    ).setBackground(cxt.getDrawable(R.color.backgroundColor))
                                        .setTitle(R.string.list_contact_item_whatsapp)
                                        .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                        .setPositiveButton(number1.phoneNumber) { _, _ ->
                                            openWhatsapp(Converter.converter06To33(number1.phoneNumber), cxt)
                                        }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                            openWhatsapp(Converter.converter06To33(number2.phoneNumber), cxt)
                                        }.show()
                                }
                                "telegram" -> {
                                    MaterialAlertDialogBuilder(
                                        cxt, R.style.AlertDialog
                                    ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle("Telegram")
                                        .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                        .setPositiveButton(number1.phoneNumber) { _, _ ->
                                            goToTelegram(cxt, number1.phoneNumber)
                                        }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                            goToTelegram(cxt, number2.phoneNumber)
                                        }.show()
                                }
                            }
                        },
                        onNotMobileFlagClicked = { action, phoneNumber, message ->
                            MaterialAlertDialogBuilder(
                                cxt, R.style.AlertDialog
                            ).setBackground(cxt.getDrawable(R.color.backgroundColor))
                                .setTitle(cxt.getString(R.string.not_mobile_flag_title))
                                .setMessage(cxt.getString(R.string.not_mobile_flag_msg))
                                .setPositiveButton(phoneNumber) { _, _ ->
                                    when (action) {
                                        "send_whatsapp" -> {
                                            sendMessageWithWhatsapp(phoneNumber, message, cxt)
                                        }
                                        "send_message" -> {
                                            sendMessageWithAndroidMessage(phoneNumber, message, cxt)
                                        }
                                        "sms" -> {
                                            ContactGesture.openSms(phoneNumber, cxt)
                                        }
                                        "whatsapp" -> {
                                            openWhatsapp(phoneNumber, cxt)
                                        }
                                        "telegram" -> {
                                            goToTelegram(cxt, phoneNumber)
                                        }
                                    }
                                }.setNegativeButton(cxt.getString(R.string.alert_dialog_no)) { _, _ ->
                                }.show()
                        },
                        ""
                    )
                    closeNotificationPopup()
                }

                buttonSend.setOnClickListener {
                    NotificationsListenerService.alarmSound?.stop()
                    if (messageToSend.text.toString() == "") {
                        Toast.makeText(cxt, R.string.notif_adapter, Toast.LENGTH_SHORT).show()
                    } else {
                        val message = messageToSend.text.toString()
                        when (popup.platform) {
                            "WhatsApp" -> {
                                ContactGesture.handleContactWithMultiplePhoneNumbers(
                                    cxt = cxt,
                                    phoneNumbers = popup.listOfPhoneNumbersWithSpinner,
                                    action = "send_whatsapp",
                                    onClickedMultipleNumbers = { action, number1, number2 ->
                                        when (action) {
                                            "call" -> {
                                                MaterialAlertDialogBuilder(
                                                    cxt, R.style.AlertDialog
                                                ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle(R.string.notif_adapter_call)
                                                    .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                    .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                        ContactGesture.callPhone(number1.phoneNumber, cxt)
                                                    }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                        ContactGesture.callPhone(number2.phoneNumber, cxt)
                                                    }.show()
                                            }
                                            "sms" -> {
                                                MaterialAlertDialogBuilder(
                                                    cxt, R.style.AlertDialog
                                                ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle(R.string.list_contact_item_sms)
                                                    .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                    .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                        ContactGesture.openSms(number1.phoneNumber, cxt)
                                                    }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                        ContactGesture.openSms(number2.phoneNumber, cxt)
                                                    }.show()
                                            }
                                            "whatsapp" -> {
                                                MaterialAlertDialogBuilder(
                                                    cxt, R.style.AlertDialog
                                                ).setBackground(cxt.getDrawable(R.color.backgroundColor))
                                                    .setTitle(R.string.list_contact_item_whatsapp)
                                                    .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                    .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                        openWhatsapp(Converter.converter06To33(number1.phoneNumber), cxt)
                                                    }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                        openWhatsapp(Converter.converter06To33(number2.phoneNumber), cxt)
                                                    }.show()
                                            }
                                            "telegram" -> {
                                                MaterialAlertDialogBuilder(
                                                    cxt, R.style.AlertDialog
                                                ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle("Telegram")
                                                    .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                    .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                        goToTelegram(cxt, number1.phoneNumber)
                                                    }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                        goToTelegram(cxt, number2.phoneNumber)
                                                    }.show()
                                            }
                                        }
                                    },
                                    onNotMobileFlagClicked = { action, phoneNumber, message ->
                                        MaterialAlertDialogBuilder(
                                            cxt, R.style.AlertDialog
                                        ).setBackground(cxt.getDrawable(R.color.backgroundColor))
                                            .setTitle(cxt.getString(R.string.not_mobile_flag_title))
                                            .setMessage(cxt.getString(R.string.not_mobile_flag_msg))
                                            .setPositiveButton(phoneNumber) { _, _ ->
                                                when (action) {
                                                    "send_whatsapp" -> {
                                                        sendMessageWithWhatsapp(phoneNumber, message, cxt)
                                                    }
                                                    "send_message" -> {
                                                        sendMessageWithAndroidMessage(phoneNumber, message, cxt)
                                                    }
                                                    "sms" -> {
                                                        ContactGesture.openSms(phoneNumber, cxt)
                                                    }
                                                    "whatsapp" -> {
                                                        openWhatsapp(phoneNumber, cxt)
                                                    }
                                                    "telegram" -> {
                                                        goToTelegram(cxt, phoneNumber)
                                                    }
                                                }
                                            }.setNegativeButton(cxt.getString(R.string.alert_dialog_no)) { _, _ ->
                                            }.show()
                                    },
                                    message = message
                                )
                                closeNotificationPopup()
                            }
                            "Gmail" -> {
                                sendMail(popup.email, "", message, cxt)
                                closeNotificationPopup()
                            }
                            "Message" -> {
                                ContactGesture.handleContactWithMultiplePhoneNumbers(
                                    cxt = cxt,
                                    phoneNumbers = popup.listOfPhoneNumbersWithSpinner,
                                    action = "send_message",
                                    onClickedMultipleNumbers = { action, number1, number2 ->
                                        when (action) {
                                            "call" -> {
                                                MaterialAlertDialogBuilder(
                                                    cxt, R.style.AlertDialog
                                                ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle(R.string.notif_adapter_call)
                                                    .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                    .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                        ContactGesture.callPhone(number1.phoneNumber, cxt)
                                                    }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                        ContactGesture.callPhone(number2.phoneNumber, cxt)
                                                    }.show()
                                            }
                                            "sms" -> {
                                                MaterialAlertDialogBuilder(
                                                    cxt, R.style.AlertDialog
                                                ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle(R.string.list_contact_item_sms)
                                                    .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                    .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                        ContactGesture.openSms(number1.phoneNumber, cxt)
                                                    }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                        ContactGesture.openSms(number2.phoneNumber, cxt)
                                                    }.show()
                                            }
                                            "whatsapp" -> {
                                                MaterialAlertDialogBuilder(
                                                    cxt, R.style.AlertDialog
                                                ).setBackground(cxt.getDrawable(R.color.backgroundColor))
                                                    .setTitle(R.string.list_contact_item_whatsapp)
                                                    .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                    .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                        openWhatsapp(Converter.converter06To33(number1.phoneNumber), cxt)
                                                    }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                        openWhatsapp(Converter.converter06To33(number2.phoneNumber), cxt)
                                                    }.show()
                                            }
                                            "telegram" -> {
                                                MaterialAlertDialogBuilder(
                                                    cxt, R.style.AlertDialog
                                                ).setBackground(cxt.getDrawable(R.color.backgroundColor)).setTitle("Telegram")
                                                    .setMessage(cxt.getString(R.string.two_numbers_dialog_message))
                                                    .setPositiveButton(number1.phoneNumber) { _, _ ->
                                                        goToTelegram(cxt, number1.phoneNumber)
                                                    }.setNegativeButton(number2.phoneNumber) { _, _ ->
                                                        goToTelegram(cxt, number2.phoneNumber)
                                                    }.show()
                                            }
                                        }
                                    },
                                    onNotMobileFlagClicked = { action, phoneNumber, msg ->
                                        MaterialAlertDialogBuilder(
                                            cxt, R.style.AlertDialog
                                        ).setBackground(cxt.getDrawable(R.color.backgroundColor))
                                            .setTitle(cxt.getString(R.string.not_mobile_flag_title))
                                            .setMessage(cxt.getString(R.string.not_mobile_flag_msg))
                                            .setPositiveButton(phoneNumber) { _, _ ->
                                                when (action) {
                                                    "send_whatsapp" -> {
                                                        sendMessageWithWhatsapp(phoneNumber, msg, cxt)
                                                    }
                                                    "send_message" -> {
                                                        sendMessageWithAndroidMessage(phoneNumber, msg, cxt)
                                                    }
                                                    "sms" -> {
                                                        ContactGesture.openSms(phoneNumber, cxt)
                                                    }
                                                    "whatsapp" -> {
                                                        openWhatsapp(phoneNumber, cxt)
                                                    }
                                                    "telegram" -> {
                                                        goToTelegram(cxt, phoneNumber)
                                                    }
                                                }
                                            }.setNegativeButton(cxt.getString(R.string.alert_dialog_no)) { _, _ ->
                                            }.show()
                                    },
                                    message = message
                                )
                                closeNotificationPopup()
                            }
                        }
                    }
                }
            }
        }

        private fun setupIconAndColor(
            platformImage: AppCompatImageView, wrappedDrawable: Drawable, iconId: Int, colorId: Int, cxt: Context
        ) {
            platformImage.setImageResource(iconId)
            DrawableCompat.setTint(
                wrappedDrawable, cxt.resources.getColor(
                    colorId, null
                )
            )
        }

        private fun getLastChangeMillis(): Long {
            return lastChanged
        }
    }

    private fun phoneCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(
                cxt, Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(cxt, cxt.getString(R.string.allow_phone_call), Toast.LENGTH_SHORT).show()
            numberForPermission = phoneNumber
        } else {
            closeNotificationPopup()
            val intent = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", phoneNumber, null))
            intent.flags = FLAG_ACTIVITY_NEW_TASK
            if (numberForPermission.isEmpty()) {
                cxt.startActivity(intent)
            } else {
                cxt.startActivity(intent)
                numberForPermission = ""
            }
        }
    }

    private fun closeNotificationPopup() {
        NotificationsListenerService.alarmSound?.stop()
        windowManager.removeView(popupView)
        val sharedPreferences = cxt.getSharedPreferences("Knockin_preferences", Context.MODE_PRIVATE)
        val edit = sharedPreferences.edit()
        edit.putBoolean("view", false)
        edit.apply()
    }

    class PopupNotificationViewStateComparator : DiffUtil.ItemCallback<PopupNotificationViewState>() {
        override fun areItemsTheSame(
            oldItem: PopupNotificationViewState, newItem: PopupNotificationViewState
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: PopupNotificationViewState, newItem: PopupNotificationViewState
        ): Boolean {
            return oldItem == newItem
        }
    }
}