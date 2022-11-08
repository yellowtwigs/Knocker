package com.yellowtwigs.knockin.ui.contacts.list

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yellowtwigs.knockin.R
import com.yellowtwigs.knockin.databinding.ActivityContactsListBinding
import com.yellowtwigs.knockin.model.service.NotificationsListenerService
import com.yellowtwigs.knockin.ui.CircularImageView
import com.yellowtwigs.knockin.ui.HelpActivity
import com.yellowtwigs.knockin.ui.add_edit_contact.add.AddNewContactActivity
import com.yellowtwigs.knockin.ui.add_edit_contact.edit.EditContactActivity
import com.yellowtwigs.knockin.ui.cockpit.CockpitActivity
import com.yellowtwigs.knockin.ui.contacts.contact_selected.ContactSelectedWithAppsActivity
import com.yellowtwigs.knockin.ui.contacts.multi_channel.MultiChannelActivity
import com.yellowtwigs.knockin.ui.groups.list.GroupsListActivity
import com.yellowtwigs.knockin.ui.groups.manage_group.ManageGroupActivity
import com.yellowtwigs.knockin.ui.in_app.PremiumActivity
import com.yellowtwigs.knockin.ui.notifications.history.NotificationsHistoryActivity
import com.yellowtwigs.knockin.ui.notifications.settings.NotificationsSettingsActivity
import com.yellowtwigs.knockin.ui.settings.ManageMyScreenActivity
import com.yellowtwigs.knockin.ui.teleworking.TeleworkingActivity
import com.yellowtwigs.knockin.utils.Converter
import com.yellowtwigs.knockin.utils.EveryActivityUtils.checkTheme
import com.yellowtwigs.knockin.utils.EveryActivityUtils.hideKeyboard
import com.yellowtwigs.knockin.utils.RandomDefaultImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.net.URLEncoder

@AndroidEntryPoint
class ContactsListActivity : AppCompatActivity() {

    private val contactsListViewModel: ContactsListViewModel by viewModels()

    private lateinit var sortByPreferences: SharedPreferences

    private var listOfItemSelected = arrayListOf<Int>()

    private var listOfHasSms = arrayListOf<Boolean>()
    private var listOfPhoneNumbers = arrayListOf<String>()

    private var listOfHasEmail = arrayListOf<Boolean>()
    private var listOfEmails = arrayListOf<String>()

    private var listOfHasWhatsapp = arrayListOf<Boolean>()
    var modeMultiSelect = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkTheme(this)

        val binding = ActivityContactsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (isNotificationServiceEnabled()) {
            toggleNotificationListenerService()
        }

        sortByPreferences = getSharedPreferences("Sort_By_Preferences", Context.MODE_PRIVATE)

