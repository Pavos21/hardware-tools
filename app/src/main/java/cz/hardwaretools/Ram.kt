@file:OptIn(ExperimentalMaterial3Api::class)

package cz.hardwaretools

import android.app.ActivityManager
import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class Ram : ComponentActivity() {
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

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    RamView(windowSizeClass)
                }
            }
        }
    }
}

fun getRamTotal(context: Context): String {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)

    val totalRamBytes: Long = memoryInfo.totalMem

    // Convert bytes to megabytes and format the result
    val totalRamMB = totalRamBytes / (1024.0 * 1024.0)
    val unit = context.getString(R.string.ram_total_unit) // Assuming this is "MB"
    val formatedString = String.format(Locale.getDefault(), "%.2f %s", totalRamMB, unit)

    return formatedString
}

fun getRamAvailable(context: Context): String {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)

    val availableRamBytes: Long = memoryInfo.availMem
    val totalRamBytes: Long = memoryInfo.totalMem

    // Convert bytes to megabytes
    val availableRamMB = availableRamBytes / (1024.0 * 1024.0)

    // Calculate percentage of available RAM
    val availableRamPercentage = (availableRamBytes.toDouble() / totalRamBytes.toDouble()) * 100

    val unit = context.getString(R.string.ram_available_unit) // Assuming this is "MB"
    val formatedString = String.format(Locale.getDefault(), "%.2f %s (%.2f %%)", availableRamMB, unit, availableRamPercentage)

    return formatedString
}


@Composable
fun RamView(windowSizeClass: WindowWidthSizeClass) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // State for battery information
    val ramTotal = remember { mutableStateOf(getRamTotal(context)) }
    val ramAvailable = remember { mutableStateOf(getRamAvailable(context)) }

    // Refresh the state every second
    LaunchedEffect(Unit) {
        while (true) {
            // Update the states
            ramTotal.value = getRamTotal(context)
            ramAvailable.value = getRamAvailable(context)

            // Delay for 1 second
            delay(2000L)
        }
    }

    // Define the list of card items once
    val cardItems = listOf(
        Pair(R.string.ram_total, ramTotal.value),
        Pair(R.string.ram_available, ramAvailable.value),
    )

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
                title = { Text(stringResource(id = R.string.title_activity_ram_emoji) +" "+ stringResource(id = R.string.title_activity_ram)) },
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
fun RamViewPreview() {
    HardwareToolsTheme {
        RamView(WindowWidthSizeClass.Compact)
    }
}
