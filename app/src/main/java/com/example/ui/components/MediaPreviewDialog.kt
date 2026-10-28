package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.MediaType
import com.example.model.StatusItem
import com.example.ui.theme.WhatsAppGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewDialog(
    items: List<StatusItem>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onSave: (StatusItem) -> Unit,
    onShare: (StatusItem) -> Unit,
    onRepost: (StatusItem) -> Unit,
    onDelete: (StatusItem) -> Unit,
    onSetAlbum: (StatusItem, String) -> Unit,
    availableAlbums: List<String>
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showInfoSheet by remember { mutableStateOf(false) }
    var showAlbumSheet by remember { mutableStateOf(false) }
    var showQuickShareSheet by remember { mutableStateOf(false) }

    val safeInitialIndex = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(
        initialPage = safeInitialIndex,
        pageCount = { items.size }
    )

    val currentItem = items.getOrNull(pagerState.currentPage) ?: items[0]

    BackHandler {
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("media_preview_dialog")
        ) {
            // Horizontal Pager: Swiping left or right reveals the next/previous status
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("media_preview_pager")
            ) { page ->
                val pageItem = items[page]
                val isPageActive = page == pagerState.currentPage
                if (pageItem.mediaType == MediaType.IMAGE) {
                    ImagePreviewContent(item = pageItem)
                } else {
                    VideoPreviewContent(item = pageItem, isActive = isPageActive)
                }
            }

            // Left Navigation Button
            if (pagerState.currentPage > 0) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .size(46.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .testTag("preview_prev_status_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Status",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Right Navigation Button
            if (pagerState.currentPage < items.size - 1) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(46.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .testTag("preview_next_status_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Status",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Top Overlay Bar: Story Segment Progress Dashes + Header details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 4.dp)
                    .align(Alignment.TopStart)
            ) {
                // WhatsApp-style status segment dashes
                if (items.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in items.indices) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .background(
                                        color = if (i == pagerState.currentPage) WhatsAppGreen else Color.White.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("preview_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentItem.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (items.size > 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1}/${items.size}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${currentItem.appType.displayName} • ${currentItem.formattedDateTime} • ${currentItem.formattedSize}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = { showInfoSheet = true },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("preview_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Details",
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom Action Bar: operations on currentItem
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Save to Gallery button
                        Button(
                            onClick = { onSave(currentItem) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentItem.isSaved) Color(0xFF059669) else WhatsAppGreen
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("preview_save_button")
                        ) {
                            Icon(
                                imageVector = if (currentItem.isSaved) Icons.Default.Check else Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentItem.isSaved) "Saved" else "Save to Gallery",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Share button (triggers QuickShareBottomSheet)
                        FilledTonalButton(
                            onClick = { showQuickShareSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("preview_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 13.sp)
                        }

                        // Repost to WhatsApp
                        OutlinedButton(
                            onClick = { onRepost(currentItem) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("preview_repost_button")
                        ) {
                            Text("Repost", fontSize = 13.sp)
                        }
                    }

                    // Secondary row: Organize into Album & Delete (if saved)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAlbumSheet = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("preview_album_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Album: ${currentItem.albumTag}",
                                fontSize = 12.sp
                            )
                        }

                        if (currentItem.isSaved) {
                            IconButton(
                                onClick = { onDelete(currentItem) },
                                modifier = Modifier.testTag("preview_delete_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Details Modal Bottom Sheet
        if (showInfoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Status Information",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    InfoRow(label = "File Name", value = currentItem.title)
                    InfoRow(label = "Source App", value = currentItem.appType.displayName)
                    InfoRow(label = "Media Type", value = currentItem.mediaType.name)
                    InfoRow(label = "File Size", value = currentItem.formattedSize)
                    val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                        .format(Date(currentItem.dateModified))
                    InfoRow(label = "Date & Time", value = dateStr)
                    InfoRow(label = "Album / Tag", value = currentItem.albumTag)
                    InfoRow(label = "Saved to Device", value = if (currentItem.isSaved) "Yes (Gallery & App Vault)" else "Not yet saved")

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showInfoSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Album Tag Chooser Bottom Sheet
        if (showAlbumSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAlbumSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Assign to Album",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Organize this status in your built-in File Manager",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val nonAllAlbums = availableAlbums.filter { it != "All" }
                    nonAllAlbums.forEach { album ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentItem.albumTag == album) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            onClick = {
                                onSetAlbum(currentItem, album)
                                showAlbumSheet = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (currentItem.albumTag == album) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = album,
                                    fontWeight = if (currentItem.albumTag == album) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (currentItem.albumTag == album) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Quick Share Sheet (Direct social media platforms)
        if (showQuickShareSheet) {
            QuickShareBottomSheet(
                item = currentItem,
                onDismiss = { showQuickShareSheet = false }
            )
        }
    }
}

// Single-item overload for backwards compatibility
@Composable
fun MediaPreviewDialog(
    item: StatusItem,
    onDismiss: () -> Unit,
    onSave: (StatusItem) -> Unit,
    onShare: (StatusItem) -> Unit,
    onRepost: (StatusItem) -> Unit,
    onDelete: (StatusItem) -> Unit,
    onSetAlbum: (StatusItem, String) -> Unit,
    availableAlbums: List<String>
) {
    MediaPreviewDialog(
        items = listOf(item),
        initialIndex = 0,
        onDismiss = onDismiss,
        onSave = onSave,
        onShare = onShare,
        onRepost = onRepost,
        onDelete = onDelete,
        onSetAlbum = onSetAlbum,
        availableAlbums = availableAlbums
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ImagePreviewContent(item: StatusItem) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val model = if (item.filePath != null) {
        File(item.filePath)
    } else {
        item.uriString
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1.2f) 1f else 2.5f
                        offsetX = 0f
                        offsetY = 0f
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = item.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}

@Composable
private fun VideoPreviewContent(item: StatusItem, isActive: Boolean = true) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(isActive) }
    var durationMs by remember { mutableIntStateOf(0) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(true) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    // When the user swipes away to another page, pause immediately
    LaunchedEffect(isActive) {
        if (!isActive) {
            videoViewRef?.pause()
            isPlaying = false
        } else {
            videoViewRef?.start()
            isPlaying = true
        }
    }

    // Progress updater ticker
    LaunchedEffect(isPlaying, videoViewRef) {
        while (true) {
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    currentPosMs = vv.currentPosition
                    durationMs = vv.duration
                }
            }
            delay(250)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    val targetUri = if (item.filePath != null) {
                        Uri.fromFile(File(item.filePath))
                    } else {
                        Uri.parse(item.uriString)
                    }
                    setVideoURI(targetUri)

                    setOnPreparedListener { mp ->
                        mediaPlayerRef = mp
                        durationMs = mp.duration
                        mp.isLooping = isLooping
                        if (isMuted) {
                            mp.setVolume(0f, 0f)
                        } else {
                            mp.setVolume(1f, 1f)
                        }
                        if (isActive) {
                            start()
                            isPlaying = true
                        } else {
                            pause()
                            isPlaying = false
                        }
                    }

                    setOnCompletionListener {
                        if (!isLooping) {
                            isPlaying = false
                        }
                    }

                    videoViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay playback controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        videoViewRef?.let { vv ->
                            if (vv.isPlaying) {
                                vv.pause()
                                isPlaying = false
                            } else {
                                vv.start()
                                isPlaying = true
                            }
                        }
                    }
                }
        ) {
            // Center Play/Pause button indicator
            AnimatedVisibility(
                visible = !isPlaying,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            // Bottom Video Scrubbing & Audio Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp, start = 16.dp, end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatTime(currentPosMs),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Slider(
                        value = if (durationMs > 0) currentPosMs.toFloat() / durationMs else 0f,
                        onValueChange = { frac ->
                            val target = (frac * durationMs).toInt()
                            currentPosMs = target
                            videoViewRef?.seekTo(target)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = WhatsAppGreen,
                            activeTrackColor = WhatsAppGreen,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    Text(
                        text = formatTime(durationMs),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            videoViewRef?.let { vv ->
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                isLooping = !isLooping
                                mediaPlayerRef?.isLooping = isLooping
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Looping",
                                tint = if (isLooping) WhatsAppGreen else Color.White.copy(alpha = 0.5f)
                            )
                        }

                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                if (isMuted) {
                                    mediaPlayerRef?.setVolume(0f, 0f)
                                } else {
                                    mediaPlayerRef?.setVolume(1f, 1f)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Int): String {
    val sec = (millis / 1000) % 60
    val min = (millis / (1000 * 60)) % 60
    return String.format("%02d:%02d", min, sec)
}
