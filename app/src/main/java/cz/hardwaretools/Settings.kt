@file:OptIn(ExperimentalMaterial3Api::class)

package cz.hardwaretools

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.window.layout.WindowMetricsCalculator
import cz.hardwaretools.ui.theme.HardwareToolsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class Settings : ComponentActivity() {
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
                    SettingsView(windowSizeClass)
                }
            }
        }
    }
}

data class Item(
    val id: Int,
    val name: String,
    val icon: Int,
)

@Composable
fun SettingsView(windowSizeClass: WindowWidthSizeClass) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // Create the list of items
    val itemsStateFlow =
        MutableStateFlow(
            listOf(
                Item(0, context.getString(R.string.title_activity_battery), R.drawable.battery),
                Item(1, context.getString(R.string.title_activity_camera), R.drawable.camera),
                Item(2, context.getString(R.string.title_activity_cpu), R.drawable.cpu),
                Item(3, context.getString(R.string.title_activity_ram), R.drawable.ram),
                Item(4, context.getString(R.string.title_activity_wifi), R.drawable.wifi),
                Item(5, context.getString(R.string.title_activity_bluetooth), R.drawable.bluetooth),
                Item(6, context.getString(R.string.title_activity_flashlight), R.drawable.flashlight),
                Item(7, context.getString(R.string.title_activity_otg), R.drawable.otg),
                Item(8, context.getString(R.string.title_activity_storage), R.drawable.storage),
                Item(9, context.getString(R.string.title_activity_speaker), R.drawable.speaker),
                Item(10, context.getString(R.string.title_activity_nfc), R.drawable.nfc),
                Item(11, context.getString(R.string.title_activity_brightness), R.drawable.brightness),
                Item(12, context.getString(R.string.title_activity_location), R.drawable.location),
                Item(13, context.getString(R.string.title_activity_compass), R.drawable.compass),
                Item(14, context.getString(R.string.title_activity_display), R.drawable.display),
                Item(15, context.getString(R.string.title_activity_vibration), R.drawable.vibration),
                Item(16, context.getString(R.string.title_activity_infraport), R.drawable.infraport),
                Item(17, context.getString(R.string.title_activity_fingerprint), R.drawable.fingerprint),
                Item(18, context.getString(R.string.title_activity_light), R.drawable.light),
                Item(19, context.getString(R.string.title_activity_proximity), R.drawable.proximity),
            )
        )

    fun swapItems(from: Int, to: Int) {
        itemsStateFlow.update {
            val newList = it.toMutableList()
            val fromItem = it[from].copy()
            val toItem = it[to].copy()
            newList[from] = toItem
            newList[to] = fromItem

            println("it: $it, newList: $newList")

            newList
        }
    }

    // Initialize Shared Preferences
    val sharedPreferences = context.getSharedPreferences("card_order_prefs", MODE_PRIVATE)
    val editor = sharedPreferences.edit()

    // Load saved card order (default order is the current list if no order is saved)
    val defaultCardOrder = listOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19
    )

    // Function to save the current order of IDs to shared preferences
    fun saveCardOrder() {
        val currentOrder = itemsStateFlow.value.map { it.id }
        val orderString = currentOrder.joinToString(",")
        editor.putString("card_order_key", orderString)
        editor.apply()
        Toast.makeText(context, R.string.saved, Toast.LENGTH_SHORT).show()
    }

    // Function to load the saved order and update the itemsStateFlow
    fun loadSavedOrder() {
        val savedCardOrder = sharedPreferences.getString("card_order_key", null)?.split(",")?.map { it.toInt() } ?: defaultCardOrder
        val items = itemsStateFlow.value
        val sortedItems = items.sortedBy { savedCardOrder.indexOf(it.id) }
        itemsStateFlow.update { sortedItems }
    }

    LaunchedEffect(Unit) {
        loadSavedOrder()
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
        else -> 17.sp  // Default value
    }

    val maxWidth = when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> 1f
        WindowWidthSizeClass.Medium -> 0.85f
        WindowWidthSizeClass.Expanded -> 0.70f
        else -> 1f
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.title_activity_settings_emoji) +" "+ stringResource(id = R.string.title_activity_settings)) },
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
                .padding(it)
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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = cardPadding)
                                .padding(top = cardPadding),
                            colors = CardDefaults.cardColors(colors.surface),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(cardPadding)
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = context.getString(R.string.drag_drop_info),
                                    textAlign = TextAlign.Center,
                                    fontSize = fontSize,
                                )
                            }
                        }

                        // Drag-Drop
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = cardPadding)
                                .padding(top = cardPadding),
                            colors = CardDefaults.cardColors(colors.surface),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                val configuration = LocalConfiguration.current
                                val screenHeight = configuration.screenHeightDp.dp

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(screenHeight * 1/2)  // Set height to 3/4 of the screen height
                                        .padding(cardPadding) // Add some padding at the bottom for the Button
                                ) {
                                    DragDropColumn(
                                        items = itemsStateFlow.collectAsState().value,
                                        onSwap = ::swapItems
                                    ) { item ->
                                        Card(
                                            modifier = Modifier
                                                .clickable {
                                                    // Add your click handling here
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(colors.primary)
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically // Align the icon and text vertically
                                            ) {
                                                // Display the icon
                                                Icon(
                                                    painter = painterResource(id = item.icon),
                                                    contentDescription = null, // Optional: describe the icon for accessibility
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier
                                                        .size((fontSize.value*2).dp) // Adjust the size of the icon
                                                        .padding(end = 8.dp) // Add some space between the icon and text
                                                )

                                                // Display the text
                                                Text(
                                                    text = item.name,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = fontSize,
                                                )
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = { saveCardOrder() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(cardPadding)
                                ) {
                                    Text(text = context.getString(R.string.save_button))
                                }
                            }
                        }
                    }
                }
            }

            // Add some spacing at the end
            item {
                Spacer(modifier = Modifier.height(cardPadding))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T : Any> DragDropColumn(
    items: List<T>,
    onSwap: (Int, Int) -> Unit,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    var overscrollJob = remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val dragDropState = rememberDragDropState(listState) { fromIndex, toIndex ->
        onSwap(fromIndex, toIndex)
    }

    LazyColumn(
        modifier = Modifier
            .pointerInput(dragDropState) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, offset ->
                        change.consume()
                        dragDropState.onDrag(offset = offset)

                        if (overscrollJob.value?.isActive == true)
                            return@detectDragGesturesAfterLongPress

                        dragDropState
                            .checkForOverScroll()
                            .takeIf { it != 0f }
                            ?.let {
                                overscrollJob.value =
                                    scope.launch {
                                        dragDropState.state.animateScrollBy(
                                            it*1.3f, tween(easing = FastOutLinearInEasing)
                                        )
                                    }
                            }
                            ?: run { overscrollJob.value?.cancel() }
                    },
                    onDragStart = {
                            offset -> dragDropState.onDragStart(offset)
                    },
                    onDragEnd = {
                        dragDropState.onDragInterrupted()
                        overscrollJob.value?.cancel()
                    },
                    onDragCancel = {
                        dragDropState.onDragInterrupted()
                        overscrollJob.value?.cancel()
                    }
                )
            },
        state = listState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items = items) { index, item ->
            DraggableItem(
                dragDropState = dragDropState,
                index = index
            ) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp, label = "")
                Card(
                    elevation = CardDefaults.elevatedCardElevation(elevation)
                ) {
                    itemContent(item)
                }

            }
        }
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onSwap: (Int, Int) -> Unit
): DragDropState {
    val scope = rememberCoroutineScope()
    val state = remember(lazyListState) {
        DragDropState(
            state = lazyListState,
            onSwap = onSwap,
            scope = scope
        )
    }
    return state
}

