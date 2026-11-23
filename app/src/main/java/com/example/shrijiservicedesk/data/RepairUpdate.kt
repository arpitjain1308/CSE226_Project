package com.example.shrijiservicedesk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repair_updates")
data class RepairUpdate(
    @PrimaryKey(autoGenerate = true)
    val updateId: Int = 0,
    val repairId: Int,
    val previousStatus: String,
    val newStatus: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)