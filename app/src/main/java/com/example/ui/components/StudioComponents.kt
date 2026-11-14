package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.engine.CanvasRenderer
import com.example.model.BackgroundType
import com.example.model.CanvasDocument
import com.example.ui.theme.LocalIosifyColors
import kotlin.math.roundToInt

val StudioPaletteColors: List<Long> = listOf(
    0xFF1D1D1FL,
    0xFFFFFFFFL,
    0xFF007AFFL,
    0xFFAF52DEL,
    0xFF34C759L,
    0xFFFF9500L,
    0xFFFF3B30L,
    0xFF5856D6L,
    0xFF00C7BEL,
    0xFFFF2D55L,
    0xFF86868BL,
    0xFFF5F5F7L,
    0xFF1C1C1EL,
    0xFF000000L
)

@Composable
fun IosifySectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalIosifyColors.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(0.8.dp, ext.separator.copy(alpha = 0.55f), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun ColorSwatchRow(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Long> = StudioPaletteColors
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEachIndexed { idx, argb ->
            val isSelected = selectedColor == argb
            val swatchColor = Color(argb)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(swatchColor)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .testTag("color_swatch_$idx")
                    .clickable { onColorSelected(argb) }
            )
        }
    }
}

@Composable
fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun InteractiveStudioCanvas(
    document: CanvasDocument,
    renderer: CanvasRenderer,
    customEmojiPaths: Map<String, String>,
    selectedElementId: String?,
    zoomScale: Float,
    panOffset: Offset,
    onZoomPanChange: (Float, Offset) -> Unit,
    onSelectElement: (String?) -> Unit,
    onMoveElement: (String, Float, Float) -> Unit,
    onResizeElement: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true
) {
    val ext = LocalIosifyColors.current
    val aspect = document.canvasWidth.toFloat() / document.canvasHeight.coerceAtLeast(1).toFloat()

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        if (interactive) {
            val newScale = (zoomScale * zoomChange).coerceIn(0.6f, 3.5f)
            val newOffset = panOffset + panChange
            onZoomPanChange(newScale, newOffset)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(if (interactive) Modifier.transformable(transformState) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffset.x
                    translationY = panOffset.y
                }
                .fillMaxWidth(0.92f)
                .aspectRatio(aspect.coerceIn(0.45f, 1.9f))
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, ext.separator, RoundedCornerShape(18.dp))
        ) {
            // Transparency grid behind canvas if background is transparent
            if (document.background.type == BackgroundType.TRANSPARENT) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val tile = 20f
                    var y = 0f
                    var row = 0
                    while (y < size.height) {
                        var x = 0f
                        var col = 0
                        while (x < size.width) {
                            val isEven = (row + col) % 2 == 0
                            drawRect(
                                color = if (isEven) ext.canvasCheckeredLight else ext.canvasCheckeredDark,
                                topLeft = Offset(x, y),
                                size = Size(tile, tile)
                            )
                            x += tile
                            col++
                        }
                        y += tile
                        row++
                    }
                }
            }

            // Native Android CanvasRenderer for crisp Urdu/English text effects & custom emojis
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(document.elements, interactive) {
                        if (!interactive) return@pointerInput
                        detectTapGestures { tapOffset ->
                            val fx = tapOffset.x / size.width.coerceAtLeast(1)
                            val fy = tapOffset.y / size.height.coerceAtLeast(1)
                            val hit = document.elements
                                .sortedByDescending { it.zIndex }
                                .firstOrNull { el ->
                                    fx in el.xFraction..(el.xFraction + el.widthFraction) &&
                                        fy in el.yFraction..(el.yFraction + el.heightFraction)
                                }
                            onSelectElement(hit?.id)
                        }
                    }
            ) {
                drawIntoCanvas { canvas ->
                    renderer.renderDocument(
                        canvas = canvas.nativeCanvas,
                        document = document,
                        targetWidth = size.width,
                        targetHeight = size.height,
                        customEmojiPaths = customEmojiPaths
                    )
                }
            }

            // Selection box & drag/resize handles overlay
            if (interactive && selectedElementId != null) {
                val selected = document.elements.firstOrNull { it.id == selectedElementId }
                if (selected != null) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val canvasPxW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                        val canvasPxH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
                        val elX = (selected.xFraction * canvasPxW).roundToInt()
                        val elY = (selected.yFraction * canvasPxH).roundToInt()
                        val elW = with(density) { (selected.widthFraction * canvasPxW).toDp() }
                        val elH = with(density) { (selected.heightFraction * canvasPxH).toDp() }

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(elX, elY) }
                                .size(elW, elH)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .pointerInput(selected.id, canvasPxW, canvasPxH) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        onMoveElement(
                                            selected.id,
                                            dragAmount.x / canvasPxW,
                                            dragAmount.y / canvasPxH
                                        )
                                    }
                                }
                        ) {
                            // Bottom-right resize handle (48dp touch target)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(48.dp)
                                    .pointerInput(selected.id, canvasPxW, canvasPxH) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            onResizeElement(
                                                selected.id,
                                                dragAmount.x / canvasPxW,
                                                dragAmount.y / canvasPxH
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .border(2.dp, Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
