@file:Suppress("NAME_SHADOWING")

package com.example.knocker.controller

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.util.Base64
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.example.knocker.*
import com.example.knocker.model.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.example.knocker.model.ModelDB.ContactDB
import com.example.knocker.model.ModelDB.ContactWithAllInformation
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ContactDetailsActivity : AppCompatActivity() {

    // Contact's informations
    private var contact_details_FirstName: TextView? = null
    private var contact_details_LastName: TextView? = null
    private var contact_details_PhoneNumberSMS: TextView? = null
    private var contact_details_PhoneNumberCall: TextView? = null
    private var contact_details_PhoneNumberSMSProperty: TextView? = null
    private var contact_details_phoneNumberCallProperty: TextView? = null
    private var contact_details_MailProperty: TextView? = null
    private var contact_details_Mail: TextView? = null
    private var contact_details_RoundedImageView: ImageView? = null
    private var contact_details_ContactImageBackgroundImage: ImageView? = null

    // Floating Button
    private var contact_details_FloatingButtonOpen: FloatingActionButton? = null
    private var contact_details_FloatingButtonEdit: FloatingActionButton? = null
    private var contact_details_FloatingButtonDelete: FloatingActionButton? = null

    private var contact_details_FloatingButtonOpenAnimation: Animation? = null
    private var contact_details_FloatingButtonCloseAnimation: Animation? = null
    private var contact_details_FloatingButtonClockWiserAnimation: Animation? = null
    private var contact_details_FloatingButtonAntiClockWiserAnimation: Animation? = null

    internal var contact_details_IsOpen = false

    private var contact_details_phone_number_SMS_RelativeLayout: RelativeLayout? = null
    private var contact_details_phone_number_Call_RelativeLayout: RelativeLayout? = null
    private var contact_details_mail_RelativeLayout: RelativeLayout? = null
    private var contact_details_messenger_RelativeLayout: RelativeLayout? = null
    private var contact_details_whatsapp_RelativeLayout: RelativeLayout? = null
    private var contact_details_instagram_RelativeLayout: RelativeLayout? = null
    private var contact_details_telegram_RelativeLayout: RelativeLayout? = null
    private var contact_details_linkedin_RelativeLayout: RelativeLayout? = null
    private var contact_details_twitter_RelativeLayout: RelativeLayout? = null

    private var contact_details_id: Int? = null
    private var contact_details_first_name: String? = null
    private var contact_details_last_name: String? = null
    private var contact_details_phone_number: String? = null
    private var contact_details_phone_property: String? = null
    private var contact_details_mail: String? = null
    private var contact_details_mail_property: String? = null

    private var contact_details_facebook_id: String? = null
    private var contact_details_twitter_id: String? = null
    private var contact_details_skype_id: String? = null
    private var contact_details_telegram_id: String? = null
    private var contact_details_linkedin_id: String? = null
    private var contact_details_instagram_id: String? = null

    private var contact_details_rounded_image: Int = 0
    private var contact_details_image64: String? = null
    private var contact_details_priority: Int = 1

    // Database && Thread
    private var contact_details_ContactsDatabase: ContactsRoomDatabase? = null
    private lateinit var contact_details_mDbWorkerThread: DbWorkerThread

    // Alert Dialog SMS & Phone Call
    private val SEND_SMS_PERMISSION_REQUEST_CODE = 111

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_details)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)

        // on init WorkerThread
        contact_details_mDbWorkerThread = DbWorkerThread("dbWorkerThread")
        contact_details_mDbWorkerThread.start()

        //on get la base de données
        contact_details_ContactsDatabase = ContactsRoomDatabase.getDatabase(this)

        // Contact's informations, link between Layout and the Activity
        contact_details_FirstName = findViewById(R.id.contact_details_first_name_id)
        contact_details_LastName = findViewById(R.id.contact_details_last_name_id)
        contact_details_PhoneNumberSMS = findViewById(R.id.contact_details_phone_number_sms_text_id)
        contact_details_PhoneNumberCall = findViewById(R.id.contact_details_phone_number_call_text_id)
        contact_details_PhoneNumberSMSProperty = findViewById(R.id.contact_details_phone_sms_property_text_id)
        contact_details_phoneNumberCallProperty = findViewById(R.id.contact_details_phone_call_property_text_id)
        contact_details_Mail = findViewById(R.id.contact_details_mail_id)
        contact_details_RoundedImageView = findViewById(R.id.contact_details_rounded_image_view_id)
        contact_details_ContactImageBackgroundImage = findViewById(R.id.contact_details_background_image_id)
        contact_details_MailProperty = findViewById(R.id.contact_details_mail_property_id)


        // Create the Intent, and get the data from the GridView
        val intent = intent
        contact_details_id = intent.getIntExtra("ContactId", 1)


        if (contact_details_ContactsDatabase?.contactsDao()?.getContact(contact_details_id!!.toInt()) == null) {
            var contactList: List<ContactWithAllInformation>?
            val contactString= FakeContact.loadJSONFromAsset(this)
            contactList= FakeContact.buildList(contactString)

            var contact= FakeContact.getContactId(contact_details_id!!,contactList)!!
            contact_details_first_name = contact.contactDB!!.firstName
            contact_details_last_name = contact.contactDB!!.lastName
            var tmpPhone=contact.contactDetailList!!.get(0)
            contact_details_phone_number = NumberAndMailDB.numDBAndMailDBtoDisplay(tmpPhone.contactDetails)
            contact_details_phone_property = NumberAndMailDB.extractStringFromNumber(tmpPhone.contactDetails)
            var tmpMail=contact.contactDetailList!!.get(1)
            contact_details_mail = NumberAndMailDB.numDBAndMailDBtoDisplay(tmpMail.contactDetails)
            contact_details_mail_property= NumberAndMailDB.extractStringFromNumber(tmpMail.contactDetails)
            contact_details_rounded_image = intent.getIntExtra("ContactImage", 1)
            contactList = FakeContact.buildList(contactString)
            contact_details_image64 = contact.contactDB!!.profilePicture64
            contact_details_RoundedImageView!!.setImageBitmap(base64ToBitmap(contact_details_image64.toString()))

            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            val actionbar = supportActionBar
            actionbar!!.setDisplayHomeAsUpEnabled(true)
            actionbar.setHomeAsUpIndicator(R.drawable.ic_left_arrow)
            println("contact name = " + contact_details_first_name)
            println("contact last name = " + contact_details_last_name)
            println("contact image = " + contact_details_rounded_image)
            actionbar.setTitle("Détails du contact " + contact_details_first_name!!)

        } else {
                val executorService:ExecutorService = Executors.newFixedThreadPool(1)
                val callDb= Callable { contact_details_ContactsDatabase!!.contactsDao().getContact(contact_details_id!!) }
                val result=executorService.submit(callDb)
                val contact:ContactWithAllInformation = result.get()
                println("contact "+contact.contactDetailList + " taille "+contact.contactDetailList!!.size)
                println("contact db"+contact.contactDB)
                contact_details_first_name = contact.contactDB!!.firstName
                contact_details_last_name = contact.contactDB!!.lastName

                if(contact.contactDetailList!!.size==0){
                    print("if 0")
                    contact_details_phone_property=""
                    contact_details_phone_number=""
                    contact_details_mail=""
                    contact_details_mail_property=""
                }else if(contact.contactDetailList!!.size==1){
                    print("if 1 ou 2")
                    var tmpPhone = contact.contactDetailList!!.get(0)
                    println(" test méthode "+tmpPhone.contactDetails)
                    contact_details_phone_number = NumberAndMailDB.numDBAndMailDBtoDisplay(tmpPhone.contactDetails)
                    contact_details_phone_property = NumberAndMailDB.extractStringFromNumber(tmpPhone.contactDetails)
                    contact_details_mail=""
                    contact_details_mail_property=""

                }else if(contact.contactDetailList!!.size == 2) {
                            print("if 2")

                            var tmpPhone = contact.contactDetailList!!.get(0)
                            var tmpMail = contact.contactDetailList!!.get(1)
                            contact_details_phone_number = NumberAndMailDB.numDBAndMailDBtoDisplay(tmpPhone.contactDetails)
                            contact_details_phone_property = NumberAndMailDB.extractStringFromNumber(tmpPhone.contactDetails)
                            contact_details_mail = NumberAndMailDB.numDBAndMailDBtoDisplay(tmpMail.contactDetails)
                            contact_details_mail_property = NumberAndMailDB.extractStringFromNumber(tmpMail.contactDetails)
                        }

                val contactDB = contact_details_ContactsDatabase?.contactsDao()?.getContact(contact_details_id!!)
                contact_details_image64 = contactDB!!.contactDB!!.profilePicture64
                if (contact_details_image64 == "") {
                    println(" contact detail ======= " + contact_details_rounded_image)
                    contact_details_RoundedImageView!!.setImageResource(contact_details_rounded_image)
                } else {
                    val image64 = contact_details_image64
                    contact_details_RoundedImageView!!.setImageBitmap(base64ToBitmap(image64!!))
                }
                val toolbar = findViewById<Toolbar>(R.id.toolbar)
                setSupportActionBar(toolbar)
                val actionbar = supportActionBar
                actionbar!!.setDisplayHomeAsUpEnabled(true)
                actionbar.setHomeAsUpIndicator(R.drawable.ic_left_arrow)
                println("contact name = " + contact_details_first_name)
                println("contact last name = " + contact_details_last_name)
                println("contact image = " + contact_details_rounded_image)
                actionbar.setTitle("Détails du contact " + contact_details_first_name!!)


        }

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)
        actionbar.setHomeAsUpIndicator(R.drawable.ic_left_arrow)

        println("contact name = " + contact_details_first_name)
        println("contact last name = " + contact_details_last_name)
        println("contact image = " + contact_details_rounded_image)
        actionbar.setTitle("Détails du contact " + contact_details_first_name!!)


        // RelativeLayout to link with other SM's apps, link between Layout and the Activity
        contact_details_phone_number_SMS_RelativeLayout = findViewById(R.id.contact_details_phone_number_sms_relative_layout_id)
        contact_details_phone_number_Call_RelativeLayout = findViewById(R.id.contact_details_phone_number_call_relative_layout_id)
        contact_details_messenger_RelativeLayout = findViewById(R.id.contact_details_messenger_relative_layout_id)
        contact_details_whatsapp_RelativeLayout = findViewById(R.id.contact_details_whatsapp_relative_layout_id)
        contact_details_instagram_RelativeLayout = findViewById(R.id.contact_details_instagram_relative_layout_id)
        contact_details_mail_RelativeLayout = findViewById(R.id.contact_details_mail_relative_layout_id)

        // Floating Button link between Layout and the Activity
        contact_details_FloatingButtonOpen = findViewById(R.id.contact_details_floating_button_open_id)
        contact_details_FloatingButtonEdit = findViewById(R.id.contact_details_floating_button_edit_id)
        contact_details_FloatingButtonDelete = findViewById(R.id.contact_details_floating_button_delete_id)
        contact_details_FloatingButtonOpenAnimation = AnimationUtils.loadAnimation(applicationContext, R.anim.fab_open)
        contact_details_FloatingButtonCloseAnimation = AnimationUtils.loadAnimation(applicationContext, R.anim.fab_close)
        contact_details_FloatingButtonClockWiserAnimation = AnimationUtils.loadAnimation(applicationContext, R.anim.rotate_clockwiser)
        contact_details_FloatingButtonAntiClockWiserAnimation = AnimationUtils.loadAnimation(applicationContext, R.anim.rotate_anticlockwiser)


        if (!contact_details_phone_number.isNullOrEmpty()) {

            contact_details_phone_number_SMS_RelativeLayout!!.visibility = View.VISIBLE
            contact_details_whatsapp_RelativeLayout!!.visibility = View.VISIBLE
            contact_details_phone_number_Call_RelativeLayout!!.visibility = View.VISIBLE
        }
        if (!contact_details_mail.isNullOrEmpty()) {
            contact_details_mail_RelativeLayout!!.visibility = View.VISIBLE
        }
        if (!contact_details_facebook_id.isNullOrEmpty()) {
            contact_details_messenger_RelativeLayout!!.visibility = View.VISIBLE
        }
        if (!contact_details_instagram_id.isNullOrEmpty()) {
            contact_details_instagram_RelativeLayout!!.visibility = View.VISIBLE
        }
        if (!contact_details_twitter_id.isNullOrEmpty()) {
            contact_details_twitter_RelativeLayout!!.visibility = View.VISIBLE
        }
        if (!contact_details_linkedin_id.isNullOrEmpty()) {
            contact_details_linkedin_RelativeLayout!!.visibility = View.VISIBLE
        }
        if (!contact_details_telegram_id.isNullOrEmpty()) {
            contact_details_telegram_RelativeLayout!!.visibility = View.VISIBLE
        }

        // Set Resources from MainActivity to ContactDetailsActivity
        println("ressources"+contact_details_first_name+"  "+ contact_details_last_name)
        contact_details_FirstName!!.text = contact_details_first_name
        contact_details_LastName!!.text = contact_details_last_name
        contact_details_PhoneNumberSMS!!.text = contact_details_phone_number
        contact_details_PhoneNumberCall!!.text = contact_details_phone_number
        contact_details_PhoneNumberSMSProperty!!.text = contact_details_phone_property
        contact_details_phoneNumberCallProperty!!.text = contact_details_phone_property
        contact_details_Mail!!.text = contact_details_mail
        contact_details_MailProperty!!.text = contact_details_mail_property

        // The click for the animation
        contact_details_FloatingButtonOpen!!.setOnClickListener {
            if (contact_details_IsOpen) {
                onFloatingClickBack()
                contact_details_IsOpen = false
            } else {
                onFloatingClick()
                contact_details_IsOpen = true
            }
        }

        // Link to Whatsapp contact chat
        contact_details_whatsapp_RelativeLayout!!.setOnClickListener {
            ContactGesture.openWhatsapp(contact_details_PhoneNumberSMS!!.text, this)
        }

        // Floating button, edit a contact
        contact_details_FloatingButtonEdit!!.setOnClickListener {

            val intent = Intent(this@ContactDetailsActivity, EditContactActivity::class.java)

            println("imaaage befor send to edit = " + contact_details_rounded_image)
            // Creation of a intent to transfer data's contact from ContactDetailDB to EditContact
            intent.putExtra("ContactFirstName", contact_details_first_name)
            intent.putExtra("ContactLastName", contact_details_last_name)
            intent.putExtra("ContactPhoneNumber", contact_details_phone_number + NumberAndMailDB.convertSpinnerStringToChar(contact_details_phone_property!!))
            intent.putExtra("ContactImage", contact_details_rounded_image)
            intent.putExtra("ContactId", contact_details_id!!)
            intent.putExtra("ContactMail", contact_details_mail!! + NumberAndMailDB.convertSpinnerStringToChar(contact_details_mail_property!!))
            intent.putExtra("ContactPriority", contact_details_priority)

            startActivity(intent)
        }

        contact_details_phone_number_SMS_RelativeLayout!!.setOnClickListener {
            println("image befor send to edit = " + contact_details_rounded_image)

            val intent = Intent(this@ContactDetailsActivity, ComposeMessageActivity::class.java)

            intent.putExtra("ContactId", contact_details_id!!)

            startActivity(intent)
        }

        contact_details_phone_number_Call_RelativeLayout!!.setOnClickListener {
            if (!TextUtils.isEmpty(contact_details_phone_number)) {
                val dial = "tel:$contact_details_phone_number"
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(dial)))
            } else {
                Toast.makeText(this@ContactDetailsActivity, "Enter a phone number", Toast.LENGTH_SHORT).show()
            }
        }

        // Floating button, detete a contact
        contact_details_FloatingButtonDelete!!.setOnClickListener {
            //crée une pop up de confirmation avant de supprimer un contact
            MaterialAlertDialogBuilder(this)
                    .setTitle("Supprimer un contact")
                    .setMessage("Voulez vous vraiment supprimer ce contact ?")
                    .setPositiveButton("Oui") { _, _ -> positiveFloatingDeleteButtonClick() }
                    .setNegativeButton("Non") { _, _ -> }
                    .show()
        }
    }

    fun base64ToBitmap(base64: String): Bitmap {
        val imageBytes = Base64.decode(base64, 0)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    // Intent to return to the MainActivity
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val loginIntent = Intent(this@ContactDetailsActivity, MainActivity::class.java)
                startActivity(loginIntent)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // Animation for the Floating Button
    fun onFloatingClickBack() {
        contact_details_FloatingButtonDelete!!.startAnimation(contact_details_FloatingButtonCloseAnimation)
        contact_details_FloatingButtonEdit!!.startAnimation(contact_details_FloatingButtonCloseAnimation)
        contact_details_FloatingButtonOpen!!.startAnimation(contact_details_FloatingButtonAntiClockWiserAnimation)

        contact_details_FloatingButtonEdit!!.isClickable = false
        contact_details_FloatingButtonDelete!!.isClickable = false
    }

    // Animation for the Floating Button
    fun onFloatingClick() {
        contact_details_FloatingButtonDelete!!.startAnimation(contact_details_FloatingButtonOpenAnimation)
        contact_details_FloatingButtonEdit!!.startAnimation(contact_details_FloatingButtonOpenAnimation)
        contact_details_FloatingButtonOpen!!.startAnimation(contact_details_FloatingButtonClockWiserAnimation)

        contact_details_FloatingButtonEdit!!.isClickable = true
        contact_details_FloatingButtonDelete!!.isClickable = true
    }

    fun positiveFloatingDeleteButtonClick() {
        val deleteContact = Runnable {
            contact_details_ContactsDatabase?.contactsDao()?.deleteContactById(contact_details_id!!.toInt())

            val intent = Intent(this@ContactDetailsActivity, MainActivity::class.java)
            intent.putExtra("isDelete", true);
            startActivity(intent)
        }
        contact_details_mDbWorkerThread.postTask(deleteContact)
    }
}