        setupToolbar(binding)
        setupRecyclerView(binding)
        setupBottomNavigationView(binding)
        setupDrawerLayout(binding)
        floatingButtonsClick(binding)
        multiSelectToolbarClick(binding)
    }

    //region =========================================== TOOLBAR ============================================

    private fun setupToolbar(binding: ActivityContactsListBinding) {
        setSupportActionBar(binding.toolbarMenu)
        val actionbar = supportActionBar
        actionbar?.title = ""

        binding.toolbarSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(query: CharSequence?, p1: Int, p2: Int, p3: Int) {
                contactsListViewModel.setSearchTextChanged(query.toString())
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.contacts_toolbar_menu, menu)

        when (sortByPreferences.getInt("Sort_By_Preferences", R.id.sort_by_priority)) {
            R.id.sort_by_first_name -> {
                menu.findItem(R.id.sort_by_first_name).isChecked = true
                contactsListViewModel.setSortedBy(R.id.sort_by_first_name)
            }
            R.id.sort_by_last_name -> {
                menu.findItem(R.id.sort_by_last_name).isChecked = true
                contactsListViewModel.setSortedBy(R.id.sort_by_last_name)
            }
            R.id.sort_by_priority -> {
                menu.findItem(R.id.sort_by_priority).isChecked = true
                contactsListViewModel.setSortedBy(R.id.sort_by_priority)
            }
            R.id.sort_by_favorite -> {
                menu.findItem(R.id.sort_by_favorite).isChecked = true
                contactsListViewModel.setSortedBy(R.id.sort_by_favorite)
            }
            else -> {
                menu.findItem(R.id.sort_by_priority).isChecked = true
                contactsListViewModel.setSortedBy(R.id.sort_by_priority)
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort_by_first_name -> {
                val editSortBy = sortByPreferences.edit()
                editSortBy.putInt("Sort_By_Preferences", R.id.sort_by_first_name)
                editSortBy.apply()

                item.isChecked = !item.isChecked
                contactsListViewModel.setSortedBy(R.id.sort_by_first_name)
                return true
            }
            R.id.sort_by_last_name -> {
                val editSortBy = sortByPreferences.edit()
                editSortBy.putInt("Sort_By_Preferences", R.id.sort_by_last_name)
                editSortBy.apply()
                item.isChecked = !item.isChecked
                contactsListViewModel.setSortedBy(R.id.sort_by_last_name)
                return true
            }
            R.id.sort_by_priority -> {
                val editSortBy = sortByPreferences.edit()
                editSortBy.putInt("Sort_By_Preferences", R.id.sort_by_priority)
                editSortBy.apply()
                item.isChecked = !item.isChecked
                contactsListViewModel.setSortedBy(R.id.sort_by_priority)
                return true
            }
            R.id.sort_by_favorite -> {
                val editSortBy = sortByPreferences.edit()
                editSortBy.putInt("Sort_By_Preferences", R.id.sort_by_favorite)
                editSortBy.apply()
                item.isChecked = !item.isChecked
                contactsListViewModel.setSortedBy(R.id.sort_by_favorite)
                return true
            }
            R.id.sms_filter -> {
                item.isChecked = !item.isChecked
                contactsListViewModel.setFilterBy(R.id.sms_filter)
                return true
            }
            R.id.empty_filter -> {
                item.isChecked = !item.isChecked
                contactsListViewModel.setFilterBy(R.id.empty_filter)
                return true
            }
            R.id.mail_filter -> {
                item.isChecked = !item.isChecked
                contactsListViewModel.setFilterBy(R.id.mail_filter)
                return true
            }
            R.id.whatsapp_filter -> {
                item.isChecked = !item.isChecked
                contactsListViewModel.setFilterBy(R.id.whatsapp_filter)
                return true
            }
            R.id.messenger_filter -> {
                item.isChecked = !item.isChecked
                contactsListViewModel.setFilterBy(R.id.messenger_filter)
                return true
            }
            R.id.signal_filter -> {
                item.isChecked = !item.isChecked
                contactsListViewModel.setFilterBy(R.id.signal_filter)
                return true
            }
            R.id.telegram_filter -> {
                item.isChecked = !item.isChecked
                contactsListViewModel.setFilterBy(R.id.telegram_filter)
                return true
            }
            else -> {
            }
        }

        return super.onOptionsItemSelected(item)
    }

    //endregion

    //region ======================================== DRAWER LAYOUT =========================================

    private fun setupDrawerLayout(binding: ActivityContactsListBinding) {
        binding.drawerLayout.apply {
            val menu = binding.navView.menu
            menu.findItem(R.id.nav_home).isChecked = true

            binding.navView.setNavigationItemSelectedListener { menuItem ->
                hideKeyboard(this@ContactsListActivity)
                closeDrawers()

                val itemLayout = findViewById<ConstraintLayout>(R.id.teleworking_item)
                val itemText = findViewById<AppCompatTextView>(R.id.teleworking_item_text)

                itemText.text =
                    "${getString(R.string.teleworking)} ${getString(R.string.left_drawer_settings)}"

                itemLayout.setOnClickListener {
                    startActivity(
                        Intent(
                            this@ContactsListActivity,
                            TeleworkingActivity::class.java
                        )
                    )
                }
                when (menuItem.itemId) {
                    R.id.nav_notifications -> startActivity(
                        Intent(this@ContactsListActivity, NotificationsSettingsActivity::class.java)
                    )
                    R.id.nav_in_app -> startActivity(
                        Intent(this@ContactsListActivity, PremiumActivity::class.java)
                    )
                    R.id.nav_manage_screen -> startActivity(
                        Intent(this@ContactsListActivity, ManageMyScreenActivity::class.java)
                    )
                    R.id.nav_help -> startActivity(
                        Intent(this@ContactsListActivity, HelpActivity::class.java)
                    )
                }

                true
            }

            binding.openDrawer.setOnClickListener {
                if (isOpen) {
                    closeDrawer(GravityCompat.START)
                } else {
                    openDrawer(GravityCompat.START)
                }
            }
        }
    }

    //endregion

    //region ========================================= MULTI SELECT =========================================

    private fun refreshActivity(binding: ActivityContactsListBinding) {
        binding.apply {
            toolbar.visibility = View.VISIBLE

            addNewContact.isVisible = true

            multiSelectToolbar.isVisible = false
            groupButton.isVisible = false
            smsButton.isVisible = false
            gmailButton.isVisible = false
            whatsappButton.isVisible = false
            buttonMultichannel.isVisible = false

            listOfHasSms.clear()
            listOfPhoneNumbers.clear()

            listOfHasEmail.clear()
            listOfHasWhatsapp.clear()
        }

//        startActivity(Intent(this@ContactsListActivity, ContactsListActivity::class.java))
//        finish()
    }

    private fun setupMultiSelectToolbar(binding: ActivityContactsListBinding, isVisible: Boolean) {
        binding.apply {
            if (isVisible) {
                toolbar.visibility = View.INVISIBLE
            } else {
                toolbar.visibility = View.VISIBLE
            }
            addNewContact.isVisible = !isVisible

            multiSelectToolbar.isVisible = isVisible
            groupButton.isVisible = isVisible
            smsButton.isVisible = isVisible && !listOfHasSms.contains(false)
            gmailButton.isVisible = isVisible && !listOfHasEmail.contains(false)
            whatsappButton.isVisible = isVisible && !listOfHasWhatsapp.contains(false)
            buttonMultichannel.isVisible = isVisible
        }
    }

    private fun itemSelected(image: CircularImageView, contact: ContactsListViewState) {
        if (listOfItemSelected.contains(contact.id)) {
            listOfItemSelected.remove(contact.id)

            listOfHasSms.remove(!contact.listOfPhoneNumbers.contains(""))
            if (!contact.listOfPhoneNumbers.contains("")) {
                listOfPhoneNumbers.remove(contact.listOfPhoneNumbers.random())
            }

            listOfHasEmail.remove(!contact.listOfMails.contains(""))
            if (!contact.listOfMails.contains("")) {
                listOfEmails.remove(contact.listOfMails.random())
            }

            listOfHasWhatsapp.remove(contact.hasWhatsapp)

            if (contact.profilePicture64 != "") {
                val bitmap = Converter.base64ToBitmap(contact.profilePicture64)
                image.setImageBitmap(bitmap)
            } else {
                image.setImageResource(
                    RandomDefaultImage.randomDefaultImage(
                        contact.profilePicture, this
                    )
                )
            }
        } else {
            listOfItemSelected.add(contact.id)
            image.setImageResource(R.drawable.ic_item_selected)

            listOfHasSms.add(!contact.listOfPhoneNumbers.contains(""))
            if (!contact.listOfPhoneNumbers.contains("")) {
                listOfPhoneNumbers.add(contact.listOfPhoneNumbers.random())
            }

            listOfHasEmail.add(!contact.listOfMails.contains(""))
            if (!contact.listOfMails.contains("")) {
                listOfEmails.add(contact.listOfMails.random())
            }

            listOfHasWhatsapp.add(contact.hasWhatsapp)
        }
    }

    private fun multiSelectToolbarClick(binding: ActivityContactsListBinding) {
        binding.apply {
            close.setOnClickListener {
                listOfItemSelected.clear()
                modeMultiSelect = false

                refreshActivity(binding)
                setupRecyclerView(binding)
            }

            modeDelete.setOnClickListener {
                MaterialAlertDialogBuilder(this@ContactsListActivity, R.style.AlertDialog)
                    .setTitle(getString(R.string.main_alert_dialog_delete_contact_title))
                    .setMessage(
                        String.format(
                            resources.getString(R.string.notification_history_delete_notifications_confirmation)
                        )
                    )
                    .setPositiveButton(R.string.edit_contact_validate) { _, _ ->
                        CoroutineScope(Dispatchers.IO).launch {
                            contactsListViewModel.deleteContactsSelected(listOfItemSelected)
                            listOfItemSelected.clear()
                            modeMultiSelect = false
                            withContext(Dispatchers.Main) {
                                refreshActivity(binding)
                                setupRecyclerView(binding)
                            }
                        }
                    }
                    .setNegativeButton(R.string.delete_contact_from_group_cancel) { _, _ -> }
                    .show()
            }

            modeMenu.setOnClickListener {

            }
        }
    }

    private fun floatingButtonsClick(binding: ActivityContactsListBinding) {
        binding.apply {
            addNewContact.setOnClickListener {
                startActivity(Intent(this@ContactsListActivity, AddNewContactActivity::class.java))
            }

            groupButton.setOnClickListener {
                startActivity(
                    Intent(
                        this@ContactsListActivity,
                        ManageGroupActivity::class.java
                    ).putIntegerArrayListExtra(
                        "contacts", listOfItemSelected
                    )
                )
            }
            buttonMultichannel.setOnClickListener {
                startActivity(
                    Intent(
                        this@ContactsListActivity,
                        MultiChannelActivity::class.java
                    ).putIntegerArrayListExtra(
                        "contacts", listOfItemSelected
                    )
                )
            }

            whatsappButton.setOnClickListener {
                multiChannelSendMessageWhatsapp()
            }
            smsButton.setOnClickListener {
                monoChannelSmsClick(listOfPhoneNumbers)
            }
            gmailButton.setOnClickListener {
                monoChannelMailClick(listOfEmails)
            }
        }
    }

    private fun monoChannelSmsClick(listOfPhoneNumber: ArrayList<String>) {
        var message = "smsto:" + listOfPhoneNumber[0]
        for (i in 0 until listOfPhoneNumber.size) {
            message += ";" + listOfPhoneNumber[i]
        }
        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(message)))
    }

    private fun monoChannelMailClick(listOfMail: ArrayList<String>) {
        val contact = listOfMail.toArray(arrayOfNulls<String>(listOfMail.size))
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(
            Intent.EXTRA_EMAIL,
            contact
        )
        intent.data = Uri.parse("mailto:")
        intent.type = "message/rfc822"
        intent.putExtra(Intent.EXTRA_SUBJECT, "")
        intent.putExtra(Intent.EXTRA_TEXT, "")
        startActivity(intent)
    }

    private fun multiChannelSendMessageWhatsapp() {
        val i = Intent(Intent.ACTION_VIEW)

        try {
            val url = "https://api.whatsapp.com/send?text=" + URLEncoder.encode(".", "UTF-8")
            i.setPackage("com.whatsapp")
            i.data = Uri.parse(url)
            if (i.resolveActivity(packageManager) != null) {
                startActivity(i)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //endregion

    //region =========================================== SETUP UI ===========================================

    private fun setupRecyclerView(binding: ActivityContactsListBinding) {
        val sharedPreferences = getSharedPreferences("Gridview_column", Context.MODE_PRIVATE)
        val nbGrid = sharedPreferences.getInt("gridview", 1)

        val contactsListAdapter = ContactsListAdapter(this, { id ->
            hideKeyboard(this)

            startActivity(
                Intent(this, EditContactActivity::class.java).putExtra(
                    "ContactId",
                    id
                )
            )
        }, { id, civ, contact ->
            hideKeyboard(this)

            itemSelected(civ, contact)
            modeMultiSelect = listOfItemSelected.isNotEmpty()

            setupMultiSelectToolbar(binding, modeMultiSelect)
        })

        if (nbGrid == 1) {
            binding.recyclerView.apply {
                contactsListViewModel.getAllContacts()
                    .observe(this@ContactsListActivity) { contacts ->
                        contactsListAdapter.submitList(null)
                        contactsListAdapter.submitList(contacts)
                    }
                adapter = contactsListAdapter
                layoutManager = LinearLayoutManager(context)
                setItemViewCacheSize(500)
            }
        } else {
            binding.recyclerView.apply {
                val contactsGridAdapter = ContactsGridAdapter(this@ContactsListActivity, { id ->
                    hideKeyboard(this@ContactsListActivity)

                    startActivity(
                        Intent(this@ContactsListActivity, ContactSelectedWithAppsActivity::class.java).putExtra(
                            "ContactId",
                            id
                        )
                    )
                }, { id, civ, contact ->
                    hideKeyboard(this@ContactsListActivity)

                    itemSelected(civ, contact)
                    modeMultiSelect = listOfItemSelected.isNotEmpty()

                    setupMultiSelectToolbar(binding, modeMultiSelect)
                })
                contactsListViewModel.getAllContacts()
                    .observe(this@ContactsListActivity) { contacts ->
                        contactsGridAdapter.submitList(null)
                        contactsGridAdapter.submitList(contacts)
                    }
                adapter = contactsGridAdapter
                layoutManager = GridLayoutManager(context, nbGrid)
                setItemViewCacheSize(500)
            }
        }
    }

    private fun setupBottomNavigationView(binding: ActivityContactsListBinding) {
        binding.bottomNavigation.menu.getItem(0).isChecked = true
        binding.bottomNavigation.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_groups -> {
                    startActivity(
                        Intent(
                            this@ContactsListActivity,
                            GroupsListActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    )
                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_notifcations -> {
                    startActivity(
                        Intent(
                            this@ContactsListActivity,
                            NotificationsHistoryActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    )
                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_cockpit -> {
                    startActivity(
                        Intent(
                            this@ContactsListActivity,
                            CockpitActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    )
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        })
    }

    //endregion

    //region ======================================== NOTIFICATIONS =========================================

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

    private fun toggleNotificationListenerService() {
        val pm = packageManager
        val cmpName = ComponentName(this, NotificationsListenerService::class.java)
        pm.setComponentEnabledSetting(
            cmpName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            cmpName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    //endregion
}