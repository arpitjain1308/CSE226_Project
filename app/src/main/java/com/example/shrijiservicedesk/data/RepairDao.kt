package com.example.shrijiservicedesk.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairDao {

    @Insert
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("SELECT * FROM customers ORDER BY name")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE customerId = :id LIMIT 1")
    suspend fun getCustomerById(id: Int): Customer?

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun getCustomerByPhone(phone: String): Customer?

    @Insert
    suspend fun insertRepair(repair: Repair): Long

    @Update
    suspend fun updateRepair(repair: Repair)

    @Query("DELETE FROM repairs WHERE repairId = :id")
    suspend fun deleteRepairById(id: Int)

    @Query("SELECT * FROM repairs ORDER BY createdAt DESC")
    fun getAllRepairs(): Flow<List<Repair>>

    @Query("SELECT * FROM repairs WHERE repairId = :id LIMIT 1")
    suspend fun getRepairById(id: Int): Repair?

    @Query("SELECT * FROM repairs WHERE repairId = :id LIMIT 1")
    fun observeRepair(id: Int): Flow<Repair?>

    @Query("SELECT * FROM payments WHERE repairId = :id ORDER BY createdAt DESC")
    fun getRepairPayments(id: Int): Flow<List<Payment>>

    @Insert
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)

    @Query("SELECT * FROM payments ORDER BY createdAt DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM payments WHERE verificationStatus = 'VERIFIED'
    """)
    fun getTotalCollected(): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM payments WHERE verificationStatus = 'PENDING'
    """)
    fun getPendingPayments(): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM payments
        WHERE repairId = :repairId AND verificationStatus = 'VERIFIED'
    """)
    suspend fun getVerifiedAmount(repairId: Int): Double

    @Insert
    suspend fun insertRepairUpdate(update: RepairUpdate)

    @Query("""
        SELECT * FROM repair_updates
        WHERE repairId = :id ORDER BY createdAt DESC
    """)
    fun getRepairHistory(id: Int): Flow<List<RepairUpdate>>

    @Query("SELECT COUNT(*) FROM repairs")
    fun getRepairCount(): Flow<Int>

    @Query("DELETE FROM repair_updates WHERE repairId = :id")
    suspend fun deleteRepairHistory(id: Int)

    @Query("DELETE FROM payments WHERE repairId = :id")
    suspend fun deleteRepairPayments(id: Int)
}