package com.example.trackback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.trackback.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    var properties by remember { mutableStateOf(MapProperties(mapType = MapType.NORMAL)) }
    var uiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = false)) }
    var showMapTypeSelector by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text("TrackBack Menu", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                Divider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
                    label = { Text("My Profile") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("profile")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "My Listings") },
                    label = { Text("My Listings") },
                    selected = false,
                    onClick = { /* TODO */ }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { /* TODO */ }
                )
                Spacer(modifier = Modifier.weight(1f))
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log Out") },
                    label = { Text("Log Out") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        Firebase.auth.signOut()
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInClient.signOut()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Campus Finds", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Filled.Menu, "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                BottomAppBar(containerColor = Color.White) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}) { Icon(Icons.Filled.Home, "Home") }
                        IconButton(onClick = {}) { Icon(Icons.Filled.Chat, "Chats") }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val cvRamanUniversity = LatLng(20.221495, 85.735871)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(cvRamanUniversity, 16f)
                }
                val coroutineScope = rememberCoroutineScope()

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = properties,
                        uiSettings = uiSettings
                    )
                    Column(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        FloatingActionButton(
                            onClick = { showMapTypeSelector = !showMapTypeSelector },
                            modifier = Modifier.size(40.dp)
                        ) { Icon(Icons.Filled.Layers, "Toggle Map Type") }
                        AnimatedVisibility(visible = showMapTypeSelector) {
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 8.dp)) {
                                SmallFloatingActionButton(
                                    onClick = { properties = properties.copy(mapType = MapType.NORMAL); showMapTypeSelector = false },
                                    modifier = Modifier.size(35.dp)
                                ) { Icon(Icons.Filled.Map, "Map View") }
                                Spacer(modifier = Modifier.height(8.dp))
                                SmallFloatingActionButton(
                                    onClick = { properties = properties.copy(mapType = MapType.SATELLITE); showMapTypeSelector = false },
                                    modifier = Modifier.size(35.dp)
                                ) { Icon(Icons.Filled.Satellite, "Satellite View") }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { coroutineScope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomIn()) } },
                            modifier = Modifier.size(40.dp)
                        ) { Icon(Icons.Default.Add, "Zoom In") }
                        Spacer(modifier = Modifier.height(8.dp))
                        FloatingActionButton(
                            onClick = { coroutineScope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomOut()) } },
                            modifier = Modifier.size(40.dp)
                        ) { Icon(Icons.Default.Remove, "Zoom Out") }
                    }
                }

                Column(
                    modifier = Modifier.weight(0.7f).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ActionButton(text = "I LOST SOMETHING", icon = Icons.Filled.Search, color = AppOrange, modifier = Modifier.weight(1f), onClick = { })
                        ActionButton(text = "I FOUND SOMETHING", icon = Icons.Filled.AddLocation, color = AppGreen, modifier = Modifier.weight(1f), onClick = { navController.navigate("upload_item") })
                    }
                    Text(
                        text = "Recent Listings",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                    ) {
                        item {
                            Text(text = "Item list will go here...", style = MaterialTheme.typography.bodyMedium, color = LightText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(text: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, textAlign = TextAlign.Center, fontSize = 14.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    TrackBackTheme {
        HomeScreen(navController = rememberNavController())
    }
}