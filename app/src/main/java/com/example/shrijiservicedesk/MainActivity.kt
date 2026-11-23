package com.example.shrijiservicedesk

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shrijiservicedesk.data.*
import com.example.shrijiservicedesk.ui.theme.*
import com.example.shrijiservicedesk.viewmodel.RepairViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SHOP_NAME = "Shri Ji Electronics"
private const val SHOP_UPI_ID = "jainshahab@ptaxis"

private val repairStatuses = listOf(
    "Received",
    "Under Inspection",
    "Problem Identified",
    "Repair in Progress",
    "Ready for Pickup",
    "Completed"
)

private val tabNames = listOf("Home", "Repairs", "Customers", "Payments")

private val tabIcons = listOf(
    Icons.Filled.Home,
    Icons.Filled.Build,
    Icons.Filled.People,
    Icons.Filled.Payments
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ShriJiServiceDeskTheme {
                ServiceDeskApp()
            }
        }
    }
}

private enum class SheetType {
    NONE, NEW_REPAIR, REPAIR_DETAILS, ADD_PAYMENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDeskApp(vm: RepairViewModel = viewModel()) {
    val repairs by vm.repairs.collectAsState()
    val customers by vm.customers.collectAsState()
    val payments by vm.payments.collectAsState()
    val collected by vm.totalCollected.collectAsState()
    val pending by vm.pendingPayments.collectAsState()
    val totalRepairs by vm.totalRepairs.collectAsState()
    val message by vm.message.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    var sheet by remember { mutableStateOf(SheetType.NONE) }
    var selectedRepairId by remember { mutableIntStateOf(0) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    fun openRepair(repair: Repair) {
        selectedRepairId = repair.repairId
        sheet = SheetType.REPAIR_DETAILS
    }

    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            PremiumBottomBar(
                selected = pagerState.currentPage,
                onSelect = { page ->
                    scope.launch { pagerState.animateScrollToPage(page) }
                }
            )
        },
        floatingActionButton = {
            if (pagerState.currentPage == 0 || pagerState.currentPage == 1) {
                FloatingActionButton(
                    onClick = { sheet = SheetType.NEW_REPAIR },
                    containerColor = Violet,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New repair")
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        repairs = repairs,
                        totalRepairs = totalRepairs,
                        collected = collected,
                        pending = pending,
                        onNewRepair = { sheet = SheetType.NEW_REPAIR },
                        onRepairClick = ::openRepair,
                        onNavigate = { target ->
                            scope.launch { pagerState.animateScrollToPage(target) }
                        }
                    )

                    1 -> RepairsScreen(
                        repairs = repairs,
                        onRepairClick = ::openRepair
                    )

                    2 -> CustomersScreen(
                        customers = customers,
                        repairs = repairs
                    )

                    3 -> PaymentsScreen(
                        payments = payments,
                        onVerify = vm::verifyPayment
                    )
                }
            }
        }
    }

    if (sheet != SheetType.NONE) {
        ModalBottomSheet(
            onDismissRequest = { sheet = SheetType.NONE },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            dragHandle = {
                Box(
                    Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 42.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDAD8E2))
                )
            }
        ) {
            when (sheet) {
                SheetType.NEW_REPAIR -> NewRepairSheet(
                    onClose = { sheet = SheetType.NONE },
                    onSave = { name, phone, appliance, brand, model, problem, estimate ->
                        vm.addRepair(
                            name, phone, appliance, brand,
                            model, problem, estimate
                        )
                        sheet = SheetType.NONE
                    }
                )

                SheetType.REPAIR_DETAILS -> RepairDetailsSheet(
                    repairId = selectedRepairId,
                    vm = vm,
                    customers = customers,
                    onClose = { sheet = SheetType.NONE },
                    onAddPayment = {
                        sheet = SheetType.ADD_PAYMENT
                    }
                )

                SheetType.ADD_PAYMENT -> AddPaymentSheet(
                    repair = repairs.find { it.repairId == selectedRepairId },
                    onClose = { sheet = SheetType.NONE },
                    onSave = { amount, method, reference ->
                        vm.addPayment(
                            selectedRepairId, amount, method, reference
                        )
                        sheet = SheetType.REPAIR_DETAILS
                    }
                )

                SheetType.NONE -> Unit
            }
        }
    }

    if (message != null) {
        AlertDialog(
            onDismissRequest = { vm.clearMessage() },
            title = { Text("Shri Ji Service Desk") },
            text = { Text(message.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { vm.clearMessage() }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
private fun PremiumBottomBar(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabNames.forEachIndexed { index, label ->
                val active = selected == index
                val tint by animateColorAsState(
                    if (active) Violet else Muted,
                    label = "navigationTint"
                )

                val pillWidth by animateDpAsState(
                    if (active) 54.dp else 42.dp,
                    label = "navigationWidth"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .width(pillWidth)
                            .height(34.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) Lavender else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            tabIcons[index],
                            contentDescription = label,
                            tint = tint
                        )
                    }

                    Spacer(Modifier.height(3.dp))

                    Text(
                        label,
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    repairs: List<Repair>,
    totalRepairs: Int,
    collected: Double,
    pending: Double,
    onNewRepair: () -> Unit,
    onRepairClick: (Repair) -> Unit,
    onNavigate: (Int) -> Unit
) {
    val today = SimpleDateFormat(
        "EEEE, dd MMMM",
        Locale.getDefault()
    ).format(Date())

    val active = repairs.count { it.status != "Completed" }
    val ready = repairs.count { it.status == "Ready for Pickup" }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(22.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "SHRI JI ELECTRONICS",
                    color = Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "Your Service Hub.",
                    color = Ink,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(today, color = Muted, fontSize = 12.sp)
            }

            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Lavender),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Bolt,
                    contentDescription = "Shop",
                    tint = Violet,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        // Animated business summary
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(500)) + slideInVertically(
                initialOffsetY = { it / 5 },
                animationSpec = tween(500)
            )
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Violet)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "TOTAL COLLECTED",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFFBFF3D7)
                        )
                    }

                    AnimatedMoney(
                        value = collected,
                        color = Color.White,
                        size = 31
                    )

                    Text(
                        "Verified payments only",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryMetric(
                            "Active repairs",
                            active.toString(),
                            Modifier.weight(1f)
                        )
                        SummaryMetric(
                            "Ready for pickup",
                            ready.toString(),
                            Modifier.weight(1f)
                        )
                        SummaryMetric(
                            "Total jobs",
                            totalRepairs.toString(),
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Quick actions", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("SHORTCUTS", color = Muted, fontSize = 10.sp)
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            QuickAction(
                "New repair", Icons.Filled.Add, PaleMint, Mint,
                Modifier.weight(1f), onNewRepair
            )
            QuickAction(
                "Customers", Icons.Filled.People, PaleBlue, Sky,
                Modifier.weight(1f), { onNavigate(2) }
            )
        }

        Spacer(Modifier.height(11.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            QuickAction(
                "Payments", Icons.Filled.Payments, PaleCoral, Coral,
                Modifier.weight(1f), { onNavigate(3) }
            )
            QuickAction(
                "All repairs", Icons.Filled.Build, Lavender, Violet,
                Modifier.weight(1f), { onNavigate(1) }
            )
        }

        Spacer(Modifier.height(22.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent activity", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { onNavigate(1) }) {
                Text("See all", color = Violet)
            }
        }

        if (repairs.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Build,
                title = "Your service desk is ready",
                subtitle = "Register your first repair to start tracking jobs.",
                action = "Add first repair",
                onAction = onNewRepair
            )
        } else {
            repairs.take(5).forEachIndexed { index, repair ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(350, delayMillis = index * 70)) +
                            slideInVertically(
                                initialOffsetY = { it / 6 },
                                animationSpec = tween(350, delayMillis = index * 70)
                            )
                ) {
                    RepairCard(repair, onRepairClick)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SoftInfoCard(
            title = "Awaiting payment verification",
            amount = money(pending),
            subtitle = "Check your bank or UPI account before verifying.",
            icon = Icons.Filled.VerifiedUser,
            color = PaleCoral,
            iconColor = Coral,
            onClick = { onNavigate(3) }
        )

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun AnimatedMoney(
    value: Double,
    color: Color,
    size: Int
) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "moneyCounter"
    )

    Text(
        money(animated.toDouble()),
        color = color,
        fontSize = size.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .padding(11.dp)
    ) {
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp)
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    background: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(105.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(25.dp))
            Text(label, color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SoftInfoCard(
    title: String,
    amount: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp)
                Text(subtitle, color = Muted, fontSize = 10.sp)
            }
            Text(amount, fontWeight = FontWeight.Bold, color = Ink)
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: String,
    onAction: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Lavender),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, color = Ink, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = Violet)) {
            Text(action)
        }
    }
}

