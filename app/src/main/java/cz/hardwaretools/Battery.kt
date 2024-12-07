@file:OptIn(ExperimentalMaterial3Api::class)

package cz.hardwaretools

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
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
import kotlinx.coroutines.delay
import java.util.Locale

class Battery : ComponentActivity() {
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
                    BatteryView(windowSizeClass)
                }
            }
        }
    }
}

fun getBatteryPercents(context: Context): String {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val unit = context.getString(R.string.charge_level_unit)
    return "$batteryLevel $unit"
}


fun getBatteryCapacityCurrent(context: Context): String {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val unit = context.getString(R.string.battery_capacity_current_unit)
    return try {
        val capacity = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        if (capacity < 0) {
            "---"  // Return an empty string if capacity is invalid
        } else {
            "${Math.round(capacity / 1000.0)} $unit"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "error"  // Return an empty string if there's an exception
    }
}

@SuppressLint("PrivateApi")
fun getBatteryCapacityFactory(context: Context): String {
    val powerProfileClass = "com.android.internal.os.PowerProfile"
    val unit = context.getString(R.string.battery_capacity_factory_unit)
    return try {
        val powerProfile = Class.forName(powerProfileClass)
            .getConstructor(Context::class.java)
            .newInstance(context)

        val batteryCapacity = Class.forName(powerProfileClass)
            .getMethod("getAveragePower", String::class.java)
            .invoke(powerProfile, "battery.capacity") as Double

        kotlin.math.round(batteryCapacity).toInt().toString()+" $unit"
    } catch (e: Exception) {
        e.printStackTrace()
        "error"
    }
}

fun getBatteryVoltage(context: Context): String {
    val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val batteryStatus = context.registerReceiver(null, intentFilter)
    val unit = context.getString(R.string.battery_voltage_unit)

    val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1

    return if (voltage == -1) {
        "---"  // Print "---" when voltage is not found
    } else {
        // Convert millivolts to volts and format to 2 decimal places with a dot
        val voltageInVolts = voltage / 1000.0
        String.format(Locale.US, "%.2f %s", voltageInVolts, unit)
    }
}

fun getBatteryTemperature(context: Context): String {
    val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val batteryStatus = context.registerReceiver(null, intentFilter)

    // Retrieve the battery temperature in tenths of degrees Celsius
    val temperature = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1

    return if (temperature == -1) {
        "---"  // Print "---" when temperature is not found
    } else {
        // Convert tenths of Celsius to actual Celsius
        val tempInCelsius = temperature / 10.0
        // Convert Celsius to Fahrenheit
        val tempInFahrenheit = tempInCelsius * 9 / 5 + 32
        // Format both values to 1 decimal place and append units
        val celsiusString = String.format(Locale.US, "%.1f %s", tempInCelsius, context.getString(R.string.unit_celsius))
        val fahrenheitString = String.format(Locale.US, "%.1f %s ", tempInFahrenheit, context.getString(R.string.unit_fahrenheit))
        "$celsiusString / $fahrenheitString"
    }
}

fun getChargingStatus(context: Context): String {
    val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val batteryStatus = context.registerReceiver(null, intentFilter)

    // Get the battery charging status
    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
    val isFull = status == BatteryManager.BATTERY_STATUS_FULL

    // Get the plugged status (USB, AC, or wireless charging)
    val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
    val isUsbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
    val isAcCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
    val isWirelessCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS

    return when {
        isFull -> context.getString(R.string.battery_full)
        isCharging && isUsbCharge -> context.getString(R.string.charging_usb)
        isCharging && isAcCharge -> context.getString(R.string.charging_ac)
        isCharging && isWirelessCharge -> context.getString(R.string.charging_wireless)
        status == BatteryManager.BATTERY_STATUS_DISCHARGING -> context.getString(R.string.discharging)
        else -> context.getString(R.string.not_charging)
    }
}

fun getChargingRemainingTime(context: Context): String {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val timeMillis = batteryManager.computeChargeTimeRemaining()
        val timeMinutes = timeMillis / 1000 / 60
        val hours = timeMinutes / 60
        val minutes = timeMinutes % 60

        if (timeMinutes > 0) {
            // Format to "hours minutes" (e.g., "1 45")
            String.format("%d %s %d %s", hours, context.getString(R.string.unit_hours), minutes, context.getString(R.string.unit_minutes))
        } else {
            "---"
        }
    } else {
        "---"
    }
}

