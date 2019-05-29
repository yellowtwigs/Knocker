package com.example.knocker.model

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.UserManager
import android.provider.ContactsContract
import android.util.Base64
import android.widget.GridView
import com.example.knocker.R
import com.example.knocker.model.ModelDB.ContactDB
import com.example.knocker.model.ModelDB.ContactDetailDB
import com.example.knocker.model.ModelDB.ContactWithAllInformation
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.ArrayList
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
/**
 * La Classe qui contient toute les fonctions qui touche à la synchronisation des contacts, les filtre et searchbar
 * @author Florian Striebel, Kenzy Suon, Ryan Granet
 */
class ContactList(var contacts: List<ContactWithAllInformation>,var context:Context) {
    constructor(context: Context) : this(mutableListOf<ContactWithAllInformation>()
            , context)

    private lateinit var mDbWorkerThread: DbWorkerThread
    private var contactsDatabase: ContactsRoomDatabase? = null

    init {
        mDbWorkerThread = DbWorkerThread("dbWorkerThread")
        mDbWorkerThread.start()
        contactsDatabase = ContactsRoomDatabase.getDatabase(context)
        val executorService: ExecutorService = Executors.newFixedThreadPool(1)
        val callDb = Callable {
            contactsDatabase!!.contactsDao()
                    .getContactAllInfo()
        }
        val result = executorService.submit(callDb)
        println("result knocker" + result?.get())
        val tmp: List<ContactWithAllInformation>? = result.get()
        if (tmp!!.isEmpty()) {
            contacts = buildContactListFromJson(context)
        } else {
            contacts = tmp
        }
    }

    fun synchronizedList() {

        val executorService: ExecutorService = Executors.newFixedThreadPool(1)
        val callDb = Callable { contactsDatabase!!.contactsDao().getContactAllInfo() }
        val result = executorService.submit(callDb)
        println("result knocker" + result.get())
        contacts = result.get()
        //TODO verifiy with Ryan

    }

    fun getDetailsOfPlatform(name: String, platform: String): String {
        // on init WorkerThread


        var info = "Platform Error"
        when (platform) {
            "message" -> {
                info = getPhoneNumberWithName(name)
            }
        }
        return info
    }

    fun getPhoneNumberWithName(name: String): String {
        var info = "Name Error"
        if (name.contains(" ")) {
            contacts!!.forEach { dbContact ->
                if (dbContact.contactDB!!.firstName + " " + dbContact.contactDB!!.lastName == name) {
                    info = dbContact.contactDetailList!!.get(0).content.dropLast(1)
                }
            }
        } else {
            contacts!!.forEach { dbContact ->
                if (dbContact.contactDB!!.firstName == name && dbContact.contactDB!!.lastName == "" || dbContact.contactDB!!.firstName == "" && dbContact.contactDB!!.lastName == name) {
                    info = dbContact.contactDetailList!!.get(0).content.dropLast(1)
                }
            }
        }
        return info
    }

    fun getContactById(id:Int):ContactWithAllInformation?{
       for( contact in this.contacts){
           if(contact.contactDB!!.id==id){
               return contact
           }
       }
        return null;
    }

    fun sortContactByFirstNameAZ() {
        val executorService: ExecutorService = Executors.newFixedThreadPool(1)
        val callDb = Callable { contactsDatabase!!.contactsDao().sortContactByFirstNameAZ() }
        val result = executorService.submit(callDb)
        contacts = result.get()
    }

    private fun getAllContactFilter(filterList: ArrayList<String>): List<ContactWithAllInformation>? {
        val allFilters: MutableList<List<ContactWithAllInformation>> = mutableListOf()
        var filter: List<ContactWithAllInformation>?
        //val allContacts = contactsDatabase?.contactsDao()!!.getContactAllInfo()
        println(filterList)
        if (filterList.contains("sms")) {
            val executorService: ExecutorService = Executors.newFixedThreadPool(1)
            val callDb = Callable { contactsDatabase?.contactsDao()?.getContactWithPhoneNumber() }
            val result = executorService.submit(callDb)
            filter = result.get()
            if (filter != null && filter.isEmpty() == false) {
                allFilters.add(filter)
            }
        }
        if (filterList.contains("mail")) {
            val executorService: ExecutorService = Executors.newFixedThreadPool(1)
            val callDb = Callable { contactsDatabase?.contactsDao()?.getContactWithMail() }
            val result = executorService.submit(callDb)
            filter = result.get()
            if (filter != null && filter.isEmpty() == false) {
                allFilters.add(filter)
            }
        }
        if (filterList.isEmpty())
            return null
        var i = 0
        if (allFilters.size > 1) {
            while (i < allFilters.size - 1) {
                allFilters[i + 1] = allFilters[i].intersect(allFilters[i + 1]).toList()
                i++
            }
        } else if (allFilters.size == 0) {
            return null
        } else
            return allFilters[0]
        return allFilters[i]
    }