@Composable
private fun RepairsScreen(
    repairs: List<Repair>,
    onRepairClick: (Repair) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filters = listOf("All", "Active", "Ready", "Completed")

    val filtered = repairs.filter { repair ->
        val matchesSearch =
            repair.appliance.contains(search, true) ||
                    repair.brand.contains(search, true) ||
                    repair.problem.contains(search, true) ||
                    repair.status.contains(search, true) ||
                    repair.repairId.toString().contains(search)

        val matchesFilter = when (selectedFilter) {
            "Active" -> repair.status != "Completed" && repair.status != "Ready for Pickup"
            "Ready" -> repair.status == "Ready for Pickup"
            "Completed" -> repair.status == "Completed"
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(22.dp))
        Text("Your repairs", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("${repairs.size} service records", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search repairs, brands, problems...") },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Muted) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            )
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Lavender,
                        selectedLabelColor = Violet
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            EmptyState(
                Icons.Filled.Search,
                "No repairs found",
                "Try another search or filter.",
                "Clear search"
            ) {
                search = ""
                selectedFilter = "All"
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.repairId }) { repair ->
                    RepairCard(repair, onRepairClick)
                }
            }
        }
    }
}

@Composable
private fun RepairCard(
    repair: Repair,
    onClick: (Repair) -> Unit
) {
    val animatedElevation by animateDpAsState(
        targetValue = 1.dp,
        label = "repairCardElevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onClick(repair) },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation)
    ) {
        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(repairStatusBackground(repair.status)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    applianceIcon(repair.appliance),
                    contentDescription = null,
                    tint = repairStatusColor(repair.status),
                    modifier = Modifier.size(25.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "${repair.brand} ${repair.appliance}".trim(),
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    "Repair #${repair.repairId} • ${repair.model.ifBlank { "Model not added" }}",
                    color = Muted,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    repair.problem,
                    color = Muted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
                StatusPill(repair.status)
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Open repair",
                tint = Muted
            )
        }
    }
}

