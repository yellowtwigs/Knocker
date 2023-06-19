package com.yellowtwigs.knockin.ui.statistics.daily_statistics

import android.app.Application
import android.provider.Telephony
import android.util.Log
import androidx.lifecycle.*
import com.yellowtwigs.knockin.R
import com.yellowtwigs.knockin.repositories.contacts.list.ContactsListRepository
import com.yellowtwigs.knockin.repositories.notifications.NotificationsRepository
import com.yellowtwigs.knockin.ui.statistics.dashboard.DashboardViewState
import com.yellowtwigs.knockin.utils.CoroutineDispatcherProvider
import com.yellowtwigs.knockin.utils.NotificationsGesture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DailyStatisticsViewModel @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
    private val contactsListRepository: ContactsListRepository,
    coroutineDispatcherProvider: CoroutineDispatcherProvider,
    private val application: Application
) : ViewModel() {

    val spinnerSelectedItemFlow = MutableStateFlow(0)

    val dailyStatisticsViewStateLiveData: LiveData<DailyStatisticsViewState> = liveData(coroutineDispatcherProvider.io) {
        combine(
            notificationsRepository.getAllNotifications().asFlow(),
            contactsListRepository.getNumbersOfContactsVipFlow(),
            contactsListRepository.getNumbersOfContactsStandardFlow(),
            contactsListRepository.getNumbersOfContactsSilentFlow(),
            spinnerSelectedItemFlow
        ) { list, numberOfContactsVIP, numberOfContactsStandard, numberOfContactsSilent, selectItem ->

            val platform = when (selectItem) {
                0 -> ""
                1 -> "Message"
                2 -> NotificationsGesture.WHATSAPP_PACKAGE
                3 -> NotificationsGesture.MESSENGER_PACKAGE
                4 -> NotificationsGesture.TELEGRAM_PACKAGE
                5 -> NotificationsGesture.SIGNAL_PACKAGE
                6 -> NotificationsGesture.OUTLOOK_PACKAGE
                7 -> NotificationsGesture.GMAIL_PACKAGE
                else -> ""
            }

            val allVipNumbers = list.filter { notification ->
                if (platform != "") {
                    if (platform == "Message") {
                        notification.isSystem == 0 && notification.priority == 2 && notification.platform == NotificationsGesture.MESSAGE_PACKAGE || notification.platform == NotificationsGesture.XIAOMI_MESSAGE_PACKAGE || notification.platform == NotificationsGesture.MESSAGE_SAMSUNG_PACKAGE || notification.platform == NotificationsGesture.MESSAGES_PACKAGE || notification.platform == Telephony.Sms.getDefaultSmsPackage(
                            application
                        )
                    } else {
                        notification.isSystem == 0 && notification.platform == platform && notification.priority == 2
                    }
                } else {
                    notification.isSystem == 0 && notification.priority == 2
                }
            }.size.toString()
            val vipNumbersDaily = list.filter { notification ->
                if (platform != "") {
                    if (platform == "Message") {
                        notification.isSystem == 0 && compareIfNotificationDateIsToday(notification.timestamp) && notification.platform == NotificationsGesture.MESSAGE_PACKAGE || notification.platform == NotificationsGesture.XIAOMI_MESSAGE_PACKAGE || notification.platform == NotificationsGesture.MESSAGE_SAMSUNG_PACKAGE || notification.platform == NotificationsGesture.MESSAGES_PACKAGE || notification.platform == Telephony.Sms.getDefaultSmsPackage(
                            application
                        ) && notification.priority == 2
                    } else {
                        notification.isSystem == 0 && compareIfNotificationDateIsToday(notification.timestamp) && notification.platform == platform && notification.priority == 2
                    }
                } else {
                    notification.isSystem == 0 && compareIfNotificationDateIsToday(notification.timestamp) && notification.priority == 2
                }
            }.size / 2
            val vipNumbersWeekly = list.filter { notification ->
                if (platform != "") {
                    if (platform == "Message") {
                        notification.isSystem == 0 && compareIfNotificationDateIsFromThisWeek(notification.timestamp) && notification.platform == NotificationsGesture.MESSAGE_PACKAGE || notification.platform == NotificationsGesture.XIAOMI_MESSAGE_PACKAGE || notification.platform == NotificationsGesture.MESSAGE_SAMSUNG_PACKAGE || notification.platform == NotificationsGesture.MESSAGES_PACKAGE || notification.platform == Telephony.Sms.getDefaultSmsPackage(
                            application
                        ) && notification.priority == 2
                    } else {
                        notification.isSystem == 0 && compareIfNotificationDateIsFromThisWeek(notification.timestamp) && notification.platform == platform && notification.priority == 2
                    }
                } else {
                    notification.isSystem == 0 && compareIfNotificationDateIsFromThisWeek(notification.timestamp) && notification.priority == 2
                }
            }.size.toString()
            val vipNumbersMonthly = list.filter { notification ->
                if (platform != "") {
                    if (platform == "Message") {
                        notification.isSystem == 0 && compareIfNotificationDateIsFromThisMonth(notification.timestamp) && notification.platform == NotificationsGesture.MESSAGE_PACKAGE || notification.platform == NotificationsGesture.XIAOMI_MESSAGE_PACKAGE || notification.platform == NotificationsGesture.MESSAGE_SAMSUNG_PACKAGE || notification.platform == NotificationsGesture.MESSAGES_PACKAGE || notification.platform == Telephony.Sms.getDefaultSmsPackage(
                            application
                        ) && notification.priority == 2
                    } else {
                        notification.isSystem == 0 && compareIfNotificationDateIsFromThisMonth(notification.timestamp) && notification.platform == platform && notification.priority == 2
                    }
                } else {
                    notification.isSystem == 0 && compareIfNotificationDateIsFromThisMonth(notification.timestamp) && notification.priority == 2
                }
            }.size.toString()

            val allMessagingNumbers = list.filter { notification ->
                if (platform != "") {
                    if (platform == "Message") {
                        notification.isSystem == 0 && notification.platform == NotificationsGesture.MESSAGE_PACKAGE || notification.platform == NotificationsGesture.XIAOMI_MESSAGE_PACKAGE || notification.platform == NotificationsGesture.MESSAGE_SAMSUNG_PACKAGE || notification.platform == NotificationsGesture.MESSAGES_PACKAGE || notification.platform == Telephony.Sms.getDefaultSmsPackage(
                            application
                        )
                    } else {
                        notification.isSystem == 0 && notification.platform == platform
                    }
                } else {
                    notification.isSystem == 0
                }
            }.size.toString()
            val messagingNumbersDaily = list.filter { notification ->
                if (platform != "") {
                    if (platform == "Message") {
                        notification.isSystem == 0 && compareIfNotificationDateIsToday(notification.timestamp) && notification.platform == NotificationsGesture.MESSAGE_PACKAGE || notification.platform == NotificationsGesture.XIAOMI_MESSAGE_PACKAGE || notification.platform == NotificationsGesture.MESSAGE_SAMSUNG_PACKAGE || notification.platform == NotificationsGesture.MESSAGES_PACKAGE || notification.platform == Telephony.Sms.getDefaultSmsPackage(
                            application
                        )
                    } else {
                        notification.isSystem == 0 && compareIfNotificationDateIsToday(notification.timestamp) && notification.platform == platform
                    }
                } else {
                    notification.isSystem == 0 && compareIfNotificationDateIsToday(notification.timestamp)
                }
            }.size
            val messagingNumbersWeekly = list.filter { notification ->
                if (platform != "") {
                    if (platform == "Message") {
                        notification.isSystem == 0 && compareIfNotificationDateIsFromThisWeek(notification.timestamp) && notification.platform == NotificationsGesture.MESSAGE_PACKAGE || notification.platform == NotificationsGesture.XIAOMI_MESSAGE_PACKAGE || notification.platform == NotificationsGesture.MESSAGE_SAMSUNG_PACKAGE || notification.platform == NotificationsGesture.MESSAGES_PACKAGE || notification.platform == Telephony.Sms.getDefaultSmsPackage(
                            application
                        )
                    } else {
                        notification.isSystem == 0 && compareIfNotificationDateIsFromThisWeek(notification.timestamp) && notification.platform == platform
                    }
                } else {
                    notification.isSystem == 0 && compareIfNotificationDateIsFromThisWeek(notification.timestamp)
                }
            }.size.toString()
            val messagingNumbersMonthly = list.filter { notification ->
                if (platform != "") {
                    if (platform == "Message") {
                        notification.isSystem == 0 && compareIfNotificationDateIsFromThisMonth(notification.timestamp) && notification.platform == NotificationsGesture.MESSAGE_PACKAGE || notification.platform == NotificationsGesture.XIAOMI_MESSAGE_PACKAGE || notification.platform == NotificationsGesture.MESSAGE_SAMSUNG_PACKAGE || notification.platform == NotificationsGesture.MESSAGES_PACKAGE || notification.platform == Telephony.Sms.getDefaultSmsPackage(
                            application
                        )
                    } else {
                        notification.isSystem == 0 && compareIfNotificationDateIsFromThisMonth(notification.timestamp) && notification.platform == platform
                    }
                } else {
                    notification.isSystem == 0 && compareIfNotificationDateIsFromThisMonth(notification.timestamp)
                }
            }.size.toString()

            val allNotificationsAvoided = allMessagingNumbers.toInt().minus(allVipNumbers.toInt())

//            val icon = when (messagingNumbersDaily.minus(vipNumbersDaily)) {
//                in 0..9 -> R.drawable.ic_speedometer_strong_red
//                in 10..30 -> R.drawable.ic_speedometer_light_green
//                in 30..50 -> R.drawable.ic_speedometer_strong_green
//                in 50..100 -> R.drawable.ic_speedometer_orange
//                else -> {
//                    R.drawable.ic_speedometer_yellow
//                }
//            }

            // 5+ non-VIP notifs from other contacts

            val nonVipNotificationsNumbers = messagingNumbersDaily.minus(vipNumbersDaily)

            val numberOfContacts = numberOfContactsStandard.plus(numberOfContactsVIP).plus(numberOfContactsSilent)

            val isAllOtherContactsSilent = numberOfContacts.minus(numberOfContactsVIP) == numberOfContactsSilent

            val icon = if (numberOfContactsVIP == 0 && numberOfContactsSilent == 0) {
                R.drawable.ic_speedometer_strong_red
            } else if (numberOfContactsVIP < 5 && numberOfContactsSilent == 0) {
                R.drawable.ic_speedometer_orange
            } else if (numberOfContactsVIP < 5 && nonVipNotificationsNumbers >= 5) {
                R.drawable.ic_speedometer_yellow
            } else if (numberOfContactsVIP == 5 && nonVipNotificationsNumbers >= 5) {
                R.drawable.ic_speedometer_light_green
            } else if (numberOfContactsVIP > 1 && isAllOtherContactsSilent) {
                R.drawable.ic_speedometer_strong_green
            } else {
                R.drawable.ic_speedometer_yellow
            }

            val strongRed = numberOfContactsVIP == 0 && numberOfContactsSilent == 0
            val orange = numberOfContactsVIP < 5 && numberOfContactsSilent == 0
            val yellow = numberOfContactsVIP < 5 && nonVipNotificationsNumbers >= 5
            val lightGreen = numberOfContactsVIP == 5 && nonVipNotificationsNumbers >= 5
            val strongGreen = numberOfContactsVIP > 1 && isAllOtherContactsSilent

            val adviceMessage = if (strongRed) {
                application.getString(R.string.strong_red_advice)
            } else if (orange) {
                application.getString(R.string.orange_advice)
            } else if (yellow) {
                application.getString(R.string.yellow_advice)
            } else if (lightGreen) {
                application.getString(R.string.light_green_advice)
            } else if (strongGreen) {
                application.getString(R.string.strong_green_advice)
            } else {
                application.getString(R.string.yellow_advice)
            }

            val unprocessedNotifications = (messagingNumbersDaily - vipNumbersDaily) / 2

            emit(
                DailyStatisticsViewState(
                    icon = icon,
                    numberOfNotificationsUnprocessed = application.getString(
                        R.string.x_notifications_received, unprocessedNotifications
                    ),
                    numberOfNotificationsVip = application.getString(R.string.number_vip_notifications, vipNumbersDaily),
                    adviceMessage = adviceMessage
                )
            )
        }.collect()
    }

    private fun compareIfNotificationDateIsToday(timestamp: Long): Boolean {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val date = LocalDateTime.now().format(formatter)

            val notificationDate = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(timestamp))

            val dateToday = date?.split("-")
            val notificationDateToday = notificationDate.split("-")

            val todayYear = dateToday?.get(0)?.toInt()
            val todayMonth = dateToday?.get(1)?.toInt()
            val todayDay = dateToday?.get(2)?.split(" ")?.get(0)
//            val todayHour = dateToday?.get(2)?.split(" ")?.get(1)?.split(":")?.get(0)
//            val todayMinutes = dateToday?.get(2)?.split(" ")?.get(1)?.split(":")?.get(1)

            val notificationYear = notificationDateToday[0].toInt()
            val notificationMonth = notificationDateToday[1].toInt()
            val notificationDay = notificationDateToday[2].split(" ")[0]
//            val notificationHour = dateToday?.get(2)?.split(" ")?.get(1)?.split(":")?.get(0)
//            val notificationMinutes = dateToday?.get(2)?.split(" ")?.get(1)?.split(":")?.get(1)

            return if (notificationYear != todayYear || notificationMonth != todayMonth) {
                false
            } else {
                if (notificationDay == todayDay) {
                    true
                } else {
                    (todayDay?.toInt()?.minus(notificationDay.toInt())) == 1
                }
            }
        } catch (e: Exception) {
            Log.i("GetLocalDateTime", "Exception : ${e}")
        }
        return false
    }

    private fun compareIfNotificationDateIsFromThisWeek(timestamp: Long): Boolean {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val date = LocalDateTime.now().format(formatter)

            val notificationDate = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(timestamp))

            val dateToday = date?.split("-")
            val notificationDateToday = notificationDate.split("-")

            val todayYear = dateToday?.get(0)?.toInt() // 2023
            val todayMonth = dateToday?.get(1)?.toInt() // 01
            val todayDay = dateToday?.get(2)?.split(" ")?.get(0) // 22

            val notificationYear = notificationDateToday[0].toInt()
            val notificationMonth = notificationDateToday[1].toInt()
            val notificationDay = notificationDateToday[2].split(" ")[0]