    fun getContactByName(name: String): List<ContactWithAllInformation> {
        val executorService: ExecutorService = Executors.newFixedThreadPool(1)
        val callDb = Callable { contactsDatabase?.contactsDao()?.getContactByName(name) }
        val result = executorService.submit(callDb)
        return result.get()!!
    }

    private fun intersectContactWithAllInformation(contactList: List<ContactWithAllInformation>, contactFilterList: List<ContactWithAllInformation>): List<ContactWithAllInformation> {
        val listContacts = mutableListOf<ContactWithAllInformation>()
        contactFilterList.forEach { type ->
            contactList.forEach {
                if (type.getContactId() == it.getContactId())
                    listContacts.add(type)
            }
        }
        return listContacts
    }

    fun getContactConcernByFilter(filterList: ArrayList<String>, name: String): List<ContactWithAllInformation> {
        val contactFilterList: List<ContactWithAllInformation>? = getAllContactFilter(filterList)

        val contactList = getContactByName(name)
        if (contactFilterList != null) {
            return intersectContactWithAllInformation(contactList, contactFilterList)
        }
        return contactList
    }


    //region region Creation FakeContact


    fun loadJSONFromAsset(context: Context): String {
        var json = ""
        try {
            json = context.assets.open("premiers_contacts.json").bufferedReader().use {
                it.readText()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
        return json
    }

    fun buildContactListFromJson(context: Context): List<ContactWithAllInformation> {
        var listContacts = mutableListOf<ContactWithAllInformation>()
        var contactString = loadJSONFromAsset(context)
        try {
            val jsArray = JSONArray(contactString)
            for (x in 0..(jsArray.length() - 1)) {
                listContacts.add(getContactFromJSONObject(jsArray.getJSONObject(x), x))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return listContacts
    }

    fun getContactFromJSONObject(json: JSONObject, id: Int): ContactWithAllInformation {
        val firstName: String = json.getString("first_name")
        val lastName: String = json.getString("last_name")

        val profilPicture: Int = R.drawable.img_avatar
        val contactPriority: Int = json.getInt("contact_priority")
        val profilPictureStr: String = json.getString("profile_picture_str")

        println("contact :" + firstName + " " + lastName)
        val contact = ContactDB(id, firstName, lastName, profilPicture, contactPriority, profilPictureStr)
        val contactInfo = ContactWithAllInformation()
        contactInfo.contactDB = contact
        contactInfo.contactDetailList = getContactDeatailFromJSONObject(json, id)
        return contactInfo
    }

    fun getContactDeatailFromJSONObject(json: JSONObject, idContact: Int): List<ContactDetailDB> {
        val phoneNumber: String = json.getString("phone_number")
        val mail: String = json.getString("mail")
        val contactDetails = ContactDetailDB(null, idContact, phoneNumber + "M", "phone", "", 0)
        val contactDetails2 = ContactDetailDB(null, idContact, mail + "B", "mail", "", 1)
        return mutableListOf<ContactDetailDB>(contactDetails, contactDetails2)
    }
    /*   fun getContactId(id:Int): ContactWithAllInformation? {
        for (contactJSON in contacts){
            if(contactJSON.contactDB!!.id==id){
                return contactJSON
            }
        }
        return null
    }*/

    //endregion

    //region region ContactSync

    private fun isDuplicate(id: Int, contactPhoneNumber: List<Pair<Int, Triple<String, String, String?>>>): Boolean {
        contactPhoneNumber.forEach {
            if (it.first == id)
                return true
        }
        return false
    }

    private fun getStructuredNameSync(main_contentResolver: ContentResolver): List<Pair<Int, Triple<String, String, String>>>? {
        val phoneContactsList = arrayListOf<Pair<Int, Triple<String, String, String>>>()
        var idAndName: Pair<Int, Triple<String, String, String>>
        var StructName: Triple<String, String, String>
        val phonecontact = main_contentResolver.query(ContactsContract.Data.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)
        if (phonecontact != null) {
            while (phonecontact.moveToNext()) {
                val phone_id = phonecontact.getString(phonecontact.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID)).toInt()
                var firstName = phonecontact.getString(phonecontact.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME))
                var middleName = phonecontact.getString(phonecontact.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME))
                var lastName = phonecontact.getString(phonecontact.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME))
                val mimeType = phonecontact.getString(phonecontact.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIMETYPE))
                if (phoneContactsList.isEmpty() && mimeType == "vnd.android.cursor.item/name") {
                    if (firstName == null)
                        firstName = ""
                    if (middleName == null)
                        middleName = ""
                    if (lastName == null)
                        lastName = ""
                    StructName = Triple(firstName, middleName, lastName)
                    idAndName = Pair(phone_id, StructName)
                    phoneContactsList.add(idAndName)
                } else if (!isDuplicate(phone_id, phoneContactsList) && mimeType == "vnd.android.cursor.item/name") {
                    if (firstName == null)
                        firstName = ""
                    if (middleName == null)
                        middleName = ""
                    if (lastName == null)
                        lastName = ""
                    StructName = Triple(firstName, middleName, lastName)
                    idAndName = Pair(phone_id, StructName)
                    phoneContactsList.add(idAndName)
                }
            }
        }
        phonecontact?.close()
        println("ok ?= " + phoneContactsList)
        return phoneContactsList
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val imageBytes = baos.toByteArray()

        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private fun isDuplicateNumber(idAndPhoneNumber: Map<Int, Any>, contactPhoneNumber: List<Map<Int, Any>>): Boolean {
        contactPhoneNumber.forEach {
            if (it[1] == idAndPhoneNumber[1] && it[2] == idAndPhoneNumber[2])
                return true
        }
        return false
    }

