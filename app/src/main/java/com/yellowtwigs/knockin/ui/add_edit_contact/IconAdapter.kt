package com.yellowtwigs.knockin.ui.add_edit_contact

import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import com.yellowtwigs.knockin.R
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import android.graphics.Canvas
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yellowtwigs.knockin.ui.add_edit_contact.add.AddNewContactActivity
import com.yellowtwigs.knockin.ui.add_edit_contact.edit.EditContactActivity

class IconAdapter(private val context: Context, private val bottomSheetDialog: BottomSheetDialog) :
    RecyclerView.Adapter<IconAdapter.ViewHolder>() {
    private val iconsList: IntArray

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.icone_adapter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.imageViewIcone.setImageResource(iconsList[position])
        holder.iconeLayout.setOnClickListener {
            if (context is EditContactActivity) {
                val drawable = context.getDrawable(iconsList[position])
                val bitmap = if (drawable!!.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                    Bitmap.createBitmap(
                        1,
                        1,
                        Bitmap.Config.ARGB_8888
                    )
                } else {
                    Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    )
                }

                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                context.addContactIcon(bitmap)
                bottomSheetDialog.dismiss()
            } else if (context is AddNewContactActivity) {
                val drawable = context.getDrawable(iconsList[position])
                val bitmap = if (drawable!!.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                    Bitmap.createBitmap(
                        1,
                        1,
                        Bitmap.Config.ARGB_8888
                    )
                } else {
                    Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    )
                }
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                context.addContactIcon(bitmap)
                bottomSheetDialog.dismiss()
            }
        }
    }

    override fun getItemCount(): Int {
        return iconsList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var iconeLayout: RelativeLayout
        var imageViewIcone: AppCompatImageView

        init {
            iconeLayout = itemView.findViewById(R.id.icone_adapter_relativelayout)
            imageViewIcone = itemView.findViewById(R.id.icone_adapter_imageView)
        }
    }

    init {
        val array = context.resources.obtainTypedArray(R.array.icone_ressource)
        iconsList = IntArray(array.length())
        for (i in 0 until array.length()) {
            iconsList[i] = array.getResourceId(i, 0)
        }
    }
}