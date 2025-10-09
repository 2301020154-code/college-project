package com.example.trackback

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState() // Changed back to an object
    data class Success(val message: String) : UploadState()
    data class Error(val message: String) : UploadState()
}

class UploadViewModel : ViewModel() {

    private val _itemName = MutableStateFlow("")
    val itemName = _itemName.asStateFlow()
    private val _category = MutableStateFlow("")
    val category = _category.asStateFlow()
    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()
    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber = _phoneNumber.asStateFlow()
    private val _date = MutableStateFlow("")
    val date = _date.asStateFlow()
    private val _time = MutableStateFlow("")
    val time = _time.asStateFlow()
    private val _location = MutableStateFlow("")
    val location = _location.asStateFlow()
    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri = _imageUri.asStateFlow()
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState = _uploadState.asStateFlow()

    fun onItemNameChange(newName: String) { _itemName.update { newName } }
    fun onCategoryChange(newCategory: String) { _category.update { newCategory } }
    fun onDescriptionChange(newDescription: String) { _description.update { newDescription } }
    fun onPhoneNumberChange(newNumber: String) { _phoneNumber.update { newNumber } }
    fun onDateChange(newDate: String) { _date.update { newDate } }
    fun onTimeChange(newTime: String) { _time.update { newTime } }
    fun onLocationChange(newLocation: String) { _location.update { newLocation } }
    fun onImageUriChange(newUri: Uri?) { _imageUri.update { newUri } }

    fun submitItem(context: Context) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            _uploadState.value = UploadState.Error("User not logged in.")
            return
        }
        if (_imageUri.value == null) {
            _uploadState.value = UploadState.Error("Please select an image.")
            return
        }

        _uploadState.value = UploadState.Loading
        viewModelScope.launch {
            try {
                val imageUrl = uploadImageToSupabase(_imageUri.value!!, context)
                val newItem = Item(
                    name = "${_itemName.value} (Found on ${_date.value})",
                    category = _category.value,
                    description = _description.value,
                    phoneNumber = _phoneNumber.value,
                    location = _location.value,
                    imageUrl = imageUrl,
                    userId = currentUser.uid,
                    status = "FOUND"
                )
                saveItemToSupabase(newItem)
                _uploadState.value = UploadState.Success("Item uploaded successfully!")
            } catch (e: Exception) {
                Log.e("UploadViewModel", "Submission failed", e)
                _uploadState.value = UploadState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    private suspend fun uploadImageToSupabase(uri: Uri, context: Context): String {
        val bytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.readBytes()
        } ?: throw IllegalStateException("Cannot read image URI")

        val bucket = supabase.storage["item-images"]
        val path = "public/${UUID.randomUUID()}.jpg"

        // Removed the progress listener lambda
        bucket.upload(path, bytes)
        return bucket.publicUrl(path)
    }

    private suspend fun saveItemToSupabase(item: Item) {
        supabase.postgrest["items"].insert(item)
    }
}
