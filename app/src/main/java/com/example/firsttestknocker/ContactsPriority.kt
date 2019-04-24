package com.example.firsttestknocker

import android.os.Build
import android.provider.Settings
import android.support.v7.app.AppCompatActivity

object ContactsPriority : AppCompatActivity() {

    // fonction qui recupere la priorité grâce au nom du contact et la plateforme
    fun getPriorityWithName(name: String, platform: String, listContact: List<Contacts>?): Int {
        var priority = 1
        when (platform) {
            "message" -> {
                // jean, jean michel, jean michel pelletier
                priority = getPriority(name,listContact)
            }
        }
        return priority
    }

    // get la priorité grace à la liste
    fun getPriority(name: String, listContact: List<Contacts>?): Int {
        var priority = -1
        if (name.contains(" ")) {
            listContact!!.forEach { dbContact ->
                if (dbContact.firstName+" "+dbContact.lastName == name) { //contain or == |jean michel pellier && michel pellier !=
                    priority = dbContact.contactPriority
                }
            }
        } else {
            listContact!!.forEach { dbContact ->
                if (dbContact.firstName == name && dbContact.lastName == "" || dbContact.firstName == "" && dbContact.lastName == name) {
                    priority = dbContact.contactPriority
                }
            }
        }
        return priority
    }

    fun checkPriority2(contactList: List<Contacts>?): Boolean {
        if (contactList != null) {
            for (contact in contactList){
               if(contact.contactPriority==2){
                   return true
               }
            }
        }
        return false
    }

}