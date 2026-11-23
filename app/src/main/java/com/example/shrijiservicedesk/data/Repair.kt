package com.example.shrijiservicedesk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repairs")
data class Repair(
    @PrimaryKey(autoGenerate = true)
    val repairId: Int = 0,
    val customerId: Int,
    val appliance: String,
    val brand: String,
    val model: String = "",
    val problem: String,
    val diagnosis: String = "",
    val repairNotes: String = "",
    val estimatedCost: Double = 0.0,
    val finalCost: Double = 0.0,
    val expectedDate: Long? = null,
    val status: String = "Received",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)