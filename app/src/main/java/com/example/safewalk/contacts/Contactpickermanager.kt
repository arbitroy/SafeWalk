package com.example.safewalk.contacts

import android.content.Context
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import android.Manifest
import android.content.pm.PackageManager
import com.example.safewalk.permissions.PermissionsManager

@Singleton
class ContactPickerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionsManager: PermissionsManager,
) {

    suspend fun getSystemContacts(): List<SystemContact> = withContext(Dispatchers.IO) {
        if (!permissionsManager.hasContactsPermission()) {
            return@withContext emptyList()
        }

        val contacts = mutableListOf<SystemContact>()
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
            ),
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

            while (it.moveToNext()) {
                val contactId = it.getString(idIndex)
                val name = it.getString(nameIndex)

                // Get phone numbers for this contact
                val phones = getPhoneNumbers(contactId)
                if (phones.isNotEmpty()) {
                    phones.forEach { phone ->
                        contacts.add(
                            SystemContact(
                                id = contactId,
                                name = name,
                                phone = phone,
                            )
                        )
                    }
                }
            }
        }

        contacts
    }

    private fun getPhoneNumbers(contactId: String): List<String> {
        val phones = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            arrayOf(contactId),
            null
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val phone = it.getString(numberIndex)
                if (phone != null && phone.isNotEmpty()) {
                    phones.add(phone)
                }
            }
        }

        return phones
    }
}

data class SystemContact(
    val id: String,
    val name: String,
    val phone: String,
)