@Composable
private fun CustomersScreen(
    customers: List<Customer>,
    repairs: List<Repair>
) {
    var search by remember { mutableStateOf("") }

    val filtered = customers.filter {
        it.name.contains(search, true) || it.phone.contains(search, true)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(22.dp))
        Text("Customers", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("${customers.size} customer profiles", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name or phone") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )

        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Text("No customers found.", color = Muted)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.customerId }) { customer ->
                    val customerRepairs = repairs.filter {
                        it.customerId == customer.customerId
                    }

                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            Modifier.padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(45.dp)
                                    .clip(CircleShape)
                                    .background(PaleBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    customer.name.take(1).uppercase(),
                                    color = Sky,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(customer.name, fontWeight = FontWeight.Bold, color = Ink)
                                Text(customer.phone, color = Muted, fontSize = 12.sp)
                                Text(
                                    "${customerRepairs.size} repair(s)",
                                    color = Violet,
                                    fontSize = 11.sp
                                )
                            }

                            CustomerContactActions(customer.phone)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerContactActions(phone: String) {
    val context = LocalContext.current
    val number = normalizePhone(phone)

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IconButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                )
            }
        ) {
            Icon(
                Icons.Filled.Phone,
                contentDescription = "Call customer",
                tint = Sky
            )
        }

        IconButton(
            onClick = {
                val url = "https://wa.me/$number"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        ) {
            Icon(
                Icons.Filled.Chat,
                contentDescription = "WhatsApp customer",
                tint = Mint
            )
        }
    }
}

