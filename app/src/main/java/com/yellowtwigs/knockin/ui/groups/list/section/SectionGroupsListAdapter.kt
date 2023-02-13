package com.yellowtwigs.knockin.ui.groups.list.section

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.*
import com.yellowtwigs.knockin.R
import com.yellowtwigs.knockin.databinding.GroupSectionItemBinding
import com.yellowtwigs.knockin.ui.CircularImageView
import com.yellowtwigs.knockin.ui.groups.list.ContactInGroupViewState
import com.yellowtwigs.knockin.ui.groups.list.GroupsGridFiveAdapter
import com.yellowtwigs.knockin.ui.groups.list.GroupsGridFourAdapter
import com.yellowtwigs.knockin.ui.groups.list.GroupsListAdapter
import com.yellowtwigs.knockin.ui.groups.manage_group.ManageGroupActivity
import com.yellowtwigs.knockin.utils.Converter
import com.yellowtwigs.knockin.utils.RandomDefaultImage
import java.net.URLEncoder

class SectionGroupsListAdapter(
    private val cxt: Context, private val packageManager: PackageManager, private val onClickedCallback: (Int) -> Unit
) : ListAdapter<SectionViewState, SectionGroupsListAdapter.ViewHolder>(
    SectionViewStateComparator()
) {

    private var isMultiSelect = false
    var listOfItemSelected = ArrayList<ContactInGroupViewState>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            GroupSectionItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    inner class ViewHolder(private val binding: GroupSectionItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun onBind(section: SectionViewState) {
            val sharedPreferences = cxt.getSharedPreferences("Gridview_column", Context.MODE_PRIVATE)
            val nbGrid = sharedPreferences.getInt("gridview", 1)

            binding.apply {
                groupName.text = section.title

                when (section.sectionColor) {
                    R.color.red_tag_group -> {
                        groupNameSection.background = ResourcesCompat.getDrawable(
                            cxt.resources, R.drawable.rounded_button_color_red, null
                        )
                    }
                    R.color.green_tag_group -> {
                        groupNameSection.background = ResourcesCompat.getDrawable(
                            cxt.resources, R.drawable.rounded_button_color_green, null
                        )
                    }
                    R.color.blue_tag_group -> {
                        groupNameSection.background = ResourcesCompat.getDrawable(
                            cxt.resources, R.drawable.rounded_button_color_blue, null
                        )
                    }
                    R.color.purple_tag_group -> {
                        groupNameSection.background = ResourcesCompat.getDrawable(
                            cxt.resources, R.drawable.rounded_button_color_purple, null
                        )
                    }
                    R.color.yellow_tag_group -> {
                        groupNameSection.background = ResourcesCompat.getDrawable(
                            cxt.resources, R.drawable.rounded_button_color_yellow, null
                        )
                    }
                    R.color.orange_tag_group -> {
                        groupNameSection.background = ResourcesCompat.getDrawable(
                            cxt.resources, R.drawable.rounded_button_color_orange, null
                        )
                    }
                    else -> {
                        groupNameSection.background = ResourcesCompat.getDrawable(
                            cxt.resources, R.drawable.rounded_button_color_red, null
                        )
                    }
                }

                val groupsListAdapter = GroupsListAdapter(cxt, !isMultiSelect, { id ->
                }) { id, civ, contact ->
                    itemSelected(civ, contact)
                    isMultiSelect = listOfItemSelected.isNotEmpty()
                }

                sectionRecyclerView.apply {
                    if (nbGrid == 1) {
                        groupsListAdapter.submitList(section.items)
                        adapter = groupsListAdapter
                        layoutManager = LinearLayoutManager(context)
                    } else {
                        val groupsGridAdapter = if (nbGrid == 4) {
                            GroupsGridFourAdapter(cxt, false, {})
                        } else {
                            GroupsGridFiveAdapter(cxt, false, {})
                        }
                        groupsGridAdapter.submitList(section.items)
                        adapter = groupsGridAdapter
                        layoutManager = GridLayoutManager(context, nbGrid)
                    }
                }

                groupNameSection.setOnClickListener {
                    if (listOfItemSelected.isEmpty()) {
                        isMultiSelect = true
                        listOfItemSelected.addAll(section.items)
                        itemsSelected(groupsListAdapter, section.items)
                    } else {
                        isMultiSelect = false
                        listOfItemSelected.clear()
                    }

                    sectionRecyclerView.apply {
                        if (nbGrid == 1) {
                            groupsListAdapter.submitList(section.items)
                            adapter = groupsListAdapter
                            layoutManager = LinearLayoutManager(context)
                        } else {
                            val groupsGridAdapter = if (nbGrid == 4) {
                                GroupsGridFourAdapter(cxt, false, {})
                            } else {
                                GroupsGridFiveAdapter(cxt, false, {})
                            }
                            groupsGridAdapter.submitList(section.items)
                            adapter = groupsGridAdapter
                            layoutManager = GridLayoutManager(context, nbGrid)
                        }
                    }
                }

                sectionMore.setOnClickListener {
                    val popupMenu = PopupMenu(cxt, it)
                    popupMenu.inflate(R.menu.section_menu_group_manager)
                    popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                        when (item.itemId) {
                            R.id.menu_group_edit_group -> {
                                cxt.startActivity(
                                    Intent(
                                        cxt, ManageGroupActivity::class.java
                                    ).putExtra("GroupId", section.id)
                                )
                            }
                            R.id.menu_group_delete_group -> {
                                onClickedCallback(section.id)
                            }
                        }
                        return@setOnMenuItemClickListener true
                    }
                    popupMenu.show()
                }
            }
        }

        private fun itemSelected(image: CircularImageView, contact: ContactInGroupViewState) {
            if (listOfItemSelected.contains(contact)) {
                listOfItemSelected.remove(contact)

                if (contact.profilePicture64 != "") {
                    val bitmap = Converter.base64ToBitmap(contact.profilePicture64)
                    image.setImageBitmap(bitmap)
                } else {
                    image.setImageResource(
                        RandomDefaultImage.randomDefaultImage(
                            contact.profilePicture, cxt
                        )
                    )
                }
            } else {
                listOfItemSelected.add(contact)
                image.setImageResource(R.drawable.ic_item_selected)
            }
        }

        private fun itemsSelected(groupsListAdapter: GroupsListAdapter, items: List<ContactInGroupViewState>) {
            items.map {
                it.profilePicture = R.drawable.ic_item_selected
            }
            groupsListAdapter.submitList(items)
        }
    }

    class SectionViewStateComparator : DiffUtil.ItemCallback<SectionViewState>() {
        override fun areItemsTheSame(
            oldItem: SectionViewState, newItem: SectionViewState
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: SectionViewState, newItem: SectionViewState
        ): Boolean {
            return oldItem.id == newItem.id && oldItem.title == newItem.title && oldItem.sectionColor == newItem.sectionColor && oldItem.items == newItem.items && oldItem.phoneNumbers == newItem.phoneNumbers && oldItem.emails == newItem.emails
        }
    }
}