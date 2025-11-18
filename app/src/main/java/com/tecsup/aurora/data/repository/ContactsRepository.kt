package com.tecsup.aurora.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.tecsup.aurora.data.model.PhoneContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {

    // Esta función es 'suspend' y usa Dispatchers.IO
    // porque leer contactos puede ser muy lento.
    suspend fun getPhoneContacts(): List<PhoneContact> = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<PhoneContact>()
        val contentResolver = context.contentResolver

        // Columnas que queremos leer
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        // Query al ContentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC" // Ordenar alfabéticamente
        )

        cursor?.use {
            val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val id = it.getString(idCol)
                val name = it.getString(nameCol)
                val number = it.getString(numberCol)

                // Limpia el número (quita espacios, guiones)
                val cleanNumber = number.replace(Regex("[\\s-]"), "")

                contactsList.add(PhoneContact(id, name, cleanNumber))
            }
        }

        // Devuelve la lista (eliminando duplicados por número)
        return@withContext contactsList.distinctBy { it.number }
    }
}