@Composable
private fun PaymentsScreen(
    payments: List<Payment>,
    onVerify: (Payment) -> Unit
) {
    val verified = payments.filter { it.verificationStatus == "VERIFIED" }
    val pending = payments.filter { it.verificationStatus == "PENDING" }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(22.dp))
        Text("Payments", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("Track and verify collections", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PaymentSummary(
                "Verified",
                money(verified.sumOf { it.amount }),
                PaleMint,
                Mint,
                Modifier.weight(1f)
            )
            PaymentSummary(
                "Awaiting verification",
                money(pending.sumOf { it.amount }),
                PaleCoral,
                Coral,
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("Payment records", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(10.dp))

        if (payments.isEmpty()) {
            Text("No payments recorded yet.", color = Muted)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(payments, key = { it.paymentId }) { payment ->
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    money(payment.amount),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Ink
                                )
                                StatusPill(
                                    if (payment.verificationStatus == "VERIFIED") {
                                        "Completed"
                                    } else {
                                        "Pending"
                                    }
                                )
                            }

                            Spacer(Modifier.height(5.dp))
                            Text("Repair #${payment.repairId}", color = Muted, fontSize = 12.sp)
                            Text(
                                "${payment.method} • ${payment.transactionReference.ifBlank { "No reference" }}",
                                color = Muted,
                                fontSize = 11.sp
                            )

                            if (payment.verificationStatus == "PENDING") {
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { onVerify(payment) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Mint)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Verify received payment")
                                }
                                Text(
                                    "Verify only after checking your bank or UPI account.",
                                    color = Muted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentSummary(
    label: String,
    amount: String,
    background: Color,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .padding(15.dp)
    ) {
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(amount, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun NewRepairSheet(
    onClose: () -> Unit,
    onSave: (
        String, String, String, String, String, String, Double
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var appliance by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf("") }
    var estimate by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 30.dp)
    ) {
        SheetHeader(
            title = "New repair",
            subtitle = "Create a service record",
            icon = Icons.Filled.Add,
            onClose = onClose
        )

        Spacer(Modifier.height(18.dp))

        Text("CUSTOMER DETAILS", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        PremiumField(name, { name = it }, "Customer name", Icons.Filled.Person)
        PremiumField(phone, { phone = it }, "Phone number", Icons.Filled.Phone)

        Spacer(Modifier.height(12.dp))
        Text("APPLIANCE DETAILS", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        PremiumField(appliance, { appliance = it }, "Appliance", Icons.Filled.Devices)
        PremiumField(brand, { brand = it }, "Brand")
        PremiumField(model, { model = it }, "Model")
        PremiumField(problem, { problem = it }, "Describe the problem", Icons.Filled.Description)
        PremiumField(estimate, { estimate = it }, "Estimated cost (₹)", Icons.Filled.CurrencyRupee)

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {
                onSave(
                    name, phone, appliance, brand,
                    model, problem, estimate.toDoubleOrNull() ?: 0.0
                )
            },
            enabled = name.isNotBlank() &&
                    phone.isNotBlank() &&
                    appliance.isNotBlank() &&
                    problem.isNotBlank() &&
                    (estimate.isBlank() || (estimate.toDoubleOrNull() ?: -1.0) >= 0),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violet)
        ) {
            Icon(Icons.Filled.Check, null)
            Spacer(Modifier.width(8.dp))
            Text("Create repair", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RepairDetailsSheet(
    repairId: Int,
    vm: RepairViewModel,
    customers: List<Customer>,
    onClose: () -> Unit,
    onAddPayment: () -> Unit
) {
    val repair by vm.observeRepair(repairId).collectAsState(initial = null)
    val payments by vm.getRepairPayments(repairId).collectAsState(initial = emptyList())
    val history by vm.getRepairHistory(repairId).collectAsState(initial = emptyList())

    var finalCost by remember(repair?.repairId) {
        mutableStateOf(repair?.finalCost?.takeIf { it > 0 }?.toString() ?: "")
    }

    val current = repair ?: return
    val customer = customers.find { it.customerId == current.customerId }
    val context = LocalContext.current
    var statusToNotify by remember(current.repairId) {
        mutableStateOf<String?>(null)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 30.dp)
    ) {
        SheetHeader(
            title = "Repair #${current.repairId}",
            subtitle = "${current.brand} ${current.appliance}",
            icon = Icons.Filled.Build,
            onClose = onClose
        )

        Spacer(Modifier.height(16.dp))

        // Repair overview
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Lavender)
        ) {
            Column(Modifier.padding(17.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("CURRENT STATUS", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(current.status, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    StatusPill(current.status)
                }

                Spacer(Modifier.height(15.dp))
                RepairProgress(current.status)
            }
        }

        Spacer(Modifier.height(18.dp))

        Text("CUSTOMER", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(45.dp)
                    .clip(CircleShape)
                    .background(PaleBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    customer?.name?.take(1)?.uppercase() ?: "?",
                    color = Sky,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(customer?.name ?: "Customer not found", color = Ink, fontWeight = FontWeight.Bold)
                Text(customer?.phone ?: "No phone number", color = Muted, fontSize = 12.sp)
            }

            IconButton(
                onClick = {
                    customer?.phone?.let {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${normalizePhone(it)}"))
                        )
                    }
                },
                enabled = !customer?.phone.isNullOrBlank()
            ) {
                Icon(Icons.Filled.Phone, "Call customer", tint = Sky)
            }

            IconButton(
                onClick = {
                    customer?.phone?.let {
                        val url = "https://wa.me/${normalizePhone(it)}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
                enabled = !customer?.phone.isNullOrBlank()
            ) {
                Icon(Icons.Filled.Chat, "WhatsApp customer", tint = Mint)
            }
        }

        Spacer(Modifier.height(18.dp))
        Divider(color = Color(0xFFEDEBF2))
        Spacer(Modifier.height(15.dp))

        Text("REPAIR INFORMATION", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        DetailLine("Appliance", current.appliance)
        DetailLine("Brand", current.brand)
        DetailLine("Model", current.model.ifBlank { "Not provided" })
        DetailLine("Problem", current.problem)
        DetailLine("Created", dateText(current.createdAt))

        Spacer(Modifier.height(18.dp))
        Text("UPDATE REPAIR STATUS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
            "Tap a stage to update the job. The selected stage syncs with the database.",
            color = Muted,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(12.dp))

        repairStatuses.forEachIndexed { index, status ->
            val selected = current.status == status
            val completed = repairStatuses.indexOf(current.status).let { it >= index }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (selected) Lavender else Color.Transparent)
                    .clickable {
                        if (!selected) {
                            vm.changeStatus(current.repairId, status)
                            statusToNotify = status
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                selected -> Violet
                                completed -> PaleMint
                                else -> Color(0xFFF0EFF4)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (completed && !selected) {
                        Icon(Icons.Filled.Check, null, tint = Mint, modifier = Modifier.size(17.dp))
                    } else {
                        Text(
                            "${index + 1}",
                            color = if (selected) Color.White else Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    status,
                    Modifier.weight(1f),
                    color = if (selected) Violet else Ink,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )

                if (selected) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Violet)
                }
            }
        }








        // WhatsApp status notification
        statusToNotify?.let { newStatus ->

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PaleMint
                )
            ) {
                Column(
                    Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0xFFD7F5E5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MarkChatUnread,
                                contentDescription = "Customer notification",
                                tint = Color(0xFF168B57),
                                modifier = Modifier.size(25.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                "Notify customer",
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            Text(
                                "WhatsApp status update",
                                fontSize = 11.sp,
                                color = Muted
                            )
                        }

                        Icon(
                            Icons.Filled.NotificationsActive,
                            contentDescription = "Notification available",
                            tint = Color(0xFF168B57)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Status updated to \"$newStatus\". " +
                                "Would you like to inform the customer?",
                        color = Muted,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val phone = customer?.phone.orEmpty()
                            val number = normalizePhone(phone)

                            val customerName = customer?.name ?: "Customer"
                            val applianceName =
                                "${current.brand} ${current.appliance}".trim()

                            val message = """
                        Hello $customerName,

                        Update from Shri Ji Electronics!

                        Your $applianceName (Repair #${current.repairId}) status is now: $newStatus.

                        Thank you for choosing Shri Ji Electronics.
                    """.trimIndent()

                            val url =
                                "https://wa.me/$number?text=${Uri.encode(message)}"

                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            )
                        },
                        enabled = !customer?.phone.isNullOrBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Mint
                        )
                    ) {
                        Icon(Icons.Filled.Chat, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Send WhatsApp Update")
                    }

                    TextButton(
                        onClick = { statusToNotify = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Dismiss", color = Muted)
                    }
                }
            }
        }




        Spacer(Modifier.height(18.dp))
        Divider(color = Color(0xFFEDEBF2))
        Spacer(Modifier.height(15.dp))

        Text("COST & PAYMENT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        DetailLine("Estimated cost", money(current.estimatedCost))
        DetailLine("Final cost", if (current.finalCost > 0) money(current.finalCost) else "Not set")

        PremiumField(
            finalCost,
            { finalCost = it },
            "Set or update final cost (₹)",
            Icons.Filled.CurrencyRupee
        )

        OutlinedButton(
            onClick = { vm.updateFinalCost(current.repairId, finalCost) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp)
        ) {
            Text("Save final cost")
        }

        val verifiedAmount = payments
            .filter { it.verificationStatus == "VERIFIED" }
            .sumOf { it.amount }

        val totalCost = if (current.finalCost > 0) current.finalCost else current.estimatedCost
        val balance = (totalCost - verifiedAmount).coerceAtLeast(0.0)

        DetailLine("Verified payments", money(verifiedAmount))
        DetailLine("Outstanding balance", money(balance))

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onAddPayment,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violet)
        ) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Record payment")
        }

        Spacer(Modifier.height(18.dp))
        Text("PAYMENT HISTORY", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        if (payments.isEmpty()) {
            Text("No payments recorded.", color = Muted, fontSize = 12.sp)
        } else {
            payments.forEach { payment ->
                DetailLine(
                    "${payment.method} • ${payment.verificationStatus}",
                    money(payment.amount)
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("STATUS HISTORY", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        if (history.isEmpty()) {
            Text("No status history.", color = Muted, fontSize = 12.sp)
        } else {
            history.forEach { update ->
                DetailLine(
                    update.newStatus,
                    dateText(update.createdAt)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = { vm.deleteRepair(current.repairId); onClose() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)
        ) {
            Icon(Icons.Filled.Delete, null)
            Spacer(Modifier.width(8.dp))
            Text("Delete repair")
        }
    }
}

@Composable
private fun AddPaymentSheet(
    repair: Repair?,
    onClose: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var showQr by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("UPI") }
    var reference by remember { mutableStateOf("") }
    val context = LocalContext.current

    if (repair == null) {
        Text("Repair not found.", Modifier.padding(24.dp))
        return
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 30.dp)
    ) {
        SheetHeader(
            title = "Record payment",
            subtitle = "Repair #${repair.repairId}",
            icon = Icons.Filled.Payments,
            onClose = onClose
        )

        Spacer(Modifier.height(16.dp))
        Text("Estimated cost: ${money(repair.estimatedCost)}", color = Muted)

        PremiumField(
            amount,
            { amount = it },
            "Amount received (₹)",
            Icons.Filled.CurrencyRupee
        )

        Spacer(Modifier.height(12.dp))
        Text("PAYMENT METHOD", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("UPI", "Cash", "Card", "Bank").forEach {
                FilterChip(
                    selected = method == it,
                    onClick = { method = it },
                    label = { Text(it) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Lavender,
                        selectedLabelColor = Violet
                    )
                )
            }
        }

        PremiumField(
            reference,
            { reference = it },
            "Transaction reference (optional)",
            Icons.Filled.Receipt
        )

        if (method == "UPI") {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    showQr = true
                },
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.QrCode, null, tint = Violet)
                Spacer(Modifier.width(8.dp))
                Text("Show receiving QR code")
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = PaleCoral),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, null, tint = Coral)
                Spacer(Modifier.width(9.dp))
                Text(
                    "This payment will remain pending until you verify the actual credit in your bank or UPI account.",
                    color = Ink,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = { onSave(amount, method, reference) },
            enabled = amount.toDoubleOrNull()?.let { it > 0 } == true,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violet)
        ) {
            Text("Save payment")
        }
    }

    if (showQr) {
        com.example.shrijiservicedesk.ui.components.UpiQrDialog(
            upiId = "jainshahab@ptaxis",
            shopName = "Shri Ji Electronics",
            amount = amount,
            repairId = repair.repairId,
            onDismiss = { showQr = false }
        )
    }
}

@Composable
private fun SheetHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClose: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Lavender),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Violet, modifier = Modifier.size(24.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFFF1F0F5))
        ) {
            Icon(Icons.Filled.Close, "Close", tint = Ink)
        }
    }
}

