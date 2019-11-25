package com.yellowtwigs.knockin.controller.activity.group

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yellowtwigs.knockin.R
import com.yellowtwigs.knockin.controller.ContactGridViewAdapter
import com.yellowtwigs.knockin.controller.ContactRecyclerViewAdapter
import com.yellowtwigs.knockin.controller.NotificationListener
import com.yellowtwigs.knockin.controller.SelectContactAdapter
import com.yellowtwigs.knockin.controller.activity.*
import com.yellowtwigs.knockin.model.ContactManager
import com.yellowtwigs.knockin.model.ContactsRoomDatabase
import com.yellowtwigs.knockin.model.DbWorkerThread
import com.yellowtwigs.knockin.model.ModelDB.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.yellowtwigs.knockin.FirstLaunchActivity
import kotlinx.android.synthetic.main.activity_group.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

/**
 * @author Florian Striebel
 */
@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class GroupActivity : AppCompatActivity() {
    private var drawerLayout: DrawerLayout? = null

    //region ========================================= Var or Val ===========================================

    private var group_GridView: RecyclerView? = null
    private var group_RecyclerView: RecyclerView? = null

    private var group_FloatingButtonSend: FloatingActionButton? = null

    private var group_SMSButton: FloatingActionButton? = null
    private var group_MailButton: FloatingActionButton? = null
    private var group_groupButton: FloatingActionButton? = null

    internal var group_search_bar_value = ""
    private var main_filter = arrayListOf<String>()
    private var group_SearchBar: EditText? = null
    private var scaleGestureDetectore: ScaleGestureDetector? = null

    // Database && Thread
    private var group_ContactsDatabase: ContactsRoomDatabase? = null
    private lateinit var main_mDbWorkerThread: DbWorkerThread

    private var group_BottomNavigationView: BottomNavigationView? = null

    private var gestionnaireContacts: ContactManager? = null
    private var gridViewAdapter: ContactGridViewAdapter? = null
    private var recyclerViewAdapter: ContactRecyclerViewAdapter? = null
    private var group_layout: LinearLayout? = null
    private var main_loadingPanel: RelativeLayout? = null

    private var listOfItemSelected: ArrayList<ContactWithAllInformation> = ArrayList()

    private var firstClick: Boolean = true

    private val PERMISSION_CALL_RESULT = 1

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_contacts -> {
                startActivity(Intent(this@GroupActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifcations -> {
                startActivity(Intent(this@GroupActivity, NotificationHistoryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_cockpit -> {
                startActivity(Intent(this@GroupActivity, CockpitActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    //endregion

    /**
     * @param Bundle @type
     */
    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //region ======================================== Theme Dark ========================================

        val sharedThemePreferences = getSharedPreferences("Knockin_Theme", Context.MODE_PRIVATE)
        if (sharedThemePreferences.getBoolean("darkTheme", false)) {
            setTheme(R.style.AppThemeDark)
        } else {
            setTheme(R.style.AppTheme)
        }

        //endregion

        setContentView(R.layout.activity_group)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

        val sharedFirstLaunch = getSharedPreferences("FirstLaunch", Context.MODE_PRIVATE)
        if (sharedFirstLaunch.getBoolean("first_launch", true)) {
            startActivity(Intent(this@GroupActivity, FirstLaunchActivity::class.java))
            finish()
        }
        val decorView = window.decorView
//        val window = window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        val isDelete = intent.getBooleanExtra("isDelete", false)
        if (isDelete) {
            Toast.makeText(this, R.string.main_toast_delete_contact, Toast.LENGTH_LONG).show()
        }

        if (isNotificationServiceEnabled()) {
            toggleNotificationListenerService()
        }

        //region ====================================== Worker Thread =======================================

        // on init WorkerThread
        main_mDbWorkerThread = DbWorkerThread("dbWorkerThread")
        main_mDbWorkerThread.start()

        //endregion

        //on get la base de données
        group_ContactsDatabase = ContactsRoomDatabase.getDatabase(this)

        //region ======================================= FindViewById =======================================


        group_BottomNavigationView = findViewById(R.id.navigation)

        group_BottomNavigationView!!.menu.getItem(1).isChecked = true
        group_BottomNavigationView!!.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        // Search bar
        group_SearchBar = findViewById(R.id.group_search_bar)
        group_layout = findViewById(R.id.group_layout)
        main_loadingPanel = findViewById(R.id.loadingPanel)

        group_MailButton = findViewById(R.id.group_gmail_button)
        group_SMSButton = findViewById(R.id.group_sms_button)
        group_groupButton = findViewById(R.id.group_group_button)
        group_FloatingButtonSend = findViewById(R.id.group_floating_button_send_id)
        //endregion

        //region ========================================== Toolbar =========================================

        val toolbar = findViewById<Toolbar>(R.id.group_toolbar)
        setSupportActionBar(toolbar)
        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)
        actionbar.setHomeAsUpIndicator(R.drawable.ic_open_drawer)
        actionbar.title = ""
        actionbar.setBackgroundDrawable(ColorDrawable(Color.parseColor("#ffffff")))

        //endregion

        //region ======================================= DrawerLayout =======================================

        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val menu = navigationView.menu
        val navItem = menu.findItem(R.id.nav_home)
        navItem.isChecked = true
        val navSyncContact = menu.findItem(R.id.nav_sync_contact)
        navSyncContact.isVisible = true

        navigationView!!.menu.getItem(0).isChecked = true

        navigationView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            drawerLayout!!.closeDrawers()

            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this@GroupActivity, MainActivity::class.java))
                }
                R.id.nav_informations -> startActivity(Intent(this@GroupActivity, EditInformationsActivity::class.java))
                R.id.nav_notif_config -> startActivity(Intent(this@GroupActivity, ManageNotificationActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this@GroupActivity, SettingsActivity::class.java))
                R.id.nav_manage_screen -> startActivity(Intent(this@GroupActivity, ManageMyScreenActivity::class.java))
                R.id.nav_data_access -> {
                }
                R.id.nav_knockons -> startActivity(Intent(this@GroupActivity, ManageKnockonsActivity::class.java))
                R.id.nav_statistics -> {
                }
                R.id.nav_help -> startActivity(Intent(this@GroupActivity, HelpActivity::class.java))
            }

            val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
            drawer.closeDrawer(GravityCompat.START)
            true
        }

        //endregion

        //region ========================================= Runnable =========================================

        //affiche tout les contactList de la Database

        group_GridView = findViewById(R.id.group_grid_view_id)
        group_RecyclerView = findViewById(R.id.group_recycler_view_id)

        //region commentaire
//        val listParams = group_GridView!!.layoutParams
//        if (!hasMenuKey && !hasBackKey) {
//            listParams.height = height - group_BottomNavigationView!!.getMeasuredHeight() - resources.getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height", "dimen", "android")) - getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android"))
//        } else {
//            listParams.height = height - group_BottomNavigationView!!.getMeasuredHeight() - getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android"))
//        }
//        group_ListView!!.layoutParams = listParams

        //endregion
        val sharedPreferences = getSharedPreferences("group", Context.MODE_PRIVATE)
        val len = sharedPreferences.getInt("gridview", 4)

        if (len <= 1) {
            group_GridView!!.visibility = View.GONE
            group_RecyclerView!!.visibility = View.VISIBLE
        } else {
            group_RecyclerView!!.visibility = View.GONE
            group_GridView!!.visibility = View.VISIBLE
        }

//        group_GridView!!.numColumns = len // permet de changer
        gestionnaireContacts = ContactManager(this.applicationContext)

        if (group_GridView != null) {
            when {
                sharedPreferences.getString("tri", "group") == "nom" -> gestionnaireContacts!!.sortContactByFirstNameAZ()
                sharedPreferences.getString("tri", "group") == "priorite" -> gestionnaireContacts!!.sortContactByPriority()
                else -> gestionnaireContacts!!.sortContactByGroup()
            }

            gridViewAdapter = ContactGridViewAdapter(this, gestionnaireContacts!!, len)

            group_GridView!!.adapter = gridViewAdapter
            val index = sharedPreferences.getInt("index", 0)
            val edit: SharedPreferences.Editor = sharedPreferences.edit()
            edit.apply()

//            group_GridView!!.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
//                if (group_GridView!!.adapter is SelectContactAdapter && !firstClick) {
//                    val adapter = (group_GridView!!.adapter as SelectContactAdapter)
//                    adapter.itemSelected(position)
//                    adapter.notifyDataSetChanged()
//
//                    if (listOfItemSelected.contains(gestionnaireContacts!!.contactList[position])) {
//                        listOfItemSelected.remove(gestionnaireContacts!!.contactList[position])
//                    } else {
//                        listOfItemSelected.add(gestionnaireContacts!!.contactList[position])
//                    }
//
//                    verifiedContactsChannel(listOfItemSelected)
//
//                    if (adapter.listContactSelect.size == 0) {
//                        gridViewAdapter = ContactGridViewAdapter(this, gestionnaireContacts, len)
//                        group_GridView!!.adapter = gridViewAdapter
//                        group_SearchBar!!.visibility = View.VISIBLE
//
//                        Toast.makeText(this, R.string.main_toast_multi_select_deactived, Toast.LENGTH_SHORT).show()
//
//                        group_MailButton!!.visibility = View.GONE
//                        group_SMSButton!!.visibility = View.GONE
//                        group_groupButton!!.visibility = View.GONE
//                        group_floating_button_send_id!!.visibility = View.GONE
//                    }
//                }
//                firstClick = false
//            }
//
//            group_GridView!!.setOnScrollListener(object : AbsListView.OnScrollListener {
//                override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
//
//                }
//
//                override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
//                    if (gridViewAdapter != null) {
//                        gridViewAdapter!!.closeMenu()
//                    }
//                }
//            })
        }

        if (group_RecyclerView != null) {
            recyclerViewAdapter = ContactRecyclerViewAdapter(this, gestionnaireContacts, len)
            group_RecyclerView!!.adapter = recyclerViewAdapter
            val index = sharedPreferences.getInt("index", 0)
            val edit: SharedPreferences.Editor = sharedPreferences.edit()
            group_RecyclerView!!.scrollToPosition(index)
            edit.putInt("index", 0)
            edit.apply()

            group_RecyclerView!!.layoutManager = LinearLayoutManager(this)

            group_RecyclerView!!.setOnClickListener {
                if (group_RecyclerView!!.adapter is SelectContactAdapter && !firstClick) {
                    val adapter = ContactRecyclerViewAdapter(this, gestionnaireContacts, len)
                    group_RecyclerView!!.adapter = adapter
//                    adapter.itemSelected(adapter.)

                    adapter.notifyDataSetChanged()
                    if (adapter.listOfItemSelected.size == 0) {
                        group_RecyclerView!!.adapter = ContactRecyclerViewAdapter(this, gestionnaireContacts, len)
                        group_SearchBar!!.visibility = View.VISIBLE

                        Toast.makeText(this, R.string.main_toast_multi_select_deactived, Toast.LENGTH_SHORT).show()

                        group_MailButton!!.visibility = View.GONE
                        group_SMSButton!!.visibility = View.GONE
                        group_groupButton!!.visibility = View.GONE
                        group_floating_button_send_id!!.visibility = View.GONE
                    }
                }
                firstClick = false
            }
        }

        //main_mDbWorkerThread.postTask(printContacts)

        //endregion

        //region ======================================== Listeners =========================================

        group_FloatingButtonSend!!.setOnClickListener {
            val intent = Intent(this@GroupActivity, MultiChannelActivity::class.java)
            val iterator: IntIterator?
            val listOfIdContactSelected: ArrayList<Int> = ArrayList()

            if (len > 1) {
                val adapter: SelectContactAdapter = (group_GridView!!.adapter as SelectContactAdapter)
                iterator = (0 until adapter.listContactSelect.size).iterator()

                for (i in iterator) {
                    listOfIdContactSelected.add(adapter.listContactSelect[i].getContactId())
                }
                intent.putIntegerArrayListExtra("ListContactsSelected", listOfIdContactSelected)

                startActivity(intent)
                finish()
            } else {
                iterator = (0 until listOfItemSelected.size).iterator()

                for (i in iterator) {
                    listOfIdContactSelected.add(listOfItemSelected[i].getContactId())
                }
                intent.putIntegerArrayListExtra("ListContactsSelected", listOfIdContactSelected)

                startActivity(intent)
                refreshActivity()
            }
        }

        //Sync contact
        navSyncContact.setOnMenuItemClickListener {
            //check les permissions
            drawerLayout!!.closeDrawers()
            group_GridView!!.visibility = View.GONE
            group_RecyclerView!!.visibility = View.GONE
            val sync = Runnable {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 1)
                }
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    /////////////////////////////////////////////////////////////////

                    //Synchronise tout les contactList du carnet android
                    val displayLoading = Runnable {
                        main_loadingPanel!!.visibility = View.VISIBLE
                    }
                    runOnUiThread(displayLoading)
                    gestionnaireContacts!!.getAllContacsInfoSync(contentResolver)
                    val sharedPreferencesSync = getSharedPreferences("save_last_sync", Context.MODE_PRIVATE)
                    var index = 1
                    var stringSet = listOf<String>()
                    if (sharedPreferencesSync.getStringSet(index.toString(), null) != null)
                        stringSet = sharedPreferencesSync.getStringSet(index.toString(), null)!!.sorted()
                    val changedContactList = arrayListOf<Pair<ContactDB, List<ContactDetailDB>>>()
                    while (sharedPreferencesSync.getStringSet(index.toString(), null) != null && stringSet.isNotEmpty()) {
                        stringSet = sharedPreferencesSync.getStringSet(index.toString(), null)!!.sorted()
                        changedContactList.add(gestionnaireContacts!!.setToContactList(stringSet))
                        index++
                    }
                    changedContactList.forEach { changedContact ->
                        MaterialAlertDialogBuilder(this, R.style.AlertDialog)
                                .setTitle("Contact modifié")
                                .setMessage("Le Contact " + changedContact.first.firstName + " " + changedContact.first.lastName + " a été changé, garder la version du carnet d'adresse d'Android ou de Knockin ?")
                                .setPositiveButton("Knockin") { _, _ ->
                                }
                                .setNegativeButton("Android") { _, _ ->
                                    val allId = gestionnaireContacts!!.sliceLastSync(sharedPreferences.getString("last_sync_2", "")!!)
                                    allId.forEach {
                                        if (changedContact.first.id == it.first)
                                            changedContact.first.id = it.second
                                    }

                                    group_ContactsDatabase!!.contactsDao().updateContactByIdSync(changedContact.first.id!!, changedContact.first.firstName, changedContact.first.lastName)
                                    group_ContactsDatabase!!.contactDetailsDao().deleteAllDetailsOfContact(changedContact.first.id!!)
                                    changedContact.second.forEach {
                                        it.idContact = changedContact.first.id
                                        group_ContactsDatabase!!.contactDetailsDao().insert(it)
                                    }
                                    val displaySync = Runnable {
                                        gestionnaireContacts!!.contactList.clear()
                                        val sharedPreferences = applicationContext.getSharedPreferences("group", Context.MODE_PRIVATE)
                                        val executorService: ExecutorService = Executors.newFixedThreadPool(1)
                                        if (sharedPreferences.getString("tri", "") == "priorite") {
                                            val callDb = Callable { group_ContactsDatabase!!.contactsDao().sortContactByPriority20() }
                                            val result = executorService.submit(callDb)
                                            gestionnaireContacts!!.contactList.addAll(result.get())
                                        } else {
                                            val callDb = Callable { group_ContactsDatabase!!.contactsDao().sortContactByFirstNameAZ() }
                                            val result = executorService.submit(callDb)
                                            gestionnaireContacts!!.contactList.addAll(result.get())
                                        }
                                        gridViewAdapter!!.setGestionnaireContact(gestionnaireContacts!!)
                                        gridViewAdapter!!.notifyDataSetChanged()
                                    }
                                    runOnUiThread(displaySync)
                                }
                                .show()
                    }
                    //gestionnaireContacts!!.
                    //ContactSync.getAllContact(contentResolver)//TODO put this code into ContactManager
                    //val len = sharedPreferences.getInt("gridview", 4)
                    /*  gridViewAdapter = ContactGridViewAdapter(applicationContext, gestionnaireContacts!!, len)
              group_GridView!!.adapter = gridViewAdapter
  */
                    index = 1
                    val edit: SharedPreferences.Editor = sharedPreferencesSync.edit()
                    while (sharedPreferencesSync.getStringSet(index.toString(), null) != null && stringSet.isNotEmpty()) {
                        stringSet = sharedPreferencesSync.getStringSet(index.toString(), null)!!.sorted()
                        edit.remove(index.toString())
                        index++
                    }
                    edit.apply()
                    val sharedPrefe = applicationContext.getSharedPreferences("group", Context.MODE_PRIVATE)
                    if (sharedPrefe.getString("tri", "") == "priorite")
                        gestionnaireContacts!!.sortContactByPriority()
                    else
                        gestionnaireContacts!!.sortContactByFirstNameAZ()

                    val displaySync = Runnable {
                        main_loadingPanel!!.visibility = View.GONE

                        if (len >= 3) {
                            group_GridView!!.visibility = View.VISIBLE
                            gridViewAdapter!!.setGestionnaireContact(gestionnaireContacts!!)
                            gridViewAdapter!!.notifyDataSetChanged()
                        } else {
                            group_RecyclerView!!.visibility = View.VISIBLE
                            recyclerViewAdapter!!.setGestionnaireContact(gestionnaireContacts!!)
                            recyclerViewAdapter!!.notifyDataSetChanged()
                        }
                        drawerLayout!!.closeDrawers()
                    }
                    runOnUiThread(displaySync)
                }
            }
            main_mDbWorkerThread.postTask(sync)
            true
        }

        group_layout!!.setOnTouchListener { _, _ ->
            val view = this@GroupActivity.currentFocus
            val imm = this@GroupActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (view != null) {
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
            true
        }

        group_SearchBar!!.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
                gridViewAdapter!!.closeMenu()
                group_search_bar_value = group_SearchBar!!.text.toString()
                val sharedPref = getSharedPreferences("group", Context.MODE_PRIVATE)
                val length = sharedPref.getInt("gridview", 4)
                val filteredList = gestionnaireContacts!!.getContactConcernByFilter(main_filter, group_search_bar_value)
                val contactListDb = ContactManager(this@GroupActivity)
                if (sharedPref.getString("tri", "nom") == "nom") {
                    contactListDb.sortContactByFirstNameAZ()
                    contactListDb.contactList.retainAll(filteredList)
                } else {
                    contactListDb.sortContactByPriority()
                    contactListDb.contactList.retainAll(filteredList)
                }
                gestionnaireContacts!!.contactList.clear()
                gestionnaireContacts!!.contactList.addAll(contactListDb.contactList)
                if (length > 1) {
                    gridViewAdapter = ContactGridViewAdapter(this@GroupActivity, gestionnaireContacts, length)
                    group_GridView!!.adapter = gridViewAdapter
                } else {
                    group_RecyclerView!!.adapter = recyclerViewAdapter
                    recyclerViewAdapter = ContactRecyclerViewAdapter(this@GroupActivity, gestionnaireContacts, length)
                    recyclerViewAdapter!!.notifyDataSetChanged()
                }
            }
        })

        group_SMSButton!!.setOnClickListener {
            val iterator: IntIterator?
            val listOfPhoneNumberContactSelected: ArrayList<String> = ArrayList()

            if (len > 1) {
                val adapter: SelectContactAdapter = (group_GridView!!.adapter as SelectContactAdapter)

                iterator = (0 until adapter.listContactSelect.size).iterator()

                for (i in iterator) {
                    listOfPhoneNumberContactSelected.add(adapter.listContactSelect[i].getFirstPhoneNumber())
                }
            } else {
                iterator = (0 until listOfItemSelected.size).iterator()

                for (i in iterator) {
                    listOfPhoneNumberContactSelected.add(listOfItemSelected[i].getFirstPhoneNumber())
                }
            }
            monoChannelSmsClick(listOfPhoneNumberContactSelected)
        }

        group_MailButton!!.setOnClickListener {
            val iterator: IntIterator?
            val listOfMailContactSelected: ArrayList<String> = ArrayList()

            if (len > 1) {
                val adapter: SelectContactAdapter = (group_GridView!!.adapter as SelectContactAdapter)
                iterator = (0 until adapter.listContactSelect.size).iterator()

                for (i in iterator) {
                    listOfMailContactSelected.add(adapter.listContactSelect[i].getFirstMail())
                }
            } else {
                iterator = (0 until listOfItemSelected.size).iterator()

                for (i in iterator) {
                    listOfMailContactSelected.add(listOfItemSelected[i].getFirstPhoneNumber())
                }
            }
            monoChannelMailClick(listOfMailContactSelected)
        }

        group_groupButton!!.setOnClickListener {
            val iterator: IntIterator?
            val listOfContactSelected: ArrayList<ContactWithAllInformation> = ArrayList()

            if (len > 1) {
                val adapter: SelectContactAdapter = (group_GridView!!.adapter as SelectContactAdapter)
                iterator = (0 until adapter.listContactSelect.size).iterator()

                for (i in iterator) {
                    listOfContactSelected.add(adapter.listContactSelect[i])
                }
            } else {
                iterator = (0 until listOfItemSelected.size).iterator()

                for (i in iterator) {
                    listOfContactSelected.add(listOfItemSelected[i])
                }
            }
            if (len >= 3) {
                gridViewAdapter = ContactGridViewAdapter(this, gestionnaireContacts, len)
                group_GridView!!.adapter = gridViewAdapter
                group_SearchBar!!.visibility = View.VISIBLE


                group_MailButton!!.visibility = View.GONE
                group_SMSButton!!.visibility = View.GONE
                group_groupButton!!.visibility = View.GONE
                group_floating_button_send_id!!.visibility = View.GONE
            } else {
                group_RecyclerView!!.adapter = ContactRecyclerViewAdapter(this, gestionnaireContacts, len)
                group_SearchBar!!.visibility = View.VISIBLE


                group_MailButton!!.visibility = View.GONE
                group_SMSButton!!.visibility = View.GONE
                group_groupButton!!.visibility = View.GONE
                group_floating_button_send_id!!.visibility = View.GONE
            }
            //saveGroupMultiSelect(listOfItemSelected, len)
            // recreate()
        }
        //endregion

        scaleGestureDetectore = ScaleGestureDetector(this,
                MyOnScaleGestureListener())

    }

    //region ========================================== Functions ===========================================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetectore?.onTouchEvent(event)
        return true
    }

    inner class MyOnScaleGestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            if (scaleFactor > 1) {
                println("Zooming Out$scaleFactor")
            } else {
                println("Zooming In$scaleFactor")
            }
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            println("begin")
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            println("end")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.toolbar_menu_filter_group, menu)
        val triNom = menu.findItem(R.id.tri_par_nom)
        val sortLastname = menu.findItem(R.id.tri_par_lastname)
        val triPrio = menu.findItem(R.id.tri_par_priorite)
        val triGroup = menu.findItem(R.id.tri_par_group)
        val sharedPreferences = getSharedPreferences("group", Context.MODE_PRIVATE)
        val tri = sharedPreferences.getString("tri", "nom")
        if (tri == "nom") {
            triNom.isChecked = true
        } else if (tri == "lastname") {
            sortLastname.isChecked = true
        } else if (tri == "priorite") {
            triPrio.isChecked = true
        } else {
            triGroup.isChecked = true
        }
        return true
    }


    //check les checkbox si elle ont été check apres une recherche
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        val main_filter = intent.getStringArrayListExtra("Filter")
        if (main_filter != null && main_filter.contains("sms")) {
            menu?.findItem(R.id.sms_filter)?.isChecked = true
            intent.putStringArrayListExtra("Filter", main_filter)
        }
        if (main_filter != null && main_filter.contains("mail")) {
            menu?.findItem(R.id.mail_filter)?.isChecked = true
            intent.putStringArrayListExtra("Filter", main_filter)
        }
        return true
    }

    @SuppressLint("ShowToast")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        gridViewAdapter!!.closeMenu()
        when (item.itemId) {
            android.R.id.home -> {
                drawerLayout!!.openDrawer(GravityCompat.START)
                hideKeyboard()
                return true
            }
            R.id.item_help -> {
                MaterialAlertDialogBuilder(this, R.style.AlertDialog)
                        .setTitle(R.string.help)
                        .setMessage(this.resources.getString(R.string.help_group))
                        .show()
                return true
            }
            R.id.sms_filter -> {
                if (item.isChecked) {
                    item.isChecked = false
                    main_filter.remove("sms")
                    // duplicate
                    group_search_bar_value = group_SearchBar!!.text.toString()

                    val sharedPreferences = getSharedPreferences("group", Context.MODE_PRIVATE)
                    val len = sharedPreferences.getInt("gridview", 4)
                    val filteredContact = gestionnaireContacts!!.getContactConcernByFilter(main_filter, group_search_bar_value)
                    val contactListDb = ContactManager(this)
                    if (sharedPreferences.getString("tri", "nom") == "nom") {
                        contactListDb.sortContactByFirstNameAZ()
                    } else if (sharedPreferences.getString("tri", "nom") == "priorite") {
                        contactListDb.sortContactByPriority()
                    } else {
                        contactListDb.sortContactByGroup()
                    }
                    contactListDb.contactList.retainAll(filteredContact)
                    gestionnaireContacts!!.contactList.clear()
                    gestionnaireContacts!!.contactList.addAll(contactListDb.contactList)
                    if (len > 1) {
                        gridViewAdapter = ContactGridViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                        group_GridView!!.adapter = gridViewAdapter
                    } else {
                        group_RecyclerView!!.adapter = recyclerViewAdapter
                        recyclerViewAdapter = ContactRecyclerViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                    }
                } else {
                    item.isChecked = true
                    main_filter.add("sms")
                    // duplicate
                    group_search_bar_value = group_SearchBar!!.text.toString()

                    val sharedPreferences = getSharedPreferences("group", Context.MODE_PRIVATE)
                    val len = sharedPreferences.getInt("gridview", 4)
                    val filteredContact = gestionnaireContacts!!.getContactConcernByFilter(main_filter, group_search_bar_value)
                    if (sharedPreferences.getString("tri", "nom") == "nom") {
                        gestionnaireContacts!!.sortContactByFirstNameAZ()
                    } else if (sharedPreferences.getString("tri", "nom") == "priorite") {
                        gestionnaireContacts!!.sortContactByPriority()
                    } else {
                        gestionnaireContacts!!.sortContactByGroup()
                    }
                    gestionnaireContacts!!.contactList.retainAll(filteredContact)

                    if (len > 1) {
                        gridViewAdapter = ContactGridViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                        group_GridView!!.adapter = gridViewAdapter
                    } else {
                        group_RecyclerView!!.adapter = recyclerViewAdapter
                        recyclerViewAdapter = ContactRecyclerViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                        recyclerViewAdapter!!.notifyDataSetChanged()
                    }
                }
                return true
            }
            R.id.mail_filter -> {
                if (item.isChecked) {
                    item.setChecked(false)
                    main_filter.remove("mail")
                    // duplicate
                    group_search_bar_value = group_SearchBar!!.text.toString()

                    val sharedPreferences = getSharedPreferences("group", Context.MODE_PRIVATE)
                    val len = sharedPreferences.getInt("gridview", 4)
                    val filteredContact = gestionnaireContacts!!.getContactConcernByFilter(main_filter, group_search_bar_value)
                    val contactListDb = ContactManager(this)
                    if (sharedPreferences.getString("tri", "nom") == "nom") {
                        contactListDb.sortContactByFirstNameAZ()
                    } else if (sharedPreferences.getString("tri", "nom") == "priorite") {
                        contactListDb.sortContactByPriority()
                    } else {
                        contactListDb.sortContactByGroup()
                    }
                    contactListDb.contactList.retainAll(filteredContact)
                    gestionnaireContacts!!.contactList.clear()
                    gestionnaireContacts!!.contactList.addAll(contactListDb.contactList)
                    if (len > 1) {
                        gridViewAdapter = ContactGridViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                        group_GridView!!.adapter = gridViewAdapter
                    } else {
                        group_RecyclerView!!.adapter = recyclerViewAdapter
                        recyclerViewAdapter = ContactRecyclerViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                        recyclerViewAdapter!!.notifyDataSetChanged()
                    }
                } else {
                    item.isChecked = true
                    main_filter.add("mail")
                    // duplicate
                    group_search_bar_value = group_SearchBar!!.text.toString()
                    val sharedPreferences = getSharedPreferences("group", Context.MODE_PRIVATE)
                    val len = sharedPreferences.getInt("gridview", 4)
                    val filteredContact = gestionnaireContacts!!.getContactConcernByFilter(main_filter, group_search_bar_value)
                    if (sharedPreferences.getString("tri", "nom") == "nom") {
                        gestionnaireContacts!!.sortContactByFirstNameAZ()
                    } else if (sharedPreferences.getString("tri", "nom") == "priorite") {
                        gestionnaireContacts!!.sortContactByPriority()
                    } else {
                        gestionnaireContacts!!.sortContactByGroup()
                    }
                    gestionnaireContacts!!.contactList.retainAll(filteredContact)
                    if (len > 1) {
                        gridViewAdapter = ContactGridViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                        group_GridView!!.adapter = gridViewAdapter
                    } else {
                        recyclerViewAdapter = ContactRecyclerViewAdapter(this@GroupActivity, gestionnaireContacts, len)

                        recyclerViewAdapter!!.notifyDataSetChanged()
                    }
                }
                return true
            }
            R.id.tri_par_nom -> {
                if (!item.isChecked) {
                    item.isChecked = true
                    gestionnaireContacts!!.sortContactByFirstNameAZ()
                    val sharedPreferences = getSharedPreferences("group", Context.MODE_PRIVATE)
                    val len = sharedPreferences.getInt("gridview", 4)
                    if (len > 1) {
                        gridViewAdapter = ContactGridViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                        group_GridView!!.adapter = gridViewAdapter
                    } else {
                        recyclerViewAdapter = ContactRecyclerViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                        group_RecyclerView!!.adapter = recyclerViewAdapter
                        recyclerViewAdapter!!.notifyDataSetChanged()
                    }
                    val edit: SharedPreferences.Editor = sharedPreferences.edit()
                    edit.putString("tri", "nom")
                    edit.apply()
                }
            }
            R.id.tri_par_priorite -> {
                if (!item.isChecked) {
                    item.setChecked(true)
                    gestionnaireContacts!!.sortContactByPriority()
                    val sharedPreferences = getSharedPreferences("group", Context.MODE_PRIVATE)
                    val len = sharedPreferences.getInt("gridview", 4)
                    if (len > 1) {
                        gridViewAdapter = ContactGridViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                        group_GridView!!.adapter = gridViewAdapter
                    } else {
                        recyclerViewAdapter = ContactRecyclerViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                        group_RecyclerView!!.adapter = recyclerViewAdapter
                        recyclerViewAdapter!!.notifyDataSetChanged()
                    }

                    val edit: SharedPreferences.Editor = sharedPreferences.edit()
                    edit.putString("tri", "priorite")
                    edit.apply()
                }
            }
            R.id.tri_par_group -> {
                if (!item.isChecked) {
                    item.setChecked(true)
                    gestionnaireContacts!!.sortContactByGroup()
                    val sharedPreferences = getSharedPreferences("group", Context.MODE_PRIVATE)
                    val len = sharedPreferences.getInt("gridview", 4)
                    if (len > 1) {
                        gridViewAdapter = ContactGridViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                        group_GridView!!.adapter = gridViewAdapter
                    } else {
                        recyclerViewAdapter = ContactRecyclerViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                        group_RecyclerView!!.adapter = recyclerViewAdapter
                        recyclerViewAdapter!!.notifyDataSetChanged()
                    }
                    val edit: SharedPreferences.Editor = sharedPreferences.edit()
                    edit.putString("tri", "group")
                    edit.apply()
                }
            }
            R.id.tri_par_lastname -> {
                if (!item.isChecked) {
                    item.setChecked(true)
                    gestionnaireContacts!!.sortContactByLastname()
                    val sharedPreferences = getSharedPreferences("group", Context.MODE_PRIVATE)
                    val len = sharedPreferences.getInt("gridview", 4)
                    if (len > 1) {
                        gridViewAdapter = ContactGridViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                        group_GridView!!.adapter = gridViewAdapter
                    } else {
                        group_RecyclerView!!.adapter = recyclerViewAdapter
                        recyclerViewAdapter = ContactRecyclerViewAdapter(this@GroupActivity, gestionnaireContacts, len)
                        recyclerViewAdapter!!.notifyDataSetChanged()
                    }
                    val edit: SharedPreferences.Editor = sharedPreferences.edit()
                    edit.putString("tri", "lastname")
                    edit.apply()
                }
            }
        }
        return super.onOptionsItemSelected(item)
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
    }//TODO: enlever code duplicate

    private fun toggleNotificationListenerService() {
        val pm = packageManager
        val cmpName = ComponentName(this, NotificationListener::class.java)
        pm.setComponentEnabledSetting(cmpName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(cmpName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    }

    private fun hideKeyboard() {
        // Check if no view has focus:
        val view = this.currentFocus

        view?.let { v ->
            val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    fun gridMultiSelectItemClick(len: Int, position: Int, firstPosVis: Int) {
//        group_GridView!!.setSelection(firstPosVis)
        val adapter = SelectContactAdapter(this, gestionnaireContacts, len)
//        group_GridView!!.adapter = adapter
        adapter.itemSelected(position)
        adapter.notifyDataSetChanged()
        group_SearchBar!!.visibility = View.GONE
        firstClick = true

        if (listOfItemSelected.contains(gestionnaireContacts!!.contactList[position])) {
            listOfItemSelected.remove(gestionnaireContacts!!.contactList[position])
        } else {
            listOfItemSelected.add(gestionnaireContacts!!.contactList[position])
        }

        verifiedContactsChannel(listOfItemSelected)
        Toast.makeText(this, R.string.main_toast_multi_select_actived, Toast.LENGTH_SHORT).show()
    }

    fun clickGroupGrid(len: Int, positions: List<Int>, firstPosVis: Int, secondClickLibelle: Boolean, fromLibelleClick: Boolean) {
//        group_GridView!!.setSelection(firstPosVis)
        val adapter = SelectContactAdapter(this, gestionnaireContacts, len)
//        group_GridView!!.adapter = adapter
        adapter.notifyDataSetChanged()

        if (!secondClickLibelle) {
            group_SearchBar!!.visibility = View.GONE
            firstClick = true

            for (position in positions) {
                adapter.itemSelected(position)
            }

            verifiedContactsChannel(listOfItemSelected)

            if (fromLibelleClick && firstClick) {
                group_groupButton!!.visibility = View.GONE
            }

            Toast.makeText(this, R.string.main_toast_multi_select_actived, Toast.LENGTH_SHORT).show()
        } else {
            group_FloatingButtonSend!!.visibility = View.GONE
            group_SearchBar!!.visibility = View.VISIBLE
            group_SMSButton!!.visibility = View.GONE
            group_MailButton!!.visibility = View.GONE
            group_groupButton!!.visibility = View.GONE

            adapter.itemDeselected()
            gridViewAdapter = ContactGridViewAdapter(this, gestionnaireContacts, len)
            group_GridView!!.adapter = gridViewAdapter

            Toast.makeText(this, R.string.main_toast_multi_select_deactived, Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifiedContactsChannel(listOfItemSelected: ArrayList<ContactWithAllInformation>) {
        val iterator = (0 until listOfItemSelected.size).iterator()
        var allContactsHaveMail = true
        var allContactsHavePhoneNumber = true

        for (i in iterator) {
            if (listOfItemSelected[i].getFirstMail() == "") {
                allContactsHaveMail = false
            }

            if (listOfItemSelected[i].getFirstPhoneNumber() == "") {
                allContactsHavePhoneNumber = false
            }
        }
        var i = 2
        val metrics = DisplayMetrics()
        this.windowManager.defaultDisplay.getMetrics(metrics)
        val margin = (0.5 * metrics.densityDpi).toInt()
        println("metric smartphone" + metrics.densityDpi)
        group_floating_button_send_id!!.visibility = View.VISIBLE
        if (allContactsHavePhoneNumber) {
            group_SMSButton!!.visibility = View.VISIBLE
            i++
        } else {
            println("false phoneNumber")
            group_SMSButton!!.visibility = View.GONE
        }
        if (allContactsHaveMail) {
            group_MailButton!!.visibility = View.VISIBLE
            val params: ViewGroup.MarginLayoutParams = group_MailButton!!.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = margin * i
            group_MailButton!!.layoutParams = params
            println("height of floating mail" + group_MailButton!!.height)
            i++
        } else {
            println("false mail")
            group_MailButton!!.visibility = View.GONE
        }
        val params: ViewGroup.MarginLayoutParams = group_groupButton!!.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = margin * i
        group_groupButton!!.layoutParams = params

        /*   if (listOfItemSelected.size == 1) {
               group_groupButton!!.visibility = View.GONE
           } else {
               group_groupButton!!.visibility = View.VISIBLE
           }*/
    }

    fun recyclerMultiSelectItemClick(position: Int, secondClickLibelle: Boolean, fromLibelleClick: Boolean) {
        if (!secondClickLibelle) {
            if (listOfItemSelected.contains(gestionnaireContacts!!.contactList[position])) {
                listOfItemSelected.remove(gestionnaireContacts!!.contactList[position])

                if (listOfItemSelected.size == 0) {
                    group_SearchBar!!.visibility = View.VISIBLE
                    group_SMSButton!!.visibility = View.GONE
                    group_MailButton!!.visibility = View.GONE
                    group_groupButton!!.visibility = View.GONE
                    group_floating_button_send_id!!.visibility = View.GONE
                }

            } else {
                listOfItemSelected.add(gestionnaireContacts!!.contactList[position])
                group_SearchBar!!.visibility = View.GONE
                verifiedContactsChannel(listOfItemSelected)
            }

            if (fromLibelleClick) {
                group_groupButton!!.visibility = View.GONE
                firstClick = true
            }
        } else {
            listOfItemSelected.clear()
            group_SearchBar!!.visibility = View.VISIBLE
            group_SMSButton!!.visibility = View.GONE
            group_MailButton!!.visibility = View.GONE
            group_groupButton!!.visibility = View.GONE
            group_floating_button_send_id!!.visibility = View.GONE
        }


        if (listOfItemSelected.size == 1 && firstClick) {
            Toast.makeText(this, R.string.main_toast_multi_select_actived, Toast.LENGTH_SHORT).show()
            firstClick = false
        } else if (listOfItemSelected.size == 0) {
            Toast.makeText(this, R.string.main_toast_multi_select_deactived, Toast.LENGTH_SHORT).show()
            firstClick = true
        }
    }

    fun recyclerItemClick(position: Int, fromLibelleClick: Boolean) {
        if (listOfItemSelected.contains(gestionnaireContacts!!.contactList[position])) {
            listOfItemSelected.remove(gestionnaireContacts!!.contactList[position])

            verifiedContactsChannel(listOfItemSelected)

        } else {
            if (fromLibelleClick && firstClick) {
                var i = 0
                /*     while (i < listOfItemSelected.size) {
                         listOfItemSelectedFromLibelleClick.add(listOfItemSelected[i])
                         i++
                     }
                     firstClick = false
                 }*/

                listOfItemSelected.add(gestionnaireContacts!!.contactList[position])
                verifiedContactsChannel(listOfItemSelected)
            }

            /* if (checkIfTwoListsAreSame(listOfItemSelected, listOfItemSelectedFromLibelleClick)) {
                 group_groupButton!!.visibility = View.GONE
             } else {
                 group_groupButton!!.visibility = View.VISIBLE
             }*/

            if (listOfItemSelected.size == 0) {

                group_FloatingButtonSend!!.visibility = View.GONE
                group_SearchBar!!.visibility = View.VISIBLE
                group_SMSButton!!.visibility = View.GONE
                group_MailButton!!.visibility = View.GONE
                group_groupButton!!.visibility = View.GONE

                Toast.makeText(this, R.string.main_toast_multi_select_deactived, Toast.LENGTH_SHORT).show()
                firstClick = true
            }
        }
    }

    /*private fun appIsInstalled(): Boolean {
        val pm = this.packageManager
        return try {
            pm.getApplicationInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) {
            false
        }
    }*/

    private fun checkIfTwoListsAreSame(listContactsSelected: ArrayList<ContactWithAllInformation>, listContactsSelectedFromLibelle: ArrayList<ContactWithAllInformation>): Boolean {
        val iterator = (0 until listContactsSelected.size).iterator()
        val iterator2 = (0 until listContactsSelectedFromLibelle.size).iterator()
        var isTrue = false

        for (i in iterator) {
            for (y in iterator2) {
                if (listContactsSelected[i] == listContactsSelectedFromLibelle[y]) {
                    isTrue = true
                }
            }
        }

        return (listContactsSelected.size == listContactsSelectedFromLibelle.size && isTrue)
    }

    private fun monoChannelSmsClick(listOfPhoneNumber: ArrayList<String>) {

        var message = "smsto:" + listOfPhoneNumber[0]
        for (i in 1 until listOfPhoneNumber.size) {
            message += ";" + listOfPhoneNumber[i]
        }
        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(message)))
    }

    private fun monoChannelMailClick(listOfMail: ArrayList<String>) {
        val contact = listOfMail.toArray(arrayOfNulls<String>(listOfMail.size))
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_EMAIL, contact)/*listOfMail.toArray(new String[listOfMail.size()]*/
        intent.data = Uri.parse("mailto:")
        intent.type = "message/rfc822"
        intent.putExtra(Intent.EXTRA_SUBJECT, "")
        intent.putExtra(Intent.EXTRA_TEXT, "")
        startActivity(intent)
    }

    private fun refreshActivity() {
        val sharedPreferences = getSharedPreferences("Gridview_column", Context.MODE_PRIVATE)
        val len = sharedPreferences.getInt("gridview", 4)
        if (len > 1) {
            gridViewAdapter = ContactGridViewAdapter(this@GroupActivity, gestionnaireContacts, len)
            group_GridView!!.adapter = gridViewAdapter
        } else {
            recyclerViewAdapter = ContactRecyclerViewAdapter(this@GroupActivity, gestionnaireContacts, len)
            group_RecyclerView!!.adapter = recyclerViewAdapter
        }

        group_MailButton!!.visibility = View.GONE
        group_SMSButton!!.visibility = View.GONE
        group_FloatingButtonSend!!.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {

            PERMISSION_CALL_RESULT -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (group_GridView!!.visibility == View.VISIBLE) {
                    gridViewAdapter!!.callPhone(gridViewAdapter!!.phonePermission)
                } else {
                    recyclerViewAdapter!!.callPhone(recyclerViewAdapter!!.phonePermission)
                }
            }
        }
    }
}