    private fun openPhoto(contactId: Long, main_contentResolver: ContentResolver): InputStream? {
        val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        val photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
        val cursor = main_contentResolver.query(photoUri,
                arrayOf(ContactsContract.Contacts.Photo.PHOTO), null, null, null) ?: return null
        try {
            if (cursor.moveToFirst()) {
                val data = cursor.getBlob(0)
                if (data != null) {
                    return ByteArrayInputStream(data)
                }
            }
        } finally {
            cursor.close()
        }
        return null
    }

    private fun getContactMailSync(main_contentResolver: ContentResolver): List<Map<Int, Any>> {
        val contactDetails = arrayListOf<Map<Int, Any>>()
        var idAndMail = mapOf<Int, Any>()
        val phonecontact = main_contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Email.DISPLAY_NAME + " ASC")
        while (phonecontact.moveToNext()) {
            val phoneId = phonecontact?.getString(phonecontact.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID))
            var phoneEmail = phonecontact?.getString(phonecontact.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS))
            val phoneTag = phonecontact?.getString(phonecontact.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE))
            if (phoneEmail == null)
                phoneEmail = ""
            idAndMail = mapOf(1 to phoneId!!.toInt(), 2 to phoneEmail, 3 to assignTagEmail(phoneTag!!.toInt()), 4 to "")
            println("FINALU STRAIKE ! = " + idAndMail)
            if (contactDetails.isEmpty() || !isDuplicateNumber(idAndMail, contactDetails)) {
                contactDetails.add(idAndMail)
            }
        }
        phonecontact?.close()
        return contactDetails
    }

    private fun getPhoneNumberSync(main_contentResolver: ContentResolver): List<Map<Int, Any>> {
        val contactPhoneNumber = arrayListOf<Map<Int, Any>>()
        var idAndPhoneNumber = mapOf<Int, Any>()
        val phonecontact = main_contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")
        while (phonecontact.moveToNext()) {
            val phoneId = phonecontact?.getString(phonecontact.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
            var phoneNumber = phonecontact?.getString(phonecontact.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            var phonePic = phonecontact?.getString(phonecontact.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))
            val phoneTag = phonecontact?.getString(phonecontact.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE))
            println("phone numberrrr = " + phoneNumber)
            if (phoneNumber == null)
                phoneNumber = ""
            if (phonePic == null || phonePic.contains("content://com.android.contacts/contacts/", ignoreCase = true)) {
                phonePic = ""
            } else {
                println("phone pic" + phonePic)
                println("openPhoto " + phoneId!!.toLong() + " main content resolver " + main_contentResolver) // huwei content://com.android.contacts/contacts/1600/photo
                phonePic = bitmapToBase64(BitmapFactory.decodeStream(openPhoto(phoneId.toLong(), main_contentResolver)))
            }
            //idAndPhoneNumber = Triple(phoneId!!.toInt(), phoneNumber, phonePic)
            idAndPhoneNumber = mapOf(1 to phoneId!!.toInt(), 2 to phoneNumber, 3 to assignTagNumber(phoneTag!!.toInt()), 4 to phonePic)
            println("FINAL STRIKE = " + idAndPhoneNumber)
            if (contactPhoneNumber.isEmpty() || !isDuplicateNumber(idAndPhoneNumber, contactPhoneNumber)) {
                //println("1er = "+idAndPhoneNumber)
                contactPhoneNumber.add(idAndPhoneNumber)
            }
        }
        phonecontact?.close()
        println("number ?= " + contactPhoneNumber)
        return contactPhoneNumber
    }

    private fun assignTagEmail(intTag: Int): String {
        var tag = "other"
        when (intTag) {
            1 -> tag = "home"
            2 -> tag = "work"
        }
        return tag
    }

    private fun assignTagNumber(intTag: Int): String {
        var tag = "other"
        when (intTag) {
            1 -> tag = "home"
            2 -> tag = "mobil"
            3 -> tag = "work"
        }
        return tag
    }

    private fun randomDefaultImage(): Int {

        val nextValues = kotlin.random.Random.nextInt(0, 10)
        var randomUserImage = 0

        when (nextValues) {
            0 -> randomUserImage = R.drawable.ic_user_black
            1 -> randomUserImage = R.drawable.ic_user_blue
            2 -> randomUserImage = R.drawable.ic_user_brown
            3 -> randomUserImage = R.drawable.ic_user_green
            4 -> randomUserImage = R.drawable.ic_user_grey
            5 -> randomUserImage = R.drawable.ic_user_om
            6 -> randomUserImage = R.drawable.ic_user_orange
            7 -> randomUserImage = R.drawable.ic_user_pink
            8 -> randomUserImage = R.drawable.ic_user_purple
            9 -> randomUserImage = R.drawable.ic_user_red
            10 -> randomUserImage = R.drawable.ic_user_yellow
        }

        return randomUserImage
    }

    private fun isDuplicate(contacts: List<ContactWithAllInformation>?, phoneContactList: ContactDB): Boolean {
        contacts?.forEach { contactsInfo ->
            val contactsDB = contactsInfo.contactDB!!
            if (contactsDB.firstName == phoneContactList.firstName && contactsDB.lastName == phoneContactList.lastName)
                return true
        }
        return false//TODO
    }

    fun getLastSync(applicationContext: Context): List<Pair<Int, String>> {
        val lastSyncList = arrayListOf<Pair<Int, String>>()
        var idAndName: Pair<Int, String>
        val sharedPreferences = applicationContext.getSharedPreferences("save_last_sync", Context.MODE_PRIVATE)
        val lastSync = sharedPreferences.getString("last_sync", "")
        if (lastSync != null && lastSync != "" && lastSync.contains("|")) {
            val lastSyncSplit = lastSync.split("|")
            lastSyncSplit.forEach {
                if (it != "") {
                    val idAndNameSplit = it.split(":")
                    idAndName = Pair(idAndNameSplit[0].toInt(), idAndNameSplit[1])
                    lastSyncList.add(idAndName)
                }
            }
        } else if (lastSync != null && lastSync != "") {
            val idAndNameSplit = lastSync.split(":")
            idAndName = Pair(idAndNameSplit[0].toInt(), idAndNameSplit[1])
            lastSyncList.add(idAndName)
        }
        return lastSyncList
    }

    fun storeLastSync(contactsList: List<ContactDB>, applicationContext: Context, lastSync: List<Pair<Int, String>>, isFirstTime: Boolean) {
        val sharedPreferences = applicationContext.getSharedPreferences("save_last_sync", Context.MODE_PRIVATE)
        val edit: SharedPreferences.Editor = sharedPreferences.edit()
        var name = ""
        if (isFirstTime == true) {
            contactsList.forEach {
                name += it.id.toString() + ":" + it.firstName + " " + it.lastName + "|"
            }
            edit.putString("last_sync", name)
            edit.apply()
        } else {
            contactsList.forEach {
                if (it.id == null) {
                    var i = 0
                    while (i != (lastSync.size) - 1 && it.firstName + " " + it.lastName != lastSync[i].second) {
                        i++
                    }
                    if (i != (lastSync.size) && it.firstName + " " + it.lastName == lastSync[i].second) {
                        name += lastSync[i].first.toString() + ":" + it.firstName + " " + it.lastName + "|"
                    }
                } else {
                    name += it.id.toString() + ":" + it.firstName + " " + it.lastName + "|"
                }
            }
            edit.putString("last_sync", name)
            edit.apply()
        }
    }

    fun deleteDeletedContactFromPhone(lastSync: List<Pair<Int, String>>, newSync: List<ContactDB>) {
        var contactsDatabase: ContactsRoomDatabase? = null
        //var id: Int? = null
        var isDelete: Boolean
        lastSync.forEach { old ->
            isDelete = true
            newSync.forEach {
                if (it.firstName + " " + it.lastName == old.second)
                    isDelete = false
            }
            if (isDelete == true) {
                println("NEW = " + old.second + "   id = " + old.first)
                contactsDatabase?.contactsDao()?.deleteContactById(old.first)
            }
        }
    }

    private fun getDetailsById(id: Int, contactNumberAndPic: List<Map<Int, Any>>): List<ContactDetailDB> {
        val contactDetails = arrayListOf<ContactDetailDB>()
        var fieldPosition = 0
        contactNumberAndPic.forEach {
            if (it[1] == id) {
                contactDetails.add(ContactDetailDB(null, null, it[2].toString() + "M", "phone", it[3].toString(), fieldPosition))
                fieldPosition++
            }
        }
        return contactDetails
    }

    fun createListContactsSync(phoneStructName: List<Pair<Int, Triple<String, String, String>>>?, contactNumberAndPic: List<Map<Int, Any>>, gridView: GridView?, applicationContext: Context, gestionnaireContacts: ContactList) {
        val phoneContactsList = arrayListOf<ContactDB>()
        val lastId = arrayListOf<Int>()
        val executorService: ExecutorService = Executors.newFixedThreadPool(1)
        val callDb = Callable {
            val allcontacts = contactsDatabase?.contactsDao()?.sortContactByFirstNameAZ()
            phoneStructName!!.forEach { fullName ->
                contactNumberAndPic.forEach { numberPic ->
                    val id = numberPic[1].toString().toInt()
                    if (!lastId.contains(id)) {
                        val contactDetails = getDetailsById(id, contactNumberAndPic)
                        if (fullName.first == numberPic[1]) {
                            lastId.add(id)
                            if (fullName.second.second == "") {
                                val contacts = ContactDB(null, fullName.second.first, fullName.second.third, randomDefaultImage(), 1, numberPic[4]!!.toString())
                                if (!isDuplicate(allcontacts, contacts)) {

                                    contacts.id = contactsDatabase?.contactsDao()?.insert(contacts)!!.toInt()
                                    for (details in contactDetails) {
                                        details.idContact = contacts.id
                                    }
                                    contactsDatabase!!.contactsDao().insertDetails(contactDetails)
                                }
                                phoneContactsList.add(contacts)
                            } else if (fullName.second.second != "") {
                                val contacts = ContactDB(null, fullName.second.first, fullName.second.second + " " + fullName.second.third, randomDefaultImage(), 1, numberPic[4]!!.toString())
                                phoneContactsList.add(contacts)
                                if (!isDuplicate(allcontacts, contacts)) {
                                    contacts.id = contactsDatabase?.contactsDao()?.insert(contacts)!!.toInt()
                                    for (details in contactDetails) {
                                        details.idContact = contacts.id
                                    }
                                    contactsDatabase!!.contactsDao().insertDetails(contactDetails)
                                }
                            } else {
                            }
                        }
                    }
                }
            }
            contactsDatabase?.contactsDao()?.getContactAllInfo()
        }
        val syncContact = executorService.submit(callDb).get()
        val lastSyncList = getLastSync(applicationContext)
        if (lastSyncList.isEmpty()) {
            storeLastSync(phoneContactsList, applicationContext, lastSyncList, true)
        } else {
            deleteDeletedContactFromPhone(lastSyncList, phoneContactsList)
            storeLastSync(phoneContactsList, applicationContext, lastSyncList, false)
        }
        gestionnaireContacts.contacts = syncContact!!

    }

    private fun getContactGroupSync(main_contentResolver: ContentResolver): List<Triple<Int, String?, String?>> {
        val phoneContact = main_contentResolver.query(ContactsContract.Groups.CONTENT_URI, null, null, null, ContactsContract.Groups.TITLE + " ASC")
        var allGroupMembers = listOf<Triple<Int, String?, String?>>()
        while (phoneContact.moveToNext()) {
            val groupId = phoneContact?.getString(phoneContact.getColumnIndex(ContactsContract.Groups._ID))
            val groupName = phoneContact?.getString(phoneContact.getColumnIndex(ContactsContract.Groups.TITLE))
            val groupMembers = getMemberOfGroup(main_contentResolver, groupId.toString(), groupName)
            if (!groupMembers.isEmpty() && !allGroupMembers.isEmpty() && !isDuplicateGroup(allGroupMembers, groupMembers)) {
                allGroupMembers = allGroupMembers.union(groupMembers).toList()
            } else if (allGroupMembers.isEmpty())
                allGroupMembers = groupMembers
        }
        phoneContact?.close()
        return allGroupMembers
    }

    private fun getMemberOfGroup(main_contentResolver: ContentResolver, groupId: String, groupeName: String?): List<Triple<Int,String?,String?>> {
        val where = ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + "=" + groupId
        val phoneContact = main_contentResolver.query(ContactsContract.Data.CONTENT_URI, null, where, null, ContactsContract.Data.DISPLAY_NAME + " ASC")
        var member: Triple<Int, String?, String?>
        val groupMembers = arrayListOf<Triple<Int, String?, String?>>()
        while (phoneContact.moveToNext()) {
            val contactId = phoneContact?.getString(phoneContact.getColumnIndex(ContactsContract.Data.CONTACT_ID))
            val contactName = phoneContact?.getString(phoneContact.getColumnIndex(ContactsContract.Data.DISPLAY_NAME))
            member = Triple(contactId!!.toInt(), contactName, groupeName)
            if (!groupMembers.contains(member)) {
                groupMembers.add(member)
            }
        }
        phoneContact?.close()
        return groupMembers
    }

    private fun isDuplicateGroup(member: List<Triple<Int, String?, String?>>, groupMembers: List<Triple<Int, String?, String?>>): Boolean{
        groupMembers.forEach {
            if (it.third == member[1].third)
                return true
        }
        return false
    }

    fun getAllContacsInfoSync(main_contentResolver: ContentResolver, gridView: GridView?, applicationContext: Context) {
        val phoneStructName = getStructuredNameSync(main_contentResolver)
        println("PHONE STRUCT = "+phoneStructName)
        val contactNumberAndPic = getPhoneNumberSync(main_contentResolver)
        val contactMail = getContactMailSync(main_contentResolver)
//        val contactGroup = getContactGroupSync(main_contentResolver)
        val contactDetail = contactNumberAndPic.union(contactMail)
        createListContactsSync(phoneStructName, contactDetail.toList(), gridView, applicationContext, this)
    }
