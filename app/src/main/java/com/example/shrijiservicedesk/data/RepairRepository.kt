package com.example.shrijiservicedesk.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class RepairRepository(private val db: AppDatabase) {

    private val dao = db.repairDao()

    val customers = dao.getAllCustomers()
    val repairs = dao.getAllRepairs()
    val payments = dao.getAllPayments()
    val totalCollected = dao.getTotalCollected()
    val pendingPayments = dao.getPendingPayments()
    val totalRepairs = dao.getRepairCount()

    suspend fun addRepair(
        name: String,
        phone: String,
        appliance: String,
        brand: String,
        model: String,
        problem: String,
        estimate: Double
    ) {
        db.withTransaction {
            val existing = dao.getCustomerByPhone(phone.trim())

            val customerId = existing?.customerId
                ?: dao.insertCustomer(
                    Customer(name = name.trim(), phone = phone.trim())
                ).toInt()

            val repairId = dao.insertRepair(
                Repair(
                    customerId = customerId,
                    appliance = appliance.trim(),
                    brand = brand.trim(),
                    model = model.trim(),
                    problem = problem.trim(),
                    estimatedCost = estimate
                )
            ).toInt()

            dao.insertRepairUpdate(
                RepairUpdate(
                    repairId = repairId,
                    previousStatus = "",
                    newStatus = "Received",
                    note = "Repair registered"
                )
            )
        }
    }

    suspend fun updateStatus(repairId: Int, newStatus: String) {
        db.withTransaction {
            val repair = dao.getRepairById(repairId)
                ?: throw IllegalArgumentException("Repair not found")

            if (repair.status == newStatus) return@withTransaction

            dao.updateRepair(
                repair.copy(
                    status = newStatus,
                    updatedAt = System.currentTimeMillis(),
                    completedAt = if (newStatus == "Completed") {
                        System.currentTimeMillis()
                    } else null
                )
            )

            dao.insertRepairUpdate(
                RepairUpdate(
                    repairId = repairId,
                    previousStatus = repair.status,
                    newStatus = newStatus
                )
            )
        }
    }

    suspend fun addPayment(
        repairId: Int,
        amount: Double,
        method: String,
        reference: String
    ) {
        require(amount > 0) { "Enter a valid payment amount." }

        dao.insertPayment(
            Payment(
                repairId = repairId,
                amount = amount,
                method = method,
                transactionReference = reference,
                verificationStatus = "PENDING"
            )
        )
    }

    suspend fun verifyPayment(payment: Payment) {
        dao.updatePayment(
            payment.copy(
                verificationStatus = "VERIFIED",
                verifiedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateFinalCost(repairId: Int, amount: Double) {
        require(amount >= 0) { "Final cost cannot be negative." }

        val repair = dao.getRepairById(repairId)
            ?: throw IllegalArgumentException("Repair not found")

        dao.updateRepair(
            repair.copy(
                finalCost = amount,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getCustomer(id: Int) = dao.getCustomerById(id)

    fun observeRepair(id: Int): Flow<Repair?> = dao.observeRepair(id)

    fun getRepairPayments(id: Int) = dao.getRepairPayments(id)

    fun getRepairHistory(id: Int) = dao.getRepairHistory(id)

    suspend fun getBalance(repair: Repair): Double {
        val paid = dao.getVerifiedAmount(repair.repairId)
        val total = if (repair.finalCost > 0) repair.finalCost
        else repair.estimatedCost
        return (total - paid).coerceAtLeast(0.0)
    }

    suspend fun deleteRepair(repairId: Int) {
        db.withTransaction {
            dao.deleteRepairHistory(repairId)
            dao.deleteRepairPayments(repairId)
            dao.deleteRepairById(repairId)
        }
    }
}