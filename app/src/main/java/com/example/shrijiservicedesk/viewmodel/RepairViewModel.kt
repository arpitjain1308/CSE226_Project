package com.example.shrijiservicedesk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shrijiservicedesk.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RepairViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RepairRepository(
        AppDatabase.getDatabase(application)
    )

    val customers = repository.customers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val repairs = repository.repairs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val payments = repository.payments.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val totalCollected = repository.totalCollected.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0
    )

    val pendingPayments = repository.pendingPayments.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0
    )

    val totalRepairs = repository.totalRepairs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun addRepair(
        name: String,
        phone: String,
        appliance: String,
        brand: String,
        model: String,
        problem: String,
        estimate: Double
    ) {
        viewModelScope.launch {
            runCatching {
                require(name.isNotBlank()) { "Customer name is required." }
                require(phone.isNotBlank()) { "Phone number is required." }
                require(appliance.isNotBlank()) { "Appliance is required." }
                require(problem.isNotBlank()) { "Problem description is required." }
                require(estimate >= 0) { "Estimate cannot be negative." }

                repository.addRepair(
                    name, phone, appliance, brand, model, problem, estimate
                )
            }.onSuccess {
                _message.value = "Repair registered successfully."
            }.onFailure {
                _message.value = it.message ?: "Could not register repair."
            }
        }
    }

    fun changeStatus(repairId: Int, status: String) {
        viewModelScope.launch {
            runCatching {
                repository.updateStatus(repairId, status)
            }.onFailure {
                _message.value = it.message ?: "Status update failed."
            }
        }
    }

    fun addPayment(
        repairId: Int,
        amount: String,
        method: String,
        reference: String
    ) {
        viewModelScope.launch {
            runCatching {
                val value = amount.toDoubleOrNull()
                    ?: throw IllegalArgumentException("Enter a valid amount.")

                repository.addPayment(repairId, value, method, reference)
            }.onSuccess {
                _message.value = "Payment saved as pending verification."
            }.onFailure {
                _message.value = it.message ?: "Payment could not be saved."
            }
        }
    }

    fun verifyPayment(payment: Payment) {
        viewModelScope.launch {
            runCatching {
                repository.verifyPayment(payment)
            }.onSuccess {
                _message.value = "Payment verified."
            }.onFailure {
                _message.value = "Payment verification failed."
            }
        }
    }

    fun updateFinalCost(repairId: Int, amount: String) {
        viewModelScope.launch {
            runCatching {
                val value = amount.toDoubleOrNull()
                    ?: throw IllegalArgumentException("Enter a valid final cost.")
                repository.updateFinalCost(repairId, value)
            }.onSuccess {
                _message.value = "Final cost updated."
            }.onFailure {
                _message.value = it.message ?: "Could not update final cost."
            }
        }
    }

    fun deleteRepair(repairId: Int) {
        viewModelScope.launch {
            runCatching {
                repository.deleteRepair(repairId)
            }.onSuccess {
                _message.value = "Repair deleted."
            }.onFailure {
                _message.value = "Could not delete repair."
            }
        }
    }

    fun observeRepair(id: Int) = repository.observeRepair(id)

    suspend fun getCustomer(id: Int) = repository.getCustomer(id)

    fun getRepairPayments(id: Int) = repository.getRepairPayments(id)

    fun getRepairHistory(id: Int) = repository.getRepairHistory(id)

    suspend fun getBalance(repair: Repair) = repository.getBalance(repair)
}