fun getBatteryHealth(context: Context): String {
    val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val batteryStatus = context.registerReceiver(null, intentFilter)

    val deviceHealth = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

    return when (deviceHealth) {
        BatteryManager.BATTERY_HEALTH_COLD -> context.getString(R.string.health_cold)
        BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.health_dead)
        BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.health_good)
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(R.string.health_overheat)
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(R.string.health_over_voltage)
        BatteryManager.BATTERY_HEALTH_UNKNOWN -> context.getString(R.string.health_unknown)
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> context.getString(R.string.health_failure)
        else -> context.getString(R.string.health_unknown) // Default case
    }
}

fun getBatteryTechnology(context: Context): String {
    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    return batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "---"
}




@Composable
fun BatteryView(windowSizeClass: WindowWidthSizeClass) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // State for battery information
    val batteryPercents = remember { mutableStateOf(getBatteryPercents(context)) }
    val batteryCapacityCurrent = remember { mutableStateOf(getBatteryCapacityCurrent(context)) }
    val batteryCapacityFactory = remember { mutableStateOf(getBatteryCapacityFactory(context)) }
    val batteryVoltage = remember { mutableStateOf(getBatteryVoltage(context)) }
    val batteryTemperature = remember { mutableStateOf(getBatteryTemperature(context)) }
    val chargingStatus = remember { mutableStateOf(getChargingStatus(context)) }
    val chargeTimeRemaining = remember { mutableStateOf(getChargingRemainingTime(context)) }
    val batteryHealth = remember { mutableStateOf(getBatteryHealth(context)) }
    val batteryTechnology = remember { mutableStateOf(getBatteryTechnology(context)) }

    // Refresh the state every second
    LaunchedEffect(Unit) {
        while (true) {
            // Update the states
            batteryPercents.value = getBatteryPercents(context)
            batteryCapacityCurrent.value = getBatteryCapacityCurrent(context)
            batteryCapacityFactory.value = getBatteryCapacityFactory(context)
            batteryVoltage.value = getBatteryVoltage(context)
            batteryTemperature.value = getBatteryTemperature(context)
            chargingStatus.value = getChargingStatus(context)
            chargeTimeRemaining.value = getChargingRemainingTime(context)
            batteryHealth.value = getBatteryHealth(context)
            batteryTechnology.value = getBatteryTechnology(context)

            // Delay for 1 second
            delay(2000L)
        }
    }

    // Define the list of card items once
    val cardItems = listOf(
        Pair(R.string.charge_level, batteryPercents.value),
        Pair(R.string.battery_charge_status, chargingStatus.value),
        Pair(R.string.battery_capacity_current, batteryCapacityCurrent.value),
        Pair(R.string.battery_capacity_factory, batteryCapacityFactory.value),
        Pair(R.string.battery_voltage, batteryVoltage.value),
        Pair(R.string.battery_temperature, batteryTemperature.value),
        Pair(R.string.charge_time_remaining, chargeTimeRemaining.value),
        Pair(R.string.battery_health, batteryHealth.value),
        Pair(R.string.battery_technology, batteryTechnology.value),
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
                title = { Text(stringResource(id = R.string.title_activity_battery_emoji) +" "+ stringResource(id = R.string.title_activity_battery)) },
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
fun BatteryViewPreview() {
    HardwareToolsTheme {
        BatteryView(WindowWidthSizeClass.Compact)
    }
}

