package com.yellowtwigs.knockin.models.data

import android.content.Context
import androidx.room.Embedded
import androidx.room.Relation
import com.yellowtwigs.knockin.models.AppDatabase

class GroupWithContact {
    fun getListContact(context: Context): ArrayList<ContactWithAllInformation> {
        val contactRoom = AppDatabase.getDatabase(context)
        val listContact: ArrayList<ContactWithAllInformation> = arrayListOf()
        for (idContact in ContactIdList!!) {
            listContact.add(contactRoom!!.contactsDao().getContact(idContact))
        }
        return listContact
    }

    @Embedded
    var groupDB: GroupDB? = null

    @Relation(parentColumn = "id", entityColumn = "id_group", entity = LinkContactGroup::class, projection = ["id_contact"])
    var ContactIdList: List<Int>? = null

    //var contactList:List<ContactDB>?= null

}