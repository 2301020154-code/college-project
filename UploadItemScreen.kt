package com.example.trackback

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.trackback.ui.theme.LightGray
import com.example.trackback.ui.theme.LightText
import com.example.trackback.ui.theme.TrackBackTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.util.*

@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(fusedLocationClient: FusedLocationProviderClient, context: Context, onAddressFetched: (String) -> Unit) {
    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
        if (loc != null) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                val address = addresses?.firstOrNull()?.getAddressLine(0) ?: "Lat: ${loc.latitude}, Lon: ${loc.longitude}"
                onAddressFetched(address)
            } catch (e: Exception) {
                onAddressFetched("Could not get address")
            }
        } else {
            Toast.makeText(context, "Could not get location. Try again.", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun getTmpFileUri(context: Context): Uri {
    val tmpFile = File.createTempFile("tmp_image_file", ".png", context.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tmpFile)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadItemScreen(
    navController: NavController,
    uploadViewModel: UploadViewModel
) {
    val itemName by uploadViewModel.itemName.collectAsState()
    val category by uploadViewModel.category.collectAsState()
    val description by uploadViewModel.description.collectAsState()
    val phoneNumber by uploadViewModel.phoneNumber.collectAsState()
    val date by uploadViewModel.date.collectAsState()
    val time by uploadViewModel.time.collectAsState()
    val location by uploadViewModel.location.collectAsState()
    val imageUri by uploadViewModel.imageUri.collectAsState()
    val uploadState by uploadViewModel.uploadState.collectAsState()

    var showPhotoDialog by remember { mutableStateOf(false) }
    var showImagePreview by remember { mutableStateOf(false) }
    var isItemNameError by remember { mutableStateOf(false) }
    var isDescriptionError by remember { mutableStateOf(false) }
    var isPhoneNumberError by remember { mutableStateOf(false) }
    var isDateError by remember { mutableStateOf(false) }
    var isTimeError by remember { mutableStateOf(false) }
    var isLocationError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(uploadState) {
        when (val state = uploadState) {
            is UploadState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            is UploadState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val backStackEntry = navController.currentBackStackEntry
    val lifecycleOwner = LocalContext.current as LifecycleOwner

    DisposableEffect(backStackEntry) {
        val observer = androidx.lifecycle.Observer<LatLng> { latLng ->
            if (latLng != null) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    val address = addresses?.firstOrNull()?.getAddressLine(0) ?: "Lat: ${latLng.latitude}, Lon: ${latLng.longitude}"
                    uploadViewModel.onLocationChange(address)
                    backStackEntry?.savedStateHandle?.remove<LatLng>("picked_location")
                } catch (e: Exception) {
                    uploadViewModel.onLocationChange("Could not get address")
                }
            }
        }
        backStackEntry?.savedStateHandle?.getLiveData<LatLng>("picked_location")?.observe(lifecycleOwner, observer)
        onDispose {
            backStackEntry?.savedStateHandle?.getLiveData<LatLng>("picked_location")?.removeObserver(observer)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uploadViewModel.onImageUriChange(uri) }
    )
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> if (success) { uploadViewModel.onImageUriChange(tempCameraUri) } }
    )
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                tempCameraUri = getTmpFileUri(context)
                cameraLauncher.launch(tempCameraUri)
            } else {
                Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                fetchCurrentLocation(fusedLocationClient, context) { address ->
                    uploadViewModel.onLocationChange(address)
                }
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Upload Found Item", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                AddPhotosBox(
                    imageUri = imageUri,
                    onClearClick = { uploadViewModel.onImageUriChange(null) },
                    onAddClick = { showPhotoDialog = true },
                    onImageClick = { if (imageUri != null) showImagePreview = true }
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(
                    value = itemName,
                    onValueChange = { uploadViewModel.onItemNameChange(it); isItemNameError = false },
                    label = "Item Name",
                    placeholder = "e.g., iPhone 14 Pro, Blue Umbrella",
                    isRequired = true, isError = isItemNameError
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(
                    value = category,
                    onValueChange = { uploadViewModel.onCategoryChange(it) },
                    label = "Category (Optional)",
                    placeholder = "e.g., Electronics, Keys, Wallet"
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(
                    value = description,
                    onValueChange = { uploadViewModel.onDescriptionChange(it); isDescriptionError = false },
                    label = "Description",
                    placeholder = "Add details like color, size, any identifying marks...",
                    isRequired = true, isError = isDescriptionError,
                    singleLine = false, minLines = 3
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text("Location & Contact", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (location.isNotBlank()) {
                    Text(text = location, style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text(text = "Please select a location", color = if (isLocationError) MaterialTheme.colorScheme.error else Color.Unspecified)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            val permission = Manifest.permission.ACCESS_FINE_LOCATION
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                fetchCurrentLocation(fusedLocationClient, context) { address ->
                                    uploadViewModel.onLocationChange(address)
                                }
                            } else {
                                locationPermissionLauncher.launch(permission)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Current Location")
                    }
                    OutlinedButton(
                        onClick = { navController.navigate("location_picker") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Choose on Map")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(
                    value = phoneNumber,
                    onValueChange = { uploadViewModel.onPhoneNumberChange(it); isPhoneNumberError = false },
                    label = "Phone Number",
                    isRequired = true,
                    isError = isPhoneNumberError,
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    keyboardType = KeyboardType.Phone
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(
                    value = date,
                    onValueChange = { uploadViewModel.onDateChange(it); isDateError = false },
                    label = "Date",
                    placeholder = "e.g., 08/10/2025",
                    isRequired = true, isError = isDateError,
                    leadingIcon = { Icon(Icons.Default.DateRange, null) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(
                    value = time,
                    onValueChange = { uploadViewModel.onTimeChange(it); isTimeError = false },
                    label = "Time",
                    placeholder = "e.g., 03:00 PM",
                    isRequired = true, isError = isTimeError,
                    leadingIcon = { Icon(Icons.Default.Schedule, null) }
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        isItemNameError = itemName.isBlank()
                        isDescriptionError = description.isBlank()
                        isLocationError = location.isBlank()
                        isPhoneNumberError = phoneNumber.isBlank()
                        isDateError = date.isBlank()
                        isTimeError = time.isBlank()
                        val isFormValid = !isItemNameError && !isDescriptionError && !isLocationError && !isPhoneNumberError && !isDateError && !isTimeError
                        if (imageUri == null) {
                            Toast.makeText(context, "Please select an image", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isFormValid) {
                            uploadViewModel.submitItem(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SUBMIT ITEM", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (showPhotoDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoDialog = false },
                title = { Text("Choose an option") },
                confirmButton = {
                    TextButton(onClick = {
                        showPhotoDialog = false
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Choose from Gallery") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPhotoDialog = false
                        val permission = Manifest.permission.CAMERA
                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                            tempCameraUri = getTmpFileUri(context)
                            cameraLauncher.launch(tempCameraUri)
                        } else {
                            cameraPermissionLauncher.launch(permission)
                        }
                    }) { Text("Take Photo") }
                }
            )
        }
        if (showImagePreview && imageUri != null) {
            FullScreenImageViewer(imageUri = imageUri!!, onDismiss = { showImagePreview = false })
        }
        if (uploadState is UploadState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun FullScreenImageViewer(imageUri: Uri, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Full-screen preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    imageVector = Icons.Filled.Close, "Close preview", tint = Color.White,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                )
            }
        }
    }
}

@Composable
fun AddPhotosBox(imageUri: Uri?, onClearClick: () -> Unit, onAddClick: () -> Unit, onImageClick: () -> Unit) {
    Box {
        Surface(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, LightGray)
        ) {
            if (imageUri == null) {
                Box(
                    modifier = Modifier.fillMaxSize().clickable(onClick = onAddClick),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoCamera, "Add Photos", tint = LightText)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Add Photos", color = LightText)
                    }
                }
            } else {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected image",
                    modifier = Modifier.fillMaxSize().clickable(onClick = onImageClick),
                    contentScale = ContentScale.Fit
                )
            }
        }
        if (imageUri != null) {
            IconButton(
                onClick = onClearClick,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Cancel, "Remove Image", tint = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.background(Color.White, shape = CircleShape)
                )
            }
        }
    }
}

@Composable
fun CustomTextField(
    value: String, onValueChange: (String) -> Unit, label: String,
    placeholder: String = "", isRequired: Boolean = false, isError: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null, readOnly: Boolean = false,
    singleLine: Boolean = true, minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = {
            Text(buildAnnotatedString {
                append(label)
                if (isRequired) {
                    withStyle(style = SpanStyle(color = Color.Red)) { append(" *") }
                }
            })
        },
        placeholder = { Text(placeholder) },
        leadingIcon = leadingIcon,
        isError = isError,
        supportingText = {
            if (isError) {
                Text(text = "Required field", color = MaterialTheme.colorScheme.error)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = if (singleLine) 1 else 5,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = LightGray,
            unfocusedContainerColor = LightGray,
        )
    )
}

@Preview(showBackground = true)
@Composable
fun UploadItemScreenPreview() {
    TrackBackTheme {
        UploadItemScreen(navController = rememberNavController(), uploadViewModel = viewModel())
    }
}
