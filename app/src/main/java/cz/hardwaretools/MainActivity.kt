@file:OptIn(ExperimentalMaterial3Api::class)

package cz.hardwaretools

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import cz.hardwaretools.ui.theme.HardwareToolsTheme

class MainActivity : ComponentActivity() {
    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        appUpdateManager = AppUpdateManagerFactory.create(this)

        /*
        // Make sure system windows fit correctly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets.Type.systemBars()
            WindowInsets.Type.displayCutout()
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets.Type.systemBars()
            WindowInsets.Type.displayCutout()
        }else{
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {
            val windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
            val windowSizeClass = calculateWindowSizeClass(windowMetrics)

            HardwareToolsTheme {

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppContent(windowSizeClass)
                }
            }
        }
        checkForUpdates()
    }


    private fun checkForUpdates() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                requestUpdate(appUpdateInfo)
            }
        }
    }

    private fun requestUpdate(appUpdateInfo: AppUpdateInfo) {
        val updateOptions = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()

        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            this,
            updateOptions,
            REQUEST_CODE_UPDATE
        )
    }

    companion object {
        private const val REQUEST_CODE_UPDATE = 1001
    }
}

data class CardItem(
    val id: Int, // Unique ID for each card
    val text: String,
    val drawableId: Int,
    val activityClass: Class<out ComponentActivity>
)

@Composable
fun AppContent(windowSizeClass: WindowWidthSizeClass) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    // Initialize Shared Preferences
    val sharedPreferences = context.getSharedPreferences("card_order_prefs", MODE_PRIVATE)
    val editor = sharedPreferences.edit()

    // Load saved card order (default order is the current list if no order is saved)
    val defaultCardOrder = listOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19
    )

    // Load saved card order
    val savedCardOrder = sharedPreferences.getString("card_order_key", null)
        ?.split(",")
        ?.map { it.toInt() }
        ?: defaultCardOrder

    // Append missing IDs from defaultCardOrder to savedCardOrder
    val fullCardOrder = savedCardOrder.toMutableList().apply {
        // Add any missing items from defaultCardOrder
        val newItemsAdded = addAll(defaultCardOrder.filter { it !in this })

        // Save the updated order only if new items were added
        if (newItemsAdded) {
            val updatedOrderString = joinToString(",")
            editor.putString("card_order_key", updatedOrderString)
            editor.apply()
        }
    }

    // Define a list of CardItem with the order set by the saved preferences
    val cardItems = listOf(
        CardItem(0, context.getString(R.string.title_activity_battery), R.drawable.battery, Battery::class.java),
        CardItem(1, context.getString(R.string.title_activity_camera), R.drawable.camera, Camera::class.java),
        CardItem(2, context.getString(R.string.title_activity_cpu), R.drawable.cpu, Cpu::class.java),
        CardItem(3, context.getString(R.string.title_activity_ram), R.drawable.ram, Ram::class.java),
        CardItem(4, context.getString(R.string.title_activity_wifi), R.drawable.wifi, Wifi::class.java),
        CardItem(5, context.getString(R.string.title_activity_bluetooth), R.drawable.bluetooth, Bluetooth::class.java),
        CardItem(6, context.getString(R.string.title_activity_flashlight), R.drawable.flashlight, Flashlight::class.java),
        CardItem(7, context.getString(R.string.title_activity_otg), R.drawable.otg, Otg::class.java),
        CardItem(8, context.getString(R.string.title_activity_storage), R.drawable.storage, Storage::class.java),
        CardItem(9, context.getString(R.string.title_activity_speaker), R.drawable.speaker, Speaker::class.java),
        CardItem(10, context.getString(R.string.title_activity_nfc), R.drawable.nfc, Nfc::class.java),
        CardItem(11, context.getString(R.string.title_activity_brightness), R.drawable.brightness, Brightness::class.java),
        CardItem(12, context.getString(R.string.title_activity_location), R.drawable.location, Location::class.java),
        CardItem(13, context.getString(R.string.title_activity_compass), R.drawable.compass, Compass::class.java),
        CardItem(14, context.getString(R.string.title_activity_display), R.drawable.display, Display::class.java),
        CardItem(15, context.getString(R.string.title_activity_vibration), R.drawable.vibration, Vibration::class.java),
        CardItem(16, context.getString(R.string.title_activity_infraport), R.drawable.infraport, Infraport::class.java),
        CardItem(17, context.getString(R.string.title_activity_fingerprint), R.drawable.fingerprint, Fingerprint::class.java),
        CardItem(18, context.getString(R.string.title_activity_light), R.drawable.light, Light::class.java),
        CardItem(19, context.getString(R.string.title_activity_proximity), R.drawable.proximity, Proximity::class.java)
    )

    // Sort cards based on the saved order
    val sortedCardItems = fullCardOrder.map { id -> cardItems.first { it.id == id } }

    Scaffold(
        modifier = Modifier.background(colors.background),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.secondary,
                    titleContentColor = colors.onSecondary
                ),
                actions = {
                    IconButton(onClick = {
                        // Create an intent to start the SettingsActivity
                        val intent = Intent(context, Settings::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.settings), // Replace with your icon resource
                            contentDescription = stringResource(id = R.string.title_activity_settings), // Provide content description
                            tint = colors.onSecondary // Adjust the tint color if needed
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val contentPadding = when (windowSizeClass) {
                WindowWidthSizeClass.Compact -> 12.dp
                WindowWidthSizeClass.Medium -> 16.dp
                WindowWidthSizeClass.Expanded -> 24.dp
                else -> 8.dp  // Default value
            }

            val aspectRatio = when (windowSizeClass) {
                WindowWidthSizeClass.Compact -> 1.4f
                WindowWidthSizeClass.Medium -> 2.5f
                WindowWidthSizeClass.Expanded -> 3.5f
                else -> 1.4f  // Default value
            }

            val fontSize = when (windowSizeClass) {
                WindowWidthSizeClass.Compact -> 18.sp
                WindowWidthSizeClass.Medium -> 22.sp
                WindowWidthSizeClass.Expanded -> 26.sp
                else -> 18.sp  // Default value
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // Two columns as you had it
                contentPadding = PaddingValues(contentPadding),
                verticalArrangement = Arrangement.spacedBy(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(contentPadding)
            ) {
                items(sortedCardItems.size) { index ->
                    val cardItem = sortedCardItems[index]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth() // Set the width to fill the available space
                            .padding(contentPadding) // Optional padding for spacing
                            .clickable {
                                val intent = Intent(context, cardItem.activityClass)
                                context.startActivity(intent)
                            },
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(), // Padding inside the card
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = cardItem.drawableId),
                                contentDescription = cardItem.text,
                                tint = MaterialTheme.colorScheme.onPrimary,  // Set the tint color from MaterialTheme
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspectRatio)
                                    .padding(bottom = 8.dp)
                            )
                            Text(
                                text = cardItem.text,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = fontSize,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun calculateWindowSizeClass(windowMetrics: androidx.window.layout.WindowMetrics): WindowWidthSizeClass {
    val widthDp = windowMetrics.bounds.width() / LocalContext.current.resources.displayMetrics.density
    return when {
        widthDp < 600 -> WindowWidthSizeClass.Compact // Mobile phones
        widthDp < 900 -> WindowWidthSizeClass.Medium  // Small tablets (e.g., 7-inch)
        else -> WindowWidthSizeClass.Expanded         // Large tablets (e.g., 10-inch and larger)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    HardwareToolsTheme {
        AppContent(WindowWidthSizeClass.Compact)
    }
}