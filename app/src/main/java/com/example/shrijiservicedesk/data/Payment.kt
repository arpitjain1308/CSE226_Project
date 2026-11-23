package com.example.shrijiservicedesk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val paymentId: Int = 0,
    val repairId: Int,
    val amount: Double,
    val method: String = "UPI",
    val transactionReference: String = "",
    val verificationStatus: String = "PENDING",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long? = null
)