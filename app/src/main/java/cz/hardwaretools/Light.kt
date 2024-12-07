@file:OptIn(ExperimentalMaterial3Api::class)

package cz.hardwaretools

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.window.layout.WindowMetricsCalculator
import cz.hardwaretools.ui.theme.HardwareToolsTheme
import kotlin.math.roundToInt

class Light : ComponentActivity() {
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
                    LightView(windowSizeClass)
                }
            }
        }
    }
}

fun getLightSensorValue(context: Context): Float {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    return if (lightSensor != null) {
        // You can return a placeholder or default value here
        0f
    } else {
        -1f // Indicate that the light sensor is not available
    }
}


@Composable
fun LightView(windowSizeClass: WindowWidthSizeClass) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // State for the light sensor value
    val lightValue = remember { mutableStateOf("0 lux") }

    // SensorManager
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    // Sensor event listener
    val sensorEventListener = rememberUpdatedState(newValue = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                // Update the light value, rounded to whole numbers
                val roundedValue = it.values[0].roundToInt()
                lightValue.value = "$roundedValue ${context.getString(R.string.light_unit)}"
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Not used
        }
    })

    DisposableEffect(Unit) {
        lightSensor?.let {
            sensorManager.registerListener(sensorEventListener.value, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        onDispose {
            sensorManager.unregisterListener(sensorEventListener.value)
        }
    }

    // Define the list of card items
    val cardItems = listOf(
        Pair(R.string.light_text, lightValue.value),
    )

    // (Rest of your LightView implementation remains the same)

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
        else -> 17.sp  // Default value
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
                title = { Text(stringResource(id = R.string.title_activity_light_emoji) +" "+ stringResource(id = R.string.title_activity_light)) },
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
            items(cardItems.size) { index ->
                val (textResId, additionalText) = cardItems[index]
                val text = stringResource(id = textResId)

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
                                    text = text,
                                    textAlign = TextAlign.Left,
                                    fontSize = fontSize,
                                )
                                Spacer(modifier = Modifier.height(8.dp)) // Space between text items
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = valueText),
                                    text = additionalText,
                                    textAlign = TextAlign.Left,
                                    fontSize = fontSize,
                                    fontWeight = FontWeight.Bold, // Make the text bold
                                )
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
fun LightViewPreview() {
    HardwareToolsTheme {
        LightView(WindowWidthSizeClass.Compact)
    }
}