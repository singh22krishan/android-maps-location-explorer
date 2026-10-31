package com.krishansingh.locationexplorer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

data class OntarioLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String
)

private val ontarioLocations = listOf(
    OntarioLocation("Niagara Falls", 43.0896, -79.0849, "Iconic waterfall on the Ontario-USA border"),
    OntarioLocation("Toronto", 43.6629, -79.3957, "Ontario's capital and largest city"),
    OntarioLocation("Ottawa", 45.4215, -75.6972, "Canada's capital city"),
    OntarioLocation("Windsor", 42.3149, -83.0364, "Located across from Detroit, Michigan"),
    OntarioLocation("London", 42.9849, -81.2453, "University city in southwestern Ontario"),
    OntarioLocation("Kingston", 44.2307, -76.4860, "Historic city on Lake Ontario"),
    OntarioLocation("Thunder Bay", 48.3809, -89.2477, "Port city in northwestern Ontario")
)

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val selectedLocation = mutableStateOf<OntarioLocation?>(null)
    private val currentLocation = mutableStateOf<LatLng?>(null)
    private val showingMap = mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchCurrentLocation()
            showingMap.value = true
        } else {
            Toast.makeText(
                this,
                "Location permission denied. Destination map is still available.",
                Toast.LENGTH_LONG
            ).show()
            showingMap.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    LocationExplorerApp(
                        selectedLocation = selectedLocation.value,
                        currentLocation = currentLocation.value,
                        showingMap = showingMap.value,
                        onSelect = ::handleSelection,
                        onBack = {
                            showingMap.value = false
                            selectedLocation.value = null
                            currentLocation.value = null
                        }
                    )
                }
            }
        }
    }

    private fun handleSelection(location: OntarioLocation) {
        selectedLocation.value = location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation()
            showingMap.value = true
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            currentLocation.value = location?.let { LatLng(it.latitude, it.longitude) }
        }.addOnFailureListener {
            currentLocation.value = null
            Toast.makeText(this, "Current location is unavailable.", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun LocationExplorerApp(
    selectedLocation: OntarioLocation?,
    currentLocation: LatLng?,
    showingMap: Boolean,
    onSelect: (OntarioLocation) -> Unit,
    onBack: () -> Unit
) {
    if (showingMap && selectedLocation != null) {
        DestinationMap(selectedLocation, currentLocation, onBack)
    } else {
        LocationList(onSelect)
    }
}

@Composable
private fun LocationList(onSelect: (OntarioLocation) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ontario Location Explorer", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            "Choose a destination to view it on Google Maps.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ontarioLocations) { location ->
                ElevatedCard(Modifier.fillMaxWidth().clickable { onSelect(location) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(location.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(location.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationMap(
    destination: OntarioLocation,
    currentLocation: LatLng?,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val destinationLatLng = remember(destination) { LatLng(destination.latitude, destination.longitude) }
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(destinationLatLng, 14f)
    }
    var fitted by remember(destination, currentLocation) { mutableStateOf(false) }

    LaunchedEffect(destination, currentLocation) {
        if (!fitted) {
            if (currentLocation != null) {
                val bounds = LatLngBounds.Builder()
                    .include(destinationLatLng)
                    .include(currentLocation)
                    .build()
                camera.animate(CameraUpdateFactory.newLatLngBounds(bounds, 180), 1200)
            } else {
                camera.animate(CameraUpdateFactory.newLatLngZoom(destinationLatLng, 15f), 900)
            }
            fitted = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            onMapClick = { point ->
                Toast.makeText(
                    context,
                    "Lat ${String.format("%.4f", point.latitude)}, Lng ${String.format("%.4f", point.longitude)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        ) {
            Marker(
                state = MarkerState(destinationLatLng),
                title = destination.name,
                snippet = destination.description,
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )
            if (currentLocation != null) {
                Marker(
                    state = MarkerState(currentLocation),
                    title = "Your current location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
        ) {
            Surface(shape = MaterialTheme.shapes.small, shadowElevation = 3.dp) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.padding(10.dp))
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentLocation != null) {
                FloatingActionButton(onClick = {
                    scope.launch { camera.animate(CameraUpdateFactory.newLatLngZoom(currentLocation, 17f), 800) }
                }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Center on my location")
                }
            }
            FloatingActionButton(onClick = {
                scope.launch { camera.animate(CameraUpdateFactory.newLatLngZoom(destinationLatLng, 16f), 800) }
            }) {
                Icon(Icons.Default.Navigation, contentDescription = "Center on destination")
            }
        }
    }
}
