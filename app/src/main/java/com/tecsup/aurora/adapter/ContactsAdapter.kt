package com.tecsup.aurora.adapter

import android.net.Uri
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.tecsup.aurora.R
import com.tecsup.aurora.model.Contact

class ContactsAdapter(
    private val contacts: MutableList<Contact>,
    private val onItemClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    // Variable para guardar la posición del ítem presionado
    var longPressedPosition: Int = -1
        private set

    // VIEWHOLDER
    // Lo hacemos una inner class para acceder a la variable de posición
    inner class ContactViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener { // 1. Implementamos la interfaz

        val contactName: TextView = itemView.findViewById(R.id.text_contact_name)
        val contactInitial: TextView = itemView.findViewById(R.id.text_contact_initial)
        val contactPhoto: ShapeableImageView = itemView.findViewById(R.id.image_contact_photo)
        val indicatorEmergency: ImageView = itemView.findViewById(R.id.indicator_emergencia)
        val indicatorTrusted: ImageView = itemView.findViewById(R.id.indicator_confiable)

        init {
            //registra la vista para el menú contextual
            itemView.setOnCreateContextMenuListener(this)
        }

        //sobre el metodo para crear el menu
        override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            //inflamos el menú que creamos en XML
            menu?.setHeaderTitle("opciones") // Título opcional
            menu?.add(this.adapterPosition, R.id.menu_add_trusted, 0, "Añadir a contactos de confianza")
            menu?.add(this.adapterPosition, R.id.menu_add_emergency, 1, "Añadir a contactos de emergencia")
            menu?.add(this.adapterPosition, R.id.menu_remove, 2, "Eliminar de lista")
        }
    }
    //fin de viewholder


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contact_list_item, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.contactName.text = contact.name

        holder.itemView.setOnClickListener {
            // Ejecuta la función que recibimos en el constructor
            onItemClick(contact)
        }

        //logica para mostrar foto de perfil
        if (contact.photoUri != null) {
            holder.contactPhoto.setImageURI(Uri.parse(contact.photoUri))
            holder.contactPhoto.visibility = View.VISIBLE
            holder.contactInitial.visibility = View.GONE
        } else {
            holder.contactInitial.text = contact.name.firstOrNull()?.uppercase() ?: ""
            holder.contactInitial.visibility = View.VISIBLE
            holder.contactPhoto.visibility = View.GONE
        }

        holder.indicatorEmergency.visibility = if (contact.isEmergency) View.VISIBLE else View.GONE
        holder.indicatorTrusted.visibility = if (contact.isTrusted) View.VISIBLE else View.GONE

        //con esto guarda la posición cuando se hace una pulsación larga
        holder.itemView.setOnLongClickListener {
            longPressedPosition = holder.adapterPosition
            false //devolve 'false' para que el evento continúe y se cree el menú contextual
        }
    }

    override fun getItemCount() = contacts.size

    fun updateContacts(newContacts: List<Contact>) {
        contacts.clear()
        contacts.addAll(newContacts)
        notifyDataSetChanged()
    }

    //función para obtener el contacto presionado desde la Activity
    fun getContactAt(position: Int): Contact {
        return contacts[position]
    }
}
