@file:OptIn(ExperimentalMaterial3Api::class)

package cz.hardwaretools

//import androidx.compose.ui.tooling.preview.Preview
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Size
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.window.layout.WindowMetricsCalculator
import cz.hardwaretools.ui.theme.HardwareToolsTheme
import kotlinx.coroutines.delay
import java.util.Locale

class Camera : ComponentActivity() {
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
                    CameraView(windowSizeClass)
                }
            }
        }
    }
}

fun getMegapixels(context: Context, cameraId: String): String {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    val streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

    val sizes = streamConfigurationMap?.getOutputSizes(ImageFormat.JPEG) ?: arrayOf()
    return getCameraMP(sizes, context)
}

private fun getCameraMP(sizes: Array<Size>, context: Context): String {
    val first = sizes.firstOrNull() ?: return "Unknown MP"
    val second = sizes.lastOrNull() ?: return getMP(first, context)

    return if (first.width > second.width) {
        getMP(first, context)
    } else {
        getMP(second, context)
    }
}

private fun getMP(size: Size, context: Context): String {
    val mp = (size.width * size.height) / 1_000_000f
    return String.format(Locale.US, "%.1f %s", mp, context.getString(R.string.megapixels_unit))
}

fun getResolution(context: Context, cameraId: String): String {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val streamConfigurationMap = cameraManager.getCameraCharacteristics(cameraId)
        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

    // Ensure streamConfigurationMap is available
    if (streamConfigurationMap == null) {
        return "Unknown resolution"
    }

    // Get available output sizes for JPEG format
    val sizes: Array<Size> = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)

    // Determine maximum resolution
    val maxSize = sizes.maxByOrNull { it.width * it.height } ?: return "Unknown resolution"

    val unit = context.getString(R.string.resolution_unit)

    return "${maxSize.width} x ${maxSize.height} $unit"
}

fun getFocalLength(context: Context, cameraId: String): String {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)

    // Retrieve the focal length from CameraCharacteristics
    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

    // Ensure focal lengths are available
    if (focalLengths == null || focalLengths.isEmpty()) {
        return "Unknown focal length"
    }

    // Take the first focal length from the array (or choose the one you need)
    val focalLength = focalLengths[0]

    return String.format(Locale.US, "%.2f %s", focalLength, context.getString(R.string.focal_length_unit))
}


fun getCameraIds(context: Context): List<String> {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    return cameraManager.cameraIdList.toList()
}


