package org.ericsk.android.watchQR

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.zxing.BarcodeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    private val tag = "WearMainActivity"
    private val qrCodePath = "/qrcode"
    private val qrCodeKey = "text"

    private var qrCodeText by mutableStateOf("")
    private var codeType by mutableStateOf("QR_CODE") // "QR_CODE" or "BARCODE"
    private var qrCodeBitmap by mutableStateOf<Bitmap?>(null)
    private var generateJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp(qrCodeText = qrCodeText, qrCodeBitmap = qrCodeBitmap, codeType = codeType)
        }
        // Initialize with a default placeholder QR code on startup so the screen is never blank
        updateCode("watchQR", "QR_CODE")
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)

        // Immediately boost brightness if a code is already cached/present (minimizes latency on wake)
        if (qrCodeBitmap != null) {
            setScreenBrightness(true)
        }

        // Query last synced QR code from local data items on startup
        val uri = Uri.parse("wear://*/qrcode")

        Wearable.getDataClient(this).getDataItems(uri)
            .addOnSuccessListener { dataItems ->
                for (item in dataItems) {
                    if (item.uri.path == qrCodePath) {
                        val dataMap = DataMapItem.fromDataItem(item).dataMap
                        val text = dataMap.getString(qrCodeKey) ?: ""
                        val type = dataMap.getString("type") ?: "QR_CODE"
                        if (text.isNotEmpty()) {
                            updateCode(text, type)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to fetch QR code on startup", e)
            }
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
        setScreenBrightness(false) // Restore screen brightness to default on pause
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val item = event.dataItem
                if (item.uri.path == qrCodePath) {
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    val text = dataMap.getString(qrCodeKey) ?: ""
                    val type = dataMap.getString("type") ?: "QR_CODE"
                    updateCode(text, type)
                }
            }
        }
    }

    private fun updateCode(text: String, type: String) {
        if (text.isEmpty()) {
            generateJob?.cancel()
            qrCodeText = ""
            codeType = "QR_CODE"
            qrCodeBitmap = null
            setScreenBrightness(false)
            return
        }
        if (text == qrCodeText && type == codeType && qrCodeBitmap != null) return
        qrCodeText = text
        codeType = type

        generateJob?.cancel()
        generateJob = lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                if (type == "QR_CODE") {
                    QrCodeGenerator.generateCode(text, BarcodeFormat.QR_CODE, 300, 300)
                } else {
                    QrCodeGenerator.generateCode(text, BarcodeFormat.CODE_128, 400, 120)
                }
            }
            qrCodeBitmap = bitmap
            setScreenBrightness(bitmap != null)
        }
    }

    private fun setScreenBrightness(max: Boolean) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = if (max) {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        } else {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        window.attributes = layoutParams
    }
}

@Composable
fun WearApp(qrCodeText: String, qrCodeBitmap: Bitmap?, codeType: String) {
    MaterialTheme {
        Scaffold(
            timeText = { if (qrCodeBitmap == null) TimeText() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (qrCodeBitmap != null) {
                    if (codeType == "QR_CODE") {
                        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp
                        val isRound = configuration.isScreenRound
                        val qrSize = if (isRound) {
                            (screenWidth * 0.75f).dp // Diagonal fits completely within the circular screen
                        } else {
                            165.dp
                        }
                        Box(
                            modifier = Modifier
                                .size(qrSize)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrCodeBitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        // Barcode layout - placed exactly in the center to maximize horizontal screen width
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.95f) // Stretch to 95% of watch screen width!
                                .height(80.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrCodeBitmap.asImageBitmap(),
                                contentDescription = "Barcode",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        }

                        // Small text caption placed at the bottom, separate from the barcode to avoid pushing it up
                        Text(
                            text = qrCodeText,
                            style = MaterialTheme.typography.caption2,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "No Code synced",
                            style = MaterialTheme.typography.body2,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Send a QR/Barcode from your phone application.",
                            style = MaterialTheme.typography.caption2,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Preview(device = "id:wearos_large_round", showSystemUi = true)
@Composable
fun WearAppPreview() {
    // Preview with a dummy QR code text or layout
    WearApp(qrCodeText = "https://example.com", qrCodeBitmap = null, codeType = "QR_CODE")
}