fun LazyListState.getVisibleItemInfoFor(absoluteIndex: Int): LazyListItemInfo? {
    return this
        .layoutInfo
        .visibleItemsInfo
        .getOrNull(absoluteIndex - this.layoutInfo.visibleItemsInfo.first().index)
}

val LazyListItemInfo.offsetEnd: Int
    get() = this.offset + this.size

@ExperimentalFoundationApi
@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(isDragging: Boolean) -> Unit
) {
    val current: Float by animateFloatAsState(dragDropState.draggingItemOffset * 0.9f, label = "")
    val previous: Float by animateFloatAsState(dragDropState.previousItemOffset.value * 0.9f, label = "")
    val dragging = index == dragDropState.currentIndexOfDraggedItem.value

    val draggingModifier = if (dragging) {
        Modifier
            .zIndex(1f)
            .graphicsLayer {
                translationY = current
            }
    } else if (index == dragDropState.previousIndexOfDraggedItem.value) {
        Modifier
            .zIndex(1f)
            .graphicsLayer {
                translationY = previous
            }
    } else {
        Modifier.animateItem(
            tween(easing = FastOutLinearInEasing)
        )
    }
    Column(modifier = modifier.then(draggingModifier)) {
        content(dragging)
    }
}

class DragDropState internal constructor(
    val state: LazyListState,
    private val scope: CoroutineScope,
    private val onSwap: (Int, Int) -> Unit
) {
    private var draggedDistance = mutableFloatStateOf(0f)
    private var draggingItemInitialOffset = mutableIntStateOf(0)
    internal val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset.intValue + draggedDistance.floatValue - item.offset
        } ?: 0f
    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = state.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == currentIndexOfDraggedItem.value }

    internal var previousIndexOfDraggedItem = mutableStateOf<Int?>(null)
        private set
    internal var previousItemOffset = Animatable(0f)
        private set

    // used to obtain initial offsets on drag start
    private var initiallyDraggedElement = mutableStateOf<LazyListItemInfo?>(null)

    var currentIndexOfDraggedItem = mutableStateOf<Int?>(null)

    private val initialOffsets: Pair<Int, Int>?
        get() = initiallyDraggedElement.value?.let { Pair(it.offset, it.offsetEnd) }

    private val currentElement: LazyListItemInfo?
        get() = currentIndexOfDraggedItem.value?.let {
            state.getVisibleItemInfoFor(absoluteIndex = it)
        }


    fun onDragStart(offset: Offset) {
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                currentIndexOfDraggedItem.value = it.index
                initiallyDraggedElement.value = it
                draggingItemInitialOffset.intValue = it.offset
            }
    }

    fun onDragInterrupted() {
        if (currentIndexOfDraggedItem.value != null) {
            previousIndexOfDraggedItem.value = currentIndexOfDraggedItem.value
            // val startOffset = draggingItemOffset
            scope.launch {
                //previousItemOffset.snapTo(startOffset)
                previousItemOffset.animateTo(
                    0f,
                    tween(easing = FastOutLinearInEasing)
                )
                previousIndexOfDraggedItem.value = null
            }
        }
        draggingItemInitialOffset.intValue = 0
        draggedDistance.floatValue = 0f
        currentIndexOfDraggedItem.value = null
        initiallyDraggedElement.value = null
    }

    fun onDrag(offset: Offset) {
        draggedDistance.floatValue += offset.y

        initialOffsets?.let { (topOffset, bottomOffset) ->
            val startOffset = topOffset + draggedDistance.floatValue
            val endOffset = bottomOffset + draggedDistance.floatValue

            currentElement?.let { hovered ->
                state.layoutInfo.visibleItemsInfo
                    .filterNot { item -> item.offsetEnd < startOffset || item.offset > endOffset || hovered.index == item.index }
                    .firstOrNull { item ->
                        val delta = (startOffset - hovered.offset)
                        when {
                            delta > 0 -> (endOffset > item.offsetEnd)
                            else -> (startOffset < item.offset)
                        }
                    }
                    ?.also { item ->
                        currentIndexOfDraggedItem.value?.let { current ->
                            scope.launch {
                                onSwap.invoke(
                                    current,
                                    item.index
                                )
                            }
                        }
                        currentIndexOfDraggedItem.value = item.index
                    }
            }
        }
    }

    fun checkForOverScroll(): Float {
        return initiallyDraggedElement.value?.let {
            val startOffset = it.offset + draggedDistance.floatValue
            val endOffset = it.offsetEnd + draggedDistance.floatValue
            return@let when {
                draggedDistance.floatValue > 0 -> (endOffset - state.layoutInfo.viewportEndOffset+50f).takeIf { diff -> diff > 0 }
                draggedDistance.floatValue < 0 -> (startOffset - state.layoutInfo.viewportStartOffset-50f).takeIf { diff -> diff < 0 }
                else -> null
            }
        } ?: 0f
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsViewPreview() {
    HardwareToolsTheme {
        SettingsView(WindowWidthSizeClass.Compact)
    }
}