@Composable
fun CameraView(windowSizeClass: WindowWidthSizeClass) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val lifecycleOwner = LocalLifecycleOwner.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val cameraIds = remember { mutableStateOf(getCameraIds(context)) }
    val selectedCamera = remember { mutableStateOf(cameraIds.value.firstOrNull() ?: "No Camera") }

    // State for camera details
    val megapixels = remember { mutableStateOf(getMegapixels(context, selectedCamera.value)) }
    val resolution = remember { mutableStateOf(getResolution(context, selectedCamera.value)) }
    val focalLength = remember { mutableStateOf(getFocalLength(context, selectedCamera.value)) }

    // Update camera details when selectedCamera changes
    // Define a state variable for the previous camera ID
    var previousCameraId = remember { mutableStateOf(selectedCamera.value) }

    // Define a state variable for the previewView
    var previewView = remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(Unit) {
        if (selectedCamera.value.isEmpty()) {
            selectedCamera.value = "0" // Set to back camera by default
        }
        // Start camera preview
        startCameraPreview(context, selectedCamera.value, lifecycleOwner, previewView.value)
    }

    LaunchedEffect(selectedCamera.value) {
        // Check if the selected camera ID has changed
        if (selectedCamera.value != previousCameraId.value) {
            // Update camera details based on the selected camera ID
            megapixels.value = getMegapixels(context, selectedCamera.value)
            resolution.value = getResolution(context, selectedCamera.value)
            focalLength.value = getFocalLength(context, selectedCamera.value)

            // Update the camera preview
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    // Connect the preview to the PreviewView
                    setSurfaceProvider(previewView.value?.surfaceProvider)
                }
                val cameraSelector = when (selectedCamera.value) {
                    "0" -> CameraSelector.DEFAULT_BACK_CAMERA
                    "1" -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> CameraSelector.DEFAULT_BACK_CAMERA
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (_: Exception) {
                    // Handle exceptions
                }
            }, ContextCompat.getMainExecutor(context))

            // Update the previous camera ID
            previousCameraId.value = selectedCamera.value
        }

        // Delay for a short period before next potential update
        delay(2000L)
    }

    val cameraPermissionState = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> cameraPermissionState.value = granted}
    )

    // Define the list of card items once
    val cardItems = listOf(
        Pair(R.string.megapixels, megapixels.value),
        Pair(R.string.resolution, resolution.value),
        Pair(R.string.focal_length, focalLength.value),
    )

    // Responsive card padding and font size based on window size class
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
                title = { Text(stringResource(id = R.string.title_activity_camera_emoji) +" "+ stringResource(id = R.string.title_activity_camera)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.secondary,
                    titleContentColor = colors.onSecondary
                ),
                navigationIcon = {
                    IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.back),
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
            // First Card: Camera Preview and Dropdown
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center // Center the content
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(maxWidth) // Fill based on provided max width
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(cardPadding),
                            colors = CardDefaults.cardColors(colors.surface),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                // Left half: Camera preview
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f) // Maintain aspect ratio for preview
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(maxWidth) // Fill based on provided max width
                                    ) {
                                        // Card for camera preview
                                        if (cameraPermissionState.value) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = cardPadding)
                                                    .padding(horizontal = cardPadding),
                                                colors = CardDefaults.cardColors(colors.surface),
                                                elevation = CardDefaults.cardElevation(4.dp)
                                            ) {
                                                AndroidView(
                                                    factory = { context ->
                                                        val previewViewLocal = PreviewView(context).apply {
                                                            layoutParams = ViewGroup.LayoutParams(
                                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                                            )
                                                        }

                                                        // Update the state variable with the previewView
                                                        previewView.value = previewViewLocal

                                                        previewViewLocal
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(300.dp) // Adjust height as needed
                                                )
                                            }
                                        } else {
                                            LaunchedEffect(Unit) {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                            // Show a fallback UI if permission isn't granted
                                            Text("Camera permission is required for live preview.")
                                        }
                                    }
                                }

                                // Right half: Switch for selecting cameras
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .padding(cardPadding), // Optional: padding for the Box
                                    contentAlignment = Alignment.Center // Center the content horizontally and vertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxHeight(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        // Text above the Switch
                                        Text(
                                            text = if (selectedCamera.value == "1") {
                                                stringResource(id = R.string.front_camera)
                                            } else {
                                                stringResource(id = R.string.back_camera)
                                            },
                                            fontSize = fontSize,
                                            modifier = Modifier.padding(bottom = 8.dp) // Optional padding between text and switch
                                        )

                                        // Switch for selecting cameras
                                        Switch(
                                            checked = selectedCamera.value == "1",
                                            onCheckedChange = {
                                                selectedCamera.value = if (it) "1" else "0"
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary, // Thumb color when checked
                                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface, // Thumb color when unchecked
                                                checkedTrackColor = MaterialTheme.colorScheme.secondary, // Track color when checked
                                                uncheckedTrackColor = MaterialTheme.colorScheme.surface // Track color when unchecked
                                            )
                                        )
                                    }
                                }

                            }
                        }
                    }
                }
            }

            // Second, Third, and Fourth Cards: Text and Values
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
                                    fontWeight = FontWeight.Bold // Make the text bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(cardPadding)) // Space after the last item
            }
        }

    }
}

// Function to start the camera preview
private fun startCameraPreview(context: Context, cameraId: String, lifecycleOwner: LifecycleOwner, previewView: PreviewView?) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView?.surfaceProvider)
        }
        val cameraSelector = if (cameraId == "0") CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        } catch (_: Exception) {
            // Handle exceptions
        }
    }, ContextCompat.getMainExecutor(context))
}


//@Preview(showBackground = true)
//@Composable
//fun CameraViewPreview() {
//    HardwareToolsTheme {
//        CameraView(WindowWidthSizeClass.Compact)
//    }
//}