@Composable
private fun PremiumField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        label = { Text(label) },
        leadingIcon = icon?.let {
            { Icon(it, contentDescription = null, tint = Muted) }
        },
        singleLine = label != "Describe the problem",
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Violet,
            unfocusedBorderColor = Color(0xFFE7E5ED),
            focusedContainerColor = Color(0xFFFCFBFE),
            unfocusedContainerColor = Color(0xFFFCFBFE)
        )
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(
            value,
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.widthIn(max = 190.dp)
        )
    }
}

@Composable
private fun StatusPill(status: String) {
    val background = repairStatusBackground(status)
    val tint = repairStatusColor(status)

    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(tint)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            status,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RepairProgress(status: String) {
    val currentIndex = repairStatuses.indexOf(status).coerceAtLeast(0)

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repairStatuses.forEachIndexed { index, _ ->
            val active = index <= currentIndex
            val animatedColor by animateColorAsState(
                targetValue = if (active) Violet else Color(0xFFDAD8E3),
                animationSpec = tween(350),
                label = "progressColor"
            )

            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
            )

            if (index != repairStatuses.lastIndex) {
                Spacer(Modifier.width(3.dp))
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Received", color = Muted, fontSize = 9.sp)
        Text("Completed", color = Muted, fontSize = 9.sp)
    }
}

private fun repairStatusColor(status: String): Color = when (status) {
    "Received" -> Sky
    "Under Inspection" -> Color(0xFF8B68D8)
    "Problem Identified" -> Coral
    "Repair in Progress" -> Color(0xFFD58B32)
    "Ready for Pickup" -> Mint
    "Completed" -> Color(0xFF269B71)
    else -> Muted
}

private fun repairStatusBackground(status: String): Color = when (status) {
    "Received" -> PaleBlue
    "Under Inspection" -> Lavender
    "Problem Identified" -> PaleCoral
    "Repair in Progress" -> Color(0xFFFFF0D9)
    "Ready for Pickup" -> PaleMint
    "Completed" -> Color(0xFFDDF5EA)
    else -> Color(0xFFF0EFF4)
}

private fun applianceIcon(appliance: String): ImageVector {
    return when {
        appliance.contains("tv", true) -> Icons.Filled.Tv
        appliance.contains("fridge", true) -> Icons.Filled.Kitchen
        else -> Icons.Filled.Devices
    }
}

private fun normalizePhone(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return when {
        digits.length == 10 -> "91$digits"
        digits.startsWith("91") -> digits
        else -> digits
    }
}

private fun money(amount: Double): String {
    return "₹" + String.format(Locale.getDefault(), "%,.2f", amount)
}

private fun dateText(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    ).format(Date(timestamp))
}