//endregion


    fun getPriorityWithName(name: String, platform: String): Int {
        var priority = -2
        when (platform) {
            "message" -> {
                priority = getPriority(name)
            }
            "WhatsApp" -> {
                priority = getPriority(name)
            }
            "gmail" -> {
                priority = getPriority(name)
            }
        }
        return priority
    }

    // get la priorité grace à la liste
    fun getPriority(name: String): Int {
        if (name.contains(" ")) {
            this.contacts!!.forEach { dbContact ->
                val contactInfo = dbContact.contactDB!!
                //                println("contact "+dbContact+ "différent de name"+name)
                if (contactInfo.firstName + " " + contactInfo.lastName == name) {
                    return contactInfo.contactPriority
                }
            }
        } else {
            this.contacts!!.forEach { dbContact ->
                val contactInfo = dbContact.contactDB!!
                if (contactInfo.firstName == name && contactInfo.lastName == "" || contactInfo.firstName == "" && contactInfo.lastName == name) {
                    return contactInfo.contactPriority
                }
            }
        }
        return -1
    }

    fun getContactId(name: String): Int {
        if (name.contains(" ")) {
            this.contacts!!.forEach { dbContact ->
                val contactInfo = dbContact.contactDB!!
                //                println("contact "+dbContact+ "différent de name"+name)
                if (contactInfo.firstName + " " + contactInfo.lastName == name) {
                    return contactInfo.contactPriority
                }
            }
        } else {
            this.contacts!!.forEach { dbContact ->
                val contactInfo = dbContact.contactDB!!
                if (contactInfo.firstName == name && contactInfo.lastName == "" || contactInfo.firstName == "" && contactInfo.lastName == name) {
                    return contactInfo.contactPriority
                }
            }
        }
        return 0
    }
}