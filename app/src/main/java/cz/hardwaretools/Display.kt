@file:OptIn(ExperimentalMaterial3Api::class)

package cz.hardwaretools

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
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
import androidx.compose.ui.util.fastRoundToInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.window.layout.WindowMetricsCalculator
import cz.hardwaretools.ui.theme.HardwareToolsTheme
import java.util.Locale
import kotlin.math.sqrt

class Display : ComponentActivity() {
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
                    DisplayView(windowSizeClass)
                }
            }
        }
    }
}

fun getDisplayWidth(context: Context): String {
    val displayMetrics = context.resources.displayMetrics
    val widthPixels = displayMetrics.widthPixels
    return "$widthPixels px"
}

fun getDisplayHeight(context: Context): String {
    val displayMetrics = context.resources.displayMetrics
    val heightPixels = displayMetrics.heightPixels
    return "$heightPixels px"
}

fun getScreenFrequency(context: Context): String {
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val displays = displayManager.displays
    val primaryDisplay = displays.firstOrNull() // Get the first available display (usually the default one)
    val refreshRate = primaryDisplay?.refreshRate?.fastRoundToInt() ?: 0
    return "$refreshRate Hz"
}

fun getScreenDiagonal(context: Context): String {
    val displayMetrics = context.resources.displayMetrics

    // Get screen width and height in pixels
    val widthPixels = displayMetrics.widthPixels
    val heightPixels = displayMetrics.heightPixels

    // Get physical DPI values
    val xDpi = displayMetrics.xdpi
    val yDpi = displayMetrics.ydpi

    // Convert pixels to inches
    val widthInches = widthPixels / xDpi
    val heightInches = heightPixels / yDpi

    // Calculate diagonal in inches
    val diagonalInches = sqrt(widthInches * widthInches + heightInches * heightInches)

    // Convert diagonal to centimeters (1 inch = 2.54 cm)
    val diagonalCm = diagonalInches * 2.54

    val formattedString = String.format(Locale.getDefault(), "%.2f cm / %.1f in", diagonalCm, diagonalInches)

    return formattedString
}

fun getScreenDensity(context: Context): String {
    val displayMetrics = context.resources.displayMetrics
    val densityDpi = displayMetrics.densityDpi
    return "$densityDpi dpi"
}

fun getScreenOrientation(context: Context): String {
    val orientation = context.resources.configuration.orientation
    return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        context.getString(R.string.landscape)
    } else {
        context.getString(R.string.portrait)
    }
}

@Composable
fun DisplayView(windowSizeClass: WindowWidthSizeClass) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // Define the list of card items once
    val cardItems = listOf(
        Pair(R.string.display_width, getDisplayWidth(context)),
        Pair(R.string.display_height, getDisplayHeight(context)),
        Pair(R.string.display_freq, getScreenFrequency(context)),
        Pair(R.string.display_diag, getScreenDiagonal(context)),
        Pair(R.string.display_dpi, getScreenDensity(context)),
        Pair(R.string.display_orientation, getScreenOrientation(context)),
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
                title = { Text(stringResource(id = R.string.title_activity_display_emoji) +" "+ stringResource(id = R.string.title_activity_display)) },
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
fun DisplayViewPreview() {
    HardwareToolsTheme {
        DisplayView(WindowWidthSizeClass.Compact)
    }
}