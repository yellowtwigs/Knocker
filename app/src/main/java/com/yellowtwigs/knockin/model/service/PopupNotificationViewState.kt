package com.yellowtwigs.knockin.model.service

import com.yellowtwigs.knockin.ui.add_edit_contact.edit.PhoneNumberWithSpinner

data class PopupNotificationViewState(
    val id: Int,
    val title: String,
    val description: String,
    val platform: String,
    val contactName: String,
    val date: String,
    val listOfPhoneNumbersWithSpinner: List<PhoneNumberWithSpinner>,
    val messengerId: String,
    val email: String
)