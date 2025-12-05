package com.tecsup.aurora.ui.activities

import android.Manifest
import android.content.Intent
import android.view.View
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

    private val viewModel: ContactsViewModel by viewModels {
        (application as MyApplication).contactsViewModelFactory
    }

    private lateinit var phoneAdapter: PhoneContactAdapter
    private lateinit var trustedAdapter: TrustedContactAdapter
    private lateinit var trustedByAdapter: TrustedContactAdapter


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.loadAllContacts()
        } else {
            Toast.makeText(this, "El permiso para leer contactos es necesario", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Contactos"

        setupRecyclerViews()
        setupListeners()
        observeViewModel()

        requestContactsPermission()
    }

    private fun requestContactsPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.loadAllContacts()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
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
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun setupRecyclerViews() {
        phoneAdapter = PhoneContactAdapter { phoneContact ->
            showAddContactDialog(phoneContact)
        }
        binding.recyclerPhoneContacts.apply {
            layoutManager = LinearLayoutManager(this@ContactsActivity)
            adapter = phoneAdapter
        }

        // 2. A quién cuido (Editable = true, puedo borrar)
        trustedAdapter = TrustedContactAdapter(isEditable = true) { trustedContact ->
            showRemoveContactDialog(trustedContact)
        }
        binding.recyclerTrustedContacts.apply {
            layoutManager = LinearLayoutManager(this@ContactsActivity)
            adapter = trustedAdapter
        }

        // 3. Quién me cuida (Editable = false, NO puedo borrar)
        trustedByAdapter = TrustedContactAdapter(isEditable = false) {
            Toast.makeText(this@ContactsActivity, "No puedes eliminar a quien te cuida desde aquí.", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerTrustedByContacts.apply {
            layoutManager = LinearLayoutManager(this@ContactsActivity)
            adapter = trustedByAdapter
        }
    }

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

    private fun showRemoveContactDialog(contact: com.tecsup.aurora.data.model.TrustedContact) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Contacto")
            .setMessage("¿Deseas eliminar a ${contact.nombre} de tu lista de confianza?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.removeTrustedContact(contact)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupListeners() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.filterContacts(newText.orEmpty())
                return true
            }
        })
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->

            phoneAdapter.submitList(state.filteredPhoneContacts)
            trustedAdapter.submitList(state.trustedContacts)
            trustedByAdapter.submitList(state.trustedByContacts)

            binding.emptyTrustedView.visibility =
                if (state.trustedContacts.isEmpty()) View.VISIBLE else View.GONE

            binding.emptyTrustedByView.visibility =
                if (state.trustedByContacts.isEmpty()) View.VISIBLE else View.GONE // <-- NUEVO

            state.toastMessage?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                viewModel.toastShown()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_contacts2, menu) // Usa tu 'menu_contacts2.xml'
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            R.id.item_add_by_number -> {
                AddByNumberDialogFragment().show(supportFragmentManager, "AddByNumberDialog")
                true
            }

            R.id.item_help -> {
                HelpDialogFragment().show(supportFragmentManager, "HelpDialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}