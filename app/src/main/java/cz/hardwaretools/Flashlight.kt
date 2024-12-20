@file:OptIn(ExperimentalMaterial3Api::class)

package cz.hardwaretools

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.window.layout.WindowMetricsCalculator
import cz.hardwaretools.ui.theme.HardwareToolsTheme

class Flashlight : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Make sure system windows fit correctly
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {
            val windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
            val windowSizeClass = calculateWindowSizeClass(windowMetrics)

            HardwareToolsTheme {
                // Extract the surface color inside the composable
                val surfaceColor = MaterialTheme.colorScheme.surface

                // Set status bar color outside of a composable using LaunchedEffect
                LaunchedEffect(surfaceColor) {
                    window.statusBarColor = surfaceColor.toArgb()
                }

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    FlashlightView(windowSizeClass)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Turn off the flashlight when the activity is stopped
        FlashlightManager.turnOff(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Turn off the flashlight when the activity is destroyed
        FlashlightManager.turnOff(this)
    }
}

object FlashlightManager {

    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null

    // Initialize camera manager and get the camera ID
    fun init(context: Context) {
        if (cameraManager == null) {
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraId = cameraManager?.let {
                val cameraList = it.cameraIdList
                // Loop through the available cameras and find the back camera (usually at index 0)
                cameraList.firstOrNull { id ->
                    val characteristics = it.getCameraCharacteristics(id)
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
            }
        }
    }

    // Turn on the flashlight
    fun turnOn(context: Context) {
        init(context)
        cameraId?.let { id ->
            cameraManager?.setTorchMode(id, true)
        }
    }

    // Turn off the flashlight
    fun turnOff(context: Context) {
        init(context)
        cameraId?.let { id ->
            cameraManager?.setTorchMode(id, false)
        }
    }
}


@Composable
fun FlashlightView(windowSizeClass: WindowWidthSizeClass) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    var isFlashlightOn = remember { mutableStateOf(false) }

    // Flashlight control function
    fun toggleFlashlight() {
        isFlashlightOn.value = !isFlashlightOn.value

        // Flashlight control logic
        if (isFlashlightOn.value) {
            // Turn on the flashlight
            FlashlightManager.turnOn(context)
        } else {
            // Turn off the flashlight
            FlashlightManager.turnOff(context)
        }
    }

    // Responsive card padding and image height based on window size class
    val cardPadding = when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> 12.dp
        WindowWidthSizeClass.Medium -> 16.dp
        WindowWidthSizeClass.Expanded -> 20.dp
        else -> 8.dp
    }

    val fontSize = when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> 17.sp
        WindowWidthSizeClass.Medium -> 19.sp
        WindowWidthSizeClass.Expanded -> 21.sp
        else -> 17.sp
    }

    val maxWidth = when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> 1f
        WindowWidthSizeClass.Medium -> 0.85f
        WindowWidthSizeClass.Expanded -> 0.70f
        else -> 1f
    }

    val valueText = when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> 30.dp
        WindowWidthSizeClass.Medium -> 40.dp
        WindowWidthSizeClass.Expanded -> 50.dp
        else -> 30.dp
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.title_activity_flashlight_emoji) +" "+ stringResource(id = R.string.title_activity_flashlight)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.secondary,
                    titleContentColor = colors.onSecondary
                ),
                navigationIcon = {
                    IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.back), // Replace with your back icon resource
                            contentDescription = "Back",
                            tint = colors.onPrimary
                        )
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(it) // Handle inner padding from Scaffold
                .fillMaxSize()
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center // Center the content
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(maxWidth) // Fill based on provided max width
                    ) {
                        // Card for text content
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = cardPadding)
                                .padding(horizontal = cardPadding),
                            colors = CardDefaults.cardColors(colors.surface),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = if (isFlashlightOn.value) stringResource(id = R.string.flash_text_off) else stringResource(id = R.string.flash_text_on),
                                    textAlign = TextAlign.Left,
                                    fontSize = fontSize,
                                )
                                Spacer(modifier = Modifier.height(8.dp)) // Space between text items

                                // Image Button replacing Text
                                IconButton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.CenterHorizontally), // Center the button horizontally
                                    onClick = { toggleFlashlight() } // Toggle flashlight
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (isFlashlightOn.value) R.drawable.flash_on else R.drawable.flash_off
                                        ), // Change icon based on flashlight state
                                        contentDescription = if (isFlashlightOn.value) "Flashlight On" else "Flashlight Off",
                                        modifier = Modifier.size(24.dp) // Adjust size as needed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(cardPadding)) // Adjust the height as needed
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun FlashlightViewPreview() {
    HardwareToolsTheme {
        FlashlightView(WindowWidthSizeClass.Compact)
    }
}