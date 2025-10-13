package com.tecsup.aurora.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
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
import com.tecsup.aurora.R
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
    private lateinit var searchView: SearchView
    private lateinit var toolbar: MaterialToolbar

    // Adapter y lista de contactos
    private lateinit var contactsAdapter: ContactsAdapter
    private val allContacts = mutableListOf<Contact>()

    // Lanzador para la solicitud de permisos
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                showContactsView()
                loadContacts()
            } else {
                showPermissionDeniedView()
                Toast.makeText(this, "Permiso denegado. No se pueden mostrar los contactos.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        // 1. Inicializar todas las vistas
        initViews()

        // 2. Configurar la Toolbar y el menú
        setupToolbar()

        // 3. Configurar el RecyclerView
        setupRecyclerView()
        registerForContextMenu(recyclerView)

        // 4. Configurar el listener del botón para pedir permiso
        btnGrantPermission.setOnClickListener {
            requestContactsPermission()
        }

        // 5. Configurar el buscador
        setupSearch()

        // 6. Comprobar el estado del permiso al iniciar
        checkPermissionAndLoadContacts()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_contacts)
        recyclerView = findViewById(R.id.recycler_view_contacts)
        layoutPermissionRequired = findViewById(R.id.layout_permission_required)
        btnGrantPermission = findViewById(R.id.btn_grant_permission)
        searchView = findViewById(R.id.search_view_contacts)
    }

    private fun setupToolbar() {
        // Le decimos a la actividad que esta es nuestra barra de acción principal.
        // ¡Este paso es MUY IMPORTANTE para que el menú aparezca!
        setSupportActionBar(toolbar)

        // Asigna la acción de "volver" al ícono de navegación (la flecha izquierda)
        toolbar.setNavigationOnClickListener {
            finish() // Cierra la actividad actual y regresa a la anterior
        }
    }

    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter(mutableListOf()) {
            Toast.makeText(this, "Mantén presionado para ver las opciones", Toast.LENGTH_SHORT).show()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = contactsAdapter
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterContacts(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterContacts(newText)
                return true
            }
        })
    }

    private fun filterContacts(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            allContacts // Si la búsqueda está vacía, muestra todos los contactos
        } else {
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
                this, Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                showContactsView()
                loadContacts()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                showPermissionRequiredView()
            }
            else -> {
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
                projection, null, null,
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

            allContacts.clear()
            allContacts.addAll(contactsList)

            withContext(Dispatchers.Main) {
                if (allContacts.isEmpty()) {
                    showEmptyView()
                } else {
                    contactsAdapter.updateContacts(allContacts)
                }
            }
        }
    }

    // --- Funciones para controlar la visibilidad de las vistas ---

    private fun showContactsView() {
        recyclerView.visibility = View.VISIBLE
        searchView.visibility = View.VISIBLE
        layoutPermissionRequired.visibility = View.GONE
    }

    private fun showPermissionRequiredView() {
        recyclerView.visibility = View.GONE
        searchView.visibility = View.GONE
        layoutPermissionRequired.visibility = View.VISIBLE
        findViewById<TextView>(R.id.text_permission_title).text = "Acceso a Contactos Requerido"
        findViewById<TextView>(R.id.text_permission_description).text = "Para mostrar tus contactos, necesitamos que nos des permiso."
        btnGrantPermission.text = "Conceder Permiso"
    }

    private fun showPermissionDeniedView() {
        recyclerView.visibility = View.GONE
        searchView.visibility = View.GONE
        layoutPermissionRequired.visibility = View.VISIBLE
        findViewById<TextView>(R.id.text_permission_title).text = "Permiso Denegado"
        findViewById<TextView>(R.id.text_permission_description).text = "Has denegado el permiso. Para usar esta función, actívalo desde los ajustes de la aplicación."
        btnGrantPermission.text = "Ir a Ajustes"
    }

    private fun showEmptyView() {
        recyclerView.visibility = View.GONE
        searchView.visibility = View.GONE
        layoutPermissionRequired.visibility = View.VISIBLE
        findViewById<TextView>(R.id.text_permission_title).text = "No hay contactos"
        findViewById<TextView>(R.id.text_permission_description).text = "Tu lista de contactos está vacía. Añade algunos para verlos aquí."
        btnGrantPermission.visibility = View.GONE
    }

    // --- Métodos para el Menú de la Toolbar ---

    //metodo para crear el menu de la toolbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_contacts2, menu)
        return true
    }

    //metodo para elegir una opcion del menu de la toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.item_add_number -> {
                Toast.makeText(this, "añadir a una lista usando solo numero", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.item_fast_help -> {
                Toast.makeText(this, "ayuda con la funcion de las listas de contactos", Toast.LENGTH_SHORT).show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // --- Métodos para el Menú Contextual (clic largo en un contacto) ---

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = contactsAdapter.longPressedPosition
        if (position == -1) {
            return super.onContextItemSelected(item)
        }

        val selectedContact = contactsAdapter.getContactAt(position)

        return when (item.itemId) {
            R.id.menu_add_trusted -> {
                selectedContact.isTrusted = true
                contactsAdapter.notifyItemChanged(position)
                Toast.makeText(this, "${selectedContact.name} añadido a contactos de confianza", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_add_emergency -> {
                selectedContact.isEmergency = true
                contactsAdapter.notifyItemChanged(position)
                Toast.makeText(this, "${selectedContact.name} añadido a contactos de emergencia", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_remove -> {
                Toast.makeText(this, "Eliminar ${selectedContact.name}", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }
}
