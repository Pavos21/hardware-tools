@file:OptIn(ExperimentalMaterial3Api::class)

package cz.hardwaretools

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.window.layout.WindowMetricsCalculator
import cz.hardwaretools.ui.theme.HardwareToolsTheme
import kotlinx.coroutines.launch

class Fingerprint : FragmentActivity() {
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
                    FingerprintView(windowSizeClass)
                }
            }

        }
    }
}

@Composable
fun CustomSnackbar(snackbarHostState: SnackbarHostState) {
    val colors = MaterialTheme.colorScheme
    // Custom Snackbar Host with colors
    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = { data ->
            // Use the `snackbarData` to modify the appearance
            Snackbar(
                modifier = Modifier.padding(16.dp),
                contentColor = colors.onPrimary, // Text color
                containerColor = colors.surface, // Background color
                actionColor = colors.onPrimary, // Action button color
                snackbarData = data
            )
        }
    )
}


@Composable
fun FingerprintView(windowSizeClass: WindowWidthSizeClass) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val biometricManager = remember { BiometricManager.from(context) }

    val isBiometricAvailable = remember {
        biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
    }
    val isFingerprintAvailable = isBiometricAvailable == BiometricManager.BIOMETRIC_SUCCESS

    val snackbarHostState = remember { SnackbarHostState() }

    val executor = remember { ContextCompat.getMainExecutor(context) }
    val coroutineScope = rememberCoroutineScope() // Remember the coroutine scope

    val biometricPrompt = BiometricPrompt(
        context as FragmentActivity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                val text = context.getString(R.string.f_error)
                // Show snackbar on error, in a coroutine
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("$text $errString")
                }
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Show snackbar on success, in a coroutine
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.f_success))
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Show snackbar on failure, in a coroutine
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.f_fail))
                }
            }
        }
    )


    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setAllowedAuthenticators(BIOMETRIC_STRONG)
        .setTitle(context.getString(R.string.f_title))
        .setSubtitle(context.getString(R.string.f_subtitle))
        .setNegativeButtonText(context.getString(R.string.cancel))
        .build()

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
        snackbarHost = { CustomSnackbar(snackbarHostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.title_activity_fingerprint_emoji) +" "+ stringResource(id = R.string.title_activity_fingerprint)) },
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
            // Always-visible card at the top
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center // Center the content
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(maxWidth) // Fill based on provided max width
                    ) {
                        // This Card will always be visible
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
                                    text = stringResource(id = R.string.finger_availability), // You can adjust the text
                                    textAlign = TextAlign.Left,
                                    fontSize = fontSize,
                                )
                                Spacer(modifier = Modifier.height(8.dp)) // Space between text items
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = valueText),
                                    text = if (isFingerprintAvailable) stringResource(id = R.string.supported) else stringResource(id = R.string.not_supported),
                                    textAlign = TextAlign.Left,
                                    fontSize = fontSize,
                                    fontWeight = FontWeight.Bold, // Make the text bold
                                )
                            }
                        }
                    }
                }
            }

            // Now your fingerprint-related card (previous card)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center // Center the content
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(maxWidth) // Fill based on provided max width
                    ) {
                        // Only display the Card if the fingerprint sensor is available
                        if (isFingerprintAvailable) {
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
                                        text = stringResource(id = R.string.fingerprint_text),
                                        textAlign = TextAlign.Left,
                                        fontSize = fontSize,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp)) // Space between text items

                                    // Image Button replacing Text
                                    IconButton(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.CenterHorizontally), // Center the button horizontally
                                        onClick = { biometricPrompt.authenticate(promptInfo) } // Trigger authentication
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                id = R.drawable.fingerprint
                                            ), // Change icon based on flashlight state
                                            contentDescription = "Fingerprint icon",
                                            modifier = Modifier.size(24.dp) // Adjust size as needed
                                        )
                                    }
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
fun FingerprintViewPreview() {
    HardwareToolsTheme {
        FingerprintView(WindowWidthSizeClass.Compact)
    }
}