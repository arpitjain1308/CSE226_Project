package com.example.shrijiservicedesk.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

fun generateUpiQr(
    upiId: String,
    shopName: String,
    amount: String,
    repairId: Int
): Bitmap {
    val uri = "upi://pay?pa=$upiId" +
            "&pn=${java.net.URLEncoder.encode(shopName, "UTF-8")}" +
            "&am=$amount" +
            "&cu=INR" +
            "&tn=Repair%20$repairId"

    val matrix = QRCodeWriter().encode(
        uri,
        BarcodeFormat.QR_CODE,
        800,
        800,
        mapOf(EncodeHintType.MARGIN to 1)
    )

    val bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)

    for (x in 0 until 800) {
        for (y in 0 until 800) {
            bitmap.setPixel(
                x,
                y,
                if (matrix[x, y]) android.graphics.Color.BLACK
                else android.graphics.Color.WHITE
            )
        }
    }

    return bitmap
}

@Composable
fun UpiQrDialog(
    upiId: String,
    shopName: String,
    amount: String,
    repairId: Int,
    onDismiss: () -> Unit
) {
    val bitmap = remember(upiId, shopName, amount, repairId) {
        generateUpiQr(upiId, shopName, amount, repairId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Receive payment")
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    shopName,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "₹$amount",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(16.dp))

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "UPI payment QR code",
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                )

                Spacer(Modifier.height(12.dp))

                Text("Scan using any supported UPI app.")
                Text(
                    "Verify the actual credit in your bank account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}