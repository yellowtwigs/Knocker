package com.yellowtwigs.knockin.repositories.contacts.list

import androidx.lifecycle.MutableLiveData
import com.yellowtwigs.knockin.model.dao.ContactsDao
import com.yellowtwigs.knockin.model.data.ContactDB
import javax.inject.Inject

class ContactsListRepositoryImpl @Inject constructor(private val dao: ContactsDao) :
    ContactsListRepository {

    private val searchBarText = MutableLiveData<String>()
    private val sortByLiveData = MutableLiveData<Int>()
    private val filterByLiveData = MutableLiveData<Int>()

    override fun getAllContacts() = dao.getAllContacts()
    override fun getContact(id: Int) = dao.getContact(id)

    override suspend fun updateContact(contact: ContactDB) = dao.updateContact(contact)

    override suspend fun updateContactPriorityById(id: Int, priority: Int) {
        dao.updateContactPriorityById(id, priority)
    }

    override suspend fun deleteContact(contact: ContactDB) = dao.deleteContact(contact)

    override fun getSearchBarText() = searchBarText

    override fun setSearchBarText(text: String) {
        searchBarText.postValue(text)
    }

    override fun getSortedBy() = sortByLiveData

    override fun setSortedBy(sortBy: Int) {
        sortByLiveData.postValue(sortBy)
    }

    override fun getFilterBy() = filterByLiveData

    override fun setFilterBy(filterBy: Int) {
        filterByLiveData.postValue(filterBy)
    }
}