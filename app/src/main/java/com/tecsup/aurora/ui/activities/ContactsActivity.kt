package com.tecsup.aurora.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.R
import com.tecsup.aurora.data.model.PhoneContact
import com.tecsup.aurora.databinding.ActivityContactsBinding
import com.tecsup.aurora.ui.adapter.PhoneContactAdapter
import com.tecsup.aurora.ui.adapter.TrustedContactAdapter
import com.tecsup.aurora.ui.fragments.AddByNumberDialogFragment
import com.tecsup.aurora.ui.fragments.HelpDialogFragment
import com.tecsup.aurora.viewmodel.ContactsViewModel

class ContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactsBinding

    // 1. Obtener el ViewModel (que se comparte con los fragments)
    private val viewModel: ContactsViewModel by viewModels {
        (application as MyApplication).contactsViewModelFactory
    }

    // 2. Declarar los adaptadores para las dos listas
    private lateinit var phoneAdapter: PhoneContactAdapter
    private lateinit var trustedAdapter: TrustedContactAdapter

    // --- MANEJO DE PERMISOS ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido, cargar contactos
            viewModel.loadAllContacts()
        } else {
            // Permiso denegado, mostrar error
            Toast.makeText(this, "El permiso para leer contactos es necesario", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Contactos"

        setupRecyclerViews()
        setupListeners()
        observeViewModel()

        // Iniciar la cadena de permisos
        requestContactsPermission()
    }

    private fun requestContactsPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // El permiso ya está concedido, carga los datos
                viewModel.loadAllContacts()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                // Muestra un diálogo explicando por qué necesitas el permiso
                AlertDialog.Builder(this)
                    .setTitle("Permiso Necesario")
                    .setMessage("Aurora necesita leer tus contactos para que puedas agregarlos a tu lista de confianza.")
                    .setPositiveButton("Entendido") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            else -> {
                // Pide el permiso directamente
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun setupRecyclerViews() {
        // --- Adapter para Contactos del Teléfono (Lista Larga) ---
        phoneAdapter = PhoneContactAdapter { phoneContact ->
            // Este es el listener 'onLongClick'
            // Muestra un diálogo para confirmar si se añade
            showAddContactDialog(phoneContact)
        }
        binding.recyclerPhoneContacts.layoutManager = LinearLayoutManager(this)
        binding.recyclerPhoneContacts.adapter = phoneAdapter

        // --- Adapter para Contactos de Confianza (Lista Corta) ---
        trustedAdapter = TrustedContactAdapter { trustedContact ->
            // Este es el listener 'onRemoveClick'
            viewModel.removeTrustedContact(trustedContact)
        }
        binding.recyclerTrustedContacts.layoutManager = LinearLayoutManager(this)
        binding.recyclerTrustedContacts.adapter = trustedAdapter
    }

    /**
     * Muestra el diálogo de confirmación para añadir un contacto
     */
    private fun showAddContactDialog(contact: PhoneContact) {
        AlertDialog.Builder(this)
            .setTitle("Añadir a Confianza")
            .setMessage("¿Deseas añadir a ${contact.name} (${contact.number}) a tu lista de confianza?")
            .setPositiveButton("Añadir") { _, _ ->
                viewModel.addTrustedContact(contact.number)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupListeners() {
        // Listener para la barra de búsqueda
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // No hacer nada al presionar "Enter"
            override fun onQueryTextSubmit(query: String?): Boolean = false

            // Filtrar la lista en tiempo real
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.filterContacts(newText.orEmpty())
                return true
            }
        })
    }

    private fun observeViewModel() {
        // Observa el estado de la UI desde el ViewModel
        viewModel.uiState.observe(this) { state ->

            // (Aquí puedes manejar el 'state.isLoading' con un ProgressBar)

            // Actualiza la lista de contactos de confianza
            trustedAdapter.submitList(state.trustedContacts)

            // Actualiza la lista filtrada de contactos del teléfono
            phoneAdapter.submitList(state.filteredPhoneContacts)

            // Muestra mensajes de error o éxito (ej. "Usuario no encontrado")
            state.toastMessage?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                viewModel.toastShown() // Marca el mensaje como "consumido"
            }
        }
    }

    // --- Manejo del Menú de la Toolbar (Ayuda, Añadir por Número) ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_contacts2, menu) // Usa tu 'menu_contacts2.xml'
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Botón de "volver" en la toolbar
            android.R.id.home -> {
                finish()
                true
            }
            // Opción "Añadir por Número"
            R.id.item_add_by_number -> {
                AddByNumberDialogFragment().show(supportFragmentManager, "AddByNumberDialog")
                true
            }
            // Opción "Ayuda Rápida"
            R.id.item_help -> {
                HelpDialogFragment().show(supportFragmentManager, "HelpDialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}