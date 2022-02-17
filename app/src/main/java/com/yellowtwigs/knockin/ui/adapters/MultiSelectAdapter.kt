package com.yellowtwigs.knockin.ui.adapters

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yellowtwigs.knockin.R
import com.yellowtwigs.knockin.controller.CircularImageView
import com.yellowtwigs.knockin.controller.activity.MainActivity
import com.yellowtwigs.knockin.controller.activity.group.GroupManagerActivity
import com.yellowtwigs.knockin.databinding.MultiSelectItemBinding
import com.yellowtwigs.knockin.model.ModelDB.ContactWithAllInformation
import com.yellowtwigs.knockin.utils.Converter.base64ToBitmap
import com.yellowtwigs.knockin.utils.InitContactsForListAdapter.InitContactAdapter.spanNameTextView
import com.yellowtwigs.knockin.utils.RandomDefaultImage.randomDefaultImage

class MultiSelectAdapter(
    private val cxt: Activity,
    private val contactUnlimited: Boolean,
    private val onClicked: (Int) -> Unit
) :
    ListAdapter<ContactWithAllInformation, MultiSelectAdapter.ViewHolder>(
        ContactWithAllInformationComparator()
    ) {

    val listContactSelect = arrayListOf<ContactWithAllInformation>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            MultiSelectItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = getItem(position)
        holder.onBind(contact, position)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    inner class ViewHolder(private val binding: MultiSelectItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun onBind(contactWithAllInformation: ContactWithAllInformation, position: Int) {
            Log.i("testScroll", "$listContactSelect")
            binding.apply {
                val contact = contactWithAllInformation.contactDB
                if (contact != null)
                    spanNameTextView(
                        contact.firstName,
                        contact.lastName,
                        0.9f,
                        contactFirstName,
                        contactLastName
                    )
                if (contact?.profilePicture64 != "") {
                    val bitmap = contact?.profilePicture64?.let { base64ToBitmap(it) }
                    contactImage.setImageBitmap(bitmap)
                } else {
                    contactImage.setImageResource(randomDefaultImage(contact.profilePicture, cxt))
                }

                root.setOnClickListener {
                    onClicked(position)

                    if (cxt is MainActivity || cxt is GroupManagerActivity) {
                        if (listContactSelect.contains(contactWithAllInformation)) {
                            contactImage.setImageResource(R.drawable.ic_item_selected)
                        } else {
                            if (contact?.profilePicture64 != "") {
                                val bitmap = contact?.profilePicture64?.let { base64ToBitmap(it) }
                                contactImage.setImageBitmap(bitmap)
                            } else {
                                contactImage.setImageResource(
                                    randomDefaultImage(
                                        contact.profilePicture,
                                        cxt
                                    )
                                )
                            }
                        }
                    } else {
                        if (listContactSelect.contains(contactWithAllInformation)) {
                            setBorderColor(contactImage, R.color.priorityTwoColor)
                        } else {
                            setBorderColor(contactImage, R.color.lightColor)
                        }
                    }
                }
            }
        }

        private fun setBorderColor(cv: CircularImageView, res: Int) {
            cv.setBorderColor(ResourcesCompat.getColor(cxt.resources, res, null))
        }
    }

    class ContactWithAllInformationComparator : DiffUtil.ItemCallback<ContactWithAllInformation>() {
        override fun areItemsTheSame(
            oldItem: ContactWithAllInformation,
            newItem: ContactWithAllInformation
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: ContactWithAllInformation,
            newItem: ContactWithAllInformation
        ): Boolean {
            return oldItem.contactDB == newItem.contactDB &&
                    oldItem.contactDetailList == newItem.contactDetailList
        }
    }

    fun itemSelected(position: Int) {
        val contact = getItem(position)
        if (listContactSelect.contains(contact)) {
            listContactSelect.remove(contact)
        } else {
            if (listContactSelect.size < 5 || contactUnlimited) {
                listContactSelect.add(contact)
            }
        }
    }

    fun itemDeselected() {
        listContactSelect.clear()
    }
}