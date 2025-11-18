package com.tecsup.aurora.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tecsup.aurora.data.model.PhoneContact
import com.tecsup.aurora.databinding.ItemContactBinding

class PhoneContactAdapter(
    private val onLongClick: (PhoneContact) -> Unit
) : ListAdapter<PhoneContact, PhoneContactAdapter.PhoneViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhoneViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhoneViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhoneViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PhoneViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // El item completo responde a un 'long press'
            binding.root.setOnLongClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onLongClick(getItem(adapterPosition))
                }
                true // Indica que el evento fue consumido
            }
        }

        fun bind(contact: PhoneContact) {
            binding.contactName.text = contact.name
            binding.contactNumber.text = contact.number
            binding.actionIcon.visibility = View.GONE // Oculta el botón de borrar
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PhoneContact>() {
        override fun areItemsTheSame(oldItem: PhoneContact, newItem: PhoneContact) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PhoneContact, newItem: PhoneContact) = oldItem == newItem
    }
}