//            Log.i("GetLocalDateTime", "WEEKLY todayYear : ${todayYear}")
//            Log.i("GetLocalDateTime", "WEEKLY notificationYear : ${notificationYear}")
//
//            Log.i("GetLocalDateTime", "WEEKLY todayMonth : ${todayMonth}")
//            Log.i("GetLocalDateTime", "WEEKLY notificationMonth : ${notificationMonth}")
//
//            Log.i("GetLocalDateTime", "WEEKLY todayDay : ${todayDay}")
//            Log.i("GetLocalDateTime", "WEEKLY notificationDay : ${notificationDay}")

            return if (notificationYear != todayYear) {
                false
            } else {
                if (notificationDay == todayDay && notificationMonth == todayMonth) {
                    true
                } else {
                    if (todayMonth != null && todayDay != null && todayMonth.minus(notificationMonth) > 1) {
                        when (todayDay.toInt()) {
                            1 -> {
                                when (notificationMonth) {
                                    // 31 days months
                                    3, 5, 7, 8, 10, 12 -> {
                                        notificationDay.toInt() > 24
                                    }

                                    // 30 days months
                                    4, 6, 9, 11 -> {
                                        notificationDay.toInt() > 23
                                    }

                                    // 28-29 days months
                                    2 -> {
                                        notificationDay.toInt() > 21
                                    }
                                    else -> {
                                        false
                                    }
                                }
                            }
                            2 -> {
                                when (notificationMonth) {
                                    // 31 days months
                                    3, 5, 7, 8, 10, 12 -> {
                                        notificationDay.toInt() > 25
                                    }

                                    // 30 days months
                                    4, 6, 9, 11 -> {
                                        notificationDay.toInt() > 24
                                    }

                                    // 28-29 days months
                                    2 -> {
                                        notificationDay.toInt() > 22
                                    }
                                    else -> {
                                        false
                                    }
                                }
                            }
                            3 -> {
                                when (notificationMonth) {
                                    // 31 days months
                                    3, 5, 7, 8, 10, 12 -> {
                                        notificationDay.toInt() > 26
                                    }

                                    // 30 days months
                                    4, 6, 9, 11 -> {
                                        notificationDay.toInt() > 25
                                    }

                                    // 28-29 days months
                                    2 -> {
                                        notificationDay.toInt() > 23
                                    }
                                    else -> {
                                        false
                                    }
                                }
                            }
                            4 -> {
                                when (notificationMonth) {
                                    // 31 days months
                                    3, 5, 7, 8, 10, 12 -> {
                                        notificationDay.toInt() > 27
                                    }

                                    // 30 days months
                                    4, 6, 9, 11 -> {
                                        notificationDay.toInt() > 26
                                    }

                                    // 28-29 days months
                                    2 -> {
                                        notificationDay.toInt() > 24
                                    }
                                    else -> {
                                        false
                                    }
                                }
                            }
                            5 -> {
                                when (notificationMonth) {
                                    // 31 days months
                                    3, 5, 7, 8, 10, 12 -> {
                                        notificationDay.toInt() > 28
                                    }

                                    // 30 days months
                                    4, 6, 9, 11 -> {
                                        notificationDay.toInt() > 27
                                    }

                                    // 28-29 days months
                                    2 -> {
                                        notificationDay.toInt() > 25
                                    }
                                    else -> {
                                        false
                                    }
                                }
                            }
                            6 -> {
                                when (notificationMonth) {
                                    // 31 days months
                                    3, 5, 7, 8, 10, 12 -> {
                                        notificationDay.toInt() > 29
                                    }

                                    // 30 days months
                                    4, 6, 9, 11 -> {
                                        notificationDay.toInt() > 28
                                    }

                                    // 28-29 days months
                                    2 -> {
                                        notificationDay.toInt() > 26
                                    }
                                    else -> {
                                        false
                                    }
                                }
                            }
                            7 -> {
                                when (notificationMonth) {
                                    // 31 days months
                                    3, 5, 7, 8, 10, 12 -> {
                                        notificationDay.toInt() > 30
                                    }

                                    // 30 days months
                                    4, 6, 9, 11 -> {
                                        notificationDay.toInt() > 29
                                    }

                                    // 28-29 days months
                                    2 -> {
                                        notificationDay.toInt() > 27
                                    }
                                    else -> {
                                        false
                                    }
                                }
                            }

                            else -> {
                                (todayDay.toInt().minus(notificationDay.toInt())) <= 7
                            }
                        }
                    } else {
                        false
                    }
                }
            }
        } catch (e: Exception) {
            Log.i("GetLocalDateTime", "Exception : ${e}")
        }
        return false
    }

    private fun compareIfNotificationDateIsFromThisMonth(timestamp: Long): Boolean {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val date = LocalDateTime.now().format(formatter)

            val notificationDate = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(timestamp))

            val dateToday = date?.split("-")
            val notificationDateToday = notificationDate.split("-")

            val todayYear = dateToday?.get(0)?.toInt()
            val todayMonth = dateToday?.get(1)?.toInt()

            val notificationYear = notificationDateToday[0].toInt()
            val notificationMonth = notificationDateToday[1].toInt()

            return if (notificationYear != todayYear) {
                false
            } else {
                if (notificationMonth == todayMonth) {
                    true
                } else {
                    todayMonth?.minus(notificationMonth) == 1
                }
            }
        } catch (e: Exception) {
            Log.i("GetLocalDateTime", "Exception : ${e}")
        }
        return false
    }
}