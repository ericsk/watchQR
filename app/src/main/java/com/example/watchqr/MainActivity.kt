package com.example.watchqr

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watchqr.theme.MyApplicationTheme
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.zxing.BarcodeFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF09070F)
                ) {
                    CompanionScreen()
                }
            }
        }
    }
}

sealed interface SyncStatus {
    object Idle : SyncStatus
    object Syncing : SyncStatus
    data class Success(val message: String) : SyncStatus
    data class Error(val error: String) : SyncStatus
}

@Composable
fun CompanionScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var inputText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("QR_CODE") } // "QR_CODE" or "BARCODE"
    var syncStatus by remember { mutableStateOf<SyncStatus>(SyncStatus.Idle) }

    val rotation by animateFloatAsState(
        targetValue = if (syncStatus is SyncStatus.Syncing) 360f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(syncStatus) {
        if (syncStatus is SyncStatus.Success || syncStatus is SyncStatus.Error) {
            delay(3000)
            syncStatus = SyncStatus.Idle
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F0C20),
                        Color(0xFF1A0F30),
                        Color(0xFF0A0F1E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(350.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x1F8E2DE2),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.Center)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.05f)
            ),
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "watchQR",
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFD0BCFF),
                                Color(0xFF80DEEA)
                            )
                        ),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.sp
                    )
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Quick Sync Companion",
                    color = Color(0xFFB0AEC4),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val qrBitmap = remember(inputText, selectedType) {
                        if (inputText.isNotEmpty()) {
                            if (selectedType == "QR_CODE") {
                                QrCodeGenerator.generateCode(inputText, BarcodeFormat.QR_CODE, 400, 400)
                            } else {
                                QrCodeGenerator.generateCode(inputText, BarcodeFormat.CODE_128, 500, 150)
                            }
                        } else {
                            null
                        }
                    }

                    if (qrBitmap != null) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Code Preview",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Canvas(modifier = Modifier.size(48.dp)) {
                                val strokeWidth = 3.dp.toPx()
                                val sizeVal = this.size.width
                                val boxSize = sizeVal * 0.35f
                                val tintColor = Color(0xFF80DEEA).copy(alpha = 0.6f)

                                // Top-Left Anchor
                                drawRect(
                                    color = tintColor,
                                    topLeft = Offset(0f, 0f),
                                    size = Size(boxSize, boxSize),
                                    style = Stroke(width = strokeWidth)
                                )
                                drawRect(
                                    color = tintColor,
                                    topLeft = Offset(strokeWidth * 1.5f, strokeWidth * 1.5f),
                                    size = Size(boxSize - strokeWidth * 3f, boxSize - strokeWidth * 3f)
                                )

                                // Top-Right Anchor
                                drawRect(
                                    color = tintColor,
                                    topLeft = Offset(sizeVal - boxSize, 0f),
                                    size = Size(boxSize, boxSize),
                                    style = Stroke(width = strokeWidth)
                                )
                                drawRect(
                                    color = tintColor,
                                    topLeft = Offset(sizeVal - boxSize + strokeWidth * 1.5f, strokeWidth * 1.5f),
                                    size = Size(boxSize - strokeWidth * 3f, boxSize - strokeWidth * 3f)
                                )

                                // Bottom-Left Anchor
                                drawRect(
                                    color = tintColor,
                                    topLeft = Offset(0f, sizeVal - boxSize),
                                    size = Size(boxSize, boxSize),
                                    style = Stroke(width = strokeWidth)
                                )
                                drawRect(
                                    color = tintColor,
                                    topLeft = Offset(strokeWidth * 1.5f, sizeVal - boxSize + strokeWidth * 1.5f),
                                    size = Size(boxSize - strokeWidth * 3f, boxSize - strokeWidth * 3f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Awaiting Input...",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Format Selector (QR Code vs Barcode)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val types = listOf("QR_CODE" to "QR Code", "BARCODE" to "Barcode")
                    types.forEach { (typeKey, typeLabel) ->
                        val isSelected = selectedType == typeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFD0BCFF).copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { selectedType = typeKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = typeLabel,
                                color = if (isSelected) Color(0xFFD0BCFF) else Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Text or Link to Sync") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                        focusedLabelColor = Color(0xFFD0BCFF),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color(0xFFD0BCFF)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                        }
                    ),
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = { inputText = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear text",
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                val isInputEmpty = inputText.trim().isEmpty()
                val buttonGradient = if (isInputEmpty) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF8E2DE2),
                            Color(0xFF4A00E0)
                        )
                    )
                }

                Button(
                    onClick = {
                        if (!isInputEmpty) {
                            keyboardController?.hide()
                            syncStatus = SyncStatus.Syncing
                            coroutineScope.launch {
                                try {
                                    val putDataMapReq = PutDataMapRequest.create("/qrcode")
                                    putDataMapReq.dataMap.putString("text", inputText.trim())
                                    putDataMapReq.dataMap.putString("type", selectedType)
                                    val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                                    
                                    Wearable.getDataClient(context).putDataItem(putDataReq)
                                        .addOnSuccessListener {
                                            syncStatus = SyncStatus.Success("Synced to Watch!")
                                        }
                                        .addOnFailureListener { e ->
                                            syncStatus = SyncStatus.Error("Failed: ${e.localizedMessage ?: "Unknown error"}")
                                        }
                                } catch (e: Exception) {
                                    syncStatus = SyncStatus.Error("Error: ${e.localizedMessage ?: "Unknown error"}")
                                }
                            }
                        }
                    },
                    enabled = !isInputEmpty && syncStatus !is SyncStatus.Syncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(buttonGradient, shape = RoundedCornerShape(16.dp)),
                    border = if (isInputEmpty) BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) else null
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (syncStatus is SyncStatus.Syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(rotation),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = if (isInputEmpty) Color.White.copy(alpha = 0.3f) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sync to Watch",
                            color = if (isInputEmpty) Color.White.copy(alpha = 0.3f) else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                AnimatedVisibility(
                    visible = syncStatus !is SyncStatus.Idle,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { 20 })
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    when (val status = syncStatus) {
                        is SyncStatus.Success -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E3A24))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = status.message,
                                    color = Color(0xFFE8F5E9),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        is SyncStatus.Error -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF3E1F1F))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = Color(0xFFE57373),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = status.error,
                                    color = Color(0xFFFFEBEE),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
