package com.autotyper

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ItemListAdapter(
    private val onDeleteClick: (ItemEntity) -> Unit,
    private val onItemClick: (Int) -> Unit
) : ListAdapter<ItemEntity, ItemListAdapter.ItemViewHolder>(ItemComparator()) {

    var selectedIndex: Int = -1
        set(value) {
            val oldIndex = field
            field = value
            if (oldIndex != -1) notifyItemChanged(oldIndex)
            if (value != -1) notifyItemChanged(value)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_row, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, position == selectedIndex)
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSerialNumber: TextView = itemView.findViewById(R.id.tvSerialNumber)
        private val tvText: TextView = itemView.findViewById(R.id.tvText)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val rootLayout: LinearLayout = itemView.findViewById(R.id.rootLayout)

        fun bind(item: ItemEntity, isSelected: Boolean) {
            tvSerialNumber.text = "${item.serialNumber}."
            tvText.text = item.text

            if (isSelected) {
                rootLayout.setBackgroundColor(Color.parseColor("#E0F7FA")) // Light blue
            } else {
                rootLayout.setBackgroundColor(Color.TRANSPARENT)
            }

            btnDelete.setOnClickListener { onDeleteClick(item) }
            rootLayout.setOnClickListener { onItemClick(bindingAdapterPosition) }
        }
    }

    class ItemComparator : DiffUtil.ItemCallback<ItemEntity>() {
        override fun areItemsTheSame(oldItem: ItemEntity, newItem: ItemEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ItemEntity, newItem: ItemEntity): Boolean {
            return oldItem == newItem
        }
    }
}
