package com.example.safewalk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safewalk.data.model.EmergencyContact
import com.example.safewalk.ui.viewmodel.EmergencyContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    navController: NavController,
    viewModel: EmergencyContactsViewModel = hiltViewModel(),
) {
    val contacts by viewModel.contacts.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var selectedContact by remember { mutableStateOf<com.example.safewalk.contacts.SystemContact?>(null) }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ContactsEvent.ContactAdded -> {
                    snackbarHostState.showSnackbar("Contact added successfully!")
                }
                is ContactsEvent.ContactUpdated -> {
                    snackbarHostState.showSnackbar("Contact updated!")
                }
                is ContactsEvent.ContactDeleted -> {
                    snackbarHostState.showSnackbar("Contact deleted!")
                }

                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Contacts") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "Add")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Phone, "No Contacts", Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No Contacts Yet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Add emergency contacts to be notified in emergencies")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "Add")
                        Spacer(Modifier.width(8.dp))
                        Text("Add Your First Contact")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(contacts) { contact ->
                    ContactListItem(
                        contact = contact,
                        onEdit = { showAddDialog = true },
                        onDelete = { viewModel.deleteContact(contact) },
                        onTogglePrimary = { viewModel.togglePrimary(contact) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onShowPicker = { showContactPicker = true },
            onAdd = { contact ->
                viewModel.addContact(contact)
                showAddDialog = false
            },
        )
    }

    if (showContactPicker) {
        ContactPickerDialog(
            onDismiss = { showContactPicker = false },
            onContactSelected = { contact ->
                selectedContact = contact
                showContactPicker = false
                showAddDialog = true
            },
            viewModel = viewModel,
        )
    }

    if (selectedContact != null && showAddDialog) {
        AddContactDialog(
            initialContact = selectedContact,
            onDismiss = {
                showAddDialog = false
                selectedContact = null
            },
            onShowPicker = { showContactPicker = true },
            onAdd = { contact ->
                viewModel.addContact(contact)
                showAddDialog = false
                selectedContact = null
            },
        )
    }
}

@Composable
private fun ContactListItem(
    contact: EmergencyContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePrimary: () -> Unit,
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    contact.name.first().toString(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(contact.name, fontWeight = FontWeight.Bold)
                    if (contact.isPrimary) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.Star, "Primary", tint = Color.Yellow, modifier = Modifier.size(16.dp))
                    }
                }
                Text(contact.phone, fontSize = 12.sp)
                if (contact.relationship.isNotEmpty()) {
                    Text(contact.relationship, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row {
                IconButton(onClick = onTogglePrimary) {
                    Icon(
                        Icons.Filled.Star,
                        "Primary",
                        modifier = Modifier.size(20.dp),
                        tint = if (contact.isPrimary) Color.Yellow else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, "Edit", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onShowPicker: () -> Unit,
    onAdd: (EmergencyContact) -> Unit,
    initialContact: com.example.safewalk.contacts.SystemContact? = null,
) {
    var name by remember { mutableStateOf(initialContact?.name ?: "") }
    var phone by remember { mutableStateOf(initialContact?.phone ?: "") }
    var relationship by remember { mutableStateOf("") }
    var isPrimary by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .padding(18.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Add Emergency Contact", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))

                TextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))

                TextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Primary Contact")
                    Switch(checked = isPrimary, onCheckedChange = { isPrimary = it })
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedButton(
                        onClick = onShowPicker,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Contacts, "Pick", Modifier.size(18.dp))
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Close, "Cancel", Modifier.size(18.dp))
                    }
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && phone.isNotEmpty()) {
                                onAdd(
                                    EmergencyContact(
                                        name = name,
                                        phone = phone,
                                        relationship = relationship,
                                        isPrimary = isPrimary,
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Check, "Add", Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactPickerDialog(
    onDismiss: () -> Unit,
    onContactSelected: (com.example.safewalk.contacts.SystemContact) -> Unit,
    viewModel: EmergencyContactsViewModel,
) {
    val systemContacts by viewModel.systemContacts.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoadingContacts.collectAsState(initial = false)
    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = remember(systemContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            systemContacts
        } else {
            systemContacts.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSystemContacts()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Select Contact",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    }
                } else if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (searchQuery.isBlank()) "No contacts found" else "No matching contacts",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredContacts.size) { index ->
                            val contact = filteredContacts[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onContactSelected(contact)
                                    }
                                    .padding(0.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            contact.name.first().toString().uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            contact.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                        )
                                        Text(
                                            contact.phone,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

sealed class ContactsEvent {
    data object ContactAdded : ContactsEvent()
    data object ContactUpdated : ContactsEvent()
    data object ContactDeleted : ContactsEvent()
}