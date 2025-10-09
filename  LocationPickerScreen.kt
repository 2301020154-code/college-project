package com.example.trackback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.trackback.ui.theme.TrackBackTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(navController: NavController) {
    val cvRamanUniversity = LatLng(20.221495, 85.735871)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(cvRamanUniversity, 16f)
    }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var properties by remember { mutableStateOf(MapProperties(mapType = MapType.NORMAL)) }
    var uiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = false)) }
    var showMapTypeSelector by remember { mutableStateOf(false) }

    // New state for the search functionality
    var searchQuery by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf(emptyList<String>()) } // Placeholder for predictions

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Location") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedLocation != null) {
                FloatingActionButton(onClick = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("picked_location", selectedLocation)
                    navController.popBackStack()
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Confirm Location")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    selectedLocation = latLng
                    searchQuery = "" // Clear search when map is clicked
                    predictions = emptyList()
                },
                properties = properties,
                uiSettings = uiSettings
            ) {
                if (selectedLocation != null) {
                    Marker(state = rememberMarkerState(position = selectedLocation!!), title = "Selected Location")
                }
            }

            // --- UI FOR SEARCH BAR AND RESULTS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        // TODO: Call Places API to get predictions
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search for a place...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                if (predictions.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                        items(predictions) { prediction ->
                            Text(
                                text = prediction,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // TODO: Handle suggestion click
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }

            // Map controls are still here
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // ... Map type selector code is the same ...
            }
            Column(
                modifier = Modifier.align(Alignment.TopStart).padding(top = 72.dp, start = 8.dp)
            ) {
                // ... Custom zoom controls are the same ...
            }

            if (selectedLocation == null && predictions.isEmpty()) {
                Text(
                    "Tap on the map or use the search bar",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

// Preview remains the same
@Preview
@Composable
fun LocationPickerScreenPreview() {
    TrackBackTheme {
        LocationPickerScreen(navController = rememberNavController())
    }
}