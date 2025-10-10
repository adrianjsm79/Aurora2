package com.tecsup.aurora

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.tecsup.aurora.adapter.ContactsAdapter
import com.tecsup.aurora.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsActivity : BaseActivity() {

    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutPermissionRequired: LinearLayout
    private lateinit var btnGrantPermission: Button
    private lateinit var searchView: SearchView // <-- NUEVO: Para el buscador

    // Adapter y lista original
    private lateinit var contactsAdapter: ContactsAdapter
    private val allContacts = mutableListOf<Contact>() // <-- NUEVO: Lista para guardar todos los contactos

    // Launcher para solicitar permisos
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permiso concedido, cargar contactos
                showContactsView()
                loadContacts()
            } else {
                // Permiso denegado, mostrar la vista correspondiente
                showPermissionDeniedView()
                Toast.makeText(this, "Permiso denegado. No se pueden mostrar los contactos.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        // 1. Inicializar las vistas OBLIGATORIAMENTE después de setContentView
        recyclerView = findViewById(R.id.recycler_view_contacts)
        layoutPermissionRequired = findViewById(R.id.layout_permission_required)
        btnGrantPermission = findViewById(R.id.btn_grant_permission)
        searchView = findViewById(R.id.search_view_contacts) // <-- NUEVO: Inicializar SearchView

        // 2. Llamar a la configuración de la ventana (Edge-to-Edge)
        setupEdgeToEdge(R.id.main)

        // 3. Configurar el RecyclerView
        setupRecyclerView()
        registerForContextMenu(recyclerView)

        // 4. Configurar el listener del botón para pedir permiso
        btnGrantPermission.setOnClickListener {
            requestContactsPermission()
        }

        // 5. Configurar el buscador
        setupSearch() // <-- NUEVO: Llamar a la configuración del buscador

        // 6. Comprobar el estado del permiso al iniciar
        checkPermissionAndLoadContacts()

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_contacts)

        // Asigna la acción de "volver" al ícono de navegación (la flecha)
        toolbar.setNavigationOnClickListener {
            finish() // Cierra la actividad actual y regresa a la anterior
        }

        val btnAddManual: ImageButton = findViewById(R.id.btn_add_contact_manual)
        btnAddManual.setOnClickListener {
            Toast.makeText(this, "Añadir contactos manualmente", Toast.LENGTH_SHORT).show()
            //fragmento para añadir contacto manualmente
        }
    }

    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter(mutableListOf()) // Inicializa con una lista vacía
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = contactsAdapter
    }

    // <-- NUEVA FUNCIÓN -->
    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // Se llama cuando el usuario presiona "enter" o el botón de búsqueda.
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterContacts(query)
                return true
            }

            // Se llama cada vez que el texto en el buscador cambia.
            override fun onQueryTextChange(newText: String?): Boolean {
                filterContacts(newText)
                return true
            }
        })
    }

    // <-- NUEVA FUNCIÓN -->
    private fun filterContacts(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            allContacts // Si la búsqueda está vacía, muestra todos los contactos
        } else {
            // Filtra la lista completa buscando coincidencias en nombre o número
            allContacts.filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                        contact.number.contains(query, ignoreCase = true)
            }
        }
        contactsAdapter.updateContacts(filteredList.toMutableList())
    }

    private fun checkPermissionAndLoadContacts() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido
                showContactsView()
                loadContacts()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                // El usuario ya ha denegado el permiso antes. Mostramos la UI de explicación.
                showPermissionRequiredView()
            }
            else -> {
                // Primera vez que se pide el permiso
                requestContactsPermission()
            }
        }
    }

    private fun requestContactsPermission() {
        requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    private fun loadContacts() {
        lifecycleScope.launch(Dispatchers.IO) {
            val contactsList = mutableListOf<Contact>()

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoUriIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                while (it.moveToNext()) {
                    val name = it.getString(nameIndex)
                    val number = it.getString(numberIndex)
                    val photoUri = it.getString(photoUriIndex)
                    contactsList.add(Contact(name, number, photoUri))
                }
            }

            // Limpiamos la lista maestra y la volvemos a llenar
            allContacts.clear()
            allContacts.addAll(contactsList)

            withContext(Dispatchers.Main) {
                if (allContacts.isEmpty()) {
                    showEmptyView()
                } else {
                    // Actualizamos el adaptador con la lista completa inicialmente
                    contactsAdapter.updateContacts(allContacts)
                }
            }
        }
    }

    // --- Funciones para controlar la visibilidad de las vistas ---

    private fun showContactsView() {
        recyclerView.visibility = View.VISIBLE
        searchView.visibility = View.VISIBLE // <-- MODIFICADO: Muestra el buscador
        layoutPermissionRequired.visibility = View.GONE
    }

    private fun showPermissionRequiredView() {
        recyclerView.visibility = View.GONE
        searchView.visibility = View.GONE // <-- MODIFICADO: Oculta el buscador
        layoutPermissionRequired.visibility = View.VISIBLE
        findViewById<TextView>(R.id.text_permission_title).text = "Acceso a Contactos Requerido"
        findViewById<TextView>(R.id.text_permission_description).text = "Para mostrar tus contactos, necesitamos que nos des permiso."
        btnGrantPermission.text = "Conceder Permiso"
    }

    private fun showPermissionDeniedView() {
        recyclerView.visibility = View.GONE
        searchView.visibility = View.GONE // <-- MODIFICADO: Oculta el buscador
        layoutPermissionRequired.visibility = View.VISIBLE
        findViewById<TextView>(R.id.text_permission_title).text = "Permiso Denegado"
        findViewById<TextView>(R.id.text_permission_description).text = "Has denegado el permiso. Para usar esta función, actívalo desde los ajustes de la aplicación."
        btnGrantPermission.text = "Ir a Ajustes"
    }

    private fun showEmptyView() {
        recyclerView.visibility = View.GONE
        searchView.visibility = View.GONE // <-- MODIFICADO: Oculta el buscador
        layoutPermissionRequired.visibility = View.VISIBLE
        findViewById<TextView>(R.id.text_permission_title).text = "No hay contactos"
        findViewById<TextView>(R.id.text_permission_description).text = "Tu lista de contactos está vacía. Añade algunos para verlos aquí."
        btnGrantPermission.visibility = View.GONE
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = contactsAdapter.longPressedPosition
        if (position == -1) {
            return super.onContextItemSelected(item)
        }

        val selectedContact = contactsAdapter.getContactAt(position)

        when (item.itemId) {
            R.id.menu_add_trusted -> {
                selectedContact.isTrusted = true
                contactsAdapter.notifyItemChanged(position)
                Toast.makeText(this, "${selectedContact.name} añadido a contactos de confianza", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.menu_add_emergency -> {
                selectedContact.isEmergency = true
                contactsAdapter.notifyItemChanged(position)
                Toast.makeText(this, "${selectedContact.name} añadido a contactos de emergencia", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.menu_remove -> {
                Toast.makeText(this, "Eliminar ${selectedContact.name}", Toast.LENGTH_SHORT).show()
                return true
            }
            else -> return super.onContextItemSelected(item)
        }
    }

}
