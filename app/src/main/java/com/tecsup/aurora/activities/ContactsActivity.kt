package com.tecsup.aurora.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecsup.aurora.R
import com.tecsup.aurora.adapter.ContactsAdapter
import com.tecsup.aurora.databinding.ActivityContactsBinding
import com.tecsup.aurora.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.content.Intent // Asegúrate de tener esta importación
import android.net.Uri       // Asegúrate de tener esta importación
import android.provider.Settings // Asegúrate de tener esta importación


class ContactsActivity : BaseActivity() {

    // Views -> Reemplazado por View Binding
    private lateinit var binding: ActivityContactsBinding

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
        // 1. Inflar el layout usando View Binding
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()

        // 2. Configurar la Toolbar y el menú
        setupToolbar()

        // 3. Configurar el RecyclerView
        setupRecyclerView()
        registerForContextMenu(binding.recyclerViewContacts)

        // 4. Configurar el listener del botón para pedir permiso
        binding.btnGrantPermission.setOnClickListener {
            requestContactsPermission()
        }

        // 5. Configurar el buscador
        setupSearch()

        // 6. Comprobar el estado del permiso al iniciar
        checkPermissionAndLoadContacts()
    }

    // La función initViews() ya no es necesaria con View Binding

    private fun setupToolbar() {
        // Le decimos a la actividad que esta es nuestra barra de acción principal.
        // ¡Este paso es MUY IMPORTANTE para que el menú aparezca!
        setSupportActionBar(binding.toolbarContacts)

        // Asigna la acción de "volver" al ícono de navegación (la flecha izquierda)
        binding.toolbarContacts.setNavigationOnClickListener {
            finish() // Cierra la actividad actual y regresa a la anterior
        }
    }

    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter(mutableListOf()) {
            Toast.makeText(this, "Mantén presionado para ver las opciones", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewContacts.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewContacts.adapter = contactsAdapter
    }

    private fun setupSearch() {
        binding.searchViewContacts.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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
        binding.recyclerViewContacts.visibility = View.VISIBLE
        binding.searchViewContacts.visibility = View.VISIBLE
        binding.layoutPermissionRequired.visibility = View.GONE
    }

    private fun showPermissionRequiredView() {
        binding.recyclerViewContacts.visibility = View.GONE
        binding.searchViewContacts.visibility = View.GONE
        binding.layoutPermissionRequired.visibility = View.VISIBLE
        binding.textPermissionTitle.text = "Acceso a Contactos Requerido"
        binding.textPermissionDescription.text = "Para mostrar tus contactos, necesitamos que nos des permiso."
        binding.btnGrantPermission.text = "Conceder Permiso"
        binding.btnGrantPermission.setOnClickListener {
            requestContactsPermission()
        }
    }

    private fun showPermissionDeniedView() {
        binding.recyclerViewContacts.visibility = View.GONE
        binding.searchViewContacts.visibility = View.GONE
        binding.layoutPermissionRequired.visibility = View.VISIBLE
        binding.textPermissionTitle.text = "Permiso Denegado"
        binding.textPermissionDescription.text = "Has denegado el permiso. Para usar esta función, actívalo desde los ajustes de la aplicación."
        binding.btnGrantPermission.text = "Ir a Ajustes"
        binding.btnGrantPermission.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
    }

    private fun showEmptyView() {
        binding.recyclerViewContacts.visibility = View.GONE
        binding.searchViewContacts.visibility = View.GONE
        binding.layoutPermissionRequired.visibility = View.VISIBLE
        binding.textPermissionTitle.text = "No hay contactos"
        binding.textPermissionDescription.text = "Tu lista de contactos está vacía. Añade algunos para verlos aquí."
        binding.btnGrantPermission.visibility = View.GONE
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
                Toast.makeText(this, "añadir a una lista usando numero", Toast.LENGTH_SHORT).show()
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
