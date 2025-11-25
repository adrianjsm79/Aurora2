package com.tecsup.aurora.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tecsup.aurora.data.model.TrustedContact
import com.tecsup.aurora.databinding.ItemContactBinding

class TrustedContactAdapter(
    private val isEditable: Boolean = true,
    private val onRemoveClick: (TrustedContact) -> Unit
) : ListAdapter<TrustedContact, TrustedContactAdapter.TrustedViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrustedViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrustedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrustedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TrustedViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.actionIcon.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onRemoveClick(getItem(adapterPosition))
                }
            }
        }

        fun bind(contact: TrustedContact) {
            binding.contactName.text = contact.nombre
            binding.contactNumber.text = contact.numero
            if (isEditable) {
                binding.actionIcon.visibility = View.VISIBLE
                binding.actionIcon.isEnabled = true
            } else {
                binding.actionIcon.visibility = View.GONE
                binding.actionIcon.isEnabled = false
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<TrustedContact>() {
        override fun areItemsTheSame(oldItem: TrustedContact, newItem: TrustedContact) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TrustedContact, newItem: TrustedContact) = oldItem == newItem
    }
}